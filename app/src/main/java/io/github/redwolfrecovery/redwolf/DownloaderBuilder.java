package io.github.redwolfrecovery.redwolf;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Func;
import com.tonyodev.fetch2.Func2;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2.Status;
import com.tonyodev.fetch2downloaders.OkHttpDownloader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

import okhttp3.OkHttpClient;

/**
 * Created by Devil7DK on 4/3/18
 **/
public class DownloaderBuilder extends AlertDialog.Builder implements FetchListener{

    private Activity mActivity;
    private AlertDialog mDialog;

    private String mURL;
    private String mFilePath;
    private String mFileName;
    private Status downloadStatus;
    private int downloadID;

    private ProgressBar progressBar;
    private TextView txt_percentage;
    private TextView txt_remaining;
    private Button btn_cancel;
    private Button btn_pause;

    private static Request fetch_request;
    private static Fetch fetch_instance;
    private static final String FETCH_NAMESPACE = "RedWolfFetch";


    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private Boolean ActivityPaused=false;

    @Override
    public AlertDialog create() {
        LayoutInflater inflater = mActivity.getLayoutInflater();
        View layout = inflater.inflate(R.layout.dialog_downloader, null);
        super.setTitle(R.string.downloader_title);
        super.setCancelable(false);
        super.setView(layout);
        mDialog = super.create();
        mDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                startDownload();
            }
        });
        assignObjects(layout);
        assignEvents();
        return mDialog;
    }

    public DownloaderBuilder(Activity activity, String url, String filePath, String fileName) {
        super(activity);
        mActivity = activity;
        mURL = url;
        mFilePath = filePath;
        mFileName = fileName;
    }

    private void assignObjects(View view){
        progressBar = (ProgressBar) view.findViewById(R.id.download_progress);
        txt_percentage = (TextView) view.findViewById(R.id.download_percentage);
        txt_remaining = (TextView) view.findViewById(R.id.download_remaining);
        btn_cancel = (Button) view.findViewById(R.id.download_cancel);
        btn_pause = (Button) view.findViewById(R.id.download_pause);
        fetch_instance =  new Fetch.Builder(getContext().getApplicationContext(), FETCH_NAMESPACE)
                .setDownloader(new OkHttpDownloader(new OkHttpClient.Builder().build()))
                .setDownloadConcurrentLimit(1)
                .enableLogging(true)
                .build();
        mNotifyManager = (NotificationManager) getContext().getApplicationContext().getSystemService(Activity.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(getContext().getApplicationContext());
        mBuilder.setDefaults(Notification.DEFAULT_LIGHTS);
    }

    private void assignEvents(){
        btn_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseDownload();
            }
        });
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelDownload();
            }
        });
    }

    //-----------------------Events----------------------
    private void cancelDownload(){
        fetch_instance.cancel(downloadID);
    }

    private void pauseDownload(){
        if(downloadStatus == Status.DOWNLOADING){
            btn_pause.setText(R.string.resume);
            fetch_instance.pause(downloadID);
        }else{
            btn_pause.setText(R.string.pause);
            try{
                fetch_instance.resume(downloadID);
            }catch(Exception ex){
                ex.printStackTrace();
            }
        }
    }
    //-----------------------Events----------------------

    //---------------------Functions---------------------
    private void startDownload(){
        File file = new File(mFilePath);

        if(file.exists()){
            fetch_instance.deleteGroup(Status.CANCELLED.getValue());
            fetch_instance.deleteGroup(Status.FAILED.getValue());
            fetch_instance.removeGroup(Status.COMPLETED.getValue());
        }
        else{
            fetch_instance.removeAll();
        }

        fetch_request = new Request(mURL,mFilePath);
        fetch_instance.getDownload(fetch_request.getId(), new Func2<Download>() {
            @Override
            public void call(@Nullable Download download) {
                if (download == null) {
                    fetch_instance.enqueue(fetch_request, new Func<Download>() {
                        @Override
                        public void call(@NotNull Download download) {
                            updateProgress(download.getStatus(),download.getProgress(),0);
                        }
                    }, new Func<Error>() {
                        @Override
                        public void call(@NotNull Error error) {
                            Log.d("RedWolf", "Error:" + error.toString());
                        }
                    });
                } else {
                    fetch_request = download.getRequest();
                    downloadStatus = download.getStatus();
                    downloadID = download.getId();
                    updateProgress(download.getStatus(),download.getProgress(),0);
                }
            }
        });
        fetch_instance.addListener(this);
        initNotification();
    }

    public void ActivityResumed(){
        ActivityPaused = false;
        mNotifyManager.cancel(1);
    }

    public void ActivityPaused(){
        ActivityPaused = true;
        if(downloadStatus != null){
            switch (downloadStatus) {
                case QUEUED:{
                    setNotificationIntermediate();
                    break;
                }
                case PAUSED: {
                    setNotificationPaused();
                    break;
                }
                case DOWNLOADING: {
                    break;
                }
            }
        }
    }

    private void updateProgress(Status status, int progress,long etaInMilliseconds){
        String eta = Utils.getETAString(getContext(), etaInMilliseconds);
        txt_remaining.setText(eta);
        switch (status) {
            case QUEUED:{
                txt_percentage.setText("");
                progressBar.setIndeterminate(true);
                if(ActivityPaused){
                    setNotificationIntermediate();
                }
                break;
            }
            case PAUSED: {
                btn_pause.setText(R.string.resume);
                if(ActivityPaused){
                    setNotificationPaused();
                }
                break;
            }
            case DOWNLOADING:{
                String progressString = progress + "%" ;
                txt_percentage.setText(progressString);
                progressBar.setIndeterminate(false);
                progressBar.setProgress(progress);
                if(ActivityPaused){
                    setNotificationProgress(progressString, progress, eta);
                }
            }
        }
    }
    //---------------------Functions---------------------

    //--------------FetchListener Overrides-------------
    @Override
    public void onQueued(@NotNull Download download) {
        if (fetch_request != null && fetch_request.getId() == download.getId()) {
            downloadID = download.getId();
            downloadStatus = download.getStatus();
            updateProgress(download.getStatus(),download.getProgress(),0);
        }
    }

    @Override
    public void onCompleted(@NotNull Download download) {
        if (fetch_request != null && fetch_request.getId() == download.getId()) {
            downloadID = download.getId();
            downloadStatus = download.getStatus();
            fetch_instance.close();
            if(mDialog.isShowing()){mDialog.dismiss();}
            if(ActivityPaused){setNotificationComplete(mActivity.getString(R.string.notification_completed));}
            AlertDialog.Builder builder;
            builder = new AlertDialog.Builder(mActivity,R.style.Theme_AppCompat_Light_Dialog_Alert);
            builder.setTitle(R.string.download_complete_title)
                    .setMessage(R.string.download_complete_msg)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();
        }

    }

    @Override
    public void onError(@NotNull Download download) {
        if (fetch_request != null && fetch_request.getId() == download.getId()) {
            downloadID = download.getId();
            downloadStatus = download.getStatus();
            fetch_instance.close();
            if(mDialog.isShowing()){mDialog.dismiss();}
            AlertDialog.Builder builder;
            builder = new AlertDialog.Builder(mActivity,R.style.Theme_AppCompat_Light_Dialog_Alert);
            builder.setTitle(R.string.download_interrupted_title)
                    .setMessage(R.string.download_interrupted_msg)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();
        }
    }

    @Override
    public void onProgress(Download download, long etaInMilliseconds) {
        if (fetch_request != null && fetch_request.getId() == download.getId()) {
        downloadID = download.getId();
        downloadStatus = download.getStatus();
        updateProgress(download.getStatus(),download.getProgress(),etaInMilliseconds);
        }
    }

    @Override
    public void onPaused(@NotNull Download download) {
        if (fetch_request != null && fetch_request.getId() == download.getId()) {
            downloadID = download.getId();
            downloadStatus = download.getStatus();
            updateProgress(download.getStatus(),download.getProgress(),0);
        }
    }

    @Override
    public void onResumed(@NotNull Download download) {
        if (fetch_request != null && fetch_request.getId() == download.getId()) {
            downloadID = download.getId();
            downloadStatus = download.getStatus();
            updateProgress(download.getStatus(),download.getProgress(),0);
        }
    }

    @Override
    public void onCancelled(@NotNull Download download) {
        if (fetch_request != null && fetch_request.getId() == download.getId()) {
            downloadID = download.getId();
            downloadStatus = download.getStatus();
            fetch_instance.close();
            if(mDialog.isShowing()){mDialog.dismiss();}
            setNotificationComplete(mActivity.getString(R.string.notification_cancelled));
            AlertDialog.Builder builder;
            builder = new AlertDialog.Builder(mActivity,R.style.Theme_AppCompat_Light_Dialog_Alert);
            builder.setTitle(R.string.download_cancelled_title)
                    .setMessage(R.string.download_cancelled_msg)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();
        }

    }

    @Override
    public void onRemoved(@NotNull Download download) {
    }

    @Override
    public void onDeleted(@NotNull Download download) {
    }
    //--------------FetchListener Overrides-------------

    //-------------------Notification-------------------
    private void initNotification(){
        mBuilder.setContentTitle(mFileName)
                .setContentText("")
                .setSmallIcon(R.drawable.ic_download);
        Intent notificationIntent = new Intent(getContext(), MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(getContext(), 0,
                notificationIntent, 0);
        mBuilder.setContentIntent(intent);
        mBuilder.setDefaults(Notification.DEFAULT_LIGHTS);
        mBuilder.setOngoing(true);
        mBuilder.setProgress(100, 0, true);
    }
    private void setNotificationIntermediate(){
        mBuilder.setProgress(100, 0, true);
        mBuilder.setSubText(null);
        mNotifyManager.notify(1, mBuilder.build());
    }
    private void setNotificationProgress(String progressString, int progress, String eta){
        mBuilder.setContentText(progressString)
                .setSubText(eta)
                .setProgress(100, progress, false);
        mNotifyManager.notify(1, mBuilder.build());
    }
    private void setNotificationPaused(){
        mBuilder.setSubText(null)
                .setContentText(mActivity.getString(R.string.notification_paused));
        mNotifyManager.notify(1, mBuilder.build());
    }
    private void setNotificationComplete(String message){
        mBuilder.setContentText(message)
                .setAutoCancel(true)
                .setSubText(null)
                .setProgress(0,0,false);
        mBuilder.setOngoing(false);
        Intent intent = new Intent(getContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("NOTIFICATION_ID", 1);
        PendingIntent dismissIntent = PendingIntent.getActivity(getContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        mBuilder.setContentIntent(dismissIntent);
        mNotifyManager.notify(1, mBuilder.build());
    }
    //-------------------Notification-------------------
}
