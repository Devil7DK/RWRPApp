package io.github.redwolfrecovery.redwolf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

/**
 * Created by Devil7DK on 4/17/18
 **/
public class OnBootTask extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        intent.getAction();
        Fabric.with(context, new Crashlytics());
        try{
            Thread.sleep(30000); //Give some time to the system to warm up
            Log.i("RedWolf AutoUpdate","Checking whether auto update scheduled onBoot...");
            int UpdateInterval = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext()).getInt(SettingsActivity.KEY_AUTO_UPDATE,0);
            if(UpdateInterval == 5){
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
                String device_name = preferences.getString("device_name","");
                Log.i("RedWolf AutoUpdate","Checking updates for : " + device_name);
                if(!device_name.equals("")){
                    CheckForUpdatesTask task = CheckForUpdatesTask.getInstance(context, MainActivity.XML_URL, device_name, null, true);
                    if(task.getStatus() != AsyncTask.Status.RUNNING){
                        task.execute(context);
                    }
                }
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }
}
