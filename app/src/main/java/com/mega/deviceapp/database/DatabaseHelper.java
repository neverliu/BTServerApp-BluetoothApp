package com.mega.deviceapp.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Administrator on 2017/10/17.
 */

public class DatabaseHelper extends SQLiteOpenHelper{
    //数据库名字
    private static final String DB_NAME = "mega.db";

    //数据库表名
    private static final String TABLE_NAME= "mms";

    //本版号
    private static final int VERSION = 1;

    //创建表
    private static final  String CREATE_TABLE_MMS = "CREATE TABLE "+ TABLE_NAME
            +"("
            +DBtableColumn.KEY_ID+" integer primary key autoincrement,"
            +DBtableColumn.KEY_NAME+" text, "
            +DBtableColumn.KEY_NUMBER+" text, "
            +DBtableColumn.KEY_DATE+" INTEGER, "
            +DBtableColumn.KEY_BODY+" text, "
            +DBtableColumn.KEY_READ+" INTEGER DEFAULT 0,"
            +DBtableColumn.KEY_STATUS+" INTEGER DEFAULT -1"
            +")";
          //  "name text, number text, body text, date text, read INTEGER DEFAULT 0,status INTEGER DEFAULT -1)";

    //删除表
    private static final String DROP_TABLE_NOTE = "drop table if exists "+TABLE_NAME;


    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //SQLiteDatabase 用于操作数据库的工具类
        db.execSQL(CREATE_TABLE_MMS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_TABLE_NOTE);
        db.execSQL(CREATE_TABLE_MMS);
    }


}
