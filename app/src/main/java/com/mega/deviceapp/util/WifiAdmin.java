package com.mega.deviceapp.util;

/**
 * Created by Administrator on 2017/8/11.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

public class WifiAdmin {
    private final Context mContext;
    // 定义WifiManager对象
    private WifiManager mWifiManager;
    // 定义WifiInfo对象
    private WifiInfo mWifiInfo;
    // 扫描出的网络连接列表
    private List<ScanResult> mWifiList;
    // 网络连接列表
    private List<WifiConfiguration> mWifiConfiguration;
    // 定义一个WifiLock
    WifiLock mWifiLock;


    private WLANBroadcastReceiver receiver;
    private WLANStateListener mWLANStateListener;

    public void register(WLANStateListener listener) {
        mWLANStateListener = listener;
        if (receiver != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            mContext.registerReceiver(receiver, filter);
        }
    }

    public void unregister() {
        if (receiver != null) {
            mContext.unregisterReceiver(receiver);
        }
    }

    private class WLANBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("onReceive","liuhao intent:"+intent.getAction());
            if (intent != null) {
                String action = intent.getAction();
                /** wifi状态改变 */
                if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                    if (mWLANStateListener != null) {
                        Log.e("zhang", "WLANBroadcastReceiver --> onReceive--> WIFI_STATE_CHANGED_ACTION");
                        mWLANStateListener.onStateChanged();
                    }
                }
                /**
                 * WIFI_STATE_DISABLED    WLAN已经关闭
                 * WIFI_STATE_DISABLING   WLAN正在关闭
                 * WIFI_STATE_ENABLED     WLAN已经打开
                 * WIFI_STATE_ENABLING    WLAN正在打开
                 * WIFI_STATE_UNKNOWN     未知
                 */
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                switch (state) {
                    case WifiManager.WIFI_STATE_DISABLED:
                        if (mWLANStateListener != null) {
                            Log.e("zhang", "WLANBroadcastReceiver --> onReceive--> WIFI_STATE_DISABLED");
                            mWLANStateListener.onStateDisabled();
                        }
                        break;
                    case WifiManager.WIFI_STATE_DISABLING:
                        if (mWLANStateListener != null) {
                            Log.e("zhang", "WLANBroadcastReceiver --> onReceive--> WIFI_STATE_DISABLING");
                            mWLANStateListener.onStateDisabling();
                        }
                        break;
                    case WifiManager.WIFI_STATE_ENABLED:
                        if (mWLANStateListener != null) {
                            Log.e("zhang", "WLANBroadcastReceiver --> onReceive--> WIFI_STATE_ENABLED");
                            mWLANStateListener.onStateEnabled();
                        }
                        break;
                    case WifiManager.WIFI_STATE_ENABLING:
                        if (mWLANStateListener != null) {
                            Log.e("zhang", "WLANBroadcastReceiver --> onReceive--> WIFI_STATE_ENABLING");
                            mWLANStateListener.onStateEnabling();
                        }
                        break;
                    case WifiManager.WIFI_STATE_UNKNOWN:
                        if (mWLANStateListener != null) {
                            Log.e("zhang", "WLANBroadcastReceiver --> onReceive--> WIFI_STATE_UNKNOWN");
                            mWLANStateListener.onStateUnknow();
                        }
                        break;
                }
            }
        }
    }

    public interface WLANStateListener {
        void onStateChanged();

        void onStateDisabled();

        void onStateDisabling();

        void onStateEnabled();

        void onStateEnabling();

        void onStateUnknow();
    }
    // 构造器
    public WifiAdmin(Context context) {
        // 取得WifiManager对象
        mWifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        // 取得WifiInfo对象
        mWifiInfo = mWifiManager.getConnectionInfo();
        mContext = context;
        receiver = new WLANBroadcastReceiver();
    }

    // 打开WIFI
    public void openWifi(Context context) {
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        }else if (mWifiManager.getWifiState() == 2) {
            Toast.makeText(context,"亲，Wifi正在开启，不用再开了", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(context,"亲，Wifi已经开启,不用再开了", Toast.LENGTH_SHORT).show();
        }
    }
    // 关闭WIFI
    public void closeWifi(Context context) {
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
        }else if(mWifiManager.getWifiState() == 1){
            Toast.makeText(context,"亲，Wifi已经关闭，不用再关了", Toast.LENGTH_SHORT).show();
        }else if (mWifiManager.getWifiState() == 0) {
            Toast.makeText(context,"亲，Wifi正在关闭，不用再关了", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(context,"请重新关闭", Toast.LENGTH_SHORT).show();
        }
    }
    public boolean checkWifistatu(){
        return mWifiManager.isWifiEnabled();
    }

    // 检查当前WIFI状态
    public void checkState(Context context) {
        if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLING) {
            Toast.makeText(context,"Wifi正在关闭", Toast.LENGTH_SHORT).show();

        } else if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
            Toast.makeText(context,"Wifi已经关闭", Toast.LENGTH_SHORT).show();
        } else if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
            Toast.makeText(context,"Wifi正在开启", Toast.LENGTH_SHORT).show();
        } else if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            Toast.makeText(context,"Wifi已经开启", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context,"没有获取到WiFi状态", Toast.LENGTH_SHORT).show();
        }
    }

    /****获取WiFi状态
     * 0 : WIFI_STATE_DISABLING
     * 1: WIFI_STATE_DISABLED
     * 2: WIFI_STATE_ENABLING
     * 3: WIFI_STATE_ENABLED
     * @return state
     */
    public int getWifiState() {
        return mWifiManager.getWifiState();

    }
    // 锁定WifiLock
    public void acquireWifiLock() {
        mWifiLock.acquire();
    }

    // 解锁WifiLock
    public void releaseWifiLock() {
        // 判断时候锁定
        if (mWifiLock.isHeld()) {
            mWifiLock.acquire();
        }
    }

    // 创建一个WifiLock
    public void creatWifiLock() {
        mWifiLock = mWifiManager.createWifiLock("Test");
    }

    // 得到配置好的网络
    public List<WifiConfiguration> getConfiguration() {
        return mWifiConfiguration;
    }

    // 指定配置好的网络进行连接
    public void connectConfiguration(int index) {
        // 索引大于配置好的网络索引返回
        if (index > mWifiConfiguration.size()) {
            return;
        }
        // 连接配置好的指定ID的网络
        mWifiManager.enableNetwork(mWifiConfiguration.get(index).networkId,
                true);
    }

    public void startScan() {
        mWifiManager.startScan();
        // 得到扫描结果
        mWifiList = mWifiManager.getScanResults();
        // 得到配置好的网络连接
        mWifiConfiguration = mWifiManager.getConfiguredNetworks();
        if (mWifiList == null) {
            if(mWifiManager.getWifiState()==3){
                Toast.makeText(mContext,"当前区域没有无线网络", Toast.LENGTH_SHORT).show();
            }else if(mWifiManager.getWifiState()==2){
                Toast.makeText(mContext,"WiFi正在开启，请稍后重新点击扫描", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(mContext,"WiFi没有开启，无法扫描", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 得到网络列表
    public List<ScanResult> getWifiList() {
        return mWifiList;
    }

    // 查看扫描结果
    public StringBuilder lookUpScan() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < mWifiList.size(); i++) {
            stringBuilder
                    .append("Index_" + new Integer(i + 1).toString() + ":");
            // 将ScanResult信息转换成一个字符串包
            // 其中把包括：BSSID、SSID、capabilities、frequency、level
            stringBuilder.append((mWifiList.get(i)).toString());
            stringBuilder.append("/n");
        }
        return stringBuilder;
    }

    // 得到MAC地址
    public String getMacAddress() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.getMacAddress();
    }

    // 得到接入点的SSID
    public String getSSID() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.getSSID();
    }

    // 得到接入点的BSSID
    public String getBSSID() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.getBSSID();
    }

    // 得到IP地址
    public int getIPAddress() {
        return (mWifiInfo == null) ? 0 : mWifiInfo.getIpAddress();
    }

    // 得到连接的ID
    public int getNetworkId() {
        return (mWifiInfo == null) ? 0 : mWifiInfo.getNetworkId();
    }


    //判断WiFi是否连接
    public boolean isWifiConnected(Context context){
        ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifiNetworkInfo.isConnected();

    }
    // 得到WifiInfo的所有信息包
    public String getWifiInfo() {
        mWifiInfo = mWifiManager.getConnectionInfo();
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.toString();
    }

    // 得到WifiInfo的所有信息包
    public WifiInfo getWifiInfos() {
        return  mWifiManager.getConnectionInfo();
    }

    // 添加一个网络并连接
    public void addNetwork(WifiConfiguration wcg) {
        int wcgID = mWifiManager.addNetwork(wcg);
        boolean b =  mWifiManager.enableNetwork(wcgID, true);
        System.out.println("a--" + wcgID);
        System.out.println("b--" + b);
    }

    // 断开指定ID的网络
    public void disconnectWifi(int netId) {
        mWifiManager.disableNetwork(netId);
        mWifiManager.disconnect();
    }
    public void removeWifi(int netId) {
        disconnectWifi(netId);
        mWifiManager.removeNetwork(netId);
    }

//然后是一个实际应用方法，只验证过没有密码的情况：

    public WifiConfiguration CreateWifiInfo(String SSID, String Password, int Type)
    {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";

        WifiConfiguration tempConfig = this.IsExsits(SSID);
        if(tempConfig != null) {
            mWifiManager.removeNetwork(tempConfig.networkId);
        }

        if(Type == 1) //WIFICIPHER_NOPASS
        {
            config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        if(Type == 2) //WIFICIPHER_WEP
        {
            config.hiddenSSID = true;
            config.wepKeys[0]= "\""+Password+"\"";
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        if(Type == 3) //WIFICIPHER_WPA
        {
            config.preSharedKey = "\""+Password+"\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            //config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }
        return config;
    }

    private WifiConfiguration IsExsits(String SSID)
    {
        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
      //  Log.i("wifiadmin","isExsits existingconfigs = " +existingConfigs);
        if(existingConfigs != null){
            for (WifiConfiguration existingConfig : existingConfigs)
            {
                if (existingConfig.SSID.equals("\""+SSID+"\""))
                {
                    return existingConfig;
                }
            }
        }
        return null;
    }
}