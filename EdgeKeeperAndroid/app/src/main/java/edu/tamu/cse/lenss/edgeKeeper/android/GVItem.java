package edu.tamu.cse.lenss.edgeKeeper.android;


import android.content.Context;
import android.view.View;

//wrapper class for handling gridview items
public class GVItem {


    private boolean CONNECTED;
    private String NAME;
    private boolean pinned;
    private Context context;
    private View view;

    public GVItem(boolean connected, String name, boolean pinned, Context context, View view) {
        this.CONNECTED = connected;
        this.NAME = name;
        this.pinned = pinned;
        this.context = context;
        this.view = view;
    }

    public boolean isCONNECTED() {
        return this.CONNECTED;
    }

    public void setCONNECTED(boolean conn) {
        this.CONNECTED = conn;
    }

    public String getNAME (){return this.NAME;}

    public void setNAME(String name){
        this.NAME = name;
    }

    public void setPinned(boolean pin){
        this.pinned = pin;
    }

    public boolean isPinned(){
        return this.pinned;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public View getView() {
        return view;
    }

    public void setView(View view) {
        this.view = view;
    }
}
