package io.github.redwolfrecovery.redwolf;

import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Properties;

/**
 * Created by root on 3/1/18.
 */

public class Utils {

    static String appFileDirectory;
    static String dumpimage_path;
    static String unpackbootimg_path;

    public static long GetMemorySize(Context mContext) {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager)mContext.getSystemService(mContext.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        long availableMegs = mi.totalMem / 1048576L;
        return availableMegs;
    }

    //To do something after 'secs' of delay.
    public interface DelayCallback{
        void afterDelay();
    }

    public static void delay(int secs, final DelayCallback delayCallback){
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                delayCallback.afterDelay();
            }
        }, secs * 1000); // afterDelay will be executed after (secs*1000) milliseconds.
    }

    public static boolean isNetworkAvailable(Context mContext) {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }
    public static boolean isRootAvailable(){
        try{
            Process executor = Runtime.getRuntime().exec("su -c ls /data/data");
            executor.waitFor();
            int iabd = executor.exitValue();
            if(iabd != 0){return false;}else{return true;}
        }catch(Exception ex){}
     return false;
    }

    @NonNull
    public static String getETAString(@NonNull final Context context, final long etaInMilliSeconds) {
        if (etaInMilliSeconds < 0) {
            return "";
        }
        int seconds = (int) (etaInMilliSeconds / 1000);
        long hours = seconds / 3600;
        seconds -= hours * 3600;
        long minutes = seconds / 60;
        seconds -= minutes * 60;
        if (hours > 0) {
            return context.getString(R.string.download_eta_hrs, hours, minutes, seconds);
        } else if (minutes > 0) {
            return context.getString(R.string.download_eta_min, minutes, seconds);
        } else {
            return context.getString(R.string.download_eta_sec, seconds);
        }
    }

    @NonNull
    public static String getDownloadSpeedString(@NonNull final Context context, final long downloadedBytesPerSecond) {
        if (downloadedBytesPerSecond < 0) {
            return "";
        }
        double kb = (double) downloadedBytesPerSecond / (double) 1000;
        double mb = kb / (double) 1000;
        final DecimalFormat decimalFormat = new DecimalFormat(".##");
        if (mb >= 1) {
            return context.getString(R.string.download_speed_mb, decimalFormat.format(mb));
        } else if (kb >= 1) {
            return context.getString(R.string.download_speed_kb, decimalFormat.format(kb));
        } else {
            return context.getString(R.string.download_speed_bytes, downloadedBytesPerSecond);
        }
    }

    public static String getRecoveryIncrimentalVersion(Context context, ProgressDialog progressDialog){
        progressDialog.setMessage(context.getString(R.string.preparing_executables));
        PrepareExecutables(context);

        File temp = new File(context.getCacheDir() + File.separator + "temp");
        File tempImg = new File(temp.getAbsolutePath(), "recovery.img");
        File ramdisk = new File(temp.getAbsolutePath(), "ramdisk.cpio.gz");
        File propfile = new File(Environment.getExternalStorageDirectory() + File.separator + "recovery.prop");

        try {
            if(temp.exists()){
                progressDialog.setMessage(context.getString(R.string.removing_temp));
                temp.delete();
            }else{temp.mkdirs();}

            String line;
            Process process = Runtime.getRuntime().exec("su");
            OutputStream stdin = process.getOutputStream();
            InputStream stderr = process.getErrorStream();
            InputStream stdout = process.getInputStream();

            String cmd1 = "cd "+ temp.getAbsolutePath() +"\n";
            String cmd2 = dumpimage_path + " /dev/block/bootdevice/by-name/recovery " + tempImg.getAbsolutePath() + "\n";
            String cmd3 = unpackbootimg_path + " --input " + tempImg.getAbsolutePath() + " --ramdisk " + ramdisk.getAbsolutePath() + "\n";
            String cmd4 = "gzip -cd " + ramdisk.getAbsolutePath() + " | cpio -iv\n";
            String cmd5 = "chmod -R 0777 " + temp.getAbsolutePath() + "\n";
            String cmd6 = "rm -rf !(default.prop)" +"\n";
            String cmd7 = "mv default.prop " + propfile.getAbsolutePath() + "\n";


            progressDialog.setMessage(context.getString(R.string.dumping_recoveryimage));
            stdin.write(cmd1.getBytes());
            stdin.write(cmd2.getBytes());
            progressDialog.setMessage(context.getString(R.string.unpacking_recoveryimage));
            stdin.write(cmd3.getBytes());
            progressDialog.setMessage(context.getString(R.string.extracting_ramdisk));
            stdin.write(cmd4.getBytes());
            stdin.write(cmd5.getBytes());
            stdin.write(cmd6.getBytes());
            stdin.write(cmd7.getBytes());

            stdin.write("exit\n".getBytes());
            stdin.flush();

            stdin.close();

            process.waitFor();
            process.destroy();

            progressDialog.setMessage(context.getString(R.string.reading_prop));
            Properties prop = new Properties();
            try{

                prop.load(new FileInputStream(propfile.getAbsolutePath()));
                String v1 = prop.getProperty("ro.build.version.incremental","");
                //propfile.delete();
                return v1;
            }catch (Exception ex){
                ex.printStackTrace();
            }

        } catch (Exception ex) {
        } finally {
            if(temp.exists()){
                if(progressDialog.isShowing())progressDialog.setMessage(context.getString(R.string.removing_temp));
                temp.delete();
            }
        }
            return "";
    }

    public static void PrepareExecutables(Context context){
        try{
            appFileDirectory = context.getFilesDir().getPath();
            String dumpimage_name = "dump_image";
            String unpackbootimg_name = "unpackbootimg";
            dumpimage_path = appFileDirectory + File.separator + dumpimage_name;
            unpackbootimg_path = appFileDirectory + File.separator + unpackbootimg_name;
            copyAssets(dumpimage_name, dumpimage_path, context);
            copyAssets(unpackbootimg_name, unpackbootimg_path, context);
            (new File (dumpimage_path)).setExecutable(true);
            (new File (unpackbootimg_path)).setExecutable(true);
        }catch(Exception ex){ex.printStackTrace();}
    }


    public static String getArch(){
        String deviceArch = android.os.Build.SUPPORTED_ABIS[0];
        if(deviceArch.contains("armeabi")){
            return "arm";
        }else if(deviceArch.contains("arm64")){
            return "arm64";
        }else if(deviceArch.contains("x86_64")){
            return "x86_64";
        }else if(deviceArch.equals("x86")){
            return "x86";
        }
        return "arm";
    }

    public static void copyAssets(String filename, String fullpath, Context context) {
        AssetManager assetManager = context.getAssets();

        InputStream in = null;
        OutputStream out = null;

        try {
            in = assetManager.open(getArch() + "/" + filename);
            File outFile = new File(fullpath);
            out = new FileOutputStream(outFile);
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out = null;
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void copyFile(InputStream in, OutputStream out) throws IOException {
        try {

            try {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }
}