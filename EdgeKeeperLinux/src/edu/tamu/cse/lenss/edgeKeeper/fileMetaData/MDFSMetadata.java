package edu.tamu.cse.lenss.edgeKeeper.fileMetaData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

//this class works as the linux inode for a file or directory.
//if this class is used for file metadata, then getFileConstructor() is used.
//if this class is used for directory metadata, then getDirectory() is used.
//if this class is used for storing directory mergeData, then getMergeDataInode() is used.
//UUID and inodeUUID are synonymous.
public class MDFSMetadata {

	public static final Logger logger = Logger.getLogger(MDFSMetadata.class);

	//mdfs file/directory variables
	private String fileID;										//fileID: always unique | Replacement of fileName so that we can handle two files of same name;
	private long fileSize;										//size of the entire file
	private String fileCreatorGUID;								//GUID of device who is putting the file in MDFS
	private String requestorGUID;								//the GUID who is sending a request (currently unused field)
	private String filePathMDFS;  								//like linux directory..last entry is filename (ex: /sdcard/distressnet/file.txt)
	private boolean isGlobal;									//is this metadata global
	private List<Pair> files;									//list of UUIDs of files (if this class object is a directory)
	private List<Pair> folders;									//list of UUIDs of folders(if this class object is a directory))
	private List<MDFSMetadataBlock> blocks;						//list of File blocks (if this class object is for File)
	private String inodeUUID;									//inodeUUID: always unique | the name by which this inode will be stored at zookeeper | totally different entity than fileID
	private METADATA_TYPE metadataType;							//type of metadata object (file or directory or mergedata)
	private int n2;												//Reed-Solomon parameter
	private int k2;												//Reed-Solomon parameter
	private String mergeData;                                   //reason to use String instead of JSONObject is due to GSON conflicts with JSONObject.

	//enum for type of object
	public enum METADATA_TYPE {FILE, DIRECTORY, MERGEDATA};

	//private constructor for file
	private MDFSMetadata(String uuid, String fileID, long filesize, String fileCreatorGUID, String requestorGUID, String filePathMDFS, boolean isGlobal){
		this.inodeUUID = uuid;
		this.fileID = fileID;
		this.fileSize = filesize;
		this.fileCreatorGUID = fileCreatorGUID;
		this.requestorGUID = requestorGUID;
		this.filePathMDFS = filePathMDFS;
		this.metadataType = METADATA_TYPE.FILE;
		this.isGlobal = isGlobal;
		this.blocks = new ArrayList<>();
		this.files = null;
		this.folders = null;
		this.mergeData = null;
	}

	//private constructor for directory
	private MDFSMetadata(String uuid, String creatorGUID, String filePathMDFS, boolean isGlobal){
		this.inodeUUID = uuid;
		this.fileCreatorGUID = creatorGUID;
		this.requestorGUID = null;
		this.filePathMDFS = filePathMDFS;
		this.metadataType = METADATA_TYPE.DIRECTORY;
		this.isGlobal = isGlobal;
		this.blocks = null;
		this.files = new ArrayList<>();
		this.folders = new ArrayList<>();
		this.mergeData = null;
	}

	//private constructor for mergeData
	private MDFSMetadata(String uuid){
		this.inodeUUID = uuid;
		this.metadataType = METADATA_TYPE.MERGEDATA;
		this.mergeData = "";
		this.fileID = null;
		this.fileSize = 0;
		this.fileCreatorGUID = null;
		this.requestorGUID = null;
		this.filePathMDFS = null;
		this.isGlobal = false;
		this.files = null;
		this.folders = null;
		this.blocks = null;
		this.n2 = 0;
		this.k2 = 0;
	}

	//returns a new constructed object for mergeData
	public static MDFSMetadata createMergeDataInode(String uuid){
		return new MDFSMetadata(uuid);
	}

	//returns a new constructed object for file
	public static MDFSMetadata createFileMetadata(String uuid, String fileID, long filesize, String fileCreatorGUID, String requestorGUID, String filePathMDFS, boolean isGlobal){
		return new MDFSMetadata(uuid, fileID, filesize, fileCreatorGUID, requestorGUID, filePathMDFS, isGlobal);
	}

	//returns a new constructed object for directory
	public static MDFSMetadata createDirectoryMetadata(String uuid, String creatorGUID, String filePathMDFS, boolean isGlobal){
		return new MDFSMetadata(uuid, creatorGUID, filePathMDFS, isGlobal);
	}

