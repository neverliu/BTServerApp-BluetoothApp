package com.mega.btserverapp.util;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.Telephony;
import android.util.Log;

import com.mega.btserverapp.model.MmsInfo;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2017/10/26.
 */

public class MmsUtil {

    private static final String SMS_URI_ALL = "content://sms/";
    private static final String SMS_URI_INBOX = "content://sms/inbox";
    private static final String SMS_URI_SEND = "content://sms/sent";
    private static final String SMS_URI_DRAFT = "content://sms/draft";
    private static final String SMS_URI_OUTBOX = "content://sms/outbox";
    private static final String SMS_URI_FAILED = "content://sms/failed";
    private static final String SMS_URI_QUEUED = "content://sms/queued";


    public static List<MmsInfo> queryAllMms(Context context){
        List<MmsInfo> list = new ArrayList<>();
        HashMap<Integer, MmsInfo> map = new HashMap<>();
        try {
            Uri uri = Uri.parse(SMS_URI_ALL);
            String[] projection = new String[] { "_id", "address", "person", "body", "date", "type" ,"thread_id","read"};
            Cursor cur = context.getContentResolver().query(uri, projection, null, null, "date desc");      // 获取手机内部短信

            if (cur.moveToFirst()) {
                int index_Address = cur.getColumnIndex("address");
                int index_Person = cur.getColumnIndex("person");
                int index_Body = cur.getColumnIndex("body");
                int index_Date = cur.getColumnIndex("date");
                int index_Type = cur.getColumnIndex("type");
                int index_Threadid= cur.getColumnIndex("thread_id");
                int index_Read = cur.getColumnIndex("read");
                int index_ID = cur.getColumnIndex("_id");

                do {
                    String strAddress = cur.getString(index_Address);
                    int intPerson = cur.getInt(index_Person);
                    String name = ContactsUtil.queryNameByNum(context,strAddress);
                    String strbody = cur.getString(index_Body);
                    long longDate = cur.getLong(index_Date);
                    int intType = cur.getInt(index_Type);
                    int id = cur.getInt(index_ID);
                    int threadId = cur.getInt(index_Threadid);
                    int read = cur.getInt(index_Read);

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
                    MmsInfo mmsInfo = new MmsInfo();

                    mmsInfo.setNumber(strAddress);
                    mmsInfo.setBody(strbody);
                    mmsInfo.setDate(longDate);
                    mmsInfo.setType(intType);
                    mmsInfo.setId(id);
                    mmsInfo.setThreadId(threadId);
                    mmsInfo.setRead(read);
                    if(name!=null){
                        mmsInfo.setName("");
                    }

                    map.put(threadId, mmsInfo);

                } while (cur.moveToNext());

                if (!cur.isClosed()) {
                    cur.close();
                    cur = null;
                }
            } // end if
        if (map.size()>1){
            Iterator iter = map.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                int key = (int)entry.getKey();
                MmsInfo val =(MmsInfo) entry.getValue();
                list.add(val);
            }
        }

        } catch (SQLiteException ex) {
            //Log.d("SQLiteException in getSmsInPhone", ex.getMessage());
        }
        return list;
    }

    public static List<MmsInfo> queryMmsByThreadId(Context context,int thread_id){
        List<MmsInfo> list = new ArrayList<>();
        try{
            Uri uri = Uri.parse(SMS_URI_ALL);
            String[] projection = new String[] { "_id", "address", "person", "body", "date", "type" ,"thread_id","read"};
            String selection = Telephony.Mms.THREAD_ID + "='"+thread_id+"'";
            Cursor cur = context.getContentResolver().query(uri, projection, selection, null, "date desc");

            if (cur.moveToFirst()) {
                int index_Address = cur.getColumnIndex("address");
                int index_Person = cur.getColumnIndex("person");
                int index_Body = cur.getColumnIndex("body");
                int index_Date = cur.getColumnIndex("date");
                int index_Type = cur.getColumnIndex("type");
                int index_Threadid= cur.getColumnIndex("thread_id");
                int index_Read = cur.getColumnIndex("read");
                int index_ID = cur.getColumnIndex("_id");

                do {
                    String strAddress = cur.getString(index_Address);
                    int intPerson = cur.getInt(index_Person);
                    String name = ContactsUtil.queryNameByNum(context,strAddress);
                    String strbody = cur.getString(index_Body);
                    long longDate = cur.getLong(index_Date);
                    int intType = cur.getInt(index_Type);
                    int id = cur.getInt(index_ID);
                    int threadId = cur.getInt(index_Threadid);
                    int read = cur.getInt(index_Read);

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
                    MmsInfo mmsInfo = new MmsInfo();

                    mmsInfo.setNumber(strAddress);
                    mmsInfo.setBody(strbody);
                    mmsInfo.setDate(longDate);
                    mmsInfo.setType(intType);
                    mmsInfo.setId(id);
                    mmsInfo.setThreadId(threadId);
                    mmsInfo.setRead(read);
                    if(name!=null){
                        mmsInfo.setName("");
                    }

                    list.add(mmsInfo);
                } while (cur.moveToNext());

                if (!cur.isClosed()) {
                    cur.close();
                    cur = null;
                }
            } // end if
        } catch (SQLiteException ex) {
            ex.printStackTrace();
            //Log.d("SQLiteException in getSmsInPhone", ex.getMessage());
        }
        return list;
    }
}
