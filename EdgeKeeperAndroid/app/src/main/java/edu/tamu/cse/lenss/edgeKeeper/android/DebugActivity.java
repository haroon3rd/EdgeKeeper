package edu.tamu.cse.lenss.edgeKeeper.android;

import android.app.Activity;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.concurrent.atomic.AtomicBoolean;

import edu.tamu.cse.lenss.edgeKeeper.server.EKHandler;
import edu.tamu.cse.lenss.edgeKeeper.server.GNSClientHandler;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKProperties;

import static edu.tamu.cse.lenss.edgeKeeper.android.ValueStore.GNS_status;
import static edu.tamu.cse.lenss.edgeKeeper.android.ValueStore.ZKClient_status;
import static edu.tamu.cse.lenss.edgeKeeper.android.ValueStore.ZKServer_status;

public class DebugActivity extends AppCompatActivity {

    public Thread debugThread;
    AtomicBoolean isRunning = new AtomicBoolean(false);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        //start the thread
        this.debugThread = new Thread(new temporaryThread(this, getApplicationContext()));
        this.debugThread.start();

    }

    @Override
    protected void onDestroy() {


        //change the boolean
        isRunning.set(false);

        try {
            //interrupt the thread
            if (this.debugThread != null) {
                this.debugThread.interrupt();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        super.onDestroy();


    }



    // class that is run temporarily as long as user is inside DebugActivity
    class temporaryThread implements Runnable{

        Activity activity;
        Context context;
        private String res_gns;
        private String res_zkclient;
        private String res_zkserver;


        public temporaryThread(Activity activity, Context context){
            this.activity = activity;
            this.context = context;
        }

        @Override
        public void run() {
            isRunning.set(true);

            TextView gns = findViewById(R.id.debug_gns);
            TextView zkclient = findViewById(R.id.debug_zkclient);
            TextView zkserver = findViewById(R.id.debug_zkserver);

            try {
                while (isRunning.get()) {

                    //get and set gns status
                    res_gns = "GNS:";
                    if(GNS_status!=null){
                        int gns_stat = GNS_status.get();
                        if(gns_stat==-1){
                            res_gns += null;
                        }else if(gns_stat==0){
                            res_gns += GNSClientHandler.ConnectionState.CONNECTED;
                        }else if(gns_stat==1){
                            res_gns += GNSClientHandler.ConnectionState.DISCONNECTED;
                        }else if(gns_stat==2){
                            res_gns += GNSClientHandler.ConnectionState.REGISTRATION_FAILED;
                        }
                    }

                    //get and set zkclient status
                    res_zkclient = "ZKClient:";
                    if(ZKClient_status!=null){
                        int zkclient_stat = ZKClient_status.get();
                        if(zkclient_stat==-1){
                            res_zkclient += null;
                        }else if(zkclient_stat==0){
                            res_zkclient += "CONNECTED";
                        }else if(zkclient_stat==1){
                            res_zkclient += "SUSPENDED";
                        }else if(zkclient_stat==2){
                            res_zkclient += "LOST";
                        }
                    }

                    //get and set zkserver status
                    res_zkserver = "ZKServer:";
                    if(ZKServer_status!=null){
                        int zkserver_stat = ZKServer_status.get();
                        if(zkserver_stat==-1){
                            res_zkserver += null;
                        }else if(zkserver_stat==0){
                            res_zkserver += "Leading";
                        }else if(zkserver_stat==1){
                            res_zkserver += "Following";
                        }else if(zkserver_stat==2){
                            res_zkserver += "Observing";
                        }else if(zkserver_stat==3){
                            res_zkserver += "Looking";
                        }
                    }

                    //update in ui thread
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            gns.setText(res_gns);
                            zkclient.setText(res_zkclient);
                            zkserver.setText(res_zkserver);
                        }
                    });

                    //sleep
                    try {
                        int updateInterval = EKHandler.getEKProperties().getInteger(EKProperties.topoInterval);
                        Thread.sleep(updateInterval);
                    }catch (Exception e ){
                        e.printStackTrace();
                        Thread.sleep(10000);
                    }

                }
            }catch (Exception e){
                e.printStackTrace();
                isRunning.set(false);
            }

        }
    }


}