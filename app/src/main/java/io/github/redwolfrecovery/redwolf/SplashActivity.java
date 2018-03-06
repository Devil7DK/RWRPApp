package io.github.redwolfrecovery.redwolf;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    boolean Perms=true;
    boolean askedForPermission=false;
    AlertDialog.Builder dlgAlert;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean SU;
        SU=Utils.isRootAvailable();
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {Perms = false;}
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {Perms = false;}
        dlgAlert = new AlertDialog.Builder(this);
        if(SU==false) {
            dlgAlert.setMessage(R.string.su_error);
            dlgAlert.setTitle(R.string.error);
            dlgAlert.setPositiveButton(R.string.string_okay,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
            dlgAlert.setCancelable(false);
            dlgAlert.create().show();
        }else{
            if(Perms==false){
                String Per[]={Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE};
                if(askedForPermission == false){ActivityCompat.requestPermissions(this,Per,1);askedForPermission = true;}
            }
            if(Perms==true){
                final Intent intent = new Intent(this, MainActivity.class);
                Utils.delay(1, new Utils.DelayCallback() {
                    @Override
                    public void afterDelay() {
                        startActivity(intent);
                        finish();
                    }
                });
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    final Intent intent = new Intent(this, MainActivity.class);
                    Utils.delay(1, new Utils.DelayCallback() {
                        @Override
                        public void afterDelay() {
                            startActivity(intent);
                            finish();
                        }
                    });
                } else {
                    dlgAlert.setMessage(R.string.fs_error);
                    dlgAlert.setTitle(R.string.error);
                    dlgAlert.setPositiveButton(R.string.string_okay,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            });
                    dlgAlert.setCancelable(false);
                    dlgAlert.create().show();
                }
                return;
            }
        }
    }
}