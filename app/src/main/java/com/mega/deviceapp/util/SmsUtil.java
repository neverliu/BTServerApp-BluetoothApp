package com.mega.deviceapp.util;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import com.mega.deviceapp.model.SmsInfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author liuhao
 * @time 2017/10/25 14:54
 * @des ${TODO}
 * @email liuhao_nevermore@163.com
 */

public class SmsUtil {

    private Context mContext;
    private static final String TAG = "SmsUtil";
    private List<SmsInfo> mList;

    public  SmsUtil(Context context){
        mContext = context;
    }
    public List<SmsInfo> getSmsInPhone(int start, int length) {
        final String SMS_URI_ALL = "content://sms/";
        final String SMS_URI_INBOX = "content://sms/inbox";
        final String SMS_URI_SEND = "content://sms/sent";
        final String SMS_URI_DRAFT = "content://sms/draft";
        final String SMS_URI_OUTBOX = "content://sms/outbox";
        final String SMS_URI_FAILED = "content://sms/failed";
        final String SMS_URI_QUEUED = "content://sms/queued";
        StringBuilder smsBuilder = new StringBuilder();
        try {
            Uri uri = Uri.parse(SMS_URI_ALL);
            mList = new ArrayList();
            String[] projection = new String[]{"_id", "address", "person", "body", "date", "type", "thread_id","read"};
            int end = start +length;
            Cursor cur = mContext.getContentResolver().query(
                    uri,
                    projection,
                    "_id<="+end+" and "+"_id > "+start,
                    null,
                    "date asc");  // 获取手机内部短信
            if (cur.moveToFirst()) {
                int id = cur.getColumnIndex("_id");
                int index_Address = cur.getColumnIndex("address");
                int index_Person = cur.getColumnIndex("person");
                int index_Body = cur.getColumnIndex("body");
                int index_Date = cur.getColumnIndex("date");
                int index_Type = cur.getColumnIndex("type");
                do {
                    String strAddress = cur.getString(index_Address);
                    int intPerson = cur.getInt(index_Person);
                    int intId = cur.getInt(id);
                    Log.d(TAG,"liu intId:"+intId);
                    String strbody = cur.getString(index_Body);
                    long longDate = cur.getLong(index_Date);
                    int intType = cur.getInt(index_Type);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                    Date d = new Date(longDate);
                    String strDate = dateFormat.format(d);
                    String strType = "";
                    if (intType == 1) {
                        strType = "接收";
                    } else if (intType == 2) {
                        strType = "发送";
                    } else {
                        strType = "null";
                    }
                    SmsInfo info = new SmsInfo();
                    info.setAddress(strAddress);
                    info.setPerson(intPerson+"");
                    info.setType(strType);
                    info.setDate(strDate);
                    info.setBody(strbody);
                    mList.add(info);
                } while (cur.moveToNext());
                if (!cur.isClosed()) {
                    cur.close();
                    cur = null;
                }
            } else {
                smsBuilder.append("no result!");
            } // end if
            smsBuilder.append("getSmsInPhone has executed!");
        } catch (SQLiteException ex) {
            Log.d(TAG, ex.getMessage());
        }
        return mList;
    }
}
