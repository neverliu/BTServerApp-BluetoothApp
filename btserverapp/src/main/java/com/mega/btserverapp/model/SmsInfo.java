package com.mega.btserverapp.model;

/**
 * @author liuhao
 * @time 2017/10/25 15:17
 * @des ${TODO}
 * @email liuhao_nevermore@163.com
 */

public class SmsInfo {
    private String date;
    private String body;
    private String type;
    private String address;
    private String person;
    private String progress;
    private int thread_id;
    private int read;
    private int _id;
    private int isHead;

    public String getProgress() {
        return progress;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }

    public int getIsHead() {
        return isHead;
    }

    public void setIsHead(int isHead) {
        this.isHead = isHead;
    }

    public int getRead() {
        return read;
    }

    public void setRead(int read) {
        this.read = read;
    }

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public int getThread_id() {
        return thread_id;
    }

    public void setThread_id(int thread_id) {
        this.thread_id = thread_id;
    }

    public SmsInfo(String date, String body, String type, String address, String person, int thread_id, int read, int _id, int isHead, String progress) {
        this.date = date;
        this.body = body;
        this.type = type;
        this.address = address;
        this.person = person;
        this._id = _id;
        this.read = read;
        this.thread_id = thread_id;
        this.isHead = isHead;
        this.progress = progress;

    }
    public SmsInfo(){

    }
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPerson() {
        return person;
    }

    public void setPerson(String person) {
        this.person = person;
    }

    public void clear() {
        person = null;
        date = null;
        body = null;
        type = null;
        address = null;
    }

    @Override
    public String toString() {
        return "SmsInfo{" +
                "date='" + date + '\'' +
                ", body='" + body + '\'' +
                ", type='" + type + '\'' +
                ", address='" + address + '\'' +
                ", person='" + person + '\'' +
                '}';
    }

}
