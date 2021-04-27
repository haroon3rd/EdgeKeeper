package edu.tamu.cse.lenss.edgeKeeper.android;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import android.view.*;
import android.content.Context;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;








import javax.jmdns.JmDNS;
import javax.jmdns.JmmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.edgeKeeper.server.EKHandler;
import edu.tamu.cse.lenss.edgeKeeper.server.EdgeStatus;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKProperties;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtilsAndroid;


public class MainActivitySCRAP extends AppCompatActivity{

    Context context;
    Activity activity;
    Logger logger = Logger.getLogger(this.getClass());
    JmDNS jmdns;
    JmmDNS registry;
    private WifiManager.MulticastLock multicastLock;
    private SampleListener_JmDNS sampleListener_jm;
    private SampleListener_JmmDNS sampleListener_jmm;
    private String service_type = "_http._tcp.local.";
    WifiManager wm;
    InetAddress bindingAddress;
    boolean isDiscovering;



    public static GridView GV;

    static final int FILE_CHOOSER_CODE = 487;
    private EKService mEKService;
    private boolean mBound = false;
    private boolean SERVICE_STARTED = false;

    enum ServiceStatus {STARTED,CONNECTED,TERMINATED}
    ServiceStatus serviceStatus = ServiceStatus.TERMINATED;
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

        this.context = getApplicationContext();
        this.activity = this;


