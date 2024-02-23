package edu.tamu.cse.lenss.edgeKeeper.server;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

//import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.tamu.cse.lenss.edgeKeeper.clusterHealth.ClusterHealthClient;
import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MetaDataHandler;
import edu.tamu.cse.lenss.edgeKeeper.topology.TopoParser;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;
import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;

/**
 * This class is responsible for deling with the service request from the client.
 * It provides the interface for the clients to run commands. 
 * @author sbhunia
 *
 */
public class RequestTranslator implements Runnable,  Terminable{
	
//	public static final Logger logger = LoggerFactory.getLogger(RequestTranslator.class);
	public static final Logger logger = LoggerFactory.getLogger(RequestTranslator.class);

	
    public final static String commandSeparator = "\t";
    public final static String errorMessage = "ERROR";
    public final static String successMessage = "SUCCESS";

    public final static String resultField = "RESULT";
    public final static String messageField = "MESSAGE";
    public final static String requestField = "REQUEST";
    public final static String serviceField = "SERVICE";
    public final static String dutyField = "DUTY_FIELD";
    //public final static String accountName = "ACCOUNTNAME";

    //Now the commands from client
    public final static String getOwnAccountNameCommand = "GET_OWN_ACCOUNTNAME";
    public final static String getOwnGuidCommand = "GET_OWN_GUID";

    public final static String addServiceCommand = "ADD_SERVICE";
    public final static String removeServiceCommand = "REMOVE_SERVICE";
    public final static String getPeerIPsCommand = "GET_PEER_IPS";
    public final static String getPeerNamesCommand = "GET_PEER_HOSTNAMES";
    public final static String getPeerGUIDCommand = "GET_PEER_GUID";
    public static final String purgeClusterCommand = "PURGE_NAMING_CLUSTER";
	public static final String getAllLocalGUIDCommand = "GET_ALL_LOCAL_GUID";
	public static final String getmergedGUIDCommand = "GET_MERGED_GUID";
	public static final String readGUIDCommand = "READ_GUID";

    public final static String fieldNetworkInfo = "FIELD_NETWORK_INFO";
    public final static String fieldGUID = "GUID";
    public final static String fieldIP = "netAddress";
    public final static String fieldAccountName = "ACCOUNT_NAME";
    
    public final static String updateIPCommand = "UPDATE_IP";
    public final static String getIpByGuidCommand = "GET_IP_BY_GUID";
    public final static String getIpByAccountNameCommand = "GET_IP_BY_ACCOUNTNAME";
    public final static String getGUIDbyAccountNameCommand = "GET_GUID_BY_ACCOUNTNAME";
    public final static String getAccountNamebyGUIDCommand = "GET_ACCOUNTNAME_BYGUID";
    public final static String getAccountNamebyIPCommand = "GET_ACCOUNTNAME_BY_IP";
    public final static String getGUIDbyIPCommand = "GET_GUID_BY_IP";
    public final static String getTopologyCommand = "GET_TOPOLOGY";
	public static final String getZookeeperCommand = "GET_ZOOKEEPER_CONNECTING_STRING";
	public static final String fieldZookeeperString = "ZOOKEEPER_STRING";

	//public static final String putAppStatusField="PUT_APPLICATION_STATUS_COMMAND";
	//mohammad created variables
	public static final String putAppStatus 	= "PUT_APPLICATION_STATUS_COMMAND";
	public static final String putDeviceStatus  = "PUT_DEVICE_STATUS_COMMAND";
	public static final String getAppStatus 	= "GET_APPLICATION_STATUS_COMMAND";
	public static final String getDeviceStatus 	= "GET_DEVICE_STATUS_COMMAND";
    public static final String getEdgeStatus    = "GET_EDGE_STATUS_COMMAND";
    public final static String fieldAppName     = "AppName";
    public static final String deviceStatus     = "DEVICE_STATUS";
    public static final String appStatus        = "APP_STATUS";
    public static final String UPDATE_TIME      = "DATASTOR_UPDATE_TIME";
	
