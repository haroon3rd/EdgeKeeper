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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

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
public class TopoHandler extends Thread implements Terminable, TopologyMonitor {

	//logger
	public static final Logger logger = Logger.getLogger(TopoHandler.class);
	
	private TopoSender topoSender;
	private TopoCloudServer topoCloudServer;
	private TopoCloudClient topoCloudClient;
	private Map<String, DatagramSocket> sockMap = new HashMap<String, DatagramSocket>();
	TopoGraph ekGraph = null;
	private TopoNode ownNode;
	private boolean terminated;

	public TopoHandler() throws SocketException, UnknownHostException {

		//check if this piece of code running on cloud or ground edge
		if(EKHandler.getEKProperties().getBoolean(EKProperties.isRunningOnCloud)) {
			this.ownNode = new TopoNode(EKHandler.getGNSClientHandler().getOwnGUID(), NodeType.CLOUD__NODE);
		}else {
			this.ownNode = new TopoNode(EKHandler.getGNSClientHandler().getOwnGUID(), NodeType.EDGE_CLIENT);
			this.ekGraph = new TopoGraph(TopoLink.class, ownNode);
			this.ekGraph.addVertex(ownNode);
			this.topoSender = new TopoSender(sockMap, ekGraph, ownNode);
			this.topoCloudServer = new TopoCloudServer(ownNode);
			this.topoCloudClient = new TopoCloudClient(ekGraph, ownNode);
		}
	}

	//opens a new socket in the given IP and starts a new TopoReceiver thread.
	private void openNewSocket(String ip){
		DatagramSocket sock;
		try {
			sock = new DatagramSocket(EKConstants.TOPOLOGY_DISCOVERY_PORT, InetAddress.getByName(ip));
			if(EKUtils.isAndroid()) {
				EKHandler.getEKUtils().bindSocketToInterface(sock);
			}
			sock.setBroadcast(true);
			this.sockMap.put(ip, sock);
			new TopoReceiver(sock, this.ekGraph, this.ownNode).start();
			logger.log(Level.ALL, "Created UDP socket for "+sock.getLocalAddress().getHostAddress()+":"+sock.getLocalPort());
		} catch (IOException e) {
			logger.warn("Problem in opening DataGram socket for "+ip + " : "+EKConstants.TOPOLOGY_DISCOVERY_PORT, e);
		}
	}

	public void run() {
		this.terminated = false;
		if(EKHandler.getEKProperties().getBoolean(EKProperties.isRunningOnCloud)) {
			this.topoCloudServer.start();
		}else{
			this.topoCloudClient.start();
			logger.log(Level.ALL, "Started cloud client");
			Set<String> currentIPs = new HashSet<>();

			while(!this.terminated) {
				try {

					///fetch this node's interfaces and IPs
					ownNode.ipMaps = EKUtils.getOwnIPv4Map();

					//get this node's master's GUID
					ownNode.masterGUID=EKHandler.edgeStatus.getMasterGUID();

					Set<String> newIPs = ownNode.ipMaps.keySet();
					
					if (currentIPs.equals(newIPs) ) {
						//all IP addresses of all interfaces in this device has not changed.
						logger.log(Level.ALL,"Own device IP addresses remains same. No need to update" + newIPs.toString());
					}else {
						//some IP addresses of some interfaces in this device has changed.
						//so, we close dead sockets.
						logger.info("Detected change in own IP address. Closing the previous datagram sockets");
						for(String ip: sockMap.keySet()){
							if (!newIPs.contains(ip)){
								if (sockMap.get(ip) != null)
									sockMap.get(ip).close();
								sockMap.remove(ip);
							}
						}


						//open new sockets only for the newest IPs
						for(String ip: newIPs) {
							if (!sockMap.containsKey(ip)) {
								logger.log(Level.ALL, "Opening Datagram Socket for ip : " + ip);
								openNewSocket(ip);
							}
						}

						//set newIPs as CurrentIPs
						currentIPs=newIPs;
					}

					//double check if each entry in sockMap is closed for some reason, in that case re-open them
					for(Map.Entry<String, DatagramSocket> entry: sockMap.entrySet()) {
						if (entry.getValue().isClosed()) {
							logger.info("The socket for IP " + entry.getKey() + " was closed. Openning new socket");
							openNewSocket(entry.getKey());
						}
					}

				} catch (Exception e) {
					logger.error("Problem in TopoHandler.",e);
				}

				//Now send the periodic UDP ping messages
				try {
					topoSender.sendPeriodicPing();
					logger.log(Level.DEBUG, "Current Graph | "+this.ekGraph.printToString());
					TopoGraph g = TopoParser.importGraph(TopoParser.exportGraph(this.ekGraph));
					logger.log(Level.ALL, "Exported graph "+g.printToString());
				}catch(Exception e) {
					logger.error("Problem in sending periodic messages", e);
				}

				//sleep for thread interrupt
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