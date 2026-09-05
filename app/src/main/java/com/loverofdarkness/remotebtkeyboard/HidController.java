package com.loverofdarkness.remotebtkeyboard;

import android.annotation.SuppressLint;
import android.bluetooth.*;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.util.Log;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@SuppressLint("MissingPermission")
public final class HidController implements BluetoothProfile.ServiceListener {
    public enum State { IDLE, SEARCHING, SELECTED, REGISTERING, CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED, ERROR }

    public static final class DeviceRow {
        public final String address, name;
        DeviceRow(BluetoothDevice d) { address = d.getAddress(); name = safeName(d); }
    }

    public static final class Snapshot {
        public final State state;
        public final String selectedName, message, draft;
        public final boolean connected, normalConnected, live, busy;
        public final List<DeviceRow> devices;
        public final int modifiers;
        Snapshot(State s, String n, String m, String d, boolean c, boolean nc, boolean l, boolean b, List<DeviceRow> rows, int mod) {
            state=s; selectedName=n; message=m; draft=d; connected=c; normalConnected=nc; live=l; busy=b;
            devices=Collections.unmodifiableList(new ArrayList<>(rows)); modifiers=mod;
        }
    }

    private final Context context;
    private final BluetoothAdapter adapter;
    private final HandlerThread thread = new HandlerThread("DevilBT-HID");
    private final Handler h;
    private final PowerManager.WakeLock wake;
    private final List<Consumer<Snapshot>> observers = new CopyOnWriteArrayList<>();
    private final LinkedHashMap<String, BluetoothDevice> candidates = new LinkedHashMap<>();

    private BluetoothHidDevice hid;
    private BluetoothDevice selected, host;
    private State state = State.IDLE;
    private String message = "Press Search Devices.";
    private boolean registered, registering, wanted, connected, normalConnected, disconnecting, closed;
    private boolean live = true;
    private int modifiers;
    private String draft = "", remote = "";
    private EditPlan plan;
    private String jobText;
    private int jobIndex;
    private KeyCodec.Stroke pendingStroke;
    private Runnable strokeDone;
    private long epoch;
    private int connectPolls;

    private final BluetoothHidDevice.Callback callback = new BluetoothHidDevice.Callback() {
        @Override public void onAppStatusChanged(BluetoothDevice pluggedDevice, boolean ok) {
            exec(() -> {
                registering = false;
                registered = ok;
                if (!ok) {
                    connected = false;
                    host = null;
                    if (wanted && !disconnecting) fail("HID registration failed. Allow Nearby devices permission and try again.");
                    else { message = "HID registration stopped."; publish(); }
                    return;
                }
                // pluggedDevice is registration status context, not proof of a live HID cable.
                // Only onConnectionStateChanged(STATE_CONNECTED) may set connected=true.
                state = wanted ? State.REGISTERING : State.DISCONNECTED;
                message = wanted ? "HID registered. Waiting to connect to the selected laptop…" : "HID registered.";
                publish();
                if (wanted && selected != null) advance();
            });
        }

        @Override public void onConnectionStateChanged(BluetoothDevice d, int s) { exec(() -> handleConnection(d, s)); }

        @Override public void onGetReport(BluetoothDevice d, byte type, byte id, int size) {
            exec(() -> {
                if (!same(d, host) || id != 0) {
                    if (hid != null) hid.reportError(d, BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID);
                    return;
                }
                byte[] data;
                if (type == BluetoothHidDevice.REPORT_TYPE_INPUT) data = KeyCodec.release();
                else if (type == BluetoothHidDevice.REPORT_TYPE_OUTPUT) data = new byte[]{0};
                else { hid.reportError(d, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ); return; }
                if (size > 0 && size < data.length) {
                    hid.reportError(d, BluetoothHidDevice.ERROR_RSP_INVALID_PARAM);
                    return;
                }
                hid.replyReport(d, type, id, data);
            });
        }

        @Override public void onSetReport(BluetoothDevice d, byte type, byte id, byte[] data) { }
        @Override public void onSetProtocol(BluetoothDevice d, byte protocol) { }
        @Override public void onVirtualCableUnplug(BluetoothDevice d) { exec(() -> { if (same(d, selected)) loss("Laptop removed the HID virtual cable."); }); }
    };

