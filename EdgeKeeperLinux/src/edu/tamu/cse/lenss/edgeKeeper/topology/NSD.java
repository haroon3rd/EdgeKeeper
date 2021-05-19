package edu.tamu.cse.lenss.edgeKeeper.topology;

import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;

import javax.jmdns.JmmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

public class NSD extends Thread implements Terminable {

    //service variables
    String serviceType = "_http._tcp.local.";
    String serviceName = "right_laptop_eth_rightLaptop";
    String serviceText = "right_laptop_text_eth_rightLaptop";
    int servicePort = 0;
    JmmDNS registry;
    ServiceInfo serviceInfo;

    //constructor
    public NSD(){}

    @Override
    public void terminate() {
        try {
            if (registry != null) {
                registry.unregisterAllServices();
                registry.close();
                registry = null;
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

       }catch (Exception e){
           e.printStackTrace();
       }
    }

    //ServiceListener callback functions
    private static class SampleListener implements ServiceListener {

        @Override
        public void serviceAdded(ServiceEvent event) {
            System.out.println("_NSD_ Service Added:" + " Name: " + event.getInfo().getName() + ", Text: " + new String(event.getInfo().getTextBytes()));
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            System.out.println("_NSD_ Service Removed:" + " Name: " + event.getInfo().getName() + ", Text: " + new String(event.getInfo().getTextBytes()));
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            System.out.println("_NSD_ Service Resolved:" + " Name: " + event.getInfo().getName() + ", Text: " + new String(event.getInfo().getTextBytes()));
        }

    }

    public void setText(String text){
        serviceInfo.setText(text.getBytes());
    }


}
