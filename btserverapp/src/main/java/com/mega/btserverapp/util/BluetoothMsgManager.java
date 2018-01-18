package com.mega.btserverapp.util;

import com.mega.btserverapp.activity.BluetoothMsgListener;

/**
 * Created by Administrator on 2017/9/6.
 */

public class BluetoothMsgManager {

    private BluetoothMsgListener msgListener;

    public BluetoothMsgManager(){
       // this.msgListener = listener;
        //setMsgListener(this);
    }

    public void getMsg(String msg){
        if(msgListener != null)
            msgListener.getMsg(msg);
    }

    public void setMsgListener(BluetoothMsgListener listener){
        this.msgListener = listener;
    }

    public void setMsg(String msg){
        if(msgListener != null)
            msgListener.setMsg(msg);
    }
}
