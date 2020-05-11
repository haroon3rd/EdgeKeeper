package edu.tamu.cse.lenss.edgeKeeper.dns;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.xbill.DNS.Message;
import org.xbill.DNS.SimpleResolver;

import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;
import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;
import edu.umass.cs.gnsserver.gnamed.NameResolution;

/**
 * This class sends the DNS query to the upstream DNS (in reality GNS) servers.
 * @author sbhunia
 *
 */
public class DNSFallback implements Callable<Message>, Terminable{
	public static final Logger logger = Logger.getLogger(DNSFallback.class);

	private Message query;
	private String dnsServerIP;

	public DNSFallback(String dnsServerIP, Message dnsQuery) {
		this.dnsServerIP = dnsServerIP;
		this.query=dnsQuery;
	}

	/**
	 * This function relays the DNS query to the upstream GNS server. Remember that the Executorservice sets a timeout. 
	 * So, there is no need to specify timeout for the DNS Lookup.
	 * Now, if a response is received, it checks whether the response is a legitimate reply. Not something like NXDOMAIN. 
	 * If the response is not legitimate, it raises an exception. Recall that the DNS worker only accepts the result which 
	 * finishes the earliest without any error. 
	 */
	@Override
	public Message call() throws Exception {
		Message dnsResponse =null;
		
		try {
			SimpleResolver dnsServer = new SimpleResolver(dnsServerIP);
			dnsServer.setTimeout(EKConstants.DNS_TIMEOUT);
			//logger.log(Level.ALL, "sending query to DNS. "+dnsServerIP+ " Query: "+ query.toString());
			dnsResponse = dnsServer.send(query);
		} catch (IOException | NullPointerException e) {
			logger.log(Level.ALL, "DNS resolution failed for upstream DNS: "+dnsServerIP+" raising exception");
			throw e;
		}
		
		if (NameResolution.isReasonableResponse(dnsResponse)) {
//			logger.log(Level.ALL, "Got reasonable response from DNS "+dnsServerIP+". Outgoing response from DNS: "+
//					dnsResponse.toString());
			return dnsResponse;
		}else {
			logger.log(Level.ALL, "Got unreasonable response from DNS "+dnsServerIP+". response: "+
					dnsResponse.toString()+" raising exception");
			throw new UnknownHostException();
		}
	}

	@Override
	public void terminate() {
		Thread.currentThread().interrupt();
    	logger.info("Terminated"+this.getClass().getName());
	}

	@Override
	public void run() {
		// this function would not be called 
	}

}
