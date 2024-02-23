package edu.tamu.cse.lenss.edgeKeeper.server;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
//import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MetaDataHandler;
import edu.tamu.cse.lenss.edgeKeeper.server.EdgeStatus.ReplicaStatus;
import edu.tamu.cse.lenss.edgeKeeper.topology.TopoGraph;
import edu.tamu.cse.lenss.edgeKeeper.topology.TopoNode;
import edu.tamu.cse.lenss.edgeKeeper.topology.TopoNode.NodeType;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKProperties;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKRecord;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils;
import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;

public class CoordinatorClient implements Terminable{
//    static final Logger logger = LoggerFactory.getLogger(CoordinatorClient.class);
	static final Logger logger = LoggerFactory.getLogger(CoordinatorClient.class);
	boolean isTerminated;
    
	private long GUID_MERGE_INTERVAL;
	private long MDFS_MERGE_INTERVAL;
	
	public CoordinatorClient() {
		GUID_MERGE_INTERVAL= Math.max( EKHandler.getEKProperties().getInteger(EKProperties.mergerGuidInterval), 
				EKConstants.GUID_MERGE_INTERVAL);
		MDFS_MERGE_INTERVAL= Math.max( EKHandler.getEKProperties().getInteger(EKProperties.mergerMdfsInterval),
				EKConstants.MDFS_MERGE_INTERVAL);
	}

