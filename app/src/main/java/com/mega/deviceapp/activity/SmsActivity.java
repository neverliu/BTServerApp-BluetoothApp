package com.mega.deviceapp.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.mega.deviceapp.R;
import com.mega.deviceapp.adapter.SmsAdapter;
import com.mega.deviceapp.adapter.SmsChatAdapter;
import com.mega.deviceapp.fragment.SmsChatFragment;
import com.mega.deviceapp.fragment.SmsListFragment;
import com.mega.deviceapp.listen.SmsFragmentListen;
import com.mega.deviceapp.model.SmsInfo;
import com.mega.deviceapp.service.BluetoothChatService;
import com.mega.deviceapp.service.BluetoothSocketService;
import com.mega.deviceapp.util.BluetoothCmd;
import com.mega.deviceapp.util.JsonUitl;
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
 * @time 2017/10/25 11:17
 * @des ${TODO}
 * @email liuhao_nevermore@163.com
 */

public class SmsActivity extends AppCompatActivity implements SmsFragmentListen {

    private FrameLayout mLayout;
    private FrameLayout mLayoutChat;
    private static final String TAG = "SmsActivity";
    private Toolbar mToolbar;
    public static final int CHATFRAGMENT = 0;
    public static final int LISTFRAGMENT = 1;

    public int CURRENTFRAGMENT = 1;

    private SmsChatFragment mChatFragment;
    private SmsListFragment mListFragment;
    private ActionBar mActionBar;
    private Intent mIntent;
    private Messenger mMessage, sMessage;

    private StringBuffer mStrBuff = new StringBuffer();
    private List<byte[]> mListByte= new ArrayList<>();
    private List<byte[]> mListByteChat= new ArrayList<>();

    private SmsAdapter mAdapter;
    private SmsChatAdapter mChatAdapter;
    private SwipeRefreshLayout mChatRefreshLayout;
    private SwipeRefreshLayout mListRefreshLayout;
    private List<SmsInfo> Contactlist;
    private List<SmsInfo> SingleContactlist;
    private ProgressBar mProgressBar;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MessageCode.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            //  setStatus(1,mConnectedDeviceName,"连接到  " + mConnectedDeviceName);
                            Log.d(TAG, "liu连接到 ");
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            //   setStatus("连接中");
                            Log.d(TAG, "liu连接中");
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            //   setStatus("无连接");
                            Log.d(TAG, "liu无连接");
                            break;
                    }
                    break;
                case MessageCode.MESSAGE_READ:
