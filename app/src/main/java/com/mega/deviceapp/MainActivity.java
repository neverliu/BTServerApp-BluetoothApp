package com.mega.deviceapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mega.deviceapp.activity.ContactsActivity;
import com.mega.deviceapp.activity.SearchBlueToothActivity;
import com.mega.deviceapp.activity.SmsActivity;
import com.mega.deviceapp.activity.WifiAPConfigureActivity;
import com.mega.deviceapp.activity.WifiConfigureActivity;
import com.mega.deviceapp.adapter.CommonAdapter;
import com.mega.deviceapp.adapter.ViewHolder;
import com.mega.deviceapp.alexa.LoginWithAmazonActivity;
import com.mega.deviceapp.model.Contacts;
import com.mega.deviceapp.model.WifiBasicInfo;
import com.mega.deviceapp.service.BluetoothChatService;
import com.mega.deviceapp.service.BluetoothSocketService;
import com.mega.deviceapp.util.BluetoothCmd;
import com.mega.deviceapp.util.ContactsUtil;
import com.mega.deviceapp.util.MessageCode;
import com.mega.deviceapp.util.ToastUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MainActivity";
    private Toolbar toolbar;
    private View v_container;
    private List<Contacts> mContactsList;
    private List<Contacts> mMsgContacts;
    private MyAdapter myAdapter;
    private MyAdapter mMsgAdapter;

    private BluetoothAdapter ba;
    private BluetoothChatService mChatService = null;

    private String mConnectedDeviceName;
    private String mReceivedMsg = "";

    public final static int CODE_SEARCH_BLUETOOTH = 101;

    private ProgressDialog mLoadingDialog;

    private Intent mIntent;
    private Messenger mMessage , sMessage;

    private ImageButton mBtnBT;
    private ImageButton mBtnWiFi;
    private TextView mBTContectView;
    private TextView mWifiContectView;
    private TextView mBatteryTv;
    private TextView mdataTv;

    private int mChatserviceState = -1;

    private TextView mSimCardText;
    String mDeviceAddress;
    private SharedPreferences sp;
    private String SPNAME = "Bluetooth_sp";
    private final String SP_KEY_DEVICE_ADDRESS = "device_address";
    boolean firstStart = true;

    private int mDataStatus = -1;

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
            if(firstStart && mDeviceAddress != null){
                tryConnectToDevice(mDeviceAddress);
                firstStart = false;
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG,"onServiceDisconnected");
            sMessage = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mLoadingDialog = new ProgressDialog(this);
        mLoadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mLoadingDialog.setMessage("正在备份");
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        v_container = findViewById(R.id.coordinatorlayout);
        mBTContectView = (TextView) findViewById(R.id.bluetooth_matched_id);
        mWifiContectView = (TextView) findViewById(R.id.wifi_connected_id);
        mBatteryTv = (TextView) findViewById(R.id.battery_tv);
        mBatteryTv.setOnClickListener(new View.OnClickListener(){
              @Override
              public void onClick(View v) {
                  updateBatteryStatus();
              }
        });


       // toolbar.setSubtitle(R.string.main_noconnect);
        setSupportActionBar(toolbar);
        //initFab();
        sp = getSharedPreferences(SPNAME, MODE_PRIVATE);
        firstStart = true;
        mDeviceAddress = sp.getString(SP_KEY_DEVICE_ADDRESS, null);

        //mListView = (ListView) findViewById(R.id.listView_contacts);
        //mListView.setVisibility(View.GONE);
        checkReadPermission();
        ba = BluetoothAdapter.getDefaultAdapter();
        mBtnBT = (ImageButton) findViewById(R.id.bluetooth_id);
        mBtnBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinkToDevice();
            }
        });
        mBtnWiFi = (ImageButton) findViewById(R.id.wifi_id);
        mBtnWiFi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               Intent intent = new Intent(MainActivity.this, WifiConfigureActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
        mSimCardText = (TextView) findViewById(R.id.simcard_tv);
        findViewById(R.id.simcard_tv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message message = new Message();
                message.what = MessageCode.MESSAGE_WRITE_TO_SERVICE;
                byte[] command = new byte[2];
                command[0] = BluetoothCmd.COMMAND_SEND_SIM_CARD_INFO;
                message.obj = command;
                try {
                    sMessage.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
        mdataTv = (TextView) findViewById(R.id.data_tv);
        mdataTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int dataStatus = -1;
//                setDataUI(mDataStatus);
                if(mDataStatus == -1 || mDataStatus == BluetoothCmd.COMMAND_DATA_STATE_NO_SIM){

                    //数据是打开状态，点击需要关闭
                }else if(mDataStatus == BluetoothCmd.COMMAND_DATA_STATE_ON){
                    dataStatus = BluetoothCmd.COMMAND_DATA_STATE_OFF;

                    //数据是关闭状态，点击需要打开
                }else if(mDataStatus == BluetoothCmd.COMMAND_DATA_STATE_OFF){
                    dataStatus = BluetoothCmd.COMMAND_DATA_STATE_ON;
                }
                Message message = new Message();
                message.what = MessageCode.MESSAGE_WRITE_TO_SERVICE;
                byte[] command = new byte[2];
                command[0] = BluetoothCmd.COMMAND_DATA_STATE;
                command[1] = (byte) dataStatus;
                message.obj = command;
                try {
                    sMessage.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        findViewById(R.id.contacts_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ContactsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        findViewById(R.id.ringtone_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message message = new Message();
                message.what = MessageCode.MESSAGE_WRITE_TO_SERVICE;
                byte[] command = new byte[2];
                command[0] = BluetoothCmd.COMMAND_GET_RINGTONE_LIST;
                message.obj = command;
                try {
                    sMessage.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
        findViewById(R.id.send_msg).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SmsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
        findViewById(R.id.wifiap_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, WifiAPConfigureActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
        findViewById(R.id.wifiap_btn).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Message message = new Message();
                message.what = MessageCode.MESSAGE_WRITE_TO_SERVICE;
                byte[] command = new byte[2];
                command[0] = BluetoothCmd.COMMAND_GET_WIFI_AP_STATE;
                message.obj = command;
                try {
                    sMessage.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });

        findViewById(R.id.login_alexa_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LoginWithAmazonActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });


        mMessage = new Messenger(mHandler);
        mIntent = new Intent(this, BluetoothSocketService.class);
        if (!ba.isEnabled()){
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, 1);
        }else {
            startService(mIntent);
            bindService(mIntent, mConnection, BIND_AUTO_CREATE);
        }
    }
    /**
     *  ListView has been set gone
     */
    @Deprecated
    private void initListView(){
        /*
        mContactsList = ContactsUtil.getLocalContacts(this);
        Collections.sort(mContactsList, new Comparator<Contacts>(){
            @Override
            public int compare(Contacts lhs, Contacts rhs){
                return lhs.getName().compareTo(rhs.getName());
            }
        });
        setAdapter();
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                mContactsList.get(position).changeChecked();
                myAdapter.notifyDataSetChanged();
            }
        });
        */
    }

    private void checkReadPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int checkReadContactsPermission = checkSelfPermission( Manifest.permission.READ_CONTACTS);
            Log.i("MainActivity","checkperdmissons ");
            if (checkReadContactsPermission != PackageManager.PERMISSION_GRANTED) {
                Log.i("    ","requestPermissions");
                requestPermissions( new String[]{Manifest.permission.CALL_PHONE,
                        Manifest.permission.WRITE_CONTACTS ,
                        Manifest.permission.ACCESS_COARSE_LOCATION ,
                        Manifest.permission.READ_SMS}, 1);
            }else {
                initListView();
            }

        }else {
            initListView();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i(" ","onRequestPermissionsResult requestCode = " + requestCode);
        if(requestCode == 1 || requestCode == 2){
            if(permissions[0].equals(Manifest.permission.READ_CONTACTS) &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED){
                initListView();
            }
        }
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MessageCode.MESSAGE_STATE_CHANGE:
                    mChatserviceState = msg.arg1;
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            //setStatus(1,mConnectedDeviceName,"连接到  " + mConnectedDeviceName);
                            setStatus(getString(R.string.main_connected_to)+ mConnectedDeviceName);
                            sp.edit().putString(SP_KEY_DEVICE_ADDRESS, mDeviceAddress).apply();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.main_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            updateDisConnectText();

                            break;
                    }
                    break;
                case MessageCode.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    checkReadMessage(msg);
                    break;
                case MessageCode.MESSAGE_DEVICE_NAME:
                    mConnectedDeviceName = msg.getData().getString(MessageCode.DEVICE_NAME);

                    break;
                case MessageCode.MESSAGE_TOAST:
                    if (null != this){
                        ToastUtil.showMsg(MainActivity.this,msg.getData().getString(MessageCode.TOAST));
                    }
                    break;
                case MessageCode.MESSAGE_GET_SOCKET_STATE:
                    Log.i(TAG,"MESSAGE_GET_SOCKET_STATE");
                    int state = (int)msg.obj;
                    if(state == BluetoothChatService.STATE_LISTEN ||
                            state == BluetoothChatService.STATE_NONE){
                        updateDisConnectText();
                    }
                    break;
            }
        }
    };

    private void updateDisConnectText(){
        setStatus(R.string.main_noconnect);
        mWifiContectView.setText(R.string.main_noconnect);
        mSimCardText.setText(R.string.sim_card_info_text);
        mBatteryTv.setText(R.string.battery);
        mBatteryTv.setCompoundDrawablesWithIntrinsicBounds(null,
                getResources().getDrawable(R.mipmap.ic_battery_half), null, null);

        mdataTv.setText(R.string.data);
        mdataTv.setCompoundDrawablesWithIntrinsicBounds(null,
                getResources().getDrawable(R.mipmap.data), null, null);
    }

    private void checkReadMessage(Message msg){
        byte[] readBuff = (byte[]) msg.obj;
        int command = readBuff[0];
        switch (command){
            case BluetoothCmd.COMMAND_SCAN_WIFI:
                decoderWifis(msg);
                break;
            case BluetoothCmd.COMMAND_WIFI_INFO:
                setWifiStatus(msg);
                break;
            case BluetoothCmd.COMMAND_GET_RINGTONE_LIST:
                getRingtoneList(msg);
                break;
            case BluetoothCmd.COMMAND_SET_RINGTONE:
                if(readBuff[1] == BluetoothCmd.SEND_ACK){
                    ToastUtil.showMsg(this,R.string.main_ring_success);
                }else if(readBuff[1] == BluetoothCmd.SEND_ERROR){
                    ToastUtil.showMsg(this,R.string.main_ring_fail);
                }
                break;
            case BluetoothCmd.COMMAND_GET_WIFI_AP_STATE:
                int on = readBuff[1];
                showChangeApDialog(on);
                break;
            case BluetoothCmd.COMMAND_CHANGE_WIFI_AP_STATE:
                int success = readBuff[1];
                if(success == BluetoothCmd.COMMAND_WIFI_AP_STATE_ON){
                    ToastUtil.showMsg(this,R.string.main_wifi_ap_open_msg);
                }else if(success == BluetoothCmd.COMMAND_WIFI_AP_STATE_OFF) {
                    ToastUtil.showMsg(this,R.string.main_wifi_ap_close_msg);
                }else if(success == BluetoothCmd.SEND_ERROR){
                    ToastUtil.showMsg(this,R.string.main_wifi_ap_fail_msg);
                }
                break;
            case BluetoothCmd.COMMAND_SEND_SIM_CARD_INFO:
                readSimCardInfo(msg);
                break;
           case BluetoothCmd.BATTERY_LEVEL:
                Log.d(TAG,"liuhao BATTERY_LEVEL");
                setBatteryUI(msg);
                break;
            case BluetoothCmd.COMMAND_DATA_STATE:
                mDataStatus = readBuff[1];
                Log.d(TAG,"liuhao COMMAND_DATA_STATE :"+ mDataStatus);
                setDataUI(mDataStatus);

                break;
            default:
                // byte[] readBuf = (byte[]) msg.obj;
                String readMessage = new String(readBuff, 0, msg.arg1);
                Log.i(TAG,"checkReadMessage other msg : " + readMessage);
                break;
        }
    }

    private void readSimCardInfo(Message msg){
        byte[] data = (byte[]) msg.obj;
        boolean simCardReady = data[1] == BluetoothCmd.COMMAND_SIM_CARD_READY;
        if(simCardReady){
            byte typeByte = data[2];

            String type ;
            switch (typeByte){
                case BluetoothCmd.COMMAND_SIM_NETWORK_TYPE_2G:
                    type = getString(R.string.sim_network_type_2g);
                    break;
                case BluetoothCmd.COMMAND_SIM_NETWORK_TYPE_3G:
                    type = getString(R.string.sim_network_type_3g);
                    break;
                case BluetoothCmd.COMMAND_SIM_NETWORK_TYPE_4G:
                    type = getString(R.string.sim_network_type_4g);
                    break;
                default:
                    type = "";
                    break;
            }
            byte levelByte = data[3];
            Drawable drawable;
            switch (levelByte){
                case BluetoothCmd.COMMAND_SIM_SIGNAL_LEVEL0:
                    drawable = getResources().getDrawable(R.mipmap.signal_state_0);
                    break;
                case BluetoothCmd.COMMAND_SIM_SIGNAL_LEVEL1:
                    drawable = getResources().getDrawable(R.mipmap.signal_state_1);
                    break;
                case BluetoothCmd.COMMAND_SIM_SIGNAL_LEVEL2:
                    drawable = getResources().getDrawable(R.mipmap.signal_state_2);
                    break;
                case BluetoothCmd.COMMAND_SIM_SIGNAL_LEVEL3:
                    drawable = getResources().getDrawable(R.mipmap.signal_state_3);
                    break;
                case BluetoothCmd.COMMAND_SIM_SIGNAL_LEVEL4:
                    drawable = getResources().getDrawable(R.mipmap.signal_state_4);
                    break;
                default:
                    drawable = getResources().getDrawable(R.mipmap.signal_state_0);
                    break;
            }
            if(typeByte == BluetoothCmd.COMMAND_SIM_NETWORK_TYPE_NONE){
                drawable = getResources().getDrawable(R.mipmap.signal_state_0);
            }
            Log.d(TAG,"readSimCardinfo type:"+type+" level:"+ levelByte+" typebyte :"+typeByte);
            drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
            mSimCardText.setCompoundDrawables(null,drawable,null,null);
            String simName = new String(data,4,msg.arg1-4);
            mSimCardText.setText(simName+" "+ type);
        }else {
            Drawable drawable1= getResources().getDrawable(R.mipmap.no_simcard);
            /// 这一步必须要做,否则不会显示.
            drawable1.setBounds(0, 0, drawable1.getMinimumWidth(), drawable1.getMinimumHeight());
            mSimCardText.setCompoundDrawables(null,drawable1,null,null);
            mSimCardText.setText(getString(R.string.sim_card_no_sim));
        }
    }


    public StringBuffer mStrBuff = new StringBuffer();
    private void decoderWifis(Message msg){
        byte[] readBuff = (byte[]) msg.obj;
        String readMessage = new String(readBuff, 2, msg.arg1 - 2);
        if(readBuff[1] == BluetoothCmd.CONTINUE_PACKAGE){
            mStrBuff.append(readMessage);
        }else if (readBuff[1] == BluetoothCmd.LAST_PACKAGE){
            mStrBuff.append(readMessage);
            getWifisFromMsg(mStrBuff.toString());
            mStrBuff = new StringBuffer();
        }
    }
    private void setBatteryUI(Message msg){
        byte[] readBuff = (byte[]) msg.obj;
        try {
            int status = readBuff[1];
            int  level = readBuff[2];
            int scale = readBuff[3];
            Log.d(TAG," setBatteryUI status:"+status+",LEVEL="+level+",scale="+scale);
            int intLevel = level * 100 / scale;
            Drawable topDrawable = null;
            if(intLevel <= 15){
                topDrawable = getResources().getDrawable(R.mipmap.ic_battery_low);
            }else if(intLevel > 15 && intLevel < 95){
                topDrawable = getResources().getDrawable(R.mipmap.ic_battery_half);
            }else if(intLevel >= 95){
                topDrawable = getResources().getDrawable(R.mipmap.ic_battery_full);
            }


            if(status == BatteryManager.BATTERY_STATUS_CHARGING){
                String data = String.format(getResources().getString(R.string.battery_charging),intLevel);
                mBatteryTv.setText(data);
            }else{
                String data = String.format(getResources().getString(R.string.battery_current),intLevel);
                mBatteryTv.setText(data);
            }

            if(topDrawable!=null){
//            topDrawable.setBounds(topDrawable.getMinimumWidth(), 0, 0, 0);
                mBatteryTv.setCompoundDrawablesWithIntrinsicBounds(null, topDrawable, null, null);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private void setDataUI(int dataStatus){
        Drawable topDrawable = null;
        if(dataStatus==BluetoothCmd.COMMAND_DATA_STATE_ON){
            topDrawable = getResources().getDrawable(R.mipmap.data_open);
            mdataTv.setText(getResources().getString(R.string.data_open));

        }else if(dataStatus==BluetoothCmd.COMMAND_DATA_STATE_OFF){
            topDrawable = getResources().getDrawable(R.mipmap.data_close);
            mdataTv.setText(getResources().getString(R.string.data_close));
        }else if(dataStatus==BluetoothCmd.COMMAND_DATA_STATE_NO_SIM){
            topDrawable = getResources().getDrawable(R.mipmap.data_close);
            mdataTv.setText(getResources().getString(R.string.data));
        }
        if(topDrawable!=null){
            //            topDrawable.setBounds(topDrawable.getMinimumWidth(), 0, 0, 0);
            mdataTv.setCompoundDrawablesWithIntrinsicBounds(null, topDrawable, null, null);
        }
    }

    private void setWifiStatus(Message msg){
        byte[] readBuff = (byte[]) msg.obj;
        int statebyte = readBuff[1];
        int contectedbyte = readBuff[2];
        Log.d(TAG,"liuhao state = " + statebyte+"   contected = "+contectedbyte + " readBuff.length:"+readBuff.length+" msg.arg1:"+msg.arg1);
        String SSID = null;
        String ADDRESS = null;
        if(msg.arg1 > 3){
            int ssidLength = readBuff[3];
            Log.d(TAG,"liuhao ssidLength:"+ssidLength);
            byte[] ssidData = new byte[ssidLength];
            System.arraycopy(readBuff, 4, ssidData, 0, ssidLength);
            SSID = new String(ssidData);
            Log.d(TAG,"liuhao SSID:"+SSID);

            if(msg.arg1 > 4 + ssidLength){
                byte[] addressData = new byte[msg.arg1-(4+ssidLength)];
                System.arraycopy(readBuff, (4+ssidLength), addressData, 0, addressData.length);
                ADDRESS = new String(addressData);
                Log.d(TAG,"liuhao ADDRESS:"+ADDRESS);
            }
        }
        if(statebyte == BluetoothCmd.WIFI_STATE_ENABLED){
            if(contectedbyte == BluetoothCmd.WIFI_CONTECTED && SSID != null){
               // mWifiContectView.setText((getString(R.string.main_connected_to)) + SSID);
                String str = (getString(R.string.main_connected_to)) + SSID;
                mWifiContectView.setText(str);
            }else {
                mWifiContectView.setText(R.string.main_noconnect);
            }
        }else {
            mWifiContectView.setText(R.string.main_noconnect);
        }
    }
    private String ListByteToString(List<byte[]> mListByte){
        if(mListByte == null)
            return null;

        int allSize = 0;
        for(byte[] a :mListByte){
            allSize += a.length;
        }
        byte[] data = new byte[allSize];
        int currentSize = 0;
        for(int i = 0 ; i < mListByte.size() ; i++){
            System.arraycopy(mListByte.get(i), 0, data, currentSize, mListByte.get(i).length);
            currentSize += mListByte.get(i).length;
        }
        String dataList = new String(data);

        return dataList;
    }
    private List<byte[]> mRingToneBytelist = new ArrayList<>();

    private void getRingtoneList(Message msg){
        byte[] readBuff = (byte[]) msg.obj;
        String readMessage = new String(readBuff, 2, msg.arg1 - 2);
        int len = msg.arg1 -2;
        byte[] databuff = new byte[len];
        System.arraycopy(readBuff, 2, databuff, 0, len);
        if(readBuff[1] == BluetoothCmd.CONTINUE_PACKAGE){
//            mStrBuff.append(readMessage);
            mRingToneBytelist.add(databuff);
        }else if (readBuff[1] == BluetoothCmd.LAST_PACKAGE){
//            mStrBuff.append(readMessage);
            mRingToneBytelist.add(databuff);
//            String buff = mStrBuff.toString();
            String buff = ListByteToString(mRingToneBytelist);
            String poStr = buff.substring(0, buff.indexOf("["));
            int position = Integer.parseInt(poStr);
            buff = buff.substring(buff.indexOf("[")+1);
            Log.d(TAG,"getRingtoneList position = " + position);
            Log.i(TAG,"getRingtoneList , str = "+ buff);
            mStrBuff = new StringBuffer();
            mRingToneBytelist.clear();
            String[] arr = buff.split(",");
            List<String> list =  Arrays.asList(arr);
            Log.d(TAG,"getRingtoneList list="+list);
            showRingtoneDialog(arr, position);
        }
    }
    private int selectPos = -1;
    private void showRingtoneDialog(final String[] items, final int position){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ringtone");
        builder.setSingleChoiceItems(items, position, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.i(TAG,"onclick  which = " + which);
                //selectPos =
                if(position != which){
                    Log.i(TAG,"onclick  position == which");
                    selectPos = which;
                }
                String name = items[which];
                Log.d(TAG,"onclick  name = " +name);
                byte[] poByte = (which+"").getBytes();
                byte poLen = (byte)poByte.length;

                Message message = new Message();
                message.what = MessageCode.MESSAGE_WRITE_TO_SERVICE;
                byte[] command = new byte[1+poLen];
                command[0] = BluetoothCmd.COMMAND_PLAY_RINGTONE;
                System.arraycopy(poByte, 0, command, 1, poLen);
                message.obj = command;
                try {
                    sMessage.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
        });
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.i(TAG,"ok click which ="+ which);

                Message message = new Message();
                message.what = MessageCode.MESSAGE_WRITE_TO_SERVICE;
                byte[] command;
                if(selectPos == -1){
                    command = new byte[2];
                    command[0] = BluetoothCmd.COMMAND_STOP_RINGTONE;
                }else {
                    byte[] poByte = (selectPos + "").getBytes();
                    byte poLen = (byte) poByte.length;

                    command = new byte[1 + poLen];
                    command[0] = BluetoothCmd.COMMAND_SET_RINGTONE;
                    System.arraycopy(poByte, 0, command, 1, poLen);
                }
                message.obj = command;
                try {
                    sMessage.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Message message = new Message();
                message.what = MessageCode.MESSAGE_WRITE_TO_SERVICE;
                byte[] command = new byte[2];
                command[0] = BluetoothCmd.COMMAND_STOP_RINGTONE;
                message.obj = command;
                try {
                    sMessage.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void showChangeApDialog(int apOn){
        final boolean currentSate = apOn == BluetoothCmd.COMMAND_WIFI_AP_STATE_ON;
        int title = currentSate ? R.string.wifi_ap_close_title: R.string.wifi_ap_open_title;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.i(TAG,"ok click which ="+ which);
                Message message = new Message();
                message.what = MessageCode.MESSAGE_WRITE_TO_SERVICE;
                byte[] command = new byte[2];
                command[0] = BluetoothCmd.COMMAND_CHANGE_WIFI_AP_STATE;
                command[1] = currentSate? BluetoothCmd.COMMAND_WIFI_AP_STATE_OFF: BluetoothCmd.COMMAND_WIFI_AP_STATE_ON;
                message.obj = command;
                try {
                    sMessage.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                dialog.dismiss();
            }});
        builder.setNegativeButton(android.R.string.cancel,null);
        builder.show();
    }

    private void receive(final String msg)
    {
        Snackbar.make(v_container,"您收到一条消息!",Snackbar.LENGTH_INDEFINITE)
                .setAction("查看", new View.OnClickListener(){
            @Override
            public void onClick(View v){
                mReceivedMsg = msg;
                showMessageDialog();
            }
        }).show();
    }



    private void showMessageDialog(){
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v_content = View.inflate(this,R.layout.v_dlg_msg,null);
        dialog.setContentView(v_content);
        ListView listView = (ListView) v_content.findViewById(R.id.listview_dlg);
        mMsgContacts = getContactsFromMsg(mReceivedMsg);
        mMsgAdapter = new MyAdapter(this,mMsgContacts,R.layout.item_list_contacts_msg);
        listView.setAdapter(mMsgAdapter);
        Button btn_copy = (Button) v_content.findViewById(R.id.btn_copy);
        btn_copy.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                dialog.dismiss();
                mLoadingDialog.show();
                new Thread(new Runnable(){
                    @Override
                    public void run() {
                        int count = 0;
                        for(Contacts con:mMsgContacts){
                            boolean result = ContactsUtil.insert(MainActivity.this,con.getName(),con.getPhoneNumber());
                            if(result)
                                count++;
                        }
                        cancelLoadingDialg();
                        if (count==mMsgContacts.size())
                            showSnackBarMsg();

                    }
                }).start();

            }
        });
        dialog.show();
    }

    private void cancelLoadingDialg(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLoadingDialog.dismiss();
            }
        });
    }

    private void showSnackBarMsg() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Snackbar.make(v_container,"备份成功",Snackbar.LENGTH_INDEFINITE).show();
            }
        });
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

    private List<Contacts> getContactsFromMsg(String msg) {
        List<Contacts> _List = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(msg);
            for(int i=0;i<array.length();i++){
                JSONObject ob = array.getJSONObject(i);
                Contacts con = new Contacts(ob.getString("name"),ob.getString("phonenumber"),true);
                _List.add(con);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return _List;
    }

    private void sendMessageByBlueTooth(String msg){
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED){
            ToastUtil.showMsg(this,R.string.main_no_connect_device);
            return;
        }
        if (msg.length() > 0){
            //ToastUtil.showMsg(this,"正在发送...");
            byte[] send = msg.getBytes();
            Log.i("","sendMessage : send = " + send);
            mChatService.write(send);
        }
    }

    private void updateWifiContectStatus(){
        if(sMessage == null){
            return;
        }
        Log.d(TAG,"updateWifiContectStatus");
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

    //发送获取Battery command
    private void updateBatteryStatus(){
        if(sMessage == null){
            return;
        }
        Log.d(TAG,"liuhao updateBatteryStatus");
        Message message = new Message();
        byte[] command = new byte[2];
        message.what = MessageCode.MESSAGE_WRITE_TO_SERVICE;
        command[0] = BluetoothCmd.BATTERY_LEVEL;
        message.obj = command;
        try {
            sMessage.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try{
            unbindService(mConnection);
        }catch (Exception e){

        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if (mChatService != null){
            mChatService.stop();
        }
        stopService(mIntent);
        //unbindService(mConnection);
        mConnection = null;
    }

    @Override
    public void onResume(){
        super.onResume();
        if (!ba.isEnabled()){
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
           // startActivityForResult(enableIntent, 1);
            ba.enable();
        }else {

            if (mIntent != null)
                bindService(mIntent, mConnection, BIND_AUTO_CREATE);
            Log.d(TAG, "onResume mChatService =" + mChatService);
        }
        //update BT contected and Wifi contected text
        updateWifiContectStatus();
        updateSocketStatus();
      /*  if(mChatserviceState == BluetoothChatService.STATE_CONNECTED){
            setStatus(getString(R.string.main_connected_to)+ mConnectedDeviceName);
            updateWifiContectStatus();
        }else {
            setStatus(R.string.main_noconnect);
            mWifiContectView.setText(R.string.main_noconnect);
            mBatteryTv.setText(R.string.battery);
            mBatteryTv.setCompoundDrawablesWithIntrinsicBounds(null,
                    getResources().getDrawable(R.mipmap.ic_battery_half), null, null);

            mdataTv.setText(R.string.data);
            mdataTv.setCompoundDrawablesWithIntrinsicBounds(null,
                    getResources().getDrawable(R.mipmap.data), null, null);
        }*/

        if (mChatService != null){
            if (mChatService.getState() == BluetoothChatService.STATE_NONE){
                mChatService.start();
            }
        }
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
    protected void onStart() {
        super.onStart();

         if (mChatService==null){
               // mChatService = new BluetoothChatService(this,mHandler);
        }
    }

    private void setStatus(int ok,CharSequence Title ,CharSequence subTitle){
        //toolbar.setSubtitle(subTitle);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        if (null == mBTContectView){
            return;
        }
       // toolbar.setSubtitle(subTitle);
        mBTContectView.setText(subTitle);
    }

    private void setStatus(int subTitleResID) {
        if (null == mBTContectView){
            return;
        }
        //toolbar.setSubtitle(subTitleResID);
        mBTContectView.setText(subTitleResID);

    }


    public void openBlueTooth(){
        if (!ba.isEnabled()){
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent,0);
        }
    }
    private void  setBlueToothVisible(){
        Intent intent = new Intent(BluetoothAdapter.
                    ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(intent, 0);
    }

    private void setAdapter() {
        myAdapter = new MyAdapter(this,mContactsList, R.layout.item_list_contacts);
        //mListView.setAdapter(myAdapter);
    }

    /*
    private void initFab(){
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                sendPhoneNumbers();
            }
        });
    }*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.action_select_all).setVisible(false);
        menu.findItem(R.id.action_cancel_all).setVisible(false);
        menu.findItem(R.id.action_bluetooth_visible).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_bluetooth_visible){
            setBlueToothVisible();
            return true;
        }else if(id==R.id.action_new_bluetooth){
            LinkToDevice();
            return true;
        }else if(id==R.id.action_select_all){
            selectAll();
            return true;
        }else if(id==R.id.action_cancel_all){
            cancelAll();
            return true;
        }else if(id==R.id.action_bluetooth_disable){
            ba.disable();
        }
        return super.onOptionsItemSelected(item);
    }
    private void LinkToDevice(){
        if(!ba.isEnabled())
            openBlueTooth();
        else{
            Intent intent = new Intent(this, SearchBlueToothActivity.class);
            startActivityForResult(intent,CODE_SEARCH_BLUETOOTH);
        }
    }

    private void sendPhoneNumbers(){
        boolean flag = false;
        if(mContactsList == null){
            showMessage("没有联系人");
            return;
        }
        for(Contacts con:mContactsList){
            if (con.getIsChecked())
                flag = true;
        }
        if(flag){
            getNumbersAndSend();
        }else{
            showMessage("没有号码被选中");
        }
    }

    /**
     * 得到被选中的号码并发送
     */
    private void getNumbersAndSend() {
        JSONArray array = new JSONArray();
        for(Contacts con:mContactsList){
            if(con.getIsChecked()){
                JSONObject ob = new JSONObject();
                try {
                    ob.put("name",con.getName());
                    ob.put("phonenumber",con.getPhoneNumber());
                    array.put(ob);
                } catch (JSONException e){
                    e.printStackTrace();
                }
            }
        }
        sendMessageByBlueTooth(array.toString());
    }

    private void showMessage(String msg){
        View view = findViewById(R.id.coordinatorlayout);
        Snackbar.make(view, msg, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    /**
     * 取消全部选择
     */
    private void cancelAll() {
        for(Contacts con:mContactsList){
            con.setChecked(false);
        }
        myAdapter.notifyDataSetChanged();
    }

    /**
     * 选择全部号码
     */
    private void selectAll(){
        for(Contacts con:mContactsList){
            con.setChecked(true);
        }
        myAdapter.notifyDataSetChanged();
    }

    private class MyAdapter extends CommonAdapter<Contacts>{
        public MyAdapter(Context context, List<Contacts> list, int layoutId){
            super(context, list, layoutId);
        }
        @Override
        public void convert(ViewHolder holder, final Contacts contacts,int position){
            holder.setText(R.id.tv_item_contacts_name,contacts.getName());
            holder.setText(R.id.tv_item_contacts_phonenumber,contacts.getPhoneNumber());
            final CheckBox box = holder.getView(R.id.checkbox);
            final int pos = position;
            box.setChecked(contacts.getIsChecked());
            box.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    mContactsList.get(pos).changeChecked();
                }
            });

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        Log.i(TAG,"onActivityResult requestCode ="+requestCode);
        if(requestCode==CODE_SEARCH_BLUETOOTH){
            if(resultCode== Activity.RESULT_OK){
                String address = data.getStringExtra("address");
                mDeviceAddress = address;
                tryConnectToDevice(address);
            }
        }else if(requestCode == 1){
            if(resultCode == Activity.RESULT_OK){
                Intent intent = new Intent(this,MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              //  startActivity(intent);
                if(mIntent != null){
                    startService(mIntent);
                    bindService(mIntent, mConnection, BIND_AUTO_CREATE);
                }
            }else {
                finish();
            }
        }
    }

    private void tryConnectToDevice(String address){
        Message message = new Message();
        message.what = MessageCode.MESSAGE_CONNECT;
        // message.replyTo = mMessage;
        Bundle bundle = new Bundle();
        bundle.putString("address",address);
        message.setData(bundle);
        try {
            sMessage.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        //connectToDevice(address);
    }


    private void serviceMessage(int flag, Object object){
        //Message
    }
    /**
     * 连接到其他蓝牙设备
     * @param address
     */
    private void connectToDevice(String address){
        BluetoothDevice device = ba.getRemoteDevice(address);
        if(device!=null){
            mConnectedDeviceName = device.getName();
            mChatService.connect(device,true);
        }
    }
}
