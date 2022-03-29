package edu.tamu.cse.lenss.edgeKeeper.client;


import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MDFSMetadata;
import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.command.LScommand;
import edu.tamu.cse.lenss.edgeKeeper.server.RequestTranslator;
import edu.tamu.cse.lenss.edgeKeeper.topology.TopoGraph;
import edu.tamu.cse.lenss.edgeKeeper.topology.TopoParser;
import edu.tamu.cse.lenss.edgeKeeper.topology.TopoUtils;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;
import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * This is the interface to communicate with EdgeKeeper application running on 
 * the host machine. Any application can use the following functions to run 
 * commands for GNS Service. 
 * 
 * @author sbhunia
 *
 */
public class EKClientOld {
    static final Logger logger = Logger.getLogger(EKClientOld.class);
	public static String SERVER_IP = "127.0.0.1";
	//private static ExecutorService executorService = Executors.newFixedThreadPool(EKConstants.MAX_EKCLIENT_THREAD);
	
	
	/**
	 * This function try to communicate with EdgeKeeper using another thread to
	 * avoid Android's policy of prohibiting "NetworkOnMainThreadException"
	 * 
	 * @param reqJSON is the query / command JSON to the GNS service
	 * @return reply JSON from GNS Service
	 * @throws Exception
	 */
	static JSONObject getResponseFromEK(JSONObject reqJSON) throws InterruptedException,  ExecutionException  {
    	ExecutorService executorService = Executors.newSingleThreadExecutor();
        JSONObject rep= executorService.submit( new MessageTransferoverSocket (reqJSON)).get();
        executorService.shutdownNow();
        return rep;
    }

	
	 /*
	 * The format is like this:
	 * {
	 * 	request: addservice,
	 * 	service: MADOOP,
	 * 	duty: client
	 * }
	*/
	/**
	 * Retrieve the GUID used by the host machine. This GUID is derived from 
	 * the public key of the certificate used for registration.
	 * @return The GUID string 
	 */
	public static String getOwnGuid(){
		String ownGuid = null;
		try {
			JSONObject reqJSON = new JSONObject();
			reqJSON.put(RequestTranslator.requestField, RequestTranslator.getOwnGuidCommand);
			
			JSONObject repJSON = getResponseFromEK(reqJSON);
			String repResult = repJSON.getString(RequestTranslator.resultField);
	
			if (repResult.equals(RequestTranslator.successMessage)) {
				ownGuid = repJSON.getString(RequestTranslator.fieldGUID);
				logger.debug("Own Guid :" +ownGuid);
				
			} else 
				logger.debug("GNS service returns error i.e. problem in executing the desired command");
		} catch (Exception e) {
			logger.error("Communication with GNS-service failed", e);	
			
		}
		return ownGuid;	
	}

	/**
	 * Retrieve the account name of the host machine which is used by GNS. 
	 * This name is derived from the alias of the p12 certificate used in the registration.   
	 * @return Account Name string
	 */	
	public static String getOwnAccountName(){
		String accountName = null;
		try {
			JSONObject reqJSON = new JSONObject();
			reqJSON.put(RequestTranslator.requestField, RequestTranslator.getOwnAccountNameCommand);
			
			JSONObject repJSON = getResponseFromEK(reqJSON);
			String repResult = repJSON.getString(RequestTranslator.resultField);
	
			if (repResult.equals(RequestTranslator.successMessage)) {
				accountName = repJSON.getString(RequestTranslator.fieldAccountName);
				logger.debug("accountName :" +accountName);
			} else 
				logger.debug("GNS service returns error i.e. problem in executing the desired command");
		} catch (Exception e) {
			logger.error("Communication with GNS-service failed", e);		
		}
		return accountName;	
	}

	/**
	 * Fetches the OLSR JSON Info from EdgeKeeper. EdgeKeeper obtains the JSONInfo from OLSR and 
	 * converts the IP addresses with the corresponding GUID. 
	 * @return TopoGraph
	 */
	public static TopoGraph getNetworkInfo(){
		TopoGraph topo = null;
		try {
			JSONObject reqJSON = new JSONObject();
			reqJSON.put(RequestTranslator.requestField, RequestTranslator.getTopologyCommand);
			
			JSONObject repJSON = getResponseFromEK(reqJSON);
			String repResult = repJSON.getString(RequestTranslator.resultField);
	
			if (repResult.equals(RequestTranslator.successMessage)) {
				String topoString = repJSON.getString(RequestTranslator.fieldNetworkInfo);
				//topo = TopoGraph.getGraph(topoString);
				topo = TopoParser.importGraph(topoString);
				logger.debug("Network Info :"+ topo.printToString());
			} else 
				logger.debug("GNS service returns error i.e. problem in executing the desired command");
		} catch (Exception e) {
			logger.error("Communication with GNS-service failed", e);		
		}
		return topo;	
	}

	
		
