package edu.tamu.cse.lenss.edgeKeeper.fileMetaData.command;


import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MDFSMetadata;
import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MetaDataHandler;
import edu.tamu.cse.lenss.edgeKeeper.server.RequestTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bouncycastle.cert.ocsp.Req;
import org.json.JSONException;

import java.io.File;
import java.util.Arrays;

import static edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MDFSMetadata.delEmptyStr;
import static edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MetaDataHandler.*;
import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.command.MKDIRcommand.*;

public class PUTcommand {


    //handle put command
    //input: metadata as Json String
    //output: success or error message
    public static String put(String metadataStr){

        MDFSMetadata.logger.trace("EdgeKeeper local start to process put command.");

        //convert metadataStr into metadataNewInode
        MDFSMetadata metadataNewInode = MDFSMetadata.createMetadataFromBytes(metadataStr.getBytes());

        //check if Gson failed
        if(metadataNewInode==null){
            try {
                MDFSMetadata.logger.debug("EdgeKeeper local put return -> Gson error on root inode.");
                return RequestTranslator.errorJSON("System error.").toString();
            } catch (JSONException e) {
                return null;
            }
        }

        //getEdgeStatus the uuid of this file
        String uuid = metadataNewInode.getUUID();

        //first check if a metadata of this uuid already exists
        boolean metadataAlreadyExists = false;
        try {
            if(checkExists(uuid)){
                metadataAlreadyExists = true;
            }
        } catch (Exception e) {
            MDFSMetadata.logger.debug("EdgeKeeper local info -> Check for old existing metadata failed.");
        }

        //metadata already exists, that means this is a put request by one of the fragment receiver.
        //in this case, we pull the existing metadata, add more data in it, and push back.
        if(metadataAlreadyExists){

            //fetch the old metadata
            MDFSMetadata metadataOldInode = null;
            try {
                metadataOldInode = retrieveMetadata(uuid);
            } catch (Exception e) {
                try {
                    MDFSMetadata.logger.debug("Edgekeepr local put return -> Failed to fetch old metadata.");
                    return RequestTranslator.errorJSON("Failed to fetch old metadata.").toString();
                } catch (JSONException e1) {
                    return null;
                }
            }

            //check if old metadata obj retrieval succeeded
            if (metadataOldInode != null) {

                //transfer data from new object to old object
                MDFSMetadata.transfer(metadataNewInode, metadataOldInode);

                //store back old metadata obj
                try {
                    storeMetaData(metadataOldInode);
                    MDFSMetadata.logger.trace("EdgeKeeper local put return -> successfully handled put command and updated metadata.");
                    return RequestTranslator.successJSON().toString();
                } catch (Exception e) {
                    try {
                        MDFSMetadata.logger.debug("EdgeKeeper local put return -> Could not add additional file metadata to the existing metadata.");
                        return RequestTranslator.errorJSON("Could not add additional file metadata to the existing metadata.").toString();
                    } catch (JSONException e1) {
                        return null;
                    }
                }

            } else {
                try {
                    MDFSMetadata.logger.debug("EdgeKeeper local put return -> Failed to fetch old metadata");
                    return RequestTranslator.errorJSON("Failed to fetch old metadata.").toString();
                } catch (JSONException e) {
                    return null;
                }
            }


        }else{

            //metadata doesnt exist, that means it is a brand new put request for that file.
            //in this case, we push folder metadata along the way, and at the end, we push file metadata.
            String filePathMDFS = metadataNewInode.getFilePathMDFS();
            String creatorGUID = metadataNewInode.getFileCreatorGUID();
            boolean isGlobal = metadataNewInode.isGlobal();
            String fileUUID = metadataNewInode.getUUID();

            //tokenize the filePathMDFS
            //token contains folder names but the last entry is filename
            String[] tokens = filePathMDFS.split(File.separator);

            //remove empty strings
            tokens = delEmptyStr(tokens);


            //make folder path of the file
            //ex: if filePathMDFS is /a/b/c/file.txt, then we make a path /a/b/c/
            String folderPathMDFS = File.separator;
                for (int i = 0; i < tokens.length-1; i++) {
                    folderPathMDFS = folderPathMDFS + tokens[i] + File.separator;
                }


            //call mkdirWithReturn() to create the folderPathMDFS in MDFS system.
            //ex: if input folderPathMDFS /a/b/c/, then it will create the folders.
            //if partial path already exists, then this function will create the remaining folders.
            //(ex: input folderPathMDFS /a/b/c/ but /a/b/ already exists, then it will only create folder /c)
            //if full path already exists, it will not change anything.
            pair_mkdir mkdirRet = MKDIRcommand.mkdirWithReturn(creatorGUID, folderPathMDFS, isGlobal);

            //check mkdir return and getEdgeStatus inode
            MDFSMetadata lastFolderInode = null;
            if(mkdirRet.getMessage().equals(MKDIRcommand.SUCCESS) || mkdirRet.getMessage().equals(MKDIRcommand.DIRALREADYEXISTS)){
                lastFolderInode = mkdirRet.getInode();
            }

            //check if mkdirWithReturn succeeded
            if(lastFolderInode!=null){

                //add file as child of the lastFolderInode with its name and UUID
                lastFolderInode.addAFileInFilesList(tokens[tokens.length-1], fileUUID);

                //push the lastFolderInode back
                boolean lastFolderInodePushed = false;
                try {
                    storeMetaData(lastFolderInode);
                    lastFolderInodePushed = true;
                } catch (Exception e) {
                    try {
                        MDFSMetadata.logger.debug("EdgeKeeper local put return -> Failed to add file as a child to the final directory folder.");
                        return RequestTranslator.errorJSON("Failed to add file as a child to the final directory folder.").toString();
                    } catch (JSONException e1) {
                        return null;
                    }
                }


                //push the file inode only if lastFolderInode had been pushed without error
                if(lastFolderInodePushed) {
                    try {
                        storeMetaData(metadataNewInode);
                        MDFSMetadata.logger.trace("EdgeKeeper local put return ->   successfully handled put command and created new file metadata.");
                        return RequestTranslator.successJSON().toString();
                    } catch (Exception e) {
                        try {
                            MDFSMetadata.logger.debug("EdgeKeeper local put return ->  Failed to push file metadata but directory was created." + e);
                            return RequestTranslator.errorJSON("Failed to push file metadata (last folder directory may contain filename as a child)").toString();
                        } catch (JSONException e1) {
                            return null;
                        }
                    }
                }

            }else{
                try {
                    MDFSMetadata.logger.debug("EdgeKeeper local put return ->  Could not load/create directory path.");
                    return RequestTranslator.errorJSON("Could not load/create directory path.").toString();
                } catch (JSONException e) {
                    return null;
                }
            }

        }

        return null;
    }
}
