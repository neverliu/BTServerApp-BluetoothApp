package com.mega.deviceapp.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mega.deviceapp.MainActivity;
import com.mega.deviceapp.R;
import com.mega.deviceapp.adapter.CommonAdapter;
import com.mega.deviceapp.adapter.ViewHolder;
import com.mega.deviceapp.model.Contacts;
import com.mega.deviceapp.model.WifiBasicInfo;
import com.mega.deviceapp.service.BluetoothChatService;
import com.mega.deviceapp.service.BluetoothSocketService;
import com.mega.deviceapp.util.BluetoothCmd;
import com.mega.deviceapp.util.ContactsUtil;
import com.mega.deviceapp.util.MessageCode;
import com.mega.deviceapp.util.ToastUtil;
import com.mega.deviceapp.util.WifiAdmin;
import com.mega.deviceapp.util.WifiSearcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class WifiConfigureActivity extends AppCompatActivity implements  View.OnClickListener{

    private String TAG = "WifiConfigureActivity";
//    private RelativeLayout closeWifiBtn;
    private RelativeLayout setWifiBtn ;
    private View    view2;
    private LinearLayout switchLayout , view1;
    private ImageView wifiPullDown , wifiPwdDelect;
    private TextView  ssidView, reSetSateBtn,openCloseWifi;
    private EditText pwView;
    public StringBuffer mStrBuff = new StringBuffer();
    private View dialogView;
    private ListView mListView;
    private List<ScanResult> mWifisList;
    private MyAdapter myAdapter;
    private Toolbar mToolBar;

   // private AlertDialog.Builder mDialog;

    private String mConnectedDeviceName;

    private ProgressDialog mLoadingDialog;

    private Intent mIntent;
    private Messenger mMessage , sMessage;

    private int sWifiSate = -1;
    private WifiAdmin mAdmin;
    private boolean openWifiStatu = false;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MessageCode.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                          //  setStatus(1,mConnectedDeviceName,"连接到  " + mConnectedDeviceName);
                            Log.d(TAG,"连接到 "+mConnectedDeviceName);
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                         //   setStatus("连接中");
                            Log.d(TAG,"连接中");
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                         //   setStatus("无连接");
                            Log.d(TAG,"无连接");
                            break;
                    }
                    break;
                case MessageCode.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                   // receive(readMessage);
                    Log.i(TAG,"message_read  readmessage = " + readMessage);
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    checkReadMessage(msg);
                    break;
                case MessageCode.MESSAGE_DEVICE_NAME:
                    mConnectedDeviceName = msg.getData().getString(MessageCode.DEVICE_NAME);
                    if (null != this) {
                        ToastUtil.showMsg(WifiConfigureActivity.this,"Connected to "
                                + mConnectedDeviceName);
                    }
                    break;
                case MessageCode.MESSAGE_TOAST:
                    if (null != this){
                        ToastUtil.showMsg(WifiConfigureActivity.this,msg.getData().getString(MessageCode.TOAST));
                    }
                    break;
            }
        }
    };

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG,"onServiceConnected");
            sMessage = new Messenger(service);
            Message msg = new Message();
            msg.what = MessageCode.MESSAGE_BIND_SERVICE;
            msg.replyTo = mMessage;
            try {
                sMessage.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            setWifiStateCommand();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG,"onServiceDisconnected");
            sMessage = null;
        }
    };
    private WifiSearcher mWifiSearcher;
    private SwitchCompat mSwitchCompat;
    private Message mMessage1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_configure);
        //mListView = (ListView) findViewById(R.id.wifi_listview);
        mLoadingDialog = new ProgressDialog(this);
        mLoadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mLoadingDialog.setMessage("正在获取WiFi列表");
        reSetSateBtn = (TextView) findViewById(R.id.btn_reset_state);
//        reSetSateBtn.setOnClickListener(this);
        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mToolBar.setTitle(R.string.wifi_device_list);
        setSupportActionBar(mToolBar);
        final android.support.v7.app.ActionBar ab = getSupportActionBar();
        ab.setHomeAsUpIndicator(R.mipmap.ic_actionbar_back);
        ab.setDisplayHomeAsUpEnabled(true);

