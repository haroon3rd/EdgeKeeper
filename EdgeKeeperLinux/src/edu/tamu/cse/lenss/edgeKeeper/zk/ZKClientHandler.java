package edu.tamu.cse.lenss.edgeKeeper.zk;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.curator.CuratorConnectionLossException;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.RetryUntilElapsed;
import org.apache.curator.utils.ZKPaths;
//import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.apache.zookeeper.CreateMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.tamu.cse.lenss.edgeKeeper.server.EKHandler;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKRecord;
import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;


public class ZKClientHandler implements Terminable {
	
	public static final String ekClusterPath = "/ek_cluster";
    public static final String fileMetaDataPath = ekClusterPath+"/mdfs";
    public static final String nameRecordPath = ekClusterPath+"/guids";
	//private static final String healthLeaderPath = ekClusterPath+"/healthLeader";

	public static final Logger logger = LoggerFactory.getLogger(ZKClientHandler.class.getName());
	CuratorFramework client;
	TreeCache cache;

	String ownGUID;
	//String ownZPath;
	//PersistentNode ownZNode;
	EKHandler ekHandler;
	private String ownZPath;
	
	public ZKClientHandler( EKHandler eventHandler) {
    	this.ownGUID = EKHandler.getGNSClientHandler().getOwnGUID();
    	this.ekHandler = eventHandler;
		this.ownZPath = ZKPaths.makePath(nameRecordPath, ownGUID);
		logger.debug("ZKClient Handler declared. Own GUID="+ownGUID);
	}
	
	@Override
	public void run() {
		//restartCurator();
	}

    public void restartCurator(String zkServerString){
    	
        // Close the client
        this.terminate();

        //Now establish new client
        //RetryNTimes retryNTimes = new RetryNTimes(2, 100);
        
        //ExponentialBackoffRetry retry = new ExponentialBackoffRetry(500, 4);
        RetryUntilElapsed retry = new RetryUntilElapsed(10000, 5000);

        // The simplest way to getEdgeStatus a CuratorFramework instance. This will use default values.
        // The only required arguments are the connection string and the retry policy
        //return CuratorFrameworkFactory.newClient(connectionString, retryPolicy);
        //client = CuratorFrameworkFactory.newClient(zkServerString, retry);
        client = CuratorFrameworkFactory.newClient(zkServerString, retry);
        
        
        client.getUnhandledErrorListenable().addListener((message, e) -> {
            //ekHandler.curatorError(message,e);
        	logger.info("curator error=" + message, e);
        	
        });
        client.getConnectionStateListenable().addListener((c, newState) -> {
            logger.info("curator event state=" + newState);
            ekHandler.curatorStateChange(c, newState);
            
            switch(newState) {
            case CONNECTED:
            case RECONNECTED:
            	logger.info("Zookeeper connection established. Starting to cache.");
            	
            	try {
            		this.createOwnZnode();
					this.updateCurrentRecord();
	            	this.startTreeCaching();
	            	
	            	//EKHandler.getClusterLeaderHandler().initialize(this.client);
				} catch (Exception e1) {
					logger.error("Problem in creating own name record.", e1);
				}
            	break;
            case LOST:
				/*
				 * Todeal with disconnection issues, when a connection is lost, it is better to
				 * restart the client.
				 */
            	logger.info("Lost Zookeeper session. Terminating");
            	this.terminate();
            	//restartCurator();
            	//EKHandler.coordinatorClient.reconfigure();
            	
            	
            	break;
            case READ_ONLY:
            case SUSPENDED:
            	logger.error("Zookeeper client connection lost. stopping the cache");
            	this.cache.close();
            	//EKHandler.getClusterLeaderHandler().terminate();
            	break;
            default:
            	logger.error("Unrecognized client State"+ newState);
            }
        });
        
        logger.info("Starting ZKClient for server="+zkServerString);
        client.start();
        ekHandler.curatorStateChange(client, ConnectionState.LOST);
    }

