
package edu.tamu.cse.lenss.edgeKeeper.dns;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKProperties;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils;
import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;

/**
 * This class defines a DnsServer that serves DNS requests through UDP.
 *
 * DNS requests can be handled just by the GNS server or by the GNS server
 * with a DNS server as a fallback.
 * When using DNS as a fallback we send out parallel requests and whichever returns
 * first is returned to the client as the answer.
 *
 * @author Westy
 * @version 1.0
 */
public class DNSServer implements Terminable {
	public static final Logger logger = LoggerFactory.getLogger(DNSServer.class);
	
	public static final short UDP_SIZE = 512;
	ExecutorService executor = Executors.newFixedThreadPool(50);
	DatagramSocket udpSocket = null;

	//This portion of the code was taken from GNS server code and then modified.
	public void run() {
		try {
			logger.info("Starting DNS server at UDP port no. "+EKConstants.DNS_UDP_PORT);
			udpSocket = new DatagramSocket(EKConstants.DNS_UDP_PORT);
		} catch (SocketException e) {
			logger.error("Could not start DNS server. Try running with sudo previledge. ",e);
			return;
		}
		
		while (! udpSocket.isClosed()) {
			try {
				byte[] inData = new byte[UDP_SIZE];
		        DatagramPacket incomingPacket = new DatagramPacket(inData, inData.length);
		        incomingPacket.setLength(inData.length);
		        udpSocket.receive(incomingPacket);
		        executor.execute(new DNSWorker(udpSocket, incomingPacket));
			} catch (IOException  e) {
				logger.error("Error in UDP server",e);
			}
	    }
	  }

  
	@Override
	public void terminate() {
		//isTerminated = true;
		if(udpSocket!=null)
			udpSocket.close();
		//Now clost the Executorservice and all related DNS worker threads
		if(executor!=null) {
			List<Runnable> list = executor.shutdownNow();
			for(Runnable l : list) {
				((Terminable) l).terminate();
			}
		}
    	logger.info("Terminated"+this.getClass().getName());
	}
	
	
	
	public static void main(String[] args) throws IOException {
		try {
			EKUtils.initLogger("logs/client.log", Level.ALL);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		EKProperties prop;
		try {
			prop = EKProperties.loadFromFile(System.getProperty("user.dir")+"/ek.properties");
		} catch (IllegalArgumentException | IOException | IllegalAccessException e) {
			logger.error("Problem in loading properties file",e);
			return;
		}

		
		//new DNSServer(prop).start();
	}

	
	
}
