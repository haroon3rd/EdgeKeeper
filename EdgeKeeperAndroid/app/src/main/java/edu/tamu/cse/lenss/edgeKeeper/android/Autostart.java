package edu.tamu.cse.lenss.edgeKeeper.android;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.IOException;

import edu.tamu.cse.lenss.edgeKeeper.utils.EKProperties;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtilsAndroid;


/**
 * This class will get notification during boottime start the service.
 *
 * The code is taken from  https://stackoverflow.com/questions/7690350/android-start-service-on-boot
 * Modified by: sbhunia
 */
public class Autostart extends BroadcastReceiver
{
    static Logger logger = Logger.getLogger(Autostart.class);

    public void onReceive(Context context, Intent arg1)
    {
        try {
                EKUtilsAndroid.initLoggerAndroid(context);
        } catch (IOException e) {
            System.out.println("Problem with creating logger");
        }

        logger.info("Received broadcast");

        if(isEKServiceRunning(context)){
            logger.info("Service already running. Not running new service");
        }
        else
            startEKService(context);
    }

    /**
     * This function determines whether EKService is already running or not.
     * @param context
     * @return
     */
    public static boolean isEKServiceRunning(Context context) {
        boolean status = EKService.isRunning.get();
        logger.log(Level.ALL, "Checking "+EKService.class.getSimpleName()+" status:"+status);
        return status;


        /*ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        //logger.log(Level.ALL, "Currently runninf services:"+manager.getRunningServices(Integer.MAX_VALUE));
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (EKService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;*/

    }

    static void  startEKService(Context context){
        logger.info("Trying to start EKService from activity");
        Intent intent = new Intent(context, EKService.class);
        //intent.putExtra(EKConstants.SERVICE_SEQ_NO, currentServiceSeq.incrementAndGet());
        //this.startService(intent);

        // This code will take care of closing the problem of shutting down the service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent notificationIntent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, notificationIntent, 0);
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    static void stopEKService(Context context ){
        logger.debug("Trying to stop GNS Service from activity");
        context.stopService(new Intent(context, EKService.class));
    }

    static void restartEKService(Context context){
        stopEKService(context);
        startEKService(context);
    }

}
