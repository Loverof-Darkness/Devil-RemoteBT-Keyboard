package com.loverofdarkness.remotebtkeyboard;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.function.Consumer;

public final class MainActivity extends Activity {
    private static final int REQ=10;
    private KeyboardService service;
    private boolean bound;
    private boolean searchAfterBind;
    private LinearLayout root,list;
    private TextView status,info;
    private EditText editor;
    private Switch live;
    private Button send;
    private int baseBottomPadding;
    private final Consumer<HidController.Snapshot> observer=snapshot->runOnUiThread(()->render(snapshot));
    private final ServiceConnection conn=new ServiceConnection(){
        @Override public void onServiceConnected(ComponentName n,IBinder b){service=((KeyboardService.Binder)b).service();bound=true;HidController c=service.controller();if(c!=null){c.observe(observer);if(searchAfterBind){searchAfterBind=false;c.search();}}}
        @Override public void onServiceDisconnected(ComponentName n){bound=false;service=null;disableInput();}
    };
    @Override protected void onCreate(Bundle b){super.onCreate(b);getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);build();}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private Button btn(String s){Button b=new Button(this);b.setText(s);return b;}
    private void build(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);baseBottomPadding=dp(10);root.setPadding(dp(12),dp(10),dp(12),baseBottomPadding);setContentView(root);
        if(Build.VERSION.SDK_INT>=30){root.setOnApplyWindowInsetsListener((v,insets)->{int imeBottom=insets.getInsets(WindowInsets.Type.ime()).bottom;int navBottom=insets.getInsets(WindowInsets.Type.navigationBars()).bottom;int bottom=Math.max(imeBottom,navBottom);root.setPadding(root.getPaddingLeft(),root.getPaddingTop(),root.getPaddingRight(),baseBottomPadding+bottom);return insets;});root.post(()->root.requestApplyInsets());}
        TextView title=new TextView(this);title.setText("Devil RemoteBT Keyboard");title.setTextSize(24);root.addView(title);
        LinearLayout top=new LinearLayout(this);Button search=btn("Search Devices"),bt=btn("Bluetooth Settings");top.addView(search,new LinearLayout.LayoutParams(0,-2,1));top.addView(bt,new LinearLayout.LayoutParams(0,-2,1));root.addView(top);
        status=new TextView(this);status.setTextSize(18);root.addView(status);info=new TextView(this);root.addView(info);
        ScrollView sv=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);sv.addView(list);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        live=new Switch(this);live.setText("Live Mode");live.setChecked(true);root.addView(live);
        editor=new EditText(this);editor.setHint("Write something…");editor.setGravity(Gravity.TOP|Gravity.START);editor.setMinLines(3);editor.setMaxLines(5);editor.setInputType(0x00004001|0x00020000);editor.setImeOptions(0x00000006);editor.setSingleLine(false);editor.setPadding(dp(12),dp(10),dp(12),dp(10));root.addView(editor,new LinearLayout.LayoutParams(-1,-2));
        editor.setOnFocusChangeListener((v,hasFocus)->{if(hasFocus){editor.postDelayed(()->{editor.requestFocus();InputMethodManager imm=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);if(imm!=null)imm.showSoftInput(editor,InputMethodManager.SHOW_IMPLICIT);root.requestApplyInsets();},100);}});
        editor.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){if(service!=null&&service.controller()!=null)service.controller().setDraft(s.toString());}public void afterTextChanged(android.text.Editable e){}});
        LinearLayout row=new LinearLayout(this);Button emoji=btn("🙂"),attach=btn("📎"),special=btn("Keys"),enter=btn("↵");row.addView(emoji,new LinearLayout.LayoutParams(0,-2,1));row.addView(attach,new LinearLayout.LayoutParams(0,-2,1));row.addView(special,new LinearLayout.LayoutParams(0,-2,1));row.addView(enter,new LinearLayout.LayoutParams(0,-2,1));root.addView(row);
        send=btn("Send Buffer");root.addView(send);
        search.setOnClickListener(v->ensureService(true));bt.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));live.setOnCheckedChangeListener((v,c)->{if(service!=null&&service.controller()!=null)service.controller().setLive(c);});send.setOnClickListener(v->{if(service!=null&&service.controller()!=null)service.controller().sendBuffer();});enter.setOnClickListener(v->{if(service!=null&&service.controller()!=null)service.controller().special(KeyCodec.ENTER,0);});emoji.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Emoji").setMessage("Standard keyboard HID cannot universally transmit Unicode emoji. Use ASCII equivalents such as :) or <3.").setPositiveButton("OK",null).show());attach.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Attachment").setMessage("Keyboard HID is not a file-transfer protocol, so arbitrary images/files cannot be attached to the PC without companion software.").setPositiveButton("OK",null).show());special.setOnClickListener(v->showKeys());
        disableInput();ensureService(false);
    }
    private void ensureService(boolean search){
        if(Build.VERSION.SDK_INT>=31){boolean c=checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;boolean a=checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE)==PackageManager.PERMISSION_GRANTED;if(!c||!a){searchAfterBind=search;requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_ADVERTISE},REQ);return;}}
        try{startForegroundService(new Intent(this,KeyboardService.class));if(search)searchAfterBind=true;if(!bound)bound=bindService(new Intent(this,KeyboardService.class),conn,BIND_AUTO_CREATE);if(search&&service!=null&&service.controller()!=null){searchAfterBind=false;service.controller().search();}}catch(Exception e){status.setText("Service start failed: "+e.getMessage());}
    }
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ&&g.length>=2&&g[0]==PackageManager.PERMISSION_GRANTED&&g[1]==PackageManager.PERMISSION_GRANTED)ensureService(true);else if(r==REQ)status.setText("Nearby-device permissions are required for Bluetooth HID keyboard mode.");}
    private void render(HidController.Snapshot s){
        if(s==null){status.setText("Status: starting…");return;}
        status.setText("Device: "+s.selectedName+"\nHID: "+(s.connected?"● CONNECTED":"○ "+s.state));
        info.setText(s.message+"\nThe text box stays usable so the Android keyboard can open; text is transmitted only while HID is CONNECTED.");
        list.removeAllViews();
        for(HidController.DeviceRow d:s.devices){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);TextView name=new TextView(this);name.setText(d.name);name.setTextSize(17);row.addView(name);LinearLayout actions=new LinearLayout(this);Button sel=btn("Select Device"),c=btn("Connect"),x=btn("Disconnect");actions.addView(sel,new LinearLayout.LayoutParams(0,-2,1));actions.addView(c,new LinearLayout.LayoutParams(0,-2,1));actions.addView(x,new LinearLayout.LayoutParams(0,-2,1));row.addView(actions);sel.setOnClickListener(v->service.controller().select(d.address));c.setOnClickListener(v->service.controller().connect());x.setOnClickListener(v->service.controller().disconnect());list.addView(row);}
        editor.setEnabled(true);send.setEnabled(s.connected&&!s.live&&!s.busy);live.setEnabled(!s.busy);send.setText(s.live?"Send Buffer":"Send Buffer (use composer text)");
        if(!s.draft.equals(editor.getText().toString())&&!editor.hasFocus()){int start=editor.getSelectionStart();int pos=start<0?editor.length():Math.min(start,s.draft.length());editor.setText(s.draft);editor.setSelection(Math.max(0,Math.min(pos,editor.length())));}
    }
    private void disableInput(){if(editor!=null)editor.setEnabled(true);if(send!=null)send.setEnabled(false);}
    private void showKeys(){String[] names={"Esc","Tab","Backspace","Delete","Enter","Arrow Up","Arrow Down","Arrow Left","Arrow Right","Home","End","Page Up","Page Down","Insert","Caps Lock","F1","F2","F3","F4","F5","F6","F7","F8","F9","F10","F11","F12","Print Screen","Scroll Lock","Pause / Break","Ctrl","Shift","Alt","Meta / Windows"};int[] usages={0x29,0x2B,0x2A,0x4C,0x28,0x52,0x51,0x50,0x4F,0x4A,0x4D,0x4B,0x4E,0x49,0x39,0x3A,0x3B,0x3C,0x3D,0x3E,0x3F,0x40,0x41,0x42,0x43,0x44,0x45,0x46,0x47,0x48,KeyCodec.CTRL,KeyCodec.SHIFT,KeyCodec.ALT,KeyCodec.GUI};new AlertDialog.Builder(this).setTitle("Keyboard Keys").setItems(names,(d,w)->{if(service==null||service.controller()==null)return;if(w>=30)service.controller().modifier(usages[w]);else service.controller().special(usages[w],0);}).setNegativeButton("Cancel",null).show();}
    @Override protected void onStart(){super.onStart();if(!bound){try{bound=bindService(new Intent(this,KeyboardService.class),conn,0);}catch(Exception ignored){}}}
    @Override protected void onStop(){if(bound){try{unbindService(conn);}catch(Exception ignored){}bound=false;}super.onStop();}
}
