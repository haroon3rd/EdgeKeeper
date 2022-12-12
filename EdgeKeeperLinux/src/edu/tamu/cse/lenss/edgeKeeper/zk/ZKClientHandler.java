package edu.tamu.cse.lenss.edgeKeeper.zk;

import java.io.*;
import java.util.*;
  
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.json.*;
import org.json.simple.parser.JSONParser;

import edu.tamu.cse.lenss.edgeKeeper.dns.DNSServer;
import edu.tamu.cse.lenss.edgeKeeper.server.EKHandler;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKProperties;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKRecord;
import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;

import edu.tamu.cse.lenss.edgeKeeper.zk.ChildData;

public class ZKClientHandler implements Terminable{

	
	public static final String ekClusterPath = "/ek_cluster";
    public static final String fileMetaDataPath = ekClusterPath+"/mdfs";
    public static final String nameRecordPath = ekClusterPath+"/guids";

	public static final Logger logger = Logger.getLogger(ZKClientHandler.class);

	String ownGUID;
	String ownZPath;

	EKHandler ekHandler;

	Map<String,String[]> map;
	File fileObj;
	String filename;

	public ZKClientHandler( EKHandler eventHandler) {
    	this.ownGUID = EKHandler.getGNSClientHandler().getOwnGUID();
    	this.ekHandler = eventHandler;
		this.ownZPath = nameRecordPath + ownGUID;

		this.map = new HashMap<String, String[]>();
		this.filename = ownGUID + ".log";

		this.fileObj = new File(this.filename);

		// populate map using the file
		try{
			boolean ret = fileObj.createNewFile();
			if(ret)
				logger.debug("Log file created: " + fileObj.getCanonicalPath());
			else{
				BufferedReader br = new BufferedReader(new FileReader(this.fileObj));
            	String line = null;
				while ((line = br.readLine()) != null) {
					String[] parts = line.split(":",2);
					String key = parts[0].trim();
					String array = parts[1].trim();										
					array = array.substring(1, array.length() - 1);
					parts = array.split(", ",2);
					String path = parts[0].trim();
					String record = parts[1].trim();
					String[] strs = new String[]{path, record};
					logger.debug("putting in map: key>" + key + "< , path >" + strs[0] + "< , arec>" + strs[1] + "<\n");
					this.map.put(key, strs);
				}
				logger.debug("Log file exists and read from: " + fileObj.getCanonicalPath());
				for(Map.Entry<String, String[]> entry: this.map.entrySet()){
					logger.debug(entry.getKey() + ": [" + entry.getValue()[0] + ", " + entry.getValue()[1] + "]\n");
				}				
				br.close();
			}
		}
		catch (IOException e){  
			logger.fatal("Problem creating log file", e);
		}   		

		logger.debug("ZKClient Handler declared. Own GUID="+ownGUID);
	}
	
	@Override
	public void run(){
		;
	}

    public void restartCurator(String zkServerString){
		;
    }

    @Override
	public void terminate() {
		;
	}

    
    
    void createOwnZnode() throws Exception {
		;
    }

    
    void startTreeCaching(){
		;
	}

	public boolean isConnected() {
			return true;
	}

	
/***********DNS**********************************************************************************/
/**
* This function is used by DNS name resolving service. It returns the entire name record associated with the 
* accountName.
* @param accountName
* @return
*/
	public JSONObject getRecordbyAccountName(String accountName) {
		if(this.map == null) {
			logger.log(Level.ALL,"Cache is not initialized");
			return null;
		}
		for (String guid : this.map.keySet()) {
			try {
				JSONObject guidData = new JSONObject (this.map.get(guid)[1]);
				if (guidData.getString(EKRecord.ACCOUNTNAME_FIELD).equals(accountName)) {
					logger.log(Level.ALL, "Found AccountName: "+accountName+" GUID record: "+guidData.toString());
					return guidData;
				}
			} catch (JSONException | NullPointerException e) {
				//logger.log(Level.ALL, "");
			}
		}
		logger.log(Level.ALL, "Could not find accountName: "+accountName);
		return null;
	}
	/************Naming*******************************************************************************/

	public JSONArray getAllLocalGUIDs() {
		JSONArray guidJArray = new JSONArray();
		for (String guid : this.map.keySet()) {
			try{
				JSONObject record = new JSONObject (this.map.get(guid)[1]);
				if(EKHandler.edgeStatus.masterGUID.equals(record.getString(EKRecord.MASTER_GUID)))
					guidJArray.put(guid);
			}catch (JSONException e) {
			}
		}
		
		logger.log(Level.ALL, "All GUIDS: " + guidJArray.toString());
		return guidJArray;
	}
	
