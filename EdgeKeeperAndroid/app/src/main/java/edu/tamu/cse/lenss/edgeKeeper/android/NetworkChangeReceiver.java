package edu.tamu.cse.lenss.edgeKeeper.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;


public class NetworkChangeReceiver extends BroadcastReceiver {

    Context context;

    //default constructor
    private NetworkChangeReceiver(){}


    //public constructor
    public NetworkChangeReceiver(Context context){
        this.context = context;
        System.out.println("NET: inside constructor()");
    }


    @Override
    public void onReceive(Context context, Intent intent) {

        System.out.println("NET: inside onReceive()");

        int status = NetworkUtil.getConnectivityStatusString(context);

        if ("android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction())) {

            if (status == NetworkUtil.NETWORK_STATUS_NOT_CONNECTED) {
                Toast.makeText(context, status, Toast.LENGTH_SHORT).show();
                System.out.println("NET: " + status);
            } else {
                Toast.makeText(context, status, Toast.LENGTH_SHORT).show();
                System.out.println("NET: " + status);
            }
        }
    }
}
