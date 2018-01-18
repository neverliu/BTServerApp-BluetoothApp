package com.mega.btserverapp.service;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.StaleDataException;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.mega.btserverapp.MainActivity;
import com.mega.btserverapp.model.Contacts;
import com.mega.btserverapp.model.SmsInfo;
import com.mega.btserverapp.model.WifiBasicInfo;
import com.mega.btserverapp.util.BluetoothCmd;
import com.mega.btserverapp.util.ContactsUtil;
import com.mega.btserverapp.util.SmsUtil;
import com.mega.btserverapp.util.WifiAdmin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class BluetoothControlService extends Service implements Runnable{

    // Debugging
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    private static final String ACTION_WIFI_AP_STATE_CHANGE = "android.net.wifi.WIFI_AP_STATE_CHANGED";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
   // private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;

    private WifiManager wifiManager;

    private WifiAdmin mWifiAdmin;

    private StringBuffer mStringBuff = new StringBuffer();

    private WifiBasicInfo wifiBasicInfo;
    private boolean isSetWifiCommand = false;
    private boolean isSetWifiLink = false;

    private TelephonyManager mTelephonyManager;
    private PhoneStatListener mListener;

    //DISCONNECTING
    private boolean isWifiDisConnectting = false;
    private boolean isWifiConnectting = false;
    private SmsUtil smsUtil;
    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                          //  setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                           // mConversationArrayAdapter.clear();
                            sendWifState();
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            sendSimCardInfo();
                            new Handler().postDelayed(new Runnable() {
                                public void run() {
                                    sendBatteryInfo();
                                }
                            }, 300);
                            new Handler().postDelayed(new Runnable() {
                                public void run() {
                                    Log.d(TAG,"liuhao getDataState:"+getMobileDataStatus());
                                    if(getMobileDataStatus()){
                                        sendDataInfo(BluetoothCmd.COMMAND_DATA_STATE_ON);
                                    }else{
                                        sendDataInfo(BluetoothCmd.COMMAND_DATA_STATE_OFF);
                                    }

                                }
                            }, 600);
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                           // setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                           // setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    Log.i(TAG,"write : " + writeMessage);
                 //   mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Log.i(TAG,"readmessage : " + readMessage);
                  //  String read2 = new String(readBuf,2, msg.arg1);
                 //   Log.i(TAG,"read2 : " + read2);
                    if(readMessage.equals("reset")){
                        sendMessages("reset");
                    }
                    checkReadMessage(msg);
                   // mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //蓝牙变化
            if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
                int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                if(D)Log.w(TAG," mReceiver bluestate = " + blueState);
                switch (blueState){
                    case BluetoothAdapter.STATE_ON:
                        if(mChatService == null){
                            mChatService = new BluetoothChatService(context, mHandler);
                            mChatService.start();
                        }
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        if(mChatService != null){
                            mChatService.stop();
                            if(D)Log.d(TAG,"mReceiver STATE_OFF");
                        }
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Toast.makeText(context,"蓝牙关闭，请打开蓝牙。 ", Toast.LENGTH_LONG).show();
                        if(mChatService != null){
                            //mChatService.stop();
                            if(D)Log.d(TAG,"mReceiver STATE_TURNING_OFF");
                        }
                        break;
                }
            }else if(action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)){//wifi连接上与否

                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                NetworkInfo.State state = info.getState();
                System.out.println("网络状态改变: " + state);

                if(state == (NetworkInfo.State.DISCONNECTED)){
                  //  System.out.println("wifi网络连接断开");
                    if(isWifiDisConnectting){
                        sendWifState();
                        isWifiDisConnectting = false;
                    }

                }else if(state == (NetworkInfo.State.CONNECTED)){
                    WifiManager wifiManager = (WifiManager)getApplicationContext()
                            .getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();

                    //获取当前wifi名称
                    System.out.println("连接到网络 :" + wifiInfo.getSSID());
                    if(isWifiConnectting){
                        mWifiAdmin = new WifiAdmin(getApplicationContext());
                        sendWifContectedState(wifiInfo.getSSID());
                        isWifiConnectting = false;
                    }
                }else if (info.getState() == NetworkInfo.State.CONNECTING){
                    isWifiConnectting = true;
                }else if (info.getState() == NetworkInfo.State.DISCONNECTING){
                    isWifiDisConnectting = true;
                }

            } else if(intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)){//wifi打开与否
                int wifistate = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);

                if(wifistate == WifiManager.WIFI_STATE_DISABLED){
                    System.out.println("系统关闭wifi");
                    //更新WiFi状态
                    sendWifState();
                }
                else if(wifistate == WifiManager.WIFI_STATE_ENABLED) {
                    System.out.println("系统开启wifi");
                    if (wifiBasicInfo != null && isSetWifiCommand) {
                        isSetWifiLink = true;
                        isSetWifiCommand = false;
                        mWifiAdmin.addNetwork(
                                mWifiAdmin.CreateWifiInfo(wifiBasicInfo.getSsid(),
                                        wifiBasicInfo.getPassWord(), 3));


                    }
                    //更新WiFi状态
                    sendWifState();
                }
            } else if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
                int level = intent.getIntExtra("level", 0);
                int  scale = intent.getIntExtra("scale", 0);
                int status = intent.getIntExtra("status", 0);
                Log.d(TAG, "liuhao current level :" + level + ", status:" + status+",scale="+scale);
                Log.d(TAG, "liuhao pre level :" + mBatteryLevel + ", status:" + mCurrentBatteryStatus+",scale="+mBatteryScale);
                //Any change will update
                if (mBatteryLevel == -1 || level!=mBatteryLevel ||  scale!=mBatteryScale
                        || status != mCurrentBatteryStatus){
                    mBatteryLevel = level;
                    mBatteryScale = scale;
                    mCurrentBatteryStatus = status;
                    sendBatteryInfo();
                }

            }else if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)){
                NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                Log.d(TAG,"liuhao getState:"+info.getState()+" getExtraInfo:"+info.getExtraInfo());
                if(getMobileDataStatus()){
                    sendDataInfo(BluetoothCmd.COMMAND_DATA_STATE_ON);
                }else{
                    sendDataInfo(BluetoothCmd.COMMAND_DATA_STATE_OFF);
                }
            }else if(intent.getAction().equals(ACTION_WIFI_AP_STATE_CHANGE)){
                //便携式热点的状态为：10---正在关闭；11---已关闭；12---正在开启；13---已开启
                int state = intent.getIntExtra("wifi_state", 0);
                if(state == 11 || state == 13){
                    sendWifiApState(BluetoothCmd.COMMAND_CHANGE_WIFI_AP_STATE);
                }
                if(isSetupWifiAP){
                    if(state == 11){
                        setWifiApEnabled(true);
                    }else if(state == 13){
                        sendWifiApState(BluetoothCmd.COMMAND_SET_UP_WIFI_AP_INFO);
                        isSetupWifiAP = false;
                    }
                }
            }
        }

    };

    private void sendBatteryInfo() {
        Log.d(TAG, "sendBatteryInfo batteryLevel:" + mBatteryLevel + " status:" + mCurrentBatteryStatus);
        //byte[] data = new byte[2 + mBatteryLevel.length()];
        byte[] data = new byte[4];
        data[0] = BluetoothCmd.BATTERY_LEVEL;
        data[1] = (byte) mCurrentBatteryStatus;
        data[2] = (byte) mBatteryLevel;
        data[3] = (byte) mBatteryScale;
        Log.d(TAG, "liuhao Status:" + data[1] + " Level:" + data[2]+",Scale="+data[3]);
        //byte[] ssidbyte = mBatteryLevel.getBytes();
        //System.arraycopy(ssidbyte, 0, data, 2, ssidbyte.length);
        sendMessages(data);
    }

    private void sendDataInfo(int status) {
        int DataStatus = getMobileDataState(this, null);
        //        setDataConnectionState();
        byte[] data = new byte[2];
        data[0] = BluetoothCmd.COMMAND_DATA_STATE;
        data[1] = (byte) status;
        sendMessages(data);
    }

    private List<SmsInfo> mSmsInfoList = null;
    private int mSmsInfoLength;
    private static final int SOCKETTRANSFERLENGTH = 20;//短信每次固定传输20条list
    private int malreadySmsInfoLength = 1;//当前list在总list的长度
    private int mListSizeToTransferLength = 1;
    private BatteryManager mBatteryManager;
    private int mCurrentBatteryStatus;
    private int mBatteryScale;
    private int mBatteryLevel = -1;


    public BluetoothControlService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG,"service oncreate");
        // Get local Bluetooth adapter
