package com.mega.btserverapp.model;

/**
 * Created by Administrator on 2017/9/11.
 */

public class WifiBasicInfo {

    private String ssid;
    private int level = -1000;
    private String passWord;

    public WifiBasicInfo(String ssid, String passWord){
        this.ssid = ssid;
        this.passWord = passWord;
    }

    public WifiBasicInfo(String ssid, int level){
        this.ssid = ssid;
        this.level = level;
    }

    public WifiBasicInfo(String ssid, String passWord, int level){
        this.ssid = ssid;
        this.level = level;
        this.passWord = passWord;
    }

    public void setPassWord(String passWord) {
        this.passWord = passWord;
    }

    public String getPassWord() {
        return passWord;
    }

    public int getLevel() {
        return level;
    }

    public String getSsid() {
        return ssid;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    @Override
    public String toString() {

        return super.toString();
    }
}
