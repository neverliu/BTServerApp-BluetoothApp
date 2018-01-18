package com.mega.btserverapp.dbhelper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * @author liuhao
 * @time 2018/1/9 9:11
 * @des ${TODO}
 * @email liuhao_nevermore@163.com
 */

public class SmsSqliteOpenHelper extends SQLiteOpenHelper {

    public static final String CREATE_SMS_INFO = "create table smsInfo ("

            + "id integer primary key autoincrement, "

            + "thread_id integer, "//聊天框编号

            + "address text, "//号码

            + "person integer, "//联系人id

            + "read integer, "//是否已读

            + "isHead integer, "//是否为头部

            + "body integer, "//内容

            + "type text, "//短信类型

            + "date  integer,)";//发送日期

    public SmsSqliteOpenHelper(Context context) {

        //context :上下文   ， name：数据库文件的名称    factory：用来创建cursor对象，默认为null
        //version:数据库的版本号，从1开始，如果发生改变，onUpgrade方法将会调用,4.0之后只能升不能将
        super(context, "info.db", null,1);
    }
    //oncreate方法是数据库第一次创建的时候会被调用;  特别适合做表结构的初始化,需要执行sql语句；SQLiteDatabase db可以用来执行sql语句
    @Override
    public void onCreate(SQLiteDatabase db) {
        //通过SQLiteDatabase执行一个创建表的sql语句
        db.execSQL("create table info (_id integer primary key autoincrement,name varchar(20))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //添加一个phone字段
        db.execSQL("alter table info add phone varchar(11)");
    }
}
