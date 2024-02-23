package edu.tamu.cse.lenss.edgeKeeper.jmmDNSPlus;

//import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.LoggerFactory;

import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;
import javax.jmdns.*;

public class JmmDNSPlusHandler implements Terminable  {
	
	static org.slf4j.Logger logger = LoggerFactory.getLogger(JmmDNSPlusHandler.class.getName());
//	public static final Logger logger = LoggerFactory.getLogger(JmmDNSPlusHandler.class);

	//service variables
    private String serviceType = "_http._tcp.local.";
    private String serviceName = "desktop_instance";
    private String serviceText = "GUID000AAA";
    int servicePort = 0;
    private JmmDNS registry;
    private ServiceInfo serviceInfo;
    private SampleListener sampleListener;

//    public JmmDNSPlusHandler(EKHandler ekHandler) {
//		// TODO Auto-generated constructor stub
//	}


	//constructor
    public JmmDNSPlusHandler(String serviceType, String serviceName, String serviceText ){
    	this.serviceName = serviceName;
    	this.serviceText = serviceText;
    	this.serviceType = serviceType;
    }
    
    public JmmDNSPlusHandler(){
    }


    @Override
    public void run(){
        try{
        	logger.debug("Starting jmmDNS Service");
            //get new jmmdns instance
            registry = JmmDNS.Factory.getInstance();

            //sampleListener
            sampleListener = new SampleListener();

            //serviceinfo
            
            logger.debug("AMRAN_JMMDNS" + serviceType +" "+ serviceName +" "+ servicePort);
            
            serviceInfo = ServiceInfo.create(serviceType, serviceName, servicePort, 1, 1, false,  encodeServiceText(serviceText));
            
            logger.debug("AMRAN_JMMDNS - Trying to register: " + serviceInfo);
            
            //register service
            registry.registerService(serviceInfo);
            logger.debug("AMRAN_JMMDNS - Registred with info: " + serviceInfo);

            //add service listener
            registry.addServiceListener(serviceType, sampleListener);
            logger.debug("AMRAN_JMMDNS - Adding service listener");

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //ServiceListener callback functions
    private static class SampleListener implements ServiceListener {

        public SampleListener(){}

        @Override
        public void serviceAdded(ServiceEvent event) {
            final String name = event.getInfo().getName();
            final String text = decodeServiceText(new String(event.getInfo().getTextBytes()));
            final String[] ips = event.getInfo().getHostAddresses();
            String ip="";
            for(String aipee:ips){ip+=aipee + " ";}

            logger.debug("_NSD_ Service Added:" + " Name: " + name + ", Text: " + text + " ips: " + ip);
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            final String name = event.getInfo().getName();
            final String text = decodeServiceText(new String(event.getInfo().getTextBytes()));
            final String[] ips = event.getInfo().getHostAddresses();
            String ip="";
            for(String aipee:ips){ip+=aipee + " ";}

            logger.debug("_NSD_ Service Removed:" + " Name: " + name + ", Text: " + text + " ips: " + ip);

        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            final String name = event.getInfo().getName();
            final String text = decodeServiceText(new String(event.getInfo().getTextBytes()));
            final String[] ips = event.getInfo().getHostAddresses();
            String ip="";
            for(String aipee:ips){ip+=aipee + " ";}

            logger.debug("_NSD_ Service Resolved:" + " Name: " + name + ", Text: " + text + " ips: " + ip);

        }

    }

    //only used for android
    public void enableNeighborDiscoveryOnLTE(){
        if (registry!=null){
            //enable lte support
            ServiceInfo serviceInfoLTE = ServiceInfo.create("LTE_LTE_LTE", serviceName, servicePort, 1, 1, true,  encodeServiceText("LTE_LTE_LTE"));
            registry.enableLTEsupport(serviceInfoLTE, sampleListener);
        }
    }

    //only used for android
    public void disableNeighborDiscoveryOnLTE(){
        if (registry!=null){
            registry.disableLTEsupport();
        }
    }

    //adds encoding with a service text before use.
    public static String encodeServiceText(String serviceText){
        //Add '!@# before and '$%^' after serviceText
        return "!@#" + serviceText + "$%^";
    }

    //decode a service text before
    public static String decodeServiceText(String serviceText){
        //check if the string has '!@# before and '$%^' after
        if(serviceText.contains("!@#") && serviceText.contains("$%^")){
            return serviceText.substring(serviceText.indexOf("!@#") + "!@#".length(), serviceText.indexOf("$%^"));
        }

        return "<empty>";
    }

    //change service text on the fly
    public void setText(String serviceText){
        serviceInfo.setText(encodeServiceText(serviceText).getBytes());
    }
    
    @Override
    public void terminate() {
        try {
            if (this.registry != null) {
                this.registry.unregisterAllServices();
                this.registry.close();
                //this.registry.disableLTEsupport();
                System.out.println("terminated jmdns stuff successfully");
            }
        } catch (Exception ex) {
            System.out.println("error in terminating jmdns stuff");
            ex.printStackTrace();
        }
    }

}
