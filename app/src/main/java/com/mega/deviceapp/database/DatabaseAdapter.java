package com.mega.deviceapp.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.mega.deviceapp.model.MmsBasicInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/10/17.
 */

public class DatabaseAdapter {
    //数据库表名
    private static final String TABLE_NAME= "mms";


    private DatabaseHelper dbHelper;

    public DatabaseAdapter(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    /**
     * 添加数据
     *
     * @param mms
     */
    public void create(MmsBasicInfo mms){
        String sql = "insert into "+TABLE_NAME+"("
                +DBtableColumn.KEY_ID + ", "
                +DBtableColumn.KEY_NAME+ ", "
                +DBtableColumn.KEY_NUMBER+","
                +DBtableColumn.KEY_BODY+","
                +DBtableColumn.KEY_DATE+","
                +DBtableColumn.KEY_READ+","
                +DBtableColumn.KEY_STATUS
                +")values(?,?,?,?,?,?,?)";
        Object[] args = {mms.getId(), mms.getName(), mms.getNumber(), mms.getBodyText(),
                mms.getDate(), mms.getRead(), mms.getStatus()};
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        db.execSQL(sql, args);
        db.close();
    }
    /*
    public void create(Note note) {
        String sql = "insert into note(title, content, createDate, updateDate)values(?,?,?,?)";
        Object[] args = {note.getTitle(), note.getContent(), note.getCreateDate(), note.getUpdateDate()};
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        db.execSQL(sql, args);
        db.close();
    }*/

    /**
     * 删除数据
     *
     * @param id
     */
    public void remove(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String sql = "delete from note where _id = ?";
        Object[] args = {id};
        db.execSQL(sql, args);
        db.close();
    }

    /**
     * 修改数据
     *
     * @param note
     */
    /*public void update(Note note) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String sql = "update note set title = ?, content = ?, updateDate = ? where _id = ?";
        Object[] args = {note.getTitle(), note.getContent(), note.getUpdateDate(), note.getId()};
        db.execSQL(sql, args);
        db.close();
    }*/

    /**
     * 按id查询
     *
     * @param id
     * @return
     */
    public MmsBasicInfo findById(int id){
        MmsBasicInfo mms = null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "select * from note where _id = ?";
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(id)});
        if(cursor.moveToNext()){
            mms = new MmsBasicInfo();
            mms.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DBtableColumn.KEY_ID)));
            mms.setName(cursor.getString(cursor.getColumnIndexOrThrow(DBtableColumn.KEY_NAME)));
            mms.setNumber(cursor.getString(cursor.getColumnIndexOrThrow(DBtableColumn.KEY_NUMBER)));
            mms.setBodyText(cursor.getString(cursor.getColumnIndexOrThrow(DBtableColumn.KEY_BODY)));
            mms.setDate(cursor.getInt(cursor.getColumnIndexOrThrow(DBtableColumn.KEY_DATE)));
            mms.setRead(cursor.getInt(cursor.getColumnIndexOrThrow(DBtableColumn.KEY_READ)));
            mms.setStatus(cursor.getInt(cursor.getColumnIndexOrThrow(DBtableColumn.KEY_STATUS)));

        }
        cursor.close();
        db.close();
        return mms;
    }
    /*public Note findById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "select * from note where _id = ?";
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(id)});

        Note note = null;
        if (cursor.moveToNext()) {
            note = new Note();

            note.setId(cursor.getInt(cursor.getColumnIndexOrThrow(MetaData.NoteTable._ID)));
            note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(MetaData.NoteTable.TITLE)));
            note.setContent(cursor.getString(cursor.getColumnIndexOrThrow(MetaData.NoteTable.CONTENT)));
            note.setCreateDate(cursor.getString(cursor.getColumnIndexOrThrow(MetaData.NoteTable.CREATE_DATE)));
            note.setUpdateDate(cursor.getString(cursor.getColumnIndexOrThrow(MetaData.NoteTable.UPDATE_DATE)));
        }
        cursor.close();
        db.close();

        return note;
    }*/

    /**
     * 查询所有
     *
     * @return
     */
    public List<MmsBasicInfo> queryAll(){
        SQLiteDatabase mDatabase = dbHelper.getReadableDatabase();
        Cursor results = mDatabase.query(TABLE_NAME, new String[]{DBtableColumn.KEY_ID,
                        DBtableColumn.KEY_NAME, DBtableColumn.KEY_NUMBER,
                        DBtableColumn.KEY_BODY, DBtableColumn.KEY_STATUS,
                        DBtableColumn.KEY_DATE, DBtableColumn.KEY_READ},
                null, null, null, null, null, null);
        return convertToTree(results);
    }
    private List<MmsBasicInfo> convertToTree(Cursor cursor) {
        int resultCounts = cursor.getCount();
        if (resultCounts == 0 || !cursor.moveToFirst()) {
            return null;
        }
        List<MmsBasicInfo> mTreeList = new ArrayList<>();
        for (int i = 0; i < resultCounts; i++) {
            MmsBasicInfo mms = new MmsBasicInfo();
            mms.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DBtableColumn.KEY_ID)));
            mms.setName(cursor.getString(cursor.getColumnIndexOrThrow(DBtableColumn.KEY_NAME)));
            mms.setNumber(cursor.getString(cursor.getColumnIndexOrThrow(DBtableColumn.KEY_NUMBER)));
            mms.setBodyText(cursor.getString(cursor.getColumnIndexOrThrow(DBtableColumn.KEY_BODY)));
            mms.setDate(cursor.getInt(cursor.getColumnIndexOrThrow(DBtableColumn.KEY_DATE)));
            mms.setRead(cursor.getInt(cursor.getColumnIndexOrThrow(DBtableColumn.KEY_READ)));
            mms.setStatus(cursor.getInt(cursor.getColumnIndexOrThrow(DBtableColumn.KEY_STATUS)));
            mTreeList.add(mms);
            cursor.moveToNext();
        }
        return mTreeList;
    }
    /*public ArrayList<Note> findAll() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "select * from note";
        Cursor cursor = db.rawQuery(sql,null);


        ArrayList<Note> notes = new ArrayList<>();
        Note note = null;
        while (cursor.moveToNext()) {
            note = new Note();

            note.setId(cursor.getInt(cursor.getColumnIndexOrThrow(MetaData.NoteTable._ID)));
            note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(MetaData.NoteTable.TITLE)));
            note.setContent(cursor.getString(cursor.getColumnIndexOrThrow(MetaData.NoteTable.CONTENT)));
            note.setCreateDate(cursor.getString(cursor.getColumnIndexOrThrow(MetaData.NoteTable.CREATE_DATE)));
            note.setUpdateDate(cursor.getString(cursor.getColumnIndexOrThrow(MetaData.NoteTable.UPDATE_DATE)));
            notes.add(note);
        }
        cursor.close();
        db.close();
        return notes;
    }*/

    /**
     * 分页查询
     *
     * @param limit 默认查询的数量
     * @param skip 跳过的行数
     * @return
     */
   /* public ArrayList<Note> findLimit(int limit, int skip) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "select * from note order by _id desc limit ? offset ?";
        String[] strs = new String[]{String.valueOf(limit), String.valueOf(skip)};
        Cursor cursor = db.rawQuery(sql,strs);


        ArrayList<Note> notes = new ArrayList<>();
        Note note = null;
        while (cursor.moveToNext()) {
            note = new Note();

            note.setId(cursor.getInt(cursor.getColumnIndexOrThrow(MetaData.NoteTable._ID)));
            note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(MetaData.NoteTable.TITLE)));
            note.setContent(cursor.getString(cursor.getColumnIndexOrThrow(MetaData.NoteTable.CONTENT)));
            note.setCreateDate(cursor.getString(cursor.getColumnIndexOrThrow(MetaData.NoteTable.CREATE_DATE)));
            note.setUpdateDate(cursor.getString(cursor.getColumnIndexOrThrow(MetaData.NoteTable.UPDATE_DATE)));
            notes.add(note);
        }
        cursor.close();
        db.close();
        return notes;
    }*/
}
