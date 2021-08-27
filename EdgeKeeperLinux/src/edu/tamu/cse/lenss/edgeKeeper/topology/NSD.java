package edu.tamu.cse.lenss.edgeKeeper.topology;

import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;

import javax.jmdns.JmmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.util.ArrayList;
import java.util.List;


public class NSD extends Thread implements Terminable {

    private List<String> allDiscoveredIPs;

    //service variables
    String serviceType = "_http._tcp.local.";
    String serviceName = "camry10";
    String serviceText = "000.000.000.000:000.000.000.000:XXXXXXXXXXAAAAAAAAAABBBBBBBBBBCCCCCCCCCC";
    int servicePort = 0;
    JmmDNS registry;
    ServiceInfo serviceInfo;

    //constructor
    public NSD(){
        allDiscoveredIPs = new ArrayList<>();
    }

    @Override
    public void terminate() {
        try {
            if (registry != null) {
                registry.unregisterAllServices();
                registry.close();
            }
        } catch (Exception ex) {

        }
    }

    @Override
    public void run(){
       try{
           //get new jmmdns instance
           registry = JmmDNS.Factory.getInstance();

           //sampleListener
           SampleListener sampleListener = new SampleListener();

           //serviceinfo
           serviceInfo = ServiceInfo.create(serviceType, serviceName, servicePort, 1, 1, true,  serviceText);

           //register service
           registry.registerService(serviceInfo);

           //add service listener
           registry.addServiceListener(serviceType, sampleListener);

           //enable lte support
           //ServiceInfo serviceInfoLTE = ServiceInfo.create("LTE_LTE_LTE", "LTE_LTE_LTE", servicePort, 1, 1, true,  "LTE_LTE_LTE");
           //registry.enableLTEsupport(serviceInfoLTE, sampleListener);

       }catch (Exception e){
           e.printStackTrace();
       }
    }

    //ServiceListener callback functions
    //when these functions are called automatically: any network change and service found
    //when I should manually restart this service: when my Account/Setting changed
    private static class SampleListener implements ServiceListener {

        @Override
        public void serviceAdded(ServiceEvent event) {
            System.out.println("jmdns _NSD_ Service Added:" + " Name: " + event.getInfo().getName() + ", Text: " + new String(event.getInfo().getTextBytes()));
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            System.out.println("jmdns _NSD_ Service Removed:" + " Name: " + event.getInfo().getName() + ", Text: " + new String(event.getInfo().getTextBytes()));
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            System.out.println("jmdns _NSD_ Service Resolved:" + " Name: " + event.getInfo().getName() + ", Text: " + new String(event.getInfo().getTextBytes()));
        }

    }

    public void setText(String text){
        serviceInfo.setText(text.getBytes());
    }



}