	/**
	 * This function registers a service and the duty at the GNS server for service discovery.
	 * 
	 * @param ownService What is the name of the service, usually the application name
	 * @param ownDuty What duty it is playing
	 * @return true if the update is successful at the GNS server
	 */
	public static boolean addService(String ownService, String ownDuty) {
		String repResult;
		try {
			JSONObject reqJSON = new JSONObject();
			reqJSON.put(RequestTranslator.requestField, RequestTranslator.addServiceCommand);
			reqJSON.put(RequestTranslator.serviceField, ownService);
			reqJSON.put(RequestTranslator.dutyField, ownDuty);

			JSONObject repJSON = getResponseFromEK(reqJSON);
			repResult = repJSON.getString(RequestTranslator.resultField);
		} catch (Exception e) {
			logger.error("Communication with GNS-service failed", e);
			return false;		
		}
	
		if (repResult.equals(RequestTranslator.successMessage)) {
			logger.debug("Update Successful ");
			return true;
		}else {
			logger.debug("Update failed ");
			return false;
		}	
	}
	

	/**
	 * This function remove the service from GNS server, thus removing the 
	 * application from service discovery.
	 * 
	 * @param targetService
	 * @return true if the update is successful at the GNS server
	 */
	public static boolean removeService(String targetService){
		boolean removeSuccess = false;
		try {
			JSONObject reqJSON = new JSONObject();
			reqJSON.put(RequestTranslator.requestField, RequestTranslator.removeServiceCommand);
			reqJSON.put(RequestTranslator.serviceField, targetService);
			
			JSONObject repJSON = getResponseFromEK(reqJSON);
			String repResult = repJSON.getString(RequestTranslator.resultField);
	
			if (repResult.equals(RequestTranslator.successMessage)) {
				logger.debug("Successfully removed the service from GNS record");
				removeSuccess = true;
			} else 
				logger.debug("GNS service returns error i.e. problem in executing the desired command");
		} catch (Exception e) {
			logger.error("Communication with GNS-service failed", e);		
		}
		return removeSuccess;	
	}

	/**
	 * This function retrieves a list of GUID of the nodes running a targetService with the 
	 * target duty. 
	 * 
	 * @param targetService
	 * @param targetDuty
	 * @return the list of IPs or [] if no record is found in GNS server.
	 */
	
	public static List<String> getPeerGUIDs(String targetService, String targetDuty){
		
		List<String> guidList = new ArrayList<String>();
		try {
			JSONObject reqJSON = new JSONObject();
			reqJSON.put(RequestTranslator.requestField, RequestTranslator.getPeerGUIDCommand);
			reqJSON.put(RequestTranslator.serviceField, targetService);
			reqJSON.put(RequestTranslator.dutyField, targetDuty);

			JSONObject repJSON = getResponseFromEK(reqJSON);
			String repResult = repJSON.getString(RequestTranslator.resultField);
	
			if (repResult.equals(RequestTranslator.successMessage)) {
				JSONArray ipJsonArray = repJSON.getJSONArray(RequestTranslator.fieldGUID); 
				
				for(int j = 0; j < ipJsonArray.length(); j++)
	                guidList.add(ipJsonArray.getString(j));
				logger.debug("Got peers GUIDs as:  "+guidList.toString());
			} else 
				logger.debug("GNS service returns error i.e. problem in executing the desired command");
			
		} catch (Exception e) {
			logger.error("Communication with GNS-service failed", e);		
		}
		return guidList;	
	}
	
	
	/**
	 * This function getEdgeStatus the IP address of the nodes running a targetService with the
	 * target duty. 
	 * 
	 * @param targetService
	 * @param targetDuty
	 * @return the list of IPs or [] if no record is found in GNS server.
	 */
	
