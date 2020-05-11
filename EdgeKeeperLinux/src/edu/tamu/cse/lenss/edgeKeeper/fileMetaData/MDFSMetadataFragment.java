package edu.tamu.cse.lenss.edgeKeeper.fileMetaData;

import java.util.ArrayList;
import java.util.List;

public class MDFSMetadataFragment {

    //variables
    private int fragmentNumber;
    private int blockNumber;
    private List<String> fragmentHolderGUIDs;

    //constructor
    public MDFSMetadataFragment(int fragmentNumber, int blockNumber){
        this.fragmentNumber = fragmentNumber;
        this.blockNumber = blockNumber;
        this.fragmentHolderGUIDs = new ArrayList<>();
    }

    //add a fragmentHolderGUID to the list
    public void addAFragmentHolder(String GUID){
        //check if the GUID already exists
        if(!fragmentHolderGUIDs.contains(GUID)) {
            this.fragmentHolderGUIDs.add(GUID);
        }
    }

    //getEdgeStatus the GUIDs of who holds this fragment
    public List<String> getAllFragmentHoldersGUID(){
        return this.fragmentHolderGUIDs;
    }

    //getEdgeStatus fragmentNumber of this fragment
    public int getFragmentNumber(){
        return fragmentNumber;
    }

    //check if a uuid already exists
    public boolean isGUIDexists(String UUID) {
        for (int i = 0; i < fragmentHolderGUIDs.size(); i++) {
            if (fragmentHolderGUIDs.get(i).equals(UUID)) {
                return true;
            }
        }
        return false;
    }
}
