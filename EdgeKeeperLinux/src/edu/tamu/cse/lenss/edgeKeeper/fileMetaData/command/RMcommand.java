package edu.tamu.cse.lenss.edgeKeeper.fileMetaData.command;

import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MDFSMetadata;
import edu.tamu.cse.lenss.edgeKeeper.server.RequestTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MDFSMetadata.delEmptyStr;
import static edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MetaDataHandler.*;

public class RMcommand {


    public static String rm(String type, String mdfsPath){

        //check if its a file removal or directory removal
        if(type.equals(RequestTranslator.FILE)){
            return rm_file(mdfsPath);
        }else if(type.equals(RequestTranslator.DIRECTORY)){
            return rm_direcotry(mdfsPath);
        }

        return null;
    }

    private static String rm_file(String filePathMDFS) {

        MDFSMetadata.logger.trace( "EdgeKeeper local start to process rm_file command.");

        //tokenize the filepath
        //tokens contains folder names as elements but the last element is the filename
        String[] tokens = filePathMDFS.split(File.separator);

        //remove empty strings
        tokens = delEmptyStr(tokens);

        //first get the root inode object
        MDFSMetadata metadataRootInode = null;
        try {
            metadataRootInode = retrieveMetadata(uuid_root);
        } catch (Exception e) {
            MDFSMetadata.logger.debug("EdgeKeeper local rm return -> Could not load root directory.");
            try {
                return RequestTranslator.errorJSON("Could not load root directory.").toString();
            } catch (JSONException e1) {
                return null;
            }
        }

        //check if retrieve failed
        if(metadataRootInode!=null){

            //at this point we have the root inode
            //recursively check how many folders from the tokens array already exists.
            boolean pathExists = false;
            int candidateIndex = 0;
            MDFSMetadata candidateInode = metadataRootInode;
            String candidateUUID;
            while(true){
                if(tokens.length-1>candidateIndex) {

                    //check if tokens[candidateIndex] exists as a child folder of candidateInode
                    if (candidateInode.folderExists(tokens[candidateIndex])) {

                        //get the uuid of the child folder
                        candidateUUID = candidateInode.getFolderUUID(tokens[candidateIndex]);

                        //get the child folder
                        candidateInode = null;
                        try {
                            candidateInode = retrieveMetadata(candidateUUID);
                        } catch (Exception e) {
                            try {
                                MDFSMetadata.logger.debug("EdgeKeeper local rm return -> Could not load directory.");
                                return RequestTranslator.errorJSON("Could not load directory.").toString();
                            } catch (JSONException e1) {
                                return null;
                            }
                        }

                        //check if child retrieve succeeded
                        if (candidateInode != null) {

                            //increment candidateIndex
                            candidateIndex++;

                        }

                    }else {
                        //starting at index candidateIndex the folder doesnt exist so we need to create them
                        pathExists = false;
                        break;

                    }
                }else{
                    //break while loop because the candidateIndex has incremented more than or equal to tokens.length -1
                    //coming here means, all the tokens exists as folders in the system already.
                    //that means folderPathMDFS already exists.
                    //candidateIndex value is tokens.length -1 .
                    //candidateInode points to the last folder of given folderPathMDFS.
                    pathExists = true;
                    break;
                }

            }

            //check if filepath already exists
            if(!pathExists){
                try {
                    MDFSMetadata.logger.debug("EdgeKeeper local rm return -> Directory doesnt exist.");
                    return RequestTranslator.errorJSON("Directory doesnt exist.").toString();
                } catch (JSONException e) {
                    return null;
                }
            }else{

                //path exists, that means the file may exists in that directory.
                //check if the file exists in that directory
                if(candidateInode.getAllFilesByName().contains(tokens[candidateIndex])){

                    //file exists in this folder so get its uuid
                    String fileUUID = candidateInode.getFileUUID(tokens[candidateIndex]);

                    //delete child from parent's file list
                    candidateInode.removeFile(fileUUID);

                    //push back parent folder
                    try {
                        storeMetaData(candidateInode);
                    } catch (Exception e) {
                        try {
                            MDFSMetadata.logger.debug("EdgeKeeper local rm return -> Could not modify file directory.");
                            return RequestTranslator.errorJSON("Could not modify file directory.").toString();
                        } catch (JSONException e1) {
                            return null;
                        }
                    }

                    //retrieve and delete the file uuid
                    MDFSMetadata fileMetadataInode = null;
                    try {
                        fileMetadataInode = retrieveAndDelete(fileUUID);
                    } catch (Exception e) {
                        MDFSMetadata.logger.debug("EdgeKeeper local rm return -> Could not retrieve file metadata before deletion.");
                        try {
                            return RequestTranslator.errorJSON("Could not retrieve file metadata before deletion.").toString();
                        } catch (JSONException e1) {
                            return null;
                        }
                    }

                    //check if retrieveAndDelete failed
                    if(fileMetadataInode!=null){

                        //convert class object into Json string
                        String fileMetadataString = fileMetadataInode.fromClassObjecttoJSONString(fileMetadataInode);

                        //check if Gson failed
                        if(fileMetadataString!=null){

                            //return success with metadata
                            try {
                                MDFSMetadata.logger.debug("EdgeKeeper local rm return -> successfully handled rm(file) command.");
                                return RequestTranslator.successJSON().put(RequestTranslator.MDFSmetadataField, fileMetadataString).toString();
                            } catch (JSONException e) {
                                return null;
                            }

                        }else{
                            try {
                                MDFSMetadata.logger.debug("EdgeKeeper local rm return -> Gson Exception on file inode.");
                                return RequestTranslator.errorJSON("System error").toString();
                            } catch (JSONException e) {
                                return null;
                            }
                        }

                    }

                }else{
                    try {
                        MDFSMetadata.logger.debug("EdgeKeeper local rm return -> File doesnt exist in directory.");
                        return RequestTranslator.errorJSON("File doesnt exist in directory.").toString();
                    } catch (JSONException e) {
                        return null;
                    }
                }
            }
        }

        //dummy return
        try {
            return RequestTranslator.errorJSON("File deletion failed.").toString();
        } catch (JSONException e) {
            return null;
        }
    }