	public static List<String> getPeerIPs(String targetService, String targetDuty){
		
		List<String> ipList = new ArrayList<String>();
		try {
			JSONObject reqJSON = new JSONObject();
			reqJSON.put(RequestTranslator.requestField, RequestTranslator.getPeerIPsCommand);
			reqJSON.put(RequestTranslator.serviceField, targetService);
			reqJSON.put(RequestTranslator.dutyField, targetDuty);

			JSONObject repJSON = getResponseFromEK(reqJSON);
			String repResult = repJSON.getString(RequestTranslator.resultField);
	
			if (repResult.equals(RequestTranslator.successMessage)) {
				JSONArray ipJsonArray = repJSON.getJSONArray(RequestTranslator.fieldIP); 
				
				for(int j = 0; j < ipJsonArray.length(); j++)
	                ipList.add(ipJsonArray.getString(j));
				logger.debug("Got peers as:  "+ipList.toString());
			} else 
				logger.debug("GNS service returns error i.e. problem in executing the desired command");
			
		} catch (Exception e) {
			logger.error("Communication with GNS-service failed", e);		
		}
		return ipList;	
	}

	/**
	 * This function getEdgeStatus the host-names of the nodes running a targetService with the
	 * target duty. 
	 * 
	 * @param targetService
	 * @param targetDuty
	 * @return the list of host-names or [] if no record is found in GNS server.
	 */
	
	public static List<String> getPeerNames(String targetService, String targetDuty){
		
		List<String> nameList = new ArrayList<String>();
		try {
			
			
			JSONObject reqJSON = new JSONObject();
			reqJSON.put(RequestTranslator.requestField, RequestTranslator.getPeerNamesCommand);
			reqJSON.put(RequestTranslator.serviceField, targetService);
			reqJSON.put(RequestTranslator.dutyField, targetDuty);

			JSONObject repJSON = getResponseFromEK(reqJSON);
			String repResult = repJSON.getString(RequestTranslator.resultField);
	
			if (repResult.equals(RequestTranslator.successMessage)) {
				JSONArray ipJsonArray = repJSON.getJSONArray(RequestTranslator.fieldAccountName); 
				
				for(int j = 0; j < ipJsonArray.length(); j++)
	                nameList.add(ipJsonArray.getString(j));
				logger.debug("Got peers as:  "+nameList.toString());
			} else 
				logger.debug("GNS service returns error i.e. problem in executing the desired command");
			
		} catch (Exception e) {
			logger.error("Communication with GNS-service failed", e);		
		}
		return nameList;	
	}
	
	
	/**
	 * Retrieve the IP address associated with a GUID
	 * @param targetGUID
	 * @return List of IP addresses
	 */

	public static List<String> getIPbyGUID(String targetGUID){
		
		List<String> ipList = new ArrayList<String>();
		try {
			
			
			JSONObject reqJSON = new JSONObject();
			reqJSON.put(RequestTranslator.requestField, RequestTranslator.getIpByGuidCommand);
			reqJSON.put(RequestTranslator.fieldGUID, targetGUID);

			JSONObject repJSON = getResponseFromEK(reqJSON);
			String repResult = repJSON.getString(RequestTranslator.resultField);
	
			if (repResult.equals(RequestTranslator.successMessage)) {
				JSONArray ipJsonArray = repJSON.getJSONArray(RequestTranslator.fieldIP); 
				
				for(int j = 0; j < ipJsonArray.length(); j++)
	                ipList.add(ipJsonArray.getString(j));
				logger.debug("Got IPs as:  "+ipList.toString());
			} else 
				logger.debug("GNS service returns error i.e. problem in executing the desired command");
			
		} catch (Exception e) {
			logger.error("Communication with GNS-service failed", e);		
		}
		return ipList;	
	}

	/**
	 * Retrieve the IP address associated with a GUID
	 * @param targetIP
	 * @return List of IP addresses
	 */

