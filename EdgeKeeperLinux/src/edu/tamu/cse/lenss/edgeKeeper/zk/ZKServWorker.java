package edu.tamu.cse.lenss.edgeKeeper.zk;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerFactory;

import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.quorum.QuorumPeer.ServerState;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig.ConfigException;
import org.apache.zookeeper.server.quorum.QuorumPeerMain;

import edu.tamu.cse.lenss.edgeKeeper.server.EKHandler;
import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;

public class ZKServWorker extends Thread implements Terminable{
	static final Logger logger = Logger.getLogger(ZKServWorker.class);
    Properties cfg;
    EKHandler eventHandler;
    List<Terminable> shutdownHook = new ArrayList<Terminable>();
	//private String ownServerIP;
	private int currentSeq;
	private static AtomicInteger restartSeq = new AtomicInteger(0);


    public ZKServWorker(Properties zkProp, EKHandler eventHandler, String ownServerIP) {
        this.cfg = zkProp;
        this.eventHandler = eventHandler;
        //this.ownServerIP = ownServerIP; // Among multiple interfaces, which Ip it will run the ZK Server
    }
 

    @Override
    public void run() {
    	
		currentSeq = restartSeq.incrementAndGet();
		
    	//logger.info(currentSeq+"Starting ZK server status monitor");

        logger.info(currentSeq+" ZK replica configuration: " + cfg.toString());

        QuorumPeerConfig qConfig = new QuorumPeerConfig();
        try {
            qConfig.parseProperties(cfg);
            logger.debug("Parsed qquorum config: " + qConfig.toString());

            if (qConfig.isDistributed()) {
                // Run quorum peer
                logger.info(currentSeq+" Running distributed ZooKeeper quorum peer total peers: :" +
                        qConfig.getServers().size());
                ZKServMulti qp = new ZKServMulti(qConfig);
                ZKServStateMonitor sm = new ZKServStateMonitor(qp, eventHandler);
                shutdownHook.add(sm);
                shutdownHook.add(qp);
                sm.start();
                qp.restart();
                //qp.runFromConfig(qConfig);
                logger.debug(currentSeq +" Successfully Running multi-instance ZooKeeper quorum peer.");
                eventHandler.onZKMultiInstanceRun();
                //eventHandler.onStartLooking();
            } else {
                // Run standalone
                logger.info(currentSeq+ " Attempting to Runn standalone ZooKeeper quorum peer.");
                ServerConfig serverConfig = new ServerConfig();
                serverConfig.readFrom(qConfig);
                ZKServSingle singleServer = new ZKServSingle(serverConfig);
                shutdownHook.add(singleServer);
                singleServer.restart();
                logger.debug(currentSeq +"Successfully Running standalone ZooKeeper quorum peer.");
                eventHandler.onZKSingleInstanceRun();
            }

        } catch ( NumberFormatException | ConfigException | IOException e) {
            logger.fatal(currentSeq+" Problem in parsing the Quorum config or starting new servr", e);
            this.terminate();
        }
    }
    
	public void terminate() {
    	this.interrupt();
    	for(Terminable t: this.shutdownHook)
    		try {
    			t.terminate();
    		} catch(Exception e) {
    			logger.debug("Problem in terminating "+t.getClass().getSimpleName(), e);
    		}
    	eventHandler.onZKServerStop();
		logger.info(currentSeq+" Terminated "+this.getClass().getName());
    }
}



 