    private static String rm_direcotry_dummy(String folderPathMDFS){
        try {
            return RequestTranslator.errorJSON("-rm is not supported for folders yet.").toString();
        } catch (JSONException e) {
            return null;
        }
    }

    //remove a directory from MDFS.
    //that means removing all the files in all the subDirectories the given filePathMDFS may have.
    private static String rm_direcotry(String folderPathMDFS){

        MDFSMetadata.logger.trace( "EdgeKeeper local start to process rm_directory command.");

        //tokenize the filepath
        //tokens contains folder names only
        String[] tokens = folderPathMDFS.split(File.separator);

        //remove empty strings
        tokens = delEmptyStr(tokens);

        //first get the root inode object
        MDFSMetadata metadataRootInode = null;
        try {
            metadataRootInode = retrieveMetadata(uuid_root);
        } catch (Exception e) {
            try {
                MDFSMetadata.logger.debug( "EdgeKeeper local rm return -> Could not load root directory.");
                return RequestTranslator.errorJSON("Could not load root directory.").toString();
            } catch (JSONException e1) {
                return null;
            }
        }

        //check if retrieve failed
        if(metadataRootInode!=null){

            //at this point we have the root inode
            //recursively check how many folders from the tokens array already exists.
            boolean pathExists = false;
            int candidateIndex = 0;
            MDFSMetadata candidateInode = metadataRootInode;
            String parentOfLastFolderInodeUUID = null;
            String tempInodeObjUUID = null;
            String candidateUUID;
            while(true){
                if(tokens.length>candidateIndex) {

                    //check if tokens[candidateIndex] exists as a child folder of candidateInode
                    if (candidateInode.folderExists(tokens[candidateIndex])) {

                        //cache current inode uuid
                        tempInodeObjUUID = candidateInode.getUUID();

                        //get the uuid of the child folder
                        candidateUUID = candidateInode.getFolderUUID(tokens[candidateIndex]);

                        //get the child folder
                        candidateInode = null;
                        try {
                            candidateInode = retrieveMetadata(candidateUUID);
                        } catch (Exception e) {
                            try {
                                MDFSMetadata.logger.debug("EdgeKeeper local rm return -> Could not load directory.");
                                return RequestTranslator.errorJSON("Could not load directory.").toString();
                            } catch (JSONException e1) {
                                return null;
                            }
                        }

                        //check if child retrieve succeeded
                        if (candidateInode != null) {

                            //check if this is the (tokens.length -1)th inode
                            if(candidateIndex==tokens.length-1){
                                parentOfLastFolderInodeUUID =tempInodeObjUUID;
                            }

                            //increment candidateIndex
                            candidateIndex++;



                        }

                    }else {
                        //starting at index candidateIndex the folder doesnt exist
                        break;

                    }
                }else{

                    //break while loop because the candidateIndex has incremented more than or equal to tokens.length
                    //coming here means, all the tokens exists as folders in the system already.
                    //that means folderPathMDFS already exists.
                    //candidateIndex value is tokens.length.
                    //candidateInode points to the last folder of given folderPathMDFS.
                    pathExists = true;
                    break;
                }

            }

            //check if filepath already exists
            if(!pathExists){
                try {
                    MDFSMetadata.logger.debug( "EdgeKeeper local rm return -> Directory doesnt exist.");
                    return RequestTranslator.errorJSON("Directory doesnt exist.").toString();
                } catch (JSONException e) {
                    return null;
                }
            }else{

                //path exists.
                //create a resultant file list which will contain all child of childs files to delete.
                List<String> allDeletableFileUUIDs = new ArrayList<>();

                //create a resultant folder list which will contain all child of childs folders to delete.
                List<String> allDeletableFolderUUIDs = new ArrayList<>();

                //add all the files in this directory for deletion
                allDeletableFileUUIDs.addAll(candidateInode.getAllFilesByUUID());

                //get all the folders in this directory and add them for deletion
                List<String> allFolders = candidateInode.getAllFoldersByUUID();
                allDeletableFolderUUIDs.addAll(allFolders);

                //recursively fetch all uuids of all child folders
                try {
                    for (int i = 0; i < allFolders.size(); i++) {

                        MDFSMetadata folderInode = retrieveMetadata(allFolders.get(i));

                        getChildOfChilOfChildAndSoAndSo(allDeletableFileUUIDs, allDeletableFolderUUIDs, folderInode);
                    }
                }catch(Exception e){
                    try {
                        MDFSMetadata.logger.debug("EdgeKeeper local rm return -> Could not recursively fetch all folders.");
                        return RequestTranslator.errorJSON("Could not fetch all child folders.").toString();
                    } catch (JSONException e1) {
                        return null;
                    }
                }

                //retrieve all file metadata and delete from candidateObj
                JSONArray allDeletedFiles = new JSONArray();
                try {
                    for (int i = 0; i < allDeletableFileUUIDs.size(); i++) {
                        MDFSMetadata metadata= retrieveAndDelete(allDeletableFileUUIDs.get(i));
                        JSONObject obj = new JSONObject();
                        obj.put(RequestTranslator.MDFSFileName, metadata.getFileName());
                        obj.put(RequestTranslator.MDFSFileID, metadata.getFileID());
                        JSONArray nodes = new JSONArray();
                        for(int ii=0; ii< metadata.getAllUniqueFragmentHolders().size(); ii++){nodes.put(metadata.getAllUniqueFragmentHolders().get(ii));}
                        obj.put(RequestTranslator.MDFSNodes, nodes);
                        allDeletedFiles.put(obj);
                    }
                }catch(Exception e){
                    try {
                        MDFSMetadata.logger.debug("EdgeKeeper local rm return -> Could not recursively delete all files in subdirectories.");
                        return RequestTranslator.errorJSON("Could not recursively delete all files in subdirectories.").toString();
                    } catch (JSONException e1) {
                        return null;
                    }
                }

                //delete all folders metadata of candidateInode
                try {
                    for (int i = 0; i < allDeletableFolderUUIDs.size(); i++) {
                        deleteMetaData(allDeletableFolderUUIDs.get(i));
                    }
                }catch(Exception e){
                    try {
                        MDFSMetadata.logger.debug("EdgeKeeper local rm return -> Could not recursively delete all sub-directories.");
                        return RequestTranslator.errorJSON("Could not recursively delete all sub-directories.").toString();
                    } catch (JSONException e1) {
                        return null;
                    }
                }

                //delete the candidateInode
                try {
                    deleteMetaData(candidateInode.getUUID());
                } catch (Exception e) {
                    try {
                        MDFSMetadata.logger.trace( "Edgekeeper local -rm return -> Recursively removed all children under folder " + folderPathMDFS + " but could not remove the folder itself.");
                        return RequestTranslator.errorJSON("Could not recursively delete all folders.").toString();
                    } catch (JSONException e1) {
                        return null;
                    }
                }

                //load parentOfLastFolderInode and delete the foldername of candidateOBj from its folder List and push back
                MDFSMetadata parentOfLastFolderInode = null;
                try {
                    parentOfLastFolderInode = retrieveMetadata(parentOfLastFolderInodeUUID);
                    parentOfLastFolderInode.removeFolder(candidateInode.getUUID());
                    storeMetaData(parentOfLastFolderInode);
                } catch (Exception e) {
                    try {
                        MDFSMetadata.logger.trace( "Edgekeeper local -rm return -> Removed all children under folder " + folderPathMDFS + " including the folder itself but could not update its parent.");
                        return RequestTranslator.errorJSON("Could not recursively delete all folders. " + " parent uuid: " + parentOfLastFolderInodeUUID).toString();
                    } catch (JSONException e1) {
                        return null;
                    }
                }


                //success return
                try {
                    JSONObject successJson = RequestTranslator.successJSON();
                    successJson.put(RequestTranslator.MDFSFileList, allDeletedFiles);
                    return successJson.toString();

                } catch (JSONException e) {
                    MDFSMetadata.logger.error("Directory deletion succeeded but could not generate success message.");
                }

            }
        }else{
            try {
                return RequestTranslator.errorJSON("Could not load root directory.").toString();
            } catch (JSONException e) {
                return null;
            }
        }

        //dummy return
        try {
            return RequestTranslator.errorJSON("Directory deletion failed.").toString();
        } catch (JSONException e) {
            return null;
        }
    }

