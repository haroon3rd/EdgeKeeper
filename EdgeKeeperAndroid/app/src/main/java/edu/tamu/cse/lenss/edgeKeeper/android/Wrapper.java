package edu.tamu.cse.lenss.edgeKeeper.android;

public class Wrapper {

    public boolean cloudConnected;
    public boolean zkClientConnected;

    public Wrapper(boolean cloudConnected, boolean zkClientConnected){
        this.cloudConnected = cloudConnected;
        this.zkClientConnected = zkClientConnected;
    }

}