	public static List<String> getIPbyName(String targetName){
		
		List<String> ipList = new ArrayList<String>();
		try {
			JSONObject reqJSON = new JSONObject();
			reqJSON.put(RequestTranslator.requestField, RequestTranslator.getIpByAccountNameCommand);
			reqJSON.put(RequestTranslator.fieldAccountName, targetName);

			JSONObject repJSON = getResponseFromEK(reqJSON);
			String repResult = repJSON.getString(RequestTranslator.resultField);
	
			if (repResult.equals(RequestTranslator.successMessage)) {
				JSONArray ipJsonArray = repJSON.getJSONArray(RequestTranslator.fieldIP); 
				
				for(int j = 0; j < ipJsonArray.length(); j++)
	                ipList.add(ipJsonArray.getString(j));
				logger.debug("Got IPs as:  "+ipList.toString());
			} else 
				logger.debug("GNS service returns error i.e. problem in executing the desired command");
			
		} catch (Exception e) {
			logger.error("Communication with GNS-service failed", e);		
		}
		return ipList;	
	}


	/**
	 * Retrieve the GUID string associated with a particular Account name
	 * @param accountName
	 * @return GUID string
	 */
	public static String getGUIDbyAccountName(String accountName){
		String guid = null;
		try {
			JSONObject reqJSON = new JSONObject();
			reqJSON.put(RequestTranslator.requestField, RequestTranslator.getGUIDbyAccountNameCommand);
			reqJSON.put(RequestTranslator.fieldAccountName, accountName);
			
			
			JSONObject repJSON = getResponseFromEK(reqJSON);
			String repResult = repJSON.getString(RequestTranslator.resultField);
	
			if (repResult.equals(RequestTranslator.successMessage)) {
				guid = repJSON.getString(RequestTranslator.fieldGUID);
				logger.debug("Own Guid :" +guid);
			} else 
				logger.debug("GNS service returns error i.e. problem in executing the desired command");
		} catch (Exception e) {
			logger.error("Communication with GNS-service failed", e);		
		}
		return guid;	
	}

	/**
	 * Retrieve the Accountname or alias for a particular GUID
	 * @param guid
	 * @return accountName
	 */
	public static String getAccountNamebyGUID(String guid){
		String accountName = null;
		try {
			JSONObject reqJSON = new JSONObject();
			reqJSON.put(RequestTranslator.requestField, RequestTranslator.getAccountNamebyGUIDCommand);
			reqJSON.put(RequestTranslator.fieldGUID, guid);
			
			
			JSONObject repJSON = getResponseFromEK(reqJSON);
			String repResult = repJSON.getString(RequestTranslator.resultField);
	
			if (repResult.equals(RequestTranslator.successMessage)) {
				accountName = repJSON.getString(RequestTranslator.fieldAccountName);
				logger.debug("AccountName :" +accountName);
			} else 
				logger.debug("GNS service returns error i.e. problem in executing the desired command");
		} catch (Exception e) {
			logger.error("Communication with GNS-service failed", e);		
		}
		return accountName;	
	}

	
	
	
	/**
	 * Retrieve the list of account names which has this IP. It may return multiple 
	 * hostnames if multiple nodes updated their GUID record with the targetIP
	 * @param targetIp
	 * @return AccountName
	 */
	public static List<String> getAccountNamebyIP(String targetIp){
		List<String> accountNameList = new ArrayList<String>();
		try {
			JSONObject reqJSON = new JSONObject();
			reqJSON.put(RequestTranslator.requestField, RequestTranslator.getAccountNamebyIPCommand);
			reqJSON.put(RequestTranslator.fieldIP, targetIp);
			
			
			JSONObject repJSON = getResponseFromEK(reqJSON);
			String repResult = repJSON.getString(RequestTranslator.resultField);
	
			if (repResult.equals(RequestTranslator.successMessage)) {
				//accountName = repJSON.getString(RequestTranslator.fieldAccountName);
				
				JSONArray jsonArray = repJSON.getJSONArray(RequestTranslator.fieldAccountName); 
				
				for(int j = 0; j < jsonArray.length(); j++)
					accountNameList.add(jsonArray.getString(j));
				logger.debug("AccountName :" +accountNameList);
			} else 
				logger.debug("GNS service returns error i.e. problem in executing the desired command");
		} catch (Exception e) {
			logger.error("Communication with GNS-service failed", e);		
		}
		return accountNameList;	
	}
	
