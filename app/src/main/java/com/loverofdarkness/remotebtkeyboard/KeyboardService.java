package com.loverofdarkness.remotebtkeyboard;

import android.Manifest;
import android.app.*;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.os.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class KeyboardService extends Service {
    static final String ACTION_DISCONNECT="com.loverofdarkness.remotebtkeyboard.DISCONNECT";
    private static final int ID=7; private static final String CHANNEL="keyboard";
    final class Binder extends android.os.Binder { KeyboardService service(){return KeyboardService.this;} }
    private final Binder binder=new Binder(); private final CopyOnWriteArrayList<HidController.Snapshot> unused=new CopyOnWriteArrayList<>();
    private HidController controller; private boolean receiver;
    private final BroadcastReceiver btReceiver=new BroadcastReceiver(){
        @Override public void onReceive(Context c,Intent i){
            if(controller==null)return;
            String a=i.getAction();
            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(a)) controller.adapterChanged(i.getIntExtra(BluetoothAdapter.EXTRA_STATE,BluetoothAdapter.ERROR));
            else if(BluetoothDevice.ACTION_FOUND.equals(a)) controller.discoveryFound(device(i));
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(a)) controller.discoveryFinished();
            else if(BluetoothDevice.ACTION_ACL_CONNECTED.equals(a)) controller.aclChanged(device(i),true);
            else if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(a)) controller.aclChanged(device(i),false);
        }
    };
    private static BluetoothDevice device(Intent i){if(Build.VERSION.SDK_INT>=33)return i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE,BluetoothDevice.class);return i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);}
    @Override public void onCreate(){super.onCreate();getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(CHANNEL,"Bluetooth keyboard",NotificationManager.IMPORTANCE_LOW));}
    @Override public int onStartCommand(Intent intent,int flags,int id){
        Notification n=new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.stat_sys_data_bluetooth).setContentTitle("Devil RemoteBT Keyboard").setContentText("Bluetooth keyboard service").setOngoing(true).build();
        if(Build.VERSION.SDK_INT>=29)startForeground(ID,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);else startForeground(ID,n);
        if(controller==null){controller=new HidController(this);registerReceiver();}
        if(intent!=null&&ACTION_DISCONNECT.equals(intent.getAction()))controller.disconnect();
        return START_NOT_STICKY;
    }
    private void registerReceiver(){
        IntentFilter f=new IntentFilter();
        f.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        f.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        f.addAction(BluetoothDevice.ACTION_FOUND);
        f.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        f.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(btReceiver,f,Context.RECEIVER_EXPORTED);else registerReceiver(btReceiver,f);
        receiver=true;
    }
    HidController controller(){return controller;}
    @Override public IBinder onBind(Intent i){return binder;}
    @Override public void onDestroy(){if(receiver)unregisterReceiver(btReceiver);if(controller!=null)controller.close();super.onDestroy();}
}