    public HidController(Context c) {
        context = c.getApplicationContext();
        BluetoothManager bm = context.getSystemService(BluetoothManager.class);
        adapter = bm == null ? null : bm.getAdapter();
        PowerManager pm = context.getSystemService(PowerManager.class);
        wake = pm == null ? null : pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DevilRemoteBTKeyboard:input");
        if (wake != null) wake.setReferenceCounted(false);
        thread.start();
        h = new Handler(thread.getLooper());
        exec(() -> {
            if (adapter == null) fail("Bluetooth unavailable.");
            else if (!adapter.isEnabled()) { state=State.DISCONNECTED; message="Bluetooth is off."; publish(); }
            else acquire();
        });
    }

    public void observe(Consumer<Snapshot> o) { observers.add(o); o.accept(snapshot()); }

    private void exec(Runnable r) {
        if (closed) return;
        h.post(() -> {
            try { r.run(); }
            catch (SecurityException e) { fail("Bluetooth permission denied. Check Nearby devices permissions."); }
            catch (RuntimeException e) { Log.e("DevilRemoteBT", "controller error", e); fail("Bluetooth operation failed: " + e.getMessage()); }
        });
    }

    private void later(long ms, Runnable r) {
        if (closed) return;
        h.postDelayed(() -> {
            if (closed) return;
            try { r.run(); }
            catch (RuntimeException e) { Log.e("DevilRemoteBT", "scheduled error", e); fail("Bluetooth operation failed."); }
        }, ms);
    }

    private Snapshot snapshot() {
        return new Snapshot(state, selected == null ? "None" : safeName(selected), message, draft,
                connected, normalConnected, live, pendingStroke != null || jobText != null, rows(), modifiers);
    }

    private void publish() { Snapshot s=snapshot(); for (Consumer<Snapshot> o: observers) o.accept(s); }
    private List<DeviceRow> rows() { List<DeviceRow> r=new ArrayList<>(); for (BluetoothDevice d:candidates.values()) r.add(new DeviceRow(d)); return r; }
    private static String safeName(BluetoothDevice d) { try { String n=d.getName(); return n==null||n.isBlank()?"Bluetooth device":n; } catch(Exception e) { return "Bluetooth device"; } }
    private static boolean same(BluetoothDevice a, BluetoothDevice b) { return a!=null&&b!=null&&a.getAddress().equals(b.getAddress()); }

    private void acquire() {
        if (adapter == null || !adapter.isEnabled() || hid != null) return;
        try { adapter.getProfileProxy(context, this, BluetoothProfile.HID_DEVICE); }
        catch (Exception e) { fail("Could not open Bluetooth HID profile."); }
    }

    @Override public void onServiceConnected(int profile, BluetoothProfile proxy) {
        exec(() -> {
            if (profile == BluetoothProfile.HID_DEVICE) { hid=(BluetoothHidDevice)proxy; refreshCandidates(); advance(); }
            else refreshCandidates();
        });
    }

    @Override public void onServiceDisconnected(int profile) {
        exec(() -> {
            if (profile != BluetoothProfile.HID_DEVICE) return;
            hid=null; registered=false; registering=false; connected=false; host=null;
            if (wanted) loss("Bluetooth HID service disappeared.");
            else { state=State.DISCONNECTED; message="Bluetooth HID service unavailable."; publish(); }
        });
    }

    public void adapterChanged(int newState) {
        exec(() -> {
            if (newState == BluetoothAdapter.STATE_ON) { state=State.DISCONNECTED; message="Bluetooth enabled. Search for a laptop to select."; publish(); acquire(); }
            else if (newState == BluetoothAdapter.STATE_OFF) { wanted=false; connected=false; normalConnected=false; host=null; registered=false; registering=false; state=State.DISCONNECTED; message="Bluetooth is off."; publish(); }
        });
    }

    public void search() {
        exec(() -> {
            if (adapter==null || !adapter.isEnabled()) { state=State.DISCONNECTED; message="Enable Bluetooth first."; publish(); return; }
            state=State.SEARCHING; message="Searching paired Bluetooth devices…"; publish();
            acquire();
            refreshCandidates();
            state=selected==null?State.DISCONNECTED:State.SELECTED;
            message=candidates.isEmpty()?"No paired Bluetooth devices found. Pair your laptop first.":"Select the target device, then press Connect.";
            publish();
        });
    }