//        stateView = (TextView) findViewById(R.id.tv_wifi_state);

        ssidView = (TextView) findViewById(R.id.tv_ssid);
//        wifiState = (TextView) findViewById(R.id.wifi_state);
        ssidView.setOnClickListener(this);

        wifiPullDown = (ImageView) findViewById(R.id.wifi_pull_down);
        wifiPullDown.setOnClickListener(this);

        wifiPwdDelect = (ImageView) findViewById(R.id.wifi_pwd_delect);
        wifiPwdDelect.setOnClickListener(this);
        pwView = (EditText) findViewById(R.id.tv_pw);

        setWifiBtn = (RelativeLayout) findViewById(R.id.btn_set_wifi);
        view1 = (LinearLayout) findViewById(R.id.view1);
        view2 =  findViewById(R.id.view2);
        switchLayout = (LinearLayout) findViewById(R.id.switch_layout);
        switchLayout.setOnClickListener(this);
        setWifiBtn.setOnClickListener(this);

//        closeWifiBtn = (RelativeLayout) findViewById(R.id.btn_close_wifi);
//        closeWifiBtn.setOnClickListener(this);
//        openCloseWifi = (TextView) findViewById(R.id.tv_close_wifi);
        mMessage = new Messenger(mHandler);
        mAdmin = new WifiAdmin(this);
        mSwitchCompat = (SwitchCompat)findViewById(R.id.sc_switch_compat);
