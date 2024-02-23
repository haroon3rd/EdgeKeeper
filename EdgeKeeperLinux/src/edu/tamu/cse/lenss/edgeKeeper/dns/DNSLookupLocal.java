package edu.tamu.cse.lenss.edgeKeeper.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Section;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import edu.tamu.cse.lenss.edgeKeeper.server.EKHandler;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKRecord;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils;
import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;

public class DNSLookupLocal implements Callable<Message>, Terminable{

	public static final Logger logger = LoggerFactory.getLogger(DNSFallback.class);
	private Message query;

	public DNSLookupLocal(Message query) {
		this.query=query;
	}

	@Override
	public Message call() throws Exception  {
		int queryType = query.getQuestion().getType();
		// check for queries we can't handle
		// Was the query legitimate or implemented?
		if ((Type.string(queryType) == null) ||(!Type.isRR(queryType) && queryType != Type.ANY)) {
			logger.trace( "Wrong query type: "+ Type.string(queryType));
			throw new IllegalArgumentException("Wrong query type");
		}

		// extract the domain  and field from the query
		final Name requestedName = query.getQuestion().getName();
		
		//logger.info("Question Domain Name="+requestedName.toString());
		//final byte[] rawName = requestedName.toWire();
		//final String domainName = NameResolution.querytoStringForGNS(rawName);

		String domainName = requestedName.toString();
		
		if(domainName == null || ! domainName.endsWith(".") ) {
			logger.trace("The domain name " + domainName + " is not an absolute name!");
			throw new IllegalArgumentException("Unsupported Name");
		}
		
		//logger.trace( "Looking at local EK cluster for "+domainName);
		
		/*
		 * This is a hack for EK. In the EK, we did not store the alias name separately. We
		 * assumed that the account name and domain name are same. So, we need to getEdgeStatus the
		 * accountName from the domain name by eliminating the trailing '.'
		 * Afterwards getEdgeStatus the name record from Zookeeper CLient.
		*/
		String accountName =  domainName.substring(0, domainName.length() - 1);
		
		/**
		 * Create a response message, build the header first. The response is
		 * constructed later after GNS query.
		 */
		Message response = new Message(query.getHeader().getID());
		response.getHeader().setFlag(Flags.QR);
		if (query.getHeader().getFlag(Flags.RD)) {
			response.getHeader().setFlag(Flags.RA);
		}
		
		response.addRecord(query.getQuestion(), Section.QUESTION);
		response.getHeader().setFlag(Flags.AA);
		
		//If the query is for master.distressnet.org then reply own IPs
		if(accountName.equals(EKConstants.MASTER_NAME)) {
		
			EKHandler.getEKUtils();
			for (String ip: EKUtils.getOwnIPv4s()) {
				//logger.debug("IP: "+ip);
				ARecord record = new ARecord(new Name(domainName), DClass.IN, 60, InetAddress.getByName(ip));
				//logger.debug("Record: "+record);
				response.addRecord( record, Section.ANSWER);
			}
			return response;
		}
		
		
		JSONObject guidRecord = EKHandler.getZKClientHandler().getRecordbyAccountName(accountName);
		if(guidRecord == null) {
			logger.trace( "Could not find "+accountName+" in local EK. raising Exception");
			throw new UnknownHostException();
		}
		

		/* 
		 * Current EK implementation only reply for Query Type A. The code first fetches the GUID record for the
		 * particular accountName from local EKCluster. Since the local GUID records are stored at the local EK Masters 
		 * using Zookeeper, the ZKClientHandler is used to fetch the record. If the ZKClient is not connected or the 
		 * AccountName is not found, it will return NULL. From the EK Record, this code block prepare the DNS response.
		 * If there is a problem in parsing the JSON or preparing the DNS record, appropriate Exceptions are thrown.
		 * 
		 * If the queries are 
		 *  
		 * Sample EK Record:
		 *	{
		 *	    "EKaccountName": "suman2",
		 *	    "UPDATE_TIME": 1568080579040,
		 *	    "A": {
		 *	        "record": [
		 *	            "192.168.1.131",
		 *	            "128.194.142.72"
		 *	        ],
		 *	        "ttl": 0
		 *	    },
		 *	    "netAddress": [
		 *	        "192.168.1.131",
		 *	        "128.194.142.72"
		 *	    ]
		 *	}
		 * 
		*/
		switch (queryType) {
		case Type.A: 
			try {
				
				JSONObject jobj = guidRecord.getJSONObject(EKRecord.A_RECORD_FIELD);
				JSONArray jarr = jobj.getJSONArray(EKRecord.RECORD_FIELD);
				long ttl = jobj.getInt(EKRecord.TTL_FIELD);
				Name name = new Name(domainName);
				//logger.debug("JArray: "+jarr.toString());
				for (int i =0; i< jarr.length(); i++) {
					String ip = jarr.getString(i);
					//logger.debug("IP: "+ip);
					ARecord record = new ARecord(name, DClass.IN, ttl, InetAddress.getByName(ip));
					//logger.debug("Record: "+record);
					response.addRecord( record, Section.ANSWER);
				}
				//logger.trace( "DNSLookupLocal finds the record. Legitimate response: "+response.toString());
				//return response;

			} catch (JSONException | TextParseException | NullPointerException  e) {
				logger.debug("Problem accessing A record in local EKCluster for accountName: "+accountName);
				//throw e;
			}
			break;
		case Type.AAAA:
			try {
				//JSONObject guidRecord = EKHandler.getZKClientHandler().getRecordbyAccountName(accountName);
				JSONObject jobj = guidRecord.getJSONObject(EKRecord.AAAA_RECORD_FIELD);
				JSONArray jarr = jobj.getJSONArray(EKRecord.RECORD_FIELD);
				long ttl = jobj.getInt(EKRecord.TTL_FIELD);
				Name name = new Name(domainName);
				//logger.debug("JArray: "+jarr.toString());
				for (int i =0; i< jarr.length(); i++) {
					String ip = jarr.getString(i);
					//logger.debug("IP: "+ip);
					AAAARecord record = new AAAARecord(name, DClass.IN, ttl, InetAddress.getByName(ip));
					//logger.debug("Record: "+record);
					response.addRecord( record, Section.ANSWER);
				}
			} catch (JSONException | TextParseException | NullPointerException  e) {
				logger.debug("Problem accessing AAA record in local EKCluster for accountName: "+accountName);
				//throw e;
			}
			break;
		default:
			logger.trace( "Unsupported query type. DNSLookupLocal can not resolve it.");
			throw new IllegalArgumentException("Unsupported query type");
		}
		logger.trace( "DNSLookupLocal finds the Hostname"+ accountName+" in local EK Cluster.");
		return response;
	}

	/*private JSONObject fakeGUIDRecord() {
		JSONObject record = new JSONObject();
		try {
			
			JSONArray jrr = new JSONArray().put("192.168.1.1").put("192.168.1.2");
			JSONObject a = new JSONObject().put(EKRecord.RECORD_FIELD, jrr);
			a.put(EKRecord.TTL_FIELD, 0);
			record.put(EKRecord.A_RECORD_FIELD, a);
			logger.debug(record.toString());
		
		}catch(Exception e) {
			e.printStackTrace();
		}
		return record;
	}*/


	@Override
	public void terminate() {
		Thread.currentThread().interrupt();
    	logger.info("Terminated"+this.getClass().getName());
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
	}

}
