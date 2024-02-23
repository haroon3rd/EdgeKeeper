package edu.tamu.cse.lenss.edgeKeeper.topology;

import java.io.Serializable;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils.NetworkInterfaceType;

public class TopoNode implements Serializable{
	public enum NodeType {
		CLOUD__NODE, 
		EDGE_CLIENT,
		EDGE_MASTER,
		EDGE_NEIBOR
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1460349546705428202L;
	
	public NodeType type;
	public String guid = null;
	public String masterGUID = null;
	//public List<String> ips;
	public Map<String, NetworkInterfaceType> ipMaps = null;
	
	String lastSessionID;
	int lastSeqRcvd; //this variable will be used by the neighbors to keep track of this node's last seq
	int expectedSeq; //this variable will be used by the neighbors to keep track of this node's last seq
	
	public TopoNode(String guid, NodeType nodeType) {
		super();
		this.guid = guid;
		this.type = nodeType;
	}

	public String toString()
    {
        return guid+"\n"+type+", "+ipMaps.keySet()+", "+this.lastSeqRcvd+"/"+this.expectedSeq;
    }
    
    public int hashCode()
    {
        return guid.hashCode();
    }
    
    public boolean equals(Object o)
    {
        return (o instanceof TopoNode) && (hashCode() == o.hashCode());
    }
    
    boolean checkStaleness(String destSession, Integer destSeq){
    	if (this.lastSessionID == null || !destSession.equals(this.lastSessionID)) {
    		TopoHandler.logger.trace( "New session for "+this.guid+" " +destSession+ " "+ destSeq);
			this.lastSessionID = destSession;
			this.lastSeqRcvd = destSeq;
			this.expectedSeq = destSeq;
		}
    	else {
    		this.lastSeqRcvd = Math.max(this.lastSeqRcvd, destSeq);
    		this.expectedSeq = Math.max(this.lastSeqRcvd, this.expectedSeq);
    	}

    	if(this.expectedSeq > this.lastSeqRcvd+3)
    		return true;
    	else
    		return false;
    	
    }

}
