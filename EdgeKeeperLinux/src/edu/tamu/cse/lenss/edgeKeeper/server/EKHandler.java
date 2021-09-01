package edu.tamu.cse.lenss.edgeKeeper.server;


import javax.jmdns.*;

import edu.tamu.cse.lenss.edgeKeeper.topology.NSD;
import edu.tamu.cse.lenss.edgeKeeper.topology.TopoHandler;
import edu.tamu.cse.lenss.edgeKeeper.topology.TopologyMonitor;
import org.apache.curator.framework.CuratorFramework;

import org.apache.curator.framework.state.ConnectionState;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.zookeeper.server.quorum.QuorumPeer.ServerState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import edu.tamu.cse.lenss.edgeKeeper.clusterHealth.ClusterHealthClient;
import edu.tamu.cse.lenss.edgeKeeper.clusterHealth.ClusterHealthServer;
import edu.tamu.cse.lenss.edgeKeeper.clusterHealth.HealthWebView;
import edu.tamu.cse.lenss.edgeKeeper.dns.DNSServer;
//import edu.tamu.cse.lenss.edgeKeeper.topology.TopoHandler;
//import edu.tamu.cse.lenss.edgeKeeper.topology.TopoHandler;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKProperties;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKRecord;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils;
import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;
import edu.tamu.cse.lenss.edgeKeeper.zk.ZKServerHandler;
import edu.tamu.cse.lenss.edgeKeeper.zk.ZKClientHandler;


/**
 * This class handles all the complexities for starting the Request servers,
 * stopping them, etc. It is written for putting common code between Desktop and
 * android in one place.
 * @author sbhunia
 *
 */
public class EKHandler extends Thread implements Terminable{

	//logger
	public static final Logger logger = Logger.getLogger(EKHandler.class);

	//variables
	boolean isTerminated;
	JmDNS jmdns;
	String ownGUID;

	//more variables
	static GNSClientHandler gnsClientHandler;
	static EKUtils ekUtils;
	private static EKProperties ekProperties;
	static ZKServerHandler zkServerHandler;
	static ZKClientHandler zkClientHandler;
	public static EKRecord ekRecord = new EKRecord();
	RequestResolver requestResolver;
	public static EdgeStatus edgeStatus;
	public static CoordinatorServer coordinatorServer;
	public static CoordinatorClient coordinatorClient;


    ExecutorService executorService;

    //list of terminable objects which we terminate when process is being killed
    List<Terminable> terminableTasks = new ArrayList<Terminable>();
    
    DNSServer dnsServer;
    RequestServer javaThread;
    RequestServer cppThread;
    IPMonitor ipMonitor;
	private ClusterHealthServer clusterHealthServer;
	private HealthWebView healthWebView;

	// Switching between two Topology Monitor Implementation
	public static TopoHandler topoHandler;
	//public static TopologyMonitor topoHandler;
	//public static TopologyMonitor topologyMonitor;

	public static AtomicInteger conNum=new AtomicInteger(0);
	private ClusterHealthClient clusterHealthClient;

	//only public constructor
	public EKHandler(EKUtils _ekUtils, EKProperties prop) {
        ekUtils= _ekUtils;
        ekProperties = prop;
    }

    //getter functions
    public static ZKClientHandler getZKClientHandler() {
    	return zkClientHandler;
    }
    public static ZKServerHandler getZKServerHandler() {
    	return zkServerHandler;
    }
    public static GNSClientHandler getGNSClientHandler() {
    	return gnsClientHandler;
    }
    public static EKProperties getEKProperties() {
    	return ekProperties;
    }
    public static EKUtils getEKUtils() {
    	return ekUtils;
    }

	//returns either TopoHandler or TopologyMonitor depending on what implementation is being used
	public static TopologyMonitor getTopoMonitor() {
		return topoHandler;
	}