//        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mBatteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            // finish();
            return;
        }
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            if (mChatService == null)
                setupChat();
        }
        mRingtoneManager = new RingtoneManager(this);
        mRingtoneManager.setType(RingtoneManager.TYPE_RINGTONE);
        mRingToneHandler = new Handler();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        intentFilter.addAction(Intent.ACTION_BATTERY_OKAY);
        intentFilter.addAction(Intent.ACTION_BATTERY_LOW);
        intentFilter.addAction(ACTION_WIFI_AP_STATE_CHANGE);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.setPriority(1000);//设置优先级
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(mReceiver, intentFilter);

        mWifiAdmin = new WifiAdmin(this);
        smsUtil = new SmsUtil(this);
       // smsUtil.showSmsList();

        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
//开始监听
        mListener = new PhoneStatListener();
//监听信号强度
        mTelephonyManager.listen(mListener, PhoneStatListener.LISTEN_SIGNAL_STRENGTHS);

    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");
        // Initialize the array adapter for the conversation thread
        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);
        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessages(String message) {
        if(mChatService == null){
            return;
        }
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
           // Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);
            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
        }
    }

    private void sendMessages(byte[] message) {
        if(mChatService == null){
            return;
        }
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            return;
        }
        // Check that there's actually something to send
        if (message.length> 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            mChatService.write(message);
        }
    }

    private void sendMessageByByte(byte flag, String message){
        final int DEFAULT_PK_LENGTH = 912;
        byte[] data = new byte[914];
//        String listString = array.toString();
        byte[] MsgByte = message.getBytes();
        Log.d(TAG,"sendMessageByByte listbyte.length = " +MsgByte.length);
        int MsgLength = MsgByte.length;
        if(MsgLength > DEFAULT_PK_LENGTH){
            int packsum = MsgLength / DEFAULT_PK_LENGTH ;
            int rand = MsgLength % DEFAULT_PK_LENGTH;
            packsum = rand > 0 ? packsum+1: packsum;

            for (int i = 0; i< packsum; i++){
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(i == packsum - 1){
                    int lastLength = MsgLength -(i*DEFAULT_PK_LENGTH);
                    data = new byte[lastLength+2];
                    data[0] = flag;
                    data[1] = BluetoothCmd.LAST_PACKAGE;
                    System.arraycopy(MsgByte, i*DEFAULT_PK_LENGTH, data, 2, lastLength);
                    Log.d(TAG,"sendMessageByByte LAST_PACKAGE : "+data[0] );
                }else {
                    data[0] = flag;
                    data[1] = BluetoothCmd.CONTINUE_PACKAGE;
                    System.arraycopy(MsgByte, i * DEFAULT_PK_LENGTH, data, 2, DEFAULT_PK_LENGTH);
                    Log.d(TAG,"sendMessageByByte CONTINUE_PACKAGE :" +data[0] );
                }
                sendMessages(data);
            }
        }else {
            data = new byte[MsgLength+2];
            data[0] = flag;
            data[1] = BluetoothCmd.LAST_PACKAGE;
            System.arraycopy(MsgByte, 0, data, 2, MsgLength);
            Log.d(TAG,"sendMessageByByte LAST_PACKAGE  data.length = " +data.length);
            sendMessages(data);
        }
    }

    /**
     * 热点开关是否打开
     * @return
     */
    public boolean isWifiApEnabled() {
        try {
            Method method = wifiManager.getClass().getMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifiManager);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void sendWifiApState(byte command){
        boolean isApon = isWifiApEnabled();
        String ssid = null;
        String passWord = null;
        int keyType = 0;
        try {
            Method getWifiApMethod = wifiManager.getClass().getMethod("getWifiApConfiguration");
            getWifiApMethod.setAccessible(true);
            WifiConfiguration apConfig = (WifiConfiguration) getWifiApMethod.invoke(wifiManager);
            ssid = apConfig.SSID;
            passWord = apConfig.preSharedKey;
            keyType = apConfig.allowedKeyManagement.length();
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] data = new byte[2];
        if(ssid != null){
            data = new byte[3+ssid.length()];
            if(passWord != null){
                data = new byte[3+ ssid.length()+ passWord.length()];
            }
        }

        data[0] = command;// BluetoothCmd.COMMAND_GET_WIFI_AP_STATE;
        data[1] = isApon ? BluetoothCmd.COMMAND_WIFI_AP_STATE_ON: BluetoothCmd.COMMAND_WIFI_AP_STATE_OFF;
        if(ssid != null){
            data[2] = (byte)ssid.length();
            byte[] ssidByte = ssid.getBytes();
            System.arraycopy(ssidByte, 0, data, 3, ssidByte.length);
            if(passWord != null){
                byte[] pwByte = passWord.getBytes();
                System.arraycopy(pwByte, 0, data, 3+ssid.length(), pwByte.length);
            }
        }
        sendMessages(data);
    }

    private boolean isSetupWifiAP = false;
    private void changeWifiAPInfo(Message msg){
        byte[] data = (byte[])msg.obj;
        int dataLen = msg.arg1;
        boolean setOn = data[1] == BluetoothCmd.COMMAND_WIFI_AP_STATE_ON;
        boolean isSuccess = false;
        Log.i(TAG,"changeWifiAPinfo datalen: "+ dataLen);
        if(dataLen == 2) {
          //  isSuccess = setWifiApEnabled(setOn);
        }else {
            if(dataLen >2){
                int ssidLen = data[2];
                String ssid = new String(data, 3, ssidLen);
                if(dataLen > (3+ssidLen)){
                    String pw = new String(data, 3+ssidLen, dataLen - (3+ssidLen));
                    Log.d(TAG,"changeWifiAPinfo :  ssid :"+ ssid+ "  pw:"+pw);

                    isSuccess = setUpWifiApEnabled(ssid, pw, true, true);
                }else {
                    Log.d(TAG,"changeWifiAPinfo :  ssid :"+ ssid);
                  //  setWifiApEnabled(false);
                    isSuccess = setUpWifiApEnabled(ssid, null, false, true);
                }
            }
        }
        isSetupWifiAP = isSuccess;
        boolean isApon = isWifiApEnabled();

        if(!isSetupWifiAP){
            byte[] successData = new byte[2];
            successData[0] = BluetoothCmd.COMMAND_SET_UP_WIFI_AP_INFO;
            successData[1] = BluetoothCmd.SEND_ERROR;
            sendMessages(successData);
        }else if(!isApon && isSetupWifiAP){
            setWifiApEnabled(true);
        }else if(isApon && isSetupWifiAP){
            setWifiApEnabled(false);
        }

    }

    private void changeWifiAPstate(Message msg){
        byte[] data = (byte[])msg.obj;
        int dataLen = msg.arg1;
        boolean setOn = data[1] == BluetoothCmd.COMMAND_WIFI_AP_STATE_ON;
        boolean isSuccess = setWifiApEnabled(setOn);
    }

    // wifi热点开关
    public boolean setWifiApEnabled(boolean enabled) {
        if (enabled) { // disable WiFi in any case
            //wifi和热点不能同时打开，所以打开热点的时候需要关闭wifi
            wifiManager.setWifiEnabled(false);
        }
        boolean needPw = false;
            try {
                /*
                //热点的配置类
                WifiConfiguration apConfig = new WifiConfiguration();
                //配置热点的名称(可以在名字后面加点随机数什么的)
                apConfig.SSID = "BT_123";// "BT_chen";
                //配置热点的密码
                apConfig.preSharedKey= needPw ? "123456789" : null;//"\"123456789\"";
                // apConfig.
                //安全：WifiConfiguration.KeyMgmt.WPA_PSK = 4
                //apConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                apConfig.allowedKeyManagement.set(needPw ? 4 :WifiConfiguration.KeyMgmt.NONE);//WifiConfiguration.KeyMgmt.WPA_PSK
                apConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                apConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                apConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                apConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                apConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                //*/
            Method getWifiApMethod = wifiManager.getClass().getMethod("getWifiApConfiguration");
            getWifiApMethod.setAccessible(true);
            WifiConfiguration apConfig = (WifiConfiguration) getWifiApMethod.invoke(wifiManager);
             Log.d(TAG,"setWifiApEnabled wifi ap  ssid: "+ apConfig.SSID+ "  key :" + apConfig.preSharedKey);
            //    Log.i(TAG," allowedKeyManagement:"+apConfig.allowedKeyManagement.toString() +" wpk2:"+apConfig.allowedKeyManagement.length());
            //通过反射调用设置热点
            Method method = wifiManager.getClass().getMethod(
                    "setWifiApEnabled", WifiConfiguration.class, boolean.class);//Boolean.TYPE);
            //返回热点打开状态
            return (Boolean) method.invoke(wifiManager, apConfig, enabled);
        } catch (Exception e) {
            Log.w(TAG,"exeep: " +e);
            e.printStackTrace();
            return false;
        }
    }
    /**set up wifi ap and open */
    public boolean setUpWifiApEnabled(String ssid, String password, boolean needPw, boolean enabled) {
        if (enabled) { // disable WiFi in any case
            //wifi和热点不能同时打开，所以打开热点的时候需要关闭wifi
            wifiManager.setWifiEnabled(false);
        }
      //  boolean needPw = false;
        try {
            //*
            //热点的配置类
            WifiConfiguration apConfig = new WifiConfiguration();
            //配置热点的名称(可以在名字后面加点随机数什么的)
            apConfig.SSID = ssid;// "BT_chen";
            //配置热点的密码
            apConfig.preSharedKey= needPw ? password : null;//"\"123456789\"";
            // apConfig.
            //安全：WifiConfiguration.KeyMgmt.WPA_PSK = 4
            //apConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            apConfig.allowedKeyManagement.set(needPw ? 4 :WifiConfiguration.KeyMgmt.NONE);//WifiConfiguration.KeyMgmt.WPA_PSK
            apConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            apConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            apConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            apConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            apConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            //*/
            Method getWifiApMethod = wifiManager.getClass().getMethod("getWifiApConfiguration");
            getWifiApMethod.setAccessible(true);
            //WifiConfiguration apConfig = (WifiConfiguration) getWifiApMethod.invoke(wifiManager);
            Log.i(TAG,"setUpWifiApEnabled ap  ssid: "+ apConfig.SSID+ "  key :" + apConfig.preSharedKey);
           // Log.i(TAG," allowedKeyManagement:"+apConfig.allowedKeyManagement.toString() +" wpk2:"+apConfig.allowedKeyManagement.length());
            //通过反射调用设置热点
            Method method = wifiManager.getClass().getMethod(
            "setWifiApEnabled", WifiConfiguration.class, boolean.class);//Boolean.TYPE);

            Method method2 = wifiManager.getClass().getMethod(
                    "setWifiApConfiguration", WifiConfiguration.class);//Boolean.TYPE);

            return   (Boolean) method2.invoke(wifiManager, apConfig);
            //返回热点打开状态
         //return  (Boolean) method.invoke(wifiManager, apConfig, true);
        } catch (Exception e) {
            Log.w(TAG,"exeep: " +e);
            e.printStackTrace();
            return false;
        }
    }
/**
 * 需要系统权限：android.permission.MODIFY_PHONE_STATE
 * */
    public boolean setMobileDataEnabled(boolean enabled) {
        final TelephonyManager mTelManager;
        mTelManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Method m = mTelManager.getClass().getDeclaredMethod("getITelephony");
            m.setAccessible(true);
            Object telephony = m.invoke(mTelManager);
            m = telephony.getClass().getMethod((enabled ? "enable" : "disable") + "DataConnectivity");
            return (Boolean) m.invoke(telephony);
          //  return true;
        } catch (Exception e) {
            Log.e("", "cannot fake telephony", e);
            e.printStackTrace();
            return false;
        }
    }

    public boolean getMobileDataStatus()
    {
        ConnectivityManager cm;
        cm = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
        Class cmClass = cm.getClass();
        Class[] argClasses = null;
        Object[] argObject = null;
        Boolean isOpen = false;
        try{
            Method method = cmClass.getMethod("getMobileDataEnabled", argClasses);
            isOpen = (Boolean)method.invoke(cm, argObject);
        }catch(Exception e)
        {
            e.printStackTrace();
        }
        return isOpen;
    }


    private void setMobileDataEnabledS(boolean enabled) {
        Log.d(TAG,"liuhao setMobileDataEnabledS");
        TelephonyManager telephonyService = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Method setMobileDataEnabledMethod = telephonyService.getClass()
                    .getDeclaredMethod("setDataEnabled", boolean.class);
            if (null != setMobileDataEnabledMethod) {
                setMobileDataEnabledMethod.invoke(telephonyService,
                        enabled);
            }
        } catch (Exception e) {
            Log.e("InstallActivity", "Errot setting"
                    + ((InvocationTargetException) e).getTargetException()
                    + telephonyService);
        }
    }
    /**
     * set data enabled
     * */
    public void setDataConnectionState(boolean state) {
        ConnectivityManager connectivityManager = null;
       // Class connectivityManagerClz = null;
        try {
            connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);//"connectivity"
            //connectivityManagerClz = connectivityManager.getClass();
            Method method = connectivityManager.getClass()
                    .getMethod("setMobileDataEnabled", new Class[] { boolean.class });
            method.invoke(connectivityManager, state);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 返回手机移动数据的状态
     *
     * @param pContext
     * @param arg      默认填null
     * @return true 连接 false 未连接
     */
    public static int getMobileDataState(Context pContext, Object[] arg) {
        int state = -1;
        TelephonyManager telMgr = (TelephonyManager)
                pContext.getSystemService(Context.TELEPHONY_SERVICE);
        int simState = telMgr.getSimState();
        switch (simState) {
            case TelephonyManager.SIM_STATE_ABSENT:
                state = BluetoothCmd.COMMAND_DATA_STATE_NO_SIM; // 没有SIM卡
                break;
            case TelephonyManager.SIM_STATE_UNKNOWN:
                state = BluetoothCmd.COMMAND_DATA_STATE_NO_SIM;
                break;
        }
        if(state == -1){
            try {
                ConnectivityManager mConnectivityManager = (ConnectivityManager) pContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                Class ownerClass = mConnectivityManager.getClass();
                Class[] argsClass = null;
                if (arg != null) {
                    argsClass = new Class[1];
                    argsClass[0] = arg.getClass();
                }
                Method method = ownerClass.getMethod("getMobileDataEnabled", argsClass);
                Boolean isOpen = (Boolean) method.invoke(mConnectivityManager, arg);
                if (isOpen) {
                    state = BluetoothCmd.COMMAND_DATA_STATE_ON;
                } else {
                    state = BluetoothCmd.COMMAND_DATA_STATE_OFF;
                }
            } catch (Exception e) {
                Log.d(TAG, "得到移动数据状态出错");
            }
        }
        return state;
    }

    private void checkReadMessage(Message msg){
        byte[] data = (byte[]) msg.obj;
        byte[] readBuf = (byte[]) msg.obj;
        // construct a string from the valid bytes in the buffer
        String readMessage = new String(readBuf, 0, msg.arg1);
        int command = data[0];
        Log.i(TAG,"checkReadMessage command : " + command   );
        switch (command){
            //data
            case BluetoothCmd.COMMAND_DATA_STATE:
                boolean setDataOn = data[1] == BluetoothCmd.COMMAND_DATA_STATE_ON;
                Log.d(TAG,"liuhao setDataOn:"+setDataOn);
                setMobileDataEnabled(setDataOn);
                Log.d(TAG,"liuhao getMobileEnables:"+getMobileDataStatus());
                if(getMobileDataStatus()){
                    sendDataInfo(BluetoothCmd.COMMAND_DATA_STATE_ON);
                }else{
                    sendDataInfo(BluetoothCmd.COMMAND_DATA_STATE_OFF);
                }
                //                setMobileDataEnabled(setDataOn);
//                setDataConnectionState(setDataOn);
                break;
            /////wifi
            case BluetoothCmd.COMMAND_SCAN_WIFI:
                sendWifiScanList();
                break;
            case BluetoothCmd.COMMAND_LINK_WIFI:
                setLinkWifi(msg);
                break;
            case BluetoothCmd.COMMAND_WIFI_INFO:
                sendWifState();
                break;
            case BluetoothCmd.COMMAND_CHANGE_WIFI_STATE:
                int state = data[1];
                if(state == BluetoothCmd.COMMAND_CLOSE_WIFI){
                    mWifiAdmin.closeWifi(this);
                }else if(state == BluetoothCmd.COMMAND_OPEN_WIFI){
                    mWifiAdmin.openWifi(this);
                }
                break;
            //wifi ap
            case BluetoothCmd.COMMAND_GET_WIFI_AP_STATE:
                sendWifiApState(BluetoothCmd.COMMAND_GET_WIFI_AP_STATE);
                break;
            case BluetoothCmd.COMMAND_CHANGE_WIFI_AP_STATE:
                changeWifiAPstate(msg);
                break;
            case BluetoothCmd.COMMAND_SET_UP_WIFI_AP_INFO:
                changeWifiAPInfo(msg);
                break;
            //contacts
            case BluetoothCmd.COMMAND_QUERY_CONTACTS_LIST:
                sendContactsList();
                break;
            case BluetoothCmd.COMMAND_INSERT_CONTACT:
                insertContact(msg);
                break;
            case BluetoothCmd.COMMAND_DELETE_CONTACT:
                deleteContact(msg);
                break;
            case BluetoothCmd.COMMAND_MODIFY_CONTACT:
                modifyContact(msg);
                break;
            case BluetoothCmd.COMMAND_MAKE_CALL_CONTACT:
                String number = new String(readBuf, 1, msg.arg1 - 1);
                Log.i(TAG,"COMMAND_MAKE_CALL_CONTACT number ="+number);
                //用intent启动拨打电话
                Intent intent = new Intent(Intent.ACTION_CALL,Uri.parse("tel:"+number));
                if(PhoneNumberUtils.isEmergencyNumber(number)){
                    //CALL_EMERGENCY need system app permission :android.permission.CALL_PRIVILEGED
                   // intent = new Intent("android.intent.action.CALL_EMERGENCY", Uri.parse("tel:"+number));
                }

                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                int checkCallPhonePermission = checkSelfPermission( Manifest.permission.CALL_PHONE);
                TelecomManager tm = (TelecomManager)getApplicationContext().getSystemService(Context.TELECOM_SERVICE);
                boolean isincall = tm.isInCall();
                Log.i(TAG,"isincall ="+isincall +"  checkCallPhonePermission ="+checkCallPhonePermission);
                if (checkCallPhonePermission == PackageManager.PERMISSION_GRANTED && !isincall)
                    startActivity(intent);
                break;
            //ringtone
            case BluetoothCmd.COMMAND_GET_RINGTONE_LIST:
                sendRingtoneList();
                break;
            case BluetoothCmd.COMMAND_SET_RINGTONE:
                setRingTone(msg);
                break;
            case BluetoothCmd.COMMAND_PLAY_RINGTONE:
                playSelectRingtone(msg);
                break;
            case BluetoothCmd.COMMAND_STOP_RINGTONE:
                stopAnyPlayingRingtone();
                break;
            //mms
            case BluetoothCmd.COMMAND_QUERY_CONTACTS_MMS:
                PowerTransmissionSendSmsList(msg);
                break;
            case BluetoothCmd.COMMAND_QUERY_SINGLE_CONTACT_MMS:
                sendSingleSmsList(msg);

                break;
            case BluetoothCmd.COMMAND_CHANGE_READ_STATUS_MMS:

                break;
            case BluetoothCmd.COMMAND_SEND_MMS:

                break;
            //sim card
            case BluetoothCmd.COMMAND_SEND_SIM_CARD_INFO:
                sendSimCardInfo();
                break;
            case BluetoothCmd.BATTERY_LEVEL:
                if (mBatteryLevel != -1) {
                    sendBatteryInfo();
                }
                break;
            default:
                Log.i(TAG,"default other command");
                break;
        }
    }


    /**
     * send WiFi状态*/
    private void sendWifState(){
        mWifiAdmin = new WifiAdmin(getApplication());
        int state = mWifiAdmin.getWifiState();
        String ssid = null;
        String address = null;
        boolean contected = mWifiAdmin.isWifiConnected(this);
        if(state == WifiManager.WIFI_STATE_ENABLED && contected){
            ssid = mWifiAdmin.getSSID();
            ssid = ssid.replace("\"","");
            address = mWifiAdmin.getWifiAddress();
            Log.i(TAG,"liuhao wifi info : " + ssid+" ssid.length:"+ssid.length()
                    +" address:"+address+" address.length():"+address.length());
        }
        byte[] data = null;
        if(ssid == null){
            data = new byte[3];
        }else {
            if(address == null){
                data = new byte[4+ ssid.length()];
            }else{
                data = new byte[4+ ssid.length()+address.length()];
            }

        }

        data[0] = BluetoothCmd.COMMAND_WIFI_INFO;
        Log.d(TAG,"liuhao datalength:"+data.length);
        switch (state){
            case WifiManager.WIFI_STATE_DISABLED:
                data[1] = BluetoothCmd.WIFI_STATE_DISABLED;
                break;
            case WifiManager.WIFI_STATE_DISABLING:
                data[1] = BluetoothCmd.WIFI_STATE_DISABLING;
                break;
            case WifiManager.WIFI_STATE_ENABLED:
                data[1] = BluetoothCmd.WIFI_STATE_ENABLED;
                break;
            case WifiManager.WIFI_STATE_ENABLING:
                data[1] = BluetoothCmd.WIFI_STATE_ENABLING;
                break;
            default:
                data[1] = BluetoothCmd.WIFI_STATE_UNKNOWN;
                break;
        }
        data[2] = contected ? BluetoothCmd.WIFI_CONTECTED : BluetoothCmd.WIFI_NOT_CONTECTED;

        if(ssid != null){
            byte[] ssidbyte = ssid.getBytes();
            int ssidLength = ssidbyte.length;
            data[3] = (byte) ssidLength;
            System.arraycopy(ssidbyte, 0, data, 4, ssidbyte.length);

            if(address != null){
                byte[] addressbyte = address.getBytes();
                System.arraycopy(addressbyte, 0, data, 4+ssidbyte.length, addressbyte.length);
            }
        }
        Log.d(TAG,"liuhao data:"+new String(data));
        sendMessages(data);

    }
    /**send contected wifi state */
    private void sendWifContectedState(String ssid){
        int state = mWifiAdmin.getWifiState();
        String address = null;
        boolean contected = mWifiAdmin.isWifiConnected(this);
        ssid = ssid.replace("\"","");
        Log.i(TAG,"sendWifContectedState wifi info : " + ssid +"  contected = "+contected);

        address = mWifiAdmin.getWifiAddress();

        byte[] data = null;
        if(ssid == null){
            data = new byte[3];
        }else {
            if(address == null){
                data = new byte[4+ ssid.length()];
            }else{
                data = new byte[4+ ssid.length()+address.length()];
            }

        }
        data[0] = BluetoothCmd.COMMAND_WIFI_INFO;
        data[1] = BluetoothCmd.WIFI_STATE_ENABLED;
        data[2] = BluetoothCmd.WIFI_CONTECTED;
        //Log.d(TAG,"sendwifiState state = " + state + "  contected = " + contected);
        if(ssid != null){
            byte[] ssidbyte = ssid.getBytes();
            int ssidLength = ssidbyte.length;
            data[3] = (byte) ssidLength;
            System.arraycopy(ssidbyte, 0, data, 4, ssidbyte.length);

            if(address != null){
                byte[] addressbyte = address.getBytes();
                System.arraycopy(addressbyte, 0, data, 4+ssidbyte.length, addressbyte.length);
            }
        }
        sendMessages(data);
    }

    /**配置WiFi*/
    private void setLinkWifi(Message msg){
        byte[] data = (byte[])msg.obj;
        byte ssidLen = data[1];
        byte[] ssidbyte = new byte[ssidLen];
        System.arraycopy(data, 3, ssidbyte, 0, ssidLen);
        byte pwLen = data[2];
        byte[] pwbyte = new byte[pwLen];
        System.arraycopy(data, 3+ssidLen, pwbyte,0, pwLen);
        String ssid = new String(ssidbyte);
        String pw = new String(pwbyte);
        wifiBasicInfo = new WifiBasicInfo(ssid, pw);
        Log.i(TAG,"setLinkWifi ssid : " + ssid+" , pw : " + pw);
        int state = mWifiAdmin.getWifiState();

        if(state == WifiManager.WIFI_STATE_ENABLED){
            isSetWifiLink = true;
            mWifiAdmin.addNetwork(mWifiAdmin.CreateWifiInfo(wifiBasicInfo.getSsid(), wifiBasicInfo.getPassWord(),3));
            Log.i(TAG,"setLinkWifi wifi info : " );
        }else {
            isSetWifiCommand = true;
            mWifiAdmin.openWifi(this);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                WifiInfo wifiInfo = mWifiAdmin.getWifiInfos();
                String  ssid = wifiInfo.getSSID();
                ssid = ssid.replace("\"","");
                Log.i(TAG,"ssid = "+ ssid);
                Log.d(TAG,"wifi info ssid = " + wifiBasicInfo.getSsid()  + " , flag = " +ssid.equals(wifiBasicInfo.getSsid()) );
                byte[] data = new byte[2];
                data[0] = BluetoothCmd.COMMAND_LINK_WIFI;
                if(ssid != null && ssid.equals(wifiBasicInfo.getSsid())){
                    data[1] = BluetoothCmd.SEND_ACK;
                    Log.i(TAG,"setLinkWifi SEND_ACK");
                }else {
                    data[1] = BluetoothCmd.SEND_ERROR;
                    Log.i(TAG,"setLinkWifi SEND_ERROR");
                }
                sendMessages(data);
            }
        }, 4000);
    }

    /**
     * send 获取service扫描的WiFi信息列表
     * */
    private void sendWifiScanList(){
        mWifiAdmin.startScan(this);
        List<ScanResult> list = mWifiAdmin.getWifiList();
        Log.i(TAG,"sendwifilist list = " + list);

        JSONArray array = new JSONArray();
        for(ScanResult con : list){
            JSONObject ob = new JSONObject();
            Log.i(TAG,"sendwifiscanlist ssid = "+ con.SSID+",  level = " + con.level);
            try {
                ob.put("ssid",con.SSID);
                ob.put("level",con.level);
                array.put(ob);
            } catch (JSONException e){
                e.printStackTrace();
            }
        }
        String listdata = array.toString();
        sendMessageByByte(BluetoothCmd.COMMAND_SCAN_WIFI, listdata);
    }

    ////////////////////////////////Contacts//////////////////////////
    /**
     * query contacts list
     * */
    private void sendContactsList(){
        List<Contacts> contactsList = ContactsUtil.getLocalContacts(this);
        JSONArray array = new JSONArray();
        for(Contacts con : contactsList){
            JSONObject ob = new JSONObject();
            try {
                ob.put("id", con.getId());
                ob.put("name",con.getName());
                ob.put("phonenumber",con.getPhoneNumber());
                array.put(ob);
            } catch (JSONException e){
                e.printStackTrace();
            }
        }
        String listString = array.toString();
        sendMessageByByte(BluetoothCmd.COMMAND_QUERY_CONTACTS_LIST, listString);
    }

    /**insert contact*/
    private void insertContact(Message msg){
        byte[] data = (byte[])msg.obj;
        byte nameLen = data[1];
        byte phoneLen = data[2];
        byte[] nameByte = new byte[nameLen];
        byte[] phoneByte = new byte[phoneLen];
        System.arraycopy(data, 3, nameByte, 0, nameLen);
        System.arraycopy(data, 3+nameLen, phoneByte, 0, phoneLen);
        String name = new String(nameByte);
        String phone = new String(phoneByte);
        Log.d(TAG,"checkReadMessage COMMAND_INSERT_CONTACT name: " + name+" , phone: " +phone);
        boolean result = false;
        if(name.length()<1 && phone.length()<1){
            result = false;
        }else {
            result = ContactsUtil.insert(this, name, phone);
        }
        if(result){
            Log.i(TAG,"COMMAND_INSERT_CONTACT ok");
            byte[] conOk = new byte[2];
            conOk[0] = BluetoothCmd.COMMAND_INSERT_CONTACT;
            conOk[1] = BluetoothCmd.SEND_ACK;
            sendMessages(conOk);
            sendContactsList();
        }else {
            byte[] conFaild = new byte[2];
            conFaild[0] = BluetoothCmd.COMMAND_INSERT_CONTACT;
            conFaild[1] = BluetoothCmd.SEND_ERROR;
            sendMessages(conFaild);
            Log.i(TAG,"COMMAND_INSERT_CONTACT fiald");
        }
    }

    /**
     * delete contact
     * */
    private void deleteContact(Message msg){
        byte[] data = (byte[])msg.obj;
        byte idLen = data[1];
        byte[] idByte = new byte[idLen];
        System.arraycopy(data, 2, idByte, 0, idLen);
        String id = new String(idByte);
        Log.i(TAG,"COMMAND_DELETE_CONTACT id = "+ id);
        boolean result = ContactsUtil.deleteContact(this,id);
        if(result){
            Log.i(TAG,"COMMAND_INSERT_CONTACT ok");
            byte[] conOk = new byte[2];
            conOk[0] = BluetoothCmd.COMMAND_DELETE_CONTACT;
            conOk[1] = BluetoothCmd.SEND_ACK;
            sendMessages(conOk);
            sendContactsList();
        }else {
            byte[] conFaild = new byte[2];
            conFaild[0] = BluetoothCmd.COMMAND_DELETE_CONTACT;
            conFaild[1] = BluetoothCmd.SEND_ERROR;
            sendMessages(conFaild);
            Log.i(TAG,"COMMAND_INSERT_CONTACT fiald");
        }
    }

    /**modify contact*/
    private void modifyContact(Message msg){
        byte[] data = (byte[])msg.obj;
        byte nameLen = data[1];
        byte phoneLen = data[2];
        byte idLen = data[3];
        byte[] nameByte = new byte[nameLen];
        byte[] phoneByte = new byte[phoneLen];
        byte[] idByte = new byte[idLen];
        System.arraycopy(data, 4, nameByte, 0, nameLen);
        System.arraycopy(data, 4+nameLen, phoneByte, 0, phoneLen);
        System.arraycopy(data, 4+nameLen+phoneLen, idByte, 0, idLen);
        String name = new String(nameByte);
        String phone = new String(phoneByte);
        String id = new String(idByte);
        Log.d(TAG,"checkReadMessage COMMAND_INSERT_CONTACT name: " + name+" , phone: " +phone + " id = "+id);
        boolean result = false;
        if(name.length()<1 && phone.length()<1){
            result = false;
        }else {
            result = ContactsUtil.updateContact(this, id, name, phone);
        }
        if(result){
            Log.i(TAG,"COMMAND_MODIFY_CONTACT ok");
            byte[] conOk = new byte[2];
            conOk[0] = BluetoothCmd.COMMAND_MODIFY_CONTACT;
            conOk[1] = BluetoothCmd.SEND_ACK;
            sendMessages(conOk);
            sendContactsList();
        }else {
            byte[] conFaild = new byte[2];
            conFaild[0] = BluetoothCmd.COMMAND_MODIFY_CONTACT;
            conFaild[1] = BluetoothCmd.SEND_ERROR;
            sendMessages(conFaild);
            Log.i(TAG,"COMMAND_MODIFY_CONTACT fiald");
        }
    }
