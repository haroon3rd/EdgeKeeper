package edu.tamu.cse.lenss.edgeKeeper.topology;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.tamu.cse.lenss.edgeKeeper.server.EKHandler;
import edu.tamu.cse.lenss.edgeKeeper.topology.TopoNode.NodeType;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKProperties;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtilsAndroid;
import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;

/**
 * This class is responsible for starting the receiver and the sender threads. Remember, everytime the EdgeKeeper
 * restarts, (EK Handler will be restarted as well), this class will be created as new.
 * @author sbhunia
 *
 */
public class TopoHandler extends Thread implements Terminable {
	public static final Logger logger = LoggerFactory.getLogger(TopoHandler.class);
	
	private TopoSender topoSender;
	private TopoCloudServer topoCloudServer;
	private TopoCloudClient topoCloudClient;
	private Map<String, DatagramSocket> sockMap = new HashMap<String, DatagramSocket>();
	
	TopoGraph ekGraph = null;

	private TopoNode ownNode;

	private boolean terminated;

	public TopoHandler() throws SocketException, UnknownHostException {
		
		if(EKHandler.getEKProperties().getBoolean(EKProperties.isRunningOnCloud))
			this.ownNode = new TopoNode(EKHandler.getGNSClientHandler().getOwnGUID(),NodeType.CLOUD__NODE);
		else
			this.ownNode = new TopoNode(EKHandler.getGNSClientHandler().getOwnGUID(),NodeType.EDGE_CLIENT);
	    this.ekGraph = new TopoGraph(TopoLink.class, ownNode);
	    this.ekGraph.addVertex(ownNode);
	    this.topoSender  = new TopoSender(sockMap, ekGraph, ownNode);
	    this.topoCloudServer = new TopoCloudServer(ownNode);
	    this.topoCloudClient = new TopoCloudClient(ekGraph, ownNode);

	}
	
//	public void startTopoDiscovery(){
//		this.topoReceiver.start();
//		this.topoSender.start();
//	}
	
	void openNewSocket(String ip){
		DatagramSocket sock;
		try {
			sock = new DatagramSocket(EKConstants.TOPOLOGY_DISCOVERY_PORT, InetAddress.getByName(ip));
			if(EKUtils.isAndroid()) {
				EKHandler.getEKUtils().bindSocketToInterface(sock);
			}
			sock.setBroadcast(true);
			this.sockMap.put(ip, sock);
			new TopoReceiver(sock, this.ekGraph, this.ownNode).start();
			logger.trace("Created UDP socket for "+sock.getLocalAddress().getHostAddress()+":"+sock.getLocalPort());
		} catch (IOException e) {
			logger.warn("Problem in opening DataGram socket for "+ip + " : "+EKConstants.TOPOLOGY_DISCOVERY_PORT, e);
		}
	}
	
	
	public void run() {
		this.terminated = false;
		if(EKHandler.getEKProperties().getBoolean(EKProperties.isRunningOnCloud)) {
			this.topoCloudServer.start();
		}
		else {
			this.topoCloudClient.start();
			//this.topoSender.start();
			logger.trace("Started cloud client");
			
			Set<String> ownIPs = new HashSet<>();
			while(!this.terminated) {
				try {
					ownNode.ipMaps = EKUtils.getOwnIPv4Map();
					ownNode.masterGUID=EKHandler.edgeStatus.getMasterGUID();
					
					Set<String> newIPs = ownNode.ipMaps.keySet();
					
					if (ownIPs.equals(newIPs) ) {
						logger.trace("Own device IP addresses remains same. No need to update" + newIPs.toString());
					}
					else {
						/* 
						 * When IP change is detected, it is best to close the corresponding socket and remove
						 * it from the socket map. We do not need to stop the corresponding receiver thread
						 * as it will be closed automatically upon the socket closure. 
						 * Next, we shall open the socket for the newly detected IP.
						*/
						logger.info("Detected change in own IP address. Closing the previous datagram sockets");
						for(String ip: sockMap.keySet()) 
							if(!newIPs.contains(ip)) {
								if(sockMap.get(ip)!=null)
									sockMap.get(ip).close();
								sockMap.remove(ip);
							}
						
						for(String ip: newIPs) 
							if(!sockMap.containsKey(ip)) {
								logger.trace("Opening Datagram Socket for ip : " + ip);
								openNewSocket(ip);
							}
						// Stop the respective socket
						ownIPs=newIPs;
					}
					/*
					 * Now check if some socket was accidently closed due to network fluctuation or
					 * somethine else. This kind of checking makes sure that the EK doesn;t crash.
					 */
					for(Map.Entry<String, DatagramSocket> entry: sockMap.entrySet()) 
						if(entry.getValue().isClosed()) {
							logger.info("The socket for IP "+entry.getKey() +" was closed. Openning new socket");
							openNewSocket(entry.getKey());
						}
					
				} catch (Exception e) {
					logger.error("Problem in TopoHandler.",e);
				}

				try {
					/*
					 * Now send the periodic UDP ping messages
					*/
					topoSender.sendPeriodicPing();
					logger.debug("Current Graph | "+this.ekGraph.printToString());
					//this.ekGraph.printToFile();
					TopoGraph g = TopoParser.importGraph(TopoParser.exportGraph(this.ekGraph));
					logger.trace("Exported graph "+g.printToString());

				}catch(Exception e) {
					logger.error("Problem in sending periodic messages", e);
				}
				
				
				try {
					sleep(EKHandler.getEKProperties().getInteger(EKProperties.topoInterval));
				} catch (InterruptedException e) {
					logger.debug("TopoHandler Sleep interupted");
				}
				
			}
		}
	}

	@Override
	public void terminate() {
		this.terminated = true;
		
		for(String ip: sockMap.keySet()) {
			try {
			sockMap.get(ip).close();
			}catch(Exception e) {
				logger.error("Problem in closing server socket for "+ip);
			}
		}
		
		if(topoCloudServer!=null)
			topoCloudServer.terminate();
		if(topoCloudClient!=null)
			topoCloudClient.terminate();
    	logger.info("Terminated "+this.getClass().getName());
	}
	
	public TopoGraph getGraph() {
		return this.ekGraph;
	}

}
