package com.mega.deviceapp.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.mega.deviceapp.R;
import com.mega.deviceapp.activity.SmsActivity;
import com.mega.deviceapp.adapter.SmsAdapter;
import com.mega.deviceapp.model.SmsInfo;
import com.mega.deviceapp.util.BluetoothCmd;
import com.mega.deviceapp.util.MessageCode;
import com.mega.deviceapp.util.SmsUtil;
import com.mega.deviceapp.view.EndlessRecyclerOnScrollListener;
import com.mega.deviceapp.view.MyDecoration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liuhao
 * @time 2017/10/28 11:42
 * @des ${TODO}
 * @email liuhao_nevermore@163.com
 */

public class SmsListFragment extends Fragment implements SmsAdapter.OnRecyclerItemClickListener {

    private RecyclerView mRecycler;
    private SmsUtil mInfo;
    private List<SmsInfo> mList;
    private SmsAdapter mAdapter;
    private SwipeRefreshLayout mRefreshLayout;
    public static final int showListLineNum = 20;
    public int RefreshNumber = 0;
    private static final String TAG = "SmsActivity";
    private SmsActivity mParentActivity;
    private ProgressBar mProgressBar;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_sms_list, container, false);
        mParentActivity = (SmsActivity) getActivity();
        mInfo = new SmsUtil(mParentActivity);
        mRecycler = (RecyclerView) view.findViewById(R.id.recycler_list);
        mRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.layout_swipe_refresh);
        mProgressBar = (ProgressBar) view.findViewById(R.id.pb_progress_bar);
        mAdapter = new SmsAdapter(mParentActivity, mList);
        mParentActivity.getAdapterAndRefreshLayout(mAdapter,mRefreshLayout,mProgressBar);
        //在activty中置换掉adapter参数
        mRefreshLayout.setRefreshing(true);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            public void onRefresh() {
                if(mAdapter!=null)
                mAdapter.flashList(mList);
                mProgressBar.setProgress(0);
                mParentActivity.sendMsg(BluetoothCmd.COMMAND_QUERY_CONTACTS_MMS,-1, BluetoothCmd.COMMAND_REQUEST_FLASH_STATUS_MMS);
            }
        });
        //最后一个参数是反转布局一定是false,为true的时候为逆向显示，在聊天记录中可能会有使用
        //这个东西在显示后才会加
        // 载，不会像ScollView一样一次性加载导致内存溢出
        LinearLayoutManager layoutManager = new LinearLayoutManager(mParentActivity, LinearLayoutManager.VERTICAL, false);
        mRecycler.addOnScrollListener(new EndlessRecyclerOnScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int currentPage) {
                //我在List最前面加入一条数据
                //                RefreshNumber++;
                //                mAdapter.insertList(mInfo.getSmsInPhone(RefreshNumber*showListLineNum,showListLineNum));
                //                //数据重新加载完成后，提示数据发生改变，并且设置现在不在刷新
                //                mAdapter.notifyDataSetChanged();
                //                mRefreshLayout.setRefreshing(false);
            }
        });
        mAdapter.setOnRecyclerItemClickListener(this);
        mRecycler.setAdapter(mAdapter);
        //这句就是添加我们自定义的分隔线
        mRecycler.addItemDecoration(new MyDecoration(mParentActivity, MyDecoration.VERTICAL_LIST));
        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setRemoveDuration(1000);
        mRecycler.setItemAnimator(animator);
        mRecycler.setLayoutManager(layoutManager);
        return view;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void OnRecyclerItemClick(RecyclerView parent, View view, int position, SmsInfo data) {
//        Toast.makeText(mParentActivity, data.getThread_id() + " liu", Toast.LENGTH_LONG).show();
        mParentActivity.getSmsInfoInSmsList(data);
    }

}
