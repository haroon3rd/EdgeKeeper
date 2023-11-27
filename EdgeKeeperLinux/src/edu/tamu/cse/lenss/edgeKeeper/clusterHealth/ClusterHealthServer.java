package edu.tamu.cse.lenss.edgeKeeper.clusterHealth;


import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import edu.tamu.cse.lenss.edgeKeeper.server.RequestTranslator;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;
import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;

/**
 * this is a cluster monitor server class that runs in any of the phones local_edgekeeper.
 * this server receives app+device information from local edgeKeepers of any device.
 * this server also sends back app+device information to local edgeKeeper of any device whoever asks for.
 * this server is maintained by zookeeper so that if it dies zookeeper starts it off in other devices.
 * @author sbhunia
 *
 */
public class ClusterHealthServer implements Terminable{

	static final Logger logger = Logger.getLogger(ClusterHealthServer.class);

	//tcp variables
	public ServerSocket serverSocket;
	private boolean isRunning = false;
	public DataStore datastore;

	//constructor
	public ClusterHealthServer(){

		logger.log(Level.ALL, "CHS CluserHealthServer initializing DatStore.");

		//initialize the dataStore
		datastore = new DataStore();

	}
  
  

	@Override
	public void run(){
		logger.log(Level.ALL, "CHS CluserHealthServer initializing ServerSocket.");
		long t1 = 0;
		long t2 = 0;
		
		//bind
		try { 
			serverSocket = new ServerSocket(EKConstants.CLUSTER_MONITOR_SERVER_PORT);
		} catch (IOException e) { 
			logger.fatal("CHS Could not start Cluster Health Monitor server", e);
			return;
		}
              
		//update boolean
		isRunning = true;
		logger.log(Level.ALL, "CHS  Cluster Health Monitor server running....");
		while(isRunning){

			Socket cSocket = null;
			DataInputStream in = null;
			DataOutputStream out = null;
			
			try{
				//accept a new client
				cSocket = serverSocket.accept();
				//logger.log(Level.ALL, "CHS Cluster Health Monitor accepted a new client");
				t1 = new Date().getTime();
				
				//initialize input and output streams
				in = new DataInputStream(new BufferedInputStream(cSocket.getInputStream()));
				out = new DataOutputStream(cSocket.getOutputStream());
              
				//read data
				String inMessage = in.readUTF();
              
				//process request
				//logger.log(Level.ALL, "	CHS Cluster Health Monitor start to process client request");
				String rep = processRequestJava(inMessage);
			  
				out.writeUTF(rep+"\n\n");
				
				t2 = new Date().getTime();
				logger.log(Level.DEBUG, "CHS Cluster Monitor Server handled a request from "+cSocket.getInetAddress().getHostAddress()
					+" in " + (t2-t1) + " milli seconds.");
			}catch(Exception e){
				logger.debug("Exception occured for accepting client request ", e);
			}finally {
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
	}

	//takes each client request and handles it
	private String processRequestJava(String inMessage){	

		try {
      	
    	  	JSONObject reqJSON = new JSONObject(inMessage);
    	  	String command = reqJSON.getString(RequestTranslator.requestField);
         
    	  	if(reqJSON!=null){
    	  		if(command.equals(RequestTranslator.putAppStatus)) {

    	  			//store data
    	  			String GUID = reqJSON.getString(RequestTranslator.fieldGUID);
    	  			String appName = reqJSON.getString(RequestTranslator.fieldAppName);
					datastore.putAppStatus(GUID, appName, reqJSON);
                  	
                 	//return success json
    				//logger.log(Level.ALL, "CHS Cluster Health Monitor processed the request succesfully");
    				return successJSON().toString();
                 	
    	  		}else if(command.equals(RequestTranslator.putDeviceStatus)) {

					//logger.log(Level.ALL, "CHS Cluster Health Monitor start to process putDeviceStatus");

    	  			//store data
    	  			String GUID = reqJSON.getString(RequestTranslator.fieldGUID);
					datastore.putDeviceStatus(GUID, reqJSON);

                 	//return success json
    	  			//logger.log(Level.ALL, "CHS Cluster Health Monitor processed the request succesfully");
    	  			return successJSON().toString();
                 	
    	  		}else if(command.equals(RequestTranslator.getAppStatus)) {

					//logger.log(Level.ALL, "CHS Cluster Health Monitor start to process getAppStatus");

					//getEdgeStatus parameters
    	  			String GUID = reqJSON.getString(RequestTranslator.fieldGUID);
    	  			String appName = reqJSON.getString(RequestTranslator.fieldAppName);
              	
    	  			//getEdgeStatus app status json
    	  			JSONObject repJSON = datastore.getAppStatus(GUID, appName);
              	
    	  			//check and return  
    	  			if(repJSON!=null) {

    	  				//add success tag in the json
						JSONObject reply = new JSONObject(repJSON.toString());
						reply.put(RequestTranslator.resultField, RequestTranslator.successMessage);


    	  				//return
    	  				//logger.log(Level.ALL, "CHS Cluster Health Monitor processed the request succesfully");
						return reply.toString();
    	  			}
              	
    	  		}else if(command.equals(RequestTranslator.getDeviceStatus)) {

					//logger.log(Level.ALL, "CHS Cluster Health Monitor start to process getDeviceStatus");


					//getEdgeStatus parameters
    	  			String GUID = reqJSON.getString(RequestTranslator.fieldGUID);
              	
    	  			//getEdgeStatus device status json
    	  			JSONObject repJSON = datastore.getDeviceStatus(GUID);

    	  			//check and return  
    	  			if(repJSON!=null) {

						//add success tag in the json
						JSONObject reply = new JSONObject(repJSON.toString());
						reply.put(RequestTranslator.resultField, RequestTranslator.successMessage);

    	  				//return
    	  				//logger.log(Level.ALL, "CHS Cluster Health Monitor processed the request succesfully");
						return reply.toString();
    	  			}
    	  		}
    	  		
    	  		else if(command.equals(RequestTranslator.getEdgeStatus)){

					//logger.log(Level.ALL, "CHS Cluster Health Monitor start to process getEdgeStatus");

					//getEdgeStatus edge status json
					JSONObject repJSON = datastore.getEdgeStatus();

					//check and return
					if(repJSON!=null){

						//add success tag in the json
						JSONObject reply = new JSONObject(repJSON.toString());
						reply.put(RequestTranslator.resultField, RequestTranslator.successMessage);

						//return
						//logger.log(Level.ALL, "CHS Cluster Health Monitor processed the request succesfully " + repJSON.toString(4));
						return reply.toString();
					}
				}
    	  	}
		}catch(Exception e){
			logger.log(Level.DEBUG, "CHS Cluster Health Monitor failed to process the client request", e);
		}
		//dummy error return 
		return errorJSON("CHS request failed").toString();
	}

	//@Override
	public void terminate() {
		logger.log(Level.ALL, "CHS terminate function called.");
		isRunning = false;
		Thread.currentThread().interrupt();
		if(serverSocket!=null) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				logger.log(Level.ALL, "CHS terminate() call failed");
			}
		}
		logger.info("CHS Terminated "+this.getClass().getName());
	}
	
	//generic success json
	JSONObject successJSON() throws JSONException {
		JSONObject jsonObj = new JSONObject();
		jsonObj.put(RequestTranslator.resultField, RequestTranslator.successMessage);
		return jsonObj;
	}
  
	//generic error json
	JSONObject errorJSON(String msg) {
		try {
			JSONObject jsonObj = new JSONObject();
			jsonObj.put(RequestTranslator.resultField, RequestTranslator.errorMessage);
			jsonObj.put(RequestTranslator.messageField, msg);
			return jsonObj;
		}catch(JSONException e) {
			e.printStackTrace();
		}
		return null;
	}



}


	


