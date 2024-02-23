package edu.tamu.cse.lenss.edgeKeeper.server;

//import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.tamu.cse.lenss.edgeKeeper.dns.DNSServer;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;
import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class is responsible for running a server to which the client applications communicate.
 * It runs in a different thread. Each thread corresponding to Java and CPP servers.
 * @author sbhunia
 *
 */
public class RequestServer implements Terminable{
	
	/* Some constants to be used*/
    public static final int JAVA_PORT = 22222;
    public static final int CPP_PORT = 22223;
    public enum ServerType{JAVA,CPP};
	
    // Limit how many request can be handeled concurrently
    ExecutorService executor = Executors.newFixedThreadPool(EKConstants.REQUEST_SERVER_THREAD);
    RequestResolver requestResolver;
    ServerType serverType;
    ServerSocket serverSocket;

//	public static final Logger logger = LoggerFactory.getLogger(RequestServer.class);
    public static final Logger logger = LoggerFactory.getLogger(RequestServer.class);
    
    
    /**
     * the default constructor
     * @param requestResolver
     * @param olsrInfoRunner 
     * @param serverType
     * @param serverPort
     * @throws IOException
     */
    public RequestServer(RequestResolver requestResolver, ServerType serverType) throws IOException {
        super();
        this.serverType = serverType;
        this.requestResolver = requestResolver;
        
        int serverPort = isJsonClient() ? JAVA_PORT : CPP_PORT;
        
        serverSocket = new ServerSocket(serverPort);
        logger.info("Started RequestServer for " + serverType.name() + ", listening at " + serverPort);
    }

    private boolean isJsonClient() {
        return  serverType == ServerType.JAVA;
    }
    
    
    /**
     * The purpose of the server is to accept new connection and let 
     * the Request Handler del with the service request in another thread.
     */
    public void run() {
        //isRunning = true;
        while (!serverSocket.isClosed()) {
            //Accept the socket
            Socket cSocket;
            try {
                cSocket = serverSocket.accept();
                //logger.debug("New connection at " + serverType);
                executor.execute(new RequestTranslator(serverType, cSocket, requestResolver));
            } catch (IOException e) {
                logger.error("Error in accepting client connection", e);
            }
        }
        // This is the portion for killing all queued threads
        logger.debug("Trying to shutdown the Executors and its associated threads");
        List<Runnable> rhList = executor.shutdownNow(); // Get the list who are waiting for execution
        logger.debug("The remaining tasks shuted down: "+ rhList.toString());
        for (Runnable rh: rhList)
            ((RequestTranslator) rh).terminate();
        logger.debug("Terminated all request Handlers");



    }

    /*    public void closeServerSocket() throws IOException {
            serverSocket.close();
        }
    */
    /**
     * This the hook function executed when the request server is shutting down. 
     * Basically esecuted during the service termination.
     */
    public void terminate() {
        try {
            serverSocket.close();
            logger.info("Closed the Serversocket for " + serverType.name() );
        } catch (Exception e) {
            logger.error("Problem in closing the server socket for "+ serverType.name(), e);
        }
        Thread.currentThread().interrupt();
        if (executor!=null)
        	executor.shutdown();
    	logger.info("Terminated "+this.getClass().getName()+" server type: "+serverType.name());

    }
}
