package edu.tamu.cse.lenss.edgeKeeper.clusterHealth;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import edu.tamu.cse.lenss.edgeKeeper.server.EKHandler;
import edu.tamu.cse.lenss.edgeKeeper.server.RequestTranslator;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKProperties;
import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;

//this is a simple tcp client that connects to a server and send/receive JSONObject
public class ClusterHealthClient implements Terminable {

	//logger
	static final Logger logger = Logger.getLogger(ClusterHealthClient.class);
	long CLUSTER_MONITOR_DEVICE_STATUS_SUBMISSION_INTERVAL;

	//global variable
	private boolean isRunning;


	//default public constructor
	public ClusterHealthClient(){
		CLUSTER_MONITOR_DEVICE_STATUS_SUBMISSION_INTERVAL = Math.max( 
				EKHandler.getEKProperties().getInteger(EKProperties.deviceStatusInterval),10000);
	}

	//connect(), timeout(), send(), receive(), close() and return
	public static synchronized JSONObject communicateWiteHealthServer(JSONObject data) {
		long t1 = new Date().getTime();

		String leaderIP = null;
		try {
			//leaderIP = EKHandler.getZKClientHandler().getHealthLeader();
			leaderIP = EKHandler.edgeStatus.getMasterIps().iterator().next();
			//logger.log(Level.ALL, "Cluster health leader: "+leaderIP);
		} catch (Exception e1) {
			logger.debug("Could not obtain the IP of EK master. Aborting communication", e1);
			return null;
		}

		Socket cSocket = null;
		DataInputStream in = null;
		DataOutputStream out = null;
		try {
			//connect
			cSocket = new Socket();
			cSocket.connect(new InetSocketAddress(leaderIP, EKConstants.CLUSTER_MONITOR_SERVER_PORT), EKConstants.ClientConnectTimeout);
			in = new DataInputStream (new BufferedInputStream(cSocket.getInputStream()));
			out = new DataOutputStream(cSocket.getOutputStream());
			//timeout
			cSocket.setSoTimeout(EKConstants.ClientSoTimeout);
			//send
			out.writeUTF(data.toString() + "\n\n");
			//receive
			String rep = in.readUTF().trim();
			if(rep!=null){
				logger.log(Level.DEBUG, "CHS client successfully communicated with master with master IP "+cSocket.getInetAddress().getHostAddress()
					+" in " + (new Date().getTime()-t1) + " milli seconds.");
				return new JSONObject(rep);
			} else {
				logger.log(Level.DEBUG, "CHS client got null in return from master "+cSocket.getInetAddress().getHostAddress()
						+" in " + (new Date().getTime()-t1) + " milli seconds.");
					return null;
			}
		} catch (Exception e) {
			logger.debug("Problem in creating connection socket for master IP "+leaderIP, e);
			return null;
		} finally {
			try {
				in.close();
			} catch (Exception e) {}
			try {
				out.close();
			} catch (Exception e) {}
			try {
				cSocket.close();
			} catch (Exception e) {}
		}
	}


	@Override
	public void run() {
		this.isRunning = true;
		while(isRunning) {
			try {
				reportDeviceStatus();
			}catch (Exception e) {
				logger.debug("Problem in pushing device status", e);
			}
			try {
				//Thread.sleep(EKConstants.CLUSTER_MONITOR_DEVICE_STATUS_SUBMISSION_INTERVAL);
				Thread.sleep(CLUSTER_MONITOR_DEVICE_STATUS_SUBMISSION_INTERVAL);
			} catch (Exception e) {
				logger.debug("Sleep in device updater interruped");
			}
		}
	}


	//@Override
	public void terminate() {
		logger.log(Level.ALL, "CHS terminate function called.");

		isRunning = false;
		Thread.currentThread().interrupt();
		logger.info("CHS Terminated "+this.getClass().getName());
	}

	//static function to fetch device status
	public static void reportDeviceStatus() {

		//logger.log(Level.ALL, "Trying to update device status");
		try {
			//fetch device status
			JSONObject deviceStatusData = EKHandler.getEKUtils().getDeviceStatus();

			if(deviceStatusData!=null) {
				//logger.log(Level.ALL, "fetched valid device status from getDeviceStatus()");

				//add my ownGUID
				deviceStatusData.put(RequestTranslator.fieldGUID, EKHandler.getGNSClientHandler().getOwnGUID());

				//send and receive reply
				JSONObject repJSON = communicateWiteHealthServer( deviceStatusData);

				//check reply
				if(repJSON!=null && repJSON.getString(RequestTranslator.resultField).equals(RequestTranslator.successMessage)) {
					logger.log(Level.ALL, "device status pushed to server succesfully");
				}else {
					logger.log(Level.ALL, "failed to push device status to server");
				}
			}
			else
				logger.warn("Can not fetch Device status using EKUtils. Not reporting to Master.");
		}catch(JSONException e) {
			logger.log(Level.DEBUG, "Problem with adding GUID field to device status JSON.", e);
		}

	}
}

