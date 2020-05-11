package edu.tamu.cse.lenss.edgeKeeper.utils;

import java.net.SocketImpl;

public class EKConstants {
    
	public static final String HOME_DIR_ANDR = "/distressnet/edge_keeper/";
    
//	public static final String PREFERENCE_FILE = "edu.tamu.cse.lenss.gnshandler.PREFERENCE_FILE";
	public static final String LOG_FILE_NAME = "edu.tamu.cse.lenss.gnshandler.LOG_FILE_NAME";
	public static final String LOG_LEVEL = "edu.tamu.cse.lenss.gnshandler.LOG_LEVEL";

	
	
    
//    public static final String GNS_SERVICE_BROADCAST = "edu.tamu.cse.lenss.gnshandler.PRINT_MESSAGE";
//    public static final String BROADCAST_MESSAGE = "edu.tamu.cse.lenss.gnshandler.MESSAGE";
//    public static final String GNS_SERVICE_EVENT = "edu.tamu.cse.lenss.gnshandler.SERVICE_EVENT";
//    public static final String GNS_SERVICE_ACTIVITY = "edu.tamu.cse.lenss.gnshandler.SERVICE_ACTIVITY";
//    public static final String GNS_SERVICE_START = "edu.tamu.cse.lenss.gnshandler.SERVICE_START";
//    public static final String GNS_SERVICE_STOP = "edu.tamu.cse.lenss.gnshandler.SERVICE_STOP";
//    public static final String GNS_SERVICE_CONNECTED = "edu.tamu.cse.lenss.gnshandler.SERVICE_CONNECTED";
//    public static final String GNS_SERVICE_DISCONNECTED = "edu.tamu.cse.lenss.gnshandler.SERVICE_DISCONNECTED";
//
//    public static final String CERTIFICATE_PATH = "edu.tamu.cse.lenss.gnshandler.CERTIFICATE_PATH";
//    public static final String CERTIFICATE_PASSWORD = "edu.tamu.cse.lenss.gnshandler.CERTIFICATE_PASSWORD";
//    public static final String GNS_SERVER_ADDR = "edu.tamu.cse.lenss.gnshandler.GNS_SERVER_ADDR";
//    public static final String SERVICE_SEQ_NO = "edu.tamu.cse.lenss.gnshandler.SERVICE_SEQ_NO";
    

	public static final int COORDINATOR_PORT = 22221;
    public static final int CLUSTER_MONITOR_SERVER_PORT = 22224;
    public static final int HEALTH_VIEW_PORT = 22225;
    public static final int TOPOLOGY_DISCOVERY_PORT = 22226;
    public static final int TOPOCLOUD_DISCOVERY_PORT = 22227;
	
	public static final String MASTER_NAME = "master.distressnet.org";
	
	public static final String TOPO_BROADCAST_IP = "255.255.255.255";

	
    public static final long IP_CHANGE_DETECT_INTERVAL = 5000;
    
//    public static final long DEVICE_STATUS_UPDATE_INTERVAL = 10000;
    
    public final static long GNS_TIMEOUT = 5000; // Timoeout of 10 seconds
    
    public final static long GNS_CONNECTION_CHECK_INTERVAL = 30000;

	public static final int PING_TIMEOUT = 30000;

	public static final String FIELD_RTT_TO_CLOUD = "FIELD_RTT_TO_CLOUD";

	public static final int ClientSoTimeout = 30000;
	public static final int ClientConnectTimeout = 3000;
    
    public static final long clusterHealthThreshold = 100000;
    
    //public final static long CLUSTER_MONITOR_DEVICE_STATUS_SUBMISSION_INTERVAL = (1000*5);

	public static final String REAL_ID_URL = "http://checkip.amazonaws.com";

	public static final int MAX_EKCLIENT_THREAD = 5;

	public static final int zkServerRestartEpoch = 4;

	public static final int DNS_TIMEOUT = 5; // TImeout in sec

	public static final int DNS_UDP_PORT = 53;
	
	public static final int MDFS_CONCURRENT_THREAD = 1;
    public static final long MDFS_THREAD_TIMEOUT = 29000;

	public static final int REQUEST_SERVER_THREAD = 10;

	public static final String STATUS_FILE_NAME = "EK_RUNNING_STATUS.out";

	public static final int TOPO_BUFFER_SIZE = 64000;

	public static final String TOPO_BRIADCAST_IP = "255.255.255.255";

	public static final double TOPO_INITIAL_PROB = Double.MIN_VALUE;//0.5;

	public static final long GUID_MERGE_INTERVAL = 10000;
	
	public static final long MDFS_MERGE_INTERVAL = 30000;

	public static String graphPath = "graph.png";

	public static final int BRD_SEQ_INCR = 2;
}