    //recursive function
    private static void getChildOfChilOfChildAndSoAndSo(List<String> allDeletableFileUUIDs, List<String> allDeletableFolderUUIDs, MDFSMetadata currentInode) {
        try {
            //first get all the files of currentInode
            allDeletableFileUUIDs.addAll(currentInode.getAllFilesByUUID());

            //check if reached base case
            if (currentInode.getAllFoldersByUUID().size() == 0) {
                return;

            } else {

                //meaning currentInode has more folders in it.
                //get all the folder uuids of this inode and add them for deletion.
                List<String> allFolders = currentInode.getAllFoldersByUUID();
                allDeletableFolderUUIDs.addAll(allFolders);

                //recursively get all uuids of all child
                for (int i = 0; i < allFolders.size(); i++) {

                    MDFSMetadata currentInode1 = retrieveMetadata(allFolders.get(i));
                    getChildOfChilOfChildAndSoAndSo(allDeletableFileUUIDs,allDeletableFolderUUIDs, currentInode1);
                }
            }
        }catch(Exception e){
            //todo:
        }
    }


    //takes a list of MDFSMetadata objects and returns json string with success message and list metadata
    private String getString(){

        return "";
    }

    //simple tuple class in java
    public static class Pair implements Serializable {
        private String filename;
        private String fileID;

        public Pair(){}


        public Pair(String filename, String fileID){
            this.filename = filename;
            this.fileID = fileID;
        }

        public String getFilename(){
            return filename;
        }

        public String getFileid(){
            return fileID;
        }

        public void setFilename(String filename){ this.filename = filename;}

        public void setFileID(String fileid){this.fileID = fileid;}

    }
}