//        mSwitchCompat.setOnClickListener(this);
        mWifiSearcher = new WifiSearcher(this, new WifiSearcher.SearchWifiListener() {
            @Override
            public void onSearchWifiFailed(WifiSearcher.ErrorType errorType) {

            }

            @Override
            public void onSearchWifiSuccess(List<ScanResult> results) {
                mLoadingDialog.dismiss();
                mAdmin.startScan();
                mWifisList = results;
                showMessageDialog();
            }
        });
        mAdmin.register(new WifiAdmin.WLANStateListener() {
            @Override
            public void onStateChanged() {
                Log.d(TAG, "MainActivity --> onStateEnabled--> ");
            }

            @Override
            public void onStateDisabled() {

            }

            @Override
            public void onStateDisabling() {

            }

            @Override
            public void onStateEnabled() {
                Log.d(TAG, "liuhao MainActivity --> onStateEnabled--> ");
                if (mLoadingDialog != null && openWifiStatu == true) {
                    Log.d(TAG, "liuhao onStateEnabled:");
                            mWifiSearcher.search();
                            openWifiStatu = false;
                }
            }
            @Override
            public void onStateEnabling() {

            }

            @Override
            public void onStateUnknow() {

            }
        });
        bindService();
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if(id==android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
        mAdmin.unregister();
    }

    private void bindService(){
        mIntent = new Intent(this, BluetoothSocketService.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);
    }

    private void setDialog(String name){
        if(mLoadingDialog !=null){
            mLoadingDialog.dismiss();
        }
        mLoadingDialog = new ProgressDialog(this);
        mLoadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mLoadingDialog.setMessage(name);
        mLoadingDialog.show();
    }

    private void checkReadMessage(Message msg){
        byte[] readBuff = (byte[]) msg.obj;
        int command = readBuff[0];
        Log.i(TAG,"checkreadMessage : command = " + command);
        //final Message msg1 = msg;
        switch (command){
            case BluetoothCmd.COMMAND_SCAN_WIFI:

                break;
            case BluetoothCmd.COMMAND_WIFI_INFO:
                String SSID = null;
                String ADDRESS = null;
//                ssidView.setText("");
                if(msg.arg1 > 3){
                    int ssidLength = readBuff[3];
                    //            byte[] data = new byte[msg.arg1-3];
                    //            System.arraycopy(readBuff, 3, data, 0, data.length);
                    //            SSID = new String(data);
                    byte[] ssidData = new byte[ssidLength];
                    System.arraycopy(readBuff, 4, ssidData, 0, ssidLength);
                    SSID = new String(ssidData);
                    Log.d(TAG,"setWifiStatus:"+SSID);

                    if(msg.arg1 > 4 + ssidLength){
                        byte[] addressData = new byte[msg.arg1-(4+ssidLength)];
                        System.arraycopy(readBuff, (4+ssidLength), addressData, 0, addressData.length);
                        ADDRESS = new String(addressData);
                        Log.d(TAG,"setWifiStatus ADDRESS:"+ADDRESS);
                    }
                }
                int statebyte = readBuff[1];
                int contectedbyte = readBuff[2];
                switch(statebyte){
                    case BluetoothCmd.WIFI_STATE_DISABLED:
                        sWifiSate = WifiManager.WIFI_STATE_DISABLED;
                        break;
                    case BluetoothCmd.WIFI_STATE_DISABLING:
                        sWifiSate = WifiManager.WIFI_STATE_DISABLING;
                        break;
                    case BluetoothCmd.WIFI_STATE_ENABLED:
                        sWifiSate = WifiManager.WIFI_STATE_ENABLED;
                        break;
                    case BluetoothCmd.WIFI_STATE_ENABLING:
                        sWifiSate = WifiManager.WIFI_STATE_ENABLING;
                        if(contectedbyte == BluetoothCmd.WIFI_CONTECTED && SSID != null){
                            ssidView.setText(SSID);
                        }
                        break;
                    default:
                        sWifiSate = WifiManager.WIFI_STATE_UNKNOWN;
                        break;
                }
                Log.d(TAG,"liu COMMAND_WIFI_INFO wifisate = " + sWifiSate);
                if(view1.getVisibility() == View.GONE){
                    view1.setVisibility(View.VISIBLE);
                    view2.setVisibility(View.GONE);
                }
                switch (sWifiSate){
                    case WifiManager.WIFI_STATE_ENABLED:
                        mSwitchCompat.setChecked(true);
                        break;
                    case WifiManager.WIFI_STATE_DISABLED:
                      mSwitchCompat.setChecked(false);
                        break;
                    default:
                        mSwitchCompat.setChecked(false);
                        break;
                }
                mLoadingDialog.dismiss();
                break;
            case BluetoothCmd.COMMAND_LINK_WIFI:
                if(readBuff[1] == BluetoothCmd.SEND_ACK){
                    Toast.makeText(this,getResources().getString(R.string.toast_configuration_success),Toast.LENGTH_SHORT).show();

                }else if(readBuff[1] == BluetoothCmd.SEND_ERROR){
                    Toast.makeText(this,getResources().getString(R.string.toast_configuration_fail),Toast.LENGTH_SHORT).show();
                }
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                      setWifiStateCommand();
                    }
                }, 500);
                mLoadingDialog.dismiss();
                break;
            default:
                // byte[] readBuf = (byte[]) msg.obj;
                mLoadingDialog.dismiss();
                String readMessage = new String(readBuff, 0, msg.arg1);
                Log.i(TAG,"checkReadMessage other msg : " + readMessage);
                break;
        }
    }
    private void showMessageDialog(){
        View dialogView = View.inflate(this,R.layout.v_dlg_msg,null);
        final AlertDialog mDialog = new AlertDialog.Builder(this)
        //dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        .setTitle("选择WiFi")
        .setView(dialogView).create();
        ListView   mListView = (ListView) dialogView.findViewById(R.id.listview_dlg);
        if(myAdapter == null)
            myAdapter = new MyAdapter(this,mWifisList,R.layout.item_list_scan_wifis);
        myAdapter.setList(mWifisList);
        mListView.setAdapter(myAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String ssid = mWifisList.get(position).SSID;
                ssidView.setText(ssid);
                mDialog.dismiss();
            }
        });
        Button btn_copy = (Button) dialogView.findViewById(R.id.btn_copy);
        btn_copy.setVisibility(View.GONE);
        mDialog.show();
    }

    private List<WifiBasicInfo> getWifisFromMsg(String msg) {
        List<WifiBasicInfo> list = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(msg);
            for(int i=0;i<array.length();i++){
                JSONObject ob = array.getJSONObject(i);
                Log.i(TAG,"getWifisFromMsg ssid : " + ob.getString("ssid")+ " , level : "+ob.getInt("level"));
                WifiBasicInfo con = new WifiBasicInfo(ob.getString("ssid"),ob.getInt("level"));
                list.add(con);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    //发送获取WiFi command
    private void setWifiStateCommand(){
        Message message = new Message();
        byte[] command = new byte[2];
        message.what = MessageCode.MESSAGE_WRITE_TO_SERVICE;
        command[0] = BluetoothCmd.COMMAND_WIFI_INFO;
        message.obj = command;
        try {
            sMessage.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        Message message = new Message();
        message.what = MessageCode.MESSAGE_WRITE_TO_SERVICE;
        byte[] command = new byte[2];
        switch (viewId){
            case R.id.wifi_pull_down:
            case R.id.tv_ssid:
                mLoadingDialog.show();
                if(mAdmin.checkWifistatu()){
                    mLoadingDialog.dismiss();
                    mAdmin.startScan();
                    mWifisList = mAdmin.getWifiList();
                    showMessageDialog();
                    openWifiStatu = false;
                }else{
                    openWifiStatu = true;
                    mAdmin.openWifi(this);
                }
                break;
            case R.id.wifi_pwd_delect:
                pwView.setText(null);
                break;
            case R.id.btn_set_wifi://设置WiFi
                String ssidPw = ssidView.getText() + pwView.getText().toString();
                message.what = MessageCode.MESSAGE_WRITE_TO_SERVICE;
                command = new byte[512];
                command[0] = BluetoothCmd.COMMAND_LINK_WIFI;
                command[1] = (byte) ssidView.getText().length();
                command[2] = (byte) pwView.getText().length();
                byte[] data = ssidPw.getBytes();
                System.arraycopy(data, 0 , command, 3, data.length);
                if(sWifiSate != -1){
                    setDialog("设置wifi中...");
                }
                break;
//            case R.id.btn_close_wifi://开关WiFi
            case R.id.switch_layout:
                command[0] = BluetoothCmd.COMMAND_CHANGE_WIFI_STATE;
                if(sWifiSate == WifiManager.WIFI_STATE_DISABLED){
                    command[1] = BluetoothCmd.COMMAND_OPEN_WIFI;
                }else if(sWifiSate == WifiManager.WIFI_STATE_ENABLED){
                    command[1] = BluetoothCmd.COMMAND_CLOSE_WIFI;
                }
                if(sWifiSate != -1){
                    if(command[1] == BluetoothCmd.COMMAND_OPEN_WIFI){
                        setDialog("打开WIFI中...");
                    }else{
                        setDialog("关闭wifi中...");
                    }
                }
                break;
        }

        message.obj = command;
        try {
            sMessage.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private class MyAdapter extends CommonAdapter<ScanResult> {
        int[] singleDrawable = {R.mipmap.ic_wifi_lock_signal_1_dark, R.mipmap.ic_wifi_lock_signal_2_dark,
                R.mipmap.ic_wifi_lock_signal_3_dark, R.mipmap.ic_wifi_lock_signal_4_dark};
        /** anything worse than or equal to this will show 0 bars. */
        private static final int min_rssi = -100;

        /** anything better than or equal to this will show the max bars. */
        private static final int max_rssi = -55;

        public MyAdapter(Context context, List<ScanResult> list, int layoutId){
            super(context, list, layoutId);
        }
        @Override
        public void convert(ViewHolder holder, final ScanResult wifiInfo, int position){
            int drawable =getSingleImage(wifiInfo.level);
            //Log.w(TAG,"convert level : " + wifiInfo.getLevel());
            holder.setText(R.id.tv_item_wifi_ssid,wifiInfo.SSID);
            holder.display(R.id.wifi_signal_image,drawable);


        }

        private int getSingleImage(int level){
            if(level >= max_rssi){//55
                return singleDrawable[3];
            }else if (level > max_rssi - 20 && level < max_rssi){//-75 / -55
                return singleDrawable[2];
            }else if(level > min_rssi && level <= max_rssi - 20){//-100 / -75
                return singleDrawable[1];
            }else {//-100
                return singleDrawable[0];
            }
        }
    }


}
