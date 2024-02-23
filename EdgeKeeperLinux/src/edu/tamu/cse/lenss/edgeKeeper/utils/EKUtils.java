package edu.tamu.cse.lenss.edgeKeeper.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.curator.framework.state.ConnectionState;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.apache.zookeeper.server.quorum.QuorumPeer.ServerState;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.tamu.cse.lenss.edgeKeeper.server.EKHandler;
import edu.tamu.cse.lenss.edgeKeeper.server.GNSClientHandler;


/**
 * This class contains miscleneous useful functionalities
 * 
 * @author sbhunia
 *
 */
public abstract class EKUtils {
//	public static final Logger logger = LoggerFactory.getLogger(EKUtils.class);
	public static final Logger logger = LoggerFactory.getLogger(EKUtils.class);
//	static final Logger logger = LoggerFactory.getLogger(GNSClientHandler.class.getName());
	EKProperties ekProp;

	public static final String availableJVMmemory = "availableJVMmemory";
	public static final String batteryPercentage = "batteryPercentage";
	public static final String numberOfCores = "numberOfCores";
	public static final String freeExternalMemory = "freeExternalMemory";

	/**
	 * This is a abstract method to be defined
	 */
	// public abstract void printResult(String message);
	public abstract void onStart();

	public abstract void onStop();

	public abstract void onGNSStateChnage(GNSClientHandler.ConnectionState connectionState);

	public abstract void onCuratorStateChange(ConnectionState newState);

	public abstract void onZKServerStateChange(ServerState newServStatus);

	public abstract void onError(String string);
	
	public abstract void bindSocketToInterface(DatagramSocket sock) throws IOException;

	public abstract Set<String> getDnsAddress();
	public abstract Set<String> getDefaultGateway();

	public abstract JSONObject getDeviceStatus();
	
	public enum NetworkInterfaceType{
		ETHERNET,
		WIFI,
		MOBILE,
		WIMAX,
		BLUETOOTH,
		UNKNOWN,
	};

//	/**
//	 * This is the constructor to be used in Android Environment
//	 * 
//	 * @param applicationContext
//	 * @param gnsAddrStr
//	 * @throws UnknownHostException
//	 */
//	public EKUtils(EKProperties prop, Context applicationContext) {
//		this.ekProp = prop;
//	}

	/**
	 * This constructor to be used in Linux environment
	 * 
	 * @param gnsAddrStr
	 * @throws UnknownHostException
	 */
	public EKUtils(EKProperties prop) {
		this.ekProp = prop;
	}

	/**
	 * This function checks if the code running in Android or Linux
	 * 
	 * @return
	 */
	public static boolean isAndroid() {
		return System.getProperty("java.vm.name").toUpperCase().equals("DALVIK") ? true : false;
	}

	/**
	 * Converts a string of IP addresses to list, the IPs mut be separated by ','
	 * 
	 * @param addrStr
	 * @return
	 * @throws UnknownHostException
	 */
	public static List<InetAddress> strToInet(String addrStr) {
		if (addrStr == null || addrStr.length() == 0)
			return null;
		if (addrStr.toUpperCase().equals("DHCP"))
			return null;
		String[] strList = addrStr.split(",");
		List<InetAddress> inetList = new ArrayList<InetAddress>();
		for (String s : strList)
			try {
				inetList.add(InetAddress.getByName(s));
			} catch (UnknownHostException e) {
				logger.warn("Problem parsing string to Inet socket. " + s + " " + e);
			}
		logger.debug("These are the InetAddress for GNS: " + inetList.toString());
		return inetList;
	}

	public static Set<String> getAllAPs() {
		Set<String> newIPs = getOwnIPv4s();

		// Check if this device wants the Real IP to be added to the IP detection list
		if (EKHandler.getEKProperties().getBoolean(EKProperties.enableRealIP)) {
			String realIP = EKUtils.getRealIP();
			if (realIP != null)
				newIPs.add(realIP);
		}
		return newIPs;
	}

