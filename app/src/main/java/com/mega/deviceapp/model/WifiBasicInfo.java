package com.mega.deviceapp.model;

import android.net.wifi.WifiInfo;

/**
 * Created by Administrator on 2017/9/11.
 */

public class WifiBasicInfo {

    private String ssid;
    private int level;

    public WifiBasicInfo(String ssid, int level){
        this.ssid = ssid;
        this.level = level;
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