    private void refreshCandidates() {
        candidates.clear();
        if (adapter==null || !adapter.isEnabled()) return;
        for (BluetoothDevice d : adapter.getBondedDevices()) candidates.put(d.getAddress(), d);
        if (hid!=null) for (BluetoothDevice d : hid.getConnectedDevices()) candidates.put(d.getAddress(), d);
    }

    public void aclChanged(BluetoothDevice d, boolean on) {
        exec(() -> {
            if (d==null) return;
            if (on) candidates.put(d.getAddress(), d);
            else {
                boolean paired=false;
                try { for (BluetoothDevice x:adapter.getBondedDevices()) if (same(x,d)) { paired=true; break; } } catch(Exception ignored) { }
                if (!paired) candidates.remove(d.getAddress());
            }
            if (same(d, selected)) normalConnected=on;
            if (!on && same(d, host) && connected) loss("HID laptop Bluetooth connection was lost.");
            publish();
        });
    }

    public void select(String address) {
        exec(() -> {
            if (wanted || connected || disconnecting) { message="Disconnect before changing device."; publish(); return; }
            refreshCandidates();
            BluetoothDevice d=candidates.get(address);
            if (d==null) { message="Device is no longer paired or available."; publish(); return; }
            selected=d; normalConnected=false;
            state=State.SELECTED; message="Selected "+safeName(d)+". Press Connect."; publish();
        });
    }

    public void connect() {
        exec(() -> {
            if (wanted || connected || disconnecting) return;
            if (adapter==null || !adapter.isEnabled()) { message="Enable Bluetooth first."; publish(); return; }
            if (selected==null) { message="Search and select a device first."; publish(); return; }
            refreshCandidates();
            BluetoothDevice d=candidates.get(selected.getAddress());
            if (d==null) { message="Selected device is no longer paired or available."; publish(); return; }
            selected=d;
            if (selected.getBondState()!=BluetoothDevice.BOND_BONDED) { message="Device must be paired with this phone first."; publish(); return; }
            // Public Android APIs do not provide a generic arbitrary classic-Bluetooth connect call.
            // This action therefore means the user-selected target is ready for the normal Bluetooth
            // connection flow; the actual keyboard cable is started separately by startHid/connectHid.
            normalConnected = false;
            state=State.SELECTED;
            message="Selected "+safeName(selected)+". Use the Bluetooth settings Connect action if normal Bluetooth is not already connected, then press Start Keyboard HID.";
            publish();
        });
    }

    public void startHid() {
        exec(() -> {
            if (wanted || connected || disconnecting) return;
            if (adapter==null || !adapter.isEnabled()) { message="Enable Bluetooth first."; publish(); return; }
            if (selected==null) { message="Search and select a device first."; publish(); return; }
            refreshCandidates();
            BluetoothDevice d=candidates.get(selected.getAddress());
            if (d==null) { message="Selected device is no longer paired or available."; publish(); return; }
            selected=d;
            if (selected.getBondState()!=BluetoothDevice.BOND_BONDED) { message="Device must be paired with this phone first."; publish(); return; }
            wanted=true; disconnecting=false; connectPolls=0;
            state=State.REGISTERING; message="Preparing HID keyboard registration…"; publish();
            acquire(); advance();
            later(30000, () -> { if (wanted && !connected) fail("HID connection timed out. Check laptop Bluetooth HID/input support and try again."); });
        });
    }

    // Kept for source compatibility with the previous UI/controller API.
    public void connectHid() { startHid(); }

    private void advance() {
        if (closed || !wanted || selected==null || connected || hid==null) return;
        if (!registered) {
            if (registering) return;
            registering=true; state=State.REGISTERING; message="Registering phone as Bluetooth keyboard…"; publish();
            BluetoothHidDeviceAppSdpSettings s=new BluetoothHidDeviceAppSdpSettings(
                    "Devil RemoteBT Keyboard", "Bluetooth keyboard", "Devil RemoteBT Keyboard",
                    BluetoothHidDevice.SUBCLASS1_KEYBOARD, KeyCodec.descriptor());
            boolean accepted=hid.registerApp(s,null,null,h::post,callback);
            if (!accepted) { registering=false; fail("Android rejected HID registration. Check Nearby devices permissions and retry."); }
            return;
        }
        startHostConnect();
    }

