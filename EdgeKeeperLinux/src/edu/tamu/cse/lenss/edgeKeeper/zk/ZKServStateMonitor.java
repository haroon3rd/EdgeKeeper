package edu.tamu.cse.lenss.edgeKeeper.zk;

import java.util.concurrent.atomic.AtomicInteger;

//import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.zookeeper.server.quorum.QuorumPeer.ServerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.tamu.cse.lenss.edgeKeeper.server.EKHandler;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;
import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;

public class ZKServStateMonitor extends Thread implements Terminable{
	public static final Logger logger = LoggerFactory.getLogger(ZKServStateMonitor.class.getName());
    
    ZKServMulti qp;
    EKHandler eventHandler;
    long pertubationInterval;
	private boolean isTerminated = false;

	private int currentSeq;
	
	private static AtomicInteger restartSeq = new AtomicInteger(0);

    
	public ZKServStateMonitor(ZKServMulti qp, EKHandler eventHandler) {
		this.qp=qp;
		this.eventHandler = eventHandler;
		pertubationInterval = EKHandler.getEKProperties().getTicktime();
		
	}

	
	
	public void run() {
		currentSeq = restartSeq.incrementAndGet();
		
    	logger.info(currentSeq+"Starting ZK server status monitor");
    	
    	ServerState oldservStatus=null;
    	
    	int lookingepoch = 0;
    	
    	while(!isTerminated ) {
    		
    		ServerState newServStatus = qp.getServerState();
			//logger.log(Level.ALL,"New Server status: "+newServStatus);
    		if(newServStatus == ServerState.LOOKING) 
    			lookingepoch++;
//    		else if(newServStatus == ServerState.LEADING)
//    			EKHandler.getZKClientHandler().setHealthLeader();
    		
    		
    		//Basically, if the Zookeeper server is in looking state for too long, restart the Zookeeper server.
    		if(lookingepoch>=EKConstants.zkServerRestartEpoch) {
    			lookingepoch = 0;
    			logger.info(currentSeq+" Zookeeper is in LOOKING state for EPOCH threshold time. Need to restart Zookeeper server");
    			qp.restart();
    		}
    		
    		if(newServStatus!=null && oldservStatus!=newServStatus) {
    			logger.info(currentSeq+" the ZK server status has changed. new status: "+newServStatus);
    			this.eventHandler.onZKServerStateChnage(newServStatus);
    			oldservStatus = newServStatus;
    			lookingepoch = 0;
    		}
    		else
    			logger.info(currentSeq+" the ZK server status remained same. Status: "+newServStatus);
    		
			try {
				sleep(pertubationInterval);
			} catch (InterruptedException e) {
				logger.debug("Problem in sleep", e);;
			}
    	}	
   }
    
	public void terminate() {
		isTerminated = true;
		this.interrupt();
		logger.info(currentSeq+" Terminated "+this.getClass().getName());
	}
	
	

}