    @Override
	public void terminate() {
		if(cache!= null) {
			logger.info("closing cache");
			this.cache.close();
		}
		if (client!=null) {
			logger.info("closing Zookeeper client");
			this.client.close();
		}
		ekHandler.curatorStateChange(client, ConnectionState.LOST);
		logger.info("Terminated "+this.getClass().getSimpleName());
	}

    
    
    void createOwnZnode() throws Exception {
//    	if (client.checkExists().forPath(ekClusterPath) == null) {
//			logger.info("EK cluster doen's exist. creating one");
//			
//		}
    	
    	client.create().orSetData().forPath(ekClusterPath, "new EK cluster".getBytes());
		logger.debug("Created new EK cluster");
    	
		// Now create the root cluster for EK if it doesn't exist already
//		if (client.checkExists().forPath(nameRecordPath) == null) {
//			logger.info("EK cluster name record path doen's exist. creating one");
//			client.create().forPath(nameRecordPath, "new cluster".getBytes());
//			logger.debug("Created new EK name record path");
//		}
		
		client.create().orSetData().forPath(nameRecordPath, "new cluster".getBytes());
		logger.debug("Created new EK name record path");

		
		if (client.checkExists().forPath(ownZPath) != null) {
			logger.info("Znode for this GUID already exist. Deliting the record");
			client.delete().forPath(ownZPath);
		}
		client.create().withMode(CreateMode.EPHEMERAL).forPath(ownZPath, "new cluster".getBytes());
		logger.debug("Created the Znode for own GUID");
	
			
		
    }
    
    
    void startTreeCaching(){
    	if(this.cache!=null){
        	logger.debug("closing existing GUID cache");
        	this.cache.close();
        }
    	
    	this.cache = TreeCache.newBuilder(client, nameRecordPath).setCacheData(true).build();
        this.cache.getListenable().addListener((c, event) -> {
        	//ekHandler.treeCacheEvent(event.getType());
        	logger.info( "Cache client state change. New stat="+event.getType());
        });
        
        try {
			this.cache.start();
			logger.info("Started caching the EK cluster updates");
		} catch (Exception e) {
			logger.error("Could not start the EK cashing through Treecaching ", e);
		}
    }

    
    
    /**
	 * This function throws an error if the Zookeeper client is not connected. 
	 * @throws Exception
	 */
	public void checkIfConnected() throws Exception {
		 if( (client == null) || client.getState()!=CuratorFrameworkState.STARTED 
				 || !client.getZookeeperClient().isConnected()) {
			 logger.info( "Zookeeper Client is not connected");
			 throw new CuratorConnectionLossException();
		 }
	}

