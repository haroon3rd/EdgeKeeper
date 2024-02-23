package edu.tamu.cse.lenss.edgeKeeper.server;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jgrapht.Graphs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.tamu.cse.lenss.edgeKeeper.topology.TopoGraph;
import edu.tamu.cse.lenss.edgeKeeper.topology.TopoLink;
import edu.tamu.cse.lenss.edgeKeeper.topology.TopoNode;
import edu.tamu.cse.lenss.edgeKeeper.topology.TopoNode.NodeType;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKProperties;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils.NetworkInterfaceType;

public class EdgeStatus {
	
	public static final Logger logger = LoggerFactory.getLogger(EdgeStatus.class);
	
	enum ReplicaStatus{
		FORMED,
		LOOKING,
	} 
	
	ReplicaStatus replicaStatus;
	
	public String masterGUID = null;
	public Set<String> masterIPs = null;
	public Map<String, String> replicaMap= new HashMap<>();
	
	synchronized void reinitialize(){
		this.replicaStatus=ReplicaStatus.LOOKING;
	}
	
	synchronized boolean manageReplica() {
		//Create a separate map
		Map<String, String> oldReplicaMap = new HashMap<String, String>();
		oldReplicaMap.putAll(this.replicaMap);
		
		try {
			if (!selfMaster()) {
				logger.error("EdgeStatus reset function is called. Current node is not acting as master.");
				return false;
			}

			/*
			 * Now in the edge status put own node identities as the edge master. 
			 * For GUID, fetch it from GNS. 
			 * For IPs, there are two option. If the properties says auto, then put all the IPs of 
			 * the current node. In some special case, master might use a particular IP. In that case, 
			 * edgeStatus contain those IPs of the master
			 * 
			*/
			this.masterGUID = EKHandler.getGNSClientHandler().getOwnGUID();
			String masters = EKHandler.getEKProperties().getProperty(EKProperties.ekMaster);
			if(masters.toLowerCase().equals("auto")) {
				this.masterIPs = EKUtils.getOwnIPv4Map().keySet();
			} else {
				this.masterIPs = new HashSet<>(Arrays.asList(masters.split(",")));
			}
			
			this.replicaStatus = ReplicaStatus.LOOKING;
			
			/*
			 * First copy the graph. this ensures that the graph is not modified by anyone
			 * else while thie function is executing
			 */
			TopoGraph graph = TopoGraph.getGraph( EKHandler.getTopoHandler().getGraph().getByteArray());
			int noReplica = EKHandler.getEKProperties().getInteger(EKProperties.noReplica);
			
			String suitableMasterIP = getSuitableMasterIP(graph);
			if(suitableMasterIP==null) {
				logger.error("The master node does not contain a suitable IP for replica. "
						+ "Can not build EdgeKeeper. Own IP map: "+graph.ownNode.ipMaps);
				return false;
			}
			
			
			this.replicaMap.put(graph.ownNode.guid, suitableMasterIP);
			
			
			
			/*
			 * Go through the current replica map and see if those nodes are still present in the topograph
			 * If they are not present in the topograph, delete the node from replicamap. 
			 * If the IP of the replica has changed, update the IP in the replica map. If no suitable Ip can be found for the 
			 * replica then remove node from replica map.  
			 */
			for(String guid:replicaMap.keySet()) {
				TopoNode node = graph.getVertexByGuid(guid);
				if(node == null) {
					logger.info("Replica "+guid+" left the network. Deleting it from replica map");
					this.replicaMap.remove(guid);
				} 
				else {
					if(! node.ipMaps.containsKey(this.replicaMap.get(guid))){
						logger.info("IP address for replica "+ guid+ " changed.");
						
						String suitableIP = this.getSuitableReplicaIP(graph, node, suitableMasterIP);
						if(suitableIP==null || suitableIP.isEmpty()) {
							EKUtils.logger.debug("Replica "+node.guid+" doesn't have a valid IP any more. Removing this replica");
							this.replicaMap.remove(node.guid);
						}
						else {
							this.replicaMap.put(guid, suitableIP);
							logger.debug(" For replica "+guid+" associating new IP "+suitableIP);
						}
					}
					else {
						logger.trace("IP address for replica "+ guid+ " remained same");
					}
				}
			}
			
			/*
			 * Now, get a list of client nodes from the topograph and check 
			*/
			Iterator<TopoNode> clientIterator = graph.getVertexbyType(NodeType.EDGE_CLIENT).iterator();
			while(this.replicaMap.size()<noReplica && clientIterator.hasNext()) {
				TopoNode client = clientIterator.next();
				
				if(! this.replicaMap.keySet().contains(client.guid)) {
					String suitableIP = this.getSuitableReplicaIP(graph,client, suitableMasterIP);
					if(suitableIP != null && !suitableIP.isEmpty()) {
						this.replicaMap.put(client.guid, suitableIP);
						EKUtils.logger.info("Adding new replica "+client.guid +" for ip "+suitableIP);
					}
					else {
						logger.debug("Node"+client.guid +" does not have suitable IP ");
					}
				}
			}
			
			logger.trace( "replica map size="+replicaMap.size()+" no of replica needed ="+noReplica);
			if(replicaMap.size() >= (noReplica+1)/2) {
				this.replicaStatus = ReplicaStatus.FORMED;
			}
			else {
				this.replicaStatus = ReplicaStatus.LOOKING;
				EKUtils.logger.debug("Enough nodes not found to be replicas. EdgeKeeper replicas not formed");
			}
			
		} catch(Exception e) {
			EKUtils.logger.error("Exception occured resetting edgeStatus ", e);
		} 
		
		if(oldReplicaMap.equals(this.replicaMap)) {
			logger.trace("The replica status remained same after reset");
			return false;
		}
		else {
			logger.trace("The replica status remained same after reset");
			return true;
		}
	}

//	String getSuitableIP(TopoNode node) {
//		if(node.ipMaps==null || node.ipMaps.isEmpty()) {
//			logger.debug("IPs for node is empty. "+node.guid);
//			return null;
//		}
//		for(String ip: node.ipMaps.keySet()) {
//			if(node.ipMaps.get(ip)==NetworkInterfaceType.ETHERNET 
//					|| node.ipMaps.get(ip)==NetworkInterfaceType.WIFI) {
//				return ip;
//			}
//		}
//		return null;
//	}

	
	Set<String> getSuitableIPSet(TopoNode node) {
		Set<String> ipSet = new HashSet<>();
		if(node.ipMaps==null || node.ipMaps.isEmpty()) {
			logger.debug("IPs for node is empty. "+node.guid);
		}
		else {
			for(String ip: node.ipMaps.keySet()) {
//				if(node.ipMaps.get(ip)==NetworkInterfaceType.ETHERNET 
//						|| node.ipMaps.get(ip)==NetworkInterfaceType.WIFI) {
				//if(node.ipMaps.get(ip)!=NetworkInterfaceType.MOBILE) {
					ipSet.add(ip);
				//}
			}
		}
		return ipSet;
	}
	
