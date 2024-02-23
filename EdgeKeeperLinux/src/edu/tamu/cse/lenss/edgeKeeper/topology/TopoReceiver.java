package edu.tamu.cse.lenss.edgeKeeper.topology;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.tamu.cse.lenss.edgeKeeper.dns.DNSServer;
import edu.tamu.cse.lenss.edgeKeeper.topology.TopoNode.NodeType;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;

/**
 * This class is responsible for running a thread and accept the UDP packets on the server ports.
 * @author sbhunia
 *
 */

public class TopoReceiver extends Thread {
	public static final Logger logger = LoggerFactory.getLogger(TopoReceiver.class);
	DatagramSocket serverSock;
	private TopoGraph ekGraph;
	private TopoNode ownNode;
	
	public TopoReceiver(DatagramSocket servSocket, TopoGraph ekGraph, TopoNode ownNode) {
		super();
		this.serverSock=servSocket;
		this.ekGraph = ekGraph;
		this.ownNode = ownNode;
	}

	/**
	 * This function basically accepts the UDP packets on the server port and the let the processor to handle it
	 */
	@Override
	public void run() {
		logger.debug( "Started receiver thread for "+this.serverSock.getLocalAddress().getHostAddress());
		while (!serverSock.isClosed()) {
			try {
				byte[] recvBuf = new byte[EKConstants.TOPO_BUFFER_SIZE];
				DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
				serverSock.receive(packet);

				// This ensures that the topo broadcast is not sent while processing incoming packet
				synchronized (ekGraph) { 
					processPacket(packet);
				}
			} catch (Exception e) {
				logger.debug("Problem in accepting UDP packet ", e);
			}
		}
	}
	  

	
	private void processPacket(DatagramPacket packet) {
        try {
			//Packet received
	        String senderAddress = packet.getAddress().getHostAddress();
	        //String dataStr = new String(packet.getData());
	        
	        //logger.log(Level.ALL, "received packet from" + senderAddress+ " message: "+dataStr);
	        
	        //TopoMessage inMsg = TopoMessage.createTopoMessage(packet.getData());
	        TopoMessage inMsg = TopoMessage.getMessage(packet.getData());
	        
	        if (ownNode.equals(inMsg.sender)) {
	        	logger.trace("The packet is from own node. Discarding it.");
	        	return;
	        }
	        
	        if(!inMsg.destinationIP.equals(this.serverSock.getLocalAddress().getHostAddress())) {
	        	logger.trace( "Message received on wrong interface. "
	        			+ "actual = "+ serverSock.getLocalAddress().getHostAddress()
	        			+" destined = "+inMsg.destinationIP);
	        	return;
	        }
	        
	        			
	        
	        switch(inMsg.messageType) {
	        case TOPO_BROADCAST:
	        	onBroadcastReceived(inMsg,senderAddress);
	        	break;
	        case TOPO_REPLY:
	        	onReplyReceived(inMsg, senderAddress);
	        	break;
	        case TOPO_NEIGHBOR_MSG:
	        	onNeighMessage(inMsg,senderAddress);
	        	break;
	        case TOPO_NEIGHBOR_REPLY:
	        	onNeighbourReply(inMsg,senderAddress);
	        	break;

	        default:
	        	logger.warn("Unknown message type = "+inMsg.messageType+" from " +senderAddress);
	        }
        }catch(Exception e) {
        	logger.debug("Problem in processing incoming data from "+packet.getAddress()
        		+" length="+packet.getLength(), e);
        }
	}

	/*
	 * This function process the reply packets. It updates the RTT value.
	 */
	private void onReplyReceived(TopoMessage inMsg, String senderIP) {
        long rtt = TopoSender.getRTT(inMsg.sessionID,inMsg.BroadcastSeq);
		//logger.log(Level.ALL, "Reply received for link "+senderIP+"-"+inMsg.destinationIP+" RTT="+rtt);
		logger.trace(inMsg.messageType+" received for "+senderIP+"-"+inMsg.destinationIP +" RTT="+rtt);
		ekGraph.updateRTT(ownNode, inMsg.sender, inMsg.destinationIP, senderIP,	rtt );
 	}
	

