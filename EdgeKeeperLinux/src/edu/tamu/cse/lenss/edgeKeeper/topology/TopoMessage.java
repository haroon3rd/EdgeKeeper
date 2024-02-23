package edu.tamu.cse.lenss.edgeKeeper.topology;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.tamu.cse.lenss.edgeKeeper.utils.Base64;

public class TopoMessage implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5591979432520885024L;
//	public static final Logger logger = LoggerFactory.getLogger(TopoMessage.class);
//	
	enum MessageType {
		TOPO_BROADCAST, 
		TOPO_REPLY,
		TOPO_CLOUD,
		TOPO_NEIGHBOR_MSG,
		TOPO_NEIGHBOR_REPLY

	}
	public TopoNode sender;
	public Map<TopoNode, NeighborStatus> neighborStatus;

	double thisLinkRcvProb; //stores etx of a vertex;
	public String destinationIP;
	public MessageType messageType;
	public int BroadcastSeq;
	public String sessionID;

	private TopoMessage() {
	}


	public static TopoMessage newBroadcastMessage(String sessionID, int broadcastSeq) {
		TopoMessage msg = new TopoMessage();
		msg.sessionID = sessionID;
		msg.messageType = MessageType.TOPO_BROADCAST;
		msg.BroadcastSeq = broadcastSeq;
		return msg;
	}

	public static TopoMessage newReplyMessage(String sessionID, int seq, String destIP) {
		TopoMessage msg = new TopoMessage();
		msg.sessionID = sessionID;
		msg.messageType = MessageType.TOPO_REPLY;
		msg.BroadcastSeq = seq;
		msg.destinationIP = destIP;
		return msg;
	}
	
	public static TopoMessage newCloudMsg(String sessionID, int msgSeq) {
		TopoMessage msg = new TopoMessage();
		msg.sessionID = sessionID;
		msg.messageType = MessageType.TOPO_CLOUD;
		msg.BroadcastSeq = msgSeq;
		return msg;
	}
	
	public static TopoMessage newNeighborMessage(String sessionID, int neighborSeq) {
		TopoMessage msg = new TopoMessage();
		msg.sessionID = sessionID;
		msg.messageType = MessageType.TOPO_NEIGHBOR_MSG;
		msg.BroadcastSeq = neighborSeq;
		return msg;
	}
	
	public static TopoMessage neighborReply(String sessionID, int seq, String destIP) {
		TopoMessage msg = new TopoMessage();
		msg.sessionID = sessionID;
		msg.messageType = MessageType.TOPO_NEIGHBOR_REPLY;
		msg.BroadcastSeq = seq;
		msg.destinationIP = destIP;
		return msg;
	}


	/**
	 * This function serialize the object and obtains its corresponding byte array
	 * @return byte array
	 */
	byte[] getByteArray() {
		byte[] data = null;
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = null;
			oos = new ObjectOutputStream(bos);
			oos.writeObject(this);
			oos.flush();
			return bos.toByteArray();
		} catch (Exception e) {
			TopoHandler.logger.warn("Could not serialize the object", e);
			return null;
		}
	}

	/**
	 * This function constructs an object corresponding to a byte array that was obtained 
	 * by serializing another object of same class.
	 * @param bytes
	 * @return 
	 */
	public static TopoMessage getMessage(byte[] bytes) {

		TopoMessage classOBj = null;
		try {
			// create class object from raw byteArray
			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
			ObjectInputStream ois = new ObjectInputStream(bis);
			classOBj = (TopoMessage) ois.readObject();
			bis.close();
			ois.close();
		} catch (Exception e) {
			TopoHandler.logger.error("Could not construct "+TopoMessage.class.getSimpleName() +" from the byte array", e);
		}
		return classOBj;
	}
	
	/**
	 * This method is created such that the serialized byte array can be included in JSONObject
	 * @return
	 */
	
	public String getRawString() {
		return Base64.encodeToString(getByteArray(), Base64.NO_WRAP);
	}
	
	public static TopoMessage getMessage(String str) {
		return getMessage( Base64.decode(str, Base64.NO_WRAP)); 
	}


	String printBroadcastDigest() {
		return "Sender ="+sender.guid+", Neighbors: "+neighborStatus.keySet();
	}
}

class NeighborStatus implements Serializable{
	private static final long serialVersionUID = -8035605642039911301L;
	public Double etx;
	public String nextHopGuid;
	public String lastKnownSession;
	public int lastKnownSeq;
}
