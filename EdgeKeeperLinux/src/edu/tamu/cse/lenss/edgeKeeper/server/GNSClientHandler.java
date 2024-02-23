package edu.tamu.cse.lenss.edgeKeeper.server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
//import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKProperties;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKRecord;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils;
import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;
import edu.umass.cs.gnsclient.client.GNSClient;
import edu.umass.cs.gnsclient.client.GNSCommand;
import edu.umass.cs.gnsclient.client.util.GuidEntry;
import edu.umass.cs.gnsclient.client.util.GuidUtils;
import edu.umass.cs.gnscommon.GNSProtocol;
import edu.umass.cs.gnscommon.exceptions.GNSException;
import edu.umass.cs.gnscommon.exceptions.client.ClientException;

/**
 * This class maintains communication with GNS server, update the GUID, run query, etc.
 * @author sbhunia
 *
 */
public class GNSClientHandler implements Terminable{
//    static final Logger logger = LoggerFactory.getLogger(GNSClientHandler.class);
	static final Logger logger = LoggerFactory.getLogger(GNSClientHandler.class);
    ReentrantLock concurrentLock;

    int		gnsServerPort = 2178;
    String p12FilePath;
    private String p12FilePassword;

    GNSClient client = null;

    EKProperties ekProperties;
    String accountName;
    private String accountPassword;
    GuidEntry ownGUID;
    EKUtils ekUtils;
    
    boolean isTerminated = false;

    protected final static String RECORD_FIELD = "record";
    protected final static String DOMAIN_NAME = "domain";
    protected final static String TTL_FIELD = "ttl";
    private final static String A_RECORD_FIELD = "A";
    private final static String AAAA_RECORD_FIELD = "AAAA";
    private final static int default_ttl = 0; // or specify any value in second for how long each device should cache
    final static String fieldIP = GNSProtocol.IPADDRESS_FIELD_NAME.toString();
    
    

    final static String fieldUserName = GNSProtocol.ACCOUNT_RECORD_USERNAME.toString();
    
    /**
     * The default constructor
     * @param ekUtils this is passed as argument because it will be different 
     * 				depending on whether it is running on Android or Linux 
     * @param p12FilePath
     * @param p12Filepassword
     * @param accountName
     * @param accountPassword
     */
    public GNSClientHandler(EKUtils gnsServiceUtils, EKProperties ekProperties) {
    	this.ekProperties = ekProperties;
        this.ekUtils = gnsServiceUtils;
        this.p12FilePath = ekProperties.getProperty(EKProperties.p12Path);
        this.p12FilePassword = ekProperties.getProperty(EKProperties.p12pass);

        this.accountName = ekProperties.getAccountName();
        this.accountPassword = "password";
        concurrentLock = new ReentrantLock();
        connectionState = ConnectionState.DISCONNECTED;
    }
 
    
    public enum ConnectionState {
        CONNECTED,DISCONNECTED,REGISTRATION_FAILED;
    }
    
    ConnectionState connectionState;

    
    private void gnsDisconnected() {
    	if(this.connectionState!=ConnectionState.DISCONNECTED) {
	    	this.connectionState = ConnectionState.DISCONNECTED;
	    	logger.info("GNS connection got disconnected");
	    	ekUtils.onGNSStateChnage(this.connectionState);
    	}
	}

	private void gnsRegistrationFailed() {
		if(this.connectionState!=ConnectionState.REGISTRATION_FAILED) {
			this.connectionState = ConnectionState.REGISTRATION_FAILED;
			logger.info("GNS Registration Failed");
	    	ekUtils.onGNSStateChnage(this.connectionState);
		}
		
	}

	private void gnsConnected() {
		if(this.connectionState!=ConnectionState.CONNECTED) {
			this.connectionState = ConnectionState.CONNECTED;
			logger.info("GNS client got connected");
	    	ekUtils.onGNSStateChnage(this.connectionState);
		}
	}
    

