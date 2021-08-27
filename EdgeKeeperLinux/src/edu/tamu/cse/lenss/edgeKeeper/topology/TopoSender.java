package edu.tamu.cse.lenss.edgeKeeper.topology;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import android.telephony.NeighboringCellInfo;
import edu.tamu.cse.lenss.edgeKeeper.dns.DNSServer;
import edu.tamu.cse.lenss.edgeKeeper.server.EKHandler;
import edu.tamu.cse.lenss.edgeKeeper.topology.TopoMessage.MessageType;
import edu.tamu.cse.lenss.edgeKeeper.topology.TopoNode.NodeType;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKProperties;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils;
import org.omg.CORBA.INTERNAL;

/**
 * Thic class is responsible for sending UDP broadcast with two hop neighbors
 * @author sbhunia
 *
 */
public class TopoSender {

	public static final Logger logger = Logger.getLogger(TopoSender.class);
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
		super(); //No NeEd
		sockMap = sMap;
		this.ekGraph = ekGraph;
		ownNode = oNode;
		sessionID = UUID.randomUUID().toString(); // generate separate Session ID
	}
	
	//the function that does the sending
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
			ownSocket.send(packet);
			logger.log(Level.ALL, message.messageType +" sent to " +destIP  
						+" through "+ownSocket.getLocalAddress().getHostAddress()+", length="+packet.getLength());
		} catch (IOException e) {
			logger.warn("Problem in sending "+message.messageType +" to " +destIP  
					+" through "+ownSocket.getLocalAddress().getHostAddress()+":"+ownSocket.getLocalPort());
		}
	}

	//prepare a broadcast message
	TopoMessage prepareBroadcast(){

		//increment broadcast sequence number
		int msgSeq = broadcastSeq.addAndGet(EKConstants.BRD_SEQ_INCR);

		//create new broadcast message
		TopoMessage message = TopoMessage.newBroadcastMessage(sessionID, msgSeq);

		//sender = myself
		message.sender = ownNode;

		//append my own neighbor status
		message.neighborStatus = ekGraph.getNeighborMap();

		//add current time
		seq_time.put(msgSeq, System.currentTimeMillis());

		seq_time.remove(msgSeq-28);  // Always delete the old values to prevent the map from growing infinitely
		return message;
	}


	//Send LTE unicast to all IPs.
	//Works as poor man's Neighbor Service Discovery over LTE.
	private void topoLTEunicast(TopoMessage message) {

		try {
			//set message type
			message.messageType = MessageType.TOPO_LTE_UNICAST;

			//check if this node currently have a LTE interface, if so, get the IP and subnet Mask
			String[] LTEipAndMask = EKUtils.getLTEipAndSubnetMask();

			//if this node has an LTE interface and class C
			if(LTEipAndMask!=null && Integer.parseInt(LTEipAndMask[1])>=24){

				//check if sockMap already contains a socket for this ip
				if(sockMap.containsKey(LTEipAndMask[0])) {
					//send broadcast from ip range 2 to 254
					String destIPprefix = LTEipAndMask[0].substring(0,LTEipAndMask[0].lastIndexOf("."));

					for(int i=2; i<255; i++){

						//prepare destIP
						String destIP = destIPprefix + "." + i;

						//set probability
						if(this.ekGraph.getAllIPs().contains(destIP)){
							message.thisLinkRcvProb = ekGraph.getPrcvforNeighbor(ekGraph.getNodebyIP(destIP));
						}else{
							message.thisLinkRcvProb = EKConstants.TOPO_INITIAL_PROB;
						}

						//send
						sendMessage(message, destIP, sockMap.get(LTEipAndMask[0]));
					}
				}else{
					//we do not send message now, wait until a socket for LTE interface is opened by TopoHandler.run()
				}
			}


		}catch (Exception e){
			e.printStackTrace();
		}


	}


	//everyone regardless of being a master or client, sends this broadcast to all nodes in its current graph
	void topoBroadcast(TopoMessage message){

		try {
			message.messageType=MessageType.TOPO_BROADCAST;

			Set<String> brodDests = new HashSet<String>();
			
			if (EKHandler.edgeStatus.getMasterIps()==null || EKHandler.edgeStatus.getMasterIps().isEmpty()) {
				logger.log(Level.ALL, "Could not retrieve the IPs of the master. Not sending any ping.");
				return;
			}
			
			brodDests.addAll(EKHandler.edgeStatus.getMasterIps());
			brodDests.removeAll(ownNode.ipMaps.keySet());

			//Send the broadcast to only nodes which belong to same edge.
			for (TopoNode v : ekGraph.vertexSet()) {
				try {
					//if destination not me, and same edge, and destination IP exists, and not a neighbor, and not a cloud node
					//this means destination is a non-master node in same edge
					if ((!ownNode.equals(v)) && (v.masterGUID.equals(ownNode.masterGUID)) && !v.ipMaps.isEmpty() && v.type!=NodeType.EDGE_NEIBOR && v.type!=NodeType.CLOUD__NODE) {

						//get percent receive for destination node
						message.thisLinkRcvProb = ekGraph.getPrcvforNeighbor(v);

						//for each IP of destination node
						for (String ip : v.ipMaps.keySet()) {
							//for each IP of this node
							for(DatagramSocket serverSock:sockMap.values()) {
								sendMessage(message, ip, serverSock);
							}
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
			logger.warn("Problem in sending the broadcast", e);
		}
	}


	//called in a while() loop in TopoHandler class.
	void sendPeriodicPing() {
		// This lock ensure that the graph is not modified while sending broadcast
		synchronized (ekGraph) {

			//prepare broadcast message
			TopoMessage message = prepareBroadcast();

			//if this node is Master
			if(EKHandler.edgeStatus.selfMaster()){

				//init ownNode type
				ownNode.type=NodeType.EDGE_MASTER;

				//send periodic ping to neighbor master IPs if I am a master
				topoNeigbor(message);
			}else {
				//init ownNode type
				ownNode.type = NodeType.EDGE_CLIENT;
			}

			//everyone regardless of being a master or client, sends this broadcast to all nodes in its current graph
			topoBroadcast(message);

			//send lte unicast to all possible class C IPs
			//topoLTEunicast(message);

			// Now check all the links and update the link if broadcast is not received on that link for long time
			ekGraph.cleanup();

			if(!EKUtils.isAndroid())
				TopoUtils.toPng(ekGraph);
		}
	}

	//send periodic ping to neighbor master IPs if I am a master
	private void topoNeigbor(TopoMessage message) {

		//set message type
		message.messageType=MessageType.TOPO_NEIGHBOR_MSG;

		//get all neighbor IPs fro Properties file
		String neighborIps = EKHandler.getEKProperties().getProperty(EKProperties.neighborIPs);

		//if all IPs valid
		if (EKProperties.validIP(neighborIps)) {

			//parse IPs
			String[] ips = neighborIps.split(",");

			//set probability
			message.thisLinkRcvProb = EKConstants.TOPO_INITIAL_PROB;

			//send message to all IPs
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