/////////////ringtone//////////
    private void sendRingtoneList(){
        List<String> ringtoneList = new ArrayList<>();

        Cursor cursor = mRingtoneManager.getCursor();
        if (cursor.moveToFirst()) {
            do {
                ringtoneList.add(cursor
                        .getString(RingtoneManager.TITLE_COLUMN_INDEX));
            } while (cursor.moveToNext());
        }
      //  cursor.close();
        Log.i(TAG,"sendRingtoneList ringTonelist : "+ ringtoneList.toString());
        Uri ringtoneUri = mRingtoneManager.getActualDefaultRingtoneUri(this , RingtoneManager.TYPE_RINGTONE);
        int currentPosition = mRingtoneManager.getRingtonePosition(ringtoneUri);

        mDefaultRingtonePos = currentPosition;
        Log.i(TAG,"sendRingtoneList position = " +currentPosition);
        String arr =currentPosition + ringtoneList.toString();
        arr = arr.replace("]","");
        arr = arr.replace(" ","");

        sendMessageByByte(BluetoothCmd.COMMAND_GET_RINGTONE_LIST, arr);


    }

    private void setRingTone(Message msg) {
        stopAnyPlayingRingtone();
        byte[] data = (byte[]) msg.obj;
        String readBuff = new String(data, 1, msg.arg1 -1);
//        Log.i(TAG,"setRingTone readBuff ="+ readBuff);
        int position = Integer.parseInt(readBuff);
//        Log.i(TAG,"setRingTone position ="+ position);
        //RingtoneManager rm = new RingtoneManager(this);
        Uri uri = mRingtoneManager.getRingtoneUri(position);
//        Log.d(TAG,"setRingTone uri = " + uri);
        RingtoneManager.setActualDefaultRingtoneUri(this,
                RingtoneManager.TYPE_RINGTONE, uri);
        Uri ringtoneUri = mRingtoneManager.getActualDefaultRingtoneUri(this , RingtoneManager.TYPE_RINGTONE);
        int current = mRingtoneManager.getRingtonePosition(ringtoneUri);
//        Log.i(TAG,"setRingTone position ="+ position +" ,current = "+current);
        byte[] command = new byte[2];
        command[0] = BluetoothCmd.COMMAND_MODIFY_CONTACT;
        if(current == position){
            command[1] = BluetoothCmd.SEND_ACK;
        }else {
            command[1] = BluetoothCmd.SEND_ERROR;
        }
        sendMessages(command);
    }

    private void playSelectRingtone(Message msg){
        byte[] data = (byte[]) msg.obj;
        String readBuff = new String(data, 1, msg.arg1 -1);
        Log.i(TAG,"playSelectRingtone readBuff ="+ readBuff);
        int position = Integer.parseInt(readBuff);
       // setRingTone(msg);
        Log.i(TAG,"playSelectRingtone position ="+ position);
        playRingtone(position, 0);
    }

    private void sendCurrentRingTone(){
        RingtoneManager rm = new RingtoneManager(this);
        Uri ringtoneUri = rm.getActualDefaultRingtoneUri(this , RingtoneManager.TYPE_RINGTONE);
        int position = rm.getRingtonePosition(ringtoneUri);
        Log.i(TAG,"sendCurrentRingTone position = " +position);
    }

    private RingtoneManager mRingtoneManager;
    private static final int POS_UNKNOWN = -1;
    private int mSilentPos = POS_UNKNOWN;
    private int mDefaultRingtonePos = POS_UNKNOWN;
    private Ringtone mDefaultRingtone;
    private static Ringtone sPlayingRingtone;
    private int mSampleRingtonePos = POS_UNKNOWN;
    private Uri mUriForDefaultItem = Settings.System.DEFAULT_RINGTONE_URI;
    private Handler mRingToneHandler;

    private void stopAnyPlayingRingtone() {
        if (sPlayingRingtone != null && sPlayingRingtone.isPlaying()) {
            sPlayingRingtone.stop();
        }
        sPlayingRingtone = null;

        if (mDefaultRingtone != null && mDefaultRingtone.isPlaying()) {
            mDefaultRingtone.stop();
        }

        if (mRingtoneManager != null) {
            mRingtoneManager.stopPreviousRingtone();
        }
    }

    private void playRingtone(int position, int delayMs) {
        mRingToneHandler.removeCallbacks(this);
        mSampleRingtonePos = position;
        mRingToneHandler.postDelayed(this, delayMs);
    }

    @Override
    public void run() {
        stopAnyPlayingRingtone();
        if (mSampleRingtonePos == mSilentPos) {
            return;
        }

        Ringtone ringtone;
        if (mSampleRingtonePos == mDefaultRingtonePos) {
            if (mDefaultRingtone == null) {
                mDefaultRingtone = RingtoneManager.getRingtone(this, mUriForDefaultItem);
            }
           /*
            * Stream type of mDefaultRingtone is not set explicitly here.
            * It should be set in accordance with mRingtoneManager of this Activity.
            */
            if (mDefaultRingtone != null) {
                mDefaultRingtone.setStreamType(mRingtoneManager.inferStreamType());
            }
            ringtone = mDefaultRingtone;

            sPlayingRingtone = ringtone;
        } else {
            try {
                Uri uri = mRingtoneManager.getRingtoneUri(mSampleRingtonePos);

                Log.i(TAG,"Utir = "+ uri );
               // ringtone = RingtoneManager.getRingtone(this, uri);
               ringtone =mRingtoneManager.getRingtone(mSampleRingtonePos);
                if (ringtone != null) {
                    ringtone.setStreamType(mRingtoneManager.inferStreamType());
                }
            } catch (StaleDataException staleDataException) {
                ringtone = null;
            } catch (IllegalStateException illegalStateException) {
                ringtone = null;
            }
            sPlayingRingtone = ringtone;
        }

        Log.i(TAG,"Run ringtone = " + ringtone.toString());
          mDefaultRingtone = RingtoneManager.getRingtone(this, mUriForDefaultItem);
        Log.i(TAG,"Run mDefaultRingtone = " + mDefaultRingtone.toString());

        if (sPlayingRingtone != null) {
            sPlayingRingtone.play();
        }
    }

    ////////////mms and sms///////////////////////////
    /////////////Sms//////////

    private void setSmsListData(){
        mSmsInfoList = smsUtil.showSmsList();//传输总数据
        mSmsInfoLength = mSmsInfoList.size();
        malreadySmsInfoLength = 1;//当为头部数据时是第一次刷新
        mListSizeToTransferLength = mSmsInfoLength/SOCKETTRANSFERLENGTH + (mSmsInfoLength%SOCKETTRANSFERLENGTH > 0 ? 1 : 0);//传输的次数
        Log.d(TAG,"liuhao size:"+mSmsInfoLength+" :"+" sizelength:"+mListSizeToTransferLength);
    }
    private void setSmsLength(){
        malreadySmsInfoLength = smsUtil.getSmsCount();
        mListSizeToTransferLength = mSmsInfoLength/SOCKETTRANSFERLENGTH + (mSmsInfoLength%SOCKETTRANSFERLENGTH > 0 ? 1 : 0);//传输的次数
    }
    private void PowerTransmissionSendSmsList(Message msg){
        byte[] data = (byte[]) msg.obj;
        byte ifFlash = data[1];
        if(ifFlash == BluetoothCmd.COMMAND_CONTINUE_RESET_STATUS_MMS){
            malreadySmsInfoLength = 1;//重置为1
            mListSizeToTransferLength = 1;
            mSmsInfoList = null;//重置为null
            smsUtil.closeCursor();
            return;
        }
        if(ifFlash == BluetoothCmd.COMMAND_REQUEST_FLASH_STATUS_MMS){
            Log.d(TAG,"liuhao ifFlash ==");
            setSmsListData();
        }
        Log.d(TAG,"liuhao3 ifFlash:"+ifFlash+" :"+malreadySmsInfoLength+"/"+mListSizeToTransferLength);
        List<SmsInfo> currentSmsInfo;//当前要传输过去的list
        if(mSmsInfoLength >= SOCKETTRANSFERLENGTH){
            //当数据总长度大于20条时
            if(ifFlash == BluetoothCmd.COMMAND_REQUEST_FLASH_STATUS_MMS){
//                setSmsListData();//传输总数据
                Log.d(TAG,"liuhao4 if1:"+(mSmsInfoList.size() >= SOCKETTRANSFERLENGTH)
                        +" if2:"+ifFlash+" if3:"+mListSizeToTransferLength);
                //                Log.d(TAG,"liuhao head---"+0*SOCKETTRANSFERLENGTH+
                //                        ":"+SOCKETTRANSFERLENGTH+" "+mSmsInfoLength);
                currentSmsInfo = mSmsInfoList.subList(0*SOCKETTRANSFERLENGTH,SOCKETTRANSFERLENGTH);
                sendSmsList(currentSmsInfo,-1,malreadySmsInfoLength+"/"+mListSizeToTransferLength);
                malreadySmsInfoLength++;
            }else if(ifFlash == BluetoothCmd.COMMAND_CONTINUE_FLASH_STATUS_MMS){
                if(mListSizeToTransferLength > malreadySmsInfoLength){//当前要发送的数据次数小于总数据长度
                    //                    Log.d(TAG,"liuhao"+(malreadySmsInfoLength-1)*SOCKETTRANSFERLENGTH+
                    //                            ":"+((malreadySmsInfoLength-1)*SOCKETTRANSFERLENGTH+20)+" "+mSmsInfoLength);
                    currentSmsInfo = mSmsInfoList.subList((malreadySmsInfoLength-1)*SOCKETTRANSFERLENGTH,
                            (malreadySmsInfoLength-1)*SOCKETTRANSFERLENGTH+20);
                    sendSmsList(currentSmsInfo,0,malreadySmsInfoLength+"/"+mListSizeToTransferLength);
                    malreadySmsInfoLength++;
                }else if(mListSizeToTransferLength == malreadySmsInfoLength){//当前要发送数据次数是最后一次，也就说尾部数据
                    currentSmsInfo = mSmsInfoList.subList((malreadySmsInfoLength-1)*SOCKETTRANSFERLENGTH,
                            mSmsInfoList.size());
                    sendSmsList(currentSmsInfo,1,malreadySmsInfoLength+"/"+mListSizeToTransferLength);
                    malreadySmsInfoLength = 1;
                }
            }

        }else{//当数据总长度小于20条时，直接传送全部list
//            setSmsListData();//传输总数据
            malreadySmsInfoLength = 1;//当为头部数据时是第一次刷新
            sendSmsList(mSmsInfoList, 1,"");
        }

    }

    /**
     *
     * @param mSmsInfoList
     * @param isHead isHead 为 -1 时是头部，0是中间部分，1是尾部
     * @param isHead 进度显示。格式为 "4/14"
     */
    private void sendSmsList(List<SmsInfo> mSmsInfoList, int isHead, String progress){
        JSONArray array = new JSONArray();
        for(SmsInfo con : mSmsInfoList){
            JSONObject ob = new JSONObject();
            try {
                ob.put("address", con.getAddress());
                ob.put("body",con.getBody());
                ob.put("date",con.getDate());
                ob.put("person",con.getPerson());
                ob.put("type",con.getType());
                ob.put("thread_id",con.getThread_id());
                ob.put("_id",con.get_id());
                ob.put("read",con.getRead());
                //isHead 为 -1 时是头部，0是中间部分，1是尾部
                ob.put("isHead",isHead);
                ob.put("progress",progress);
                array.put(ob);
            } catch (JSONException e){
                e.printStackTrace();
            }
        }
        String listString = array.toString();
        Log.d(TAG,"liuhao listString:"+listString);
        sendMessageByByte( BluetoothCmd.COMMAND_QUERY_CONTACTS_MMS, listString);
    }


    /////////////SingleSms//////////
    private void sendSingleSmsList(Message msg){
        byte[] data1 = (byte[]) msg.obj;
        String readBuff = new String(data1, 2, msg.arg1 -2);
        Log.i(TAG,"liu readBuff ="+ readBuff);
        int thread_id = Integer.parseInt(readBuff);
        List<SmsInfo> contactsList = smsUtil.getSingleSmsInPhone(thread_id);
        JSONArray array = new JSONArray();
        for(SmsInfo con : contactsList){
            JSONObject ob = new JSONObject();
            try {
                ob.put("address", con.getAddress());
                ob.put("body",con.getBody());
                ob.put("date",con.getDate());
                ob.put("person",con.getPerson());
                ob.put("type",con.getType());
                ob.put("thread_id",con.getThread_id());
                ob.put("_id",con.get_id());
                ob.put("read",con.getRead());

                ob.put("isHead",-1);
                ob.put("progress","");
                array.put(ob);
            } catch (JSONException e){
                e.printStackTrace();
            }
        }
        String listString = array.toString();
        sendMessageByByte(BluetoothCmd.COMMAND_QUERY_SINGLE_CONTACT_MMS , listString);
    }

