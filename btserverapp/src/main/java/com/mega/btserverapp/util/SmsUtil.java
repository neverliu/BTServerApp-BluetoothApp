package com.mega.btserverapp.util;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.util.Log;
import android.util.SparseIntArray;

import com.mega.btserverapp.model.SmsInfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
    private Cursor mCur;
    private boolean isContinueToReadTheDatabase = true;

    private static final String SMS_URI_ALL = "content://sms/";
    private static final String SMS_URI_INBOX = "content://sms/inbox";
    private static final String SMS_URI_SEND = "content://sms/sent";
    private static final String SMS_URI_DRAFT = "content://sms/draft";
    private static final String SMS_URI_OUTBOX = "content://sms/outbox";
    private static final String SMS_URI_FAILED = "content://sms/failed";
    private static final String SMS_URI_QUEUED = "content://sms/queued";

    public SmsUtil(Context context) {
        mContext = context;
    }

    //短信界面显示list
    public List<SmsInfo> showSmsList() {
        List<SmsInfo> list = getSmsInPhone(0, 9999);
        if (list == null)
            return null;
        List<SmsInfo> listA = new ArrayList();
        HashMap hashmap = new HashMap();
        for (int i = 0; i < list.size(); i++) {
            int threadId = list.get(i).getThread_id();
            hashmap.put(threadId, list.get(i));
        }
        Iterator iter = hashmap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            SmsInfo val = (SmsInfo) entry.getValue();
//            Log.d(TAG, "liu val:" + val.getAddress());
            listA.add(val);
        }
        return listA;
    }


    //if time1 > time2 ,return 0 , or time1 < time2 return 1 , or time1 = time2 return 2
    private int compareTime(String time1, String time2) {
        //yyyy-MM-dd hh:mm:ss
        //1234567890123456789
        //         1111111111

        //        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        //        String date=sdf.format(new java.util.Date());
        //        Log.d(TAG,"liu date:"+
        //                date.substring(0,4)+" "+
        //                date.substring(5,7)+" "+
        //                date.substring(8,10)+" "+
        //                date.substring(11,13)+" "+
        //                date.substring(14,16)+" "+
        //                date.substring(17,19));

        if (time1 == null || time2 == null) {
            return -1;
        }
        int year1, month1, day1, hour1, min1, sec1;
        int year2, month2, day2, hour2, min2, sec2;
        year1 = Integer.parseInt(time1.substring(0, 4));
        year2 = Integer.parseInt(time2.substring(0, 4));
        if (year1 > year2) {
            return 0;
        } else if (year1 == year2) {
            month1 = Integer.parseInt(time1.substring(5, 7));
            month2 = Integer.parseInt(time2.substring(5, 7));
            if (month1 > month2) {
                return 0;
            } else if (month1 == month2) {
                day1 = Integer.parseInt(time1.substring(8, 10));
                day2 = Integer.parseInt(time2.substring(8, 10));
                if (day1 > day2) {
                    return 0;
                } else if (day1 == day2) {
                    hour1 = Integer.parseInt(time1.substring(11, 13));
                    hour2 = Integer.parseInt(time2.substring(11, 13));
                    if (hour1 > hour2) {
                        return 0;
                    } else if (hour1 == hour2) {
                        min1 = Integer.parseInt(time1.substring(14, 16));
                        min2 = Integer.parseInt(time2.substring(14, 16));
                        if (min1 > min2) {
                            return 0;
                        } else if (min1 == min2) {
                            sec1 = Integer.parseInt(time1.substring(17, 19));
                            sec2 = Integer.parseInt(time2.substring(17, 19));
                            if (sec1 > sec2) {
                                return 0;
                            } else if (sec1 == sec2) {
                                return 2;
                            }
                        }
                    }
                }
            }
        }
        return 1;
    }
    public void closeCursor(){
//        if(mCur != null){
//            if (!mCur.isClosed()) {
//                mCur.close();
//                mCur = null;
//            }
//        }
        isContinueToReadTheDatabase = false;
    }

    public int getSmsCount(){
        int result = 0;
        try {
            Uri uri = Uri.parse(SMS_URI_ALL);
            String[] projection = new String[]{"_id"};
            Cursor csr = mContext.getContentResolver().query(uri, projection,
                    null, null, "date desc");
            if (csr.moveToFirst()) {
                int id = csr.getColumnIndex("_id");
                boolean isRead = true;
                do{
                    int intId = csr.getInt(id);
                    isRead = false;
                    result = intId;
                }while (csr.moveToNext() && isRead);
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        return result;
    }

    public List<SmsInfo> getSmsInPhone(int start, int length) {
        isContinueToReadTheDatabase = true;
        StringBuilder smsBuilder = new StringBuilder();
        try {
            Uri uri = Uri.parse(SMS_URI_ALL);
            mList = new ArrayList();
            String[] projection = new String[]{"_id", "address", "person", "body", "date", "type", "thread_id", "read"};
            int end = start + length;
            // 获取手机内部短信
            mCur = mContext.getContentResolver().query(
                    uri,
                    projection,
                    "_id<=" + end + " and " + "_id > " + start,
                    null,
                    "date asc");
            if (mCur.moveToFirst()) {
                int id = mCur.getColumnIndex("_id");
                int index_Address = mCur.getColumnIndex("address");
                int index_Person = mCur.getColumnIndex("person");
                int index_Body = mCur.getColumnIndex("body");
                int index_Date = mCur.getColumnIndex("date");
                int index_Type = mCur.getColumnIndex("type");
                int index_Thread = mCur.getColumnIndex("thread_id");
                int read = mCur.getColumnIndex("read");
                do {
//                    if(!isContinueToReadTheDatabase){
//                        return null;
//                    }
                    String strAddress = mCur.getString(index_Address);
                    int intPerson = mCur.getInt(index_Person);
                    String name = ContactsUtil.queryNameByNum(mContext, strAddress);
                    int intId = mCur.getInt(id);
                    Log.d(TAG, "liu intId:" + intId+" read:"+read);
                    String strbody = mCur.getString(index_Body);
                    long longDate = mCur.getLong(index_Date);
                    int intType = mCur.getInt(index_Type);
                    int intThread = mCur.getInt(index_Thread);
                    int intread = mCur.getInt(read);
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
                    info.setPerson(name);
                    info.setType(strType);
                    info.setDate(strDate);
                    info.setBody(strbody);
                    info.setThread_id(intThread);
                    info.setRead(intread);
                    info.set_id(intId);
                    mList.add(info);
                } while (mCur.moveToNext() && isContinueToReadTheDatabase);
                if (!mCur.isClosed()) {
                    mCur.close();
                    mCur = null;
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

    public List<SmsInfo> getSingleSmsInPhone(int thread_id) {
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
            String[] projection = new String[]{"_id", "address", "person", "body", "date", "type", "thread_id", "read"};
            Cursor cur = mContext.getContentResolver().query(
                    uri,
                    projection,
                    "thread_id =" + thread_id,
                    null,
                    "date asc");  // 获取手机内部短信
            if (cur.moveToFirst()) {
                int id = cur.getColumnIndex("_id");
                int index_Address = cur.getColumnIndex("address");
                int index_Person = cur.getColumnIndex("person");
                int index_Body = cur.getColumnIndex("body");
                int index_Date = cur.getColumnIndex("date");
                int index_Type = cur.getColumnIndex("type");
                int index_Thread = cur.getColumnIndex("thread_id");
                int read = cur.getColumnIndex("read");
                do {
                    String strAddress = cur.getString(index_Address);
                    int intPerson = cur.getInt(index_Person);
                    int intId = cur.getInt(id);
                    Log.d(TAG, "liu intId:" + intId);
                    String strbody = cur.getString(index_Body);
                    long longDate = cur.getLong(index_Date);
                    int intType = cur.getInt(index_Type);
                    int intThread = cur.getInt(index_Thread);
                    int intread = cur.getInt(read);
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
                    info.setPerson(intPerson + "");
                    info.setType(strType);
                    info.setDate(strDate);
                    info.setBody(strbody);
                    info.setThread_id(intThread);
                    info.setRead(intread);
                    info.set_id(intId);
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
