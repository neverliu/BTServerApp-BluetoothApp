package com.mega.btserverapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;


import com.mega.btserverapp.service.BluetoothControlService;

public class MainActivity extends Activity {

   private Intent mIntent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkReadPermission();
        setContentView(R.layout.activity_main);
        Intent intent = new Intent(MainActivity.this, BluetoothControlService.class);
        startService(intent);
       // finish();


    }

    private void checkReadPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int checkReadContactsPermission = checkSelfPermission( Manifest.permission.READ_CONTACTS);

            Log.i("MainActivity","checkperdmissons ");
            if (checkReadContactsPermission != PackageManager.PERMISSION_GRANTED ) {
                Log.i("    ","requestPermissions");
                requestPermissions( new String[]{Manifest.permission.CALL_PHONE,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.WRITE_CONTACTS ,
                        Manifest.permission.ACCESS_COARSE_LOCATION ,
                        Manifest.permission.READ_SMS}, 1);
            }

        if(!Settings.System.canWrite(this)) {
            requestWriteSettings();
        }
        }
    }

    private void requestWriteSettings() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, 1 );
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,  String[] permissions,  int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       //stopService(mIntent);
    }
}
