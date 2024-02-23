package edu.tamu.cse.lenss.edgeKeeper.clusterHealth;


import java.util.*;
import edu.tamu.cse.lenss.edgeKeeper.server.RequestTranslator;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONObject;

//this class contains all the health information that the ClusterHealthServer.java receives.
public class DataStore {
	static final Logger logger =  ClusterHealthServer.logger;

    //data structures
    private Map<String, JSONObject> deviceStatus;  //GUID, deviceSTatus
    private Map<String, HashMap<String, JSONObject>> appStatus;  //GUID, App , AppStatus

    //public constructor
    public DataStore(){

        //init
        this.deviceStatus = new HashMap<>();
        this.appStatus = new HashMap<>();
    }

    public void putAppStatus(String GUID, String appName, JSONObject status){

        //remove unnecessary fields
        status.remove(RequestTranslator.requestField);
        status.remove(RequestTranslator.resultField);
        status.remove(RequestTranslator.fieldAppName);
        status.remove(RequestTranslator.fieldGUID);

        //check if entry for GUID exists, if not create it
        if(!appStatus.containsKey(GUID)){
            appStatus.put(GUID, new HashMap<String, JSONObject>());
        }


        //add timestamp in app status
        try {
            status.put(RequestTranslator.UPDATE_TIME, System.currentTimeMillis());
        } catch (Exception e) {
            logger.debug("DataStore could not add timestamp on app status");
        }

        //now push the new app status
        appStatus.get(GUID).put(appName, status);

    }

    public void putDeviceStatus(String GUID, JSONObject status){

        //remove unnecessary fields
        status.remove(RequestTranslator.requestField);
        status.remove(RequestTranslator.fieldGUID);

        //add timestamp in device status
        try {
            status.put(RequestTranslator.UPDATE_TIME, System.currentTimeMillis());
        } catch (Exception e) {
            logger.debug("DataStore could not add timestamp on device status", e);
        }
        //create new entry or replace the old entry
        deviceStatus.put(GUID, status);
    }

    public JSONObject getAppStatus(String GUID, String appName){

        //check if guid exists
        if(appStatus.containsKey(GUID)){

            //check if appname exists
            if(appStatus.get(GUID).containsKey(appName)){

                //getEdgeStatus the app status
                JSONObject aa = appStatus.get(GUID).get(appName);

                //check time
                if(aa!=null) {
                    try {
                        long now = new Date().getTime();
                        long then = aa.getLong(RequestTranslator.UPDATE_TIME);
                        long diff = now - then;
                        if((diff > 0 ) && (diff <= EKConstants.clusterHealthThreshold)) {
                            return aa;
                        }
                    } catch (Exception e) {
                        logger.debug("DataStore could not getEdgeStatus long value from app status json object.");
                    }
                }
            }
        }

        return null;
    }

    public JSONObject getDeviceStatus(String GUID){

        //check if guid exists
        if(deviceStatus.containsKey(GUID)){

            //getEdgeStatus the device status
            JSONObject dd = deviceStatus.get(GUID);

            //check time
            if(dd!=null) {
                try {
                    long now = new Date().getTime();
                    long then = dd.getLong(RequestTranslator.UPDATE_TIME);
                    long diff = now - then;
                    if((diff > 0 ) && (diff <= EKConstants.clusterHealthThreshold)) {
                        return dd;
                    }
                }catch(Exception e){
                    logger.debug("DataStore could not getEdgeStatus long value from device status json object.");
                }
            }
        }

        return null;
    }


    //getEdgeStatus all unique GUIDs
    private List<String> getAllUniqueGUIDs(){

        //create a set
        Set<String> GUIDs = new HashSet<>();

        //add all guids from device status Map
        GUIDs.addAll(deviceStatus.keySet());

        //add all guids from app status map
        GUIDs.addAll(appStatus.keySet());

        //return as a list
        return new ArrayList<>(GUIDs);
    }

    private List<String> getAllUniqueAppNames(){

        //create a set
        Set<String> APPs = new HashSet<>();

        //getEdgeStatus all GUIDs from app status map
        List<String> guids = new ArrayList<>(appStatus.keySet());

        //for each GUID, getEdgeStatus all app names under it
        for(int i=0; i< guids.size(); i++){
            APPs.addAll(appStatus.get(guids.get(i)).keySet());
        }

        //return as a list
        return new ArrayList<>(APPs);
    }

    public JSONObject getEdgeStatus(){
        try {
            //create return JSONObject
            JSONObject repJSON = new JSONObject();

            //getEdgeStatus all unique GUIDs
            List<String> GUIDs = getAllUniqueGUIDs();

            //getEdgeStatus all unique APPs
            List<String> APPs = getAllUniqueAppNames();

            //make deviceStatus health JSON object
            JSONObject deviceStatus = new JSONObject();
            for(int i=0; i < GUIDs.size(); i++){
                String g = GUIDs.get(i);
                JSONObject dd =  this.getDeviceStatus(g);
                if(dd!=null) {
                    deviceStatus.put(g,dd);
                }
            }
            repJSON.put(RequestTranslator.deviceStatus, deviceStatus);

            //make appStatus health JSON object
            JSONObject appStatus = new JSONObject();
            for(int i=0; i< APPs.size(); i++){
                String a = APPs.get(i);
                JSONObject app = new JSONObject();
                for(int j=0; j< GUIDs.size(); j++){
                    String g = GUIDs.get(j);
                    JSONObject aa = this.getAppStatus(g,a);
                    if(aa!=null) {
                    	app.put(g, aa);
                    }
                    
                }
                appStatus.put(a, app);
            }
            repJSON.put(RequestTranslator.appStatus, appStatus);

            //return json object
            return repJSON;

        }catch(Exception e){
        	logger.debug("Problem in getEdgeStatus function",e);
        }

        return null;
    }

}
