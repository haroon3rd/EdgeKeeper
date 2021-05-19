package edu.tamu.cse.lenss.edgeKeeper.android;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;


//Function in this class restarts the Android Service, that will also restart everything
public class Autostart {

    static Logger logger = Logger.getLogger(Autostart.class);



    static boolean isEKServiceRunning(Context context) {
        boolean status = EKService.isRunning.get();
        logger.log(Level.ALL, "Checking "+EKService.class.getSimpleName()+" status:"+status);
        return status;

    }

    static void  startEKService(Context context){
        logger.info("Trying to start EKService from activity");
        Intent intent = new Intent(context, EKService.class);

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