        this.getActionBar();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        checkPermissions();

    }


    @Override
    protected void onStart() {
        super.onStart();

    }

    private void stopScan_jm() {
        try {
            if (jmdns != null) {
                jmdns.unregisterAllServices();
                jmdns.close();
                jmdns = null;
            }
            if (multicastLock != null) {
                multicastLock.release();
                multicastLock = null;
            }
        } catch (Exception ex) {
            logger.log(Level.ALL, "exception in stopScan_jm(): " , ex);
        }
    }

    private void stopScan_jmm() {
        try {
            if (registry != null) {
                registry.unregisterAllServices();
                registry.close();
                registry = null;
            }
            if (multicastLock != null) {
                multicastLock.release();
                multicastLock = null;
            }
        } catch (Exception ex) {
            logger.log(Level.ALL, "exception in stopScan_jmm(): " , ex);
        }
    }


    //initializes gridview.
    private void setupGridView() {

        //init gridview object pointing to the right resource at xml
        GV = findViewById(R.id.idGV);


        // Set Long-Clickable
        GV.setLongClickable(true);
        GV.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @SuppressLint("NewApi")
            public boolean onItemLongClick(AdapterView<?> parent, View arg1,
                                           int position, long arg3) {
                Toast.makeText(getApplicationContext(), "Item Long click not implemented", Toast.LENGTH_SHORT).show();;

                return false;
            }
        });

        //Set On-Clicked
        GV.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @SuppressLint("NewApi")
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Toast.makeText(getApplicationContext(), "Item click not implemented", Toast.LENGTH_SHORT).show();;
            }
        });

    }

    protected void onDestroy() {

        //stopScan_jm();
        stopScan_jmm();

        logger.info("Inside onDestroy function. isFinishing: "+isFinishing());
        Autostart.stopEKService(this.getApplicationContext());

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

        // Configure the logger to store logs in file
        try {
            EKUtilsAndroid.initLoggerAndroid(context);
        } catch (IOException e) {
            logger.error(e);
        }

        logger.info(" Initializing App main Activity");
        if( sharedPreferences.getString(EKProperties.p12Path, null) == null) {
            showAccount();
        }else if(Autostart.isEKServiceRunning(this)){
            logger.info(EKService.class.getSimpleName()+" is already running. So, not starting service");
            //return;
        } else {
            Autostart.startEKService(this.getApplicationContext());
        }

        //init gridivew
        setupGridView();

        //start thread that will periodically update gridview
        Thread GVupdater = new Thread(new UpdateGridView(getApplicationContext(), this, GV));
        GVupdater.start();

        //init jmdns stuff
        //new jmdnss().execute();

        //init jmmdns stuff
        new jmmdnss().execute();

    }

    //jmdns class running on background
    private class jmdnss extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... voids) {

            isDiscovering = false;
            wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            multicastLock = wm.createMulticastLock(activity.getPackageName());
            multicastLock.setReferenceCounted(false);
            try {
                WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                // get the device ip address
                final InetAddress deviceIpAddress = getDeviceIpAddress(wifi);
                multicastLock = wifi.createMulticastLock(getClass().getName());
                multicastLock.setReferenceCounted(true);
                multicastLock.acquire();
                jmdns = JmDNS.create(deviceIpAddress, InetAddress.getLocalHost().getHostName());
                ServiceInfo serviceInfo = ServiceInfo.create(service_type, "EK_NSD_android_jm", 0, 1,1,true,  "android_text_wifi_jm");
                sampleListener_jm = new SampleListener_JmDNS(logger, jmdns);

                jmdns.registerService(serviceInfo);
                jmdns.addServiceListener(service_type, sampleListener_jm);
            } catch (IOException ex) {
                logger.log(Level.ALL, "_NSD_sss jmdns exception in onstart()", ex);
            }
            return null;
        }

        private InetAddress getDeviceIpAddress(WifiManager wifi) {
            InetAddress result = null;
            try {
                // default to Android localhost
                result = InetAddress.getLocalHost();
                // figure out our wifi address, otherwise bail
                WifiInfo wifiinfo = wifi.getConnectionInfo();
                int intaddr = wifiinfo.getIpAddress();
                byte[] byteaddr = new byte[] {
                        (byte) (intaddr & 0xff),
                        (byte) (intaddr >> 8 & 0xff),
                        (byte) (intaddr >> 16 & 0xff),
                        (byte) (intaddr >> 24 & 0xff) };
                result = InetAddress.getByAddress(byteaddr);
            } catch (UnknownHostException ex) {
                logger.log(Level.ALL, "_NSD_sss jmmdns exception getDeviceIpAddress(): ", ex);
            }

            return result;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

        }

    }

    //jmmdns class running on background
    private class jmmdnss extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... voids) {

            isDiscovering = false;
            wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            multicastLock = wm.createMulticastLock(activity.getPackageName());
            multicastLock.setReferenceCounted(false);
            try {
                WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                // get the device ip address
                final InetAddress deviceIpAddress = getDeviceIpAddress(wifi);
                multicastLock = wifi.createMulticastLock(getClass().getName());
                multicastLock.setReferenceCounted(true);
                multicastLock.acquire();
                registry = JmmDNS.Factory.getInstance();
                ServiceInfo serviceInfo = ServiceInfo.create(service_type, "EK_NSD_android_jmm_RIGHT", 0, 1,1,true,  "android_text_wifi_jmm");
                sampleListener_jmm = new SampleListener_JmmDNS(logger, registry);

                registry.registerService(serviceInfo);
                registry.addServiceListener(service_type, sampleListener_jmm);
            } catch (IOException ex) {
                logger.log(Level.ALL, "_NSD_sss jmmdns exception in onstart()", ex);
            }
            return null;
        }

        private InetAddress getDeviceIpAddress(WifiManager wifi) {
            InetAddress result = null;
            try {
                // default to Android localhost
                result = InetAddress.getLocalHost();
                // figure out our wifi address, otherwise bail
                WifiInfo wifiinfo = wifi.getConnectionInfo();
                int intaddr = wifiinfo.getIpAddress();
                byte[] byteaddr = new byte[] {
                        (byte) (intaddr & 0xff),
                        (byte) (intaddr >> 8 & 0xff),
                        (byte) (intaddr >> 16 & 0xff),
                        (byte) (intaddr >> 24 & 0xff) };
                result = InetAddress.getByAddress(byteaddr);
            } catch (UnknownHostException ex) {
                logger.log(Level.ALL, "_NSD_sss jmmdns exception getDeviceIpAddress(): ", ex);
            }

            return result;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

        }

    }


    private static class SampleListener_JmDNS implements ServiceListener {

        Logger logger;
        JmDNS jmdns;

        public SampleListener_JmDNS(Logger logger, JmDNS jmdns){
            this.logger = logger;
            this.jmdns = jmdns;
        }
        @Override
        public void serviceAdded(ServiceEvent event) {
            logger.log(Level.ALL, "SSS_mohammad_in_android_EDGEKEEPER_NSD_added:" + " Name: " + event.getInfo().getName() + ", Text: " + new String(event.getInfo().getTextBytes()));
            System.out.println("SSS_mohammad_in_android_EDGEKEEPER_NSD_added:" + " Name: " + event.getInfo().getName() + ", Text: " + new String(event.getInfo().getTextBytes()));
            jmdns.requestServiceInfo(event.getType(), event.getName(), 5000);
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            logger.log(Level.ALL, "SSS_mohammad_in_android_EDGEKEEPER_NSD_removed:" + " Name: " + event.getInfo().getName() + ", Text: " + new String(event.getInfo().getTextBytes()));
            System.out.println("SSS_mohammad_in_android_EDGEKEEPER_NSD_removed:" + " Name: " + event.getInfo().getName() + ", Text: " + new String(event.getInfo().getTextBytes()));
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            logger.log(Level.ALL, "SSS_mohammad_in_android_EDGEKEEPER_NSD_resolved:" + " Name: " + event.getInfo().getName() + ", Text: " + new String(event.getInfo().getTextBytes()));
            System.out.println( "SSS_mohammad_in_android_EDGEKEEPER_NSD_resolved:" + " Name: " + event.getInfo().getName() + ", Text: " + new String(event.getInfo().getTextBytes()));

        }
    }


    private static class SampleListener_JmmDNS implements ServiceListener {

        Logger logger;
        JmmDNS jmmdns;

        public SampleListener_JmmDNS(Logger logger, JmmDNS jmmdns){
            this.logger = logger;
            this.jmmdns = jmmdns;
        }
        @Override
        public void serviceAdded(ServiceEvent event) {
            logger.log(Level.ALL, "SSS_mohammad_in_android_EDGEKEEPER_NSD_added:" + " Name: " + event.getInfo().getName() + ", Text: " + new String(event.getInfo().getTextBytes()));
            System.out.println("SSS_mohammad_in_android_EDGEKEEPER_NSD_added:" + " Name: " + event.getInfo().getName() + ", Text: " + new String(event.getInfo().getTextBytes()));
            jmmdns.requestServiceInfo(event.getType(), event.getName(), 5000);
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            logger.log(Level.ALL, "SSS_mohammad_in_android_EDGEKEEPER_NSD_removed:" + " Name: " + event.getInfo().getName() + ", Text: " + new String(event.getInfo().getTextBytes()));
            System.out.println("SSS_mohammad_in_android_EDGEKEEPER_NSD_removed:" + " Name: " + event.getInfo().getName() + ", Text: " + new String(event.getInfo().getTextBytes()));
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            logger.log(Level.ALL, "SSS_mohammad_in_android_EDGEKEEPER_NSD_resolved:" + " Name: " + event.getInfo().getName() + ", Text: " + new String(event.getInfo().getTextBytes()));
            System.out.println( "SSS_mohammad_in_android_EDGEKEEPER_NSD_resolved:" + " Name: " + event.getInfo().getName() + ", Text: " + new String(event.getInfo().getTextBytes()));

        }
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
            case R.id.menu_setting:
                showSetting();
                return true;
            case R.id.menu_account:
                showAccount();
                return true;
            case R.id.menu_debug:
                showDebug();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showDebug() {
        String data = "from the other side";
        Intent intent = new Intent(MainActivitySCRAP.this,
                DebugActivity.class);
        intent.putExtra("hello", data);
        startActivity(intent);
    }

    private void showSetting() {
        Intent intent = new Intent(MainActivitySCRAP.this, SettingsActivity.class);
        startActivity(intent);
    }

    public void showAccount(){
        Intent intent = new Intent(MainActivitySCRAP.this, AccountActivity.class);
        startActivity(intent);
    }


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



    //a child class that will run in a thread that will periodically update gridview on MainActivity to show all available devices in this edge
    class UpdateGridView implements Runnable {

        Context context;
        Activity activity;
        GridView GV;

        public UpdateGridView(Context c, Activity activity, GridView gv){
            this.context = c;
            this.activity = activity;
            this.GV = gv;
        }

        @Override
        public void run() {
            try {

                ArrayList<GVItem> pinned;
                ArrayList<GVItem> unpinned;
                ArrayList<String> temp;
                ArrayList<GVItem> cloud;

                while (true) {

                    pinned = new ArrayList<>();
                    unpinned = new ArrayList<>();
                    temp = new ArrayList<>();
                    cloud = new ArrayList<>();

                    //HANDLE GNS/CLOUD INFORMATION HERE
                    if(ValueStore.GNS_status!=null && ValueStore.GNS_status.get()==0){

                        //cloud is always pinned
                        cloud.add(new GVItem(true, "Cloud", true, getApplicationContext(), findViewById(android.R.id.content).getRootView()));

                    }else{

                        //there was a valid item found in Queue that is false
                        cloud.add(new GVItem(false, "Cloud", true, getApplicationContext(), findViewById(android.R.id.content).getRootView()));
                    }


                    //we fetch edge replica and topology information only if EKClient is connected
                    if(ValueStore.ZKClient_status!=null && ValueStore.ZKClient_status.get()==0) {

                        //get my name from EKClient
                        if(ValueStore.myName==null) {
                            ValueStore.myName = EKClient.getOwnAccountName();
                        }

                        //HANDLE EDGE REPLICA STATUS INFORMATION HERE
                        try {
                            EdgeStatus status = null;
                            status = EKHandler.edgeStatus;
                            if (status != null) {

                                //get all guids which formed this edge
                                Set<String> replicaGUIDs = status.replicaMap.keySet();

                                System.out.println("xyz replica info (guid): " + replicaGUIDs);

                                //iterate over each guid
                                for (String guid : replicaGUIDs) {

                                    //get name for each GUID
                                    String name = EKClient.getAccountNamebyGUID(guid);

                                    if (name != null) {

                                        //trim name
                                        name = name.substring(0, name.indexOf("."));

                                        //check if this name has already been handled
                                        if (!temp.contains(name)) {

                                            //add this name to res
                                            if (ValueStore.pinnedItems.contains(name)) {
                                                pinned.add(new GVItem(true, name, true, getApplicationContext(), findViewById(android.R.id.content).getRootView()));
                                            } else {
                                                unpinned.add(new GVItem(true, name, false, getApplicationContext(), findViewById(android.R.id.content).getRootView()));
                                            }

                                            //add name into temp list
                                            temp.add(name);

                                        }
                                    } else {
                                        System.out.println("XYZ guid to name conversion failed!");
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("Exception is processing edge replica information");
                            e.printStackTrace();
                        }


                        //HANDLE TOPOLOGY INFORMATION HERE
                        try {
                            Set<String> ips = null;
                            //NOTE: if you came here because you saw an exception in standard output/logcat dont worry this exception is expected and will go away in next loop
                            ips = EKClient.getNetworkInfo().getAllIPs();
                            if (ips != null) {
                                //iterate over each IP
                                for (String ip : ips) {

                                    System.out.println("xyz network info (ip): " + ips);

                                    //get all names for this IP
                                    List<String> names = EKClient.getAccountNamebyIP(ip);

                                    //check if not null or empty
                                    if (names != null && names.size() > 0) {

                                        //trim name
                                        String name = names.get(0).substring(0, names.get(0).indexOf("."));

                                        //check if this name has already been handled
                                        if (!temp.contains(name)) {

                                            //add this name to res
                                            if (ValueStore.pinnedItems.contains(name)) {
                                                pinned.add(new GVItem(true, name, true, getApplicationContext(), findViewById(android.R.id.content).getRootView()));
                                            } else {
                                                unpinned.add(new GVItem(true, name, false, getApplicationContext(), findViewById(android.R.id.content).getRootView()));
                                            }

                                            //add name into temp list
                                            temp.add(name);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.log(Level.ALL, "Exception is processing topology information");
                            e.printStackTrace();
                        }
                    }

                    //check cloud for last time
                    if(cloud==null || (cloud!=null && cloud.size()==0)){
                        cloud.add(new GVItem(ValueStore.cloudStatusCache, "Cloud", true, getApplicationContext(), findViewById(android.R.id.content).getRootView()));
                    }

                    //cloud = cloud + pinned + pinnedItems + unpinned
                    //add pinned item(CONNECTED) into cloud list
                    cloud.addAll(pinned);

                    //add pinned items(DISCONNECTED) into cloud list
                    for(String pinnedDisItem: ValueStore.pinnedItems){
                        if(!temp.contains(pinnedDisItem)){
                            cloud.add(new GVItem(false, pinnedDisItem, true, getApplicationContext(), findViewById(android.R.id.content).getRootView()));
                            temp.add(pinnedDisItem);
                        }
                    }

                    //add unpinned (CONNECTED) items into cloud list
                    cloud.addAll(unpinned);

                    //delete myself
                    if(ValueStore.myName!=null) {
                        for (int i = 0; i < cloud.size(); i++) {
                            if (cloud.get(i).getNAME().equals(ValueStore.myName.substring(0, ValueStore.myName.indexOf(".")))) {
                                cloud.remove(i);
                            }
                        }
                    }

                    //test add multiple test
                    //for (int i=0; i< 30; i++){
                    //    cloud.add(new GVItem(true, "camry" + i, true, getApplicationContext(), findViewById(android.R.id.content).getRootView()));
                    //}

                    //update UI
                    //run on UI thread aka mainActivity in this case
                    GVAdapter adapter = new GVAdapter(context, cloud);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //set adapter for gridview
                            GV.setAdapter(adapter);
                        }
                    });


                    //sleep
                    //NOTE: if you came here because you saw an exception in standard output/logcat dont worry this exception is expected and will go away in next loop
                    try {
                        int updateInterval = EKHandler.getEKProperties().getInteger(EKProperties.topoInterval);
                        Thread.sleep(updateInterval);
                    }catch (Exception e ){
                        e.printStackTrace();
                        Thread.sleep(10000);
                    }

                    //testing to print all available network interfaces
                    //Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
                    //for (NetworkInterface netint : Collections.list(nets)) {
                    //    //displayInterfaceInformation(netint);
                    //}


                }


            }catch (Exception e){
                logger.log(Level.ALL, "EXCEPTION in mainActivityin UpdateGridView class" + e);
                e.printStackTrace();
            }
        }

        void displayInterfaceInformation(NetworkInterface netint) throws Exception {
            System.out.printf("INTERFACEX Display name: %s\n", netint.getDisplayName());
            System.out.printf("INTERFACEX Name: %s\n", netint.getName());
            Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
            for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                System.out.printf("INTERFACEX InetAddress: %s\n", inetAddress);
            }
            System.out.printf("INTERFACEX\n\n");
        }


    }



}
