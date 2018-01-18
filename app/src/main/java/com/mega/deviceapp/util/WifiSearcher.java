package com.mega.deviceapp.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author liuhao
 * @time 2017/10/14 15:08
 */

public class WifiSearcher {
    private static final int WIFI_SEARCH_TIMEOUT = 20; //扫描WIFI的超时时间

    private Context mContext;
    private WifiManager mWifiManager;
    private WiFiScanReceiver mWifiReceiver;
    private Lock mLock;
    private Condition mCondition;
    private SearchWifiListener mSearchWifiListener;
    private boolean mIsWifiScanCompleted = false;

    public static enum ErrorType {
        SEARCH_WIFI_TIMEOUT, //扫描WIFI超时（一直搜不到结果）
        NO_WIFI_FOUND,       //扫描WIFI结束，没有找到任何WIFI信号
    }

    //扫描结果通过该接口返回给Caller
    public interface SearchWifiListener {
        public void onSearchWifiFailed(ErrorType errorType);

        public void onSearchWifiSuccess(List<ScanResult> results);
    }

    public WifiSearcher(Context context, SearchWifiListener listener) {
        mContext = context;
        mSearchWifiListener = listener;
        mLock = new ReentrantLock();
        mCondition = mLock.newCondition();
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mWifiReceiver = new WiFiScanReceiver();
    }

    public void search() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                //如果WIFI没有打开，则打开WIFI
                if (!mWifiManager.isWifiEnabled()) {
                    mWifiManager.setWifiEnabled(true);
                }

                //注册接收WIFI扫描结果的监听类对象
                mContext.registerReceiver(mWifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

                //开始扫描
                mWifiManager.startScan();

                mLock.lock();

                //阻塞等待扫描结果
                try {
                    mIsWifiScanCompleted = false;
                    mCondition.await(WIFI_SEARCH_TIMEOUT, TimeUnit.SECONDS);
                    if (!mIsWifiScanCompleted) {
                        mSearchWifiListener.onSearchWifiFailed(ErrorType.SEARCH_WIFI_TIMEOUT);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mLock.unlock();
                //删除注册的监听类对象
                mContext.unregisterReceiver(mWifiReceiver);
            }
        }).start();
    }

    //系统WIFI扫描结果消息的接收者
    protected class WiFiScanReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {
            //提取扫描结果
            List<String> ssidResults = new ArrayList<>();
            List<ScanResult> scanResults = mWifiManager.getScanResults();
            for (ScanResult result : scanResults) {
                ssidResults.add(result.SSID);
            }
            //检测扫描结果
            if (ssidResults.isEmpty()) {
                mSearchWifiListener.onSearchWifiFailed(ErrorType.NO_WIFI_FOUND);
            } else {
                mSearchWifiListener.onSearchWifiSuccess(scanResults);
            }
            mLock.lock();
            mIsWifiScanCompleted = true;
            mCondition.signalAll();
            mLock.unlock();
        }
    }
}