	public static String getRealIP() {
		String ipAddress = null;
		BufferedReader in = null;
		System.currentTimeMillis();
		try {
			URL whatismyip = new URL(EKConstants.REAL_ID_URL);
			in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
			ipAddress = in.readLine(); // you getEdgeStatus the IP as a String
		} catch (IOException e) {
			logger.trace("Problem in fetching the real IP");
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					logger.warn("Problem in closing the Stream reader");
				}
			}
		}
		System.currentTimeMillis();
		// logger.log(Level.ALL,"Real IP address: " + ipAddress+", msec lapsed:
		// "+(t2-t1));
		return ipAddress;
	}

	/**
	 * This function returns the IP v4 address associated with different interfaces.
	 * 
	 * @return
	 * @throws SocketException
	 */
	public static Set<String> getOwnIPv4s() {
		Set<String> addrList = new HashSet<String>();
		try {
			Enumeration<NetworkInterface> enumNI = NetworkInterface.getNetworkInterfaces();
			while (enumNI.hasMoreElements()) {
				NetworkInterface ifc = enumNI.nextElement();
				if (ifc.isUp()) {
					Enumeration<InetAddress> enumAdds = ifc.getInetAddresses();
					while (enumAdds.hasMoreElements()) {
						InetAddress addr = enumAdds.nextElement();
						// Now discard the local and loopback addresses
						if (!(addr.isLoopbackAddress() || addr.isAnyLocalAddress() || addr.isLinkLocalAddress()))
							if (addr instanceof Inet4Address)
								addrList.add(addr.getHostAddress());
					}
				}
			}
		} catch (SocketException e) {
			logger.error("Problem in obtaining IP address for own device", e);
		}
		return addrList;
	}

	/**
	 * This function returns the IP v6 address associated with different interfaces.
	 * 
	 * @return
	 * @throws SocketException
	 */
	public List<String> getOwnIPv6s() throws SocketException {
		List<String> addrList = new ArrayList<String>();
		Enumeration<NetworkInterface> enumNI = NetworkInterface.getNetworkInterfaces();
		while (enumNI.hasMoreElements()) {
			NetworkInterface ifc = enumNI.nextElement();
			if (ifc.isUp()) {
				Enumeration<InetAddress> enumAdds = ifc.getInetAddresses();
				while (enumAdds.hasMoreElements()) {
					InetAddress addr = enumAdds.nextElement();
					// logger.log(Level.ALL, "Got addresses: "+addr.getHostAddress());
					// Now discard the local and loopback addresses
					if (!(addr.isLoopbackAddress() || addr.isAnyLocalAddress() || addr.isLinkLocalAddress()))
						if (addr instanceof Inet6Address)
							addrList.add(addr.getHostAddress());
				}
			}
		}
		return addrList;
	}
	
	/**
	 * This function returns the IP v4 address associated with different interfaces.
	 * 
	 * @return
	 * @throws SocketException
	 */
	public static Map<String, NetworkInterfaceType> getOwnIPv4Map() {
		Map<String, NetworkInterfaceType> addrList = new HashMap<String, NetworkInterfaceType>();
		try {
			Enumeration<NetworkInterface> enumNI = NetworkInterface.getNetworkInterfaces();
			while (enumNI.hasMoreElements()) {
				NetworkInterface ifc = enumNI.nextElement();
				if (ifc.isUp()) {
					Enumeration<InetAddress> enumAdds = ifc.getInetAddresses();
					while (enumAdds.hasMoreElements()) {
						InetAddress addr = enumAdds.nextElement();
						// Now discard the local and loopback addresses
						if (!(addr.isLoopbackAddress() || addr.isAnyLocalAddress() || addr.isLinkLocalAddress()))
							if (addr instanceof Inet4Address) {
								NetworkInterfaceType ifcType= null;
								if(ifc.getName().startsWith("e"))
									ifcType=NetworkInterfaceType.ETHERNET;
								else if(ifc.getName().startsWith("w") || ifc.getName().startsWith("p2p"))
									ifcType=NetworkInterfaceType.WIFI;
								else if(ifc.getName().startsWith("rmnet") || ifc.getName().startsWith("pgwtun"))
									ifcType=NetworkInterfaceType.MOBILE;
								else
									ifcType=NetworkInterfaceType.UNKNOWN;
								addrList.put(addr.getHostAddress(), ifcType);
							}
					}
				}
			}
		} catch (SocketException e) {
			logger.error("Problem in obtaining IP address for own device", e);
		}
		return addrList;
	}


	public Set<String> getArpIps() {
		Set<String> ips = new HashSet<String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader("/proc/net/arp"));
			String line;
			while ((line = br.readLine()) != null) {
				String ip = line.split(" ")[0];
				if (EKProperties.validIP(ip))
					ips.add(ip);
			}
		} catch (Exception e) {
			logger.warn("Could not read the ARP cache", e);
		} finally {
			try {
				br.close();
			} catch (Exception e) {

			}
		}
		logger.trace("Obtained list of ips from ARP: " + ips);
		return ips;
	}

	public static String getErrorString(Throwable e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}

	/**
	 * This function initializes the logger using Java code where no properties file
	 * is used
	 * 
	 * @param loggerFilePath
	 * @param logLevel
	 * @throws IOException
	 */
//	public static void initLogger(String loggerFilePath, Level logLevel) throws IOException {
//		// First let the logger show the messages to System.out
//		Logger rootLogger = Logger.getRootLogger();
//		rootLogger.removeAllAppenders();
//		rootLogger.setLevel(logLevel);
//		rootLogger.addAppender(new ConsoleAppender(new PatternLayout("[%-5p] %d (%c{1}): %m%n"), "System.out"));
//
//		logger.info("Trying to initialize logger with " + logLevel + " mode at " + loggerFilePath);
//		PatternLayout layout = new PatternLayout("[%-5p] %d (%c{1}): %m%n");
//		RollingFileAppender appender = new RollingFileAppender(layout, loggerFilePath);
//		appender.setName("myFirstLog");
//		appender.setMaxFileSize("100MB");
//		appender.activateOptions();
//		rootLogger.addAppender(appender);
//		rootLogger.info("\n\n======================== New Logger Initialized ============================");
//		rootLogger.info("Logfile with level " + logLevel + " Stored at: " + loggerFilePath);
//	}

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

	

	public static String sha256(byte[] message) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(message);
			return Base64.getEncoder().encodeToString(hash);
		} catch (Exception ex) {
			logger.error("Problem creating messageDigest sha, ", ex);
			throw new RuntimeException(ex);
		}
	}

	

}
