package edu.tamu.cse.lenss.edgeKeeper.zk;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

// import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
// import org.apache.zookeeper.server.quorum.QuorumPeerMain;

import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;

// import org.apache.zookeeper.server.quorum.QuorumPeer.ServerState;
import edu.tamu.cse.lenss.edgeKeeper.utils.ServerState;

public class EKServMulti implements  Terminable {
    static final Logger logger = Logger.getLogger(EKServMulti.class);
	
	private static AtomicInteger restartSeq = new AtomicInteger(0);
	
	public EKServMulti() {
	}
	
	public void terminate() {
	}
	
	public ServerState getServerState() {
		return ServerState.LOOKING;
	}

	public void restart() {
		new Thread() {
			public void run() {
			}
		}.start();
		
	}

	@Override
	public void run() {
		restart();	
	}

	
}