	public JSONArray getMergedGUIDs() {
		JSONArray guidJArray = new JSONArray();
		try {
			for (String guid : this.map.keySet()) {
				guidJArray.put(guid);
			}
		}catch(NullPointerException e) {
			logger.log(Level.ALL, "Problem in accessing the local records", e);
		}
		logger.log(Level.ALL, "All GUIDS: " +guidJArray.toString());
		return guidJArray;
	}

	
	public boolean purgeNamingCluster() {
		return true;
	}

	public JSONObject readGUID(String guid) {
		if(this.map == null) {
			logger.log(Level.ALL,"Cache is not initialized");
			return null;
		}
		JSONObject record = null;
		try {
			record = new JSONObject (this.map.get(guid)[1]);
		} catch (JSONException e) {
			logger.log(Level.ALL,"GUID "+ guid+ " could not be found or read");
		}
		logger.log(Level.ALL, "Record for guid "+guid+" is "+record.toString());
		return record;
	}
	
	public List<String> getPeerGUIDs(String service, String duty) {
		if(this.map == null) {
			logger.log(Level.ALL,"Map is not initialized");
			return null;
		}
		List<String> peerGUIDs = new ArrayList<String>();
		for (String guid : this.map.keySet()) {
			try {
				JSONObject guidData = new JSONObject (this.map.get(guid)[1]);
				if (guidData.getString(service).equals(duty)) {
					peerGUIDs.add(guid);
					//logger.log(Level.ALL, "GUID "+guid+" contains "+service);
				}
			} catch (JSONException e) {
				//logger.log(Level.ALL, "The "+service+" fild does not exist in "+guid, e);
			}
		}
		logger.log(Level.ALL, "Service: "+service+", duty: "+duty+" matches for the GUIDS" +peerGUIDs);
		return peerGUIDs;
	}


	public String getGUIDbyAccountName(String accountName) {
		if(this.map == null) {
			logger.log(Level.ALL,"Cache is not initialized");
			return null;
		}
		for (String guid : map.keySet()) {
			try {
				JSONObject guidData = new JSONObject (this.map.get(guid)[1]);
				if (guidData.getString(EKRecord.ACCOUNTNAME_FIELD).equals(accountName)) {
					logger.log(Level.ALL, "GUID "+guid+" matches the account name "+accountName);
					return guid;
				}
			} catch (JSONException | NullPointerException e) {
				//logger.log(Level.ALL, "The accountname "+accountName+" does not match for GUID "+guid, e);
			}
		}
		logger.log(Level.ALL, "The accountname not found. "+accountName);
		return null;
	}
		
	public List<String> getIPsFromGuid(String guid) {
		List<String> ipList=new ArrayList<String>();
		try {
			if(this.map == null) {
				logger.log(Level.ALL,"Cache is not initialized");
				return null;
			}
			JSONObject jobj = new JSONObject (this.map.get(guid)[1]);
			JSONArray jarr = jobj.getJSONObject(EKRecord.A_RECORD_FIELD).getJSONArray(EKRecord.RECORD_FIELD);
			for (int i =0; i< jarr.length(); i++)
				ipList.add(jarr.getString(i));
		} catch (JSONException | NullPointerException e) {
			logger.log(Level.ALL,"Can not find IP information for GUID: "+guid,e);
		}
		logger.log(Level.ALL, "The IPs for guid: "+guid+" are: "+ipList);
		return ipList;
	}


	public String getAccountNamebyGUID(String guid) {
		if(this.map == null) {
			logger.log(Level.ALL,"Map is not initialized");
			return null;
		}
		String accountName = null;
		try {
			JSONObject jobj = new JSONObject (this.map.get(guid)[1]);
			accountName = jobj.getString(EKRecord.ACCOUNTNAME_FIELD);
		} catch (JSONException | NullPointerException e) {
			logger.warn("GUID "+ guid+ " does not contain account name field");
		}
		logger.log(Level.ALL, "Account name for guid "+guid+" is "+accountName);
		return accountName;
	}