	@Override
	public void run() {
		isTerminated = false;
		while(!isTerminated) {
			
			try {
				if (EKHandler.getZKClientHandler().isConnected() && !EdgeStatus.selfMaster()) {
					logger.trace( "Slave is connected with the master. Not changing any configuration");
				}
				else {
					boolean isStatusChanged = true;
					logger.trace( "Trying to reconfigure the EdgeStatus");
					while(!isTerminated) {
						if(EdgeStatus.selfMaster()) {
							isStatusChanged = EKHandler.edgeStatus.manageReplica();
							logger.debug("After resetting the edge Status as master, the edge Status is "
									+EKHandler.edgeStatus.toJSONString());
							
							if(isStatusChanged)
								notifyReplicas();
						}
						else {
							logger.debug("Trying to obtin configuration from master");
							EKHandler.edgeStatus.reinitialize();
							fetchEdgeMasterInfo();
						}
						if(EKHandler.edgeStatus.replicaStatus == ReplicaStatus.FORMED)
							break;
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
							logger.debug("problem in sleep");
						}
					}
					
					logger.debug(" Valid replica reached "+EKHandler.edgeStatus.toJSONString());
					
					if(!isTerminated && isStatusChanged) {
						EKHandler.ekRecord.updateField(EKRecord.MASTER_GUID, EKHandler.edgeStatus.masterGUID);
						logger.trace( "Trying to restart ZK server");
						EKHandler.getZKServerHandler().restart();
						logger.trace( "Restarted ZK server");
						
						logger.trace( "Trying to restart ZK client");
						EKHandler.getZKClientHandler().restartCurator(EKHandler.edgeStatus.getZKServerString());
						logger.trace( "Restarted ZK client");
					}
					if (!isTerminated && !EKHandler.getZKClientHandler().isConnected()) {
						logger.trace( "Trying to restart ZK client");
						EKHandler.getZKClientHandler().restartCurator(EKHandler.edgeStatus.getZKServerString());
						logger.trace( "Restarted ZK client");
					}
				
				}
	
				/*
				 * If this node is the Edge-master, then try to merge information with neighbors.
				*/
				if(EKHandler.edgeStatus.selfMaster()) {
					tryEdgeMerge();
				}
				
				
			}catch(Exception e) {
				logger.error("Exception in Coordination client ", e);
			}
			
			
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				logger.debug("problem in sleep");
			}
		}
	}

    private void notifyReplicas() {
    	logger.trace( "Trying to notify Replicas");
    	JSONObject reqJSON = null;
    	try {
			reqJSON = new JSONObject();
			reqJSON.put(CoordinatorServer.requestField, CoordinatorServer.REPLICA_NOTIFICATION);
			reqJSON.put(CoordinatorServer.edgeStatusField, EKHandler.edgeStatus.toJSONString());
		} catch(Exception e) {
			logger.error("Problem in creating request json for Notifying replicas", e);
			return;
		}
    	
    	if(reqJSON == null)
    		return;

		class NotifyThread extends Thread{
			private String host;
			private JSONObject req;
			public NotifyThread(String host, JSONObject req) {
				this.host=host;
				this.req=req;
			}
			public void run(){
				try {
					JSONObject repJSON = communicate(host, req);
					logger.trace( "Notified edge status to replica "+host);
				} catch (Exception e) {
					logger.debug("Problem in Notifying replica ",e);
				}
			}
		}

		for(String ip: EKHandler.edgeStatus.replicaMap.values()) {
			if(! EKUtils.getOwnIPv4Map().keySet().contains(ip)) 
				new NotifyThread(ip, reqJSON).start();
		}
    }

	void fetchEdgeMasterInfo() {
		Set<String> dests = new HashSet<String>();
		
		String masters = EKHandler.getEKProperties().getProperty(EKProperties.ekMaster);
		if(masters.toLowerCase().equals("auto")) {
	    	dests.add(EKConstants.MASTER_NAME);
	    	dests.addAll(EKHandler.getEKUtils().getDefaultGateway());
		} else {
			dests.addAll(Arrays.asList(masters.split(",")));
		}
		logger.info("Trying to fetch master info from "+dests.toString());
    	
    	for(String hostName: dests) {
    		try {
        		JSONObject reqJSON = new JSONObject();
				reqJSON.put(CoordinatorServer.requestField, CoordinatorServer.edgeStatusField);
				JSONObject repJSON = communicate(hostName, reqJSON);
				EdgeStatus eStatus = EdgeStatus.getStatus( repJSON.getString(CoordinatorServer.edgeStatusField) );
				EKHandler.edgeStatus = eStatus;
				logger.info("Fetched Status from master = "+eStatus.toJSONString());
				return;
			} catch (Exception e) {
				logger.debug("Communication with "+ hostName +" failed. Not updating the Edge Status");
			}
    	}
    }
    
    public JSONObject communicate(String hostName, JSONObject reqJSON) throws Exception {
    	Socket cSocket  = null;
    	DataInputStream in = null;
    	DataOutputStream out = null;
    	try {
            cSocket = new Socket();
            cSocket.connect(new InetSocketAddress(hostName, EKConstants.COORDINATOR_PORT), EKConstants.ClientConnectTimeout);
            cSocket.setSoTimeout(EKConstants.ClientSoTimeout);
            in = new DataInputStream (new BufferedInputStream(cSocket.getInputStream()));
            out = new DataOutputStream(cSocket.getOutputStream());
            out.writeUTF(reqJSON.toString());
            String rep = in.readUTF().trim();
            
            logger.trace("Communication with "+hostName +" Request=> "+reqJSON.toString()+"Reply=> "+rep);
            return new JSONObject(rep);
    	}catch(Exception e) {
    		logger.debug("Problem in communicating with "+hostName, e);
    		throw e;
    	}
        finally {
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

    long lastGUIDmargeTime = 0;
    long lastMDFSMergeTime=0;
	/**
	 * This function checks whether there is a neighbor edge connected via mesh link (i.e. without 
	 * any intermediate middlebox). If a neighbor edge is found, then it tries to merge 
	 * GUID data and MDFS directory information 
	 * 
	 */
	private void tryEdgeMerge() {
		TopoGraph g = EKHandler.getTopoHandler().getGraph();
		Set<TopoNode> neighbors = g.getVertexbyType(NodeType.EDGE_NEIBOR);
		
		if(System.currentTimeMillis() - lastGUIDmargeTime > GUID_MERGE_INTERVAL) {
			sendGUIDtoNeighbor(neighbors);
			lastGUIDmargeTime = System.currentTimeMillis();
		}
		if(System.currentTimeMillis() - lastMDFSMergeTime > MDFS_MERGE_INTERVAL) {
			sendMDFStoNeighbor(neighbors);
			lastMDFSMergeTime = System.currentTimeMillis();				
		}
	}
	
	void sendGUIDtoNeighbor(Set<TopoNode> neighbors) {
		new Thread() {
			public void run(){
				JSONObject guidJobj = EKHandler.getZKClientHandler().prepareMergeData();
				if (guidJobj == null) {
					logger.error("GUID merge data is null");
					return;
				}
				
				JSONObject reqJSON = new JSONObject();
				try {
					reqJSON.put(CoordinatorServer.requestField, CoordinatorServer.GUID_MERGE_COMMAND);
					reqJSON.put(CoordinatorServer.GUID_MERGE_DATA, guidJobj);
				} catch (JSONException e) {
					logger.error("ERROR in creating json object for GUID merger", e);
					return;
				}

				if (neighbors==null || neighbors.isEmpty()) {
					logger.trace( "No edge neighbor found.");
					return;
				}
				for(TopoNode n: neighbors) 
					sendNeighbor(reqJSON, n);
			}
		}.start();
	}

	void sendMDFStoNeighbor(Set<TopoNode> neighbors) {
		new Thread(){
			public void run(){

				if (neighbors==null || neighbors.isEmpty()) {
					logger.trace( "No edge neighbor found.");
					return;
				}
				
				JSONObject mdfsJobj = MetaDataHandler.prepareMergeData();
				if (mdfsJobj == null) {
					logger.error("MDFS merger data is null");
					return;
				}
				
				JSONObject reqJSON = new JSONObject();
				try {
					reqJSON.put(CoordinatorServer.requestField, CoordinatorServer.MDFS_MERGE_COMMAND);
					reqJSON.put(CoordinatorServer.MDFS_MERGE_DATA, mdfsJobj);
				} catch (JSONException e) {
					logger.error("ERROR in creating json object for MDFS merger", e);
					return;
				}

				if (neighbors==null || neighbors.isEmpty()) {
					logger.trace("No edge neighbor found.");
					return;
				}
				
				for(TopoNode n: neighbors) {
					sendNeighbor(reqJSON, n);
				}
			}

		}.start();
	}

	private void sendNeighbor(JSONObject reqJSON, TopoNode n) {
		new Thread() { // Try in different thread  because communication with one neighbor might delay others
			public void run() {
				if(n.ipMaps.keySet() == null || n.ipMaps.keySet().isEmpty()) {
					logger.warn("Not sending merger info to neighbor "+n.guid+" as no IP is found.");
					return;
				}
				for(String ip: n.ipMaps.keySet()) {
					try {
						JSONObject repJSON = communicate(ip, reqJSON);
						if(repJSON.getString(CoordinatorServer.resultField).equals(CoordinatorServer.successMessage)) {
							logger.debug(reqJSON.getString(CoordinatorServer.requestField)+" successful for neighbor "+ n.guid +" through "+ip);
							break;
						}
					} catch (Exception e) {
						logger.warn("Failed to merge data with neighbor "+n.guid+" with IP "+ip, e);
					}
				}
			}
		}.start();
	}

	@Override
	public void terminate() {
		isTerminated=true;
		Thread.currentThread().interrupt();
		logger.info("Terminated "+this.getClass().getName());
	}

    
    
	
}
