package io.github.redwolfrecovery.redwolf;

import android.app.AlarmManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

/**
 * Created by Devil7DK on 4/16/18
 **/
public class SettingsActivity extends AppCompatActivity {

    public static String KEY_AUTO_UPDATE = "auto_update";

    RadioGroup AutoUpdateInterval;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.title_settings);
            actionBar.setLogo(R.drawable.ic_settings);
            actionBar.setDisplayUseLogoEnabled(true);
        }
        setContentView(R.layout.activity_redwolf_settings);
        AutoUpdateInterval = (RadioGroup)  findViewById(R.id.rg_autocheck_interval);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        LoadPreferences();
        AssignEvents();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void AssignEvents(){
        AutoUpdateInterval.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                RadioButton selected = (RadioButton) group.findViewById(checkedId);
                switch(selected.getId()){
                    case R.id.rb_disabled:{
                        Schedule(0);
                        break;
                    }
                    case R.id.rb_halfday:{
                        Schedule(1);
                        break;
                    }
                    case R.id.rb_day:{
                        Schedule(2);
                        break;
                    }
                    case R.id.rb_2day:{
                        Schedule(3);
                        break;
                    }
                    case R.id.rb_week:{
                        Schedule(4);
                        break;
                    }
                    case R.id.rb_onboot:{
                        Schedule(5);
                        break;
                    }
                }
            }
        });
    }

    private void LoadPreferences(){

        int updateInterval = sharedPreferences.getInt(KEY_AUTO_UPDATE,0);
        switch (updateInterval){
            case 0:{
                AutoUpdateInterval.check(R.id.rb_disabled);
                break;
            }
            case 1:{
                AutoUpdateInterval.check(R.id.rb_halfday);
                break;
            }
            case 2:{
                AutoUpdateInterval.check(R.id.rb_day);
                break;
            }
            case 3:{
                AutoUpdateInterval.check(R.id.rb_2day);
                break;
            }
            case 4:{
                AutoUpdateInterval.check(R.id.rb_week);
                break;
            }
            case 5:{
                AutoUpdateInterval.check(R.id.rb_onboot);
                break;
            }
        }
    }

    private void Schedule(int intervalIndex) {
        long intervalValue;
        switch (intervalIndex) {
            case 0:
                intervalValue = 0;
                break;
            case 1:
                intervalValue = AlarmManager.INTERVAL_HALF_DAY;
                break;
            case 2:
                intervalValue = AlarmManager.INTERVAL_DAY;
                break;
            case 3:
                intervalValue = AlarmManager.INTERVAL_DAY * 2;
                break;
            case 4:
                intervalValue = AlarmManager.INTERVAL_DAY * 7;
                break;
            case 5:
                intervalValue = 0;
                break;
            default:
                intervalValue =0;
                break;
        }

        sharedPreferences.edit().putInt(KEY_AUTO_UPDATE, intervalIndex).apply();

        JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);

        if (intervalValue > 0) {
            if(jobScheduler != null) {
                jobScheduler.cancelAll();
                jobScheduler.schedule(new JobInfo.Builder(0, new ComponentName(this, CheckForUpdateService.class))
                        .setPeriodic(intervalValue)
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .setPersisted(true)
                        .build());
                Toast.makeText(this, R.string.auto_update_enabled, Toast.LENGTH_LONG).show();
            }
        } else {
            if(jobScheduler != null)jobScheduler.cancelAll();
            if(intervalIndex == 0){
                Toast.makeText(this, R.string.auto_update_disabled, Toast.LENGTH_LONG).show();
            }
        }
    }

}