    //this method start the service and create two Request server threads.
    public void run()  {

		//start ekUtils for
    	ekUtils.onStart();
    	
    	logger.debug("Starting GNS service. ConcurrentServices: " +conNum.incrementAndGet());

    	//validate ek properties
    	try {
    		ekProperties.validate();
    		logger.log(Level.ALL, "Validated all properties");
    	} catch(Exception e) {
    		logger.fatal("Invalid EK Properties. Not starting EdgeKeeper services", e);
    		ekUtils.onError("Invalid EK Properties. Not starting EdgeKeeper services");
    		return;
    	}
    	isTerminated = false;
    	
    	gnsClientHandler = new GNSClientHandler(ekUtils, ekProperties);
    	this.terminableTasks.add(gnsClientHandler);

    	//Now, fetch the device's GUID
		ownGUID = gnsClientHandler.getOwnGUID();
		if (ownGUID == null) {
			logger.fatal("Could not fetch Own GUID entry");
			ekUtils.onError("Could not fetch Own GUID");
			terminate();
			return;
		}

		ekProperties.setGUID(ownGUID);
		ekRecord.updateField(EKRecord.ACCOUNTNAME_FIELD, ekProperties.getAccountName());

		edgeStatus = new EdgeStatus();
		
		try {
			if(edgeStatus.selfMaster()) { //Run health server only if acting as master
		    	this.clusterHealthServer = new ClusterHealthServer();
		    	this.terminableTasks.add(clusterHealthServer);
		    	
				this.dnsServer = new DNSServer();
				this.terminableTasks.add(dnsServer);
			}

			//init topology handler
			topoHandler = new TopoHandler();
			//topologyMonitor = new TopologyMonitorImpl();
			this.terminableTasks.add((Terminable) topoHandler);
			//this.terminableTasks.add((Terminable) topologyMonitor);

			coordinatorServer = new CoordinatorServer();
			this.terminableTasks.add(coordinatorServer);
			
			coordinatorClient = new CoordinatorClient();
			this.terminableTasks.add(coordinatorClient);
			
	    	//Now start ZK CLient
			zkClientHandler = new ZKClientHandler(this);
			this.terminableTasks.add(zkClientHandler);
			
			//First start Zookeeper servers
			zkServerHandler = new ZKServerHandler(this);
			this.terminableTasks.add(zkServerHandler);

			this.clusterHealthClient = new ClusterHealthClient();
			this.terminableTasks.add(clusterHealthClient);
			
			this.requestResolver = new RequestResolver(gnsClientHandler, zkClientHandler);

    		
        	this.javaThread = new RequestServer(requestResolver,  RequestServer.ServerType.JAVA);
        	this.terminableTasks.add(javaThread);

        	this.cppThread = new RequestServer(requestResolver, RequestServer.ServerType.CPP);
        	this.terminableTasks.add(cppThread);

        	this.ipMonitor = new IPMonitor();
        	this.terminableTasks.add(ipMonitor);

    		this.healthWebView = new HealthWebView();
    		this.terminableTasks.add(healthWebView);

    		//register JMDNS service
			if(false){
				NSD jmdns = new NSD();
				ExecutorService executorService = Executors.newFixedThreadPool(1);
				executorService.execute(jmdns);

				//add shutdownhook
				this.terminableTasks.add(jmdns);

				//abcd
			}

		} catch (IOException e) {
			logger.fatal("Problem in creating one of the server thread"+e);
    		ekUtils.onError("Could not start server port for clients"+e.getStackTrace());
			terminate();
			return;
		}
    	
		this.executorService = Executors.newFixedThreadPool(20);
		for(Terminable task: terminableTasks)
			executorService.submit(task);
		logger.info("EdgeKeeper all tasks started");
    }

    /**
     * This function updates the current host IP in the GNS server
     */

    public void networkCHange(){

        (new Thread(){
            public void run(){
                try {
                    requestResolver.updateIP();
                } catch (Exception e) {
                    logger.error("Problem connecting", e);
                }
            }
        }).start();

    }

    private class IPMonitor extends Thread implements Terminable{
    	boolean terminated = false;



