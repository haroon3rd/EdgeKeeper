package edu.tamu.cse.lenss.edgeKeeper.android;

import android.Manifest;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.*;
import android.view.*;
import android.content.Context;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKProperties;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtilsAndroid;


public class MainActivity extends AppCompatActivity {
    Context context;
    public TextView loggerTV;
    Logger logger = Logger.getLogger(this.getClass());

    static final int FILE_CHOOSER_CODE = 487;

    private EKService mEKService;
    private boolean mBound = false;
    private boolean SERVICE_STARTED = false;


    enum ServiceStatus {STARTED,CONNECTED,TERMINATED}
    ServiceStatus serviceStatus = ServiceStatus.TERMINATED;

    // We use this to store the certificate name and password
/*
    SharedPreferences sharedPref;
    SharedPreferences.Editor prefEditor;
*/

    SharedPreferences sharedPreferences;

    // Create the service binder
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mEKService = ((EKService.LocalBinder) iBinder).getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mEKService = null;
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        this.getActionBar();

/*
        sharedPref = context.getSharedPreferences(EKConstants.PREFERENCE_FILE, Context.MODE_PRIVATE);
        prefEditor = sharedPref.edit();
*/

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        checkPermissions();
    }

    protected void onDestroy() {
        logger.info("Inside onDestroy function. isFinishing: "+isFinishing());
        Autostart.stopEKService(this.getApplicationContext());
        //restartGNSService();
        //stopEKService();

        String enablePerpetualRunKey = context.getString(R.string.enable_perpetual_run);
        if( sharedPreferences.getBoolean(enablePerpetualRunKey, false)) {
            logger.info("Perpetual run is enabled. Calling Autostart to restart EKService");
            //Restart the Service
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(context.getString(R.string.restartservice));
            broadcastIntent.setClass(this, Autostart.class);
            this.sendBroadcast(broadcastIntent);
        }
        else
            logger.info("Perpetual run is disabled. Not starting service");

        super.onDestroy();

    }

    /**
     * This function initializes the App
     */
    protected void initializeApp(){
        loggerTV = (TextView) findViewById(R.id.gns_logger_text_view);
        loggerTV.setMovementMethod(new ScrollingMovementMethod());

        // COnfigure the logger to store logs in file
        try {
            EKUtilsAndroid.initLoggerAndroid(context);
        } catch (IOException e) {
            loggerTV.append("Can not create log file probably due to insufficient permission");
            logger.error(e);
        }

        logger.info("Initializing App main Activity");


        if( sharedPreferences.getString(EKProperties.p12Path, null) == null)
            showSetting();
        else if(Autostart.isEKServiceRunning(this)){
            logger.info(EKService.class.getSimpleName()+" is already running. So, not starting service");
            //return;
        } else
            Autostart.startEKService(this.getApplicationContext());
  }

    /*
        Now deal with the menus
    */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_help:
                showHelp();
                return true;
            case R.id.menu_setting:
                showSetting();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showSetting() {
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(intent);
    }


    /*
        Show what help.
    */
    private void showHelp() {
        loggerTV.append("You pressed Help menu");
    }

/*
    void startEKService(){
        logger.info("Trying to start EKService from activity");
        Intent intent = new Intent(this, EKService.class);
        //intent.putExtra(EKConstants.SERVICE_SEQ_NO, currentServiceSeq.incrementAndGet());
        //this.startService(intent);

        // This code will take care of closing the problem of shutting down the service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, notificationIntent, 0);
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }
*/

/*
    void stopEKService(){
        logger.debug("Trying to stop GNS Service from activity");
        this.stopService(new Intent(this, EKService.class));
    }
*/

    /*
        Before executing any code, the app should check whether the permissions are granted or not.
        If the permission is not granted then ask the user to grant the required permission.
        Check the detail code here: https://developer.android.com/training/permissions/requesting#java
    */
    private static final int REQEUST_PERMISSION_GNSSERVICE = 22;

    private void checkPermissions() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            logger.warn("Permission not granted for Writing to external storage");
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQEUST_PERMISSION_GNSSERVICE);
            }
        }
        else
            initializeApp();
   }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {
            case REQEUST_PERMISSION_GNSSERVICE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    logger.info("WRITE_EXTERNAL_STORAGE permission granted");
                    initializeApp();
                } else {
                    Toast.makeText(this, "You denied the permission", Toast.LENGTH_SHORT).show();
                    logger.info("Permission not granted");
                    checkPermissions();
                }
        }
    }

    @Override
    public void onBackPressed(){
        logger.debug("Back button pressed. Not doing anything");
    }
}
