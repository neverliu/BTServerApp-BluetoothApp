package com.mega.btserverapp.model;

/**
 * Created by Administrator on 2017/10/26.
 */

public class MmsInfo {

    public int threadId;
    public String number;
    public String name;
    public long date;
    public String body;
    public int type;
    public int read;
    public int id;


    public MmsInfo(){}

    public MmsInfo(int id,int threadId, String number, String name, String body, long date, int type, int read){
        this.threadId = threadId;
        this.number = number;
        this.name = name;
        this.body = body;
        this.date = date;
        this.type = type;
        this.read = read;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setRead(int read) {
        this.read = read;
    }

    public void setThreadId(int threadId) {
        this.threadId = threadId;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getNumber() {
        return number;
    }

    public int getRead() {
        return read;
    }

    public int getThreadId() {
        return threadId;
    }

    public int getType() {
        return type;
    }

    public long getDate() {
        return date;
    }

    public String getBody() {
        return body;
    }
}
