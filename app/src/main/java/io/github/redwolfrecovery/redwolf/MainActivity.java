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
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class MainActivity extends AppCompatActivity {
    AlertDialog.Builder dlgAlert;
    static String device_name;
    static String device_build;
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
    static Boolean Checked=false;
    static SharedPreferences preferences;
    final String XML_URL= "https://redwolfrecovery.github.io/redwolf.xml";
    final String DownloadBaseURL="https://mirrors.c0urier.net/android/Dadi11/RedWolf/";
    String Filename;
    Long RAMSize;
    LinearLayout layout_UpdateStatus;
    TextView txt_UpdateStatus;
    ImageView img_UpdateStatus;
    Button btn_CheckUpdates;
    Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(" RedWolf Recovery Project");
            getSupportActionBar().setLogo(R.drawable.ic_wolf_color_small);
            getSupportActionBar().setDisplayUseLogoEnabled(true);
        }
        setContentView(R.layout.activity_redwolf_home);
        RAMSize = Utils.GetMemorySize(this);
        txt_Device = (TextView) findViewById(R.id.txt_device_name_val);
        txt_Memory = (TextView) findViewById(R.id.txt_device_size_val);
        txt_SupportStatus = (TextView) findViewById(R.id.txt_support_status_val);
        txt_Maintainer = (TextView) findViewById(R.id.txt_maintainer_val);
        txt_LatestVersion = (TextView) findViewById(R.id.txt_version_val);
        txt_LastUpdated = (TextView) findViewById(R.id.txt_updated_val);
        layout_UpdateStatus = (LinearLayout)  findViewById(R.id.layout_UpdateStatus);
        if(layout_UpdateStatus != null)layout_UpdateStatus.setVisibility(View.GONE);
        txt_UpdateStatus = (TextView) findViewById(R.id.txtUpdateStatus);
        img_UpdateStatus = (ImageView) findViewById(R.id.imgUpdateStatus);
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
        btn_CheckUpdates = (Button) findViewById(R.id.btn_CheckUpdates);
        btn_CheckUpdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckForUpdates();
            }
        });
        setCustomAnimation(fabMenu,R.drawable.ic_arrow_up, R.drawable.ic_arrow_down);
        XML_Check = DownloadXML.getInstance(XML_URL);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        device_name = Build.DEVICE;
        txt_Device.setText((Build.MODEL + " (" + Build.DEVICE + ")"));
        txt_Memory.setText((Mb2Gb(RAMSize) + " GB"));
        LoadPrefs();
        dlgAlert = new AlertDialog.Builder(this);
        if(Utils.isNetworkAvailable(this)){
            try{
                XML_Check.execute(this);
            }
            catch(Exception ex){
                Log.e("Details Check",ex.getMessage());
            }
        }
        else{
            Toast msg = Toast.makeText(getApplicationContext(),R.string.network_warning,Toast.LENGTH_SHORT);
            msg.show();
        }
        mContext = this;
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

    public static void CheckForUpdateResult(Boolean result, String Error, MainActivity activity){
        if(result){
            activity.layout_UpdateStatus.setVisibility(View.VISIBLE);
            activity.img_UpdateStatus.setImageResource(R.drawable.ic_update_available);
            activity.txt_UpdateStatus.setText(R.string.update_available);
            activity.txt_UpdateStatus.setTextColor(activity.getColor(R.color.colorUpdateOkay));
            activity.btn_CheckUpdates.setVisibility(View.GONE);
        }else{
            if(Error.equals("")){
                activity.layout_UpdateStatus.setVisibility(View.VISIBLE);
                activity.img_UpdateStatus.setImageResource(R.drawable.ic_update_notavailable);
                activity.txt_UpdateStatus.setText(R.string.update_not_available);
                activity.txt_UpdateStatus.setTextColor(activity.getColor(R.color.colorUpdateOkay));
                activity.btn_CheckUpdates.setVisibility(View.GONE);
            }else{
                activity.layout_UpdateStatus.setVisibility(View.VISIBLE);
                activity.img_UpdateStatus.setImageResource(R.drawable.ic_update_warning);
                activity.txt_UpdateStatus.setText(R.string.update_error);
                activity.txt_UpdateStatus.setTextColor(activity.getColor(R.color.colorUpdateError));
            }
        }
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
        if(!maintainer.equalsIgnoreCase("")){txt_Maintainer.setText(maintainer);}else{txt_Maintainer.setText(R.string.NA);}
        if(!last_updated.equalsIgnoreCase("")){txt_LastUpdated.setText(last_updated);}else{txt_LastUpdated.setText(R.string.NA);}
        if(!rw_version.equalsIgnoreCase("")){txt_LatestVersion.setText(rw_version);}else{txt_LatestVersion.setText(R.string.NA);}
        if(!rw_build.equalsIgnoreCase("")){txt_Build.setText(rw_build);}else{txt_Build.setText(R.string.NA);}
    }

    private void CheckForUpdates(){
        CheckForUpdatesTask task = CheckForUpdatesTask.getInstance(this, XML_URL, device_name, this);
        if(task.getStatus() != AsyncTask.Status.RUNNING){
            task.execute(this);
        }
    }

    private void DownloadAndInstall(){
        if(Utils.isNetworkAvailable(getApplicationContext()))
        {
            Filename="RedWolf-" + device_build + "-" + device_name + ".img";
            Downloader downloader = new Downloader(this, DownloadBaseURL + device_name + "/" + Filename);
            downloader.setDownloadTaskListener(downloadTask);
            downloader.execute("RW");
        }else{
            Toast msg = Toast.makeText(getApplicationContext(),R.string.network_warning,Toast.LENGTH_SHORT);
            msg.show();
        }
    }

    Downloader.DownloadTask downloadTask  = new Downloader.DownloadTask() {
        @Override
        public void onDownloadCompleted(String ID, String FilePath) {
            ProgressDialog progressDialog = new ProgressDialog(mContext);
            progressDialog.setMessage(getString(R.string.flashing));
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setIndeterminate(true);
            progressDialog.show();
            File rw = new File(FilePath);
            if(rw.exists()){
                if(RAMSize > 2500){
                    String command = "su -c dd if=" + FilePath + " of=/dev/block/bootdevice/by-name/recovery";
                    try {
                        Process SU = Runtime.getRuntime().exec(command);
                        SU.waitFor();
                        Toast.makeText(MainActivity.this, "RedWolf Downloaded & Flashed Successfully", Toast.LENGTH_LONG).show();
                    }
                    catch(Exception ex){
                        Log.e("FlashIMG",ex.getMessage());
                    }

                }

            }
            if(progressDialog.isShowing()){progressDialog.dismiss();}
        }

        @Override
        public void onDownloadCanceled(String ID) {
            Toast.makeText(MainActivity.this, "Download cancelled by user", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onDownloadError(String ID, String Error) {
            Toast.makeText(MainActivity.this, "Download interrupted due to \"" + Error + "\"", Toast.LENGTH_LONG).show();
        }
    };

    private void InstallFromLocal(){
        OpenFileDialog dialog = new OpenFileDialog(this);
        dialog.setFileIcon(getDrawable(R.drawable.ic_file));
        dialog.setFolderIcon(getDrawable(R.drawable.ic_folder));
        dialog.setAccessDeniedMessage(getString(R.string.fs_error));
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
        if(!Checked){
            if(Utils.isNetworkAvailable(this))
            {
                if(XML_Check.getStatus() != AsyncTask.Status.RUNNING && XML_Check.getStatus() != AsyncTask.Status.PENDING)
                {
                    try{
                        XML_Check.execute(this);
                    }
                    catch(Exception ex){
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    private BroadcastReceiver networkStateReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            OnNetworkChanged();
        }
    };

    // DownloadXML AsyncTask
    private static class DownloadXML extends AsyncTask<MainActivity, Void, MainActivity> {
        NodeList device_list;
        NodeList changelog_list;

        static DownloadXML mInstance;

        String mURL;

        DownloadXML(String URL) {
            mURL = URL;
        }

        static DownloadXML getInstance(String URL) {
            if (mInstance == null) {
                mInstance = new DownloadXML(URL);
            }
            return mInstance;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected MainActivity doInBackground(MainActivity... activities) {
            try {
                URL url = new URL(mURL);
                DocumentBuilderFactory dbf = DocumentBuilderFactory
                        .newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(new InputSource(url.openStream()));
                doc.getDocumentElement().normalize();
                device_list = doc.getElementsByTagName("device");
                changelog_list = doc.getElementsByTagName("changelog");
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return activities[0];

        }

        @Override
        protected void onPostExecute(MainActivity activity) {
            boolean device_found=false;
            Checked=true;
            for (int temp = 0; temp < device_list.getLength(); temp++) {
                Element eElement = (Element)device_list.item(temp);
                List device= Arrays.asList(eElement.getAttribute("name").split(","));
                if(device.contains(device_name))
                {
                    device_found = true;
                    String Maintainer = eElement.getAttribute("maintainer");
                    String Version_ = eElement.getAttribute("version");
                    String LastUpdated = eElement.getAttribute("date");
                    String Build_ = eElement.getAttribute("build");
                    device_build = Build_;
                    activity.txt_SupportStatus.setText(R.string.device_support_true);
                    activity.txt_Maintainer.setText(Maintainer);
                    activity.txt_LatestVersion.setText(Version_);
                    activity.txt_LastUpdated.setText(LastUpdated);
                    activity.txt_Build.setText(Build_);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("maintainer",Maintainer);
                    editor.putString("rw_version",Version_);
                    editor.putString("last_updated",LastUpdated);
                    editor.putBoolean("supported",true);
                    editor.putString("rw_build",Build_);
                    editor.apply();
                    activity.fab_DownloadFlash.setEnabled(true);
                    activity.btn_CheckUpdates.setVisibility(View.VISIBLE);
                }
            }
            if(!device_found)
            {
                activity.txt_SupportStatus.setText(R.string.device_support_false);
                activity.txt_Maintainer.setText(R.string.NA);
                activity.txt_LatestVersion.setText(R.string.NA);
                activity.txt_LastUpdated.setText(R.string.NA);
                activity.txt_Build.setText(R.string.NA);
                activity.fab_DownloadFlash.setEnabled(false);
                activity.btn_CheckUpdates.setVisibility(View.GONE);
            }
        }
    }
}