    /**
     * Registers the account with the corresponding credential passed through constructor.
     * It also updates the IP address
     * @throws FileNotFoundException 
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws JSONException
     * @throws NullPointerException
     * @throws CertificateException
     * @throws KeyStoreException
     * @throws UnrecoverableKeyException
     * @throws GNSException 
     */
    public void registetAccount() throws UnrecoverableKeyException,  NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, JSONException, GNSException, KeyStoreException  {
        logger.debug("Trying to register to GNS server");

        //tryConnectingGNS();
        
        String creation = this.client.execute(GNSCommand.createAccountWithP12Certificate
        		(p12FilePath,p12FilePassword,accountName, accountPassword)).getResultString();

        logger.debug("Registration output: "+creation);

        
        ownGUID = GuidUtils.getGUIDKeys(accountName);

        /*Now, a hostname should be created for DNS lookup. For this purpose, we have to
         * add another alias with trailing '.'. Before doing that check if the name record is added already
         */
        if(! client.execute(GNSCommand.getAliases(ownGUID)).getResultString().contains(accountName+'.'))
            client.execute(GNSCommand.addAlias(ownGUID, accountName+'.')).getResultString();

        //updateIP();
        logger.info("[GNS] Connected with GNS server. Own GUID: "+ownGUID.toString());

        /*logger.info("test"+getAccountNamebyGUID("A2694F1EA7FAF8E917FAEB668267F74912519472"));*/
    }
    
    public String getOwnGUID() {
        //logger.debug("Trying to fetch own GUID");
        ownGUID = GuidUtils.getGUIDKeys(accountName);
        if(ownGUID==null)
		tryConnectingGNS();
		if (ownGUID==null) {
        	logger.error("ownGUID is still null even after trying to coneecting to GNS");
        	return null;
        }
		return ownGUID.getGuid();
    }
    
    public String getOwnAccountName() {
        return this.accountName;
    }

    
    /**
     * This class is used to connect to GNS server from a possible list of IP address using
     * parallel threading. This saves time in the connecting
     * @author sbhunia
     *
     */
    class ConnectSingleGNS implements Callable<GNSClient>{
        String targetAddr = null;
        public GNSClient target;

        ConnectSingleGNS(String targetAddr){
            this.targetAddr = targetAddr;
        }
        @Override
        public GNSClient call() throws IOException {
            try {
                logger.trace("[GNS] Trying to connect to GNS server: "+ targetAddr.toString());
                target = new GNSClient(new InetSocketAddress(targetAddr, gnsServerPort));
                target.setForcedTimeout(EKConstants.GNS_TIMEOUT);
                target.checkConnectivity();
                logger.info("[GNS] succesfully connected to GNS server: "+ targetAddr);
                return target;
            } catch (IOException e) {
                logger.trace("Could not connect to GNS server"+targetAddr);
                if(target!=null)
                	target.close();
                throw e;
            }
        }
    }


    
    /**
     * This function is responsible for these duties:
     Creating / Updating GNS record with the
     * @throws GNSException 
     * @throws SocketException 
     * @throws IOException
     */
    void tryConnectingGNS() {

        //logger.debug("waiting for the lock to be released");
        // First make sure that no otehr thread is trying to connect to GNS.
        concurrentLock.lock();

        try {
            //logger.debug("Checking Connectivity");
            this.client.checkConnectivity();
            logger.trace("GNS connectivity checked. Still connected with old GNS server");
            this.gnsConnected();
        } catch (IOException | NullPointerException e) {

            //logger.trace("Could not connect to the old GNS server", e);

            logger.debug("Could not connect to old GNS server. Trying to find new GNS Server");
            

            String[] gnsServerList = ekProperties.getGNSAddr();
            
            //Close the already established client
            if (client!=null)
            	this.client.close();
            this.client = null;
        /*
            Now, try to connect to GNS server one by one and return if one is connected
            Do this in parallel and when any one of the workers is succesful, move on
            THINGS TO DO: I could not stop the workers who are not connected. I think this should be tried later on.
        =========================================================
        */
            logger.trace("Now trying multithread for connecting GNS server");
            
            List<ConnectSingleGNS> tasks = new ArrayList<ConnectSingleGNS>();
            for (String addr:gnsServerList)
                tasks.add(new ConnectSingleGNS(addr));
            ExecutorService executor = Executors.newFixedThreadPool(tasks.size()); // I do not think more than 10 pools are necessary
            try {
                this.client = executor.invokeAny(tasks);
                logger.debug("Connected with one GNS server: "+ client.toString());
                
                try {
					this.registetAccount();
					this.gnsConnected(); // Announce connected
				} catch (UnrecoverableKeyException | NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException | JSONException | GNSException e1) {
					logger.error("Problem in registering account: "+e1);
					this.gnsRegistrationFailed(); // Announce disconnection problem
					this.client.close();
					this.client = null;
				}
            } catch (InterruptedException | ExecutionException| NullPointerException | IllegalArgumentException  e3) {
                logger.debug("Could not connect to any GNS server. ",e3);
                this.gnsDisconnected(); // announce that it is disconnected
            }finally {
            	executor.shutdownNow();
            }
        } finally {
            concurrentLock.unlock();
        }
        //return;
   }

    
	static long last_committed_update_time = 0;
    AtomicBoolean isUpdatorWaiting = new AtomicBoolean(false);
    
