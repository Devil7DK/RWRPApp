package io.github.redwolfrecovery.redwolf;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Devil7DK on 4/5/18
 **/
public class Downloader extends AsyncTask<String, Integer, String> {
    private String ID;

    private Activity activity;
    private DownloadTask downloadTask;

    private ProgressBar mProgressBar;
    private TextView mProgressPercentage;
    private TextView mSpeed;
    private TextView mRemainingTime;
    private Button btn_Cancel;
    private Button btn_Pause;

    private String mURL;
    private String mFilename;
    private String mBasePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
    private String mDownloadFilePath;
    private String mTempFilePath;

    boolean mIsPausing;
    long mTotalSizeDownloaded;
    long mTotalSize;
    long mCurrentSizeDownloaded;

    long mETF = -1;
    String mSpeedRate;
    boolean mUpdateETF_Speed;

    private long mLastDownloadSize;
    private long mSizeOf5Sec;

    private AlertDialog dialog;

    private boolean isPaused = false;
    private boolean isCancelled = false;

    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private String NOTIFICATION_CHANNEL = "downloader";

    public interface DownloadTask {
        void onDownloadCompleted(String ID, String FilePath);
        void onDownloadCanceled(String ID);
        void onDownloadError(String ID, String Error);
    }

    public Downloader(Activity activity, String URL) {
        this.activity = activity;
        this.mURL = URL;
        this.mFilename = URL.substring(URL.lastIndexOf('/') + 1);
        if(this.mBasePath.endsWith(File.separator)){
            mDownloadFilePath = mBasePath + mFilename;
            mTempFilePath = mBasePath + mFilename + ".part";
        }else{
            mDownloadFilePath = mBasePath + File.separator + mFilename;
            mTempFilePath = mBasePath + File.separator + mFilename + ".part";
        }
    }

    public Downloader(Activity activity, String URL, String Filename) {
        this.activity = activity;
        this.mURL = URL;
        this.mFilename = Filename;
        if(this.mBasePath.endsWith(File.separator)){
            mDownloadFilePath = mBasePath + mFilename;
            mTempFilePath = mBasePath + mFilename + ".part";
        }else{
            mDownloadFilePath = mBasePath + File.separator + mFilename;
            mTempFilePath = mBasePath + File.separator + mFilename + ".part";
        }
    }

    private void AcquirePreviousSession(){
        File file = new File(mTempFilePath);
        mTotalSizeDownloaded = file.length();
        mCurrentSizeDownloaded = 0;
        mIsPausing = file.exists();
    }

    private void Pause(){
        setNotificationPaused();
        btn_Pause.setText("Resume");
        this.isPaused = true;
    }

    private void Resume(){
        btn_Pause.setText("Pause");
        this.isPaused = false;
    }

    private void Cancel(){
        if(this.isPaused){this.isPaused = false;}
        this.isCancelled = true;
        if(dialog.isShowing()){
            dialog.dismiss();
        }
    }

    public void setDownloadTaskListener(DownloadTask listener){
        this.downloadTask = listener;
    }

    public String getDownloadFilePath(){return this.mDownloadFilePath;}

    public String getTempFilePath(){return this.mTempFilePath;}

    private void CleanUp(){
        File tmpFile = new File(mTempFilePath);
        File downFile = new File(mDownloadFilePath);
        if(tmpFile.exists())tmpFile.delete();
        if(downFile.exists())downFile.delete();
    }

    @Override
    protected String doInBackground(@NonNull String... ID) {
        this.ID = ID[0];
        PowerManager powerManager = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        wakeLock.acquire();
        Timer t = new Timer();
        AcquirePreviousSession();

        try {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;

            File BaseDir = new File(mBasePath);
            if(!BaseDir.exists()){BaseDir.mkdirs();}

            try {
                URL url = new URL(mURL);
                connection = (HttpURLConnection) url.openConnection();
                if (mIsPausing) {
                    connection.setRequestProperty("Range", "bytes=" + mTotalSizeDownloaded + "-");
                }
                connection.connect();

                int responseCode = connection.getResponseCode();

                if ( responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL){
                    if(responseCode == 416) // HTTP ERROR : 416 - Range Not Satisfiable. Occurs when the range we set is not satisfiable for server.
                    {
                        CleanUp();
                        AcquirePreviousSession();
                        connection.disconnect();
                        connection = (HttpURLConnection) url.openConnection();
                        connection.connect();
                    }else{
                        return "HTTP Error : " + responseCode;
                    }
                }

                long fileLength = - 1;
                long length = connection.getContentLength();
                Log.i("Downloader", "File Length " + length);
                if (mIsPausing) {
                    fileLength = mTotalSizeDownloaded + length;
                } else {
                    fileLength = length;
                }
                mTotalSize = fileLength;

                input = connection.getInputStream();
                if (mIsPausing)
                    output = new FileOutputStream(mTempFilePath, true);
                else
                    output = new FileOutputStream(mTempFilePath);

                byte data[] = new byte[4096];
                int count;

                long startTime = System.currentTimeMillis();
                t.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        mUpdateETF_Speed = true;
                        if(mLastDownloadSize != 0) {
                            mSizeOf5Sec = mCurrentSizeDownloaded - mLastDownloadSize;
                            mSizeOf5Sec = (getMovingTotal(mSizeOf5Sec)/2);
                        }
                        mLastDownloadSize = mCurrentSizeDownloaded;
                    }
                },0,1000);

