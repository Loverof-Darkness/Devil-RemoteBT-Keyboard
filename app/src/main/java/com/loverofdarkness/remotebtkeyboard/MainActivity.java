package com.loverofdarkness.remotebtkeyboard;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class MainActivity extends Activity {
    private static final int REQ=10;
    private KeyboardService service; private boolean bound,searchAfterBind,updatingSpinner;
    private TextView status,info,selectedInfo; private EditText editor; private Switch live; private Button send,keys,enter,connect;
    private Spinner devices;
    private final ArrayList<HidController.DeviceRow> rows=new ArrayList<>();
    private final Consumer<HidController.Snapshot> observer=s->runOnUiThread(()->render(s));
    private final ServiceConnection conn=new ServiceConnection(){
        public void onServiceConnected(ComponentName n,IBinder b){
            service=((KeyboardService.Binder)b).service(); bound=true;
            HidController c=service.controller();
            if(c!=null){c.observe(observer);if(searchAfterBind){searchAfterBind=false;c.search();}}
        }
        public void onServiceDisconnected(ComponentName n){bound=false;service=null;setComposerEnabled(false);}
    };

    @Override protected void onCreate(Bundle b){super.onCreate(b);getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);build();}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
    private GradientDrawable rounded(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}

    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(12),dp(8),dp(12),dp(8));setContentView(root);
        TextView title=new TextView(this);title.setText("Devil RemoteBT Keyboard");title.setTextSize(23);title.setPadding(0,dp(4),0,dp(6));root.addView(title);

        LinearLayout top=new LinearLayout(this);
        Button search=btn("Search Active Devices"),bt=btn("Bluetooth Settings");
        top.addView(search,new LinearLayout.LayoutParams(0,-2,1));top.addView(bt,new LinearLayout.LayoutParams(0,-2,1));root.addView(top);

        status=new TextView(this);status.setTextSize(16);status.setPadding(0,dp(5),0,0);root.addView(status);
        info=new TextView(this);info.setPadding(0,0,0,dp(4));root.addView(info);

        LinearLayout chooser=new LinearLayout(this);chooser.setOrientation(LinearLayout.VERTICAL);chooser.setPadding(0,dp(3),0,dp(5));
        TextView label=new TextView(this);label.setText("Available devices");label.setTextSize(14);chooser.addView(label);
        devices=new Spinner(this);devices.setPrompt("Select Bluetooth device");
        chooser.addView(devices,new LinearLayout.LayoutParams(-1,dp(52)));
        root.addView(chooser);

        selectedInfo=new TextView(this);selectedInfo.setTextSize(14);selectedInfo.setPadding(dp(4),dp(2),dp(4),dp(4));root.addView(selectedInfo);

        connect=btn("Connect as Keyboard");connect.setTextSize(16);root.addView(connect,new LinearLayout.LayoutParams(-1,-2));

        ScrollView spacer=new ScrollView(this);spacer.setVisibility(View.GONE);root.addView(spacer,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout controls=new LinearLayout(this);
        live=new Switch(this);live.setText("Live");live.setChecked(true);keys=btn("Keys");send=btn("Send Buffer");
        controls.addView(live,new LinearLayout.LayoutParams(0,-2,1));controls.addView(keys,new LinearLayout.LayoutParams(0,-2,1));controls.addView(send,new LinearLayout.LayoutParams(0,-2,1));root.addView(controls);

        LinearLayout composer=new LinearLayout(this);composer.setOrientation(LinearLayout.HORIZONTAL);composer.setGravity(Gravity.BOTTOM|Gravity.CENTER_VERTICAL);composer.setPadding(0,dp(4),0,0);
        editor=new EditText(this);editor.setHint("Type message…");editor.setGravity(Gravity.TOP|Gravity.START);editor.setMinLines(1);editor.setMaxLines(5);editor.setSingleLine(false);
        editor.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editor.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_ENTER_ACTION);editor.setPadding(dp(16),dp(10),dp(16),dp(10));editor.setBackground(rounded(Color.rgb(245,245,245),24));
        composer.addView(editor,new LinearLayout.LayoutParams(0,-2,1));
        enter=btn("➤");enter.setTextSize(20);enter.setContentDescription("Send Enter key");LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(dp(54),dp(54));ep.setMargins(dp(7),0,0,0);composer.addView(enter,ep);root.addView(composer);

        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,new ArrayList<>());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);devices.setAdapter(adapter);
        devices.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(android.widget.AdapterView<?> p){}
            public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){
                if(updatingSpinner||position<0||position>=rows.size()||service==null||service.controller()==null)return;
                service.controller().select(rows.get(position).address);
            }
        });

        editor.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){if(service!=null&&service.controller()!=null)service.controller().setDraft(s.toString());}public void afterTextChanged(android.text.Editable e){}});
        search.setOnClickListener(v->ensureService(true));
        bt.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        connect.setOnClickListener(v->{
            if(service==null||service.controller()==null)return;
            HidController.Snapshot s=currentSnapshot;
            if(s!=null&&(s.connected||s.state==HidController.State.CONNECTING||s.state==HidController.State.REGISTERING))service.controller().disconnect();
            else service.controller().startHid();
        });
        live.setOnCheckedChangeListener((v,c)->{if(service!=null&&service.controller()!=null)service.controller().setLive(c);});
        send.setOnClickListener(v->{if(service!=null&&service.controller()!=null)service.controller().sendBuffer();});
        enter.setOnClickListener(v->{if(service!=null&&service.controller()!=null)service.controller().special(KeyCodec.ENTER,0);});
        keys.setOnClickListener(v->showKeys());
        setComposerEnabled(false);ensureService(false);
    }

    private HidController.Snapshot currentSnapshot;

    private void ensureService(boolean search){
        if(Build.VERSION.SDK_INT>=31){
            boolean c=checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;
            boolean a=checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE)==PackageManager.PERMISSION_GRANTED;
            if(!c||!a){searchAfterBind=search;requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_ADVERTISE},REQ);return;}
        }
        try{
            startForegroundService(new Intent(this,KeyboardService.class));
            if(search)searchAfterBind=true;
            if(!bound)bound=bindService(new Intent(this,KeyboardService.class),conn,BIND_AUTO_CREATE);
            if(search&&service!=null&&service.controller()!=null){searchAfterBind=false;service.controller().search();}
        }catch(Exception e){status.setText("Service start failed: "+e.getMessage());}
    }

    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){
        super.onRequestPermissionsResult(r,p,g);
        if(r==REQ&&g.length>=2&&g[0]==PackageManager.PERMISSION_GRANTED&&g[1]==PackageManager.PERMISSION_GRANTED)ensureService(true);
        else if(r==REQ)status.setText("Nearby devices permission is required.");
    }

    private void render(HidController.Snapshot s){
        if(s==null)return; currentSnapshot=s;
        String bluetooth=s.normalConnected?"🟢 Link active":"🔵 Paired";
        String hid=s.connected?"🟢 Connected":(s.state==HidController.State.REGISTERING||s.state==HidController.State.CONNECTING?"🟠 Connecting":"🔴 Disconnected");
        status.setText("Bluetooth: "+bluetooth+"    HID Keyboard: "+hid);
        info.setText(s.message);

        rows.clear();rows.addAll(s.devices);
        ArrayAdapter<String> a=(ArrayAdapter<String>)devices.getAdapter();
        updatingSpinner=true;a.clear();
        int selected=0;
        for(int i=0;i<rows.size();i++){
            HidController.DeviceRow d=rows.get(i);
            a.add(d.name+"\n"+d.address);
            if(d.name.equals(s.selectedName))selected=i;
        }
        a.notifyDataSetChanged();
        if(rows.isEmpty()){devices.setEnabled(false);selectedInfo.setText("No paired Bluetooth devices found. Pair the laptop first, then search again.");}
        else {devices.setEnabled(!s.connected&&!s.busy);devices.setSelection(Math.min(selected,rows.size()-1),false);HidController.DeviceRow d=rows.get(Math.min(selected,rows.size()-1));selectedInfo.setText("Target: "+d.name+"\nAddress: "+d.address);}
        updatingSpinner=false;

        boolean busy=s.connected||s.state==HidController.State.REGISTERING||s.state==HidController.State.CONNECTING;
        if(s.connected){connect.setText("Disconnect");connect.setEnabled(true);}
        else if(s.state==HidController.State.REGISTERING||s.state==HidController.State.CONNECTING){connect.setText("Cancel");connect.setEnabled(true);}
        else {connect.setText("Connect as Keyboard");connect.setEnabled(!rows.isEmpty()&&!s.busy);}
        if(busy)devices.setEnabled(false);

        setComposerEnabled(s.connected&&!s.busy);
        live.setEnabled(s.connected&&!s.busy);keys.setEnabled(s.connected&&!s.busy);send.setEnabled(s.connected&&!s.live&&!s.busy);
        if(!s.draft.equals(editor.getText().toString())&&!editor.hasFocus()){editor.setText(s.draft);editor.setSelection(editor.length());}
    }

    private void setComposerEnabled(boolean enabled){
        if(editor==null)return;editor.setEnabled(enabled);editor.setFocusable(enabled);editor.setFocusableInTouchMode(enabled);enter.setEnabled(enabled);
        if(!enabled){InputMethodManager imm=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);if(imm!=null)imm.hideSoftInputFromWindow(editor.getWindowToken(),0);editor.clearFocus();}
    }

    private void showKeys(){
        String[] names={"Esc","Tab","Backspace","Delete","Enter","Arrow Up","Arrow Down","Arrow Left","Arrow Right","Home","End","Page Up","Page Down","Insert","Caps Lock","F1","F2","F3","F4","F5","F6","F7","F8","F9","F10","F11","F12","Print Screen","Scroll Lock","Pause / Break","Ctrl","Shift","Alt","Meta / Windows"};
        int[] usages={0x29,0x2B,0x2A,0x4C,0x28,0x52,0x51,0x50,0x4F,0x4A,0x4D,0x4B,0x4E,0x49,0x39,0x3A,0x3B,0x3C,0x3D,0x3E,0x3F,0x40,0x41,0x42,0x43,0x44,0x45,0x46,0x47,0x48,KeyCodec.CTRL,KeyCodec.SHIFT,KeyCodec.ALT,KeyCodec.GUI};
        new AlertDialog.Builder(this).setTitle("Keyboard Keys").setItems(names,(d,w)->{if(service==null||service.controller()==null)return;if(w>=30)service.controller().modifier(usages[w]);else service.controller().special(usages[w],0);}).setNegativeButton("Cancel",null).show();
    }

    @Override protected void onStart(){super.onStart();if(!bound)try{bound=bindService(new Intent(this,KeyboardService.class),conn,0);}catch(Exception ignored){}}
    @Override protected void onStop(){if(bound){try{unbindService(conn);}catch(Exception ignored){}bound=false;}super.onStop();}
}
