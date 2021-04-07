package edu.tamu.cse.lenss.edgeKeeper.android;

public class Wrapper {

    public int GNSConnected;
    public int zkClientConnected;
    public int zkServerConnection;

    public Wrapper(int GNSConnected, int zkClientConnected, int zkServerConnection){
        this.GNSConnected = GNSConnected;
        this.zkClientConnected = zkClientConnected;
        this.zkServerConnection = zkServerConnection;
    }

}
