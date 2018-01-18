package com.mega.btserverapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.mega.btserverapp.service.BluetoothChatService;
import com.mega.btserverapp.service.BluetoothControlService;

public class OpenBluetoothReceiver extends BroadcastReceiver {
    private String TAG = "OpenBluetoothReceiver";
    private static final boolean D = true;
    final private String ACTION_REOPEN_SERVICE = "action.reopen.bluetoothservice";
    private Context mContext;
    private final int Time = 15 * 1000;
    Handler mHandler = new Handler();
    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            startService(mContext);
        }
    };
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        String action = intent.getAction();
        mContext = context;
        Log.e(TAG,"action = " + action);
        if(action.equals("android.intent.action.BOOT_COMPLETED")){
            startService(context);
        }else if(action.equals(ACTION_REOPEN_SERVICE)){
            startService(context);
        }else if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
            if(D)Log.i(TAG,"isBluetooth on = " + isBluetoothOn(context));
            int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
            if(D)Log.i(TAG,"blueState = " + blueState);
            switch (blueState) {
                case BluetoothAdapter.STATE_ON:
                    Intent intent2 = new Intent(context, BluetoothControlService.class);
                    context.startService(intent2);
                    break;
            }
            if(isBluetoothOn(context)){
                Intent intent2 = new Intent(context, BluetoothControlService.class);
                context.startService(intent2);
            }
        }
    }
    private void startService(Context context){
        boolean isBluetoothOn = isBluetoothOn(context);
        Intent intent = null;
        Log.e(TAG,"isbluetoothon = " +isBluetoothOn);
        if(isBluetoothOn){
//            intent = new Intent(context, MainActivity.class);
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            context.startService(intent);
            intent = new Intent(context, BluetoothControlService.class);
            context.startService(intent);

        }else{
            intent = new Intent(ACTION_REOPEN_SERVICE);
            context.sendBroadcast(intent);
        }
    }
    private  boolean isBluetoothOn(Context context){
         BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
         BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(context, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            // finish();
            return false;
        }
        if (mBluetoothAdapter.isEnabled()) {
            //Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
            return true;
        }
        return false;
    }
}