	public boolean isConnected() {
		try {
			this.checkIfConnected();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

//	/**
//	 * This function throws an error if the Zookeeper client is not connected. 
//	 * @throws Exception
//	 */
//	public void checkIfConnected() throws Exception {
//		if( client.getState()==CuratorFrameworkState.STARTED 
//				&& client.getZookeeperClient().isConnected()) {
//			 logger.log(Level.ALL, "Zookeeper Client is not connected.");
//			 throw new CuratorConnectionLossException();
//		 }
//	}
//
//	public boolean isConnected() {
//		if(this.client == null)
//			return false;
//		else
//			;
////		try {
////			this.checkIfConnected();
////			return true;
////		} catch (Exception e) {
////			return false;
////		}
//	}
    

	
	/***********DNS**********************************************************************************/
	/**
	 * This function is used by DNS name resolving service. It returns the entire name record associated with the 
	 * accountName.
	 * @param accountName
	 * @return
	 */
	public JSONObject getRecordbyAccountName(String accountName) {
		Map<String,ChildData> map =  cache.getCurrentChildren(nameRecordPath);
		if(map == null) {
			logger.info("Cache is not initialized");
			return null;
		}
		for (String guid : map.keySet()) {
			try {
				JSONObject guidData = parseBytetoJSON( map.get(guid).getData());
				if (guidData.getString(EKRecord.ACCOUNTNAME_FIELD).equals(accountName)) {
					logger.info("Found AccountName: "+accountName+" GUID record: "+guidData.toString());
					return guidData;
				}
			} catch (JSONException | NullPointerException e) {
				//logger.log(Level.ALL, "");
			}
		}
		logger.info("Could not find accountName: "+accountName);
		return null;
	}
	/************Naming*******************************************************************************/

	private JSONObject parseBytetoJSON(byte[] data) {
		String dataStr = new String(data);
		JSONObject jobj = null;
		try {
			jobj=new JSONObject(dataStr);
		} catch (JSONException e) {
			logger.info("Problem in parsing GUID data from Zookeepr. Data:"+dataStr);
		}
		return jobj;
	}

	public JSONArray getAllLocalGUIDs() {
		JSONArray guidJArray = new JSONArray();
		try {
			Map<String,ChildData> map =  cache.getCurrentChildren(nameRecordPath);
			for (String guid : map.keySet()) {
				try {
					JSONObject record = parseBytetoJSON(map.get(guid).getData());
					if(EKHandler.edgeStatus.masterGUID.equals(record.getString(EKRecord.MASTER_GUID)))
						guidJArray.put(guid);
				} catch (JSONException e) {
				}
			}
		}catch(NullPointerException e) {
			logger.info("Problem in accessing the local records", e);
		}
		logger.info("All GUIDS: " +guidJArray.toString());
		return guidJArray;
	}
	
	public JSONArray getMergedGUIDs() {
		JSONArray guidJArray = new JSONArray();
		try {
			Map<String,ChildData> map =  cache.getCurrentChildren(nameRecordPath);
			for (String guid : map.keySet()) {
				guidJArray.put(guid);
			}
		}catch(NullPointerException e) {
			logger.info("Problem in accessing the local records", e);
		}
		logger.info("All GUIDS: " +guidJArray.toString());
		return guidJArray;
	}

	
	public boolean purgeNamingCluster() {
		logger.info("Purging the Naming Cluster");
		try {
			Map<String,ChildData> map =  cache.getCurrentChildren(nameRecordPath);
			for (String guid : map.keySet()) {
				logger.info("Deleting :"+guid);
				String target = ZKPaths.makePath(nameRecordPath, guid);
				this.client.delete().forPath(target);
			}
			return true;
		}catch(Exception e) {
			logger.error("Failed to purge local edgeKeeper cluster",e);
			return false;
		}
	}

	public JSONObject readGUID(String guid) {
		Map<String,ChildData> map =  cache.getCurrentChildren(nameRecordPath);
		if(map == null) {
			logger.info("Cache is not initialized");
			return null;
		}
		JSONObject record = null;;
		try {
			record = parseBytetoJSON(map.get(guid).getData());			
		} catch (NullPointerException e) {
			logger.info("GUID "+ guid+ " could not be found");
		}
		logger.info( "Record for guid "+guid+" is "+record.toString());
		return record;
	}
	
	public List<String> getPeerGUIDs(String service, String duty) {
		Map<String,ChildData> map =  cache.getCurrentChildren(nameRecordPath);
		if(map == null) {
			logger.info("Cache is not initialized");
			return null;
		}
		List<String> peerGUIDs = new ArrayList<String>();
		for (String guid : map.keySet()) {
			try {
				JSONObject guidData = parseBytetoJSON( map.get(guid).getData());
				if (guidData.getString(service).equals(duty)) {
					peerGUIDs.add(guid);
					//logger.log(Level.ALL, "GUID "+guid+" contains "+service);
				}
			} catch (JSONException | NullPointerException e) {
				//logger.log(Level.ALL, "The "+service+" fild does not exist in "+guid, e);
			}
		}
		logger.info( "Service: "+service+", duty: "+duty+" matches for the GUIDS" +peerGUIDs);
		return peerGUIDs;
	}


	public String getGUIDbyAccountName(String accountName) {
		Map<String,ChildData> map =  cache.getCurrentChildren(nameRecordPath);
		if(map == null) {
			logger.info("Cache is not initialized");
			return null;
		}
		for (String guid : map.keySet()) {
			try {
				JSONObject guidData = parseBytetoJSON( map.get(guid).getData());
				if (guidData.getString(EKRecord.ACCOUNTNAME_FIELD).equals(accountName)) {
					logger.info("GUID "+guid+" matches the account name "+accountName);
					return guid;
				}
			} catch (JSONException | NullPointerException e) {
				//logger.log(Level.ALL, "The accountname "+accountName+" does not match for GUID "+guid, e);
			}
		}
		logger.info("The accountname not found. "+accountName);
		return null;
	}
		
	public List<String> getIPsFromGuid(String guid) {
		List<String> ipList=new ArrayList<String>();
		try {
			Map<String,ChildData> map =  cache.getCurrentChildren(nameRecordPath);
			if(map == null) {
				logger.info("Cache is not initialized");
				return null;
			}
			JSONObject jobj = parseBytetoJSON(map.get(guid).getData());
			JSONArray jarr = jobj.getJSONObject(EKRecord.A_RECORD_FIELD).getJSONArray(EKRecord.RECORD_FIELD);
			for (int i =0; i< jarr.length(); i++)
				ipList.add(jarr.getString(i));
		} catch (JSONException | NullPointerException e) {
			logger.info("Can not find IP information for GUID: "+guid,e);
		}
		logger.trace("The IPs for guid: "+guid+" are: "+ipList);
		return ipList;
	}


	public String getAccountNamebyGUID(String guid) {
		Map<String,ChildData> map =  cache.getCurrentChildren(nameRecordPath);
		if(map == null) {
			logger.trace("Cache is not initialized");
			return null;
		}
		String accountName = null;
		try {
			JSONObject jobj = parseBytetoJSON(map.get(guid).getData());
			accountName = jobj.getString(EKRecord.ACCOUNTNAME_FIELD);
		} catch (JSONException | NullPointerException e) {
			logger.warn("GUID "+ guid+ " does not contain account name field");
		}
		logger.trace("Account name for guid "+guid+" is "+accountName);
		return accountName;
	}


	public List<String> getGUIDbyIP(String ip) {
		logger.trace( "Trying to fetch GUID for IP:"+ip);
		Map<String,ChildData> map =  cache.getCurrentChildren(nameRecordPath);
		if(map == null) {
			logger.trace("Cache is not initialized");
			return null;
		}
		List<String> peerGUIDs = new ArrayList<String>();
		for (String guid : map.keySet()) {
			try {
				JSONObject guidData = parseBytetoJSON( map.get(guid).getData());
				JSONArray jarr = guidData.getJSONObject(EKRecord.A_RECORD_FIELD).getJSONArray(EKRecord.RECORD_FIELD);
				for (int i =0; i< jarr.length(); i++)
					if(ip.equals(jarr.getString(i))) {
						peerGUIDs.add(guid);
				}		
			} catch (JSONException | NullPointerException e) {
				logger.trace( "Problem in parsing the GUID data for GUID "+guid, e);
			}
		}
		logger.trace("IP: "+ip+" matches GUID: "+peerGUIDs);
		return peerGUIDs;
	}

	public boolean update(JSONObject record) {
		try {
			checkIfConnected();
			byte[] rec = record.toString().getBytes();
			//ownZNode.setData(rec);
			client.setData().forPath(ownZPath, rec);
			logger.debug("Update own data to Zookeeper: "+record.toString());
			return true;
		} catch (Exception e) {
			logger.warn("Could not update the data");
			return false;
		}
	}

	public boolean updateCurrentRecord() {
		logger.trace( "Trying to update current name record");
		return update(EKHandler.ekRecord.fetchRecord());
	}
	
	public String getZKConnString() {
		return this.client.getZookeeperClient().getCurrentConnectionString();
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
    	if (client.checkExists().forPath(fileMetaDataPath) == null) {
			logger.info("MDFS znode doesn't exist. creating the Znode");
			client.create().forPath(fileMetaDataPath, "File metadata path".getBytes());
			logger.debug("Created new MDFS Znode");
		}
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
		checkIfConnected();
		checkandCreateMDFSRoot();
		String metaDataPath = ZKPaths.makePath(fileMetaDataPath, id);
		if (client.checkExists().forPath(metaDataPath) == null) {
			logger.trace( "Creating metadata for path" + metaDataPath);
			client.create().forPath(metaDataPath, metaData);
			logger.trace( "Created new MetaData" + metaDataPath);
		} else {
			client.setData().forPath(metaDataPath, metaData);
			logger.trace("Modified data for path: " + metaDataPath);
		}
	}

	/**
	 * Retrieves the content of a file metadata
	 * 
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public byte[] retrieveMDFSMetadata(String id) throws Exception {
		checkIfConnected();
		return client.getData().forPath(ZKPaths.makePath(fileMetaDataPath, id));
	}

	/**
	 * Deletes an already existing file Metadata
	 * 
	 * @param id
	 * @throws Exception
	 */
	public void deleteMDFSMetaData(String id) throws Exception {
		checkIfConnected();
		client.delete().forPath(ZKPaths.makePath(fileMetaDataPath, id));
	}

	public boolean checkMetadataExists(String id) throws Exception {
		checkIfConnected();
		if (client.checkExists().forPath(ZKPaths.makePath(fileMetaDataPath, id)) == null)
			return false;
		else
			return true;
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
		
		logger.trace( "Merging GUID data from another edge"+guidMergeData.toString());
		Map<String,ChildData> map = null;
		try {
			map =  cache.getCurrentChildren(nameRecordPath);
		} catch(Exception e) {
			logger.trace("Merger failed as the current cache is null due to ZK client not connected.");
			return;
		}
		
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
				logger.debug("Error in fetching the GUID data from neighbor data info for GUID "+nGuid);
				continue;
			}
			
			/*
			 * Fetch the GUID record corrsponding to the neighbor GUID. Check the master GUID for this node
			 * If the nodes does not belong to the local edge, update the local record. If the node belongs to the 
			 * local edge, ignore the neighbor GUID info. 
			*/
			String lGuidMaster = null;
			try {
				JSONObject lGuidData = parseBytetoJSON( map.get(nGuid).getData());
				lGuidMaster = lGuidData.getString(EKRecord.MASTER_GUID);
			}catch(Exception e ) {
			}
			if(lGuidMaster == null || !lGuidMaster.equals(ownGUID)) {
				try {
					String nZPath = ZKPaths.makePath(nameRecordPath, nGuid);
					client.create().orSetData().withMode(CreateMode.EPHEMERAL).forPath(nZPath, nRec);
					logger.trace("Updated neighbor to the local edge, GUID: "+nGuid);
				} catch (Exception e) {
					logger.warn("Could not update the data");
				}
			}
			else {
				logger.trace("The GUID "+nGuid+" belongs to local edge. Not updating the data.");
			}
		}
	}
	
