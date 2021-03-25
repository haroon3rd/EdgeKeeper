package edu.tamu.cse.lenss.edgeKeeper.android;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.IBinder;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.os.Binder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import org.apache.log4j.Logger;

import edu.tamu.cse.lenss.edgeKeeper.server.EKHandler;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKProperties;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtilsAndroid;
import edu.tamu.cse.lenss.edgeKeeper.server.GNSClientHandler;


import org.apache.curator.framework.state.ConnectionState;
import org.apache.zookeeper.server.quorum.QuorumPeer.ServerState;


/**
 * This is the Android service class that runs in the background and servs the EKService
 */
public class EKService extends Service {

    Logger logger = Logger.getLogger(this.getClass());
    private final IBinder mBinder = new LocalBinder();
    Intent intent;
    EKPropertiesAndroid ekProperties = new EKPropertiesAndroid();
    static AtomicBoolean isRunning = new AtomicBoolean(false);

    public static final String CHANNEL_ID = "EKForegroundServiceChannel";

    // We use this to store the certificate name and password
    //SharedPreferences sharedPref;





    static AtomicInteger numberOfServiceRunning = new AtomicInteger(0);

    private Context context;
    EKHandler ekHandler;

    @Override
    public void onCreate() {
        context = getApplication().getApplicationContext();
        //sharedPref = context.getSharedPreferences(EKConstants.PREFERENCE_FILE, Context.MODE_PRIVATE);

        //sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        try {
            EKUtilsAndroid.initLoggerAndroid(context);
        } catch (IOException e) {
            System.out.println("Problem with creating logger");
        }
    }

//    @Override
//    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
//        ekProperties.load(sharedPreferences);
//    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning.set(true);
        this.intent = intent;

        Toast.makeText(this, "EK Service is starting", Toast.LENGTH_SHORT).show();
        logger.info("EK Service is starting. ");
        showNotification("Starting", false);

        ekProperties.load(PreferenceManager.getDefaultSharedPreferences(this));
        logger.info("Loaded config: "+ ekProperties.toString());

        logger.debug("Logging in. Concurrent services: " + numberOfServiceRunning.getAndIncrement());

            // Create a GNS ServiceUtils and define the onconnect Handler
            EKUtils gnsServiceUtils= new EKUtilsAndroid((EKProperties) ekProperties, context){

                String gnsState;
                String zkClientState;
                String zkServerState;

                @Override
                public void onStart() {

                    //notifyActivity(EKConstants.GNS_SERVICE_START);
                }
                public void onGNSStateChnage(GNSClientHandler.ConnectionState connectionState){
                    gnsState = connectionState.toString();
                    putNotice();
                }
                @Override
                public void onStop() {
                    //notifyActivity(EKConstants.GNS_SERVICE_STOP);
                }

                @Override
                public void onCuratorStateChange(ConnectionState newState) {
                    zkClientState = newState.toString();
                    putNotice();
                }


                @Override
                public void onZKServerStateChange(ServerState newServStatus) {
                    zkServerState = newServStatus.toString();
                    putNotice();
                }

                @Override
                public void onError(String string) {
                    showNotification(string, true);

                }


                void putNotice(){
                    showNotification("GNS:"+gnsState+", ZKClient:"+zkClientState+", ZKServer:"+zkServerState, false);
                }


            };

            ekHandler = new EKHandler(gnsServiceUtils, ekProperties);
            ekHandler.start();

            // Start network monitor which updates Ip whenever there is a IP change
            networkMonitor(ekHandler);

        return START_NOT_STICKY;
    }

    //this function
    private void updateGridView(){

    }


    private void showNotification(String message, boolean error) {
        String input = intent.getStringExtra("inputExtra");
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(message)
                .setContentText(message)
                .setContentText(input)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();

        try {
            startForeground(1, notification);
        } catch (RuntimeException e){
            logger.fatal("Foreground permission not granted",e);
            Toast.makeText(this, "Foreground permission not granted", Toast.LENGTH_SHORT).show();
            this.onDestroy();
        }

        System.out.println("xyz notification called!");
        processEdgeReplicaInfo(message, error);

    }


    //puts current cloud and zkClient status into the GridViewStore class for MainActivity to consume.
    private void processEdgeReplicaInfo(String message, boolean error) {

        try {

            //Create Wrapper object with default value
            Wrapper wrap = new Wrapper(false, false);

            //parse GNS/cloud information
            if(!error && message!=null && message.contains("GNS")){

                //get GNS/Cloud status
                String[] tokens = message.split(", ");
                String GNS_status = tokens[0].split(":")[1];
                if(GNS_status.equals("CONNECTED") || GNS_status.equals("RECONNECTED") ){
                    System.out.println("xyz GNS is TRUE");
                    wrap.cloudConnected = true;
                }


                //get zkClient status
                String zkclient_status = tokens[1].split(":")[1];
                if(zkclient_status.equals("CONNECTED") || zkclient_status.equals("RECONNECTED")) {
                    System.out.println("xyz ZKClient is TRUE");
                    wrap.zkClientConnected = true;
                }

            }


            GridViewStore.GNS_status.set(wrap.cloudConnected);
            GridViewStore.EKClient_status.set(wrap.zkClientConnected);

        }catch (Exception e){
            System.out.println("xyz EXCEPTION in EKService processEdgeReplicaInfo(): " + e);
        }

    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    /*@Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }*/

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }



    @Override
    public void onDestroy() {
        isRunning.set(false);
        logger.info("Inside onDestroy.");
        Toast.makeText(this, "GNS Service is Shutting down", Toast.LENGTH_SHORT).show();
        if (ekHandler != null)
            ekHandler.terminate();

        //broadcaster.sendBroadcast(new Intent(GNS_SERVICE_STOP));
        super.onDestroy();
        //logger.debug("Logging out. COncurrent services: " + numberOfServiceRunning.getAndDecrement());

/*
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("restartservice");
        broadcastIntent.setClass(this, Restarter.class);
        this.sendBroadcast(broadcastIntent);
*/

        logger.info("Stopped EKService");

    }

    public class LocalBinder extends Binder {
        EKService getService() {
            return EKService.this;
        }
    }



    /**
     * This function registers a broadcast receiver for CONNECTIVITY_ACTION from the the Network Manager of
     *         the Android phone. Upon receiving the broadcast message, it updates the new IPs.
     * @param ekHandler
     */
    void networkMonitor(final EKHandler ekHandler){
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //logger.info("Network state changed");
                logger.info("\n message received from network manager broadcast listener. "+intent.getAction());
                ekHandler.networkCHange();
            }
        };

        Context context = getApplication().getApplicationContext();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(broadcastReceiver, intentFilter);
    }

}