	public static final String MDFS_COMMAND_PREFIX = "MDFS_";
	public static final String MDFSFileName = MDFS_COMMAND_PREFIX + "FILE_NAME";
	public static final String MDFSFileID = MDFS_COMMAND_PREFIX + "FILE_ID";
	public static final String MDFSNodes = MDFS_COMMAND_PREFIX + "NODES";
    public static final String MDFSFileList = MDFS_COMMAND_PREFIX + "FILE_LIST";
	public static final String MDFSPutCommand = MDFS_COMMAND_PREFIX +"PUT_COMMAND";
	public static final String MDFSGetCommand = MDFS_COMMAND_PREFIX + "GET_COMMAND";
    public static final String MDFSLsCommand = MDFS_COMMAND_PREFIX + "LS_COMMAND";
    public static final String MDFSLsReply = MDFS_COMMAND_PREFIX + "LS_REPLY";
    public static final String MDFSMkdirCommand = MDFS_COMMAND_PREFIX + "MKDIR_COMMAND";
    public static final String MDFSRmCommand = MDFS_COMMAND_PREFIX + "RM_COMMAND";
    public static final String MDFSRmType = MDFS_COMMAND_PREFIX + "RM_TYPE   ";
    public static final String MDFSmetadataField = MDFS_COMMAND_PREFIX + "METADATA_FIELD";
	public static final String filePathMDFS = MDFS_COMMAND_PREFIX + "FILE_PATH_MDFS";
    public static final String folderPathMDFS = MDFS_COMMAND_PREFIX + "FOLDER_PATH_MDFS";
    public static final String creatorGUID = MDFS_COMMAND_PREFIX + "CREATOR_GUID";
    public static final String isGlobal = MDFS_COMMAND_PREFIX + "IS_GLOBAL";
    public static final String TRUE = MDFS_COMMAND_PREFIX + "TRUE";
    public static final String FALSE = MDFS_COMMAND_PREFIX + "FALSE";
    public static final String FILE = MDFS_COMMAND_PREFIX + "FILE";
    public static final String DIRECTORY = MDFS_COMMAND_PREFIX + "DIRECTORY";
	
    ExecutorService executor = Executors.newFixedThreadPool(EKConstants.MDFS_CONCURRENT_THREAD); 

    
    private Socket cSocket;
    private RequestServer.ServerType serverType;
    private static AtomicInteger reqNumber=new AtomicInteger(0);

    RequestResolver requestResolver;
    //OlsrInfoRunner olsrInfoRunner;
    int incomingReqNumber;
	private long requestArrivalTime;
	private long startTime;
	private long endTime;
    
    

    /**
     * The default constructor
     * @param serverName
     * @param cSocket
     * @param requestResolver
     * @param olsrInfoRunner 
     */
    public RequestTranslator(RequestServer.ServerType serverType, Socket cSocket, 
    		RequestResolver requestResolver) {
        super();
        this.serverType = serverType;
        this.cSocket = cSocket;
        this.requestResolver = requestResolver;

        incomingReqNumber = reqNumber.getAndAdd(1);
        //logger.debug("RequestTranslator created. Request number: "+ incomingReqNumber);
        this.requestArrivalTime = System.currentTimeMillis();
    }

    private boolean isJsonClient() {
        return  serverType == RequestServer.ServerType.JAVA;
    }
    

