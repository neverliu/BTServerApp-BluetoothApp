package com.mega.deviceapp.listen;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.widget.ProgressBar;

import com.mega.deviceapp.model.SmsInfo;

import java.util.List;

/**
 * @author liuhao
 * @time 2017/10/28 15:43
 * @des ${TODO}
 * @email liuhao_nevermore@163.com
 */

public interface SmsFragmentListen {
    public void getSmsInfoInSmsList(SmsInfo info);
    public List<SmsInfo> getSendData(int flag, SwipeRefreshLayout refreshLayout);
    public  void getAdapterAndRefreshLayout(RecyclerView.Adapter adapter, SwipeRefreshLayout refreshLayout, ProgressBar mProgressBar);
}