	/**
	 * Fetch the GUID corresponding to the provided IP address.
	 * @param ip
	 * @return GUID string
	 */
	public static List<String> getGUIDbyIP(String ip){
		List<String> guidList = new ArrayList<String>();
		try {
			JSONObject reqJSON = new JSONObject();
			reqJSON.put(RequestTranslator.requestField, RequestTranslator.getGUIDbyIPCommand);
			reqJSON.put(RequestTranslator.fieldIP, ip);
			
			
			JSONObject repJSON = getResponseFromEK(reqJSON);
			String repResult = repJSON.getString(RequestTranslator.resultField);
	
			if (repResult.equals(RequestTranslator.successMessage)) {
				//guid = repJSON.getString(RequestTranslator.fieldGUID);
				
				JSONArray jsonArray = repJSON.getJSONArray(RequestTranslator.fieldGUID); 

				for(int j = 0; j < jsonArray.length(); j++)
					guidList.add(jsonArray.getString(j));
				logger.debug("GUID :" +guidList);
			} else 
				logger.debug("GNS service returns error i.e. problem in executing the desired command");
		} catch (Exception e) {
			logger.error("Communication with GNS-service failed", e);		
		}
		return guidList;	
	}
	
	/**
	 * Obtain the local Zookeeper connection String. The Zookeeper server is running at the local 
	 * EK masters. 
	 * @return String representing the Zookeeper server addresses and ports.
	 */
	public static String getZooKeeperConnectionString() {
		String zks = null;
		try {
			JSONObject reqJSON = new JSONObject();
			reqJSON.put(RequestTranslator.requestField, RequestTranslator.getZookeeperCommand);
			
			JSONObject repJSON = getResponseFromEK(reqJSON);
			String repResult = repJSON.getString(RequestTranslator.resultField);
	
			if (repResult.equals(RequestTranslator.successMessage)) {
				zks = repJSON.getString(RequestTranslator.fieldZookeeperString);
				logger.debug("Zookeeper String :" +zks);
			} else 
				logger.debug("GNS service returns error i.e. problem in executing the desired command");
		} catch (Exception e) {
			logger.error("Communication with GNS-service failed", e);		
		}
		return zks;	
	}
	
	/**
	 * This command delets all the GUID records at the local cluster
	 * @return Whether the purge is successful or not
	 */
	public static boolean purgeNamingCluster() {
		String repResult;
		try {
			JSONObject reqJSON = new JSONObject();
			reqJSON.put(RequestTranslator.requestField, RequestTranslator.purgeClusterCommand);
	
			JSONObject repJSON = getResponseFromEK(reqJSON);
			repResult = repJSON.getString(RequestTranslator.resultField);
		} catch (Exception e) {
			logger.error("Communication with GNS-service failed", e);
			return false;		
		}
	
		if (repResult.equals(RequestTranslator.successMessage)) {
			logger.debug("Update Successful ");
			return true;
		}else {
			logger.debug("Update failed ");
			return false;
		}	
	}

	public static List<String> getAllLocalGUID () {
		List<String> guidList = new ArrayList<String>();
		try {
			JSONObject reqJSON = new JSONObject();
			reqJSON.put(RequestTranslator.requestField, RequestTranslator.getAllLocalGUIDCommand);
			JSONObject repJSON = getResponseFromEK(reqJSON);
			
			String repResult = repJSON.getString(RequestTranslator.resultField);
			if (repResult.equals(RequestTranslator.successMessage)) {
				JSONArray repJArray = repJSON.getJSONArray(RequestTranslator.fieldGUID);
				for(int j = 0; j < repJArray.length(); j++)
	                guidList.add(repJArray.getString(j));
				logger.debug("GUIDs :" +guidList);
			}
			else
				logger.log(Level.ALL, "Got error reply from EdgeKeeper");
		} catch (Exception e) {
			logger.error("Communication with GNS-service failed", e);
		}
		return guidList;
	}

	public static List<String> getMergedGUID () {
		List<String> guidList = new ArrayList<String>();
		try {
			JSONObject reqJSON = new JSONObject();
			reqJSON.put(RequestTranslator.requestField, RequestTranslator.getmergedGUIDCommand);
			JSONObject repJSON = getResponseFromEK(reqJSON);
			
			String repResult = repJSON.getString(RequestTranslator.resultField);
			if (repResult.equals(RequestTranslator.successMessage)) {
				JSONArray repJArray = repJSON.getJSONArray(RequestTranslator.fieldGUID);
				for(int j = 0; j < repJArray.length(); j++)
	                guidList.add(repJArray.getString(j));
				logger.debug("GUIDs :" +guidList);
			}
			else
				logger.log(Level.ALL, "Got error reply from EdgeKeeper");
		} catch (Exception e) {
			logger.error("Communication with GNS-service failed", e);
		}
		return guidList;
	}