    public void update() {
    	logger.trace("Trying to update the record to GNS");
    	
    	if (!isUpdatorWaiting.get()) {
    		new Thread() {
    			boolean committSuccess = false;
    			public void run() {
    				isUpdatorWaiting.set(true);
    				logger.debug("Starting a new thread to update GNS recrd");
    				while( (!committSuccess) && (!isTerminated)) {
	    				tryConnectingGNS();
	    				if(connectionState == ConnectionState.CONNECTED && client!=null) {
		    				try {
			    				JSONObject record = EKHandler.ekRecord.fetchRecord();
			    		    	long record_time = record.getLong(EKRecord.updateTime);
			    				client.execute(GNSCommand.update(ownGUID, record)).getResult();
			    				last_committed_update_time = record_time;
			    				logger.info("committed update to GNS.  record:\n"+record.toString());
			    				committSuccess =true;
		    				} catch (Exception e) {
		    					logger.error("Update failed. Shall try again", e);
		    				}
						}
						else 
							logger.trace("GNS server is still not connected. shall try again");
	    			}
	    			isUpdatorWaiting.set(false);
	    			
	    			//now check if there was a new update after the record was fetched and finishing the 
	    			// comit to GNS server
					try {
						JSONObject record = EKHandler.ekRecord.fetchRecord();
	    		    	long record_time = record.getLong(EKRecord.updateTime);
	    		    	if (record_time > last_committed_update_time && (!isTerminated)) {
	    		    		logger.debug("Updator committed an old update. Starting updator for the new update.");
	    		    		update();
	    		    	}
					} catch (Exception e) {
						logger.debug("Problem in getting the record update time");
					}
    			}
    		}.start();
    		
    	} else {
    		logger.debug("Already an updator is running,  not starting another thread");
    	}
    }
    
 
    /**
     * This fetches the GUID from AccountName
     * @param AccountName
     * @return
     * @throws IOException
     * @throws GNSException 
     */
    public String getGUIDbyAccountName(String AccountName) {
        if(this.connectionState != ConnectionState.CONNECTED) {
        	logger.trace("GNS Client is disconnected from GNS server");
        	return null;
        }
        String guid =null;
		try {
			guid = client.execute(GNSCommand.lookupGUID(AccountName)).getResultString();
		} catch (ClientException | IOException |NullPointerException e) {
			logger.debug("Error in executing GNS command with server. "+AccountName, e);
			this.gnsConnected();
		}
        return guid;
    }
    /**
     * This fetches the accountname from GUID
     * @param targetGUID
     * @return
     * @throws IOException
     * @throws JSONException
     * @throws GNSException 
     */
    public String getAccountNamebyGUID(String targetGUID) {
    	if(this.connectionState != ConnectionState.CONNECTED) {
        	logger.trace("GNS Client is disconnected from GNS server");
        	return null;
        }
        String name = null;
		try {
			JSONObject record = client.execute(GNSCommand.lookupAccountRecord(targetGUID)).getResultJSONObject();
			name= record.getString(fieldUserName);
		} catch (ClientException | IOException | JSONException | NullPointerException e) {
			logger.debug("Error in executing GNS command with server. "+targetGUID, e);
			this.gnsDisconnected();
		}    
        return name;
    }

    
    /**
     * This function gets the IPs of a client from its GUID
     * @param targetGuid
     * @return
     * @throws IOException
     * @throws JSONException
     * @throws GNSException 
     */
    public List<String> getIPsFromGuid (String targetGuid) {
    	if(this.connectionState != ConnectionState.CONNECTED) {
        	logger.trace("GNS Client is disconnected from GNS server");
        	return null;
        }
    	List<String> ipList = new ArrayList<String>();
    	try {
	        JSONObject aJsonObj = client.execute(GNSCommand.fieldRead(targetGuid, A_RECORD_FIELD,ownGUID)).getResultJSONObject();
	        JSONArray ipJsonArray = aJsonObj.getJSONObject(A_RECORD_FIELD).getJSONArray(RECORD_FIELD);
	        //logger.info(aJsonObj.toString());
	        for (int i = 0; i < ipJsonArray.length(); i++) {
	        	  ipList.add( ipJsonArray.getString(i) );
	        	}
	        logger.trace("[GNS] Target node's IPs: "+ipList.toString());
    	} catch (ClientException | IOException | JSONException | NullPointerException e) {
			logger.debug("Error in executing GNS command with server. "+targetGuid, e);
			this.gnsDisconnected();
		}
        return ipList;
    }
    