	private String getSuitableMasterIP(TopoGraph graph) {
		TopoNode ownNode = graph.ownNode;
		if(ownNode.ipMaps==null || ownNode.ipMaps.isEmpty()) 
			return null;
		List<TopoNode> neighborList = Graphs.neighborListOf(graph, ownNode);
		int maxConnectivity = Integer.MIN_VALUE;
		String ipMaxConnected = null;
		
		for(String ip: getSuitableIPSet(ownNode)) {
			int ipConnected = 0;
			if(neighborList!=null && !neighborList.isEmpty()) {
				for(TopoNode v:neighborList) {
					if(v.type==NodeType.EDGE_CLIENT)
						for(TopoLink e:graph.getAllEdges(ownNode, v)) {
							if(e.ipPair!=null && !e.ipPair.isEmpty() && e.ipPair.contains(ip))
								ipConnected ++;
						}
				}
			}
			logger.debug("IPs "+ip+" connected with "+ipConnected +" clients");
			if(ipConnected>maxConnectivity) {
				maxConnectivity=ipConnected;
				ipMaxConnected = ip;
			}
		}
		return ipMaxConnected;
	}
	
	private String getSuitableReplicaIP(TopoGraph g, TopoNode v, String masterIp) {
		for(TopoLink e: g.getAllEdges(g.ownNode, v)) {
			if(e.ipPair!=null && !e.ipPair.isEmpty() && e.ipPair.contains(masterIp)) {
				for(String ip: e.ipPair)
					if(!ip.equals(masterIp) && getSuitableIPSet(v).contains(ip))
						return ip;
			}
		}
		return null;
	}

	public static boolean selfMaster() {
		return EKHandler.getEKProperties().getBoolean(EKProperties.enableMaster);
	}
	
	String toJSONString() {
		Gson gson = new GsonBuilder()
				.serializeNulls()
				.serializeSpecialFloatingPointValues() 
				.create();
		
		return gson.toJson(this);
		//return  new Gson().toJson(this);
	}
	
	public static EdgeStatus getStatus(String jsonStr) {
		Gson gson = new GsonBuilder()
				.serializeNulls()
				.serializeSpecialFloatingPointValues() 
				.create();

		return gson.fromJson(jsonStr, EdgeStatus.class);
		//return new Gson().fromJson(jsonStr, EdgeStatus.class);
	}
	
	public Set<String> getMasterIps() {
		return masterIPs;
	}
	
	public String getMasterGUID() {
		return masterGUID;
	}
	
	public String getZKServerString() {
		String cPort = EKHandler.getEKProperties().getProperty(EKProperties.clientPort); 
		String zkServerString="";
        for (String ip: this.replicaMap.values()){
        	zkServerString += ip+":"+cPort+",";
        }
        return zkServerString;
	}
	
}
