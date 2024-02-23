package edu.tamu.cse.lenss.edgeKeeper.fileMetaData;

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;
import edu.tamu.cse.lenss.edgeKeeper.server.RequestTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.command.GETcommand;
import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.command.LScommand;
import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.command.MKDIRcommand;
import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.command.PUTcommand;
import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.command.RMcommand;
import edu.tamu.cse.lenss.edgeKeeper.server.EKHandler;

import org.json.JSONException;
import org.json.JSONObject;


public class MetaDataHandler implements Callable<String>{

	
	private final JSONObject reqJSON;

	public MetaDataHandler(JSONObject reqJSON){
		this.reqJSON=reqJSON;
	}
	
	public static Logger logger = LoggerFactory.getLogger( MetaDataHandler.class );
	public static final String uuid_root = "MDFS_abcdefghijklmnopqrstuvwxyz";
	public static final String uuid_mergeData = "MDFS_MERGEDATA_abcdefghijklmnopqrstuvwxyz";
	public static final String MASTERGUID = "MASTERGUID";
	public static final String MASTERNAME = "MASTERNAME";
	
	public static String createUUID() {
		return UUID.randomUUID().toString();
	}

	public static String getRootUUID(){
		return uuid_root;
	}

	public static String getMergeDataUUID(){ return uuid_mergeData;}

	public static void storeMetaData(MDFSMetadata metadata) throws Exception{
		String uuid = metadata.getUUID();
		EKHandler.getZKClientHandler().storeMDFSMetadata(uuid,metadata.getBytes());
	}

	public static MDFSMetadata retrieveMetadata(String uuid) throws Exception{
		byte[] data= EKHandler.getZKClientHandler().retrieveMDFSMetadata(uuid);
		return MDFSMetadata.createMetadataFromBytes(data);
	}
	
	public static void deleteMetaData(String uuid) throws Exception{
		EKHandler.getZKClientHandler().deleteMDFSMetaData(uuid);
	}

	public static MDFSMetadata retrieveAndDelete(String uuid)throws Exception{
		MDFSMetadata retrieve = retrieveMetadata(uuid);
		if(retrieve!=null) {
			deleteMetaData(uuid);
		}
		return retrieve;
	}

	public static boolean checkExists(String uuid)throws Exception{
		return EKHandler.getZKClientHandler().checkMetadataExists(uuid);
	}

	private void checkAndcreateRoot() throws Exception {
		if(!checkExists(getRootUUID())){

			//cteare root inode
			MDFSMetadata rootInode = MDFSMetadata.createDirectoryMetadata(getRootUUID(), EKHandler.getGNSClientHandler().getOwnGUID(), File.separator, true);

			//store root inode
			storeMetaData(rootInode);
		}
	}

	private void checkAndCreateMergeDataInode() throws Exception{
		if(!checkExists(getMergeDataUUID())){

			//create mergeData inode
			MDFSMetadata mergeDataInode = MDFSMetadata.createMergeDataInode(getMergeDataUUID());
			//store mergeData inode
			storeMetaData(mergeDataInode);
		}
	}

	@Override
	public String call() throws Exception {
		try {
			checkAndcreateRoot();
			checkAndCreateMergeDataInode();
			//checkAndCreateTestMergeDataInode();
			String command = reqJSON.getString(RequestTranslator.requestField);
			String rep = null;
			if (command.equals(RequestTranslator.MDFSPutCommand)) {
				rep = PUTcommand.put(reqJSON.getString(RequestTranslator.MDFSmetadataField));
			} else if (command.equals(RequestTranslator.MDFSGetCommand)) {
				rep = GETcommand.get(reqJSON.getString(RequestTranslator.filePathMDFS));
			} else if (command.equals(RequestTranslator.MDFSLsCommand)) {
				rep = LScommand.ls(reqJSON.getString(RequestTranslator.folderPathMDFS), reqJSON.getString(LScommand.lsRequestType));
			} else if (command.equals(RequestTranslator.MDFSMkdirCommand)) {
				rep = MKDIRcommand.mkdir(reqJSON.getString(RequestTranslator.creatorGUID), reqJSON.getString(RequestTranslator.folderPathMDFS), reqJSON.getString(RequestTranslator.isGlobal));
			} else if (command.equals(RequestTranslator.MDFSRmCommand)) {
				if (reqJSON.getString(RequestTranslator.MDFSRmType).equals(RequestTranslator.FILE)) {
					rep = RMcommand.rm(reqJSON.getString(RequestTranslator.MDFSRmType), reqJSON.getString(RequestTranslator.filePathMDFS));
				} else if (reqJSON.getString(RequestTranslator.MDFSRmType).equals(RequestTranslator.DIRECTORY)) {
					rep = RMcommand.rm(reqJSON.getString(RequestTranslator.MDFSRmType), reqJSON.getString(RequestTranslator.folderPathMDFS));
				}

			}
			return rep;
		} catch (Exception e){
			logger.trace("MetadataHandler Exception: ",e);
			throw e;
		}
	}

	private void checkAndCreateTestMergeDataInode() throws Exception{

		if(!checkExists(getMergeDataUUID())){

			//create mergeData inode
			MDFSMetadata mergeDataInode = MDFSMetadata.createMergeDataInode(getMergeDataUUID());

			//make a test put
			MDFSMetadata inode = MDFSMetadata.createFileMetadata("uuid", "fileid", 100, "filecreatorguid", "requestorguid", "/one/two/three/file.txt", true);
			PUTcommand.put(inode.fromClassObjecttoJSONString(inode));

			//store mergeData inode
			storeMetaData(mergeDataInode);

			//make dummy merge data
			//get my own mergedata
			JSONObject mergeData = prepareMergeData();

			//pass own mergeData to myself
			mergeNeighborData(mergeData);

		}
	}