	public static JSONObject readGUID (String guid) {
		JSONObject record =null;
		try {
			JSONObject reqJSON = new JSONObject();
			reqJSON.put(RequestTranslator.requestField, RequestTranslator.readGUIDCommand);
			reqJSON.put(RequestTranslator.fieldGUID, guid);
			JSONObject repJSON = getResponseFromEK(reqJSON);
			
			String repResult = repJSON.getString(RequestTranslator.resultField);
			if (repResult.equals(RequestTranslator.successMessage)) {
				record = repJSON.getJSONObject(RequestTranslator.fieldGUID);
				logger.debug("GUIDs :" +record);
			}
			else
				logger.log(Level.ALL, "Got error reply from EdgeKeeper");
		} catch (Exception e) {
			logger.error("Communication with GNS-service failed", e);
		}
		return record;
	}

	
	
//======================================================================MOHAMMAD======================================================================================
	/**
	 * 
	 * @param ownGUID
	 * @param AppName
	 * @param reqJSON
	 * @return JSONObject or null
	 */
    public static boolean putAppStatus(String AppName, JSONObject reqJSON){
    	try {
    		logger.log(Level.ALL, "Request to server putAppStatus: " + reqJSON.toString() );
    		//put request fields in the json object
    		reqJSON.put(RequestTranslator.requestField, RequestTranslator.putAppStatus);
    		reqJSON.put(RequestTranslator.fieldAppName, AppName);
    		
    		//send and receive reply
    		JSONObject repJSON = getResponseFromEK(reqJSON);
    	
    		//getEdgeStatus resultField as string from returning json object
    		String repResult = repJSON.getString(RequestTranslator.resultField);
    		
    		logger.log(Level.ALL, "Reply from server putAppStatus: " + repResult);
    		
    		//check result
    		if (repResult.equals(RequestTranslator.successMessage)) {
    			return true;
    		} else { 
    			return false;
    		}
		}catch(Exception e ) {
			logger.error("Communication with GNS-service failed", e);	
		}
    	
    	//dummy return
    	return false;
	
    }
    /**
     * getEdgeStatus application status of a specified appName in a specified GUID
     * @param targetGUID
     * @param appName
     * @return JSONObject or null
     */
    public static JSONObject getAppStatus(String targetGUID, String appName) {
    	try {
    		    		
    		//create a new json object for request
    		JSONObject reqJSON = new JSONObject(); 
        
			//put request fields in the json object
			reqJSON.put(RequestTranslator.requestField, RequestTranslator.getAppStatus);
			reqJSON.put(RequestTranslator.fieldGUID, targetGUID);
			reqJSON.put(RequestTranslator.fieldAppName, appName);
			
    		logger.log(Level.ALL, "Request to server getAppStatus: " + reqJSON.toString());
    		//send and receive reply
    		JSONObject repJSON = getResponseFromEK(reqJSON);
		
			if(repJSON!=null && repJSON.getString(RequestTranslator.resultField).equals(RequestTranslator.successMessage)) {
	    		logger.log(Level.ALL, "Reply from server getAppStatus: " + repJSON.toString());
	    		repJSON.remove(RequestTranslator.resultField);
	    		return repJSON;
			}
    	}catch(Exception e) {
    		logger.error("Communication with GNS-service failed", e);	
    	}
    	
    	//dummy return
		logger.log(Level.DEBUG, "Reply from server getAppStatus returned null.");
    	return null;
    	
    }
 
