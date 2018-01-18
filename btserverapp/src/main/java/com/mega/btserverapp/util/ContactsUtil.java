package com.mega.btserverapp.util;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.util.Log;


import com.mega.btserverapp.model.Contacts;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class ContactsUtil {

    public static List<Contacts> getLocalContacts(Context context){
        return getPhoneNumbersByUri(context,Phone.CONTENT_URI);
    }
    private static List<Contacts> getPhoneNumbersByUri(Context context, Uri uri){
        String[] PHONES_PROJECTION = new String[]{
                Phone.DISPLAY_NAME, Phone.NUMBER, Phone.CONTACT_ID};
        List<Contacts> _List = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(uri,PHONES_PROJECTION,null,null,null);
        if(cursor!=null){
            while (cursor.moveToNext()){
                String phoneNumber = cursor.getString(cursor.getColumnIndex(Phone.NUMBER));
                String name = cursor.getString(cursor.getColumnIndex(Phone.DISPLAY_NAME));
                int id = cursor.getInt(cursor.getColumnIndex(Phone.CONTACT_ID));
                Contacts contacts = new Contacts(name,phoneNumber,id);
                _List.add(contacts);
            }
            cursor.close();
        }
        return _List;
    }

    public static boolean insert(Context context,String name, String mobile_number){
        try {
            ContentValues values = new ContentValues();
            // 下面的操作会根据RawContacts表中已有的rawContactId使用情况自动生成新联系人的rawContactId
            Uri rawContactUri = context.getContentResolver().insert(
                    ContactsContract.RawContacts.CONTENT_URI, values);
            long rawContactId = ContentUris.parseId(rawContactUri);

            // 向data表插入姓名数据
            if (!name.equals("")) {
                values.clear();
                values.put(ContactsContract.Contacts.Data.RAW_CONTACT_ID, rawContactId);
                values.put(ContactsContract.Contacts.Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
                values.put(StructuredName.GIVEN_NAME, name);
                context.getContentResolver().insert(ContactsContract.Data.CONTENT_URI, values);
            }
            // 向data表插入电话数据
            if (!mobile_number.equals("")) {
                values.clear();
                values.put(ContactsContract.Contacts.Data.RAW_CONTACT_ID, rawContactId);
                values.put(ContactsContract.Contacts.Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
                values.put(Phone.NUMBER, mobile_number);
                values.put(Phone.TYPE, Phone.TYPE_MOBILE);
                context.getContentResolver().insert(ContactsContract.Data.CONTENT_URI,
                        values);
            }
        }
        catch (Exception e){
            return false;
        }
        return true;
    }

    public static boolean updateContact(Context context, String id, String name, String number){
        Contacts contacts = queryContactByID(context,id);
        if(contacts == null){
            return false;
        }
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        boolean updateNameResult = true;
        boolean updateNumberResult = true;
        Log.i("updateContact","update contacts.getName() ="+contacts.getName()+"  name ="+name);
        if(!contacts.getName().equals(name)) {
            values.put(StructuredName.GIVEN_NAME, name);
            values.put(ContactsContract.Contacts.Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);

            String where = ContactsContract.Data.RAW_CONTACT_ID + "=? AND "
                    + ContactsContract.Data.MIMETYPE + "=?";
            String[] selectionArgs = new String[]{id, StructuredName.CONTENT_ITEM_TYPE};
            updateNameResult = resolver.update(ContactsContract.Data.CONTENT_URI, values,
                    where, selectionArgs) > 0;
            Log.i("ContactsUtil","updateContact  updateNameResult = "+updateNameResult);
        }
        values.clear();
        Log.i("contactUtil","update  contacts.getPhoneNumber( ="+contacts.getPhoneNumber());
        if(!contacts.getPhoneNumber().equals(number)) {
            values.put(Phone.NUMBER, number);
            values.put(ContactsContract.Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
            String where2 = ContactsContract.Data.RAW_CONTACT_ID + "=? AND "
                    + ContactsContract.Data.MIMETYPE + "=?";
            String[] selectionArgs2 = new String[]{id, Phone.CONTENT_ITEM_TYPE};
            updateNumberResult = resolver.update(ContactsContract.Data.CONTENT_URI, values,
                    where2, selectionArgs2) > 0;
        }
        return updateNameResult && updateNumberResult ;
    }

    public static boolean deleteContact(Context context,String contact_id){
        String where = ContactsContract.Data._ID  + " =?";
        String[] whereparams = new String[]{contact_id};
        //boolean result = context.getContentResolver().delete(ContactsContract.RawContacts.CONTENT_URI, where, whereparams)>0;
        //boolean result = context.getContentResolver().delete(ContactsContract.Data.CONTENT_URI, where, whereparams)>0;
        long id = Long.parseLong(contact_id);
        Uri noteUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
        boolean result =  context.getContentResolver().delete(noteUri, null, null) > 0;

        return result;
    }

    public static List<Contacts> ReadAllContacts(Context context){
        List<Contacts> _List = new ArrayList<>();
        Cursor cursor = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);
        int contactIdIndex = 0;
        int nameIndex = 0;
        if(cursor == null){
            return null;
        }
        if(cursor.getCount()>0){
            contactIdIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
            nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
        }
        while(cursor.moveToNext()){
            int contactId = cursor.getInt(contactIdIndex);
            String name = cursor.getString(nameIndex);
            /*
             * 查找该联系人的phone信息
             */
            Cursor phones = context.getContentResolver().query(Phone.CONTENT_URI,
                    null,
                    Phone.CONTACT_ID + "=" + contactId,
                    null, null);
            int phoneIndex = 0;
            if(phones == null){
                return null;
            }
            if(phones.getCount() > 0){
                phoneIndex = phones.getColumnIndex(Phone.NUMBER);
            }
            while(phones.moveToNext()){
                String phoneNumber = phones.getString(phoneIndex);
                Contacts contact = new Contacts(name,phoneNumber,false);
                contact.setId(contactId);
                _List.add(contact);
            }
            cursor.close();
            phones.close();
        }
        return _List;
    }


    public static String queryNameByNum(Context context,String num) {
        Cursor cursorOriginal =
                context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[] {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME},
                        ContactsContract.CommonDataKinds.Phone.NUMBER + "='"+num+"'",null,null);
        String name = "";
        if(null!=cursorOriginal){
           // if(cursorOriginal.getCount()>1)
            if(cursorOriginal.moveToFirst()){
                name = cursorOriginal.getString(cursorOriginal.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            }
            cursorOriginal.close();
        }
        return name;
    }

    public static Contacts queryContactByID(Context context,String id) {
        Cursor cursorOriginal =
                context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[] {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                ContactsContract.CommonDataKinds.Phone.NUMBER},
                        Phone.RAW_CONTACT_ID + "='"+id+"'",null,null);
        if(null!=cursorOriginal){
            // if(cursorOriginal.getCount()>1)
            if(cursorOriginal.moveToFirst()){
                Contacts contacts = new Contacts();
                int index_name = cursorOriginal.getColumnIndex(Phone.DISPLAY_NAME);
                int index_number = cursorOriginal.getColumnIndex(Phone.NUMBER);
                String name = cursorOriginal.getString(index_name);
                String number = cursorOriginal.getString(index_number);
                contacts.setName(name);
                contacts.setPhoneNumber(number);
                cursorOriginal.close();
                return contacts;
            }else{
                return null;
            }
        }else{
            return null;
        }
    }
}