	/**
	 * This function merges merges the directory structure of the neighbor edge with own directory structure. The EK coordinator gets the 
	 * neighbor edge info and calls this function.
	 * @param neighborData
	 */
	public static void mergeNeighborData(JSONObject neighborData) {

		//check if the neighborData is null
		if(neighborData!=null){

			//get the master guid
			String masterGUID = null;
			try{
				masterGUID = neighborData.getString(MASTERGUID);
			}catch(JSONException e ){
				e.printStackTrace();
			}

			//remove the masterGUID from the object
			neighborData.remove(MASTERGUID);

			//fetch the mergeDataInode
			try {
				MDFSMetadata mergeDataInode = retrieveMetadata(uuid_mergeData);

				//check null
				if(mergeDataInode!=null){

					//get the JSONObject in it
					JSONObject mergeDataObj = mergeDataInode.getMergeDataObj();

					//put the neighborData into the JSONObject.
					//note: old data of same masterguid will be replaced.
					mergeDataObj.put(masterGUID, neighborData);

					//convert JSONObject into String
					String mergeDataStr = mergeDataObj.toString();

					//put back mergeDataStr into mergeDataInode
					mergeDataInode.putMergeData(mergeDataStr);

					//push back mergeDataInode
					storeMetaData(mergeDataInode);

					logger.trace("Received mergeData from master: " + masterGUID + " successfully.");

				}else{

					//logger: merge data pushing failed
					logger.debug("Failed to merge mdfs directory data for master: " + masterGUID);
				}

			} catch (Exception e) {
				logger.error("Failed to merge mdfs directory data for master: " + masterGUID);
				e.printStackTrace();
			}

		}
	}


	/**
	 * This function prepare a MDFS directory information of own edge which is to be sent to the
	 * neighbor edge for directory view merge.
	 * @return
	 */
	public static JSONObject prepareMergeData() {

		//make result JSONObject
		JSONObject preparedMergeData = new JSONObject();

		try {

			//first get the root inode object
			MDFSMetadata metadataRootInode = null;
			try {
				metadataRootInode = retrieveMetadata(uuid_root);

			} catch (Exception e) {
				logger.error("Could not prepare mergeData to send to other edge.");
				return null;
			}

			//check if root not null
			if(metadataRootInode!=null){

				//sanity check if entire directory is empty
				if(metadataRootInode.getAllFilesByName().size()==0 && metadataRootInode.getAllFoldersByName().size()==0){
					return null;
				}

				//pass the metadataRootInode into recursive function
				recursiveLS(metadataRootInode, preparedMergeData);

			}

			//add master guid
			//preparedMergeData.put(MASTERGUID, EKHandler.edgeStatus.getMasterGUID());
			
			preparedMergeData.put(MASTERGUID, EKHandler.getGNSClientHandler().getOwnGUID());
			
			//add master name
			preparedMergeData.put(MASTERNAME, EKHandler.getGNSClientHandler().getOwnAccountName());


			//log
			logger.trace("Prepared mergeData for own edge dir upon request.");

			//return result
			return preparedMergeData;

		} catch(Exception e) {
			logger.error("Failed to prepare mergeData for own edge dir.", e);
			return null;
		}
	}


	//starts from an inode and keep doing ls in all child folders,
	//until reached the leaf nodes, and populates the JSONObject.
	//this function is used for recursively collect own directory information,
	//and prepare a mergeData, and give it to edgekeeper,so that edgekeeper can send it to other masters.
	private static void recursiveLS(MDFSMetadata currentInode, JSONObject preparedMergeData){

		try {

			//get all files and folders in currentInode.
			List<String> fileNames = currentInode.getAllFilesByName();
			List<String> folderNames = currentInode.getAllFoldersByName();

			//make a new JSONObject for containing all files in currentInode
			JSONObject FILES = new JSONObject();
			FILES.put(LScommand.COUNT, Integer.toString(fileNames.size()));
			for(int i=0; i< fileNames.size(); i++){FILES.put(Integer.toString(i), fileNames.get(i));}

			//make a new JSONObject for containing all folders in currentInode
			JSONObject FOLDERS = new JSONObject();
			FOLDERS.put(LScommand.COUNT, Integer.toString(folderNames.size()));
			for(int i=0; i< folderNames.size(); i++){FOLDERS.put(Integer.toString(i), folderNames.get(i));}

			//put the FILES and FOLDERS object into a new JSONObject
			JSONObject currDirlsRes = new JSONObject();
			currDirlsRes.put(LScommand.FILES, FILES.toString());
			currDirlsRes.put(LScommand.FOLDERS, FOLDERS.toString());

			//put the currDirlsRes object into result obj
			preparedMergeData.put(currentInode.getFilePathMDFS(), currDirlsRes.toString());

			//check if reached base case
			if (currentInode.getAllFoldersByUUID().size() == 0) {
				return;

			} else {

				//meaning currentInode has more folders in it.
				//get all the folder uuids of this inode and pass them in the recursive function.
				List<String> folderUUIDs = currentInode.getAllFoldersByUUID();

				//recursively get all uuids of all child
				for (int i = 0; i < folderUUIDs.size(); i++) {

					MDFSMetadata currentInode1 = retrieveMetadata(folderUUIDs.get(i));
					recursiveLS(currentInode1, preparedMergeData);
				}
			}
		}catch(Exception e){
			logger.error("Could not prepare mergeData, Recursive function call failed. ");
		}
	}
	
}
