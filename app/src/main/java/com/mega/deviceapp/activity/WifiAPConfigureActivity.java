package com.mega.deviceapp.activity;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.mega.deviceapp.R;
import com.mega.deviceapp.adapter.CommonAdapter;
import com.mega.deviceapp.adapter.ViewHolder;
import com.mega.deviceapp.model.WifiBasicInfo;
import com.mega.deviceapp.service.BluetoothChatService;
import com.mega.deviceapp.service.BluetoothSocketService;
import com.mega.deviceapp.util.BluetoothCmd;
import com.mega.deviceapp.util.MessageCode;
import com.mega.deviceapp.util.ToastUtil;
import com.mega.deviceapp.util.WifiAdmin;
import com.mega.deviceapp.util.WifiSearcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class WifiAPConfigureActivity extends AppCompatActivity implements
        View.OnClickListener, AdapterView.OnItemSelectedListener {

    private String TAG = "WifiApConfigureActivity";
//    private RelativeLayout closeWifiBtn;
    private RelativeLayout setWifiBtn ;
    private View    view2;
    private LinearLayout switchLayout , view1;
    private ImageView wifiPullDown , wifiPwdDelect;


    public StringBuffer mStrBuff = new StringBuffer();
    private View dialogView;
    private ListView mListView;
    private List<ScanResult> mWifisList;
    private MyAdapter myAdapter;
    private Toolbar mToolBar;

    private EditText pwView, ssidView;
   // private TextView  ssidView;
    private Spinner mSpinner;
    private CheckedTextView mCheckBox;
    private Button mSetupBtn;
    private View pwLayout, mStateView;
    private Switch mSwitch;
    boolean currentState = false;


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
                            setViewEnabled(true);
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                         //   setStatus("连接中");
                            Log.d(TAG,"连接中");
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                         //   setStatus("无连接");
                            Log.d(TAG,"无连接");
                            setViewEnabled(false);
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
                        ToastUtil.showMsg(WifiAPConfigureActivity.this,"Connected to "
                                + mConnectedDeviceName);
                    }
                    break;
                case MessageCode.MESSAGE_TOAST:
                    if (null != this){
                        ToastUtil.showMsg(WifiAPConfigureActivity.this,msg.getData().getString(MessageCode.TOAST));
                    }
                    break;
                case MessageCode.MESSAGE_GET_SOCKET_STATE:
                    Log.i(TAG,"MESSAGE_GET_SOCKET_STATE");
                    int state = (int)msg.obj;
                    if(state == BluetoothChatService.STATE_LISTEN ||
                            state == BluetoothChatService.STATE_NONE){
                       // updateDisConnectText();
                        setViewEnabled(false);
                    }else {
                        setViewEnabled(true);
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
            sendRequestWifiApInfoCommand();
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
        setContentView(R.layout.activity_wifiap_configure);

        mLoadingDialog = new ProgressDialog(this);
        mLoadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
       // mLoadingDialog.setMessage("正在获取WiFi列表");

        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mToolBar.setTitle(R.string.wifi_ap_device_list);
        setSupportActionBar(mToolBar);
       final android.support.v7.app.ActionBar ab = getSupportActionBar();
        ab.setHomeAsUpIndicator(R.mipmap.ic_actionbar_back);
        ab.setDisplayHomeAsUpEnabled(true);

        TextChangeLinsenner changeLinsenner = new TextChangeLinsenner();
        ssidView = (EditText) findViewById(R.id.ssid_et);
        pwView = (EditText) findViewById(R.id.pw_et);
        ssidView.addTextChangedListener(changeLinsenner);
        pwView.addTextChangedListener(changeLinsenner);

        mSpinner = (Spinner) findViewById(R.id.spinner);
        mSpinner.setOnItemSelectedListener(this);

        pwLayout = findViewById(R.id.pw_view);
        mCheckBox = (CheckedTextView) findViewById(R.id.checkbox_view);
        mCheckBox.setOnClickListener(this);

        mSetupBtn = (Button) findViewById(R.id.setup_btn);
        mSetupBtn.setOnClickListener(this);

        mStateView = findViewById(R.id.ap_state_layout);
        mStateView.setOnClickListener(this);

        mSwitch = (Switch) findViewById(R.id.switch_btn);

        mMessage = new Messenger(mHandler);
       // mAdmin = new WifiAdmin(this);
        mSwitchCompat = (SwitchCompat)findViewById(R.id.sc_switch_compat);
//        mSwitchCompat.setOnClickListener(this);

        bindService();
    }

    private class TextChangeLinsenner implements TextWatcher{

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            updateSetupButton();
        }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
        @Override
        public void afterTextChanged(Editable s) {
            updateSetupButton();
        }
    }

    private void updateSetupButton(){
        if(isSetupWifiAP){
            return;
        }
        String ssid = ssidView.getText().toString();
        String pw = pwView.getText().toString();
        int ssidLen = ssid.length();
        int pwLen = pw.length();
        if(pwLayout.getVisibility() == View.VISIBLE){
        if(ssidLen < 1 || pwLen < 8){
            mSetupBtn.setEnabled(false);
        }else {
            mSetupBtn.setEnabled(true);
        }
        }else {
            mSetupBtn.setEnabled(ssidLen >= 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSocketStatus();
    }

    private void updateSocketStatus(){
        Message message = new Message();
        message.what = MessageCode.MESSAGE_GET_SOCKET_STATE;
        Log.i(TAG,"updateSocketStatus sMessage = "+sMessage);
        if(sMessage != null){
            try {
                sMessage.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
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
            case BluetoothCmd.COMMAND_GET_WIFI_AP_STATE:
                getWifiApInfo(msg);
                setViewEnabled(true);
                break;
            case BluetoothCmd.COMMAND_CHANGE_WIFI_AP_STATE:
                int success = readBuff[1];
                if(!isSetupWifiAP){
                    if(success == BluetoothCmd.COMMAND_WIFI_AP_STATE_ON){
                        currentState = true;
                        ToastUtil.showMsg(this,R.string.main_wifi_ap_open_msg);
                    }else if(success == BluetoothCmd.COMMAND_WIFI_AP_STATE_OFF) {
                        currentState = false;
                        ToastUtil.showMsg(this,R.string.main_wifi_ap_close_msg);
                    }else if(success == BluetoothCmd.SEND_ERROR){
                        currentState = false;
                        ToastUtil.showMsg(this,R.string.main_wifi_ap_fail_msg);
                    }
                    if(success != BluetoothCmd.SEND_ERROR)
                        getWifiApInfo(msg);

                    setViewEnabled(true);
                }
                break;
            case BluetoothCmd.COMMAND_SET_UP_WIFI_AP_INFO:
                int setSuccess = readBuff[1];
                if(setSuccess == BluetoothCmd.SEND_ERROR){
                    ToastUtil.showMsg(this,R.string.toast_wifi_ap_failed);
                }else {
                    ToastUtil.showMsg(this,R.string.toast_wifi_ap_success);
                    getWifiApInfo(msg);
                }
                setViewEnabled(true);
                isSetupWifiAP = false;
                break;

            default:
                // byte[] readBuf = (byte[]) msg.obj;
                mLoadingDialog.dismiss();
                String readMessage = new String(readBuff, 0, msg.arg1);
                Log.i(TAG,"checkReadMessage other msg : " + readMessage);
                break;
        }
    }

    private void getWifiApInfo(Message msg){
        byte[] data = (byte[]) msg.obj;
        int ssidLen = data[2];
        boolean wifiApOn = data[1]== BluetoothCmd.COMMAND_WIFI_AP_STATE_ON;
        mSwitch.setChecked(wifiApOn);
        currentState = wifiApOn;
        String ssid = new String(data, 3, ssidLen);
        ssidView.setText(ssid);
        if(msg.arg1 > 3+ssidLen ){
            String password = new String(data, 3+ssidLen, msg.arg1-(3+ssidLen));
            mSpinner.setSelection(1, true);
            pwView.setText(password);
        }else {
            mSpinner.setSelection(0, true);
           // pwView.setText("");
        }
    }


    //发送获取WiFi command
    private void sendRequestWifiApInfoCommand(){
        Message message = new Message();
        byte[] command = new byte[2];
        message.what = MessageCode.MESSAGE_WRITE_TO_SERVICE;
        command[0] = BluetoothCmd.COMMAND_GET_WIFI_AP_STATE;
        message.obj = command;
        try {
            sMessage.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void changeWifiAPState(boolean turnOn){
        Message message = new Message();
        message.what = MessageCode.MESSAGE_WRITE_TO_SERVICE;
        byte[] command = new byte[2];
        command[0] = BluetoothCmd.COMMAND_CHANGE_WIFI_AP_STATE;
        command[1] = turnOn ? BluetoothCmd.COMMAND_WIFI_AP_STATE_ON : BluetoothCmd.COMMAND_WIFI_AP_STATE_OFF;
        message.obj = command;
        try {
            sMessage.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void setUpServerWifiAP(boolean turnon, boolean setUp){
        Message message = new Message();
        message.what = MessageCode.MESSAGE_WRITE_TO_SERVICE;
        byte[] command = new byte[2];
        String ssid = null;
        String pw = null;
        if(setUp){
             ssid = ssidView.getText().toString();
            command = new byte[3+ssid.length()];
            if(pwLayout.getVisibility() == View.VISIBLE){
                pw = pwView.getText().toString();
                command = new byte[3+ssid.length()+pw.length()];
            }
        }
        command[0] = BluetoothCmd.COMMAND_SET_UP_WIFI_AP_INFO;
        command[1] = turnon ? BluetoothCmd.COMMAND_WIFI_AP_STATE_ON : BluetoothCmd.COMMAND_WIFI_AP_STATE_OFF;
        if(setUp){
            byte[] ssidByte = ssid.getBytes();
            command[2] = (byte)ssid.length();
            System.arraycopy(ssidByte, 0, command, 3, ssidByte.length);
            if(pw != null){
                byte[] pwbyte = pw.getBytes();
                System.arraycopy(pwbyte, 0, command, 3+ssidByte.length, pwbyte.length);
            }
        }
        Log.d(TAG,"setup datalen:"+ command.length);
        message.obj = command;
        try {
            sMessage.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void passwordVisibleChange(){
        boolean isChecked = mCheckBox.isChecked();
        mCheckBox.setChecked(!isChecked);
        if(!isChecked){
            pwView.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        }else {
            pwView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        pwView.postInvalidate();
        //切换后将EditText光标置于末尾
        CharSequence charSequence = pwView.getText();
        if (charSequence instanceof Spannable) {
            Spannable spanText = (Spannable) charSequence;
            Selection.setSelection(spanText, charSequence.length());
        }
    }

    private void setViewEnabled(boolean enabled){
        mStateView.setEnabled(enabled);
        mStateView.setClickable(enabled);
        mSetupBtn.setEnabled(enabled);
        if(mStateView instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) mStateView;
            Log.i(TAG,"vg.chialdcount = "+ vg.getChildCount());
            for (int i = 0; i < vg.getChildCount(); i++){
                View view = vg.getChildAt(i);
                view.setEnabled(enabled);
               // setEnabledAll(vg.getChildAt(i), enabled);
            }
        }
    }

    private boolean isSetupWifiAP = false;
    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        switch (viewId){
            case R.id.setup_btn:
                setUpServerWifiAP(true, true);
                mSwitch.setChecked(!currentState);
                isSetupWifiAP = true;
                setViewEnabled(false);
                break;
            case R.id.checkbox_view:
                passwordVisibleChange();
                break;
            case R.id.ap_state_layout:
                boolean isCheck = mSwitch.isChecked();
                mSwitch.setChecked(!isCheck);
                changeWifiAPState(!isCheck);
                setViewEnabled(false);
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) parent.getAdapter();
        String str = adapter.getItem(position);
        if(position == 0){
            pwLayout.setVisibility(View.GONE);
        }else if(position == 1){
            pwLayout.setVisibility(View.VISIBLE);
        }
        updateSetupButton();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        mSpinner.setSelection(0, true);
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