    /**
     * This function lookup for the particular service and the target duty
     * and retrieve a list of GUIDs
     * @param service
     * @param duty
     * @return JSONArray of GUIDs 
     * @throws IOException
     * @throws JSONException
     * @throws GNSException 
     */
    public List<String> getPeerGUIDs(String service, String duty)  {
    	if(this.connectionState != ConnectionState.CONNECTED) {
        	logger.trace("GNS Client is disconnected from GNS server");
        	return null;
        }
    	List<String> guidList = null;
    	String qur = "~"+service+" : \""+duty+"\"" ;
        try {
			guidList = (List<String>) client.execute(GNSCommand.selectQuery(qur)).getResultList();
	        logger.trace("[GNS] GUID list:"+ guidList.toString());

		} catch (ClientException | IOException | NullPointerException e) {
			logger.debug("Error in executing GNS command with server. "+service+", "+duty, e);
			this.gnsDisconnected();
		}
        return guidList;
    }

    /**
     * This function implements reverse DNS kind of thing. 
     * The IP address field is stored in a GUID record as:
     * {
     * 		"netAddress":["128.194.130.83","192.168.0.2"]
     * }
     * 
     * @param ip
     * @return
     * @throws ClientException
     * @throws IOException
     */
    public List<String> getGUIDbyIP(String ip) {
    	if(this.connectionState != ConnectionState.CONNECTED) {
        	logger.trace( "GNS Client is disconnected from GNS server");
        	return null;
        }
    	List<String> guidList = null;
    	try {
	    	String qur = "~"+fieldIP+" : \""+ip+"\"" ;
	        logger.trace("Trying to execute query: "+qur);
	        guidList = (List<String>) client.execute(GNSCommand.selectQuery(qur)).getResultList();
	        logger.debug("[GNS] GUID list:"+ guidList.toString());
    	}catch (ClientException | IOException | NullPointerException e) {
			logger.debug("Error in executing GNS command with server. "+ip, e);
			this.gnsDisconnected();
		}
        return guidList;       
    }

    
    
    public void run() {
    	while(!isTerminated) {
    		logger.trace( "checking GNS client connection");
    		tryConnectingGNS();
    		try {
				Thread.sleep(EKConstants.GNS_CONNECTION_CHECK_INTERVAL);
			} catch (InterruptedException e) {
				logger.debug("Problem with sleep.");
			}
    	}
    }
    
    public void terminate() {
    	this.isTerminated = true;
    	Thread.currentThread().interrupt();
    	logger.info("Terminated "+this.getClass().getName());
    }
}