		@Override
		public void run() {
	    	Set<String> ownIPs = new HashSet<String>();
	    	
	    	while(!this.terminated) {
				Set<String> newIPs = EKUtils.getAllAPs();
				
				if (ownIPs.equals(newIPs) ) {
					//logger.log(Level.ALL,"Own device IP addresses remains same. No need to update");
				}
				else {
					logger.info("Detected change in own IP address. Trying to update IP to GNSserver.");
                    requestResolver.updateIP(newIPs);
                    ownIPs=newIPs;
				}
				// Now sleep for predefined time
				try {
					sleep(EKConstants.IP_CHANGE_DETECT_INTERVAL);
				} catch (InterruptedException e) {
					logger.error("Problem in sleep",e);
				}
			}
		}
		@Override
		public void terminate() {
			this.terminated=true;
			this.interrupt();
			logger.info("Terminated"+this.getClass().getName());
			
		}
    }
    
    public ShutDownHook getShutDownHook() {
    	return new ShutDownHook(terminableTasks, executorService);
    }
    
    public void terminate() {
    	
    	logger.log(Level.ALL, "SSS_mohammad terminate() start");
    	
    	//stop jmdns
		//only stop jmdns if this is desktop EdgeKeeper
		if(!EKUtils.isAndroid()) {
			try {
				jmdns.unregisterAllServices();
				jmdns.close();
				logger.log(Level.ALL, "SSS_mohammad jmdns.close() succeeded");
			} catch (Exception e) {
				e.printStackTrace();
				logger.log(Level.ALL, "SSS_mohammad exception in jmdns.close()");
			}
		}

    	logger.log(Level.ALL, "SSS_mohammad terminate() end");
    	
        this.getShutDownHook().start();
    }
    
    public void restart() {
    	logger.info("Restarting EdgeKeeper");
    	this.terminate();
		// Radu: commented out
    	// try {
			// sleep(1000);
		// } catch (InterruptedException e) {
		// 	logger.error("Problem in sleep",e);;
		// }
    	this.start();
    }
    
    /**
     * This is basically a shutdown hook used for Desktop
     * @author sbhunia
     *
     */
    public class ShutDownHook extends Thread{
    	List<Terminable> tList;
		private ExecutorService exec;
    	public ShutDownHook(List<Terminable> tList, ExecutorService executorService) {
			this.tList = tList;
			this.exec=executorService;
		}

		@Override
        public void run()
        {
    		logger.info("EdgeKeeper shutting down");
    		for (Terminable t:tList) {
    			try {
    				t.terminate();
    			} catch(Exception e) {
    				logger.fatal("Problem in terminating"+t.getClass().getSimpleName(), e);
    			}
    		}
    		if(exec!=null) {
	    		List<Runnable> remaining = exec.shutdownNow();
	    		if(remaining!=null && !remaining.isEmpty())
	    			logger.fatal("Problem in terminating EdgeKeeper. Some tasks are still running");
    		}
    		
    		ekUtils.onStop();      

        	//this will release the other instances.
        	logger.debug("Stopped EdgeKeeper. ConcurrentServices: " +conNum.decrementAndGet());  
        	
        }
    }
    
    
	public void onZKServerStop() {
		onZKServerStateChnage(ServerState.OBSERVING);
		logger.info("This ZK server is stopped");
	}

	public void onZKMultiInstanceRun() {
		logger.info("Multi instance Zookeeper server is started");
	}

	public void onZKSingleInstanceRun() {
		onZKServerStateChnage(ServerState.LEADING);
		logger.info("Single instance Zookeeper server is started");
	}

	public void curatorError(String message, Throwable e) {
		logger.info("Curator client error", e);
	}

	public void curatorStateChange(CuratorFramework c, ConnectionState newState) {
		ekUtils.onCuratorStateChange(newState);
	}


	public void onZKServerStateChnage(ServerState newServStatus) {
		ekUtils.onZKServerStateChange(newServStatus);
		switch (newServStatus) {
		case LOOKING:
			//this.eventHandler.onStartLooking();
			break;
			
		case OBSERVING:
			//this.eventHandler.onStartObserving();
			break;
			
		case LEADING:
			//this.eventHandler.onStartLeading();
			//zkClientHandler.setHealthLeader();
			break;
			
		case FOLLOWING:
			//this.eventHandler.onStartFollowing();
			break;
		}
		
	}
	
}
