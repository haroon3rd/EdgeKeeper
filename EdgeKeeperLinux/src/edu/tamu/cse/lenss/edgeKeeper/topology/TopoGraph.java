package edu.tamu.cse.lenss.edgeKeeper.topology;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.KShortestPathAlgorithm;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm.SingleSourcePaths;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.alg.shortestpath.KShortestSimplePaths;
import org.jgrapht.graph.WeightedMultigraph;
import org.jgrapht.io.CSVExporter;
import org.jgrapht.io.ExportException;
import org.jgrapht.io.GraphExporter;


import edu.tamu.cse.lenss.edgeKeeper.server.EKHandler;
import edu.tamu.cse.lenss.edgeKeeper.topology.TopoNode.NodeType;
import edu.tamu.cse.lenss.edgeKeeper.utils.Base64;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKProperties;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils.NetworkInterfaceType;


/**
 * This graph is maintained for the EdgeKeeper. 
 * @author sbhunia
 *
 */
public class TopoGraph extends WeightedMultigraph<TopoNode, TopoLink> implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -9031005986884363790L;
	
	public TopoNode ownNode;  // Points out the current or resident node
	
	/*
	 * Default constructor
	 */
	public TopoGraph(Class<? extends TopoLink> edgeClass, TopoNode ownNode) {
		super(edgeClass);
		this.ownNode=ownNode;
		this.addVertex(ownNode);
	}
	
	/*
	 * It obtains the prcv for an immidiate neighbor node. If no link is established before, TOPO_INITIAL_PROB is 
	 * returned. If multiple link exist with the neighbor, the avarage value of all the links are returned.
	 */
	public double getPrcvforNeighbor(TopoNode v){
		Set<TopoLink> allEdges = this.getAllEdges(ownNode, v);
		if(allEdges==null || allEdges.isEmpty())
			return EKConstants.TOPO_INITIAL_PROB;
		//Get average receive probability
		double sum = 0.0;
		for(TopoLink e: allEdges) {
			sum+=e.prcv;
		}
		return sum/allEdges.size();
	}
	
	
	/**
	 * Returns the vertix corresponding to a GUID
	 * @param guid
	 * @return 
	 */
	public TopoNode getVertexByGuid(String guid){
		if(this.vertexSet()!=null)
			for(TopoNode v: this.vertexSet())
				if(v.guid.equals(guid))
					return v;
		return null;
	}
	
	public Set<String> getAllIPs() {
		Set<String> ips = new HashSet<String>();
		if(this.vertexSet()!=null)
			for(TopoNode v: this.vertexSet())
				if(v.ipMaps.keySet() !=null)
					ips.addAll(v.ipMaps.keySet());
		return ips;
	}
	
	public Set<TopoNode> getVertexbyType(NodeType nodeType){
		Set<TopoNode> vSet = new HashSet<>();
		for(TopoNode v:this.vertexSet())
			if(v.type==nodeType) {
				vSet.add(v);
			}
		return vSet;
	}
	
//	public Set<String> getAllIPs() {
//		Set<String> ips = new HashSet<String>();
//		if(this.vertexSet()!=null)
//			for(TopoNode v: this.vertexSet())
//				if(v.ips !=null)
//					ips.addAll(v.ips);
//		return ips;
//	}
	
	/**
	 * Obtain the Vertex/Node for a particular IP. 
	 * @param ip
	 * @return TopoNode which contains the IP
	 */
	public TopoNode getNodebyIP(String ip) {
		if (this.vertexSet() == null || this.vertexSet().isEmpty())
			return null;
		for(TopoNode v : this.vertexSet()) {
			if(v.ipMaps.keySet()!=null && v.ipMaps.keySet().contains(ip))
				return v;
		}
		return null;
	}

