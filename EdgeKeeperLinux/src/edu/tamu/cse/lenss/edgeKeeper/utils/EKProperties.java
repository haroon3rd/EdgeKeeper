package edu.tamu.cse.lenss.edgeKeeper.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EKProperties extends Properties{

	
	//Properties related to KedgeKeeper
	public static final String p12Path = "P12_FILE_PATH";
	public static final String p12pass = "P12_FILE_PASSWORD";
	public static final String gnsServAddr = "GNS_SERVER_ADDRESS";
	public static final String enableMaster = "ENABLE_MASTER";
	public static final String ekMaster = "EDGE_KEEPER_MASTER";
	public static final String noReplica = "NUMBER_OF_REPLICA";
	public static final String neighborIPs = "NEIGHBOR_IPS";
	
	public static final String enableRealIP = "ENABLE_REAL_IP_REPORTING";
	public static final String isRunningOnCloud = "IS_RUNNING_ON_CLOUD";

	//For edge topology management and edge merger
	public static final String deviceStatusInterval = "DEVICE_STATUS_INTERVAL";
	public static final String topoInterval = "TOPOLOGY_INTERVAL";
	public static final String topoCleanupIteration = "TOPOLOGY_CLEANUP_ITERATION";
	public static final String mergerGuidInterval = "MERGE_GUID_INTERVAL";
	public static final String mergerMdfsInterval = "MERGE_MDFS_INTERVAL";
	
	
	
	
	//Properties related to Zookeeper
	public static final String tickTime = "tickTime";
	public static final String initLimit = "initLimit";
	public static final String syncLimit = "syncLimit";
	public static final String dataDir = "dataDir";
	public static final String clientPort = "clientPort";
	
	

	public static boolean validateField(String key, Object vo) {
		if ( key == null || key.isEmpty() || vo == null )
			return false;		
		String value =null;
		if(vo instanceof String)
			value = (String) vo;
		else
			value = String.valueOf(vo);
		EKUtils.logger.debug( "validating Properties "+key+": "+value.toString());
		switch (key) {
		case p12Path:
			if(! value.endsWith(".p12")) 
				return false;
			break;
		case gnsServAddr:
		case neighborIPs:
		case ekMaster:
			if(! (validIP(value) || value.toLowerCase().equals("auto")))
				return false;
			break;
			

		case enableMaster:
		case enableRealIP:
			if(! ("true".equals(value.toLowerCase()) || "false".equals(value.toLowerCase()) ))
				return false;
			break;
			
		case deviceStatusInterval:
		case topoInterval:
		case topoCleanupIteration:
		case mergerGuidInterval:
		case mergerMdfsInterval:
		case tickTime:
		case initLimit:
		case syncLimit:
		case clientPort:
		case noReplica:
			if(!validInt(value))
				return false;
			break;
		}
		//return true by default
		return true;
	}
	
	/**
	 * This function validates if all the properties values are acceptable or not. If not acceptable, then it raises Exception.
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public boolean validate() throws IllegalArgumentException, IllegalAccessException {
		EKUtils.logger.trace("Validating all properties:");
		for(Field f:  EKProperties.class.getDeclaredFields()) {
			String key;
			key = (String) f.get(this);
			String value = this.getProperty(key);
			//EKUtils.logger.log(Level.ALL,"Validating key:"+key+" value:"+value);
			if ( ! validateField(key,value)) { 
				EKUtils.logger.error("Illegal value for key:"+key+" value:"+value);
				throw new IllegalArgumentException("Illegal argument. key:"+key+" value:"+value);
			}
			
		}
		return true;
	}
	
	
	public Object setProperty(String key, Object value){
	    if (value instanceof String)
	        return this.setProperty(key, (String) value);
	    else
	        return this.setProperty(key, String.valueOf(value) );
    }
	
	public static boolean validInt(String value) {
		try {
			Integer.parseInt(value);
		} catch( NumberFormatException e) {
			return false;
		}
		return true;
	}
	
	public String[] getGNSAddr(){
		return this.getProperty(this.gnsServAddr).split(",");
	}
	
	
	/**
	 * Check if the string represent multiple valid IPs seperated by ','
	 * A sample was taken from https://stackoverflow.com/questions/4581877/validating-ipv4-string-in-java
	 * @param ip
	 * @return
	 */
	public static boolean validIP (String ips) {
		if ( ips == null || ips.isEmpty() ) {
            return false;
        }
		for (String ip: ips.split(",")) {
			try {
		        String[] parts = ip.split( "\\." );
		        if ( parts.length != 4 ) {
		            return false;
		        }
		        for ( String s : parts ) {
		            int i = Integer.parseInt( s );
		            if ( (i < 0) || (i > 255) ) {
		                return false;
		            }
		        }
		        if ( ip.endsWith(".") ) {
		            return false;
		        }
		    } catch (NumberFormatException nfe) {
		        return false;
		    }

		}
		return true;
	}
	
	public long getTopoInterval() {
		return getInteger(topoInterval);
	}
	
	public int getInteger(String key) {
		return Integer.valueOf( this.getProperty(key));
	}
	
	
    public String getAccountName () {
    	String p12FilePath = this.getProperty(p12Path);
        File f = new File(p12FilePath);
        String fileName = f.getName();
        String accountName = fileName.substring(0, fileName.lastIndexOf('.'));
        EKUtils.logger.debug("p12Filepath: "+p12FilePath+"Stripped accountname: "+ accountName);
        return accountName;
    }
    
    public static EKProperties loadFromFile(String ekPropPath) throws IllegalArgumentException, IOException, IllegalAccessException{
    	EKProperties ekProp = new EKProperties();
    	
		FileInputStream input = new FileInputStream(ekPropPath);
		ekProp.load(input);
		input.close();
		ekProp.validate();
		return ekProp;	 
    }
    
    public String getOwnGUID() {
    	return this.getProperty("GUID");
    }
    public void setGUID(String guid) {
    	this.setProperty("GUID", guid);
    }
    
    public long getTicktime() {
    	try {
			return Math.max(2000, Integer.parseInt(tickTime) );
		} catch( NumberFormatException e) {
			return 2000;
		}
    }
    
    public boolean getBoolean(String key) {
    	return this.getProperty(key).toLowerCase().equals("true") ? true : false;
    }
    
//    public int getNoReplicas() {
//    	try {
//			return Integer.parseInt(noReplica) ;
//		} catch( NumberFormatException e) {
//			return 1;
//		}
//    }
}
