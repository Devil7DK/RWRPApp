package io.github.redwolfrecovery.redwolf;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

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

import static android.content.Context.ACTIVITY_SERVICE;

/**
 * Created by root on 3/1/18.
 */

public class Utils {

    private static String dumpimage_path;
    private static String unpackbootimg_path;
    private static String busybox_path;

    public static long GetMemorySize(Context mContext) {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager)mContext.getSystemService(ACTIVITY_SERVICE);
        if(activityManager != null)activityManager.getMemoryInfo(mi);
        return mi.totalMem / 1048576L;
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
        NetworkInfo ni = null;
        if (cm != null) ni = cm.getActiveNetworkInfo();
        if(ni != null){
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
            return iabd == 0;
        }catch(Exception ex){Crashlytics.logException(ex);}
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

    public static String getRecoveryIncrementalVersion(Context context){
        PrepareExecutables(context);

        File temp = new File(context.getCacheDir() + File.separator + "temp");
        File tempImg = new File(temp.getAbsolutePath(), "recovery.img");
        File ramdisk = new File(temp.getAbsolutePath(), "ramdisk.cpio.gz");
        File propfile = new File(Environment.getExternalStorageDirectory() + File.separator + "recovery.prop");

        try {
            if(temp.exists()){
                if(!temp.delete()) Log.i("getVersion","Failed to delete dir");
            }else{if(!temp.mkdirs())Log.e("getVersion","Make dir failed");}

            Process process = Runtime.getRuntime().exec("su");
            OutputStream stdin = process.getOutputStream();
            InputStream stderr = process.getErrorStream();

            String cmd1 = "cd "+ temp.getAbsolutePath() +"\n";
            String cmd2 = dumpimage_path + " /dev/block/bootdevice/by-name/recovery " + tempImg.getAbsolutePath() + "\n";
            String cmd3 = unpackbootimg_path + " --input " + tempImg.getAbsolutePath() + " --ramdisk " + ramdisk.getAbsolutePath() + "\n";
            String cmd4 = busybox_path + " gzip -cd " + ramdisk.getAbsolutePath() + " | " + busybox_path + " cpio -iv\n";
            String cmd5 = "chmod -R 0777 " + temp.getAbsolutePath() + "\n";
            String cmd6 = "rm -rf !(default.prop)" +"\n";
            String cmd7 = "mv default.prop " + propfile.getAbsolutePath() + "\n";

            stdin.write(cmd1.getBytes());
            stdin.write(cmd2.getBytes());
            stdin.write(cmd3.getBytes());
            stdin.write(cmd4.getBytes());
            stdin.write(cmd5.getBytes());
            stdin.write(cmd6.getBytes());
            stdin.write(cmd7.getBytes());

            stdin.write("exit\n".getBytes());
            stdin.flush();

            String line;
            BufferedReader br =
                    new BufferedReader(new InputStreamReader(stderr));
            while ((line = br.readLine()) != null) {
                Crashlytics.log("ERROR RUN CMDs : " + line);
            }

            stdin.close();
            process.waitFor();
            process.destroy();

            Properties prop = new Properties();
            try{

                prop.load(new FileInputStream(propfile.getAbsolutePath()));
                return prop.getProperty("ro.bootimage.build.date.utc","");
            }catch (Exception ex){
                ex.printStackTrace();
                Crashlytics.logException(ex);
            }

        } catch (Exception ex) {Crashlytics.logException(ex);
        } finally {
            if(temp.exists()){
                if(!temp.delete()) Log.i("getVersion","Failed to delete dir");
            }
        }
            return "";
    }

    private static void PrepareExecutables(Context context){
        try{
            String appFileDirectory;
            appFileDirectory = context.getFilesDir().getPath();
            String dumpimage_name = "dump_image";
            String unpackbootimg_name = "unpackbootimg";
            String gzip_name = "busybox";
            dumpimage_path = appFileDirectory + File.separator + dumpimage_name;
            unpackbootimg_path = appFileDirectory + File.separator + unpackbootimg_name;
            busybox_path = appFileDirectory + File.separator + gzip_name;
            copyAssets(dumpimage_name, dumpimage_path, context);
            copyAssets(unpackbootimg_name, unpackbootimg_path, context);
            copyAssets(gzip_name, busybox_path, context);
            if(!(new File (dumpimage_path)).setExecutable(true))Log.e("PrepareExes","SetExecutable failed");
            if(!(new File (unpackbootimg_path)).setExecutable(true))Log.e("PrepareExes","SetExecutable failed");
            if(!(new File (busybox_path)).setExecutable(true))Log.e("PrepareExes","SetExecutable failed");
        }catch(Exception ex){ex.printStackTrace();Crashlytics.logException(ex);}
    }


    private static String getArch(){
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

    private static void copyAssets(String filename, String fullpath, Context context) {
        AssetManager assetManager = context.getAssets();

        InputStream in;
        OutputStream out;

        try {
            in = assetManager.open(getArch() + "/" + filename);
            File outFile = new File(fullpath);
            out = new FileOutputStream(outFile);
            copyFile(in, out);
            in.close();
            out.flush();
        } catch(IOException e) {
            Crashlytics.logException(e);
            e.printStackTrace();
        }
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
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