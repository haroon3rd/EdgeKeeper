package edu.tamu.cse.lenss.edgeKeeper.client;

import java.util.List;

import org.json.JSONObject;

import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MDFSMetadata;
import edu.tamu.cse.lenss.edgeKeeper.topology.TopoGraph;

public interface EdgeKeeperAPI {
	
	public  String getOwnGuid();
	/**
	 * Retrieve the GUID used by the host machine. This GUID is derived from 
	 * the public key of the certificate used for registration.
	 * @return The GUID string 
	 */
	
	public  String getOwnAccountName();
	/**
	 * Retrieve the account name of the host machine which is used by GNS. 
	 * This name is derived from the alias of the p12 certificate used in the registration.   
	 * @return Account Name string
	 */	
	
	public  TopoGraph getNetworkInfo();
	/**
	 * Fetches the OLSR JSON Info from EdgeKeeper. EdgeKeeper obtains the JSONInfo from OLSR and 
	 * converts the IP addresses with the corresponding GUID. 
	 * @return TopoGraph
	 */
	
	public  boolean addService(String ownService, String ownDuty);
	/**
	 * This function registers a service and the duty at the GNS server for service discovery.
	 * 
	 * @param ownService What is the name of the service, usually the application name
	 * @param ownDuty What duty it is playing
	 * @return true if the update is successful at the GNS server
	 */
	
	public  String addService(String ownService, String ownDuty, String ip, int port);
	/**
	 * This function registers a service and the duty at the GNS server for service discovery.
	 * 
	 * @param ownService What is the name of the service, usually the application name
	 * @param ownDuty What duty it is playing
	 * @param ip is ip of Service node/container
	 * @param port is the port it uses for the service
	 * @return true if the update is successful at the GNS server
	 * @Author Amran
	 */
	
	public  String addService(String ownService, String ownDuty, String ip);
	
	
	public  boolean removeService(String targetService);
	
	
	public  List<String> getPeerGUIDs(String targetService, String targetDuty);
	
	
	public  List<String>  getPeerInfo(String targetService, String targetDuty);
	
	
	public  List<String>  getPeerList(String targetService, String targetDuty);
	
	
	public  List<String> getPeerIPs(String targetService, String targetDuty);
	
	
	public  List<String> getPeerNames(String targetService, String targetDuty);
	
	
	public  List<String> getIPbyGUID(String targetGUID);
	
	
	public  List<String> getIPbyName(String targetName);
	
	
	public  String getGUIDbyAccountName(String accountName);
	
	
	public  String getAccountNamebyGUID(String guid);
	
	
	public  List<String> getAccountNamebyIP(String targetIp);
	
	
	public  List<String> getGUIDbyIP(String ip);
	
	
	public  String getZooKeeperConnectionString();
	
	
	public  boolean purgeNamingCluster();
	
	
	public  List<String> getAllLocalGUID ();
	
	
	public  boolean putAppStatus(String AppName, JSONObject reqJSON);
	
	
	public  JSONObject getAppStatus(String targetGUID, String appName);
	
	
	public  JSONObject getAppStatus(String targetGUID, String targetServiceName, String targetServiceID);
	
	
	public  JSONObject getDeviceStatus(String targetGUID);
	
	
	public  JSONObject getEdgeStatus();
	
	
	public  JSONObject putMetadata(MDFSMetadata metadata);
	
	
	public  JSONObject getMetadata(String filePathMDFS);
	
	
	public  JSONObject mkdir(String folderPathMDFS, String creatorGUID, boolean isGlobal);
	
	
	public  JSONObject readGUID(String guid);
	
	public  String getSERVER_IP();


	public void setSERVER_IP(String serverIP);
	
	
	
	 
	
	
	
	
	
	
	
	

}