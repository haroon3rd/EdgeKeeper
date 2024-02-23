package edu.tamu.cse.lenss.edgeKeeper.topology;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.tamu.cse.lenss.edgeKeeper.server.EKHandler;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils;
import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;

public class TopoCloudClient extends Thread implements Terminable{
	public static final Logger logger = LoggerFactory.getLogger(TopoCloudClient.class);
	//public static String startIP = "";
	private TopoGraph ekGraph;
	private TopoNode ownNode;
	static AtomicInteger cloudMsgSeq = new AtomicInteger();
	private boolean isTerminated = false;


	public TopoCloudClient(TopoGraph ekGraph, TopoNode ownNode) {
		super();
		this.ekGraph = ekGraph;
		this.ownNode = ownNode;
	}
	
	@Override
	public void run() {
		String[] cloud = EKHandler.getEKProperties().getGNSAddr();
		while(!this.isTerminated) {
			for (String cloudip: cloud)
				connectCloud(cloudip);
			try {
				sleep((int)EKHandler.getEKProperties().getTopoInterval());
			} catch (InterruptedException e) {
				TopoHandler.logger.error("Problem in sleep",e);
			}
		}		
	}

	//ON EDGE
	//sendRcv is called from connectServer when node is running on Edge
	String connectCloud(String destIP){

		//		String[] cloudAddr = EKHandler.getEKProperties().getGNSAddr();
		DataInputStream in = null;
		DataOutputStream out = null;
		Socket cSocket = null;
		try {
			//TopoHandler.logger.info("Trying to connect Cloud Address # " + destIP);
			//    			sessionID = UUID.randomUUID().toString();
			cSocket = new Socket();
			cSocket.connect(new InetSocketAddress(destIP,EKConstants.TOPOCLOUD_DISCOVERY_PORT), EKConstants.ClientConnectTimeout);
			cSocket.setSoTimeout(EKConstants.ClientSoTimeout);
			in = new DataInputStream (new BufferedInputStream(cSocket.getInputStream()));
			out = new DataOutputStream(cSocket.getOutputStream());
			long t1 = System.currentTimeMillis();
			TopoHandler.logger.trace("Request sent to cloud IP: " + destIP + " seccessfully.");
			out.writeUTF(prepareCloudMsg(destIP) +"\n\n");
			TopoMessage reply = TopoMessage.getMessage(in.readUTF());
			//startIP = reply.destinationIP;
			long t2 = System.currentTimeMillis();
			long cloudRTT = t2-t1;
			if(reply.destinationIP!=null) {
				TopoHandler.logger.trace("Reply from cloud recieved at " +cSocket.getLocalAddress().getHostAddress());
//				ekGraph.updateCloudNode(ownNode,reply.sender, reply.destinationIP, cSocket.getInetAddress().getHostAddress(),cloudRTT, reply.sessionID, reply.BroadcastSeq);
				ekGraph.updateEdge(ownNode,reply.sender, reply.destinationIP, cSocket.getInetAddress().getHostAddress(),1.0, reply.sessionID, reply.BroadcastSeq, reply.sessionID, reply.BroadcastSeq,1.0 );
				ekGraph.updateRTT(ownNode, reply.sender, reply.destinationIP, cSocket.getInetAddress().getHostAddress(),cloudRTT );
			}
			//logger.log(Level.ALL, "Successfully communicated with Cloud IP "+destIP);
		}catch(Exception e) {
			TopoHandler.logger.trace("Could not communicate with cloud IP: " + destIP);
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
		return null;
	}

	String prepareCloudMsg(String destIP) {
		ownNode.ipMaps = EKUtils.getOwnIPv4Map();
		//ownNode.ips = EKUtils.getOwnIPv4s(); //This method ensures that Own node IPs are correctly updated
		TopoMessage cloudMessage = TopoMessage.newCloudMsg(null,0);
		cloudMessage.sender = ownNode;
		cloudMessage.destinationIP = destIP;

		String msgStr = cloudMessage.getRawString();
		return msgStr;
	}

	@Override
	public void terminate() {
		this.isTerminated = true;
		this.interrupt();
		TopoHandler.logger.info("Terminated "+this.getClass().getName());
	}


	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}

