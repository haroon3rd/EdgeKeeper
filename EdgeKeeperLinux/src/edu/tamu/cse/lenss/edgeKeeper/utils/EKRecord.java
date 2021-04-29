package edu.tamu.cse.lenss.edgeKeeper.utils;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.xbill.DNS.Type;

import edu.umass.cs.gnsclient.client.GNSClient;
import edu.umass.cs.gnscommon.GNSProtocol;

import java.util.concurrent.locks.ReentrantLock;

public class EKRecord {
	public static final Logger logger = Logger.getLogger(EKRecord.class);

	
	public String guid = null;
	public String accountName = null;
	public JSONObject record = new JSONObject();
	
	ReentrantLock lock = new ReentrantLock();
	
	public final static String updateTime = "UPDATE_TIME";
	public final static String RECORD_FIELD = "record";
	public final static String TTL_FIELD = "ttl";
	public final static int default_ttl = 0; 
	public final static String A_RECORD_FIELD = Type.string(Type.A);
	public final static String AAAA_RECORD_FIELD = Type.string(Type.AAAA);
	
	public final static String fieldIP = GNSProtocol.IPADDRESS_FIELD_NAME.toString();
	public final static String ACCOUNTNAME_FIELD = "EKaccountName";
	public final static String MASTER_GUID = "MasterGuid";
	
//	public EKRecord(String accountName) {
//		try {
//			this.record.put(ACCOUNTNAME_FIELD, accountName);
//		} catch (JSONException e) {
//			logger.error("Problem in adding own account name to JSON record", e);
//		}
//	}
//	
	
	@SuppressWarnings("finally")
	public boolean updateField(String key, Object value) {
		if (key==null || value ==null) {
			logger.warn("Trying to update invalid entry.");
			return false;
		}
		boolean result=false;
		
		lock.lock();
		try {
			this.record.put(key, value);
			this.record.put(updateTime, System.currentTimeMillis());
			result=true;
			logger.log(Level.ALL, "Updated record= "+this.record.toString());
		} catch (JSONException| NullPointerException e) {
			logger.warn("Problem in updating record. ",e);
			result = false;
		} finally {
			lock.unlock();
			return result;
		}
	}
	
	@SuppressWarnings("finally")
	public boolean removeField(String key) {
		if (key==null ) {
			logger.warn("Trying to update invalid entry.");
			return false;
		}
		boolean result=false;
		
		lock.lock();
		try {
			this.record.remove(key);
			this.record.put(updateTime, System.currentTimeMillis());
			result=true;
			logger.log(Level.ALL, "Updated record= "+this.record.toString());
		} catch (JSONException| NullPointerException e) {
			logger.warn("Problem in updating record. ",e);
			result = false;
		} finally {
			lock.unlock();
			return result;
		}
	}
	
	
	@SuppressWarnings("finally")
	public JSONObject fetchRecord() {
		lock.lock();
		
		JSONObject jRecord =null;
		
		// We are doing this to make sure the JSOn is deeply copied.
		try {
			jRecord = new JSONObject(this.record.toString());
			logger.log(Level.ALL, "fetched record: "+jRecord.toString());
		} catch (JSONException | NullPointerException e) {
			logger.warn("Problem in fetching ownrecord.");
		} finally {
			lock.unlock();
			return jRecord;
		}
	}
	
	
	
	//Added by Amran (Unfinished)
    //This method is written by Amran. ####################################################
    //     _                              
    //    / \   _ __ ___  _ __ __ _ _ __  
    //   / _ \ | '_ ` _ \| '__/ _` | '_ \ 
    //  / ___ \| | | | | | | | (_| | | | |
    // /_/   \_\_| |_| |_|_|  \__,_|_| |_|
	@SuppressWarnings("finally")
	public JSONObject fetchAnyRecord() {
		lock.lock();
		
		JSONObject jRecord =null;
		
		// We are doing this to make sure the JSOn is deeply copied.
		try {
			jRecord = new JSONObject(this.record.toString());
			logger.log(Level.ALL, "fetched record: "+jRecord.toString());
		} catch (JSONException | NullPointerException e) {
			logger.warn("Problem in fetching ownrecord.");
		} finally {
			lock.unlock();
			return jRecord;
		}
	}
	
	
	
	public byte[] fetchRecordBytes() {
		return this.fetchRecord().toString().getBytes();
	}
 	
}
