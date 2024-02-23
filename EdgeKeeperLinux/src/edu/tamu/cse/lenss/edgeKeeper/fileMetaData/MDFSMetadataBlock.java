package edu.tamu.cse.lenss.edgeKeeper.fileMetaData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class MDFSMetadataBlock {
    //variables
    private int blockNumber;
    private List<MDFSMetadataFragment> fragments;

    //constructor
    public MDFSMetadataBlock(int blockNumber){
        this.blockNumber = blockNumber;
        this.fragments = new ArrayList<>();
    }

    //getEdgeStatus blockNumber of this block
    public int getBlockNumber(){
        return blockNumber;
    }

    //getEdgeStatus all fragments of this block
    public List<MDFSMetadataFragment> getAllFragments(){
        return this.fragments;
    }

    //getEdgeStatus count of fragments for this block
    public int getFragmentCount(){
        return getAllFragments().size();
    }

    //put a fragment in the block
    public void addFrgament(MDFSMetadataFragment fragment){
        this.fragments.add(fragment);
    }

    //check if a fragment already exists
    public boolean isFragmentExists(int fragmentNumber){
        for(int i=0; i< fragments.size(); i++){
            if(fragments.get(i).getFragmentNumber()==fragmentNumber){
                return true;
            }
        }
        return false;
    }

    //getEdgeStatus fragment at a given index
    public MDFSMetadataFragment getFragment(int fragmentNumber){
        for(int i=0; i< fragments.size(); i++){
            if(fragments.get(i).getFragmentNumber()==fragmentNumber){
                return fragments.get(i);            }
        }
        return null;
    }



}