	public JSONObject prepareMergeData() {
		logger.trace("Preparing GUID data record for edge merger");
		Map<String,ChildData> map = null;
		try {
			map =  cache.getCurrentChildren(nameRecordPath);
		} catch(Exception e) {
			logger.trace("Could not fetch GUID informations as the cache is null");
		}
		if(map == null || map.isEmpty()) {
			logger.trace("Cache is not initialized returning null");
			return null;
		}
		JSONObject mergeJObj = new JSONObject();
		for (String guid : map.keySet()) {
			try {
				JSONObject guidData = parseBytetoJSON( map.get(guid).getData());
				/*
				 * Check if the node belongs to local edge by comparing its master GUID. Add this GUID record to the 
				 * merger data only if the node belongs to the local edge. Alos, for node belonging to 
				*/
				if (guidData.getString(EKRecord.MASTER_GUID).equals(ownGUID)) {
					mergeJObj.put(guid, guidData);
				}
				else { // clean the stale GUID records of neighbors
					if( System.currentTimeMillis() > guidData.getLong(EKRecord.updateTime) + 2*EKConstants.GUID_MERGE_INTERVAL) {
//						logger.debug("GUID record for "+guid+" is stale. Deleting it from current edge");
						client.delete().inBackground().forPath(ZKPaths.makePath(nameRecordPath, guid));
					}
				}
			} catch (Exception e) {
				logger.trace("Problem in creating merge information for "+guid, e);
			}
		}
		return mergeJObj;
	}
	
}
