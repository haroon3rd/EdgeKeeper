package edu.tamu.cse.lenss.edgeKeeper.zk;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

// import org.apache.zookeeper.server.quorum.QuorumPeer.ServerState;
import edu.tamu.cse.lenss.edgeKeeper.utils.ServerState;

// import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
// import org.apache.zookeeper.server.quorum.QuorumPeerMain;

import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;

public class EKServMulti implements  Terminable {
    static final Logger logger = Logger.getLogger(EKServMulti.class);
	
	private static AtomicInteger restartSeq = new AtomicInteger(0);
	
	public EKServMulti() {
		logger.debug("***** EKServMulti: entered constructor *****");
	}
	
	public void terminate() {
		logger.debug("***** EKServMulti: entered terminate() *****");
	}
	
	public ServerState getServerState() {
		logger.debug("***** EKServMulti: entered getServerState() *****");
		return ServerState.LOOKING;
	}

	public void restart() {
		logger.debug("***** EKServMulti: entered restart() *****");
		new Thread() {
			public void run() {
				// try {
				// 	terminate();
				// 	logger.log(Level.ALL, "Starting Zookeeper server. parallel run = "+restartSeq.incrementAndGet());
				// 	// runFromConfig(qConfig);
				// }
				// catch (IOException e) {
				// 	logger.fatal("Problem in running Zookeeper server", e);
		        //     terminate();
				// }
			}
		}.start();
		
	}

	@Override
	public void run() {
		logger.debug("***** EKServMulti: run *****");
		restart();	
	}

	
}
