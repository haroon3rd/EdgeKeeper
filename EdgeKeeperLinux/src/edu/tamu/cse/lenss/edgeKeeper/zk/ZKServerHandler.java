package edu.tamu.cse.lenss.edgeKeeper.zk;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import edu.tamu.cse.lenss.edgeKeeper.dns.DNSServer;
import edu.tamu.cse.lenss.edgeKeeper.server.EKHandler;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKProperties;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils;
import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;

public class ZKServerHandler implements Terminable{
	public static final Logger logger = Logger.getLogger(ZKServerHandler.class);

    //EKProperties ekProperties;
    EKHandler eventHandler;
    //ExecutorService executor = Executors.newFixedThreadPool(1);
    EKServWorker rz;
	public String ownServerIP;

    public ZKServerHandler(EKHandler eventHandler) {
        this.eventHandler=eventHandler;
    }

    /**
     * For now, this function just generates ZK id based on the last tupple of the IP address
     * @param ip
     * @return
     */
	String getReplicaID(String ip) {
		if (EKProperties.validIP(ip)) {
			String[] addrArray = ip.split("\\.");
			long num = 0;
			for (int i = 0; i < addrArray.length; i++) {
				int power = 3 - i;
				num += ((Integer.parseInt(addrArray[i]) % 256 * Math.pow(256, power)));
			}
			return String.valueOf(num);
		} else
			return null;
	}

    /**
     * This is the mainual configuarion
     * @param ekProperties
     * @return
     */
    Map<String, String> getReplicas(){
        Map<String, String> ekMasterMap = new HashMap<String, String>();
        
        Collection<String> replicaIps = EKHandler.edgeStatus.replicaMap.values();

        logger.log(Level.ALL, "Potential ZK replicas: "+replicaIps);
        for (String ip: replicaIps){
            //logger.log(Level.ALL, "Potential EK master: "+ip);
            if( ip==null || ip.isEmpty() || getReplicaID(ip)==null){
                logger.error("Problem in parsing IP address "+ip);
            }
            else
                ekMasterMap.put(ip, getReplicaID(ip));
        }
        return ekMasterMap;
    }

    /**
     * 
     * @param cfg
     * @return the IP address on which interface the Zookeeper server will run. Null indicates this node is not running ZK server
     * @throws IOException
     * @throws NullPointerException
     */
    String prepareDataDir(Properties cfg) throws IOException, NullPointerException {
        Map<String, String> ekMasterMap = getReplicas();

    	String ekPath = cfg.getProperty(EKProperties.dataDir);
        File ekDir = new File(ekPath);
        if (! ekDir.exists()) {
        	logger.info("Zookeeper data directory does not exist. Creating the directory. "+ekPath);
            ekDir.mkdirs();
        }else {
        	logger.info("Zookeeper data directory already exist. "+ekPath);
        }
        
        
        Set<String> ownIPs = EKUtils.getOwnIPv4s();
        for(String ip: ownIPs) {
        	//String ipStr = ip.getHostAddress();
        	logger.debug("Checking whether this ip is in config. "+ip);
        	if(ekMasterMap.containsKey(ip)) {
        		logger.info("This device is acting as master. Preparing the myid. IP: "+ip);
        	    
        		String ownID = getReplicaID(ip);
        		
        		BufferedWriter writer = new BufferedWriter(new FileWriter(ekPath+"/myid"));
        		writer.write(ownID);
        		writer.close();
        		
        		logger.debug("Prepared the myid file: "+ekPath+"/myid" +"\t"+ownID);
        		
        		return ip;
        	}
        }
        return null;
    }


    Properties prepareZKProperties() {
        Properties zkProp = new Properties();

        Map<String, String> ekMasterMap = getReplicas();
        logger.info("EK Master map: "+ekMasterMap.toString());

        for (String ip: ekMasterMap.keySet()){
            String id = ekMasterMap.get(ip);
            zkProp.setProperty("server."+id, ip+":2740:3740");
        }

        // Now getEdgeStatus the default properties

        zkProp.setProperty(EKProperties.tickTime, EKHandler.getEKProperties().getProperty(EKProperties.tickTime));
        zkProp.setProperty(EKProperties.syncLimit, EKHandler.getEKProperties().getProperty(EKProperties.syncLimit));
        zkProp.setProperty(EKProperties.initLimit, EKHandler.getEKProperties().getProperty(EKProperties.initLimit));
        zkProp.setProperty(EKProperties.dataDir, EKHandler.getEKProperties().getProperty(EKProperties.dataDir));
        zkProp.setProperty(EKProperties.clientPort, EKHandler.getEKProperties().getProperty(EKProperties.clientPort));
        
        return zkProp;
    }

    public void run(){}
    
	public void restart() {
		logger.log(Level.ALL,"Trying to restart ZKServer ");
			this.terminate();
			Properties zkProp = prepareZKProperties();
			try {
				ownServerIP = prepareDataDir(zkProp);

				if (ownServerIP != null) {
					// need to change this
					// Radu: commented out
					// Thread.sleep(1000);
					logger.info("Trying to start new Zookeeper server");
					rz = new EKServWorker(zkProp, eventHandler, ownServerIP);
					rz.run();
				} else {
					logger.info("This node not acting as EK master. Working in client mode");
				}
			} catch (NullPointerException | IOException /*| InterruptedException */ e) {
				logger.fatal("Problem in preparing Zookeeper configuration", e);
			}
		}

	public void terminate() {
		//logger.info("Stopping Zookeeper Handler");
		if (rz != null)
			rz.terminate();
		logger.info("Terminated"+this.getClass().getName());
	}
	
	String getOwnServerIP() {
		return ownServerIP;
	}



}