    public void run() {
        //logger.debug("RequestTranslator now handeling request number: "+incomingReqNumber);

		this.startTime = System.currentTimeMillis();

    	
    	
    	
        if (this.isJsonClient())	{
            // Start Java server socket and listen on these ports
            DataInputStream in = null;
            DataOutputStream out = null;
            try{
                in = new DataInputStream (new BufferedInputStream(cSocket.getInputStream()));
                out = new DataOutputStream(cSocket.getOutputStream());

                String inMessage = in.readUTF();//inBuffer.readLine();
                
                //Process the message and reply to the Daemon
                String rep = processRequestJava(inMessage);
                //logger.info("Reply message = "+rep);
                out.writeUTF(rep+"\n\n");
                
                logger.trace("Incoming message = "+inMessage
                			+"\nReply message = " + rep);
            }catch (IOException  e) {
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
        else {  // This is the portion for running Server for CPP
        	BufferedReader inBuffer = null;
        	OutputStream os =null;
            try{
                //Read the request from the CPP daemon
                inBuffer = new BufferedReader(new InputStreamReader( cSocket.getInputStream() ));
                os = cSocket.getOutputStream();
                String inMessage = inBuffer.readLine();
                //Process the message and reply to the Daemon
                String rep = processRequestCpp(inMessage);
                os.write((rep+"\n\n").getBytes());
                os.flush();
                
                logger.debug("Incoming message = "+inMessage+
                		"\nReply message = "+rep);
            }catch (IOException e) {
                logger.error("Problem handelling client socket in RequestTranslator", e);
            } finally {
            	try {
	            	//Now close the socket.
	            	if(inBuffer!=null)
	            		inBuffer.close();
	                if(os!=null)
	                	os.close();
	                if(cSocket!=null)
	                	cSocket.close();
            	}catch(Exception e) {
            		logger.warn("Problem closing inputsetreams or client socket in RequestTranslator", e);
            	}
			}

        }
        this.endTime = System.currentTimeMillis();
        logger.debug("Completed request #: "+incomingReqNumber
        		+" Client Processing Time (ms) = "+(endTime-startTime)+" Total Delay (ms) = " + (endTime-requestArrivalTime));
    }



    private String processRequestJava(String inMsg){
        String rep="";
        /* This client communicates with JSON
         * The request is in the form of
         * {
         * 	request : addService,
         * 	appName : MADOOP,
         *  appDuty	: master
         * }
         */

        try {
            JSONObject reqJSON = new JSONObject(inMsg);
            String command = reqJSON.getString(requestField);
            try {
                if (command.equals(addServiceCommand)) { // The command is to update service
                    String service = reqJSON.getString(serviceField);
                    String duty = reqJSON.getString(dutyField);
                    if(requestResolver.addService(service, duty))
                    	rep= successJSON().toString();
                    else
                    	rep=errorJSON("Update failed").toString();
                }
                else if (command.equals(removeServiceCommand)) {  // Command is to remove service
                    String service = reqJSON.getString(serviceField);
                    if(requestResolver.removeService(service))
                    	rep= successJSON().toString();
                    else
                    	rep=errorJSON("Update failed").toString();
                }
                else if (command.equals(getAllLocalGUIDCommand)) {
                    rep= successJSON().put(fieldGUID, requestResolver.getAllLocalGUIDs()).toString();
                }
                else if (command.equals(getmergedGUIDCommand)) {
                    rep= successJSON().put(fieldGUID, requestResolver.getMergedGUIDs()).toString();
                }
                else if (command.equals(readGUIDCommand)) {
                	String guid = reqJSON.getString(fieldGUID);
                    rep= successJSON().put(fieldGUID, requestResolver.readGUID(guid)).toString();
                }
                else if (command.equals(getPeerGUIDCommand)) {
                    String service = reqJSON.getString(serviceField);
                    String duty = reqJSON.getString(dutyField);
                    rep= successJSON().put(fieldGUID, requestResolver.getPeerGUIDs(service, duty)).toString();
                }
                else if (command.equals(getPeerIPsCommand)) {
                    String service = reqJSON.getString(serviceField);
                    String duty = reqJSON.getString(dutyField);
                    rep= successJSON().put(fieldIP, requestResolver.getPeerIPs(service, duty)).toString();
                }
                else if (command.equals(getPeerNamesCommand)) {
                    String service = reqJSON.getString(serviceField);
                    String duty = reqJSON.getString(dutyField);
                    rep= successJSON().put(fieldAccountName, requestResolver.getPeerNames(service, duty)).toString();
                }
                else if (command.equals(getIpByAccountNameCommand)) {
                    String an = reqJSON.getString(fieldAccountName);
                    rep= successJSON().put(fieldIP, requestResolver.getIPsFromAccountName(an)).toString();
                }
                else if (command.equals(getIpByGuidCommand)) {
                    String guid = reqJSON.getString(fieldGUID);
                    rep= successJSON().put(fieldIP, requestResolver.getIPsFromGuid(guid)).toString();
                }
                else if (command.equals(getOwnAccountNameCommand)) {
                    rep = successJSON().put(fieldAccountName, requestResolver.getOwnAccountName()).toString();
                }
                else if (command.equals(getOwnGuidCommand)) {
                    rep = successJSON().put(fieldGUID, requestResolver.getOwnGUID()).toString();
                }
                else if (command.equals(getOwnAccountNameCommand)) {
                    rep = successJSON().put(fieldAccountName, requestResolver.getOwnAccountName()).toString();
                }
                else if (command.equals(getGUIDbyAccountNameCommand)) {
                    rep = successJSON().put(fieldGUID, requestResolver.getGUIDbyAccountName(
                            reqJSON.getString(fieldAccountName) )).toString();
                }
                else if (command.equals(getAccountNamebyGUIDCommand)) {
                    rep = successJSON().put(fieldAccountName,
                            requestResolver.getAccountNamebyGUID( reqJSON.getString(fieldGUID) )).toString();
                }
                else if (command.equals(getAccountNamebyIPCommand)) {
                    rep = successJSON().put(fieldAccountName,
                            requestResolver.getAccountNamebyIP( reqJSON.getString(fieldIP) )).toString();
                }
                else if (command.equals(getGUIDbyIPCommand)) {
                    rep = successJSON().put(fieldGUID,
                            requestResolver.getGUIDbyIP( reqJSON.getString(fieldIP) )).toString();              
                }
                else if (command.equals(getZookeeperCommand)) {
                    rep = successJSON().put(fieldZookeeperString, requestResolver.getZKConnString() ).toString();
                }
                else if (command.equals(purgeClusterCommand)) {  // Command is to remove service
                    if(requestResolver.purgeNamingCluster())
                    	rep= successJSON().toString();
                    else
                    	rep= errorJSON("Could not purge the Cluster").toString();
                }
                else if (command.equals(getTopologyCommand)) {
                	rep = successJSON().put(fieldNetworkInfo, TopoParser.exportGraph(EKHandler.getTopoHandler().getGraph())).toString();
                }
                else if(command.equals(putAppStatus)) {
                	reqJSON.put(RequestTranslator.fieldGUID, requestResolver.getOwnGUID());
                	JSONObject repJSON = ClusterHealthClient.communicateWiteHealthServer(reqJSON);
                	rep = repJSON.toString();
                }else if(command.equals(getAppStatus)) {
                	JSONObject repJSON = ClusterHealthClient.communicateWiteHealthServer(reqJSON);
                	rep = repJSON.toString();
                }else if(command.equals(getDeviceStatus)) {
                	JSONObject repJSON = ClusterHealthClient.communicateWiteHealthServer( reqJSON);
                	rep = repJSON.toString();
                }else if(command.equals(getEdgeStatus)){
                    JSONObject repJSON = ClusterHealthClient.communicateWiteHealthServer(reqJSON);
                    rep = repJSON.toString();
                }
                // MDFS file metadata functionality starts
                else if(command.startsWith(MDFS_COMMAND_PREFIX)){
                    ArrayList<Callable<String>> tasks = new ArrayList<Callable<String>>();
                    tasks.add(new MetaDataHandler(reqJSON));
                    try {
                        rep = executor.invokeAny(tasks, EKConstants.MDFS_THREAD_TIMEOUT, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        logger.error("MDFS MetaDataHandler operation failed ",e);
                        rep = errorJSON("MetaData Execution Exception. "+e.getStackTrace().toString()).toString();
                    }
                }
                // MDFS file metadata functionlity ends
                else{
                    logger.debug("Invalid incoming request");
                    rep= errorJSON("Invalid request").toString();
                }      
            } catch ( NullPointerException e) {
            	logger.warn("Device EdgeKeeper failed to communicate with EK master and GNS server",e);
                rep= errorJSON("Could not connect to EK master or GNS server").toString();
            }
        } catch (JSONException e) {
            logger.warn("JSON read exception", e);
            rep= "ERROR#JSON read Exception";
        }
        return rep;
    }

    /*
     * The CPP client send the datastring in the following format:
     * command+commandSeparator+argumants
     * The return should also be a string. It returns the SUCCESS or ERROR , followed by command separator and then
     * the obtained datas
     */
    private String processRequestCpp(String inMsg) {
        //logger.info("CPP message: "+ inMsg);
        String arrStr[] = inMsg.split(commandSeparator);
        String inCommand = "";
        String inParameter = "";

        if(arrStr.length<1){
            logger.info("Invalid incoming request");
            return errorMessage+commandSeparator+"invalid format";
        }
        else
            inCommand = arrStr[0];
        if (arrStr.length>1)
            inParameter = arrStr[1];

        try {
            if (inCommand.equals(updateIPCommand)) {
                requestResolver.updateIP();
                return successMessage;
            }
            else if(inCommand.equals(getOwnAccountNameCommand)) {
                return successMessage+ commandSeparator+requestResolver.getOwnAccountName();
            }
            else if(inCommand.equals(getOwnGuidCommand)) {
                return successMessage+ commandSeparator+requestResolver.getOwnGUID();
            }
            else if(inCommand.equals( getGUIDbyAccountNameCommand )) {
                return successMessage+ commandSeparator+requestResolver.getGUIDbyAccountName(inParameter);
            }
            else if(inCommand.equals( getAccountNamebyGUIDCommand)) {
                return successMessage+ commandSeparator+requestResolver.getAccountNamebyGUID(inParameter);
            }
            else if(inCommand.equals( getIpByGuidCommand )){
                JSONArray ipJsonArray = requestResolver.getIPsFromGuid(inParameter);
                String outMsg = successMessage;
                for(int j = 0; j < ipJsonArray.length(); j++)
                    outMsg = outMsg+commandSeparator+ ipJsonArray.getString(j);
                return outMsg;
            }
            else if(inCommand.equals( getIpByAccountNameCommand )){
                JSONArray ipJsonArray = requestResolver.getIPsFromAccountName(inParameter);
                String outMsg = successMessage;
                for(int j = 0; j < ipJsonArray.length(); j++)
                    outMsg = outMsg+commandSeparator+ ipJsonArray.getString(j);
                return outMsg;
            }
            else if(inCommand.equals( getGUIDbyIPCommand )){
                JSONArray ipJsonArray = requestResolver.getGUIDbyIP(inParameter);
                String outMsg = successMessage;
                for(int j = 0; j < ipJsonArray.length(); j++)
                    outMsg = outMsg+commandSeparator+ ipJsonArray.getString(j);
                return outMsg;
            }
            else if(inCommand.equals( getAccountNamebyIPCommand )){
                JSONArray ipJsonArray = requestResolver.getAccountNamebyIP(inParameter);
                String outMsg = successMessage;
                for(int j = 0; j < ipJsonArray.length(); j++)
                    outMsg = outMsg+commandSeparator+ ipJsonArray.getString(j);
                return outMsg;
            }
            else {
                logger.debug("Invalid incoming request");
                return errorMessage+commandSeparator+"invalid format";
            }

        } catch (JSONException | NullPointerException  e) {
        	logger.debug("Edge Keeper communication failed failed",e);
            return errorMessage+commandSeparator+"Edge keeper communication failed "+e.getMessage();
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

    public void terminate(){
        try {
            this.cSocket.close();
        } catch (IOException e) {
            logger.warn("Problem closing client socket");
        }
        if (executor!=null)
        	executor.shutdownNow();
        logger.debug("The request handler is terminated");
    }
}