//                    byte[] readBuf = (byte[]) msg.obj;
//                    String readMessage = new String(readBuf, 0, msg.arg1);
                    checkReadMessage(msg);
                    break;
            }
        }
    };
    private SmsListFragment mSmsListFragment;

    private void checkReadMessage(Message msg) {
        byte[] readBuff = (byte[]) msg.obj;
        int command = readBuff[0];
        Log.i(TAG, "liuhao : command = " + command);
        switch (command) {
            case BluetoothCmd.COMMAND_QUERY_CONTACTS_MMS:
                resolveContactsList(msg);
                mListRefreshLayout.setRefreshing(false);
                break;
            case BluetoothCmd.COMMAND_QUERY_SINGLE_CONTACT_MMS:
                resolveSingleContactsList(msg);
                mChatRefreshLayout.setRefreshing(false);
            default:
                Log.d(TAG, " liuhao other msg : " + new String(readBuff));
                break;
        }
    }
    byte[] bytebuff = null;
    int currentIndex = 0;

    private String ListByteToString(List<byte[]> mListByte){
        if(mListByte == null)
            return null;

        int allSize = 0;
        for(byte[] a :mListByte){
            allSize += a.length;
        }
        byte[] data = new byte[allSize];
        int currentSize = 0;
        for(int i = 0 ; i < mListByte.size() ; i++){
            System.arraycopy(mListByte.get(i), 0, data, currentSize, mListByte.get(i).length);
            currentSize += mListByte.get(i).length;
        }
        String dataList = new String(data);

        return dataList;
    }
    /**
     * 解析sms列表
     */
    private void resolveContactsList(Message msg) {
        //        mList = mInfo.getSmsInPhone(RefreshNumber,showListLineNum);
        //        mAdapter = new SmsAdapter(this, mList);
        byte[] readBuff = (byte[]) msg.obj;
//        String readMessage = new String(readBuff, 2, msg.arg1 - 2);
        byte[] buffList = new byte[msg.arg1 - 2];
        System.arraycopy(readBuff, 2,buffList, 0, msg.arg1 - 2);
        if (readBuff[1] == BluetoothCmd.CONTINUE_PACKAGE) {
            //
            mListRefreshLayout.setEnabled(false);
            mListByte.add(buffList);
        } else if (readBuff[1] == BluetoothCmd.LAST_PACKAGE) {
            mListByte.add(buffList);
            //List<SmsInfo> list = getSmsListFromMsg(ListByteToString(mListByte));
//            List<SmsInfo> list = JsonUitl.stringToList( ListByteToString(mListByte) , SmsInfo.class ) ;
            List<SmsInfo> list = JsonUitl.jsonSmsStringToList(ListByteToString(mListByte));
            if ( list.size() == 0){
                Log.e(TAG,"resolveContactsList  list.size() == 0");
                return;
            }
            if( list.get(0).getProgress()!=null && !list.get(0).getProgress().equals("") ){
                if(mProgressBar!=null){
                    String[] progress = list.get(0).getProgress().split("/");
                    int progress1 = Integer.parseInt(progress[0]);
                    int progress2 = Integer.parseInt(progress[1]);
                    if(progress1 == 1){
                        mProgressBar.setVisibility(View.VISIBLE);
                        mProgressBar.setMax(progress2);
                        mProgressBar.setProgress(progress1);
                    }else{
                        mProgressBar.setProgress(progress1);
                    }
                    if(progress2 ==progress1){
                        mListRefreshLayout.setEnabled(true);
                        Toast.makeText(this,"loading completed",Toast.LENGTH_SHORT).show();
                        mProgressBar.setVisibility(View.GONE);
                    }
                }
            }else{
                //传输条数小于20条
                mListRefreshLayout.setEnabled(true);
            }
            //当该list为头部最开始list，重新刷新adapter的数据
            if(list.get(0).getIsHead() == -1){
                mAdapter.flashList(list);
                //当该list为中间部分list，加上adapter的数据
            }else if(list.get(0).getIsHead() == 0 || list.get(0).getIsHead() == 1){
                mAdapter.insertList(list);
            }
                mAdapter.notifyDataSetChanged();

            currentIndex = 0;
            bytebuff = null;
            //当该数据不是尾部也就是不等于1的时候，继续发送
            if(list.get(0).getIsHead() != 1){
                sendMsg(BluetoothCmd.COMMAND_QUERY_CONTACTS_MMS,-1, BluetoothCmd.COMMAND_CONTINUE_FLASH_STATUS_MMS);
            }
            mListByte.clear();
        }
    }

    /**
     * 解析singlesms列表
     */
    private void resolveSingleContactsList(Message msg) {
        //        mList = mInfo.getSmsInPhone(RefreshNumber,showListLineNum);
        //        mAdapter = new SmsAdapter(this, mList);
        byte[] readBuff = (byte[]) msg.obj;
        String readMessage = new String(readBuff, 2, msg.arg1 - 2);
        byte[] buffList = new byte[msg.arg1 - 2];
        System.arraycopy(readBuff, 2,buffList, 0, msg.arg1 - 2);
        Log.i(TAG, "liu readMessage = " + readMessage);
        if (readBuff[1] == BluetoothCmd.CONTINUE_PACKAGE) {
            mListByteChat.add(buffList);
//            mStrBuff.append(readMessage);
        } else if (readBuff[1] == BluetoothCmd.LAST_PACKAGE) {
            mListByteChat.add(buffList);
//            mStrBuff.append(readMessage);
            //List<SmsInfo> list = getSmsListFromMsg(ListByteToString(mListByteChat));
           // List<SmsInfo> list = JsonUitl.stringToList( ListByteToString(mListByteChat) , SmsInfo.class ) ;
            Log.i(TAG,"mListByteChat : " +ListByteToString(mListByteChat));
            List<SmsInfo> list = JsonUitl.jsonSmsStringToList(ListByteToString(mListByteChat));
            mChatAdapter.flashList(list);
            mChatAdapter.notifyDataSetChanged();
            Log.d(TAG,"liuhao list.size():"+list.size());
            if(list.size()>0)
            mChatFragment.getRecyclerView().smoothScrollToPosition(list.size()-1);
//            mStrBuff = new StringBuffer();
            mListByteChat.clear();
        }
    }
    private List<SmsInfo> getSmsListFromMsg(String msg) {
        //测试二：把json字符串转化成list集合
       // List<Me> listMe = JsonUitl.stringToList( json1 , Me.class ) ;
        List<SmsInfo> list = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(msg);
            for (int i = 0; i < array.length(); i++) {
                JSONObject ob = array.getJSONObject(i);
                SmsInfo con = new SmsInfo(
                        ob.getString("date"),
                        ob.getString("body"),
                        ob.getString("type"),
                        ob.getString("address"),
                        ob.getString("person"),
                        ob.getInt("thread_id"),
                        ob.getInt("read"),
                        ob.getInt("_id"),
                        ob.getInt("isHead"),
                        ob.getString("progress"));
                Log.d(TAG, "liuhao :" + con.toString());
                list.add(con);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        this.unbindService(mConnection);
        destroySmstransmission();
        super.onDestroy();
    }

    private void destroySmstransmission(){
        sendMsg(BluetoothCmd.COMMAND_QUERY_CONTACTS_MMS,-1, BluetoothCmd.COMMAND_CONTINUE_RESET_STATUS_MMS);
    }
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            Log.d(TAG,"liuhao onKeyUp:"+CURRENTFRAGMENT);
            if (CURRENTFRAGMENT == CHATFRAGMENT) {
                checkFragment(LISTFRAGMENT, null);
                return true;
            }else if(CURRENTFRAGMENT == LISTFRAGMENT){
                finish();
            }
            return false;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private void bindService() {
        mIntent = new Intent(this, BluetoothSocketService.class);
        this.bindService(mIntent, mConnection, this.BIND_AUTO_CREATE);
    }

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "liu onServiceConnected");
            sMessage = new Messenger(service);
            Message msg = new Message();
            msg.what = MessageCode.MESSAGE_BIND_SERVICE;
            msg.replyTo = mMessage;
            try {
                sMessage.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            sendMsg(BluetoothCmd.COMMAND_QUERY_CONTACTS_MMS,-1,BluetoothCmd.COMMAND_REQUEST_FLASH_STATUS_MMS);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "onServiceDisconnected");
            sMessage = null;
            if(mProgressBar!=null)
            mProgressBar.setVisibility(View.GONE);

            if(mListRefreshLayout!=null)
                mListRefreshLayout.setRefreshing(false);
        }
    };

    public void sendMsg(byte flag, int thread_id, byte isFlash) {
        Log.i(TAG, "liuhao sendMsg thread_id:"+thread_id);
        Message message = new Message();
        //当isFlash为1的时候是再次刷新，为0是被动的刷新,为2时是退出Sms界面。重置
        byte[] poByte = (thread_id + "").getBytes();

        message.what = MessageCode.MESSAGE_WRITE_TO_SERVICE;
        byte[] command = new byte[1+poByte.length+1];
        command[0] = flag;
        command[1] = isFlash;
        System.arraycopy(poByte, 0, command, 2, poByte.length);
        message.obj = command;
        try {
            sMessage.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms);
        mLayout = (FrameLayout) findViewById(R.id.id_content);
        mLayoutChat = (FrameLayout) findViewById(R.id.id_content_chat);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        bindService();
        mMessage = new Messenger(mHandler);
        setSupportActionBar(mToolbar);
        mActionBar = getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setDisplayShowTitleEnabled(false);
        }
        mToolbar.setTitle("Message");
        setDefaultFragment();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (CURRENTFRAGMENT == CHATFRAGMENT) {
                    checkFragment(LISTFRAGMENT, null);
                    return true;
                } else {
                    finish();
                }
                break;
        }
        return true;
    }

    private void setDefaultFragment() {
        FragmentManager fm = this.getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        mSmsListFragment = new SmsListFragment();
        transaction.replace(R.id.id_content, mSmsListFragment);
        transaction.commit();
    }

    public void checkFragment(int fragment, SmsInfo info) {
        FragmentManager fm = this.getSupportFragmentManager();
        // 开启Fragment事务
        FragmentTransaction transaction = fm.beginTransaction();
        switch (fragment) {
            case CHATFRAGMENT:
                mChatFragment = new SmsChatFragment();
                Bundle bundle = new Bundle();
                bundle.putInt("thread_id", info.getThread_id());
                mToolbar.setTitle(info.getAddress());
                mChatFragment.setArguments(bundle);
                // 使用当前Fragment的布局替代id_content的控件
                CURRENTFRAGMENT = CHATFRAGMENT;
                transaction.replace(R.id.id_content_chat, mChatFragment);

                mLayout.setVisibility(View.GONE);
                mLayoutChat.setVisibility(View.VISIBLE);
                break;
            case LISTFRAGMENT:
                if (mListFragment == null) {
                    mListFragment = new SmsListFragment();
                }
                CURRENTFRAGMENT = LISTFRAGMENT;
                mToolbar.setTitle("Message");

                //                transaction.replace(R.id.id_content, mListFragment);
                mLayout.setVisibility(View.VISIBLE);
                mLayoutChat.setVisibility(View.GONE);
                transaction.remove(mChatFragment);
                if(mChatFragment!=null)
                    mChatFragment.getAdapter().flashList(null);
                break;
        }
        transaction.commit();
    }

    @Override
    public void getSmsInfoInSmsList(SmsInfo info) {
        Log.d(TAG, "liuhao getSmsInfoInSmsList");
        checkFragment(CHATFRAGMENT, info);
    }

    @Override
    public List<SmsInfo> getSendData(int flag, SwipeRefreshLayout refreshLayout) {
        return null;
    }

    @Override
    public void getAdapterAndRefreshLayout(RecyclerView.Adapter adapter, SwipeRefreshLayout refreshLayout, ProgressBar mProgressBar) {
        Log.d(TAG, "liuhao getAdapterAndRefreshLayout");
        if(CURRENTFRAGMENT == LISTFRAGMENT){
                this.mAdapter = (SmsAdapter)adapter;
            this.mListRefreshLayout = refreshLayout;
            this.mProgressBar = mProgressBar;
        }else{
            mChatAdapter = (SmsChatAdapter)adapter;
            mChatRefreshLayout = refreshLayout;
        }
    }
}
