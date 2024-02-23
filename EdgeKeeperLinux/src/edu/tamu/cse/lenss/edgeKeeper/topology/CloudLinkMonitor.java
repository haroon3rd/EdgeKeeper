package edu.tamu.cse.lenss.edgeKeeper.topology;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.tamu.cse.lenss.edgeKeeper.server.EKHandler;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;
import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;

/**
 * This class monitors the delay (in terms of RoundTrip time) to each of the GNS server  IPs specified in the 
 * configuration. Note that, the checking is done for a specific maximum cutoff and if the Ping reply is not 
 * received within that time, the maximum cutoff threshold is put there.
 * @author sbhunia
 *
 */
public class CloudLinkMonitor implements Terminable{
    static final Logger logger = LoggerFactory.getLogger(CloudLinkMonitor.class);

    String[] gnsAddresses;
    ExecutorService executor;
    Map<String,Long> rttMap = new HashMap<String,Long>();
	
	public CloudLinkMonitor() {
		this.gnsAddresses = EKHandler.getEKProperties().getGNSAddr();
		executor = Executors.newFixedThreadPool(gnsAddresses.length*(2));
		for(String ip: this.gnsAddresses) 
			rttMap.put(ip, (long) EKConstants.PING_TIMEOUT);	 
	}
	
	public void triggerRTTCheck(){	
		for(String ip: this.gnsAddresses) {
			executor.submit(new RTTGetter(ip));
		}
	}
	
	public JSONObject getRTTJSON() {
		JSONObject jo = new JSONObject(rttMap);
		return jo;
	}
	
	/*
	 * This class tries to measure the RTT in a separate thread.
	 */
	class RTTGetter implements Runnable{
		String ip;
		
		public RTTGetter(String ip) {
			this.ip=ip;
		}

		@Override
		public void run() {
			try {
				long t1 = System.currentTimeMillis();
				if( InetAddress.getByName(ip).isReachable(EKConstants.PING_TIMEOUT)) {
					long t2 = System.currentTimeMillis();
					rttMap.put(ip, t2-t1);
					logger.trace("Ping RTT to ip "+ip+" is "+(t2-t1));
					return;
				}
			} catch (IOException e) {
				logger.trace("Exception occured in pinging "+ip);
			}
			rttMap.put(ip, (long) EKConstants.PING_TIMEOUT);
		}
	}

	@Override
	public void terminate() {
		if(this.executor!=null)
			this.executor.shutdownNow();
    	logger.info("Terminated"+this.getClass().getName());
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
}
