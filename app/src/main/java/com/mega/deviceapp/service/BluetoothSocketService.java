package com.mega.deviceapp.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.mega.deviceapp.util.MessageCode;
import com.mega.deviceapp.util.ToastUtil;

public class BluetoothSocketService extends Service {

    private String TAG = "BluetoothSocketService";
    boolean D = true;
    private String mConnectedDeviceName;
    private Messenger mMessage, cMessage;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothChatService mChatService = null;
    private int mState = 0;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //cMessage = msg.replyTo;
            Log.i(TAG,"handleMessage msg.what = " +msg.what);
            switch (msg.what) {
                case MessageCode.MESSAGE_BIND_SERVICE:
                    Log.i(TAG,"MESSAGE_BIND_SERVICE");
                    cMessage = msg.replyTo;
                    Message message = new Message();
                    message.what = MessageCode.MESSAGE_GET_SOCKET_STATE;
                    message.obj = mState;
                    Log.i(TAG,"socketserver cMessage = "+ cMessage);
                    if(cMessage != null)
                        try {
                            cMessage.send(message);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    break;
                case MessageCode.MESSAGE_STATE_CHANGE:
                    Message msg1 = new Message();
                    msg1.copyFrom(msg);
                    try {
                        if(cMessage != null)
                        cMessage.send(msg1);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    mState = msg.arg1;

                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            //setStatus(1,mConnectedDeviceName,"连接到  " + mConnectedDeviceName);
                            //ToastUtil.showMsg(getApplicationContext(), "Connected to  " + mConnectedDeviceName);
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            //setStatus("连接中");
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            //setStatus("无连接");
                            break;
                    }
                    break;
                case MessageCode.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    //receive(readMessage);
                  //  Log.i(TAG,"MESSAGE_READ read[0] = " + readBuf[0]);
                    Log.i(TAG,"MESSAGE_READ readmsg : " + readMessage);
                    Message readMsg = new Message();
                    readMsg.copyFrom(msg);
                    try {
                        if(cMessage != null)
                        cMessage.send(readMsg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case MessageCode.MESSAGE_DEVICE_NAME:
                    Message nameMsg = new Message();
                    nameMsg.copyFrom(msg);
                    try {
                        if(cMessage != null)
                        cMessage.send(nameMsg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    mConnectedDeviceName = msg.getData().getString(MessageCode.DEVICE_NAME);
                    break;
                case MessageCode.MESSAGE_TOAST:
                        ToastUtil.showMsg(getApplicationContext(),msg.getData().getString(MessageCode.TOAST));
                    break;
                case MessageCode.MESSAGE_WRITE_TO_SERVICE:
                    sendMessageByBlueTooth((byte[]) msg.obj);
                    break;
                case MessageCode.MESSAGE_WRITE:

                    Log.i(TAG,"write to service sueess : " + (byte[])msg.obj);
                    break;
                case MessageCode.MESSAGE_CONNECT:
                    String address = msg.getData().getString("address");
                    connectToDevice(address);
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
                       startChat();
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
                        //Toast.makeText(context,"蓝牙关闭，请打开蓝牙。 ", Toast.LENGTH_LONG).show();
                        if(mChatService != null){
                            //mChatService.stop();
                            if(D)Log.d(TAG,"mReceiver STATE_TURNING_OFF");
                        }
                        break;
                }
            }
        }
    };


    public BluetoothSocketService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        setup();
        startChat();
        return mMessage.getBinder();
       // throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG,"onCreate");
        mMessage = new Messenger(mHandler);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.setPriority(1000);//设置优先级
        registerReceiver(mReceiver, intentFilter);
        setup();
        startChat();
    }

    private void setup(){
        if(mChatService == null){
            mChatService = new BluetoothChatService(this,mHandler);
        }
        if(mBluetoothAdapter == null){
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
    }

    private void startChat(){
        if (mChatService != null){
            if (mChatService.getState() == BluetoothChatService.STATE_NONE){
                mChatService.start();
            }
        }
    }

    private int getState(){
        if(mChatService != null){
            return mChatService.getState();
        }
        return 0;
    }

    /**
     * 连接到其他蓝牙设备
     * @param address
     */
    private void connectToDevice(String address){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if(device!=null){
            mConnectedDeviceName = device.getName();
            mChatService.connect(device,true);
        }
    }

    private void sendMessageByBlueTooth(byte[] msg){
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED){
            //ToastUtil.showMsg(this,"Disconnected to device");
            return;
        }
        if (msg.length> 0){
           // ToastUtil.showMsg(this,"正在发送...");
        //    byte[] send = msg.getBytes();
            Log.d(TAG,"sendMessage : send = " + new String(msg));
            mChatService.write(msg);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mChatService != null){
            mChatService.stop();
        }
        unregisterReceiver(mReceiver);
    }
}
