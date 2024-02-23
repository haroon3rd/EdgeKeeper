package edu.tamu.cse.lenss.edgeKeeper.fileMetaData.command;

import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MDFSMetadata;
import edu.tamu.cse.lenss.edgeKeeper.server.RequestTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONException;

import java.io.File;

import static edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MDFSMetadata.delEmptyStr;
import static edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MetaDataHandler.*;

public class GETcommand {


    //handle get command
    //input: filepathMDFS must starts with a slash and,
    // has a valid fileName with extension as the last token(verified beforehand)
    //output: Json string that contains success+metadata or error messages
    public static String get(String filepathMDFS){

        MDFSMetadata.logger.trace("EdgeKeeper local start to process get command.");

        //tokenize the filePathMDFS
        //tokens contains folder names as elements but the last element is the fileName itself
        String[] tokens = filepathMDFS.split(File.separator);

        //delete empty string entries
        tokens = delEmptyStr(tokens);

        //get the metadata inode for the root folder
        MDFSMetadata metadataRootInode = null;
        try {
            metadataRootInode = retrieveMetadata(uuid_root);
        } catch (Exception e) {
            try {
                MDFSMetadata.logger.debug("EdgeKeeper local -get return -> Root inode retrieve error.", e);
                return RequestTranslator.errorJSON("Root inode retrieve error.").toString();
            } catch (JSONException e1) {
                return null;
            }
        }

        if(metadataRootInode!=null){

            //at this point We have the root inode
            //run the while loop to recursively parse child of child of child folders and so..
            //ex:if filepathMDFS was /a/b/c/file.txt, then we check folders a, b, c exists
            int candidateIndex = 0;
            MDFSMetadata candidateInode = metadataRootInode;
            String candidateUUID;
            while(true){
                if(tokens.length -1 > candidateIndex){

                    //check if tokens[candidateIndex] exists as a child folder of candidateInode
                    if(candidateInode.folderExists(tokens[candidateIndex])){

                        //get the uuid of the child folder
                        candidateUUID = candidateInode.getFolderUUID(tokens[candidateIndex]);

                        //get the child folder
                        try {
                            candidateInode = null;
                            candidateInode = retrieveMetadata(candidateUUID);

                            //check if child retrieve succeeded
                            if(candidateInode!=null){

                                //increment candidateIndex
                                candidateIndex++;

                            }else{
                                try {
                                    MDFSMetadata.logger.debug("EdgeKeeper local -get return -> Could not load directory.");
                                    return RequestTranslator.errorJSON("Could not load directory.").toString();
                                } catch (JSONException e) {
                                    return null;
                                }
                            }
                        }catch(Exception e){
                            try {
                                return RequestTranslator.errorJSON("Could not load directory.").toString();
                            } catch (JSONException ee) {
                                return null;
                            }
                        }
                    }else{
                        try {
                            MDFSMetadata.logger.debug("EdgeKeeper local -get return -> Directory doesnt exist.");
                            return RequestTranslator.errorJSON("Directory doesnt exist.").toString();
                        } catch (JSONException e) {
                            return null;
                        }
                    }

                }else{
                    //break while as index reaches the last token, that is fileName
                    break;
                }
            }

            //at this point candidateInode points to the last folder of filepathMDFS,
            //and candidateIndex points to the last index of tokens[].
            //check if candidateInode contains the file, aka the last elem of tokens[].
            if(candidateInode!=null && candidateInode.fileExists(tokens[candidateIndex])){
                try {

                    //get the file metadata inode
                    MDFSMetadata fileMetadataInode = retrieveMetadata(candidateInode.getFileUUID(tokens[tokens.length-1]));

                    //check if file inode retrieve succeeded
                    if(fileMetadataInode!=null){

                        //convert inode into String
                        String FileMetadataStr = fileMetadataInode.fromClassObjecttoJSONString(fileMetadataInode);

                        //return success Json with file metadata string in it
                        MDFSMetadata.logger.trace("EdgeKeeper local -get return -> Successfully handled get command.");
                        return RequestTranslator.successJSON().put(RequestTranslator.MDFSmetadataField, FileMetadataStr).toString();

                    }else{
                        try {
                            MDFSMetadata.logger.debug("EdgeKeeper local -get return -> Could not retrieve file metadata.");
                            return RequestTranslator.errorJSON("Could not retrieve file metadata.").toString();
                        } catch (JSONException ee) {
                            return null;
                        }
                    }
                } catch (Exception e) {
                    try {
                        return RequestTranslator.errorJSON("Could not retrieve file metadata").toString();
                    } catch (JSONException ee) {
                        return null;
                    }
                }

            }else{
                try {
                    MDFSMetadata.logger.debug("EdgeKeeper local -get return -> file doesnt exist in directory.");
                    return RequestTranslator.errorJSON("File doesnt exist in directory.").toString();
                } catch (JSONException e) {
                    return null;
                }
            }
        }else{
            try {
                MDFSMetadata.logger.debug("EdgeKeeper local -get return -> Could not load root directory.");
                return RequestTranslator.errorJSON("Could not load root directory.").toString();
            } catch (JSONException e) {
                return null;
            }
        }
    }
}
