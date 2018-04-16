package io.github.redwolfrecovery.redwolf;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

/**
 * Created by Devil7DK on 4/16/18
 **/
public class CheckForUpdateService extends JobService {
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Fabric.with(this, new Crashlytics());
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String device_name = preferences.getString("device_name","");
        Log.i("RedWolf AutoUpdate","Checking updates for : " + device_name);
        if(!device_name.equals("")){
            CheckForUpdatesTask task = CheckForUpdatesTask.getInstance(this, MainActivity.XML_URL, device_name, null, true);
            if(task.getStatus() != AsyncTask.Status.RUNNING){
                task.execute(this);
            }
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
