package com.mega.deviceapp.fragment;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.mega.deviceapp.R;
import com.mega.deviceapp.activity.SmsActivity;
import com.mega.deviceapp.adapter.SmsAdapter;
import com.mega.deviceapp.adapter.SmsChatAdapter;
import com.mega.deviceapp.model.SmsInfo;
import com.mega.deviceapp.service.BluetoothChatService;
import com.mega.deviceapp.service.BluetoothSocketService;
import com.mega.deviceapp.util.BluetoothCmd;
import com.mega.deviceapp.util.MessageCode;
import com.mega.deviceapp.view.EndlessRecyclerOnScrollListener;
import com.mega.deviceapp.view.MyDecoration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liuhao
 * @time 2017/10/28 9:21
 * @des ${TODO}
 * @email liuhao_nevermore@163.com
 */

public class SmsChatFragment extends Fragment implements SmsAdapter.OnRecyclerItemClickListener {

    private static final String TAG = "SmsChatFragment";
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mRefreshLayout;
    private SmsActivity mParentActivity;
    private int mThread_id;
    private SmsChatAdapter mAdapter;
    private List<SmsInfo> mList;


    public SmsChatAdapter getAdapter(){
        return mAdapter;
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //引用创建好的xml布局
        View view = inflater.inflate(R.layout.fragment_sms_chat,container,false);
        Bundle bundle = this.getArguments();
        mThread_id = bundle.getInt("thread_id");
        mParentActivity = (SmsActivity) getActivity();
        mRefreshLayout = (SwipeRefreshLayout)view.findViewById(R.id.layout_swipe_refresh);
        mRecyclerView = (RecyclerView)view.findViewById(R.id.recylerView);
        mAdapter = new SmsChatAdapter(mParentActivity, mList);
        mParentActivity.getAdapterAndRefreshLayout(mAdapter,mRefreshLayout,null);
        mRecyclerView.setAdapter(mAdapter);
        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setRemoveDuration(1000);
        mRecyclerView.setItemAnimator(animator);
        LinearLayoutManager layoutManager = new LinearLayoutManager(mParentActivity, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        mRefreshLayout.setRefreshing(true);
        mParentActivity.sendMsg(BluetoothCmd.COMMAND_QUERY_SINGLE_CONTACT_MMS, mThread_id, BluetoothCmd.COMMAND_REQUEST_FLASH_STATUS_MMS);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            public void onRefresh() {
                mParentActivity.sendMsg(BluetoothCmd.COMMAND_QUERY_SINGLE_CONTACT_MMS, mThread_id, BluetoothCmd.COMMAND_REQUEST_FLASH_STATUS_MMS);
            }
        });
        return view;

    }
    public RecyclerView getRecyclerView(){
        return mRecyclerView;
    }
    @Override
    public void onDestroy() {
        Log.d(TAG,"liu onDestroy");
        super.onDestroy();
    }

    @Override
    public void OnRecyclerItemClick(RecyclerView parent, View view, int position, SmsInfo data) {
        Toast.makeText(mParentActivity, data.getThread_id() + " liu", Toast.LENGTH_LONG).show();
        mParentActivity.getSmsInfoInSmsList(data);
    }
}
