package edu.tamu.cse.lenss.edgeKeeper.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import edu.tamu.cse.lenss.edgeKeeper.server.EKHandler;
import edu.tamu.cse.lenss.edgeKeeper.server.RequestTranslator;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils;
import net.minidev.json.JSONArray;

public class UnitTest {
	static  Logger logger = Logger.getLogger(UnitTest.class);
	
	public static EdgeKeeperAPI mEKClient;
	
	public static void main(String[] args) {

//		Logger logger = Logger.getLogger(UnitTest.class);
//		try {
//			System.setProperty("log4j.configuration", new File(".", File.separatorChar+
//					"client_log4j.properties").toURL().toString());
//			//System.setProperty("java.util.logging.config.file", new File(".", File.separatorChar+"logging.properties").toURL().toString());
//		} catch (MalformedURLException e2) {
//			// TODO Auto-generated catch block
//			e2.printStackTrace();
//		}
		
		try {
			EKUtils.initLogger("logs/client.log", Level.ALL);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		logger.info("Starting unit test");
		
	
		//EKClient gnsClientHandler = new EKClient();
		
		//EKClient.SERVER_IP="192.168.2.84";
		JSONObject obj = new JSONObject();
		
		try {
			obj.put(RequestTranslator.dutyField, "master");
			obj.put(RequestTranslator.fieldIP, "192.168.1.111");
			//obj.put(RequestTranslator.fieldPort, "998877");
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		logger.log(Level.ALL, "----------------------------" + obj.toString());
				
		
		
		
		try {	
			mEKClient = new EKClient();
			logger.info("Start an EdgeKeeper client ...");	
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		for (int i =0; i< 1; i++) {
			
			/*new Thread(){
				public void run() {*/
			
			
			
			try {
					String ownGUID = mEKClient.getOwnGuid();
					String ownName = mEKClient.getOwnAccountName();
					
					mEKClient.getAllLocalGUID();
					mEKClient.readGUID(ownGUID);
					
					mEKClient.getNetworkInfo();
					
					
					//EKClient.getNetworkInfo();
					
					
//					System.out.println("--------------------------------------------REGISTER--------------------------------------");
//					String serviceID = mEKClient.addService("MStrom", "master", "192.168.1.111");
//					System.out.println("-----------------" + serviceID + "-------------------------------------");
//					
//					System.out.println("--------------------------------------------REGISTER--------------------------------------");
//					String serviceID2 = mEKClient.addService("MStrom", "master", "192.168.1.222");
//					System.out.println("-----------------" + serviceID2 + "-------------------------------------");
//					
					
					
//					System.out.println("--------------------------------------------REGISTER--------------------------------------");
//					EKClient.addService("DistressNet-MStrom", "master");
//					System.out.println("------------------------------------------------------------------------------------------");
					
					
//					System.out.println("--------------------------------------------REGISTER--------------------------------------");
//					EKClient.addService("DistressNet-MStrom", obj.toString());
//					System.out.println("------------------------------------------------------------------------------------------");
					
					
//					System.out.println("--------------------------------------------FETCH-INFO------------------------------------");
//					List<String> peers = mEKClient.getPeerList("MStrom", "master");
//					System.out.println("--------------------" + peers +"----------------------------------------------------------------------");
//		
//					
//					JSONObject record = EKHandler.ekRecord.fetchRecord();
//					System.out.println("----------------------------" + record.toString());
//					
					
					mEKClient.removeService("MStrom");
					
					System.out.println(mEKClient.getAccountNamebyGUID("01EC8149379E2B441EF982853B5781C93939B30F"));
					
					
//					EKClient.getPeerGUIDs("DistressNet-MStrom", "master");
//					EKClient.getPeerIPs("DistressNet-MStrom", "master");
//					EKClient.getPeerNames("DistressNet-MStrom", "master");
//					
//					EKClient.getPeerGUIDs("DistressNet-MStrom", "client");
//					EKClient.getPeerIPs("DistressNet-MStrom", "client");
//					EKClient.getPeerNames("DistressNet-MStrom", "client");
//					
//					EKClient.getIPbyGUID(ownGUID);	
//					EKClient.getIPbyName(ownName);
//					EKClient.getGUIDbyAccountName(ownName);
//					EKClient.getAccountNamebyGUID(ownGUID);
//					
//					EKClient.getGUIDbyIP("192.168.0.1");
//					EKClient.getAccountNamebyIP("192.168.0.1");
//
//					EKClient.removeService("DistressNet-MStrom");
//					
//					EKClient.getZooKeeperConnectionString();

					//while (true) {
						//put();
						//getEdgeStatus();
//						try{
//							System.out.println(EKClient.getAppStatus(ownGUID, "TEST").toString(4));
//							System.out.println(EKClient.getAppStatus(ownGUID, "TEST").toString(4));
//						}catch (Exception e){}
						Sleep(3000);
					//}

			} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

			System.out.println("Epoch: "+i);
		}				
	}


	public static void put(){
		try {
			//make json app
			JSONObject mdfsHealth = new JSONObject();
			mdfsHealth.put("test_status", "Alive");
			mdfsHealth.put("test_network_status", "Good");
			mdfsHealth.put("test_cpu_status", "Good");

			//send
			mEKClient.putAppStatus("suman", mdfsHealth);
		}catch(JSONException e ){
			e.printStackTrace();
		}
	}


	public static void getEdgeStatus(){
		try {

			//getEdgeStatus edge status
			System.out.println(mEKClient.getEdgeStatus().toString(4));

		} catch (JSONException e) {
			e.printStackTrace();
		}

	}

	public static void Sleep(int milliSec){
		try {
			Thread.sleep(milliSec);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}



}


//package edu.tamu.cse.lenss.edgeKeeper.client;
//
//import java.io.IOException;
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils;
//
//public class UnitTest {
//	static  Logger logger = Logger.getLogger(UnitTest.class);
//
//	public static void main(String[] args) {
//
////		Logger logger = Logger.getLogger(UnitTest.class);
////		try {
////			System.setProperty("log4j.configuration", new File(".", File.separatorChar+
////					"client_log4j.properties").toURL().toString());
////			//System.setProperty("java.util.logging.config.file", new File(".", File.separatorChar+"logging.properties").toURL().toString());
////		} catch (MalformedURLException e2) {
////			// TODO Auto-generated catch block
////			e2.printStackTrace();
////		}
//		
//		try {
//			EKUtils.initLogger("logs/client.log", Level.ALL);
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		
//		logger.info("Starting unit test");
//				
//		//EKClient gnsClientHandler = new EKClient();
//		
//		//EKClient.SERVER_IP="192.168.2.84";
//		
//		for (int i =0; i< 1; i++) {
//			
//			/*new Thread(){
//				public void run() {*/
//			
//					
//			
//			try {
//					String ownGUID = EKClient.getOwnGuid();
//					String ownName = EKClient.getOwnAccountName();
//					
//					EKClient.getAllLocalGUID();
//					EKClient.readGUID(ownGUID);
//					
//					EKClient.getNetworkInfo();
//					
//					
//					//EKClient.getNetworkInfo();
//					
//					EKClient.addService("DistressNet-MStrom", "master");
//					
//					
//					EKClient.getPeerGUIDs("DistressNet-MStrom", "master");
//					EKClient.getPeerIPs("DistressNet-MStrom", "master");
//					EKClient.getPeerNames("DistressNet-MStrom", "master");
//					
//					EKClient.getPeerGUIDs("DistressNet-MStrom", "client");
//					EKClient.getPeerIPs("DistressNet-MStrom", "client");
//					EKClient.getPeerNames("DistressNet-MStrom", "client");
//					
//					EKClient.getIPbyGUID(ownGUID);	
//					EKClient.getIPbyName(ownName);
//					EKClient.getGUIDbyAccountName(ownName);
//					EKClient.getAccountNamebyGUID(ownGUID);
//					
//					EKClient.getGUIDbyIP("172.30.30.2");
//					EKClient.getAccountNamebyIP("172.30.30.2");
//
//					EKClient.removeService("DistressNet-MStrom");
//					
//					EKClient.getZooKeeperConnectionString();
//
//					//while (true) {
//						put();
//						getEdgeStatus();
//						try{
//							System.out.println(EKClient.getAppStatus(ownGUID, "TEST").toString(4));
//							System.out.println(EKClient.getAppStatus(ownGUID, "TEST").toString(4));
//						}catch (Exception e){}
//						Sleep(3000);
//					//}
//
//			} catch (Exception e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//
//			System.out.println("Epoch: "+i);
//		}				
//	}
//
//
//	public static void put(){
//		try {
//			//make json app
//			JSONObject mdfsHealth = new JSONObject();
//			mdfsHealth.put("test_status", "Alive");
//			mdfsHealth.put("test_network_status", "Good");
//			mdfsHealth.put("test_cpu_status", "Good");
//
//			//send
//			EKClient.putAppStatus("suman", mdfsHealth);
//		}catch(JSONException e ){
//			e.printStackTrace();
//		}
//	}
//
//
//	public static void getEdgeStatus(){
//		try {
//
//			//getEdgeStatus edge status
//			System.out.println(EKClient.getEdgeStatus().toString(4));
//
//		} catch (JSONException e) {
//			e.printStackTrace();
//		}
//
//	}
//
//	public static void Sleep(int milliSec){
//		try {
//			Thread.sleep(milliSec);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//	}
//
//
//
//}
