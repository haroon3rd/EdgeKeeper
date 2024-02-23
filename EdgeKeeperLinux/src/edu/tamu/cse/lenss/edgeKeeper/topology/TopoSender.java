package edu.tamu.cse.lenss.edgeKeeper.topology;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.telephony.NeighboringCellInfo;
import edu.tamu.cse.lenss.edgeKeeper.dns.DNSServer;
import edu.tamu.cse.lenss.edgeKeeper.server.EKHandler;
import edu.tamu.cse.lenss.edgeKeeper.topology.TopoMessage.MessageType;
import edu.tamu.cse.lenss.edgeKeeper.topology.TopoNode.NodeType;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKProperties;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils;

/**
 * Thic class is responsible for sending UDP broadcast with two hop neighbors
 * @author sbhunia
 *
 */
public class TopoSender {
	public static final Logger logger = LoggerFactory.getLogger(TopoSender.class);
	static Map<String, DatagramSocket>  sockMap;
	private TopoGraph ekGraph;
	private boolean isTerminated;
	private static TopoNode ownNode;
	static AtomicInteger broadcastSeq = new AtomicInteger();
	static String sessionID;
	
	static Map<Integer, Long> seq_time = new HashMap<Integer, Long>();

	/**
	 * Default constructor
	 * @param sMap Socket map  mapping IP to its associated Datagram Socket
	 * @param ekGraph 
	 * @param oNode
	 */
	public TopoSender(Map<String, DatagramSocket> sMap, TopoGraph ekGraph, TopoNode oNode) {
		super();
		sockMap = sMap;
		this.ekGraph = ekGraph;
		ownNode = oNode;
		sessionID = UUID.randomUUID().toString(); // generate separate Session ID
	}
	
	/*
	 * This function sends the message class as UDP packet.
	 */
	static void sendMessage(TopoMessage message, String destIP, DatagramSocket ownSocket) {
		message.destinationIP = destIP; // This IP will be used at the destination to check on which link the pkt is received 
		
		byte[] sendByte = message.getByteArray();
		if (sendByte.length >= EKConstants.TOPO_BUFFER_SIZE) {
			logger.warn("Topology packet size larger than receiver buffer. Discarding packet " + sendByte.length);
			return;
		}
		try {
			DatagramPacket packet = new DatagramPacket(sendByte, sendByte.length, 
					InetAddress.getByName(destIP), EKConstants.TOPOLOGY_DISCOVERY_PORT);
			//logger.log(Level.ALL, "Packet created for : " + destIP + " | " +  EKConstants.TOPOLOGY_DISCOVERY_PORT);
			ownSocket.send(packet);
			logger.trace( message.messageType +" sent to " +destIP  
						+" through "+ownSocket.getLocalAddress().getHostAddress()+", length="+packet.getLength());
		} catch (IOException e) {
			logger.warn("Problem in sending "+message.messageType +" to " +destIP  
					+" through "+ownSocket.getLocalAddress().getHostAddress()+":"+ownSocket.getLocalPort());
		}
	}
	
	TopoMessage prepareBroadcast() {
		int msgSeq = broadcastSeq.addAndGet(EKConstants.BRD_SEQ_INCR);
		TopoMessage message = TopoMessage.newBroadcastMessage(sessionID, msgSeq);
		message.sender = ownNode;
		message.neighborStatus = ekGraph.getNeighborMap();
		seq_time.put(msgSeq, System.currentTimeMillis());
		seq_time.remove(msgSeq-28);  // Always delete the old values to prevent the map from growing infinitely
		return message;
	}