    private void startHostConnect() {
        if (!wanted || selected==null || hid==null || connected) return;
        state=State.CONNECTING; message="HID registered. Connecting to "+safeName(selected)+"…"; publish();
        boolean accepted=hid.connect(selected);
        if (!accepted) { fail("Android rejected the HID connection request. Make sure the laptop is paired and supports Bluetooth HID."); return; }
        pollConnection();
    }

    private void pollConnection() {
        if (!wanted || connected || hid==null || selected==null) return;
        int s=hid.getConnectionState(selected);
        if (s==BluetoothProfile.STATE_CONNECTED) { handleConnection(selected,s); return; }
        if (s==BluetoothProfile.STATE_CONNECTING) { state=State.CONNECTING; message="HID connection in progress…"; publish(); }
        if (++connectPolls<20) later(500,this::pollConnection);
        else if (wanted&&!connected) fail("Laptop did not accept the HID keyboard connection.");
    }

    private void handleConnection(BluetoothDevice d, int s) {
        if (!same(d,selected)) {
            if ((s==BluetoothProfile.STATE_CONNECTED || s==BluetoothProfile.STATE_CONNECTING) && hid!=null) hid.disconnect(d);
            return;
        }
        if (s==BluetoothProfile.STATE_CONNECTING) { state=State.CONNECTING; message="Laptop is establishing the HID keyboard connection…"; publish(); }
        else if (s==BluetoothProfile.STATE_CONNECTED) {
            if (connected) return;
            host=d; connected=true; wanted=true; epoch++; remote=""; draft=""; plan=null;
            state=State.CONNECTED; message="HID CONNECTED. Ready to type."; publish();
        } else if (s==BluetoothProfile.STATE_DISCONNECTED && connected) loss("HID connection to laptop was lost.");
    }

    public void disconnect() {
        exec(() -> {
            wanted=false; disconnecting=true; epoch++; cancelInput(); modifiers=0;
            BluetoothDevice d=host!=null?host:selected;
            if (hid!=null && d!=null && registered) { try { hid.disconnect(d); } catch(Exception ignored) { } }
            connected=false; host=null; state=State.DISCONNECTING; message="Disconnecting HID…"; publish();
            later(1200,this::finishDisconnect);
        });
    }

    private void finishDisconnect() {
        if (!disconnecting) return;
        disconnecting=false; unregister(); state=State.DISCONNECTED; message="HID disconnected."; publish();
    }

    private void unregister() {
        if (hid!=null && (registered||registering)) { try { hid.unregisterApp(); } catch(Exception ignored) { } }
        registered=false; registering=false;
    }

    private void loss(String why) {
        wanted=false; disconnecting=false; epoch++; cancelInput(); connected=false; host=null; modifiers=0;
        if (hid!=null && registered && selected!=null) { try { hid.disconnect(selected); } catch(Exception ignored) { } }
        unregister(); state=State.DISCONNECTED; message=why; publish();
    }

    private void fail(String why) {
        wanted=false; disconnecting=false; epoch++; cancelInput(); connected=false; host=null; state=State.ERROR; message=why; publish();
    }

    private void cancelInput() {
        pendingStroke=null; strokeDone=null; jobText=null; jobIndex=0; plan=null;
        if (wake!=null && wake.isHeld()) wake.release();
    }

    public void setLive(boolean on) { exec(() -> { if(pendingStroke!=null||jobText!=null){message="Finish the current send before changing mode.";publish();return;} live=on;remote="";plan=null;message=on?"Live mode enabled.":"Buffered mode enabled.";publish(); }); }

    public void setDraft(String text) {
        exec(() -> {
            if (text.length()>20000) { message="Text is too long."; publish(); return; }
            draft=text;
            if (!connected) { publish(); return; }
            String target;
            try { target=KeyCodec.normalize(text); } catch(Exception e) { message=e.getMessage(); publish(); return; }
            if (live) scheduleSync(target);
            publish();
        });
    }

    private void scheduleSync(String target) { if(!connected)return; final long e=epoch; h.post(() -> pumpLive(target,e)); }

