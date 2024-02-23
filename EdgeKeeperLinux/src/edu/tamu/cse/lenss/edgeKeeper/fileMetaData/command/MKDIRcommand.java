package edu.tamu.cse.lenss.edgeKeeper.fileMetaData.command;

import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MDFSMetadata;
import edu.tamu.cse.lenss.edgeKeeper.server.RequestTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MDFSMetadata.delEmptyStr;
import static edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MetaDataHandler.*;

public class MKDIRcommand {


    //variables
    public final static String SUCCESS = "SUCCESS";
    public final static String ERROR = "ERROR";
    public final static String DIRALREADYEXISTS = "DIRALREADYEXISTS";

    //takes a dirctory and makes it.
    //this function returns error if the directory already exists.
    public static String mkdir(String creatorGUID, String folderPathMDFS, String isGlobal){

        MDFSMetadata.logger.trace("EdgeKeeper local start to process mkdir command.");

        //check for root
        if(folderPathMDFS.equals(File.separator)){
            try {
                MDFSMetadata.logger.debug("EdgeKeeper local mkdir return -> Root directory already exists.");
                return RequestTranslator.errorJSON("Root directory already exists.").toString();
            } catch (JSONException e) {
                return null;
            }
        }

        //check isGlobal
        boolean IsGlobal = false;
        if(isGlobal.equals(RequestTranslator.TRUE)){
            IsGlobal = true;
        }else if(isGlobal.equals(RequestTranslator.FALSE)){
            IsGlobal = false;
        }

        pair_mkdir mkdirRet = mkdirWithReturn(creatorGUID, folderPathMDFS, IsGlobal);
        if(!mkdirRet.getMessage().equals(ERROR)){

            //check if SUCCESS
            if(mkdirRet.getMessage().equals(SUCCESS)){
                try {
                    MDFSMetadata.logger.trace("EdgeKeeper local mkdir return -> successfully handled mkdir command");
                    return RequestTranslator.successJSON().toString();
                } catch (JSONException e) {
                    return null;
                }
            }else if(mkdirRet.getMessage().equals(DIRALREADYEXISTS)){
                try {
                    MDFSMetadata.logger.trace("EdgeKeeper local mkdir return -> Directory already exists.");
                    return RequestTranslator.errorJSON("Directory already exists.").toString();
                } catch (JSONException e) {
                    return null;
                }
            }
        }else{
            try {
                MDFSMetadata.logger.debug("EdgeKeeper local mkdir return -> Failed to create Directory.");
                return RequestTranslator.errorJSON("Failed to create Directory.").toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return null;
    }


    //handle mkdirWithReturn command.
    //returns either object or null.
    //ex: if input folderPathMDFS /a/b/c/, then it will create the folders and return the inode for /c.
    //if partial path already exists, then this function will create the remaining folders.
    //(ex: input folderPathMDFS /a/b/c/ but /a/b/ already exists, then it will only create folder /c).
    //if full path already exists, it will not change anything but return the last inode.
    public static pair_mkdir mkdirWithReturn(String creatorGUID, String folderPathMDFS, boolean isGlobal){

        //tokenize the filepath
        //tokens contains only folder names as elements
        String[] tokens = folderPathMDFS.split(File.separator);

        //remove empty strings
        tokens = delEmptyStr(tokens);

        //first get the root inode object
        try {
            MDFSMetadata metadataRootInode = retrieveMetadata(uuid_root);

            //check if retrieve failed
            if(metadataRootInode!=null){

                //check is asked for root inode
                if(folderPathMDFS.equals(File.separator)){
                    return new pair_mkdir(DIRALREADYEXISTS, metadataRootInode);
                }

                //at this point we have the root inode
                //recursively check how many folders from the tokens array already exists.
                boolean pathExists = false;
                int candidateIndex = 0;
                MDFSMetadata candidateInode = metadataRootInode;
                String nextUUID;
                while(true){
                    if(tokens.length>candidateIndex) {

                        //check if tokens[candidateIndex] exists as a child folder of candidateObj
                        if (candidateInode.folderExists(tokens[candidateIndex])) {

                            //get the uuid of the child folder
                            nextUUID = candidateInode.getFolderUUID(tokens[candidateIndex]);

                            //get the child folder
                            candidateInode = null;
                            candidateInode = retrieveMetadata(nextUUID);

                            //check if child retrieve succeeded
                            if (candidateInode != null) {

                                //increment candidateIndex
                                candidateIndex++;

                            } else {
                                return null;
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
                        //at this point, candidateIndex valus is tokens.length, and
                        //candidateObj points to the last folder of given folderPathMDFS
                        pathExists = true;
                        break;
                    }

                }

                //check if filepath already exists
                if(!pathExists){

                    //list of folder inodes for all non-existing folders
                    List<MDFSMetadata> folderInodes = new ArrayList<>();


                    //recursively create inodes which yet dont exist
                    String nextInodeUUID  = createUUID();
                    for(int i=candidateIndex; i< tokens.length; i++){

                        //create folder path
                        //folderPath contains a slash at the end.
                        String folderpath = File.separator;
                        for(int j=0; j<=i; j++){ folderpath = folderpath + tokens[j] + File.separator; }

                        //create this folder inode
                        MDFSMetadata newInode = MDFSMetadata.createDirectoryMetadata(nextInodeUUID, creatorGUID, folderpath, isGlobal);

                        //add tokens[i+1] as its child.
                        //the last inode will not have any child.
                        //then we store child's inode for next iteration.
                        if(i!=tokens.length-1){
                            nextInodeUUID = createUUID();
                            newInode.addAFolderInFoldersList(tokens[i+1], nextInodeUUID);

                        }

                        //store the inode in arraylist
                        folderInodes.add(newInode);

                        //add the first-non-existing-folder name and uuid as a child to its previous last-existing-folder-inode.
                        //ex: if user wants to mkdir /a/b/c/ and /a already exists, then we need to create folders /b and /c,
                        //then folder /b's name and uuid should be added to folder /a as child.
                        if(i==candidateIndex){
                            candidateInode.addAFolderInFoldersList(tokens[candidateIndex], newInode.getUUID());
                        }

                    }

                    //push back last-existing-folder-inode back
                    storeMetaData(candidateInode);

                    //push all the new folder inodes from the arraylist
                    for(int i=0; i< folderInodes.size(); i++){
                        storeMetaData(folderInodes.get(i));
                    }

                    //return the last inode from the arrayList
                    return new pair_mkdir(SUCCESS, folderInodes.get(folderInodes.size()-1));

                }else{
                    //path already exists so we return the inode candidateInode variable is pointing at( aka the last folder of folderPathMDFS)
                    return new pair_mkdir(DIRALREADYEXISTS, candidateInode);
                }
            }else{
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }


    //this clas sis only used in this file.
    //this class is used for returning result from mkdir functions.
    static class pair_mkdir{
        String message;
        MDFSMetadata inode;

        public pair_mkdir(String msg, MDFSMetadata inode){
            this.message = msg;
            this.inode = inode;
        }

        public String getMessage(){
            return message;
        }

        public MDFSMetadata getInode(){
            return inode;
        }

    }
}
