package edu.tamu.cse.lenss.edgeKeeper.dns;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Header;
import org.xbill.DNS.Message;
import org.xbill.DNS.Opcode;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Section;

import edu.tamu.cse.lenss.edgeKeeper.server.EKHandler;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKProperties;
import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;

public class DNSWorker implements  Terminable{
	private DatagramSocket udpSocket;
	private DatagramPacket incomingPacket;

	public static final Logger logger = LoggerFactory.getLogger(DNSWorker.class);
	//private EKProperties ekProperties;
	ExecutorService executor;
	private long requestArrivalTime;
	private long startTime;
	private long endTime;
	
	
	public DNSWorker(DatagramSocket sock, DatagramPacket incomingPacket) {
		this.udpSocket=sock;
		this.incomingPacket=incomingPacket;
		//this.ekProperties = ekProperties;
		this.requestArrivalTime = System.currentTimeMillis();
	}

	@Override
	public void run() {
		this.startTime = System.currentTimeMillis();
	    Message query=null;
	    Message response;
	    
	    try { //First extract the query
			query = new Message(incomingPacket.getData());
		} catch (IOException | NullPointerException e) {
			logger.warn("Problem in fetching the Query. Not replying any response."+incomingPacket);
		    return;
		}

	    Header header = query.getHeader();
		// If it's not a query we just ignore it. It indicates that this is a response message.
		if (header.getFlag(Flags.QR)) {
			logger.debug("Query header QR set. Nothing to be done. Incoming query: "+query.toString());
			return ;
		}
		else if (header.getRcode() != Rcode.NOERROR) { // Check if there is any error in the query
			logger.debug("There is error in the query. Incoming query: "+query.toString());
			response = errorMessage(query, Rcode.FORMERR);
		}
		else if (header.getOpcode() != Opcode.QUERY) {  // Check if it is really a query message
			logger.debug("Incoming message OPTCode is not a Query. Incoming query: "+query.toString());
			response = errorMessage(query, Rcode.NOTIMP);
		}
		else {
			/*
			 * THE MEAT IS IN HERE. Try to getEdgeStatus a response from the GNS or DNS servers.
			 * It generates the reply for a specific query. It sends the query to all the GNS servers 
			 * ( as DNS query) and the local EK cluster simultaneously. These calls throw error if they 
			 * can not resolve the name. Thus, only the result with Successful name resolve will be accepted. 
			 */
			String[] gnsAddresses = EKHandler.getEKProperties().getGNSAddr();
			List<Callable<Message>> taskList = new ArrayList<Callable<Message>>();
			for (String gAddr : gnsAddresses) 
				taskList.add(new DNSFallback(gAddr, (Message) query.clone()));
		
			//Also query the local EK cluster
			taskList.add(new DNSLookupLocal( (Message)query.clone()));
		
			// Get the result which returned first without any error
			executor = Executors.newFixedThreadPool(gnsAddresses.length + 1);
			try {
				response = executor.invokeAny(taskList, EKConstants.DNS_TIMEOUT, TimeUnit.SECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				logger.debug("Error in Resolving Name. Returning NXDOMAIN");
				response =  errorMessage(query, Rcode.NXDOMAIN);
			} finally {
				executor.shutdownNow();
			}
		}
	    
	    sendResponse(query,response);
	    
	}
	

	/**
	 * Generates an NXDOMAIN error response for a specific query.
	 * @param query
	 * @return
	 */
	private Message errorMessage(Message query, int rcode) {
		if(query == null)
			return null;
		Message response = new Message(query.getHeader().getID());
		response.getHeader().setFlag(Flags.QR);
		if (query.getHeader().getFlag(Flags.RD)) {
			response.getHeader().setFlag(Flags.RA);
		}
		response.addRecord(query.getQuestion(), Section.QUESTION);
		response.getHeader().setRcode(rcode);
		return response;
	}

	/**
	 * This function reply the response message to the querier. 
	 * @param response 
	 * @param query 
	 * @param responseBytes
	 */
	private void sendResponse(Message query, Message response) {
		if (response == null) { // means we don't need to do anything
	    	logger.debug("Generated Response message is null. Not replying anything");
	    	return;
	    }
	    
		//Now set the Length of the response packet
		int maxLength;
	    if (query.getOPT() != null) {
	      maxLength = Math.max(query.getOPT().getPayloadSize(), DNSServer.UDP_SIZE);
	    } else {
	      maxLength = DNSServer.UDP_SIZE;
	    }
		byte[] responseBytes = response.toWire(maxLength);

		
		DatagramPacket outgoingPacket = new DatagramPacket(responseBytes, responseBytes.length, 
				incomingPacket.getAddress(), incomingPacket.getPort());
		
	    this.endTime = System.currentTimeMillis();

	    DNSServer.logger.trace("Response sent to "+ incomingPacket.getAddress().getHostAddress() +":"+incomingPacket.getPort()
	    	+"\n DNS Query: "+query.toString()+"\n DNS Response: " +response.toString());
	    DNSServer.logger.debug("DNS Processing Time (ms) = "+(endTime-startTime)+" Total Delay (ms) = " + (endTime-requestArrivalTime));
		
	    try {
	    	udpSocket.send(outgoingPacket);
	    } catch (IOException e) {
	    	logger.warn("Error in sending the response the the querier ",e);
	    }  
	}



	@Override
	public void terminate() {
		if(executor!=null) {
			List<Runnable> list = executor.shutdownNow();
			for(Runnable l : list) {
				((Terminable) l).terminate();
			}
		}
    	logger.info("Terminated"+this.getClass().getName());
	}
}