//	/**
//	 * This function returns the map of vertex to EdgeWeight. EdgeWeight is the ETX (check OLSR for details)
//	 * @return
//	 */
//	public Map<TopoNode, Double> getETXMap(){
//		Map<TopoNode, Double> etxMap = new HashMap<TopoNode, Double>();
//		SingleSourcePaths shortestPaths = new DijkstraShortestPath(this).getPaths(ownNode);
//		for (TopoNode v : vertexSet()) {
//			if (!v.equals(ownNode)) {
//				Double pathWeight = shortestPaths.getWeight(v);
//				pathWeight =  pathWeight.isInfinite()? Double.MAX_VALUE :pathWeight;
//				etxMap.put(v, pathWeight);
//			}
//		}
//		return etxMap;
//	}
//	
	
	/**
	 * This function returns the map of vertex to EdgeWeight. EdgeWeight is the ETX (check OLSR for details)
	 * @return
	 */
	public Map<TopoNode, NeighborStatus> getNeighborMap(){
		Map<TopoNode, NeighborStatus> neighborMap = new HashMap<TopoNode, NeighborStatus>();
		SingleSourcePaths<TopoNode, TopoLink> shortestPaths = new DijkstraShortestPath(this).getPaths(ownNode);
		for (TopoNode v : vertexSet()) {
			//if (!v.equals(ownNode)) {
			//if (!v.equals(ownNode) && (v.type!=NodeType.EDGE_NEIBOR)) {
			if (!v.equals(ownNode) && !(v.type==NodeType.EDGE_NEIBOR && ownNode.type ==NodeType.EDGE_CLIENT)) {
				NeighborStatus ns = new NeighborStatus();
				ns.lastKnownSeq=v.lastSeqRcvd;
				ns.lastKnownSession = v.lastSessionID;
				
				ns.etx = shortestPaths.getWeight(v);
				if(!ns.etx.isInfinite())
					neighborMap.put(v, ns);
				
				try {
				ns.nextHopGuid=shortestPaths.getPath(v).getVertexList().get(1).guid;
				//TopoHandler.logger.log(Level.ALL,"Next hop for "+v.guid +" is "+ ns.nextHopGuid);
				}catch(Exception e) {
					TopoHandler.logger.trace("Exception in retrieving next hop for"+v.guid, e);
				}
				v.expectedSeq++; // THis will make sure of some expire
			}
			/*
			 * For master, the expectedSeq is updated in the above clause, for EDGE_CLIENT,
			 * the neighbor sequence has to be updated separately.
			 */
			else if (!v.equals(ownNode) && (v.type==NodeType.EDGE_NEIBOR)) {
				v.expectedSeq++; // THis will make sure of neighbor expire
			}
		}
		return neighborMap;
	}

	
	
	/**
	 * Obtains the cost of the shortest path from the resident node to the vertex v
	 * @param v
	 * @return
	 */
	public Double getDistance(TopoNode v) {
		SingleSourcePaths shortestPaths = new DijkstraShortestPath(this).getPaths(ownNode);
		Double pathWeight = shortestPaths.getWeight(v);
		return pathWeight.isInfinite()? Double.MAX_VALUE :pathWeight;
	}
	
	/**
	 * This function updates an edge ETX for a particular link 
	 * @param start the start node for this link
	 * @param dest the destination node for this link 
	 * @param startIP IP of start node for this link
	 * @param destIP IP of the destination node for this link 
	 * @param etx The ETX value (If the start node is resident node then ETX is calculated in this function)
	 * @param sessionID 
	 * @param seqNo
	 * @param Senderprcv Sender's advertised Prcv for this link
	 */
	public void updateEdge(TopoNode start, TopoNode dest, String startIP, String destIP, 
			Double etx, String destSession, Integer destSeq, String sessionID, Integer seqNo, Double Senderprcv) {

		this.addVertex(dest);
		
		TopoNode v = this.getVertexByGuid(dest.guid);
		
		if(v==null) {
			TopoHandler.logger.error("Problem in updating the destination vertex IP");
			return;
		}
		
		if( v.checkStaleness(destSession, destSeq)) {
			TopoHandler.logger.trace( "Stale update for "+dest.guid+" disguarding the update. "+destSession+" "+destSeq );
			return;
		}
		
		//First copy the masterGUID of the particular node.
		v.masterGUID = dest.masterGUID;
		v.type=dest.type;
		
		//Now set the IPS. sometimes the IPs change
		if(dest.ipMaps.keySet()!=null) 
			v.ipMaps=dest.ipMaps;

		//For two hop links, the IP address of the interfaces are unknown.
		if(startIP !=null && destIP != null)
			if(startIP.equals(EKConstants.TOPO_BROADCAST_IP) || destIP.equals(EKConstants.TOPO_BROADCAST_IP)) {
				TopoHandler.logger.trace("This message was received at"+EKConstants.TOPO_BROADCAST_IP+". Not addting the link to the graph");
				return;
			}
		
		Set<String> newipPair = new HashSet<String>();
        newipPair.add(startIP);
        newipPair.add(destIP);
		
		//Now deal with the edge
		Set<TopoLink> edgeList = this.getAllEdges(start, dest);
        //Now check if this edge already exist or not
        TopoLink edgeMatch = null;
        if(! edgeList.isEmpty())
        	for (TopoLink edge:edgeList) {
        		if(edge.ipPair.equals(newipPair)) {
	        		edgeMatch=edge;
	        		break;
        		}
        	}

		if (edgeMatch == null) {
			edgeMatch = this.addEdge(start, dest);
			edgeMatch.ipPair = newipPair;
		}
        
		if(start.equals(ownNode)) {
			edgeMatch.updatePrcv(sessionID, seqNo);
			etx = 1.0 / (edgeMatch.prcv * Senderprcv);
			if(etx.isInfinite()) {
				TopoHandler.logger.debug("ETX is infinite. Pushing it to lower value");
				etx = Double.MAX_VALUE;
			}
		}
		//ETX is the link weight
		this.setEdgeWeight(edgeMatch, etx);
		edgeMatch.lastUpdateTime = System.currentTimeMillis();
 	}
	
	public synchronized void updateRTT(TopoNode start, TopoNode dest, String startIP, String destIP,	Long rtt ) {
		Set<String> newipPair = new HashSet<String>();
        newipPair.add(startIP);
        newipPair.add(destIP);
		
		Set<TopoLink> edgeList = this.getAllEdges(start, dest);
		if(edgeList!=null && !edgeList.isEmpty())
        	for (TopoLink edge:edgeList) {
        		if(edge.ipPair.equals(newipPair)) {
        			edge.updateRTT(rtt);
	        		break;
        		}
        	}
	}

	/**
	 * This function deteriorate the links if it do not receive broadcast for this link
	 */
	public synchronized void cleanup() {
		
		int cleanupItr = EKHandler.getEKProperties().getInteger(EKProperties.topoCleanupIteration);
		try {
			/*
			 * To avoid ConcurrentModificationException, first create a list of links to be deleted and then delete them
			*/
			Set<TopoLink> tobeDeletedLink = new HashSet<>();
			if(this.edgeSet() !=null)
				for(TopoLink e : this.edgeSet()) {
					if( System.currentTimeMillis() > e.lastUpdateTime + cleanupItr*EKHandler.getEKProperties().getTopoInterval()) {
						tobeDeletedLink.add(e);
					}
					
					if( (this.getEdgeSource(e).equals(this.ownNode) || this.getEdgeTarget(e).equals(this.ownNode))
							&&(System.currentTimeMillis() > e.lastUpdateTime + EKHandler.getEKProperties().getTopoInterval() )) {
						this.setEdgeWeight(e, e.updateStaleLink());
					}
				}
			for(TopoLink e: tobeDeletedLink) {
				this.removeEdge(e);
				TopoHandler.logger.trace("Removed Edge: " + e);
			}
			
			Set<TopoNode> tobeDeletedNode = new HashSet<>();
			
			if(this.vertexSet()!=null) 
				for(TopoNode v: this.vertexSet()) 
					if(v.expectedSeq> cleanupItr+v.lastSeqRcvd) 
						tobeDeletedNode.add(v);
			
			for(TopoNode v : tobeDeletedNode) {
				this.removeVertex(v);
				TopoHandler.logger.debug("Deleting node: "+v.toString());
			}
		} catch(Exception e ) {
			TopoHandler.logger.error("Problem in edge Cleanup", e);
		}
		
	}
	
	/**
	 * 
	 * @return byte array of the object
	 */
	public synchronized byte[] getByteArray(){
		synchronized (this) {
			byte[] data = null;
			try {
			    ByteArrayOutputStream bos = new ByteArrayOutputStream();
			    ObjectOutputStream oos = null;
			    oos = new ObjectOutputStream(bos);
			    oos.writeObject(this);
			    oos.flush();
			    byte[] b = bos.toByteArray();
			    //TopoHandler.logger.log(Level.ALL, "Exported graph hash: "+EKUtils.sha256(b));
			    return b;
			}catch (Exception e) {
			    TopoHandler.logger.warn("Could not serialize the object",  e);
			    return null;
			}
		}
	}
	
	/**
	 * This method is created such that the serialized byte array can be included in JSONObject
	 * @return
	 */
	public String getString() {
		return Base64.encodeToString(getByteArray(), Base64.NO_WRAP);
	}
	
	/**
	 * Generate an object of this class from the serialized byte array
	 * @param recvdData
	 * @return
	 */
	public static TopoGraph getGraph(byte[] recvdData){
		//TopoHandler.logger.log(Level.ALL, "Trying to build graph from byte array with hash: "+EKUtils.sha256(recvdData));
		TopoGraph classOBj = null;
		try {
		    //create class object from raw byteArray
		    ByteArrayInputStream bis = new ByteArrayInputStream(recvdData);
		    ObjectInputStream ois = new ObjectInputStream(bis);
		    classOBj = (TopoGraph) ois.readObject();
		    bis.close();
		    ois.close();
		}catch(Exception e){
			TopoHandler.logger.error("Problem in clonstructing class ", e);
		}
		return classOBj;
	}
	
	/**
	 * It obtains the byte array from the String and then build an object from the serialized byte array
	 * @param str
	 * @return
	 */
	public static TopoGraph getGraph(String str) {
		byte[] b = Base64.decode(str, Base64.NO_WRAP); 
		return getGraph(b);
	}
	

	
	public synchronized String printToString() {
		String s = "================Topology==============";
		for(TopoNode v:this.vertexSet()) {
			s+="\nNode = " + v.guid + " | " + v.type+" | " + v.ipMaps;
		}
		for(TopoLink e:this.edgeSet()) {
			s+="\nLink = " +this.getEdgeSource(e).guid+" - "+ this.getEdgeTarget(e).guid+", "
					+e.ipPair + ", "+ e.rtt+", "+e.getEtx();
		}
		return s;
	}
	
	public void printToFile() {
		
		SimpleDateFormat formatter= new SimpleDateFormat("E yyyy.MM.dd 'at' hh:mm:ss a zzz");
		Date date = new Date(System.currentTimeMillis());
		String data = formatter.format(date) + "\n" + printToString();
		String dir = EKHandler.getEKProperties().getProperty(EKProperties.dataDir);
		BufferedWriter out = null;
		try {
	         out = new BufferedWriter(new FileWriter(dir+File.separatorChar+"EKgraph"));
	         out.write(data);
	      } catch (Exception e) {
	    	  TopoHandler.logger.warn("Error writing graph data to file",e);
	      }finally {
	    	  try {
	    		  out.close();  
	    	  }catch(Exception e) {
	    		  TopoHandler.logger.warn("Error closing the bufferreader.",e);
	    	  }
	    	  
	      }
	}

	
	public static TopoGraph dummyGraph() throws IOException {
		 
		 
//	    DefaultDirectedGraph<String, DefaultEdge> g = 
//	      new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);
//	 
		TopoNode x1 = new TopoNode("xsdjskdjklsjdakdj1", NodeType.EDGE_CLIENT);
	    
		TopoGraph g = new TopoGraph(TopoLink.class, x1);
	    
	    
	    TopoNode x2 = new TopoNode("xsdasdadasd2", NodeType.EDGE_CLIENT);
//	    TopoNode x3 = new TopoNode("xsadasdadasda3", NodeType.EDGE_CLIENT);
//	    TopoNode x4 = new TopoNode("x4asdasdasdasdasd", NodeType.EDGE_CLIENT);
//	    TopoNode x5 = new TopoNode("x5asdasdasd", NodeType.EDGE_CLIENT);
//	    
//	    
	    
	 
	    g.addVertex(x1);
	    g.addVertex(x2);
//	    g.addVertex(x3);
//	    g.addVertex(x4);
//	    g.addVertex(x5);
//	 
	    TopoLink e1 = g.addEdge(x1, x2);
	    g.setEdgeWeight(e1, 1);
//	    g.addEdge(x2, x3);
//	    g.addEdge(x3, x1);
	    TopoLink e34 = g.addEdge(x1, x2);
	    g.setEdgeWeight(e34, 1.2);

//	    TopoLink e341 = g.addEdge(x3, x4);
//	    g.setEdgeWeight(e341, 1.0);

	    
	    //g.addEdge(x1, x5);
	    //g.addEdge(x3, x4, new DefaultEdge());
	    //g.addEdge(x1, x5);
	    return g;
	}

	
	