	/**
     * getEdgeStatus device status of a specified GUID
     * @param guid
     * @return JSONObject or null
     */
	public static JSONObject getDeviceStatus(String targetGUID){
		try {
			//create request json object
			JSONObject reqJSON = new JSONObject();
			
			//put request fields in the json object
			reqJSON.put(RequestTranslator.requestField, RequestTranslator.getDeviceStatus);
			reqJSON.put(RequestTranslator.fieldGUID, targetGUID);
			
			logger.log(Level.ALL, "Request to server getDeviceStatus: " + reqJSON.toString());
    		
			//send and receive reply
			JSONObject repJSON = getResponseFromEK(reqJSON);
	    		
			//check result
			if (repJSON!=null && repJSON.getString(RequestTranslator.resultField).equals(RequestTranslator.successMessage)) {
	    		logger.log(Level.ALL, "Reply from server getDeviceStatus: " + repJSON.toString());
	    		repJSON.remove(RequestTranslator.resultField);
	    		return repJSON;
			}
		}catch (Exception e) {
			logger.error("Communication with GNS-service failed", e);	
		}
		
		//dummy return 	
		logger.log(Level.DEBUG, "Reply from server getDeviceStatus returned null.");
		return null;
	}

	/**
	 * Get edge status containing all device and app status.
	 * @param
	 * @return
	 */
	public static JSONObject getEdgeStatus(){
		try{
			//make a request json object
			JSONObject reqJSON = new JSONObject();

			//put request fields in the json object
			reqJSON.put(RequestTranslator.requestField, RequestTranslator.getEdgeStatus);

			logger.log(Level.ALL, "Request to server getEdgeStatus: " + reqJSON.toString());

			//send and receive reply
			JSONObject repJSON = getResponseFromEK(reqJSON);

			//check result
			if(repJSON!=null && repJSON.getString(RequestTranslator.resultField).equals(RequestTranslator.successMessage)){
				logger.log(Level.ALL, "Reply from server getEdgeStatus: " + repJSON.toString());
				repJSON.remove(RequestTranslator.resultField);
				return repJSON;
			}
		}catch(Exception e){
			logger.error("Communication with GNS-service failed", e);
		}

		//dummy return
		logger.log(Level.DEBUG, "Reply from server getEdgeStatus returned null.");
		return null;
	}


	
//======================================================================MOHAMMAD======================================================================================    
    