	void topoBroadcast(TopoMessage message) {
		try {
			//TopoMessage message = prepareBroadcast();
			message.messageType=MessageType.TOPO_BROADCAST;

			Set<String> brodDests = new HashSet<String>();
			
			if (EKHandler.edgeStatus.getMasterIps()==null || EKHandler.edgeStatus.getMasterIps().isEmpty()) {
				logger.trace("Could not retrieve the IPs of the master. Not sending any ping.");
				return;
			}
			
			brodDests.addAll(EKHandler.edgeStatus.getMasterIps());
			brodDests.removeAll(ownNode.ipMaps.keySet());
			
			// Send Topo broadcast to all the nodes in the graph 
			for (TopoNode v : ekGraph.vertexSet()) {
				try {
					/*
					 * Send the broadcast to only nodes which belong to same edge.
					*/
					if ((!ownNode.equals(v)) && (v.masterGUID.equals(ownNode.masterGUID)) && !v.ipMaps.isEmpty() 
							&& v.type!=NodeType.EDGE_NEIBOR && v.type!=NodeType.CLOUD__NODE) {
						message.thisLinkRcvProb = ekGraph.getPrcvforNeighbor(v);
						for (String ip : v.ipMaps.keySet()) {
							for(DatagramSocket serverSock:sockMap.values()) 
								sendMessage(message, ip, serverSock);
							brodDests.remove(ip);
						}
					} 
				}catch (Exception e) {
					logger.debug("Problem in accessing some record for node "+v.guid, e);
				}
			}
			
			// Send broadcast to remaining nodes in the Broadcast list
			message.thisLinkRcvProb = EKConstants.TOPO_INITIAL_PROB;
			for (String ip : brodDests)
				for(DatagramSocket serverSock:sockMap.values()) 
					sendMessage(message, ip, serverSock);
		} catch (Exception e) {
			logger.warn("Problem in sending the boradcast", e);
		}
	}
	
	void sendPeriodicPing() {
		synchronized (ekGraph) {  // This lock ensure that the graph is not modified while sending broadcast
			TopoMessage message = prepareBroadcast();
			
			if(EKHandler.edgeStatus.selfMaster()) {
				ownNode.type=NodeType.EDGE_MASTER;
				topoNeigbor(message);
			}
			else
				ownNode.type=NodeType.EDGE_CLIENT;
		
			topoBroadcast(message);
			// Now check all the links and update the link if broadcast is not received on that link for long time
			ekGraph.cleanup();
			if(!EKUtils.isAndroid())
				TopoUtils.toPng(ekGraph);
		}
	}
	
//	TopoMessage prepareNeighMsg() {
//		//int msgSeq = neighbourSeq.addAndGet(2);
//		int msgSeq = broadcastSeq.get();
//		TopoMessage message = TopoMessage.newNeighborMessage(sessionID, msgSeq);
//		message.sender = ownNode;
//		seq_time.put(msgSeq, System.currentTimeMillis());
//		seq_time.remove(msgSeq-18);  // Always delete the old values to prevent the map from growing infinitely
//		return message;
//	}
	
	private void topoNeigbor(TopoMessage message) {
		message.messageType=MessageType.TOPO_NEIGHBOR_MSG;
		//TopoMessage message = prepareNeighMsg();
		String neighborIps = EKHandler.getEKProperties().getProperty(EKProperties.neighborIPs);
		if (EKProperties.validIP(neighborIps)) {
			String[] ips = neighborIps.split(",");
			message.thisLinkRcvProb = EKConstants.TOPO_INITIAL_PROB;
			for(String ip: ips) {
				for(DatagramSocket serverSock:sockMap.values()) 
					sendMessage(message, ip, serverSock);
			}
		}
	}

	public static long getRTT(String replysessionID, int seqNo){
		if(!sessionID.equals(replysessionID))
			return Long.MAX_VALUE;
		Long t1 = seq_time.get(seqNo);
		if(t1 == null) {
			logger.warn("Received reply for very old broadcast. Discarding the sequence no. "+seqNo);
			return Long.MAX_VALUE;
		}
		return System.currentTimeMillis() - t1;
	}
//	public void terminate() {
//		this.isTerminated = true;
//		this.interrupt();
//		logger.info("Terminated "+this.getClass().getName());
//	}
}