//	
//	byte[] exportToBytes() throws ExportException{
//		GraphExporter<TopoNode, TopoLink> exporter = new CSVExporter<TopoNode, TopoLink>();
//		
//		ByteArrayOutputStream out = new ByteArrayOutputStream();
//		
//		exporter.exportGraph(this, out);
//		
//		return out.toByteArray();
//	}
//
//	void importGraph(){
//		GraphExporter<TopoNode, TopoLink> exporter = new CSVExporter<TopoNode, TopoLink>();
//		
//		
//		
//		
//		//GraphImporter<TopoNode, TopoLink> = new CSVIm
//	}
	
	public static void main(String[] args) throws IOException {
		
		Logger logger = LoggerFactory.getLogger(TopoGraph.class);
		
		
//		EKUtils.initLogger("logs/test.log", Level.ALL);
		logger.debug("Something");

		
		TopoGraph g = dummyGraph();
		logger.debug(g.printToString());
		
		//TopoUtils.toPng(g);
		
		
		Iterator<TopoNode> vertexes = g.vertexSet().iterator();
		TopoNode x1 = vertexes.next();
		
		TopoNode x2 = vertexes.next();
		
		
		 KShortestSimplePaths shortest = new KShortestSimplePaths(g, 1);
		 
		 List<GraphPath<TopoNode,TopoLink>> paths = shortest.getPaths(x1, x2, 2);
		 
		 GraphPath<TopoNode, TopoLink> path = paths.get(0);
		 
		// path.ge
		 
		
		logger.debug(paths.toString());
		
		
		//TopoUtils.toPng(dummyGraph());
		
		//System.out.println(dummyGraph().exportGraph());
		
//		String s = dummyGraph().getJSONString();
//		
//		System.out.println(s);
		
		
//		byte[] barr = dummyGraph().getByteArray();
//		
//		String s= Base64.getEncoder().encodeToString(barr);
//		
//		byte[] b = Base64.getDecoder().decode(s); 
//		
//		System.out.println(getGraph(b));
		
	}
	



}
