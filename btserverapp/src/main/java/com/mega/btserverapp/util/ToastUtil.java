package com.mega.btserverapp.util;

import android.content.Context;
import android.widget.Toast;

/**
 * show msg
 */
public class ToastUtil {

    public static void showMsg(Context context,String msg){
        Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
    }
}
