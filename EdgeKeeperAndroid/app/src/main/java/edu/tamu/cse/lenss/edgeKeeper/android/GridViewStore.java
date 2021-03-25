package edu.tamu.cse.lenss.edgeKeeper.android;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

//This class contains static data structures to temporarily hold gridView information
public class GridViewStore {

    //A cache for cloud status
    //Default is false, means DISCONNECTED
    public static boolean cloudStatusCache = false;

    //A cache of EdgeKeeper names that contains list of pinned items
    public static List<String> pinnedItems = new ArrayList<>();


    //my EdgeKeeper name (we update it once and read from it forever)
    public static String myName;

    //NOTE: This is not the best way to bypass information from EKService to MainActivity.
    public static AtomicBoolean GNS_status = new AtomicBoolean();
    public static AtomicBoolean EKClient_status = new AtomicBoolean();


}
