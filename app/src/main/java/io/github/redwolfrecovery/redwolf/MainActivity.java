package io.github.redwolfrecovery.redwolf;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class MainActivity extends AppCompatActivity {
    AlertDialog.Builder dlgAlert;
    String device_name;
    String device_build;
    TextView txt_Device;
    TextView txt_Memory;
    TextView txt_SupportStatus;
    TextView txt_Maintainer;
    TextView txt_LatestVersion;
    TextView txt_Build;
    TextView txt_LastUpdated;
    FloatingActionMenu fabMenu;
    FloatingActionButton fab_DownloadFlash;
    FloatingActionButton fab_LocalFlash;
    FloatingActionButton fab_Backup;
    DownloadXML XML_Check;
    Boolean Checked=false;
    private static final int BUFFER_SIZE = 4096;
    SharedPreferences preferences;
    final String XML_URL="https://redwolfrecovery.github.io/redwolf.xml";
    final String DownloadBaseURL="https://mirrors.c0urier.net/android/Dadi11/RedWolf/";
    String Filename;
    ProgressDialog progressdialog;
    URL url;
    URLConnection urlconnection ;
    int FileSize;
    InputStream inputstream;
    OutputStream outputstream;
    byte dataArray[] = new byte[1024];
    long totalSize = 0;
    Long RAMSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle(" RedWolf Recovery Project");
        getSupportActionBar().setLogo(R.drawable.ic_wolf_color_small);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        setContentView(R.layout.activity_redwolf_home);
        RAMSize = Utils.GetMemorySize(this);
        txt_Device = (TextView) findViewById(R.id.txt_device_name_val);
        txt_Memory = (TextView) findViewById(R.id.txt_device_size_val);
        txt_SupportStatus = (TextView) findViewById(R.id.txt_support_status_val);
        txt_Maintainer = (TextView) findViewById(R.id.txt_maintainer_val);
        txt_LatestVersion = (TextView) findViewById(R.id.txt_version_val);
        txt_LastUpdated = (TextView) findViewById(R.id.txt_updated_val);
        txt_Build = (TextView) findViewById(R.id.txt_build_val);
        fabMenu = (FloatingActionMenu) findViewById(R.id.rw_fab_menu);
        fab_DownloadFlash = (FloatingActionButton)  findViewById(R.id.fab_install_download);
        fab_LocalFlash = (FloatingActionButton)  findViewById(R.id.fab_install_local);
        fab_Backup = (FloatingActionButton)  findViewById(R.id.fab_backup);
        fab_DownloadFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DownloadAndInstall();
                fabMenu.close(true);
            }
        });
        fab_Backup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BackupRecovery();
                fabMenu.close(true);
            }
        });
        fab_LocalFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InstallFromLocal();
                fabMenu.close(true);
            }
        });
        setCustomAnimation(fabMenu,R.drawable.ic_arrow_up, R.drawable.ic_arrow_down);
        XML_Check = new DownloadXML();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        device_name = Build.DEVICE;
        txt_Device.setText(Build.MODEL + " (" + Build.DEVICE + ")");
        txt_Memory.setText(Mb2Gb(RAMSize) + " GB");
        LoadPrefs();
        dlgAlert = new AlertDialog.Builder(this);
        if(Utils.isNetworkAvailable(this)){
            try{
                XML_Check.execute(XML_URL);
            }
            catch(Exception ex){
                Log.e("Details Check",ex.getMessage());
            }
        }
        else{
            Toast msg = Toast.makeText(getApplicationContext(),R.string.network_warning,Toast.LENGTH_SHORT);
            msg.show();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.items, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    protected void onStart()
    {
        super.onStart();
    }

    private void setCustomAnimation(final FloatingActionMenu fabMenu, final int normal, final int pressed) {
        AnimatorSet set = new AnimatorSet();

        ObjectAnimator scaleOutX = ObjectAnimator.ofFloat(fabMenu.getMenuIconView(), "scaleX", 1.0f, 0.2f);
        ObjectAnimator scaleOutY = ObjectAnimator.ofFloat(fabMenu.getMenuIconView(), "scaleY", 1.0f, 0.2f);

        ObjectAnimator scaleInX = ObjectAnimator.ofFloat(fabMenu.getMenuIconView(), "scaleX", 0.2f, 1.0f);
        ObjectAnimator scaleInY = ObjectAnimator.ofFloat(fabMenu.getMenuIconView(), "scaleY", 0.2f, 1.0f);

        scaleOutX.setDuration(50);
        scaleOutY.setDuration(50);

        scaleInX.setDuration(50);
        scaleInY.setDuration(50);

        scaleInX.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                fabMenu.getMenuIconView().setImageResource(fabMenu.isOpened()
                        ? normal : pressed);
            }
        });

        set.play(scaleOutX).with(scaleOutY);
        set.play(scaleInX).with(scaleInY).after(scaleOutX);
        set.setInterpolator(new OvershootInterpolator(2));

        fabMenu.setIconToggleAnimatorSet(set);
    }

    public void LoadPrefs(){
        String maintainer = preferences.getString("maintainer","");
        String rw_version = preferences.getString("rw_version","");
        String last_updated = preferences.getString("last_updated","");
        boolean supported = preferences.getBoolean("supported",false);
        String rw_build = preferences.getString("rw_build","");
        if(supported){txt_SupportStatus.setText(R.string.device_support_true);}else{txt_SupportStatus.setText(R.string.device_support_false);}
        if(!maintainer.equalsIgnoreCase("")){txt_Maintainer.setText(maintainer);}else{txt_Maintainer.setText("NA");}
        if(!last_updated.equalsIgnoreCase("")){txt_LastUpdated.setText(last_updated);}else{txt_LastUpdated.setText("NA");}
        if(!rw_version.equalsIgnoreCase("")){txt_LatestVersion.setText(rw_version);}else{txt_LatestVersion.setText("NA");}
        if(!rw_build.equalsIgnoreCase("")){txt_Build.setText(rw_build);}else{txt_Build.setText("NA");}
    }

    private void DownloadAndInstall(){
        if(Utils.isNetworkAvailable(getApplicationContext()))
        {
            if(RAMSize < 2500){
                Filename="RedWolf-" + device_build + "-" + device_name + "-2GB_RAM.zip";
            }else{
                Filename="RedWolf-" + device_build + "-" + device_name + ".img";
            }
            new DownloadAndFlash().execute(DownloadBaseURL + device_name + "/" + Filename);
        }else{
            Toast msg = Toast.makeText(getApplicationContext(),R.string.network_warning,Toast.LENGTH_SHORT);
            msg.show();
        }
    }

    private void InstallFromLocal(){
        OpenFileDialog dialog = new OpenFileDialog(this);
        dialog.setFilter("(.*).img");
        dialog.setOpenDialogListener(new OpenFileDialog.OpenDialogListener() {
            @Override
            public void OnSelectedFile(String fileName) {
                String command = "su -c dd if=" + fileName + " of=/dev/block/bootdevice/by-name/recovery";
                try {
                    Runtime.getRuntime().exec(command);
                    dlgAlert.setMessage(R.string.flash_completed_local);
                    dlgAlert.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    //dismiss the dialog
                                }
                            });
                    dlgAlert.setCancelable(true);
                    dlgAlert.create().show();
                }
                catch(IOException ex)
                {
                    Log.e("InstallLocal",ex.getMessage());
                }
            }
        });
        dialog.show();
    }
    private void BackupRecovery()
    {
        OpenFileDialog dialog = new OpenFileDialog(this);
        dialog.setFilter("*img");
        dialog.setOnlyFoldersFilter();
        dialog.setOpenDialogListener(new OpenFileDialog.OpenDialogListener() {
            @Override
            public void OnSelectedFile(String fileName) {
                String command = "su -c dd if=/dev/block/bootdevice/by-name/recovery of=" + fileName + "/recovery.img bs=4096";
                try {
                    Runtime.getRuntime().exec(command);
                    dlgAlert.setMessage(R.string.backup_complete);
                    dlgAlert.setTitle(R.string.title_finished);
                    dlgAlert.setPositiveButton(R.string.string_okay,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                    dlgAlert.setCancelable(true);
                    dlgAlert.create().show();
                }
                catch(IOException ex)
                {
                    Log.e("Backup",ex.getMessage());
                }
            }
        });
        dialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        final MenuItem val=item;
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which==DialogInterface.BUTTON_POSITIVE){
                    String command="";
                    switch(val.getItemId()){
                        case R.id.reboot:
                            command="reboot";
                            break;
                        case R.id.reboot_recovery:
                            command="reboot recovery";
                            break;
                        case R.id.reboot_bootloader:
                            command="reboot bootloader";
                            break;
                    }
                    try {
                        Process SU = Runtime.getRuntime().exec("su -c " + command);
                        SU.waitFor();
                    }
                    catch(Exception ex){
                        Log.e("Reboot",ex.getMessage());
                    }
                }
            }
        };
        dlgAlert.setTitle(R.string.confirm_reboot_title);
        switch(val.getItemId()){
            case R.id.reboot:
                dlgAlert.setMessage(R.string.confirm_reboot_msg).setPositiveButton(R.string.string_yes, dialogClickListener)
                        .setNegativeButton(R.string.string_cancel, dialogClickListener).show();
                break;
            case R.id.reboot_recovery:
                dlgAlert.setMessage(R.string.confirm_reboot_recovery).setPositiveButton(R.string.string_yes, dialogClickListener)
                        .setNegativeButton(R.string.string_cancel, dialogClickListener).show();
                break;
            case R.id.reboot_bootloader:
                dlgAlert.setMessage(R.string.confirm_reboot_bootloader).setPositiveButton(R.string.string_yes, dialogClickListener)
                        .setNegativeButton(R.string.string_cancel, dialogClickListener).show();
                break;
        }
        return true;
    }

    int Mb2Gb(long RAMinMb){
        if(RAMinMb > 5500){
            return 6;
        }
        else if(RAMinMb > 4500){
            return 5;
        }
        else if(RAMinMb > 3500){
            return 4;
        }
        else if(RAMinMb > 2500){
            return 3;
        }
        else if(RAMinMb > 1500){
            return 2;
        }
        else{
            return 1;
        }
    }

    public class DownloadAndFlash extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressdialog = new ProgressDialog(MainActivity.this);
            progressdialog.setMessage("Downloading " + Filename);
            progressdialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressdialog.setCancelable(false);
            progressdialog.show();
        }

        @Override
        protected String doInBackground(String... aurl) {

            int count;

            try {

                url = new URL(aurl[0]);
                urlconnection = url.openConnection();
                urlconnection.connect();

                FileSize = urlconnection.getContentLength();

                inputstream = new BufferedInputStream(url.openStream());
                outputstream = new FileOutputStream(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/"+Filename);


                while ((count = inputstream.read(dataArray)) != -1) {

                    totalSize += count;

                    publishProgress(""+(int)((totalSize*100)/FileSize));

                    outputstream.write(dataArray, 0, count);
                }

                outputstream.flush();
                outputstream.close();
                inputstream.close();

            } catch (Exception e) {Log.e("Download",e.getMessage());}
            return null;

        }
        protected void onProgressUpdate(String... progress) {
            progressdialog.setProgress(Integer.parseInt(progress[0]));
        }

        @Override
        protected void onPostExecute(String unused) {
            String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/"+Filename;
            File rw = new File(filePath);
            if(rw.exists()){
                if(RAMSize > 2500){
                    String command = "su -c dd if=" + filePath + " of=/dev/block/bootdevice/by-name/recovery";
                    try {
                        Process SU = Runtime.getRuntime().exec(command);
                        SU.waitFor();
                        Toast.makeText(MainActivity.this, "RedWolf Downloaded & Flashed Successfully", Toast.LENGTH_LONG).show();
                    }
                    catch(Exception ex){
                        Log.e("FlashIMG",ex.getMessage());
                    }

                }
                else if(RAMSize < 2500)
                {
                    final String tmpDir = Environment.getExternalStorageDirectory() + "/WOLF/tmp";
                    File Wolf = new File(Environment.getExternalStorageDirectory() + "/WOLF");
                    if(!Wolf.exists()){Wolf.mkdir();}
                    final File zipFile = new File(filePath);
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                case DialogInterface.BUTTON_POSITIVE:
                                    try
                                    {
                                        File destDir = new File(tmpDir);
                                        if (!destDir.exists()) {
                                            destDir.delete();
                                            destDir.mkdir();
                                        }
                                        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFile));
                                        ZipEntry entry = zipIn.getNextEntry();
                                        // iterates over entries in the zip file
                                        while (entry != null) {
                                            try{
                                                String zipfilePath = tmpDir + File.separator + entry.getName();
                                                if (!entry.isDirectory()) {
                                                    // if the entry is a file, extracts it
                                                    extractFile(zipIn, zipfilePath);
                                                } else {
                                                    // if the entry is a directory, make the directory
                                                    File dir = new File(zipfilePath);
                                                    dir.mkdir();
                                                }
                                                zipIn.closeEntry();
                                                entry = zipIn.getNextEntry();
                                            }
                                            catch (Exception ex){Log.e("ExtractZIP",ex.getMessage());}
                                        }
                                        zipIn.close();
                                        String recimg=tmpDir + File.separator + "tools" + File.separator + "recovery.img";
                                        String command = "su -c dd if=" + recimg + " of=/dev/block/bootdevice/by-name/recovery";
                                        try {
                                            Process SU = Runtime.getRuntime().exec(command);
                                            SU.waitFor();
                                            destDir.delete();
                                            SU = Runtime.getRuntime().exec("su -c echo 'install "+ zipFile.getAbsolutePath()+"' > /cache/recovery/openrecoveryscript");
                                            SU.waitFor();
                                            SU = Runtime.getRuntime().exec("su -c reboot recovery");
                                            SU.waitFor();
                                        }catch(Exception ex){
                                            Log.e("FlashIMG2GB",ex.getMessage());
                                        }
                                    }
                                    catch (Exception ex){
                                        Log.e("2GB",ex.getMessage());
                                    }
                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    dlgAlert.setMessage(R.string.install_download_cancel);
                                    dlgAlert.setTitle(R.string.info);
                                    dlgAlert.setPositiveButton(R.string.string_okay,
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                }
                                            });
                                    dlgAlert.setCancelable(true);
                                    dlgAlert.create().show();
                                    break;
                            }
                        }
                    };
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("Download completed. RedWolf 2GB Variants requires reboot to complete installation. Are you sure to reboot and install?").setPositiveButton("Yes", dialogClickListener)
                            .setNegativeButton("No", dialogClickListener).show();
                }
                }
            if(progressdialog.isShowing()){progressdialog.dismiss();}
            }
        }

    void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }
    //-----------------DOWNLOAD------------

    public static void copy(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    public void onPause() {
        unregisterReceiver(networkStateReceiver);
        super.onPause();
    }

    private void OnNetworkChanged() {
        if(Checked==false){
            if(Utils.isNetworkAvailable(this))
            {
                if(XML_Check.getStatus() != AsyncTask.Status.RUNNING && XML_Check.getStatus() != AsyncTask.Status.PENDING)
                {
                    try{
                        XML_Check.execute(XML_URL);
                    }
                    catch(Exception ex){

                    }
                }
            }
        }
    }

    private BroadcastReceiver networkStateReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            OnNetworkChanged();
        }
    };

    // DownloadXML AsyncTask
    private class DownloadXML extends AsyncTask<String, Void, Void> {
        NodeList devicelist;
        NodeList changeloglist;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... Url) {
            try {
                URL url = new URL(Url[0]);
                DocumentBuilderFactory dbf = DocumentBuilderFactory
                        .newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                // Download the XML file
                Document doc = db.parse(new InputSource(url.openStream()));
                doc.getDocumentElement().normalize();
                // Locate the Tag Name
                devicelist = doc.getElementsByTagName("device");
                changeloglist = doc.getElementsByTagName("changelog");
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return null;

        }

        @Override
        protected void onPostExecute(Void args) {
            boolean device_found=false;
            Checked=true;
            for (int temp = 0; temp < devicelist.getLength(); temp++) {
                Element eElement = (Element)devicelist.item(temp);
                List device= Arrays.asList(eElement.getAttribute("name").split(","));
                if(device.contains(device_name))
                {
                    device_found = true;
                    String Maintainer = eElement.getAttribute("maintainer");
                    String Version_ = eElement.getAttribute("version");
                    String LastUpdated = eElement.getAttribute("date");
                    String Build_ = eElement.getAttribute("build");
                    device_build = Build_;
                    txt_SupportStatus.setText(R.string.device_support_true);
                    txt_Maintainer.setText(Maintainer);
                    txt_LatestVersion.setText(Version_);
                    txt_LastUpdated.setText(LastUpdated);
                    txt_Build.setText(Build_);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("maintainer",Maintainer);
                    editor.putString("rw_version",Version_);
                    editor.putString("last_updated",LastUpdated);
                    editor.putBoolean("supported",true);
                    editor.putString("rw_build",Build_);
                    editor.apply();
                }
            }
            if(device_found==false)
            {
                txt_SupportStatus.setText(R.string.device_support_false);
            }
        }
    }
}
