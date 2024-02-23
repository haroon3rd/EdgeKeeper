package edu.tamu.cse.lenss.edgeKeeper.server;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

//import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.tamu.cse.lenss.edgeKeeper.utils.EKRecord;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils;
import edu.tamu.cse.lenss.edgeKeeper.zk.ZKClientHandler;

public class RequestResolver{
//	public static final Logger logger = LoggerFactory.getLogger(RequestResolver.class);
	public static final Logger logger = LoggerFactory.getLogger(RequestResolver.class);

	
	GNSClientHandler gnsClientHandler;
	ZKClientHandler zkClientHandler;
    
    public RequestResolver(GNSClientHandler gnsClientHandler, 
    		ZKClientHandler zkClientHandler){
    	this.gnsClientHandler = gnsClientHandler;
    	this.zkClientHandler = zkClientHandler;
	}

	
	public boolean addService(String serviceName, String duty) {
		return updateFieldStrict(serviceName, duty);
//		boolean result = EKHandler.ekRecord.updateField(serviceName, duty);
//		logger.debug("Updated the service to own record ");
//		triggerUpdate();
//		return result;
	}

	public boolean removeService(String serviceName) {
		return updateFieldLazy(serviceName, "NULL");
//		boolean result = EKHandler.ekRecord.removeField(serviceName);
//		logger.debug("Removed service from own record");
//		triggerUpdate();
//		return removeFieldStrict(serviceName);
	}

	
	private boolean updateFieldStrict(String field, Object value) {
		//gnsClientHandler.update(EKHandler.ekRecord);
		//zkClientHandler.update(EKHandler.ekRecord);
		
		JSONObject record = EKHandler.ekRecord.fetchRecord();
		try {
			record.put(field, value);
		} catch (JSONException e) {
			logger.error("Could not add these key pair in the record: "+field+", "+value);
			return false;
		}
		if(zkClientHandler.update(record)) {
			EKHandler.ekRecord.updateField(field, value);
			gnsClientHandler.update();
			return true;
		}
		else {
			logger.debug("Could not update to Zookeeper. So, discarding the update.");
			return false;
		}
	}
	
	
	private boolean updateFieldLazy(String field, Object value) {
		EKHandler.ekRecord.updateField(field, value);
		zkClientHandler.updateCurrentRecord(); 
		gnsClientHandler.update();
		return true;
	}

	
//	private boolean removeFieldStrict(String field) {
//		JSONObject record = EKHandler.ekRecord.fetchRecord();
//		record.remove(field);
//		if(zkClientHandler.update(record)) {
//			EKHandler.ekRecord.removeField(field);
//			gnsClientHandler.update();
//			return true;
//		}
//		else {
//			logger.debug("Could not update to Zookeeper. So, discarding the update.");
//			return false;
//		}
//	}


	public boolean updateIP() {
		Set<String> ownIPs = EKUtils.getAllAPs();
		return updateIP(ownIPs); 
	}
	
	public boolean updateIP(Set<String> ownIPs) {
		boolean result;
		try {
	        logger.trace(" Trying to update own IP to the GNS server");
	        
	        JSONArray ownAddrJArray = new JSONArray();
	        for (String addr: ownIPs)
	            ownAddrJArray.put(addr);
	
	        /*	Now, update A record for DNS
	         * A record data type:
	         * A:	{
	         * 			record: [ip1,ip2...],
	         * 			ttl: 60
	         *		}
	         */
	        JSONObject recordObj = new JSONObject();
	        recordObj.put(EKRecord.RECORD_FIELD, ownAddrJArray);
	        recordObj.put(EKRecord.TTL_FIELD, EKRecord.default_ttl);
	        
	        //updateField(field, value)
	        
	        result = updateFieldLazy(EKRecord.A_RECORD_FIELD, recordObj);
	        result = updateFieldLazy(EKRecord.fieldIP,ownAddrJArray);
	
	        logger.debug("Updated IP fields to own record");
	        //triggerUpdate();
		} catch(NullPointerException | JSONException e) {
			logger.error("Problem in creating own name record", e);
			result = false;
		}
		return result;
	}

	public String getOwnGUID() {
		return gnsClientHandler.getOwnGUID();
	}

	public String getOwnAccountName() {
		return gnsClientHandler.getOwnAccountName();
	}

	public  JSONArray getPeerGUIDs(String service, String duty) {
		List<String> local = zkClientHandler.getPeerGUIDs(service, duty);
		logger.trace("Local query returns: "+local);
		
		List<String> global = gnsClientHandler.getPeerGUIDs(service, duty);
			logger.trace("Global query returnes: "+global);
		
		return jArrayMerge(local, global);
	}

