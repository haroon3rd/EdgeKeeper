package edu.tamu.cse.lenss.edgeKeeper.zk;

import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;

import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;

public class ZKServSingle extends ZooKeeperServerMain implements  Terminable{
	static final Logger logger = Logger.getLogger(ZooKeeperServerMain.class);
	private ServerConfig serverConfig;

	public ZKServSingle(ServerConfig serverConfig) {
		this.serverConfig = serverConfig;
	}
	public void terminate() {
		try {
			super.shutdown();
		} catch(Exception e) {
			logger.log(Level.ALL,"Problem shutting down the Single instance Zookeeper server",e);
		}
		logger.info("Terminated"+this.getClass().getName());
	}
	public void restart() {
		new Thread() {
			public void run() {
				try {
					terminate();
					
					logger.log(Level.DEBUG, "Starting Standalone ZK Server.");
					
					runFromConfig(serverConfig);
					
					logger.log(Level.DEBUG, "Successfully Started Standalone ZK Server.");
				} catch (IOException e) {
					logger.fatal("Problem in running Zookeeper server", e);
		            terminate();
				}
			}
		}.start();
		
	}
	@Override
	public void run() {
		restart();
		
	}
}