    private void pumpLive(String target,long e) {
        if(closed||e!=epoch||!connected)return;
        if(pendingStroke!=null){later(8,()->pumpLive(target,e));return;}
        if(!remote.equals(target)){
            if(plan==null||!plan.target.equals(target))plan=new EditPlan(remote,target);
            if(plan.deletes>0){tap(new KeyCodec.Stroke(KeyCodec.BACKSPACE,0),()->{if(remote.length()>0)remote=remote.substring(0,remote.length()-1);plan.deletes--;pumpLive(target,e);});return;}
            if(plan.index<target.length()){char c=target.charAt(plan.index);tap(KeyCodec.charStroke(c,false),()->{remote+=c;plan.index++;pumpLive(target,e);});}
        }
    }

    public void sendBuffer() {
        exec(() -> {
            if(!connected||live||jobText!=null)return;
            String t; try{t=KeyCodec.normalize(draft);}catch(Exception e){message=e.getMessage();publish();return;}
            if(t.isEmpty())return; jobText=t; jobIndex=0; startWake(); pumpJob();
        });
    }

    private void pumpJob(){
        if(!connected||jobText==null||pendingStroke!=null)return;
        if(jobIndex>=jobText.length()){jobText=null;releaseWake();message="Buffer sent.";publish();return;}
        tap(KeyCodec.charStroke(jobText.charAt(jobIndex),false),()->{jobIndex++;pumpJob();});
    }

    public void special(int usage,int baseMod){
        exec(() -> {
            if(!connected)return;
            int m=baseMod|modifiers; modifiers=0; final String flush=live?draft:"";
            if(live&&!flush.isEmpty()){
                String t;try{t=KeyCodec.normalize(flush);}catch(Exception e){message=e.getMessage();publish();return;}
                final long e=epoch; syncThen(t,e,()->tap(new KeyCodec.Stroke(usage,m),()->{draft="";remote="";plan=null;publish();}));
            } else tap(new KeyCodec.Stroke(usage,m),this::publish);
        });
    }

    private void syncThen(String t,long e,Runnable done){
        if(!connected||e!=epoch)return;
        if(!remote.equals(t)){
            if(pendingStroke!=null){later(8,()->syncThen(t,e,done));return;}
            if(plan==null||!plan.target.equals(t))plan=new EditPlan(remote,t);
            if(plan.deletes>0){tap(new KeyCodec.Stroke(KeyCodec.BACKSPACE,0),()->{remote=remote.substring(0,remote.length()-1);plan.deletes--;syncThen(t,e,done);});return;}
            if(plan.index<t.length()){char c=t.charAt(plan.index);tap(KeyCodec.charStroke(c,false),()->{remote+=c;plan.index++;syncThen(t,e,done);});return;}
        }
        plan=null;done.run();
    }

    public void modifier(int mask){exec(()->{if(connected){modifiers^=mask;publish();}});}
    private void startWake(){if(wake!=null&&!wake.isHeld())wake.acquire(15000);}
    private void releaseWake(){if(wake!=null&&wake.isHeld())wake.release();}

    private void tap(KeyCodec.Stroke s,Runnable done){
        if(pendingStroke!=null)throw new IllegalStateException("HID stroke overlap");
        pendingStroke=s; strokeDone=done; startWake();
        emit(KeyCodec.report(s.modifiers,s.usage),()->emit(KeyCodec.release(),()->{Runnable r=strokeDone;pendingStroke=null;strokeDone=null;if(r!=null)r.run();}));
    }

    private void emit(byte[] data,Runnable next){
        if(!connected||hid==null||host==null){pendingStroke=null;strokeDone=null;releaseWake();return;}
        sendReport(data,next,0);
    }

    private void sendReport(byte[] data,Runnable next,int attempt){
        if(!connected||hid==null||host==null){pendingStroke=null;strokeDone=null;releaseWake();return;}
        boolean ok;
        try{ok=hid.sendReport(host,0,data);}catch(Exception e){ok=false;}
        if(ok){next.run();return;}
        if(attempt<4)later(10L<<attempt,()->sendReport(data,next,attempt+1));
        else {pendingStroke=null;strokeDone=null;releaseWake();message="HID report was rejected by Android.";publish();}
    }

    public void close(){
        exec(() -> {
            closed=true; wanted=false; connected=false; normalConnected=false; cancelInput();
            if(hid!=null){try{if(registered||registering)hid.unregisterApp();}catch(Exception ignored){}try{adapter.closeProfileProxy(BluetoothProfile.HID_DEVICE,hid);}catch(Exception ignored){}}
            hid=null; registered=false; registering=false; observers.clear(); thread.quitSafely();
        });
    }
}