	//getEdgeStatus all files names
	public List<String> getAllFilesByName() {
		List<String> fileNames = new ArrayList<>();
		for(Pair p: files){
			fileNames.add(p.getName());
		}
		return fileNames;
	}

	//get mergeData json object
	public JSONObject getMergeDataObj(){
		try {
			if(this.mergeData.equals("")){
			    return new JSONObject();
            }else {
                return new JSONObject(mergeData);
            }
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	//put mergeData string
	public void putMergeData(String data){
		this.mergeData = data;
	}

	//remove all files
	public void removeAllFiles(){
		files.clear();
	}

	//remove all directory
	public void removeAllFolders(){
		folders.clear();
	}

	//getEdgeStatus all files inodeUUID
	public List<String> getAllFilesByUUID() {
		List<String> fileUUIDs = new ArrayList<>();
		for(Pair p: files){
			fileUUIDs.add(p.getUuid());
		}
		return fileUUIDs;
	}

	//getEdgeStatus all folders names
	public List<String> getAllFoldersByName() {
		List<String> folderNames = new ArrayList<>();
		for(Pair p: folders){
			folderNames.add(p.getName());
		}
		return folderNames;
	}

	//getEdgeStatus all folders inodeUUID
	public List<String> getAllFoldersByUUID() {
		List<String> folderUUIDs = new ArrayList<>();
		for(Pair p: folders){
			folderUUIDs.add(p.getUuid());
		}
		return folderUUIDs;
	}



	//put a file in the list of files
	public void addAFileInFilesList(String filename, String uuid){
		Pair p = new Pair(filename, uuid);
		this.files.add(p);
	}

	//put a folder in the list of folders
	public void addAFolderInFoldersList(String folderName, String uuid){
		Pair p = new Pair(folderName, uuid);
		this.folders.add(p);
	}

	//check if list of files contains a file by name
	public boolean fileExists(String filename){
		for(int i = 0; i< files.size(); i++){
			if(files.get(i).getName().equals(filename)){
				return true;
			}
		}
		return false;
	}

	//check if list of folders contains a folder by name
	public boolean folderExists(String folderName){
		for(int i = 0; i< folders.size(); i++){
			if(folders.get(i).getName().equals(folderName)){
				return true;
			}
		}
		return false;
	}

	//getEdgeStatus the inodeUUID of a file by name
	public String getFileUUID(String filename){
		for(int i = 0; i< files.size(); i++){
			if(files.get(i).getName().equals(filename)){
				return files.get(i).getUuid();
			}
		}

		return null;
	}

	//getEdgeStatus the inodeUUID of a folder by name
	public String getFolderUUID(String folderName){
		for(int i = 0; i< folders.size(); i++){
			if(folders.get(i).getName().equals(folderName)){
				return folders.get(i).getUuid();
			}
		}

		return null;
	}

	//getEdgeStatus number of blocks for this file
	public int getBlockCount(){
		return this.blocks.size();
	}


	//getEdgeStatus this inodes inodeUUID
	public String getUUID(){
		return this.inodeUUID;
	}

	//remove a file from the fileLIst
	public boolean removeFile(String uuid){
		for(int i=0; i< files.size(); i++){
			if(files.get(i).getUuid().equals(uuid)){
				files.remove(i);
				break;
			}
		}
		return false;
	}

	//remove a folder from the folderList
	public boolean removeFolder(String uuid){
		for(int i=0; i< folders.size(); i++){
			if(folders.get(i).getUuid().equals(uuid)){
				folders.remove(i);
				break;
			}
		}
		return false;
	}

	//transfer data from one object to another.
	//this function only transfers block, fragments and fragmentHoldersGUID information.
	public static void transfer(MDFSMetadata source, MDFSMetadata destination){

		//getEdgeStatus all the blocks of the source
		List<MDFSMetadataBlock> blocks = source.getAllBlocks();

		//deal each block
		for(int i=0; i< blocks.size(); i++){

			//getEdgeStatus the block
			MDFSMetadataBlock block = blocks.get(i);

			//check if the block alread exists in destination
			if(destination.isBlockExists(block.getBlockNumber())){

				//getEdgeStatus all the fragments of the block
				List<MDFSMetadataFragment> fragments = block.getAllFragments();

				//deal each fragment
				for(int j=0; j< fragments.size();j++){

					//getEdgeStatus the fragment
					MDFSMetadataFragment fragment = fragments.get(j);

					//check if the fragment exist in this destination block
					if(block.isFragmentExists(fragment.getFragmentNumber())){

						//getEdgeStatus all the GUIDs for this fragment
						List<String> GUIDs = fragment.getAllFragmentHoldersGUID();

						//deal each guid
						for(int k=0; k< GUIDs.size(); k++){

							//getEdgeStatus the guid
							String guid = GUIDs.get(k);

							//addInfo into the destination
							destination.addInfo(guid, block.getBlockNumber(), fragment.getFragmentNumber());
						}
					}else{

						//this fragment doesnt exist so we completely pusht his fragment into thsi block
						block.addFrgament(fragment);
					}

				}
			}else{

				//block doesnt exist,s o we complete push it as new block
				destination.addBlock(block);
			}

		}
	}



	//add information to the metadata object
	public void addInfo(String guid, int blocknum, int fragmentnum){

		//name check if a block exists of this blockNumber
		MDFSMetadataBlock block = this.getBlock(blocknum);

		if(block!=null){

			//block exists
			//check if a fragment of this fragmentnum exists
			MDFSMetadataFragment fragment = block.getFragment(fragmentnum);

			if(fragment!=null){

				//fragment exists
				//check if guid exists in this fragment
				if(!fragment.isGUIDexists(guid)){

					//guid doesnt exist, so we need to add
					fragment.addAFragmentHolder(guid);
				}
			}else{

				//fragment doesnt exist,
				// so we create a fragment,
				// add guid in it,
				// and add the fragment into the block.
				fragment = new MDFSMetadataFragment(fragmentnum, blocknum);
				fragment.addAFragmentHolder(guid);
				block.addFrgament(fragment);
			}
		}else{

			//block doesnt exist so , need to create everything new
			//create a new fragment object
			MDFSMetadataFragment fragment = new MDFSMetadataFragment(fragmentnum, blocknum);

			//add fragment holder guid into fragment object
			fragment.addAFragmentHolder(guid);

			//create a new block object
			block = new MDFSMetadataBlock( blocknum);

			//push the fragment into the block
			block.addFrgament(fragment);

			//push the block in the file metadata object
			this.addBlock(block);

		}
	}

	//getEdgeStatus a block by blockNumber
	public MDFSMetadataBlock getBlock(int blockNumber){
		for(int i = 0; i< blocks.size(); i++){
			if(blocks.get(i).getBlockNumber()==blockNumber){
				return blocks.get(i);
			}
		}
		return null;
	}

	//check if a block exists
	public boolean isBlockExists(int blockNumber){
		for(int i = 0; i< blocks.size(); i++){
			if(blocks.get(i).getBlockNumber()==blockNumber){
				return true;
			}
		}
		return false;
	}

	//getEdgeStatus all block object of this file
	public List<MDFSMetadataBlock> getAllBlocks(){
		return this.blocks;
	}

	//add a block object into the list of blocks
	public void addBlock(MDFSMetadataBlock block){
		this.blocks.add(block);
	}

	//convert class object to json string
	public String fromClassObjecttoJSONString(MDFSMetadata metadata){
		Gson gson = new GsonBuilder()
				.serializeNulls()
				.serializeSpecialFloatingPointValues() 
				.create();

		return gson.toJson(metadata).toString();
		//return new Gson().toJson(metadata).toString();
	}

	//Suman wrote this function
	public byte[] getBytes(){
		Gson gson = new GsonBuilder()
				.serializeNulls()
				.serializeSpecialFloatingPointValues() 
				.create();

		return gson.toJson(this).getBytes();
		//return (new Gson()).toJson(this).getBytes();
	}

	//convert json string to class object
	public static MDFSMetadata createMetadataFromBytes(byte[] bytes) {
		Gson gson = new GsonBuilder()
				.serializeNulls()
				.serializeSpecialFloatingPointValues() 
				.create();
		
		return gson.fromJson(new String(bytes), MDFSMetadata.class);
		//return new Gson().fromJson(new String(bytes), MDFSMetadata.class);
	}

	//is directory
	public boolean isDirectory(){
		if(metadataType== METADATA_TYPE.DIRECTORY){
			return true;
		}
		return false;
   }



	//returns a list of unique GUIDs who have one/any fragment of this file
	public List<String> getAllUniqueFragmentHolders(){
		List<String> allGUIDs = new ArrayList<>();

		//iterate all blocks
		for(int i=0; i< getBlockCount(); i++){

			//getEdgeStatus each block
			MDFSMetadataBlock block = blocks.get(i);

			//getEdgeStatus list of all fragments of this block
			List<MDFSMetadataFragment> allFragments = block.getAllFragments();

			//iterate all fragments
			for(int j=0; j<allFragments.size(); j++){

				//getEdgeStatus each fragment
				MDFSMetadataFragment fragment = allFragments.get(j);

				//getEdgeStatus list of all guids of this fragment
				List<String> guids = fragment.getAllFragmentHoldersGUID();

				//iterate all guids
				for(int k=0; k< guids.size(); k++){

					//getEdgeStatus each guid
					String guid = guids.get(k);

					//check and add unique guid
					if(!allGUIDs.contains(guid)){
						allGUIDs.add(guid);
					}
				}
			}
		}

		return allGUIDs;
	}


	//takes a GUID and returns the blockNumbers carried by this GUID for this file
	public List<String> getBlockNumbersHeldByNode(String GUID){
		List<String> blockNummbers = new ArrayList<>();

		//iterate all blocks
		for(int i=0; i< getBlockCount(); i++){

			//getEdgeStatus each block
			MDFSMetadataBlock block = blocks.get(i);

			//getEdgeStatus list of all fragments of this block
			List<MDFSMetadataFragment> allFragments = block.getAllFragments();

			//iterate all fragments
			for(int j=0; j<allFragments.size(); j++){

				//getEdgeStatus each fragment
				MDFSMetadataFragment fragment = allFragments.get(j);

				//getEdgeStatus list of all guids of this fragment
				List<String> guids = fragment.getAllFragmentHoldersGUID();

				//iterate all guids
				for(int k=0; k< guids.size(); k++){

					//getEdgeStatus each guid
					String guid = guids.get(k);

					//check and add unique blockNumber
					if(guid.equals(GUID)){
						blockNummbers.add(Integer.toString(block.getBlockNumber()));
					}
				}
			}
		}

		return blockNummbers;
	}

	//takes a GUID and a block number and returns the list of fragments of this block this node contains for this file
	public List<String> getFragmentListByNodeAndBlockNumber(String guid, String blocknum){
		List<String> fragmentNumbers = new ArrayList<>();

		//iterate all blocks
		for(int i=0; i< getBlockCount(); i++){

			//getEdgeStatus each block
			MDFSMetadataBlock block = blocks.get(i);

			//check block index matches
			if(block.getBlockNumber()== Integer.parseInt(blocknum)){

				//getEdgeStatus all fragments
				List<MDFSMetadataFragment> allFragments = block.getAllFragments();

				//iterate through each fragment
				for(int j=0; j< allFragments.size(); j++){

					//getEdgeStatus each fragment
					MDFSMetadataFragment fragment = allFragments.get(j);

					//getEdgeStatus a list of all guids of this fragment
					List<String> guids = fragment.getAllFragmentHoldersGUID();

					//check if guid exists in this fragment
					if(guids.contains(guid)){
						fragmentNumbers.add(Integer.toString(fragment.getFragmentNumber()));
					}
				}
			}
		}

		return fragmentNumbers;
	}

	public String getRequestorGUID() {
		return requestorGUID;
	}

	public String getFilePathMDFS() {
		return filePathMDFS;
	}

	public String getFileCreatorGUID() {
		return fileCreatorGUID;
	}

	public long getFileSize() {
		return fileSize;
	}

	//getEdgeStatus filename
	public String getFileName(){

		//name tokenize the filePathMDFS
		String[] tokens = filePathMDFS.split(File.separator);

		//delete empty strings
		tokens = delEmptyStr(tokens);

		//return the last element
		return tokens[tokens.length-1];
	}

	public String getFileID() {
		return this.fileID;
	}

	public boolean isGlobal() {
		return isGlobal;
	}

	//set n2 parameter
	public void setn2(int n2){
		this.n2 = n2;
	}

	//set k2 parameter
	public void setk2(int k2){
		this.k2 = k2;
	}

	public int getn2() {
		return n2;
	}

	public int getk2() {
		return k2;
	}

	//simple tuple class in java
	class Pair{
		String name;
		String uuid;

		public Pair(String name, String uuid){
			this.name = name;
			this.uuid = uuid;
		}

		public String getName(){
			return name;
		}

		public String getUuid(){
			return uuid;
		}
	}

    //eliminates empty string tokens
    public static String[] delEmptyStr(String[] tokens){
        List<String> newTokens = new ArrayList<>();
        for(String token: tokens){
            if(!token.equals("")){
                newTokens.add(token);
            }
        }

        return newTokens.toArray(new String[0]);
    }


}

