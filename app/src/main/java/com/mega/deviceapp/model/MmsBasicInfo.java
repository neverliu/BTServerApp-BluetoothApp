package com.mega.deviceapp.model;

/**
 * Created by Administrator on 2017/10/17.
 */

public class MmsBasicInfo {

    public int id;
    public String name;
    public String number;
    public String bodyText;
    public int read;
    public int status;
    public int date;

    public MmsBasicInfo(){

    }

    public MmsBasicInfo(int id, String number, String bodyText, int status, int date){
        new MmsBasicInfo(id, null, number, bodyText, 0, status, date);
    }

    public MmsBasicInfo(int id, String name, String number, String bodyText, int read, int status, int date){
        this.id = id;
        this.name = name;
        this.number = number;
        this.bodyText = bodyText;
        this.read = read;
        this.status = status;
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public int getDate() {
        return date;
    }

    public int getStatus() {
        return status;
    }

    public int getRead() {
        return read;
    }

    public String getBodyText() {
        return bodyText;
    }

    public String getName() {
        return name;
    }

    public String getNumber() {
        return number;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setBodyText(String bodyText) {
        this.bodyText = bodyText;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public void setRead(int read) {
        this.read = read;
    }

    public void setDate(int date) {
        this.date = date;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