	/*
	 * This function updates the links and the neighbors upon receiving a broadcast from a neighbor.
	 */
	private void onBroadcastReceived(TopoMessage inMsg, String senderIP) {
		
		if (! inMsg.sender.masterGUID.equals(ownNode.masterGUID)) {
			logger.trace(inMsg.messageType+" received from another edge. Sender IP="+senderIP+"Not updating ");
			return;
		}
		
		logger.trace(inMsg.messageType+" received from "+senderIP+" through "+inMsg.destinationIP);

		//First send a reply for calculating RTT
		TopoMessage outMessage = TopoMessage.newReplyMessage(inMsg.sessionID, inMsg.BroadcastSeq, senderIP);
		outMessage.sender=ownNode;
		TopoSender.sendMessage(outMessage, senderIP, this.serverSock);

		
		// FIrst update the link between the sender node and the current node
		ekGraph.updateEdge(ownNode, inMsg.sender, inMsg.destinationIP, senderIP, 
				null, inMsg.sessionID, inMsg.BroadcastSeq,  inMsg.sessionID, inMsg.BroadcastSeq, inMsg.thisLinkRcvProb);
		
		//Now update the two hop links
		if(inMsg.neighborStatus.keySet()!=null)
			for(TopoNode twoHop : inMsg.neighborStatus.keySet()) {
				if(!twoHop.equals(ownNode)) {
					NeighborStatus thStatus = inMsg.neighborStatus.get(twoHop);
					
					if(thStatus.nextHopGuid.equals(ownNode.guid)) {
						logger.trace("Discarding the link to "+twoHop.guid +" as it goes through the resident node");
						continue;
					}
					else
						ekGraph.updateEdge(inMsg.sender, twoHop, null, null,thStatus.etx, thStatus.lastKnownSession, 
								thStatus.lastKnownSeq, null, null, null);
				}
			}        
	}
	
	private void onNeighMessage(TopoMessage inMsg, String senderIP) {
		logger.trace(inMsg.messageType+" received from "+senderIP+" through "+inMsg.destinationIP);

		// Now send a reply in response to the neighbor
		TopoMessage outMessage = TopoMessage.neighborReply(inMsg.sessionID, inMsg.BroadcastSeq, senderIP);
		outMessage.sender=ownNode;
		TopoSender.sendMessage(outMessage, senderIP, this.serverSock);
		
		// First update the link between the sender node and the current node
		inMsg.sender.type = NodeType.EDGE_NEIBOR;
		inMsg.thisLinkRcvProb = ekGraph.getPrcvforNeighbor(inMsg.sender);
		ekGraph.updateEdge(ownNode, inMsg.sender, inMsg.destinationIP, senderIP, 
				null, inMsg.sessionID, inMsg.BroadcastSeq,  inMsg.sessionID, inMsg.BroadcastSeq,inMsg.thisLinkRcvProb);      
		
		//Now update the two hop links
		if(inMsg.neighborStatus.keySet()!=null)
			for(TopoNode twoHop : inMsg.neighborStatus.keySet()) {
				if(!twoHop.equals(ownNode)) {
					NeighborStatus thStatus = inMsg.neighborStatus.get(twoHop);
					
					if(thStatus.nextHopGuid.equals(ownNode.guid)) {
						logger.trace("Discarding the link to "+twoHop.guid +" as it goes through the resident node");
						continue;
					}
					else
						ekGraph.updateEdge(inMsg.sender, twoHop, null, null,thStatus.etx, thStatus.lastKnownSession, 
								thStatus.lastKnownSeq, null, null, null);
				}
			}        
	}

	private void onNeighbourReply(TopoMessage inMsg, String senderIP) {
        long rtt = TopoSender.getRTT(inMsg.sessionID,inMsg.BroadcastSeq);
		//logger.log(Level.ALL, "Reply received for Neighbor link "+senderIP+" - rtt="+rtt);
        logger.trace(inMsg.messageType+" received for "+senderIP+"-"+inMsg.destinationIP +" RTT="+rtt);
		ekGraph.updateRTT(ownNode, inMsg.sender, inMsg.destinationIP, senderIP,	rtt );
	}
}