	public List<String> getGUIDbyIP(String ip) {
		logger.log(Level.ALL, "Trying to fetch GUID for IP:"+ip);
		if(this.map == null) {
			logger.log(Level.ALL,"Cache is not initialized");
			return null;
		}
		List<String> peerGUIDs = new ArrayList<String>();
		for (String guid : this.map.keySet()) {
			try {
				JSONObject guidData = new JSONObject (this.map.get(guid)[1]);
				JSONArray jarr = guidData.getJSONObject(EKRecord.A_RECORD_FIELD).getJSONArray(EKRecord.RECORD_FIELD);
				for (int i =0; i< jarr.length(); i++)
					if(ip.equals(jarr.getString(i))) {
						peerGUIDs.add(guid);
				}		
			} catch (JSONException | NullPointerException e) {
				logger.log(Level.ALL, "Problem in parsing the GUID data for GUID "+guid, e);
			}
		}
		logger.log(Level.ALL, "IP: "+ip+" matches GUID: "+peerGUIDs);
		return peerGUIDs;
	}

	public boolean update(JSONObject record) {
		try {
			// Updating a record in file todo 
			String[] arr = new String[] {ownZPath, record.toString()};
			map.put(ownGUID, arr);

            BufferedWriter bf = new BufferedWriter(new FileWriter(this.fileObj));

			for(Map.Entry<String, String[]> entry: this.map.entrySet()){
				bf.write(entry.getKey() + ": [" + entry.getValue()[0] + ", " + entry.getValue()[1] + "]");
				bf.newLine();
			}
			bf.flush();
			bf.close();
		}
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean updateCurrentRecord() {
		logger.log(Level.ALL, "Trying to update current name record");
		return update(EKHandler.ekRecord.fetchRecord());
	}
	
	public String getZKConnString() {
		return null;
	}
	

	/***************Naming*****************************************************************************/
	
	/***************MDFS*******************************************************************************/	
    /**
     * This function checks whether the MDFS root exists in the local Zookeeper cluster.
     * In the current design, all the file Metadata are stored as a children of a single master Znode in the local
     * Zookeeper cluster. This master Znode name is declared a a constant. 
     * If a cluster is initialized recently, the cluster might not contain the root MDFS Znode. 
     * In that case, this creates the root Znode. 
     * @throws Exception
     */
    void checkandCreateMDFSRoot() throws Exception {
		// Don't this there is need for this function
		// String path = "/ek_cluster/mdfs/";
		// Mehul Questons! 
    	// if (client.checkExists().forPath(fileMetaDataPath) == null) {
		// 	logger.info("MDFS znode doesn't exist. creating the Znode");
		// 	client.create().forPath(fileMetaDataPath, "File metadata path".getBytes());
		// 	logger.debug("Created new MDFS Znode");
		// }
    }

//  /**
//  * This function creates a new File MetaData (similar to inode) with the given ID
//  * @param id
//  * @param metaData
//  * @throws Exception
//  */
// public void createFileMetaData(String id, byte[] metaData) throws Exception{
//		createMDFSRoot();
// 	client.create().forPath(ZKPaths.makePath(fileMetaDataPath, id), metaData);
//		logger.debug("Created a new fileMetadata for id: "+id+", metadata:"+new String(metaData));
// }
	/**
	 * This function updates an already existing File MetaData (similar to inode)
	 * with the given ID
	 * 
	 * @param id
	 * @param metaData
	 * @throws Exception
	 */
	public void storeMDFSMetadata(String id, byte[] metaData) throws Exception {
		// put byte array as string in data for key==id
		String path = fileMetaDataPath + "/" + id;
		String[] arr = new String[] {path, metaData.toString()};
		this.map.put(id,arr);
		logger.log(Level.ALL, "MDFS data for path: " + path);
	}

	/**
	 * Retrieves the content of a file metadata
	 * 
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public byte[] retrieveMDFSMetadata(String id) throws Exception {
		// get byte array for key==id
		return this.map.get(id)[0].getBytes();
	}

	/**
	 * Deletes an already existing file Metadata
	 * 
	 * @param id
	 * @throws Exception
	 */
	public void deleteMDFSMetaData(String id) throws Exception {
		// remove entry in map
		this.map.remove(id);
	}

	public boolean checkMetadataExists(String id) throws Exception {
		// check if id in map
		if(this.map.containsKey(id))
			return true;
		return false;	
	}

	/***************
	 * MDFS
	 *******************************************************************************/

//	public void setHealthLeader() {
//		try {
//			if(client==null)
//				Thread.sleep(500);
//			
//    		String ownServerIP = EKHandler.getZKServerHandler().getOwnServerIP();
//    		client.blockUntilConnected(500, TimeUnit.MILLISECONDS);
//			if (client.checkExists().forPath(healthLeaderPath) == null) {
//				client.create().forPath(healthLeaderPath, ownServerIP.getBytes());
//			}
//			else
//				client.setData().forPath(healthLeaderPath, ownServerIP.getBytes());
//			logger.debug("Committed Health leader as "+ownServerIP);
//		} catch (Exception e) {
//			logger.log(Level.ERROR, "Problem in commiting the health-leader IP at the ZK",e);
//		}
//	}
//	
//	public String getHealthLeader() throws Exception {
//		checkIfConnected();
//		return new String (client.getData().forPath(healthLeaderPath));
//	}

	public void mergeNeighborData(JSONObject guidMergeData) {
		if(guidMergeData==null || guidMergeData.length()==0) {
			logger.debug("The neighbor merge data is empty. Not updating.");
			return;
		}
		
		logger.log(Level.ALL, "Merging GUID data from another edge"+guidMergeData.toString());
		
//		/*
//		 * Create a separate client for updating neighbor edge GUID. This way, these GUIDs will have a time limit 
//		 */
//		CuratorFramework mergerClient = CuratorFrameworkFactory.newClient(this.getZKConnString(), new RetryUntilElapsed(10000, 5000));
//		mergerClient.start();
		
		Iterator<String> iterator = guidMergeData.keys();
		while(iterator.hasNext()) {
			String nGuid = iterator.next();
			
			byte[] nRec= null;
			try {
				JSONObject nGuidRec = guidMergeData.getJSONObject(nGuid);
				nGuidRec.put(EKRecord.updateTime, System.currentTimeMillis());
				nRec = nGuidRec.toString().getBytes();
			} catch (JSONException e) {
				logger.log(Level.DEBUG, "Error in fetching the GUID data from neighbor data info for GUID "+nGuid);
				continue;
			}
			
			/*
			 * Fetch the GUID record corrsponding to the neighbor GUID. Check the master GUID for this node
			 * If the nodes does not belong to the local edge, update the local record. If the node belongs to the 
			 * local edge, ignore the neighbor GUID info. 
			*/
			String lGuidMaster = null;
			try {
				JSONObject lGuidData = new JSONObject (this.map.get(nGuid)[1]);
				lGuidMaster = lGuidData.getString(EKRecord.MASTER_GUID);
			}catch(Exception e ) {
			}
			if(lGuidMaster == null || !lGuidMaster.equals(ownGUID)) {
				try {
					// # Mehul Questions!
					// String nZPath = ZKPaths.makePath(nameRecordPath, nGuid);
					// # Mehul CHANGES
					// client.create().orSetData().withMode(CreateMode.EPHEMERAL).forPath(nZPath, nRec);
					logger.log(Level.ALL, "Updated neighbor to the local edge, GUID: "+nGuid);
				} catch (Exception e) {
					logger.warn("Could not update the data");
				}
			}
			else {
				logger.log(Level.ALL, "The GUID "+nGuid+" belongs to local edge. Not updating the data.");
			}
		}
	}
	
	public JSONObject prepareMergeData() {
		logger.log(Level.ALL, "Preparing GUID data record for edge merger");
		if(this.map == null || this.map.isEmpty()) {
			logger.log(Level.ALL,"Cache is not initialized returning null");
			return null;
		}
		JSONObject mergeJObj = new JSONObject();
		for (String guid : map.keySet()) {
			try {
				JSONObject guidData = new JSONObject (this.map.get(guid)[1]);
				/*
				 * Check if the node belongs to local edge by comparing its master GUID. Add this GUID record to the 
				 * merger data only if the node belongs to the local edge. Alos, for node belonging to 
				*/
				if (guidData.getString(EKRecord.MASTER_GUID).equals(ownGUID)) {
					mergeJObj.put(guid, guidData);
				}
				else { // clean the stale GUID records of neighbors
					if( System.currentTimeMillis() > guidData.getLong(EKRecord.updateTime) + 2*EKConstants.GUID_MERGE_INTERVAL) {
						// logger.debug("GUID record for "+guid+" is stale. Deleting it from current edge");
						// Mehul Questions!
						// client.delete().inBackground().forPath(ZKPaths.makePath(nameRecordPath, guid));
					}
				}
			} catch (Exception e) {
				logger.log(Level.ALL, "Problem in creating merge information for "+guid, e);
			}
		}
		return mergeJObj;
	}
	
}
