package com.mega.deviceapp.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.mega.deviceapp.R;
import com.mega.deviceapp.adapter.CommonAdapter;
import com.mega.deviceapp.adapter.ViewHolder;
import com.mega.deviceapp.model.Contacts;
import com.mega.deviceapp.model.WifiBasicInfo;
import com.mega.deviceapp.service.BluetoothChatService;
import com.mega.deviceapp.service.BluetoothSocketService;
import com.mega.deviceapp.util.BluetoothCmd;
import com.mega.deviceapp.util.MessageCode;
import com.mega.deviceapp.util.ToastUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ContactsActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private String TAG = "ContactsActivity";

    private View getContactsBtn, addContactBtn;
    private ListView mListView;
    private View mProgressView;

    private MyAdapter myAdapter;

    private List<Contacts> mContactsList;

    private Intent mIntent;

    private String mConnectedDeviceName;

    private Messenger mMessage, sMessage;

    private List<byte[]> mListByte = new ArrayList<>();

    private StringBuffer mStrBuff = new StringBuffer();

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MessageCode.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            //  setStatus(1,mConnectedDeviceName,"连接到  " + mConnectedDeviceName);
                            Log.d(TAG,"连接到 "+mConnectedDeviceName);
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            //   setStatus("连接中");
                            Log.d(TAG,"连接中");
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            //   setStatus("无连接");
                            Log.d(TAG,"无连接");
                            break;
                    }
                    break;
                case MessageCode.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    // receive(readMessage);
                    Log.i(TAG,"message_read  readmessage = " + readMessage);

                    checkReadMessage(msg);
                    break;
                case MessageCode.MESSAGE_DEVICE_NAME:
                    mConnectedDeviceName = msg.getData().getString(MessageCode.DEVICE_NAME);
                    if (null != this) {
                        ToastUtil.showMsg(ContactsActivity.this,"Connected to "
                                + mConnectedDeviceName);
                    }
                    break;
                case MessageCode.MESSAGE_TOAST:
                    if (null != this){
                        ToastUtil.showMsg(ContactsActivity.this,msg.getData().getString(MessageCode.TOAST));
                    }
                    break;
                case MessageCode.MESSAGE_GET_SOCKET_STATE:
                    Log.i(TAG,"MESSAGE_GET_SOCKET_STATE");
                    int state = (int)msg.obj;
                    if(state == BluetoothChatService.STATE_LISTEN ||
                            state == BluetoothChatService.STATE_NONE){
                        // updateDisConnectText();
                        setViewEnabled(false);
                    }else {
                        setViewEnabled(true);
                    }
                    break;
            }
        }
    };

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG,"onServiceConnected");
            sMessage = new Messenger(service);
            Message msg = new Message();
            msg.what = MessageCode.MESSAGE_BIND_SERVICE;
            msg.replyTo = mMessage;
            try {
                sMessage.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            //setWifiStateCommand();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG,"onServiceDisconnected");
            sMessage = null;
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final android.support.v7.app.ActionBar ab = getSupportActionBar();
        ab.setHomeAsUpIndicator(R.mipmap.ic_actionbar_back);
        ab.setDisplayHomeAsUpEnabled(true);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        fab.setVisibility(View.GONE);

        getContactsBtn = findViewById(R.id.btn_get_contacts);
        getContactsBtn.setOnClickListener(this);

        addContactBtn = findViewById(R.id.btn_add_contact);
        addContactBtn.setOnClickListener(this);

        mListView = (ListView)findViewById(R.id.contacts_listview);
        myAdapter = new MyAdapter(this,mContactsList, R.layout.item_list_contacts);
        mListView.setAdapter(myAdapter);

        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);

        mProgressView = findViewById(R.id.pro_view);
        mMessage = new Messenger(mHandler);
        bindService();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if(id==android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSocketStatus();
    }

    private void updateSocketStatus(){
        Message message = new Message();
        message.what = MessageCode.MESSAGE_GET_SOCKET_STATE;
        Log.i(TAG,"updateSocketStatus sMessage = "+sMessage);
        if(sMessage != null){
            try {
                sMessage.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

   private void setViewEnabled(boolean enabled){
        getContactsBtn.setEnabled(enabled);
        addContactBtn.setEnabled(enabled);
       if(getContactsBtn instanceof ViewGroup) {
           ViewGroup vg = (ViewGroup) getContactsBtn;
           Log.i(TAG,"vg.chialdcount = "+ vg.getChildCount());
           for (int i = 0; i < vg.getChildCount(); i++){
               View view = vg.getChildAt(i);
               view.setEnabled(enabled);
               // setEnabledAll(vg.getChildAt(i), enabled);
           }
       }
       if(addContactBtn instanceof ViewGroup) {
           ViewGroup vg = (ViewGroup) addContactBtn;
           Log.i(TAG,"vg.chialdcount = "+ vg.getChildCount());
           for (int i = 0; i < vg.getChildCount(); i++){
               View view = vg.getChildAt(i);
               view.setEnabled(enabled);
               // setEnabledAll(vg.getChildAt(i), enabled);
           }
       }
   }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
    }

    private void bindService(){
        mIntent = new Intent(this, BluetoothSocketService.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);
    }

    private void checkReadMessage(Message msg) {
        byte[] readBuff = (byte[]) msg.obj;
        int command = readBuff[0];
        Log.i(TAG, "checkreadMessage : command = " + command);
        //final Message msg1 = msg;
        switch (command) {
            case BluetoothCmd.COMMAND_QUERY_CONTACTS_LIST:
                resolveContactsList(msg);
                break;
            case BluetoothCmd.COMMAND_INSERT_CONTACT:
                if(readBuff[1] == BluetoothCmd.SEND_ACK){
                    ToastUtil.showMsg(this, getString(R.string.toast_add_ok));
                    mProgressView.setVisibility(View.VISIBLE);
                }else if(readBuff[1] == BluetoothCmd.SEND_ERROR){
                    ToastUtil.showMsg(this,getString(R.string.toast_add_fiald));
                    mProgressView.setVisibility(View.GONE);
                }
                break;
            case BluetoothCmd.COMMAND_MODIFY_CONTACT:
                if(readBuff[1] == BluetoothCmd.SEND_ACK){
                    ToastUtil.showMsg(this, getString(R.string.toast_modify_ok));
                    mProgressView.setVisibility(View.VISIBLE);
                }else if(readBuff[1] == BluetoothCmd.SEND_ERROR){
                    ToastUtil.showMsg(this,getString(R.string.toast_modify_fiald));
                    mProgressView.setVisibility(View.GONE);
                }
                break;
            case BluetoothCmd.COMMAND_DELETE_CONTACT:
                if(readBuff[1] == BluetoothCmd.SEND_ACK){
                    ToastUtil.showMsg(this, getString(R.string.toast_delete_ok));
                }else if(readBuff[1] == BluetoothCmd.SEND_ERROR){
                    ToastUtil.showMsg(this,getString(R.string.toast_delete_fiald));
                }
                break;
            default:
                Log.d(TAG," checkReadMessage other msg : " + new String(readBuff));
                break;
        }
    }

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

    /**解析contacts列表*/
    private void resolveContactsList(Message msg){
        byte[] readBuff = (byte[]) msg.obj;
        int buffLen = msg.arg1 -2;
        String readMessage = new String(readBuff, 2, buffLen);
        Log.i(TAG,"resolveContactsList readBuff[1] = "+readBuff[1] );
        byte[] dataBuff = new byte[buffLen];
        System.arraycopy(readBuff, 2, dataBuff, 0, buffLen);
        if(readBuff[1] == BluetoothCmd.CONTINUE_PACKAGE){
            Log.d(TAG,"resolveContactsList CONTINUE_PACKAGE");
//            mStrBuff.append(readMessage);
            mListByte.add(dataBuff);
            mProgressView.setVisibility(View.VISIBLE);
        }else if (readBuff[1] == BluetoothCmd.LAST_PACKAGE){
            mListByte.add(dataBuff);
//            mStrBuff.append(readMessage);
            Log.d(TAG,"resolveContactsList LAST_PACKAGE");
            String buffString = ListByteToString(mListByte);
//            mContactsList = getContactsListFromMsg(mStrBuff.toString());
            mContactsList = getContactsListFromMsg(buffString);
//            Collections.sort(mContactsList, new Comparator<Contacts>(){
//                @Override
//                public int compare(Contacts lhs, Contacts rhs){
//                    return lhs.getName().compareTo(rhs.getName());
//                }
//            });
            myAdapter.setList(mContactsList);
            Log.d(TAG,"decoderWifis list : " + mContactsList);
            myAdapter.notifyDataSetChanged();
//            mStrBuff= new StringBuffer();
            mListByte.clear();
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    mProgressView.setVisibility(View.GONE);
                }
            }, 1000);

        }
    }

    private List<Contacts> getContactsListFromMsg(String msg){
        List<Contacts> list = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(msg);
            Log.d(TAG,"getContactsListFromMsg size ="+ array.length());
            for(int i=0;i<array.length();i++){
                JSONObject ob = array.getJSONObject(i);
                Log.i(TAG,"getContactsListFromMsg name : " + ob.getString("name")
                        + " ,phonenumber: "+ob.getString("phonenumber") +" , level : "+ob.getInt("id"));
                Contacts con = new Contacts(ob.getString("name"),ob.getString("phonenumber"), ob.getInt("id"));
                list.add(con);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Collections.sort(list, new Comparator<Contacts>(){
            @Override
            public int compare(Contacts lhs, Contacts rhs){
                return lhs.getName().compareTo(rhs.getName());
            }
        });
        return list;
    }

    private void setRequestContactsCommand(){
        Message message = new Message();
        message.what = MessageCode.MESSAGE_WRITE_TO_SERVICE;
        byte[] command = new byte[2];
        command[0] = BluetoothCmd.COMMAND_QUERY_CONTACTS_LIST;
        message.obj = command;
        try {
            sMessage.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void setAddContactCommand(){
        View contentViwe = View.inflate(this,R.layout.content_add_contact_dialog, null);
        final EditText nameView = (EditText) contentViwe.findViewById(R.id.et_name);
        final EditText numberView = (EditText) contentViwe.findViewById(R.id.et_phone);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle(R.string.dialog_add_contact)
                .setView(contentViwe)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton(android.R.string.ok, null);
                        /*new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = nameView.getText().toString();
                        String phone = numberView.getText().toString();
                        if(name.length() > 0 && phone.length() > 6 ){
                            addContactCommand(name, phone);
                        }else {
                            ToastUtil.showMsg(ContactsActivity.this, getString(R.string.toast_notify_name_phone));
                        }
                    }
                });//*/
        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = nameView.getText().toString();
                String phone = numberView.getText().toString();
                if(name.length()>0 && phone.length()>2) {
                    addContactCommand(name, phone);
                    dialog.dismiss();
                }else {
                    ToastUtil.showMsg(ContactsActivity.this, getString(R.string.toast_notify_name_phone));
                    //return;
                }
            }
        });


    }

    private void modifyContactCommand(int id, String name, String phone){
        byte[] nameByte = name.getBytes();
        byte[] phoneByte = phone.getBytes();
        byte[] idByte = (id+"").getBytes();
        byte[] command = new byte[4 + nameByte.length+ phoneByte.length+ idByte.length];
        command[0] = BluetoothCmd.COMMAND_MODIFY_CONTACT;
        command[1] = (byte) nameByte.length;
        command[2] = (byte)phoneByte.length;
        command[3] = (byte)idByte.length;
        System.arraycopy(nameByte, 0, command, 4, nameByte.length);
        System.arraycopy(phoneByte, 0, command, (4 + nameByte.length), phoneByte.length);
        System.arraycopy(idByte, 0, command, (4 + nameByte.length + phoneByte.length), idByte.length);

        Message message = new Message();
        message.what = MessageCode.MESSAGE_WRITE_TO_SERVICE;
        message.obj = command;
        try {
            sMessage.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void addContactCommand(String name, String phone){
        byte[] nameByte = name.getBytes();
        byte[] phoneByte = phone.getBytes();

        byte[] command = new byte[3 + nameByte.length+ phoneByte.length];
        command[0] = BluetoothCmd.COMMAND_INSERT_CONTACT;
        command[1] = (byte) nameByte.length;
        command[2] = (byte)phoneByte.length;
        System.arraycopy(nameByte, 0, command, 3, nameByte.length);
        System.arraycopy(phoneByte, 0, command, (3 + nameByte.length), phoneByte.length);

        Message message = new Message();
        message.what = MessageCode.MESSAGE_WRITE_TO_SERVICE;
        message.obj = command;
        try {
            sMessage.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        Message message = new Message();
        message.what = MessageCode.MESSAGE_WRITE_TO_SERVICE;
        byte[] command = new byte[2];

        switch (v.getId()){
            case R.id.btn_get_contacts:
                setRequestContactsCommand();
                mProgressView.setVisibility(View.VISIBLE);
                break;

            case R.id.btn_add_contact:
                setAddContactCommand();
                break;
        }

    }

    private void showItemMenu(final int id, final String name, final String phone){

        final String[] items = new String[]{getString(R.string.menu_modify),
                getString(R.string.menu_delete),getString(R.string.menu_dialer)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.menu_title);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which == 0){
                   showModifyDialog(id, name, phone);
                }else if(which == 1){
                    showCheckDeleteDialog(id, name, phone);
                }else if(which == 2){
                    makeCall(phone);
                }
            }
        });
        builder.show();
    }

    private void showModifyDialog(final int id, final String name, final String phone){
        View contentViwe = View.inflate(this,R.layout.content_add_contact_dialog, null);
        final EditText nameView = (EditText) contentViwe.findViewById(R.id.et_name);
        final EditText numberView = (EditText) contentViwe.findViewById(R.id.et_phone);
        nameView.setText(name);
        numberView.setText(phone);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle(R.string.modify_contact)
                .setView(contentViwe)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton(android.R.string.ok, null);

        final AlertDialog dialog = builder.create();
        dialog.show();
        //must after dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = nameView.getText().toString();
                String phone = numberView.getText().toString();
                if(name.length()>0 && phone.length()>2) {
                    modifyContactCommand(id, name, phone);
                    dialog.dismiss();
                }else {
                    ToastUtil.showMsg(ContactsActivity.this, getString(R.string.toast_notify_name_phone));
                    //return;
                }
            }
        });
    }

    private void showCheckDeleteDialog(final int id, final String name, final String phone){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(String.format(getString(R.string.delete_contact), name));
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Message message = new Message();
                byte[] idByte = (id+"").getBytes();
                message.what = MessageCode.MESSAGE_WRITE_TO_SERVICE;
                byte[] command = new byte[idByte.length+2];
                command[0] = BluetoothCmd.COMMAND_DELETE_CONTACT;
                command[1] = (byte)idByte.length;
                System.arraycopy(idByte, 0, command, 2, idByte.length);
                message.obj = command;
                try {
                    sMessage.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void makeCall(String number){
        Message message = new Message();
        byte[] numberByte = number.getBytes();
        message.what = MessageCode.MESSAGE_WRITE_TO_SERVICE;
        byte[] command = new byte[numberByte.length+1];
        command[0] = BluetoothCmd.COMMAND_MAKE_CALL_CONTACT;
        System.arraycopy(numberByte, 0, command, 1, numberByte.length);
        message.obj = command;
        try {
            sMessage.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Contacts con = mContactsList.get(position);
        int conId = con.getId();
        String name = con.getName();
        String number = con.getPhoneNumber();
        Log.i(TAG,"onItemLongClick conid : " +conId+" , name = " + name+", number:" + number);
        showItemMenu(conId, name, number);
        return true;
    }


    private class MyAdapter extends CommonAdapter<Contacts> {

        public MyAdapter(Context context, List<Contacts> list, int layoutId) {
            super(context, list, layoutId);
        }

        @Override
        public void convert(ViewHolder holder, final Contacts con, int position) {

            holder.setText(R.id.tv_item_contacts_name, con.getName());
            holder.setText(R.id.tv_item_contacts_phonenumber, con.getPhoneNumber());

            View checkBox = holder.getView(R.id.checkbox);
            checkBox.setVisibility(View.INVISIBLE);


        }
    }
}
