package edu.tamu.cse.lenss.edgeKeeper.android;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

//This class contains static data structures to temporarily hold information
public class ValueStore {

    //A cache for cloud status
    //Default is false, means DISCONNECTED
    public static boolean cloudStatusCache = false;

    //A cache of EdgeKeeper names that contains list of pinned items
    public static List<String> pinnedItems = new ArrayList<>();


    //my EdgeKeeper name (we update it once and read from it forever)
    public static String myName;

    //NOTE: This is not the best way to bypass information from ZKService to MainActivity but the easiest way without changing/breaking the existing code.
    public static AtomicInteger GNS_status = new AtomicInteger();  //-1=null, 0=CONNECTED or RECONNECTED, 1=DISCONNECTED, 2=REGISTRATION_FAILED
    public static AtomicInteger ZKClient_status = new AtomicInteger(); //-1=null, 0=CONNECTED or RECONNECTED, 1=SUSPENDED, 2=LOST
    public static AtomicInteger ZKServer_status = new AtomicInteger(); //-1=null, 0=Leading, 1=Following, 2=Observing, 3=Looking

    //every time activity change happens from Setting/Account to MainActivity, do i need to restart service?
    public static AtomicBoolean restart = new AtomicBoolean();


}
