package edu.tamu.cse.lenss.edgeKeeper.utils;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

import org.apache.curator.framework.state.ConnectionState;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.apache.zookeeper.server.quorum.QuorumPeer.ServerState;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.RouteInfo;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.StatFs;
import android.preference.PreferenceManager;
import edu.tamu.cse.lenss.edgeKeeper.server.RequestTranslator;

public class EKUtilsAndroid extends EKUtils{
	
	ConnectivityManager connectivityManager;
	SharedPreferences sharedPref;

	protected Context context;

	
	public EKUtilsAndroid(EKProperties prop, Context applicationContext) {
		super(prop);
		this.context = applicationContext;
		
		this.connectivityManager = (ConnectivityManager) applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		//this.sharedPref = applicationContext.getSharedPreferences(EKConstants.PREFERENCE_FILE, Context.MODE_PRIVATE);
		
	}

	//this function fetches this devices status information
	//this function fetches this devices status information
		@Override
		public JSONObject getDeviceStatus() {
			
			JSONObject jo = null;
			
			try {
				
				
				//getEdgeStatus cpu available memory in bytes
		    	long availmemory = ANDROIDgetAvailJVMMemoryInBytes();
		    	
		    	//getEdgeStatus battery percentage
		    	int batteryperc = ANDROIDgetBatteryPercentage();
		    	
		    	//getEdgeStatus number of cpu cores
		    	int numofcores = ANDROIDgetNumberOfCores();
		    	
		    	//getEdgeStatus size of free external memory
		    	long freeexternalmemory = ANDROIDgetFreeExternalMemoryInBytes();


		        //create a new json object for device status report
		        jo = new JSONObject();

		        
		        jo.put(RequestTranslator.requestField, RequestTranslator.putDeviceStatus);
		        jo.put("DEVICE", "ANDROID");
				jo.put(availableJVMmemory, availmemory);
				jo.put(batteryPercentage, batteryperc);
				jo.put(numberOfCores, numofcores);
				jo.put(freeExternalMemory, freeexternalmemory);

			}catch(JSONException e) {
				e.printStackTrace();
			}
			
	        //return
	        return jo;
		}
		
		
	    //gets available JVM memory info of the system
	    private long ANDROIDgetAvailJVMMemoryInBytes()    {
	        return Runtime.getRuntime().freeMemory();
	    }

	    //gets battery level(0 to 100)
	    private int ANDROIDgetBatteryPercentage() {
	        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
	        Intent batteryStatus = this.context.registerReceiver(null, iFilter);
	        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
	        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;
	        float batteryPct = level / (float) scale;
	        return (int) (batteryPct * 100);
	    }

	    //getEdgeStatus number of cores in the device
	    private int ANDROIDgetNumberOfCores() {
	    	return Runtime.getRuntime().availableProcessors();
	    }

	    //getEdgeStatus memory available in sdcard
	    private static long ANDROIDgetFreeExternalMemoryInBytes() {
	        return ANDROIDgetFreeMemory(Environment.getExternalStorageDirectory());
	    }
	    private static long ANDROIDgetFreeMemory(File path) {
	        if ((null != path) && (path.exists()) && (path.isDirectory())) {
	            StatFs stats = new StatFs(path.getAbsolutePath());
	            return stats.getAvailableBlocksLong() * stats.getBlockSizeLong();
	        }
	        return -1;
	    }
	    
	    
	    
	    
	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStop() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onGNSStateChnage(
			edu.tamu.cse.lenss.edgeKeeper.server.GNSClientHandler.ConnectionState connectionState) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCuratorStateChange(ConnectionState newState) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onZKServerStateChange(ServerState newServStatus) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onError(String string) {
		// TODO Auto-generated method stub
		
	}
	
	   /**
     * This function tries to find out the GNS server addresses by looking into
     * the DNS server address Our inherent assumption is that the GNS server is 
     * running on the same machine pointed as the DNS server
     * @return
     */
    public Set<String> getDnsAddress(){
        Set <String> dnsServerList =  new HashSet<String>();
        for (Network network:connectivityManager.getAllNetworks()){
            if (connectivityManager.getNetworkInfo(network). isConnected()) {
                LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
                for(InetAddress addr:linkProperties.getDnsServers())
                	if (!(addr.isLoopbackAddress() || addr.isAnyLocalAddress() || addr.isLinkLocalAddress()))
                		dnsServerList.add(addr.getHostAddress());
            }
        }
        //logger.log(Level.ALL, "[GNS] Obtained a list of DNS as:"+dnsServerList.toString());
        return dnsServerList;
    }