/////////sim 卡//////////////////////////////////////
    private static final int NETWORKTYPE_WIFI = 4;
    private static final int NETWORKTYPE_4G = 3;
    private static final int NETWORKTYPE_3G = 2;
    private static final int NETWORKTYPE_2G = 1;
    private static final int NETWORKTYPE_NONE = 0;
    private static final int NETWORK_TYPE_UNAVAILABLE = -1;
    // private static final int NETWORK_TYPE_MOBILE = -100;
    private static final int NETWORK_TYPE_WIFI = -101;

    private static final int NETWORK_CLASS_WIFI = -101;
    private static final int NETWORK_CLASS_UNAVAILABLE = -1;
    private String preSimName = "";
    private int preLevel = -1;
    private int preNetworkType = 0;

    private class PhoneStatListener extends PhoneStateListener {
        //获取信号强度
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);
          // final TelephonyManager tm = (TelephonyManager)Context.getSystemService(Context.TELEPHONY_SERVICE);
            //是否有卡
            boolean cardReady = insideSimCarad();
            if(cardReady){
                //运营商名字
                String simName = mTelephonyManager.getSimOperatorName();
                //获取网络信号强度
                //获取0-4的5种信号级别，越大信号越好,但是api23开始才能用
                int level = 0;
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    level = signalStrength.getLevel();
                }else {
                    level = getSignalLevel(signalStrength);
                }
               // int gsmSignalStrength = signalStrength.getGsmSignalStrength();
                //获取网络类型 2,3,4G
                int netWorkType = getNetWorkType(getApplication());
                int type = mobileNetworkType(getApplication());
                if(preLevel != level || preNetworkType != type || !preSimName.equals(simName)){
                    preLevel = level;
                    preNetworkType = type;
                    preSimName = simName;
                    sendSimCardInfo(true, simName, type, level);
                }
                Log.i(TAG, "PhoneStatListener name :" + simName+"  level:" +level+"  networktype:"+netWorkType+"  type :"+type);
            }else {
                Log.i(TAG,"PhoneStatListener cardready = false");
                sendSimCardInfo(false, preSimName, preNetworkType, preLevel);
            }
        }
    }

    private boolean insideSimCarad(){
        int simState = mTelephonyManager.getSimState();
        boolean cardReady = false;
        if(simState == TelephonyManager.SIM_STATE_ABSENT || simState == TelephonyManager.SIM_STATE_UNKNOWN ){
            cardReady = false;
        }else {
            cardReady = true;
        }
        return cardReady;
    }

    public static int getNetWorkType(Context context) {
        int mNetWorkType = -1;
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            String type = networkInfo.getTypeName();
            if (type.equalsIgnoreCase("WIFI")) {
                mNetWorkType = NETWORKTYPE_WIFI;
            } else if (type.equalsIgnoreCase("MOBILE")) {
                return mobileNetworkType(context);
            }
        } else {
            mNetWorkType = NETWORKTYPE_NONE;//没有网络
        }
        return mNetWorkType;
    }
    /**判断网络类型*/
    private static boolean isFastMobileNetwork(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_LTE) {
//这里只简单区分两种类型网络，认为4G网络为快速，但最终还需要参考信号值
            return true;
        }
        return false;
    }

    /**判断网络类型*/
    private static int mobileNetworkType(Context context) {
        TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        int networkType = tm.getNetworkType();
        switch (networkType) {
            case NETWORK_TYPE_UNAVAILABLE:
                return NETWORK_CLASS_UNAVAILABLE;
            case NETWORK_TYPE_WIFI:
                return NETWORK_CLASS_WIFI;
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return NETWORKTYPE_2G;
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return NETWORKTYPE_3G;
            case TelephonyManager.NETWORK_TYPE_LTE:
                return NETWORKTYPE_4G;
            default:
                return NETWORKTYPE_NONE;
        }
    }

    //获取信号强度
    private int getSignalLevel(SignalStrength signalStrength){
        int level = 0;
        try {
            Method levelMethod = SignalStrength.class.getDeclaredMethod("getLevel");
            level = (Integer) levelMethod.invoke(signalStrength);
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
        return level;
    }

    //send sim card info
    private void sendSimCardInfo(){
        boolean simcardReady = insideSimCarad();
        int type = mobileNetworkType(getApplication());
        String simName = mTelephonyManager.getSimOperatorName();
        sendSimCardInfo(simcardReady, simName, type, preLevel);
    }

    private void sendSimCardInfo(boolean simcardReady, String simName, int type, int level){
        byte[] types = {BluetoothCmd.COMMAND_SIM_NETWORK_TYPE_NONE, BluetoothCmd.COMMAND_SIM_NETWORK_TYPE_2G,
                BluetoothCmd.COMMAND_SIM_NETWORK_TYPE_3G,BluetoothCmd.COMMAND_SIM_NETWORK_TYPE_4G};
        byte[] levels = {BluetoothCmd.COMMAND_SIM_SIGNAL_LEVEL0, BluetoothCmd.COMMAND_SIM_SIGNAL_LEVEL1,
                BluetoothCmd.COMMAND_SIM_SIGNAL_LEVEL2, BluetoothCmd.COMMAND_SIM_SIGNAL_LEVEL3,
                BluetoothCmd.COMMAND_SIM_SIGNAL_LEVEL4,};
        byte[] data ;
        if(simcardReady){
            byte[] simNameByte = simName.getBytes();
            int simNameLen = simNameByte.length;
            data = new byte[4 + simNameLen];
            data[0] = BluetoothCmd.COMMAND_SEND_SIM_CARD_INFO;
            data[1] = BluetoothCmd.COMMAND_SIM_CARD_READY;
            data[2] = types[type];
            data[3] = levels[level];
            System.arraycopy(simNameByte, 0, data, 4, simNameLen);
            sendMessages(data);
        }else {
            data = new byte[2];
            data[0] = BluetoothCmd.COMMAND_SEND_SIM_CARD_INFO;
            data[1] = BluetoothCmd.COMMAND_SIM_CARD_NOT_READY;
            sendMessages(data);
        }

    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }




}
