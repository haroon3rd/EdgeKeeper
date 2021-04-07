package edu.tamu.cse.lenss.edgeKeeper.android;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.edgeKeeper.server.EKHandler;
import edu.tamu.cse.lenss.edgeKeeper.server.EdgeStatus;
import edu.tamu.cse.lenss.edgeKeeper.topology.TopoGraph;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKProperties;

//this class artificially puts data in the GridViewQueue.java so that MainActivity can consume and display it.
//delete this class after development is done
public class testThread implements Runnable {

    @Override
    public void run() {

        try {
            while (true) {

                //sleep
                Thread.sleep(3000);

                //get all ips from topology
                TopoGraph tpgrph = EKClient.getNetworkInfo();
                Set<String> ips = tpgrph.getAllIPs();
                for(String ip: ips){
                    System.out.println("XYZ topology (IP to name) " + EKClient.getAccountNamebyIP(ip).get(0) + " ");
                }

                System.out.println("XYZ ");

                //valid replica reached FORMED
                EdgeStatus status = EKHandler.edgeStatus;
                if(status!=null){
                    Set<String> replicaGUIDs = status.replicaMap.keySet();
                    for(String guid: replicaGUIDs){
                        System.out.println("XYZ replica (guid to name) " + EKClient.getAccountNamebyGUID(guid));
                    }
                }

                System.out.println("XYZ ");
                System.out.println("XYZ ");
                System.out.println("XYZ ");

            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


}