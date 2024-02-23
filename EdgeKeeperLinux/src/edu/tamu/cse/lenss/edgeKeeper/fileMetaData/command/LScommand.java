package edu.tamu.cse.lenss.edgeKeeper.fileMetaData.command;


import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MDFSMetadata;
import edu.tamu.cse.lenss.edgeKeeper.server.RequestTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MDFSMetadata.delEmptyStr;
import static edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MetaDataHandler.*;

public class LScommand {

    public static final String lsRequestType = "lsRequestType";
    public static final String lsReplyType = "lsReplyType";
    public static final String lsRequestForOwnEdge = "lsRequestForOwnEdge";
    public static final String lsRequestForNeighborEdge = "lsRequestForNeighborEdge";
    public static final String lsRequestForBothOwnAndNeighborEdge = "lsRequestForBothOwnAndNeighborEdge";
    public static final String lsRequestForAllDirectoryiesOfAllNeighborEdges = "lsRequestForAllDirectoryiesOfAllNeighborEdges";
    public static final String lsReplyForOwnEdge = "lsReplyForOwnEdge";
    public static final String lsReplyForNeighborEdge = "lsReplyForNeighborEdge";
    public static final String lsReplyForBothOwnAndNeighborEdge = "lsReplyForBothOwnAndNeighborEdge";
    public static final String lsReplyForAllDirectoryiesOfAllNeighborEdges = "lsReplyForAllDirectoryiesOfAllNeighborEdges";
    public static final String COUNT = "COUNT";
    public static final String FILES = "FILES";
    public static final String FOLDERS = "FOLDERS";

    //handles ls command
    //input: folderPathMDFS
    //output: Json string containing success+data Or, error\
    public static String ls(String folderPathMDFS, String lsCommandType){

        //return = success + replyType + mainData(key:replyTYpe)
        try {
            //check command type
            if (lsCommandType.equals(lsRequestForOwnEdge)) {

                //get ls result for own edge
                JSONObject ownEdgeDir = ls_own_edge_directory(folderPathMDFS);

                //check for null.
                //if null, then add errorJson.
                if (ownEdgeDir == null) {
                    ownEdgeDir = RequestTranslator.errorJSON("Failed to fetch mdfs directory for own edge.");

                }

                //make a new JSONObject with success tag
                //we use successJSON here n matter whether
                // ownEdgeDir succeeded or nah.
                JSONObject repJSON = RequestTranslator.successJSON();

                //add reply type
                repJSON.put(lsReplyType, lsReplyForOwnEdge);

                //put ownEdgeDir
                repJSON.put(lsReplyForOwnEdge, ownEdgeDir.toString());

                //return
                return repJSON.toString();

            } else if (lsCommandType.equals(lsRequestForNeighborEdge)) {

                //get ls results for neighbor edges
                JSONObject neighborsEdgeDirs = ls_neighbor_directory(folderPathMDFS);

                //check for null.
                //if null, then add errorJson.
                if (neighborsEdgeDirs == null) {

                    neighborsEdgeDirs = RequestTranslator.errorJSON("Failed to fetch mdfs directory for neighbor edge(s).");
                }

                //make a new JSONObject with success tag
                //we use successJSON here n matter whether
                // neighborEdgeDir succeeded or nah.
                JSONObject repJSON = RequestTranslator.successJSON();

                //add reply type
                repJSON.put(lsReplyType, lsReplyForNeighborEdge);

                //put neighborsEdgeDirs
                repJSON.put(lsReplyForNeighborEdge, neighborsEdgeDirs.toString());

                //return
                return repJSON.toString();


            } else if (lsCommandType.equals(lsRequestForBothOwnAndNeighborEdge)) {

                //get ls result for own edge
                JSONObject ownEdgeDir = ls_own_edge_directory(folderPathMDFS);

                //check for null.
                //if null, then add errorJson.
                if (ownEdgeDir == null) {
                    ownEdgeDir = RequestTranslator.errorJSON("Failed to fetch mdfs directory for own edge.");

                }

                //get ls results for neighbor edges
                JSONObject neighborsEdgeDirs = ls_neighbor_directory(folderPathMDFS);

                //check for null.
                //if null, then add errorJson.
                if (neighborsEdgeDirs == null) {
                    neighborsEdgeDirs = RequestTranslator.errorJSON("Failed to fetch mdfs directory for neighbor edge(s).");

                }

                //make a new JSONObject with success tag.
                //we use successJSON here no matter either of
                //ownEdgeDir or neighborEDgeDir succeeded or failed.
                JSONObject repJSON = RequestTranslator.successJSON();

                //add reply type
                repJSON.put(lsReplyType, lsReplyForBothOwnAndNeighborEdge);

                //put ownEdgeDir
                repJSON.put(lsReplyForOwnEdge, ownEdgeDir.toString());

                //put neighborsEdgeDirs
                repJSON.put(lsReplyForNeighborEdge, neighborsEdgeDirs.toString());

                //return
                return repJSON.toString();

            }else if (lsCommandType.equals(lsRequestForAllDirectoryiesOfAllNeighborEdges)){

                //user asked for entire neighborEdgeDir structure.
                //in this case, the provided folderPathMDFS doesnt matter.
                JSONObject allNeighEdgesDirs = ls_for_all_directoryies_of_all_neighborEdges();

                //check for null
                //if null, then add errorJson
                if(allNeighEdgesDirs==null){
                    allNeighEdgesDirs = RequestTranslator.errorJSON("Failed to fetch complete mdfs neighbor directory.");
                }

                //make a new JSONObject with success tag.
                //we use successJSON here no matter
                // allNeighEdgesDirs succeeded or failed.
                JSONObject repJSON = RequestTranslator.successJSON();

                //add reply type
                repJSON.put(lsReplyType, lsReplyForAllDirectoryiesOfAllNeighborEdges);

                //put allNeighEdgesDirs
                repJSON.put(lsReplyForAllDirectoryiesOfAllNeighborEdges, allNeighEdgesDirs);

                //return
                return  repJSON.toString();

            }
        }catch (JSONException e){
            e.printStackTrace();
        }

        return null;
    }


