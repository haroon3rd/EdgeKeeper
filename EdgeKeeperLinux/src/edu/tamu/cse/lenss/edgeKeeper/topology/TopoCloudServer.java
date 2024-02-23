package edu.tamu.cse.lenss.edgeKeeper.topology;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

import javax.lang.model.element.ElementKind;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.tamu.cse.lenss.edgeKeeper.server.EKHandler;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKProperties;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils.NetworkInterfaceType;
import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;

public class TopoCloudServer extends Thread implements Terminable{
	public static final Logger logger = LoggerFactory.getLogger(TopoCloudServer.class);
	private TopoNode ownNode;
	private ServerSocket server = null;
	private String sessionID;
	private long t0;
	
	public TopoCloudServer(TopoNode ownNode) {
		super();
		this.ownNode = ownNode;
	}
	
	@Override
	public void run() {

		this.sessionID = UUID.randomUUID().toString(); // generate separate Session ID
		this.t0 = System.currentTimeMillis();

		TopoHandler.logger.trace("The resident node is running on cloud.");
		try {
			server = new ServerSocket(EKConstants.TOPOCLOUD_DISCOVERY_PORT);
			TopoHandler.logger.trace("Server socket on cloud successfull.");
		} catch (IOException e) {
			TopoHandler.logger.trace("Cannot create server socket on cloud.", e);
		}
		while (!server.isClosed()) {
			Socket cSocket = null;
			DataInputStream in = null;
			DataOutputStream out = null;
			try {
				//TopoHandler.logger.log(Level.ALL,"Topo Cloud Server is listenning on pot : " + EKConstants.TOPOCLOUD_DISCOVERY_PORT);
				cSocket = server.accept();
				in = new DataInputStream (new BufferedInputStream(cSocket.getInputStream()));
				out = new DataOutputStream(cSocket.getOutputStream());
				String inMessage = in.readUTF();								//inBuffer.readLine();
				//TopoHandler.logger.log(Level.ALL, inMessage);
				String replyIP = cSocket.getInetAddress().getHostAddress(); 	//Capturing the IP of the sender Node it replying to.
				out.writeUTF(processRequest(inMessage, replyIP)+"\n\n"); 		//Calls processRequest for generating a reply from the cloud node. 
			} catch (Exception e) {
				TopoHandler.logger.error("Error in accepting client connection", e);
			}finally {
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
	}

	//ON CLOUD__NODE
	//Cloud node calls processRequest from listenClient when a node from edge connects.
	private String processRequest(String inMessage, String sender) {
        try {
			//Packet received
	        TopoMessage inMsg = TopoMessage.getMessage(inMessage);
	        switch(inMsg.messageType) {
	        case TOPO_CLOUD:
	        	String outMsg = prepareCloudReply(inMsg, sender);
	        	return outMsg;
			default:
				TopoHandler.logger.warn("Unknown message type = "+inMsg.messageType+" from " +sender);
	        }
        }catch(Exception e) {
        	TopoHandler.logger.debug("Problem in processing incoming packet ", e);
        }
		return null;
	}	
	
	String prepareCloudReply(TopoMessage inMsg, String destIP) {
		ownNode.ipMaps = EKUtils.getOwnIPv4Map();
		//ownNode.ips = EKUtils.getOwnIPv4s(); //This method ensures that Own node IPs are correctly updated
		ownNode.ipMaps.put(EKUtils.getRealIP(), NetworkInterfaceType.ETHERNET);
		
		/*
		 * We could have kept a broadcast sequence as we did for the case of Edge Topology management.
		 * However, the CloudServer is not preparing the broadcast at one time, instead, it is replying 
		 * a pseudo broadcast message whenever a client pings the cloud. That why we calculate the broadcast seq
		 * based on the start time and the current time.
		*/
		int broadcastSeq = (int) (2*(System.currentTimeMillis()-t0)/EKHandler.getEKProperties().getTopoInterval());
		TopoMessage cloudMessage = TopoMessage.newCloudMsg(this.sessionID, broadcastSeq);
		ownNode.masterGUID=EKHandler.edgeStatus.masterGUID;
		cloudMessage.sender = ownNode;
		cloudMessage.destinationIP = destIP;
		
		String msgStr = cloudMessage.getRawString();
		return msgStr;
	}
	
	@Override
	public void terminate() {
		try {
			this.server.close();
		} catch (Exception e) {
		}
		TopoHandler.logger.info("Terminated "+this.getClass().getName());
	}


	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}

