package edu.tamu.cse.lenss.edgeKeeper.server;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

//import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.JSONException;
import org.json.JSONObject;

import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MetaDataHandler;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;
import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;

public class CoordinatorServer implements Runnable, Terminable{
//    static final Logger logger = LoggerFactory.getLogger(CoordinatorServer.class);
	static final Logger logger = LoggerFactory.getLogger(CoordinatorServer.class);
	private ServerSocket serverSocket = null;
	
	public CoordinatorServer() throws IOException {
        serverSocket  = new ServerSocket(EKConstants.COORDINATOR_PORT);
        logger.info("Started Coordinator server" + ", listening at " + serverSocket.getLocalPort());
	}
	
	@Override
	public void run() {
        while (!serverSocket.isClosed()) {
            //Accept the socket
        	Socket cSocket = null;
            try{
            	cSocket = serverSocket.accept();
            	new CoordinationProcess(cSocket).start();
            }catch (IOException  e) {
                logger.warn("Problem in accepting client socet", e);
            }
        }
	}
	@Override
	public void terminate() {
		try {
            serverSocket.close();
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Problem in closing the server socket for "+ this.getClass().getSimpleName(), e);
        }
    	logger.info("Terminated "+this.getClass().getName());
	}

	
	public final static String resultField = "RESULT";
    public final static String requestField = "REQUEST";
    public final static String errorMessage = "ERROR";
    public final static String successMessage = "SUCCESS";
    public final static String messageField = "MESSAGE";
    
    public final static String MDFS_MERGE_COMMAND = "MDFS_MERGE_COMMAND";
    public final static String MDFS_MERGE_DATA = "MDFS_MERGE_DATA";
    
    public final static String GUID_MERGE_COMMAND = "GUID_MERGE_COMMAND";
    public final static String GUID_MERGE_DATA = "GUID_MERGE_DATA";
    
    public final static String edgeStatusField = "EDGE_STATUS";
    
    public final static String REPLICA_NOTIFICATION = "REPLICA_NOTIFICATION";

	
	class CoordinationProcess extends Thread{
		private Socket cSocket;

		public CoordinationProcess(Socket cSocket) {
			this.cSocket = cSocket;
		}
		@Override
		public void run() {
            //Accept the socket
            DataInputStream in = null;
            DataOutputStream out = null;
            try{
                in = new DataInputStream (new BufferedInputStream(cSocket.getInputStream()));
                out = new DataOutputStream(cSocket.getOutputStream());

                String inMessage = in.readUTF();//inBuffer.readLine();
                
                //Process the message and reply to the Daemon
                String rep = processRequest(inMessage);
                //logger.info("Reply message = "+rep);
                out.writeUTF(rep+"\n\n");
                
                logger.trace(cSocket.getInetAddress().getHostAddress()+" requests "+inMessage
                			+" Outgoing reply => " + rep);
            }catch (Exception  e) {
                logger.warn("Problem handelling client socket in RequestTranslator", e);
            }
            finally {
    			try {
    				in.close();
    			} catch (Exception e) {}
    			try {
    				out.close();
    			} catch (Exception e) {}
    			try {
    				cSocket.close();
    			} catch (Exception e) {}
            }
		}

		private String processRequest(String inMsg) {
			try {
		        JSONObject rep = null;
	            JSONObject reqJSON = new JSONObject(inMsg);
	            String command = reqJSON.getString(requestField);

	            if (command.equals(edgeStatusField)) {  // The command is to update service
	    			rep = successJSON();
					rep.put(edgeStatusField, EKHandler.edgeStatus.toJSONString());
	            }
				else if (command.equals(MDFS_MERGE_COMMAND)) {
	            	if(!EKHandler.edgeStatus.selfMaster()) {
	            		logger.error(command+" received at a non master node from "+cSocket.getInetAddress().getHostAddress());
	            		return errorJSON("Not valid master").toString();
	            	}
					JSONObject mdfsJobj = reqJSON.getJSONObject(MDFS_MERGE_DATA);
	            	try {
        				MetaDataHandler.mergeNeighborData(mdfsJobj);
        			}catch(Exception e) {
        				logger.error("Error in merging MDFS metadata", e);
        			}
	            	rep=successJSON();
	            }
	            else if (command.equals(GUID_MERGE_COMMAND)) {
	            	if(!EKHandler.edgeStatus.selfMaster()) {
	            		logger.error(command+" received at a non master node from "+cSocket.getInetAddress().getHostAddress());
	            		return errorJSON("Not valid master").toString();
	            	}
	            	JSONObject guidMergeData = reqJSON.getJSONObject(GUID_MERGE_DATA);
        			try {
        				EKHandler.getZKClientHandler().mergeNeighborData(guidMergeData);
        			}catch(Exception e) {
        				logger.error("Error in merging GUID data", e);
        			}
	            	rep=successJSON();
	            }
	            else if (command.equals(REPLICA_NOTIFICATION)) {
					EdgeStatus eStatus = EdgeStatus.getStatus( reqJSON.getString(CoordinatorServer.edgeStatusField) );
					EKHandler.edgeStatus = eStatus;
					logger.info("Got new EdgeStatus notification "+eStatus.toJSONString());
					EKHandler.getZKServerHandler().restart();
					rep=successJSON();
	            }
	    		if(rep == null)
	    			rep = errorJSON("Problem in processing request");

	    		return rep.toString();
	        }catch (Exception e) {
	        	logger.warn("Problem in processing request. ",e);
	        	return "ERROR#JSON read Exception";
	        }
		}
	}

	public static JSONObject errorJSON(String msg) throws JSONException {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put(resultField, errorMessage);
        jsonObj.put(messageField, msg);
        return jsonObj;
    }

    public static JSONObject successJSON() throws JSONException {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put(resultField, successMessage);
        return jsonObj;
    }
	
}