    //returns neighborEdgeDir for all masters for all directories.
    //this basically returns all available data for neighbors.
    //returns successJSON with masters and all directory information for each master,
    //or, erroJson with reason for error,
    //or, null f something really bad happened.
    private static JSONObject ls_for_all_directoryies_of_all_neighborEdges(){

        try {
            //fetch mergeDataInode
            MDFSMetadata mergeDataInode = retrieveMetadata(uuid_mergeData);

            //check null
            if(mergeDataInode!=null){

                //get the mergeData JSONObject
                JSONObject mergeDataObj = mergeDataInode.getMergeDataObj();

                //check null
                if(mergeDataObj!=null) {

                    //check if the mergeDataObj is empty,
                    //aka there is no neighbor information to take.
                    if(mergeDataObj.length()==0){

                        //return an error json
                        return RequestTranslator.errorJSON("No neighbor data available.");

                    }else {
                        //copy mergeData into new object
                        JSONObject result = new JSONObject(mergeDataObj.toString());

                        //add success tag into the result obj
                        result.put(RequestTranslator.resultField, RequestTranslator.successMessage);


                        //log
                        MDFSMetadata.logger.trace("returned data for ls_for_all_directoryies_of_all_neighborEdges successfully.");

                        //return result
                        return result;
                    }
                }
            }


        }catch (Exception E){
            //log
            MDFSMetadata.logger.trace("failed to process ls_for_all_directoryies_of_all_neighborEdges.");        }

        return null;
    }


    //returns ls results for neighbor edges for a particular directory.
    //returns successJson with master and particular directory information for each master,
    //or errorJson with reason for error,
    //or, null if something really bad happened.
    private static JSONObject ls_neighbor_directory(String folderPathMDFS) {

        //check if last char is a slash, if not add it.
        if(folderPathMDFS.charAt(folderPathMDFS.length()-1)!='/'){ folderPathMDFS = folderPathMDFS + "/";}

        //make result JSONObject
        JSONObject result = new JSONObject();

        try{

            MDFSMetadata mergeDataInode = retrieveMetadata(uuid_mergeData);

            //check null
            if(mergeDataInode!=null){

                //get the mergeData JSONObject
                JSONObject mergeDataObj = mergeDataInode.getMergeDataObj();

                //check null
                if(mergeDataObj!=null) {

                    //check if the mergeDataOBj is empty,
                    //aka there is no neighbor information to take.
                    if(mergeDataObj.length()==0){

                        //return an error json
                        return RequestTranslator.errorJSON("No neighbor data available.");

                    }else {

                        //get all keys, aka masterguids
                        Iterator<String> masterguids = mergeDataObj.keys();

                        //for each master guid
                        while (masterguids.hasNext()) {

                            //get each master guid
                            String master = masterguids.next();

                            //get all ls information for this master
                            JSONObject lsRes = new JSONObject(mergeDataObj.getString(master));

                            //get the ls result for only folderPathMDFS
                            JSONObject lsOnfolderPathMDFS = new JSONObject(lsRes.getString(folderPathMDFS));

                            //add lsOnfolderPathMDFS obj into result obj
                            result.put(master, lsOnfolderPathMDFS.toString());
                        }


                        //add success tag into the result obj
                        result.put(RequestTranslator.resultField, RequestTranslator.successMessage);


                        //log
                        MDFSMetadata.logger.trace("returned data for ls_neighbor_directory successfully.");

                        //return
                        return result;
                    }
                }
            }

        }catch(Exception e){
            try {

                MDFSMetadata.logger.debug("EdgeKeeper local ls return -> Could not fetch mergeData inode.", e);
                return RequestTranslator.errorJSON("Could not fetch mergeData inode.");
            } catch (JSONException e1) {
                return null;
            }
        }

        //default error return
        try {
            return RequestTranslator.errorJSON("Failed to fetch mdfs directory for neighbor edge(s).");
        } catch (JSONException e) {
            return null;
        }
    }

