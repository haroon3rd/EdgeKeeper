package edu.tamu.cse.lenss.edgeKeeper.zk;

import java.io.IOException;

import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
//import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;

public class ZKServSingle extends ZooKeeperServerMain implements  Terminable{
	static final Logger logger = LoggerFactory.getLogger(ZooKeeperServerMain.class.getName());
	private ServerConfig serverConfig;

	public ZKServSingle(ServerConfig serverConfig) {
		this.serverConfig = serverConfig;
	}
	public void terminate() {
		try {
			super.shutdown();
		} catch(Exception e) {
			logger.info("Problem shutting down the Single instance Zookeeper server",e);
		}
		logger.info("Terminated"+this.getClass().getName());
	}
	public void restart() {
		new Thread() {
			public void run() {
				try {
					terminate();
					
					logger.debug("Starting Standalone ZK Server.");
					
					runFromConfig(serverConfig);
					
					logger.debug("Successfully Started Standalone ZK Server.");
				} catch (IOException e) {
					logger.error("Problem in running Zookeeper server", e);
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
