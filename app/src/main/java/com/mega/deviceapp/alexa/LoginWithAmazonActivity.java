/**
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * <p>
 * You may not use this file except in compliance with the License. A copy of the License is located the "LICENSE.txt"
 * file accompanying this source. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.mega.deviceapp.alexa;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mega.deviceapp.R;

import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.api.authorization.AuthCancellation;
import com.amazon.identity.auth.device.api.authorization.AuthorizationManager;
import com.amazon.identity.auth.device.api.authorization.AuthorizeListener;
import com.amazon.identity.auth.device.api.authorization.AuthorizeRequest;
import com.amazon.identity.auth.device.api.authorization.AuthorizeResult;
import com.amazon.identity.auth.device.api.authorization.ScopeFactory;
import com.amazon.identity.auth.device.api.workflow.RequestContext;
import com.mega.deviceapp.activity.WifiConfigureActivity;
import com.mega.deviceapp.service.BluetoothChatService;
import com.mega.deviceapp.service.BluetoothSocketService;
import com.mega.deviceapp.util.BluetoothCmd;
import com.mega.deviceapp.util.MessageCode;
import com.mega.deviceapp.util.ToastUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LoginWithAmazonActivity extends AppCompatActivity {

    private static final String TAG = LoginWithAmazonActivity.class.getName();
    private static final String ALEXA_ALL_SCOPE = "alexa:all";
    private static final String DEVICE_SERIAL_NUMBER = "deviceSerialNumber";
    private static final String PRODUCT_INSTANCE_ATTRIBUTES = "productInstanceAttributes";
    private static final String PRODUCT_ID = "productID";

    private static final String BUNDLE_KEY_EXCEPTION = "exception";

    private static final int MIN_CONNECT_PROGRESS_TIME_MS = 1 * 1000;

    private ProvisioningClient mProvisioningClient;
    private DeviceProvisioningInfo mDeviceProvisioningInfo;
    private RequestContext mRequestContext;

    //    private EditText mAddressTextView;
    private Button mConnectButton;
    private ProgressBar mConnectProgress;
    private ProgressBar mLoginProgress;
    private ImageButton mLoginButton;
    private TextView mLoginMessage, mWelcomeText, thisWifiName, serverWifiName, step2Text;
    private Messenger mMessage, sMessage;
    private Intent mIntent;
    private int sWifiSate = -1;
    private String mConnectedDeviceName;
    private WifiManager mWiFiManager;
    private String clientWifiAddress = null;
    private Toolbar mToolBar;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MessageCode.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            Log.d(TAG, "connection to " + mConnectedDeviceName);
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            Log.d(TAG, "connecting");
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            Log.d(TAG, "not connect");
                            break;
                    }
                    break;
                case MessageCode.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    // receive(readMessage);
                    Log.i(TAG, "message_read  readmessage = " + readMessage);
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    checkReadMessage(msg);
                    break;
                case MessageCode.MESSAGE_DEVICE_NAME:
                    mConnectedDeviceName = msg.getData().getString(MessageCode.DEVICE_NAME);
                    if (null != this) {
                        ToastUtil.showMsg(LoginWithAmazonActivity.this, "Connected to "
                                + mConnectedDeviceName);
                    }
                    break;
                case MessageCode.MESSAGE_TOAST:
                    if (null != this) {
                        Toast.makeText(LoginWithAmazonActivity.this, msg.getData().getString(MessageCode.TOAST), Toast.LENGTH_SHORT).show();
                    }
                    break;
                case MessageCode.MESSAGE_GET_SOCKET_STATE:
                    Log.i(TAG, "MESSAGE_GET_SOCKET_STATE");
                    int state = (int) msg.obj;
                    if (state == BluetoothChatService.STATE_LISTEN ||
                            state == BluetoothChatService.STATE_NONE) {
                        mWelcomeText.setText(getResources().getString(R.string.text_wifi_7));
                        serverWifiName.setText(getResources().getString(R.string.bluetooth_not_connect));
                        wifiIsNoMatch();
                    }
                    break;
            }
        }
    };

    private void checkReadMessage(Message msg) {
        byte[] readBuff = (byte[]) msg.obj;
        int command = readBuff[0];
        Log.i(TAG, "checkreadMessage : command = " + command);
        //final Message msg1 = msg;
        switch (command) {
            case BluetoothCmd.COMMAND_SCAN_WIFI:

                break;
            case BluetoothCmd.COMMAND_WIFI_INFO:
                String SSID = null;
                String ADDRESS = null;
                if (msg.arg1 > 3) {
                    int ssidLength = readBuff[3];
                    //            byte[] data = new byte[msg.arg1-3];
                    //            System.arraycopy(readBuff, 3, data, 0, data.length);
                    //            SSID = new String(data);
                    byte[] ssidData = new byte[ssidLength];
                    System.arraycopy(readBuff, 4, ssidData, 0, ssidLength);
                    SSID = new String(ssidData);
                    Log.d(TAG, "setWifiStatus:" + SSID);

                    if (msg.arg1 > 4 + ssidLength) {
                        byte[] addressData = new byte[msg.arg1 - (4 + ssidLength)];
                        System.arraycopy(readBuff, (4 + ssidLength), addressData, 0, addressData.length);
                        ADDRESS = new String(addressData);
                        Log.d(TAG, "setWifiStatus ADDRESS:" + ADDRESS);
                    }
                }
                int statebyte = readBuff[1];
                int contectedbyte = readBuff[2];
                switch (statebyte) {
                    case BluetoothCmd.WIFI_STATE_DISABLED:
                        sWifiSate = WifiManager.WIFI_STATE_DISABLED;
                        mWelcomeText.setText(getResources().getString(R.string.text_wifi_3));
                        wifiIsNoMatch();
                        serverWifiName.setText(ADDRESS);
                        break;
                    case BluetoothCmd.WIFI_STATE_DISABLING:
                        sWifiSate = WifiManager.WIFI_STATE_DISABLING;
                        mWelcomeText.setText(getResources().getString(R.string.text_wifi_2));
                        serverWifiName.setText(getResources().getString(R.string.server_wifi_no_connect));
                        wifiIsNoMatch();
                        break;
                    case BluetoothCmd.WIFI_STATE_ENABLED:
                        sWifiSate = WifiManager.WIFI_STATE_ENABLED;
                        int status = mWiFiManager.getWifiState();
                        String ThisWifiAddress = null;
                        if (status == mWiFiManager.WIFI_STATE_ENABLED) {
                            ThisWifiAddress = getWifiAddress();
                        } else {
                            mWelcomeText.setText(getResources().getString(R.string.text_wifi_4));
                            wifiIsNoMatch();
                        }
                        if (ADDRESS != null && ThisWifiAddress != null) {
                            Log.d(TAG, "liuhao ADDRESS:" + ADDRESS + " ThisWifiAddress:" + ThisWifiAddress);
                            serverWifiName.setText(ADDRESS);
                            String AddressStr[] = ADDRESS.split("\\.");
                            String ThisWifiAddressStr[] = ThisWifiAddress.split("\\.");
                            try {
                                if (AddressStr[0].equals(ThisWifiAddressStr[0]) &&
                                        AddressStr[1].equals(ThisWifiAddressStr[1]) &&
                                        AddressStr[2].equals(ThisWifiAddressStr[2])) {
                                    Log.d(TAG, "liuhao ADDRESS" + ADDRESS);
                                    clientWifiAddress = "http://" + ADDRESS + ":8443";
                                    mWelcomeText.setText(getResources().getString(R.string.text_wifi_5));
                                    mConnectButton.setBackgroundColor(getResources().getColor(R.color.RoyalBlue));
                                    mConnectButton.setEnabled(true);
                                    mLoginButton.setImageResource(R.mipmap.btnlwa_gray_loginwithamazon);
                                    mLoginButton.setEnabled(false);
                                } else {
                                    Log.d(TAG, "liuhao 2");
                                    mWelcomeText.setText(getResources().getString(R.string.text_wifi_6));
                                    wifiIsNoMatch();
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "liuhao 3");
                                mWelcomeText.setText(getResources().getString(R.string.text_wifi_6));
                                wifiIsNoMatch();
                                e.printStackTrace();
                            }
                        } else {
                            Log.d(TAG, "liuhao else ADDRESS:" + ADDRESS + " ThisWifiAddress:" + ThisWifiAddress);
                            mWelcomeText.setText(getResources().getString(R.string.text_wifi_4));
                            serverWifiName.setText(ADDRESS);
                            wifiIsNoMatch();
                        }
                        break;
                    case BluetoothCmd.WIFI_STATE_ENABLING:
                        sWifiSate = WifiManager.WIFI_STATE_ENABLING;
                        int status1 = mWiFiManager.getWifiState();
                        String ThisWifiAddress1 = null;
                        if (status1 == mWiFiManager.WIFI_STATE_ENABLED) {
                            ThisWifiAddress1 = getWifiAddress();
                        } else {
                            mWelcomeText.setText(getResources().getString(R.string.text_wifi_4));
                            mConnectButton.setBackgroundColor(getResources().getColor(R.color.yellow_S));
                            mConnectButton.setEnabled(false);
                        }
                        if (contectedbyte == BluetoothCmd.WIFI_CONTECTED && SSID != null) {
                            if (ADDRESS != null && ThisWifiAddress1 != null) {
                                Log.d(TAG, "liuhao 1ADDRESS:" + ADDRESS + " ThisWifiAddress:" + ThisWifiAddress1);
                                if (ADDRESS == ThisWifiAddress1) {
                                    mWelcomeText.setText(getResources().getString(R.string.text_wifi_5));
                                    mConnectButton.setBackgroundColor(getResources().getColor(R.color.RoyalBlue));
                                    mConnectButton.setEnabled(true);
                                } else {
                                    mWelcomeText.setText(getResources().getString(R.string.text_wifi_6));
                                    mConnectButton.setBackgroundColor(getResources().getColor(R.color.yellow_S));
                                    mConnectButton.setEnabled(false);
                                }

                            } else {
                                Log.d(TAG, "liuhao 1else ADDRESS:" + ADDRESS + " ThisWifiAddress:" + ThisWifiAddress1);
                                mWelcomeText.setText(getResources().getString(R.string.text_wifi_4));
                                mConnectButton.setBackgroundColor(getResources().getColor(R.color.yellow_S));
                                mConnectButton.setEnabled(false);
                            }
                        }
                        break;
                    default:
                        sWifiSate = WifiManager.WIFI_STATE_UNKNOWN;
                        break;
                }
                Log.d(TAG, "liuhao COMMAND_WIFI_INFO wifisate = " + sWifiSate);
                switch (sWifiSate) {
                    case WifiManager.WIFI_STATE_ENABLED:
                        break;
                    case WifiManager.WIFI_STATE_DISABLED:
                        break;
                    default:
                        break;
                }
                break;
            case BluetoothCmd.COMMAND_LINK_WIFI:
                if (readBuff[1] == BluetoothCmd.SEND_ACK) {
                    Toast.makeText(this, getResources().getString(R.string.toast_configuration_success), Toast.LENGTH_SHORT).show();

                } else if (readBuff[1] == BluetoothCmd.SEND_ERROR) {
                    Toast.makeText(this, getResources().getString(R.string.toast_configuration_fail), Toast.LENGTH_SHORT).show();
                }
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setWifiStateCommand();
                    }
                }, 500);
                break;
            default:
                String readMessage = new String(readBuff, 0, msg.arg1);
                Log.i(TAG, "checkReadMessage other msg : " + readMessage);
                break;
        }
    }

    private void wifiIsNoMatch() {
        mConnectButton.setBackgroundColor(getResources().getColor(R.color.yellow_S));
        mConnectButton.setEnabled(false);
        mLoginButton.setImageResource(R.mipmap.btnlwa_gray_loginwithamazon);
        mLoginButton.setEnabled(false);
        clientWifiAddress = null;
    }

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "onServiceConnected");
            sMessage = new Messenger(service);
            Message msg = new Message();
            msg.what = MessageCode.MESSAGE_BIND_SERVICE;
            msg.replyTo = mMessage;
            try {
                sMessage.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            setWifiStateCommand();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "onServiceDisconnected");
            sMessage = null;
        }
    };

    //发送获取WiFi command
    private void setWifiStateCommand() {
        Message message = new Message();
        byte[] command = new byte[2];
        message.what = MessageCode.MESSAGE_WRITE_TO_SERVICE;
        command[0] = BluetoothCmd.COMMAND_WIFI_INFO;
        message.obj = command;
        try {
            sMessage.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRequestContext = RequestContext.create(this);
        mRequestContext.registerListener(new AuthorizeListenerImpl());

        setContentView(R.layout.lwa_activity);

        //        mAddressTextView = (EditText) findViewById(R.id.addressTextView);

        mConnectButton = (Button) findViewById(R.id.connectButton);
        mConnectProgress = (ProgressBar) findViewById(R.id.connectProgressBar);

        mLoginButton = (ImageButton) findViewById(R.id.loginButton);
        mLoginProgress = (ProgressBar) findViewById(R.id.loginProgressBar);
        mLoginMessage = (TextView) findViewById(R.id.loginMessage);
        thisWifiName = (TextView) findViewById(R.id.this_wifi_name);
        step2Text = (TextView) findViewById(R.id.step_2_text);
        serverWifiName = (TextView) findViewById(R.id.server_wifi_name);
        serverWifiName.setText(getResources().getString(R.string.server_no_wifi_ssid));
        mWelcomeText = (TextView) findViewById(R.id.welcome);
        mWiFiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        int status = mWiFiManager.getWifiState();
        String ThisWifiAddress = null;
        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
        String result = preferences.getString(getString(R.string.saved_device_address), null);
        Log.d(TAG,"liuhao oncreate result:"+result);
        if (result != null) {
            String time = preferences.getString("time", null);
            mLoginMessage.setVisibility(View.VISIBLE);
            mLoginMessage.setText(getResources().getString(R.string.login_message_1,time,result));
        }
        if (status == mWiFiManager.WIFI_STATE_ENABLED) {
            ThisWifiAddress = getWifiAddress();
            thisWifiName.setText(ThisWifiAddress);
        } else {
            thisWifiName.setText(getResources().getString(R.string.this_wifi_no_connect));
        }
        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mToolBar.setTitle(R.string.alexa);
        setSupportActionBar(mToolBar);
        final android.support.v7.app.ActionBar ab = getSupportActionBar();
        ab.setHomeAsUpIndicator(R.mipmap.ic_actionbar_back);
        ab.setDisplayHomeAsUpEnabled(true);

        mMessage = new Messenger(mHandler);
        bindService();
        connectCleanState();

        try {
            mProvisioningClient = new ProvisioningClient(this);
        } catch (Exception e) {
            connectErrorState();
            showAlertDialog(e);
            Log.e(TAG, "Unable to use Provisioning Client. CA Certificate is incorrect or does not exist.", e);
        }

        String savedDeviceAddress = getPreferences(Context.MODE_PRIVATE).getString(getString(R.string.saved_device_address), null);
        if (savedDeviceAddress != null) {
            //            mAddressTextView.setText(savedDeviceAddress);
        }

        mConnectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                //                final String address = mAddressTextView.getText().toString();
                final String address = clientWifiAddress;
                Log.e("address", "+address=" + address);
                mProvisioningClient.setEndpoint(address);

                new AsyncTask<Void, Void, DeviceProvisioningInfo>() {
                    private Exception errorInBackground;

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        connectInProgressState();
                    }

                    @Override
                    protected DeviceProvisioningInfo doInBackground(Void... voids) {
                        try {
                            long startTime = System.currentTimeMillis();
                            DeviceProvisioningInfo response = mProvisioningClient.getDeviceProvisioningInfo();
                            long duration = System.currentTimeMillis() - startTime;

                            if (duration < MIN_CONNECT_PROGRESS_TIME_MS) {
                                try {
                                    Thread.sleep(MIN_CONNECT_PROGRESS_TIME_MS - duration);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                            return response;
                        } catch (Exception e) {
                            errorInBackground = e;
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(DeviceProvisioningInfo deviceProvisioningInfo) {
                        super.onPostExecute(deviceProvisioningInfo);
                        Log.d(TAG,"liuhao onPostExecute");
                        if (deviceProvisioningInfo != null) {
                            mDeviceProvisioningInfo = deviceProvisioningInfo;

                            SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();
                            editor.putString(getString(R.string.saved_device_address), address);
                            String time = getCurrentTime();
                            editor.putString("time", time);
                            editor.commit();
                            mLoginMessage.setText(getResources().getString(R.string.login_message_1,time,address));
                            connectSuccessState();
                        } else {
                            connectCleanState();
                            showAlertDialog(errorInBackground);
                        }
                    }
                }.execute();
            }
        });

        mLoginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                loginInProgressState();

                final JSONObject scopeData = new JSONObject();
                final JSONObject productInstanceAttributes = new JSONObject();
                final String codeChallenge = mDeviceProvisioningInfo.getCodeChallenge();
                final String codeChallengeMethod = mDeviceProvisioningInfo.getCodeChallengeMethod();

                try {
                    productInstanceAttributes.put(DEVICE_SERIAL_NUMBER, mDeviceProvisioningInfo.getDsn());
                    scopeData.put(PRODUCT_INSTANCE_ATTRIBUTES, productInstanceAttributes);
                    scopeData.put(PRODUCT_ID, mDeviceProvisioningInfo.getProductId());

                    AuthorizationManager.authorize(new AuthorizeRequest.Builder(mRequestContext)
                            .addScope(ScopeFactory.scopeNamed(ALEXA_ALL_SCOPE, scopeData))
                            .forGrantType(AuthorizeRequest.GrantType.AUTHORIZATION_CODE)
                            .withProofKeyParameters(codeChallenge, codeChallengeMethod)
                            .build());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private String getCurrentTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        Date curDate = new Date(System.currentTimeMillis());
        return formatter.format(curDate);
    }

    // 得到IP地址
    public String getWifiAddress() {
        String ip = null;
        if (mWiFiManager.isWifiEnabled()) {
            WifiInfo wifiInfo = mWiFiManager.getConnectionInfo();
            int i = wifiInfo.getIpAddress();
            ip = (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
                    + "." + (i >> 24 & 0xFF);
        }
        return ip;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRequestContext.onResume();
        updateSocketStatus();
    }

    private void updateSocketStatus() {
        Message message = new Message();
        message.what = MessageCode.MESSAGE_GET_SOCKET_STATE;
        Log.i(TAG, "updateSocketStatus sMessage = " + sMessage);
        if (sMessage != null) {
            try {
                sMessage.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
    }

    private void bindService() {
        mIntent = new Intent(this, BluetoothSocketService.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);
    }

    private void connectCleanState() {
        mConnectButton.setVisibility(View.VISIBLE);
        mConnectProgress.setVisibility(View.GONE);
        step2Text.setText(getResources().getString(R.string.main_noconnect));
        //        mLoginButton.setVisibility(View.GONE);
        mLoginButton.setEnabled(false);
        mLoginButton.setImageResource(R.mipmap.btnlwa_gray_loginwithamazon);
        mLoginProgress.setVisibility(View.GONE);
//        mLoginMessage.setVisibility(View.GONE);
    }

    private void connectInProgressState() {
        mConnectButton.setVisibility(View.GONE);
        mConnectProgress.setVisibility(View.VISIBLE);
        mConnectProgress.setIndeterminate(true);
//        step2Text.setText(getResources().getString(R.string.main_noconnect));
        //        mLoginButton.setVisibility(View.GONE);
        mLoginButton.setEnabled(false);
        mLoginButton.setImageResource(R.mipmap.btnlwa_gray_loginwithamazon);
        mLoginProgress.setVisibility(View.GONE);
//        mLoginMessage.setVisibility(View.GONE);
    }

    private void connectSuccessState() {
        mConnectButton.setVisibility(View.VISIBLE);
        mConnectProgress.setVisibility(View.GONE);
        step2Text.setText(getResources().getString(R.string.step2_text));
        //        mLoginButton.setVisibility(View.VISIBLE);
        mLoginButton.setEnabled(true);
        mLoginButton.setImageResource(R.mipmap.btnlwa_gold_loginwithamazon);
        mLoginProgress.setVisibility(View.GONE);
//        mLoginMessage.setVisibility(View.GONE);
    }

    private void connectErrorState() {
        mConnectButton.setVisibility(View.GONE);
        mConnectProgress.setVisibility(View.GONE);
        step2Text.setText(getResources().getString(R.string.main_noconnect));
        //        mLoginButton.setVisibility(View.GONE);
        mLoginButton.setEnabled(false);
        mLoginButton.setImageResource(R.mipmap.btnlwa_gray_loginwithamazon);
        mLoginProgress.setVisibility(View.GONE);
//        mLoginMessage.setVisibility(View.GONE);
    }

    private void loginInProgressState() {
        mConnectButton.setVisibility(View.VISIBLE);
        mConnectProgress.setVisibility(View.GONE);
//        step2Text.setText(getResources().getString(R.string.main_noconnect));
        //        mLoginButton.setVisibility(View.GONE);
        mLoginButton.setEnabled(false);
        mLoginButton.setImageResource(R.mipmap.btnlwa_gray_loginwithamazon);
        mLoginProgress.setVisibility(View.VISIBLE);
//        mLoginMessage.setVisibility(View.GONE);
    }

    private void loginSuccessState() {
        mConnectButton.setVisibility(View.VISIBLE);
        mConnectProgress.setVisibility(View.GONE);

        //        mLoginButton.setVisibility(View.GONE);
        mLoginButton.setEnabled(false);
        mLoginButton.setImageResource(R.mipmap.btnlwa_gray_loginwithamazon);
        mLoginProgress.setVisibility(View.GONE);
        mLoginMessage.setVisibility(View.VISIBLE);
        mLoginMessage.setText(R.string.success_message);
    }

    protected void showAlertDialog(Exception exception) {
        exception.printStackTrace();
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(BUNDLE_KEY_EXCEPTION, exception);
        dialogFragment.setArguments(args);
        FragmentManager fm = getSupportFragmentManager();
        dialogFragment.show(fm, "error_dialog");
    }

    private class AuthorizeListenerImpl extends AuthorizeListener {
        @Override
        public void onSuccess(final AuthorizeResult authorizeResult) {
            final String authorizationCode = authorizeResult.getAuthorizationCode();
            final String redirectUri = authorizeResult.getRedirectURI();
            final String clientId = authorizeResult.getClientId();
            final String sessionId = mDeviceProvisioningInfo.getSessionId();
            Log.d(TAG, "liuhao authorizationCode:" + authorizationCode + " redirectUri:" + redirectUri + " clientId:" + clientId + " sessionId:" + sessionId);
            final CompanionProvisioningInfo companionProvisioningInfo = new CompanionProvisioningInfo(sessionId, clientId, redirectUri, authorizationCode);

            new AsyncTask<Void, Void, Void>() {
                private Exception errorInBackground;

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    loginInProgressState();
                }

                @Override
                protected Void doInBackground(Void... voids) {
                    try {
                        mProvisioningClient.postCompanionProvisioningInfo(companionProvisioningInfo);
                    } catch (Exception e) {
                        errorInBackground = e;
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    super.onPostExecute(result);
                    Log.d(TAG,"liuhao onPostExecute:"+(errorInBackground != null));
                    if (errorInBackground != null) {
                        connectCleanState();
                        showAlertDialog(errorInBackground);
                    } else {
                        loginSuccessState();
                    }
                }
            }.execute();
        }

        @Override
        public void onError(final AuthError authError) {
            Log.e(TAG, "AuthError during authorization", authError);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showAlertDialog(authError);
                }
            });
        }

        @Override
        public void onCancel(final AuthCancellation authCancellation) {
            Log.e(TAG, "User cancelled authorization");
        }
    }

    public static class ErrorDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Bundle args = getArguments();
            Exception exception = (Exception) args.getSerializable(BUNDLE_KEY_EXCEPTION);
            String message = exception.getMessage();

            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.error)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dismiss();
                        }
                    })
                    .create();
        }
    }
}