    //returns ls result for own edge for a particular directory.
    //returns successJson with directory information,
    //or errorJson with reason for error,
    //or, null if something really bad happened.
    public static JSONObject ls_own_edge_directory(String folderPathMDFS){

        MDFSMetadata.logger.trace("EdgeKeeper local start to process ls command.");

        //tokenize the filepath
        //tokens contains only folder names as elements
        String[] tokens = folderPathMDFS.split(File.separator);

        //remove empty string
        tokens = delEmptyStr(tokens);

        //first get the root inode object
        MDFSMetadata metadataRootInode = null;
        try {
            metadataRootInode = retrieveMetadata(uuid_root);

        } catch (Exception e) {
            try {
                MDFSMetadata.logger.debug("EdgeKeeper local ls return -> Could not load root directory.", e);
                return RequestTranslator.errorJSON("Could not load root directory.");
            } catch (JSONException e1) {
                return null;
            }
        }


        //check if root retrieve failed
        if(metadataRootInode!=null){

            //at this point we have the root inode
            //recursively check how many folders from the tokens array already exists.
            boolean pathExists = false;
            int candidateIndex = 0;
            MDFSMetadata candidateInode = metadataRootInode;
            String candidateUUID;
            while(true){
                if(tokens.length>candidateIndex) {

                    //check if tokens[candidateIndex] exists as a child folder of candidateObj
                    if (candidateInode.folderExists(tokens[candidateIndex])) {

                        //get the uuid of the child folder
                        candidateUUID = candidateInode.getFolderUUID(tokens[candidateIndex]);

                        //get the child folder
                        candidateInode = null;
                        try {
                            candidateInode = retrieveMetadata(candidateUUID);
                        } catch (Exception e) {
                            try {
                                MDFSMetadata.logger.debug("EdgeKeeper local ls return -> Could not load directory.", e);
                                return RequestTranslator.errorJSON("Could not load directory.");
                            } catch (JSONException e1) {
                                return null;
                            }
                        }

                        //check if child retrieve succeeded
                        if (candidateInode != null) {

                            //increment candidateIndex
                            candidateIndex++;

                        } else {
                            try {
                                MDFSMetadata.logger.debug("EdgeKeeper local ls return -> Could not load directory.");
                                return RequestTranslator.errorJSON("Could not load directory.");
                            } catch (JSONException e) {
                                return null;
                            }
                        }
                    }else {

                        //starting at index candidateIndex the folder doesnt exist so we need to create them
                        pathExists = false;
                        break;
                    }
                }else{
                    //break while loop because the candidateIndex has incremented more than the length of tokens[]
                    //coming here means, all the tokens exists as folders in the system already.
                    //that means folderPathMDFS already exists
                    //at this point, candidateIndex value is tokens.length, and
                    //candidateInode points to the last folder of given folderPathMDFS
                    pathExists = true;
                    break;
                }

            }

            //check if filepath exists
            if(!pathExists){

                //return error since the given path doesnt exist to begin with
                try {
                    MDFSMetadata.logger.debug("EdgeKeeper local ls return -> Directory doesnt exist.");
                    return RequestTranslator.errorJSON("Directory doesnt exist.");
                } catch (JSONException e) {
                    return null;
                }

            }else{

                //path exists so we fetch the files and folders on that inode
                List<String> fileNames = candidateInode.getAllFilesByName();
                List<String> folderNames = candidateInode.getAllFoldersByName();

                //make a resultant JSONObject and return
                try {

                    //make result JSONObject
                    JSONObject OwnEdgeDir = new JSONObject();

                    //make a JSONObject named FILES,
                    //and populate with all file names.
                    JSONObject FILES = new JSONObject();
                    FILES.put(COUNT, fileNames.size());
                    for(int i=0; i< fileNames.size(); i++){ FILES.put(Integer.toString(i), fileNames.get(i));}

                    //make a JSONObject named FOLDERS,
                    //and populate with all folder names.
                    JSONObject FOLDERS = new JSONObject();
                    FOLDERS.put(COUNT, folderNames.size());
                    for(int i=0; i< folderNames.size(); i++){ FOLDERS.put(Integer.toString(i), folderNames.get(i));}

                    //put the FILES and FOLDERS obj into OwnEdgeDir
                    OwnEdgeDir.put(LScommand.FILES, FILES.toString());
                    OwnEdgeDir.put(LScommand.FOLDERS, FOLDERS.toString());


                    //add success tag into the OwnEdgeDir obj
                    OwnEdgeDir.put(RequestTranslator.resultField, RequestTranslator.successMessage);

                    //return OwnEdgeDir
                    return OwnEdgeDir;

                }catch (JSONException e){
                    MDFSMetadata.logger.debug("EdgeKeeper local ls return -> Fetched files and folders names but could not make JSONObject to send back.", e);
                    return null;
                }
            }

        }else{
            try {
                MDFSMetadata.logger.debug("EdgeKeeper local ls return -> Could not load root directory.");
                return RequestTranslator.errorJSON("Could not load root directory.");
            } catch (JSONException e) {
                return null;
            }
        }
    }
}