                while ((count = input.read(data)) != -1) {
                    while(isPaused){Thread.sleep(1000);}

                    if (this.isCancelled)return null;

                    mTotalSizeDownloaded = mTotalSizeDownloaded + count;
                    mCurrentSizeDownloaded = mCurrentSizeDownloaded + count;

                    long endTime = System.currentTimeMillis();
                    long rate = 0;
                    try{if(mSizeOf5Sec == 0){rate=(((mCurrentSizeDownloaded) / ((endTime - startTime) / 1000)));}else{rate=(((mSizeOf5Sec) / 5));}}catch(Exception ex){}
                    rate = (long)(Math.round( rate * 100.0 ) / 100.0);
                    mSpeedRate = Utils.getDownloadSpeedString(activity, rate);

                    if (fileLength > 0)
                    {
                        try{mETF = Math.round(( (fileLength - mTotalSizeDownloaded) / rate) * 1000);}catch(Exception ex){}
                        publishProgress((int) (mTotalSizeDownloaded * 100 / fileLength));
                    }else{publishProgress(-1);}

                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                }
                catch (IOException ignored) { }

                if (connection != null)
                    connection.disconnect();
            }
        } finally {
            t.cancel();
            wakeLock.release();
        }
        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        LayoutInflater inflater = activity.getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.download_item, null);
        mProgressBar = (ProgressBar) alertLayout.findViewById(R.id.progressBar);
        mProgressPercentage = (TextView) alertLayout.findViewById(R.id.progress_TextView);
        mSpeed = (TextView) alertLayout.findViewById(R.id.downloadSpeedTextView);
        mRemainingTime = (TextView) alertLayout.findViewById(R.id.remaining_TextView);
        btn_Cancel = (Button) alertLayout.findViewById(R.id.btn_cancel);
        btn_Pause = (Button) alertLayout.findViewById(R.id.btn_pause);

        for(int i = 0; i<10; i++ ){
            mMovingTotal[i] = 0;
        }

        mProgressBar.getIndeterminateDrawable().setColorFilter(activity.getResources().getColor(R.color.colorPrimaryDark,null),android.graphics.PorterDuff.Mode.MULTIPLY);

        btn_Cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Cancel();
            }
        });
        btn_Pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isPaused){
                    Resume();
                }else{
                    Pause();
                }
            }
        });

        initNotification();
        setNotificationIntermediate();

        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
        alert.setTitle("Downloading");
        alert.setView(alertLayout);
        alert.setCancelable(false);
        dialog = alert.create();
        dialog.show();
    }


    long[] mMovingTotal = new long[10];
    private long getMovingTotal(long currentValue){
        long total = 0;
        for(int i =1; i < 10; i++){
            mMovingTotal[i-1] = mMovingTotal[i];
            total += mMovingTotal[i];
        }
        total += currentValue;
        mMovingTotal[9] = currentValue;
        return total;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        super.onProgressUpdate(progress);
        if(progress[0]>=0){
            mProgressBar.setIndeterminate(false);
            mProgressBar.setMax(100);
            mProgressBar.setProgress(progress[0]);
            mProgressPercentage.setText(progress[0] + "%");

            if(mUpdateETF_Speed){
                String etastring = Utils.getETAString(activity, mETF);
                mSpeed.setText(mSpeedRate);
                mRemainingTime.setText(etastring);
                mUpdateETF_Speed = false;
                setNotificationProgress(progress[0] + "%", progress[0], etastring);
            }
        }else{
            mProgressBar.setIndeterminate(true);
            mProgressBar.setMax(100);
            mProgressBar.setProgress(progress[0]);
            mProgressPercentage.setText("");
            if(mUpdateETF_Speed){
                mSpeed.setText(mSpeedRate);
                mRemainingTime.setText("");
                mUpdateETF_Speed = false;
                setNotificationProgress(mSpeedRate);
            }
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if(dialog.isShowing()){
            dialog.dismiss();
        }

        if (result != null) {
            setNotificationComplete(activity.getString(R.string.notification_error));
            downloadTask.onDownloadError(this.ID,result);
        }else{
            if (isCancelled) {
                setNotificationComplete(activity.getString(R.string.notification_cancelled));
                downloadTask.onDownloadCanceled(this.ID);
            }else{
                setNotificationComplete(activity.getString(R.string.notification_completed));
                try{
                    File temp = new File(mTempFilePath);
                    File download = new File(mDownloadFilePath);
                    if(download.exists())download.delete();
                    temp.renameTo(download);
                }catch(Exception ex){
                    ex.printStackTrace();
                }
                downloadTask.onDownloadCompleted(this.ID,this.mDownloadFilePath);
            }
        }
    }

    //-------------------Notification-------------------
    private void initNotification(){
        NotificationManager notificationManager =
                (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyManager = (NotificationManager) activity.getApplicationContext().getSystemService(Activity.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(activity.getApplicationContext());

        mBuilder.setContentTitle(mFilename)
                .setContentText(null)
                .setSmallIcon(R.drawable.ic_downloader);
        Intent notificationIntent = new Intent(activity, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(activity, 0,
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

    private void setNotificationProgress(String eta){
        mBuilder.setContentText(null)
                .setSubText(eta)
                .setProgress(100, 0, true);
        mNotifyManager.notify(1, mBuilder.build());
    }

    private void setNotificationPaused(){
        mBuilder.setSubText(null)
                .setContentText(activity.getString(R.string.notification_paused));
        mNotifyManager.notify(1, mBuilder.build());
    }
    private void setNotificationComplete(String message){
        mBuilder.setContentText(message)
                .setAutoCancel(true)
                .setSubText(null)
                .setProgress(0,0,false)
                .setSmallIcon(R.drawable.ic_downloader);
        mBuilder.setOngoing(false);
        Intent intent = new Intent(activity, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("NOTIFICATION_ID", 1);
        PendingIntent dismissIntent = PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        mBuilder.setContentIntent(dismissIntent);
        mNotifyManager.notify(1, mBuilder.build());
    }
    //-------------------Notification-------------------
}