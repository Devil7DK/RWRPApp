package io.github.redwolfrecovery.redwolf;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by Devil7DK on 4/13/18
 **/
public class CheckforUpdatesTask extends AsyncTask<Context, Void, Boolean>{

    private static CheckforUpdatesTask mInstance;
    private static Context mContext;
    private static ProgressDialog dialog;
    private static String mURL;
    private static String mDeviceName;
    private static String mBuildID_Local,mBuildID_Remote;
    private static String mError = "";

    private CheckforUpdatesTask() {
    }

    public static CheckforUpdatesTask getInstance(Context context, String URL, String deviceName) {
        if (mInstance == null) {
            mInstance = new CheckforUpdatesTask();
        }
        mContext = context;
        mURL = URL;
        mDeviceName = deviceName;
        return mInstance;
    }


    @Override
    protected Boolean doInBackground(Context... params) {
        mContext = params[0];

        NodeList devicelist;
        try {
            URL url = new URL(mURL);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(url.openStream()));
            doc.getDocumentElement().normalize();
            devicelist = doc.getElementsByTagName("device");
        } catch (Exception e) {
            e.printStackTrace();
            mError = e.getMessage();
            return false;
        }

        try{

            for (int temp = 0; temp < devicelist.getLength(); temp++) {
                Element eElement = (Element)devicelist.item(temp);
                List device= Arrays.asList(eElement.getAttribute("name").split(","));
                if(device.contains(mDeviceName))
                {
                    mBuildID_Remote = eElement.getAttribute("utc_build_date");
                    break;
                }
            }

            mBuildID_Local = Utils.getRecoveryIncrimentalVersion(mContext);

            if(mBuildID_Local.equals("")){
                Log.e("CheckForUpdate", "Cannot retrieve build id from remote.");
                mError = "Cannot retrieve build id from recovery.";
                return false;
            }else if(mBuildID_Remote.equals("")){
                Log.e("CheckForUpdate", "Cannot retrieve build id from remote.");
                mError = "Cannot retrieve build id from remote.";
                return false;
            }else{
                Log.i("CheckForUpdate",  "Build ID : Local - " + mBuildID_Local + " Remote - " + mBuildID_Remote);
                if(mBuildID_Remote.equals(mBuildID_Local)){
                    return false;
                }else{
                    return true;
                }
            }
        }catch (Exception ex){
            ex.printStackTrace();
            mError = ex.getMessage();
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        mInstance = null;
        if(dialog.isShowing())dialog.dismiss();
        if(result.equals(true)){
            Toast.makeText(mContext,R.string.update_available, Toast.LENGTH_LONG).show();
        }else{
            if(mError.equals("")){
                Toast.makeText(mContext,R.string.update_not_available, Toast.LENGTH_LONG).show();
            }else{
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
                alertDialogBuilder.setTitle(R.string.failed);
                alertDialogBuilder.setMessage(mError);
                        alertDialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        }
        MainActivity.CheckForUpdateResult(result, mError);
    }

    @Override
    protected void onPreExecute() {
        dialog = new ProgressDialog(mContext);
        dialog.setMessage(mContext.getString(R.string.checking_update));
        dialog.setIndeterminate(true);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.show();
    }


    @Override
    protected void onCancelled() {
        super.onCancelled();
        mInstance = null;
    }
}