	public JSONArray getIPsFromGuid(String guid) {
		List<String> local = zkClientHandler.getIPsFromGuid(guid);
		logger.trace("Local query returns: "+local);
		
		List<String> global = gnsClientHandler.getIPsFromGuid(guid);
		logger.trace("Global query returnes: "+global);
		
		return jArrayMerge(local, global);
	}

	public String getAccountNamebyGUID(String guid) {
		String local = zkClientHandler.getAccountNamebyGUID( guid);
		logger.trace("Local query returns: "+local);
		
		String global = gnsClientHandler.getAccountNamebyGUID( guid);
			logger.trace("Global query returnes: "+global);
		
		return compareAndAction(local,global);
	}
	
	public String getGUIDbyAccountName(String AccountName) {
		String local = zkClientHandler.getGUIDbyAccountName( AccountName);
		logger.trace("Local query returns: "+local);
		
		String global = gnsClientHandler.getGUIDbyAccountName( AccountName);
			logger.trace("Global query returnes: "+global);
		
		return compareAndAction(local,global);
	}

	public JSONArray getGUIDbyIP(String ip) {
		List<String> local = zkClientHandler.getGUIDbyIP(ip);
		logger.trace("Local query returns: "+local);
		
		List<String> global = gnsClientHandler.getGUIDbyIP(ip);
			logger.trace("Global query returnes: "+global);
		
		return jArrayMerge(local, global);
	}


	private String compareAndAction(String local, String global) {
		if(local == null && global==null)
			return "";
		if(local == null)
			return global;
		if (global==null)
			return local;
		
		if(!local.equals(global))
			logger.warn("local and global strings are different. local: "+local+" global:"+global);
		return local;
	}

	JSONArray jArrayMerge(Collection<String> a1, Collection<String> a2) {
		Set<String> r = new HashSet<String>();
		if (a1!= null)
			r.addAll(a1);
		if(a2!=null)
			r.addAll(a2);
		JSONArray result = new JSONArray();
		for(String s: r)
			result.put(s);
		return result;
	}


	public JSONArray getPeerIPs(String service, String duty) {
		JSONArray result = new JSONArray();
		JSONArray guids = getPeerGUIDs(service, duty);
		for (int i=0; i< guids.length(); i++) {
			try {
				JSONArray ips = getIPsFromGuid( guids.getString(i));
				for (int j =0; j<ips.length(); j++)
					result.put(ips.get(j));
			} catch (JSONException e) {
				logger.debug("Problem with JSON", e);
			}
		}
		logger.debug("Service: "+service+" Duty: "+duty+" peer IPS: "+ result);
		return result;
	}


	public JSONArray getPeerNames(String service, String duty) {
		JSONArray result = new JSONArray();
		JSONArray guids = getPeerGUIDs(service, duty);
		for (int i=0; i< guids.length(); i++) {
			try {
				result.put( getAccountNamebyGUID( guids.getString(i)) );
			} catch (JSONException e) {
				logger.debug("Problem with JSON", e);
			}
			
		}
		logger.debug("Service: "+service+" Duty: "+duty+" peer Names:: "+ result);
		return result;
	}


	public JSONArray getAccountNamebyIP(String ip) {
		JSONArray result = new JSONArray();
		JSONArray guids = getGUIDbyIP(ip);
		for (int i=0; i< guids.length(); i++) {
			try {
				result.put( getAccountNamebyGUID( guids.getString(i)) );
			} catch (JSONException e) {
				logger.debug("Problem with JSON", e);
			}
		}
		logger.debug(" Accountnames for IP: "+ip+ " Names: "+ result);
		return result;
	}
	public JSONArray getIPsFromAccountName(String name) {
		String guid = getGUIDbyAccountName(name);
		return getIPsFromGuid(guid);
	}
	
//	public JSONObject metaDataRequest(JSONObject jo) {
//		return metaDataHandler.resolveRequest(jo);
//	}


	public String getZKConnString() {
		return zkClientHandler.getZKConnString();
	}


	public boolean purgeNamingCluster() {
		return zkClientHandler.purgeNamingCluster();
	}


	public JSONArray getAllLocalGUIDs() {
		return zkClientHandler.getAllLocalGUIDs();
	}


	public JSONObject readGUID(String guid) {
		return zkClientHandler.readGUID(guid);
	}


	public JSONArray getMergedGUIDs() {
		return zkClientHandler.getMergedGUIDs();
	}
}
