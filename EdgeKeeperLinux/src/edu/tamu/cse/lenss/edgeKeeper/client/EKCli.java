package edu.tamu.cse.lenss.edgeKeeper.client;
import java.io.IOException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils;
/**
 * This class is created for TopoGraph applications to register and deregister services at GNS.
	The commands are 
		add_service		<service_name>	<duty>
		remove_service	<service_name>
 * @author sbhunia
 *
 */
public class EKCli {

	public static EdgeKeeperAPI mEKClient;

    static final Logger logger = Logger.getLogger(EKCli.class);

	public static void main(String[] args) {
		try {	
			mEKClient = new EKClient();
			logger.info("Start a Zookeeper client ...");	
		} catch (Exception e) {
			e.printStackTrace();
		}


		try {
			EKUtils.initLogger("logs/"+EKCli.class.getSimpleName()+".log", Level.ALL);
		} catch (IOException e) {
			System.err.println("Could not create log file for EdgeKeeper");
			e.printStackTrace();
		}
		
		//logger.log(Level.ALL, "Incoming command: "+args.toString());
		
		if (args.length < 1) {
			logger.debug("Invalid command");
			return;
		}
		else if (args[0].trim().toLowerCase().equals("add_service")) {
			if (args.length == 3)
				//EKClient.addService(args[1], args[2]);
				mEKClient.addService(args[1], args[2]);
			else {
				logger.debug("Invalid command");
				return;
			}
		}
		else if (args[0].trim().toLowerCase().equals("remove_service")) {
			if (args.length >1)
				//EKClient.removeService(args[1]);
				mEKClient.removeService(args[1]);
			else {
				logger.debug("Invalid command");
				return;
			}
		}
	}
}


//package edu.tamu.cse.lenss.edgeKeeper.client;
//
//import java.io.IOException;
//
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;
//
//import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils;
//
//
///**
// * This class is created for TopoGraph applications to register and deregister services at GNS.
//	The commands are 
//		add_service		<service_name>	<duty>
//		remove_service	<service_name>
// * @author sbhunia
// *
// */
//public class EKCli {
//    static final Logger logger = Logger.getLogger(EKCli.class);
//
//	public static void main(String[] args) {
//		
//		try {
//			EKUtils.initLogger("logs/"+EKCli.class.getSimpleName()+".log", Level.ALL);
//		} catch (IOException e) {
//			System.err.println("Could not create log file for EdgeKeeper");
//			e.printStackTrace();
//		}
//		
//		//logger.log(Level.ALL, "Incoming command: "+args.toString());
//		
//		if (args.length < 1) {
//			logger.debug("Invalid command");
//			return;
//		}
//		else if (args[0].trim().toLowerCase().equals("add_service")) {
//			if (args.length == 3)
//				EKClient.addService(args[1], args[2]);
//			else {
//				logger.debug("Invalid command");
//				return;
//			}
//		}
//		else if (args[0].trim().toLowerCase().equals("remove_service")) {
//			if (args.length >1)
//				EKClient.removeService(args[1]);
//			else {
//				logger.debug("Invalid command");
//				return;
//			}
//		}
//	}
//
//}