    public Set<String> getDefaultGateway(){
    	Set<String> defaultGateway = new HashSet<String>();
        for (Network network:connectivityManager.getAllNetworks()){
            if (connectivityManager.getNetworkInfo(network). isConnected()) {
                LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
                for(RouteInfo route:linkProperties.getRoutes()) {
                	InetAddress addr = route.getGateway();
                	if (!(addr.isLoopbackAddress() || addr.isAnyLocalAddress() || addr.isLinkLocalAddress()))
                		defaultGateway.add(addr.getHostAddress());
                }
            }
        }
        //logger.log(Level.ALL, "[GNS] Obtained a list of Gateways:"+defaultGateway.toString());
        return defaultGateway;
    }

	/**
	 * This function initializes log for Android where the preference is stored in
	 * App preference storage
	 * 
	 * @param context
	 * @throws IOException
	 */
	
//	public static void initLogger(String loggerFilePath, Level logLevel) throws IOException {
//	// First let the logger show the messages to System.out
//	Logger rootLogger = Logger.getRootLogger();
//	rootLogger.removeAllAppenders();
//	rootLogger.setLevel(logLevel);
//	rootLogger.addAppender(new ConsoleAppender(new PatternLayout("[%-5p] %d (%c{1}): %m%n"), "System.out"));
//
//	logger.info("Trying to initialize logger with " + logLevel + " mode at " + loggerFilePath);
//	PatternLayout layout = new PatternLayout("[%-5p] %d (%c{1}): %m%n");
//	RollingFileAppender appender = new RollingFileAppender(layout, loggerFilePath);
//	appender.setName("myFirstLog");
//	appender.setMaxFileSize("100MB");
//	appender.activateOptions();
//	rootLogger.addAppender(appender);
//	rootLogger.info("\n\n======================== New Logger Initialized ============================");
//	rootLogger.info("Logfile with level " + logLevel + " Stored at: " + loggerFilePath);
//}
//    

	public static void initLogger(String loggerFilePath, Level logLevel) throws IOException {
        // Set the log level (DEBUG, INFO, WARN, ERROR)
        org.apache.log4j.Logger rootLogger = org.apache.log4j.Logger.getRootLogger();
		rootLogger.removeAllAppenders();
		rootLogger.setLevel(logLevel);
		rootLogger.addAppender(new ConsoleAppender(new PatternLayout("[%-5p] %d (%c{1}): %m%n"), "System.out"));
        new ConsoleAppender(new PatternLayout("%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n"));

//        // Set appender threshold (optional)
//        consoleAppender.setThreshold(Level.DEBUG);
//
//        // Add the appender to the root logger
//        rootLogger.addAppender(consoleAppender);
		logger.info("Trying to initialize logger with " + logLevel + " mode at " + loggerFilePath);
		PatternLayout layout = new PatternLayout("[%-5p] %d (%c{1}): %m%n");
		RollingFileAppender appender = new RollingFileAppender(layout, loggerFilePath);
		appender.setName("myFirstLog");
		appender.setMaxFileSize("100MB");
		appender.activateOptions();
		rootLogger.addAppender(appender);
		rootLogger.info("\n\n======================== New Logger Initialized ============================");
		rootLogger.info("Logfile with level " + logLevel + " Stored at: " + loggerFilePath);
    }
    
    
    public static void initLoggerAndroid(Context context) throws IOException {

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

		// Map<String, ?> map = sharedPreferences.getAll();
		// System.out.println(map);

		// First getEdgeStatus the log properties from stored preference of the App
		// SharedPreferences sharedPref =
		// context.getSharedPreferences(EKConstants.PREFERENCE_FILE,
		// Context.MODE_PRIVATE);
		String logFileName = sharedPreferences.getString("log_file_name_key", "initial.log");
		String logLevel = sharedPreferences.getString("logging_level_key", "DEBUG");

		String homeDir = Environment.getExternalStorageDirectory().toString() + EKConstants.HOME_DIR_ANDR;
		initLogger(homeDir + logFileName, Level.toLevel(logLevel));
	}

	public void bindSocketToInterface(DatagramSocket sock) throws IOException {
		InetAddress addr = sock.getLocalAddress();
		for (Network network: this.connectivityManager.getAllNetworks())
            if (connectivityManager.getNetworkInfo(network). isConnected()) {
            	for(LinkAddress x:connectivityManager.getLinkProperties(network).getLinkAddresses()) {
            		//logger.log(Level.ALL, "IP address of the interface: "+x.getAddress().toString());
            		if(x.getAddress().equals(addr)) {
            		network.bindSocket(sock);
            		logger.info("Socket for IP" +addr+" is binded to interface Network: "+connectivityManager.getNetworkInfo(network).getTypeName());
            		return;
            		}
            	}
            }
		logger.error("Could not bind the socket to any interface "+addr.toString());
	}

 
}