	/**
	 * Put MDFS file metadata.
	 * @param metadata
	 * @return JSONObject
	 */
	public static JSONObject putMetadata(MDFSMetadata metadata){
		try {
			//make a request json
			JSONObject reqJSON = new JSONObject();
			//add requestField in reqJSON
			reqJSON.put(RequestTranslator.requestField, RequestTranslator.MDFSPutCommand);
			//add metadata in reqJSON
			reqJSON.put(RequestTranslator.MDFSmetadataField, metadata.fromClassObjecttoJSONString(metadata));
			//send and receive reply
			JSONObject repJSON = getResponseFromEK(reqJSON);
			//check reply
			if(repJSON!=null) {
				//return successJson or errorJson
				return repJSON;
			}else{
				//return errorJson as string
				return RequestTranslator.errorJSON("Could not getEdgeStatus response from local EdgeKeeper.");
			}
		} catch (Exception e) {
			logger.error("Communication with GNS-service failed", e);	
		}
		return null;
	}
	/**
	 * Get MDFS file metadata.
	 * @param filePathMDFS
	 * @return JSONObject
	 */
	public static JSONObject getMetadata(String filePathMDFS){
		try{
			//make a request json
			JSONObject reqJSON = new JSONObject();
			//add requestField in reqJSON
			reqJSON.put(RequestTranslator.requestField, RequestTranslator.MDFSGetCommand);
			//add filePathMDFS in reqJSON
			reqJSON.put(RequestTranslator.filePathMDFS, filePathMDFS);
			//send and receive reply
			JSONObject repJSON = getResponseFromEK(reqJSON);
			//check reply
			if(repJSON!=null){
				//return successJson+metadata or errorJson
				return repJSON;
			}else{
				//return errorJson as string
				return RequestTranslator.errorJSON("Could not getEdgeStatus response from local EdgeKeeper.");
			}
		}catch(Exception e){
			logger.error("Communication with GNS-service failed", e);	
		}
		return null;
	}
    /**
     *
     * @param folderPathMDFS
     * @param creatorGUID
     * @return JSONObject
     */
	public static JSONObject mkdir(String folderPathMDFS, String creatorGUID, boolean isGlobal){
		try{
			//make a request json
			JSONObject reqJSON = new JSONObject();
			//add requestField in reqJSON
			reqJSON.put(RequestTranslator.requestField, RequestTranslator.MDFSMkdirCommand);
			//add folderPathMDFS in reqJSON
			reqJSON.put(RequestTranslator.folderPathMDFS, folderPathMDFS);
			//add creatorGUID in reqJSON
			reqJSON.put(RequestTranslator.creatorGUID, creatorGUID);
			//add isGlobal as string
			if(isGlobal){
				reqJSON.put(RequestTranslator.isGlobal, RequestTranslator.TRUE);
			}else{
				reqJSON.put(RequestTranslator.isGlobal, RequestTranslator.FALSE);
			}
			///send and receive reply
			JSONObject repJSON = getResponseFromEK(reqJSON);
			//check reply
			if(repJSON!=null) {
				//return successJson or errorJson
				return repJSON;
			}else{
				//return errorJson as string
				return RequestTranslator.errorJSON("Could not getEdgeStatus response from local EdgeKeeper.");
			}
		}catch(Exception e){
			logger.error("Communication with GNS-service failed", e);	
		}
		return null;
	}
	/**
	 * Show files and folders in MDFS.
	 * @param folderPathMDFS
	 * @param lsRequestType
	 * @return JSONObject
	 */
	public static JSONObject ls(String folderPathMDFS, String lsRequestType){
		try{
			//make a request json
			JSONObject reqJSON = new JSONObject();
			//add requestField in reqJSON
			reqJSON.put(RequestTranslator.requestField, RequestTranslator.MDFSLsCommand);
			//add folderPathMDFS in reqJSON
			reqJSON.put(RequestTranslator.folderPathMDFS, folderPathMDFS);
			//add lsRequestType in reqJSON
			reqJSON.put(LScommand.lsRequestType, lsRequestType);
			///send and receive reply
			JSONObject repJSON = getResponseFromEK(reqJSON);
			//check reply
			if(repJSON!=null) {
				//return successJson+data or errorJson
				return repJSON;
			}else{
				//return errorJson as string
				return RequestTranslator.errorJSON("Could not getEdgeStatus response from local EdgeKeeper.");
			}
		}catch(Exception e){
			logger.error("Communication with GNS-service failed", e);	
		}
		return null;
	}
	/**
	 * Remove a file from MDFS.
	 * @param filePathMDFS
	 * @return JSONObject
	 */
	public static JSONObject rm_file(String filePathMDFS){
		try{
			//make a request json
			JSONObject reqJSON = new JSONObject();
			//add requestField in reqJSON
			reqJSON.put(RequestTranslator.requestField, RequestTranslator.MDFSRmCommand);
			//add filePathMDFS in reqJSON
			reqJSON.put(RequestTranslator.filePathMDFS, filePathMDFS);
			//add MDFSRmType as FILE
			reqJSON.put(RequestTranslator.MDFSRmType, RequestTranslator.FILE);
			//send and receive reply
			JSONObject repJSON = getResponseFromEK(reqJSON);
			//check reply
			if(repJSON!=null){
				//return successJson+metadata or errorJson
				return repJSON;
			}else{
				return RequestTranslator.errorJSON("Could not getEdgeStatus response from local EdgeKeeper.");
			}
		}catch(Exception e){
			logger.error("Communication with GNS-service failed", e);	
		}
		return null;
	}
	/**
	 * Remove a directory from MDFS.
	 * @param folderPathMDFS
	 * @return JSONObject
	 */
	public static JSONObject rm_directory(String folderPathMDFS){
		try{
			//make a request json
			JSONObject reqJSON = new JSONObject();
			//add requestField in reqJSON
			reqJSON.put(RequestTranslator.requestField, RequestTranslator.MDFSRmCommand);
			//add folderPathMDFS in reqJSON
			reqJSON.put(RequestTranslator.folderPathMDFS, folderPathMDFS);
			//add MDFSRmType as DIRECTORY
			reqJSON.put(RequestTranslator.MDFSRmType, RequestTranslator.DIRECTORY);
			//send and receive reply
			JSONObject repJSON = getResponseFromEK(reqJSON);
			//check reply
			if(repJSON!=null){
				//return successJson+metadata or errorJson
				return repJSON;
			}else{
				//return errorJson as string
				return RequestTranslator.errorJSON("Could not getEdgeStatus response from local EdgeKeeper.");
			}
		}catch(Exception e){
			logger.error("Communication with GNS-service failed", e);	
		}
		return null;
	}
//======================================================================MOHAMMAD======================================================================================    


    
}