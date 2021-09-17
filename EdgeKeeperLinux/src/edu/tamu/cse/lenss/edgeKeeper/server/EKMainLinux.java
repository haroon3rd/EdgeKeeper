package edu.tamu.cse.lenss.edgeKeeper.server;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.logging.LogManager;

import org.apache.log4j.Logger;

import edu.tamu.cse.lenss.edgeKeeper.utils.EKProperties;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtilsDesktop;

/**
 * This is the main class for the desktop service. 
 * @author sbhunia
 *
 */
public class EKMainLinux { 

    /**
     * It initializes the logger (n) when a properties file is used
     */
    public static void configLoggerWithPropertiesFile() {
		// First configure Log4j configuration
		try {
			System.setProperty("log4j.configuration", new File(".", File.separatorChar+"log4j.properties").toURI().toURL().toString());
			//System.setProperty("java.util.logging.config.file", new File(".", File.separatorChar+"logging.properties").toURL().toString());
		} catch (MalformedURLException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		// Now configure Java Util Log configure
		try {
		    //FileInputStream configFile = new //FileInputStream("/path/to/app.properties");
		    //preferences.load(configFile);
		    InputStream configFile = new FileInputStream(new File ("logging.properties"));
		    LogManager.getLogManager().readConfiguration(configFile);
		} catch (IOException ex)
		{
		    System.out.println("WARNING: Logging not configured (console output only)");
		}
    }
    
	public static void main(String[] args)  {
		configLoggerWithPropertiesFile();
		Logger logger = Logger.getLogger(EKMainLinux.class);
		
		EKProperties prop;
		try {
			prop = EKProperties.loadFromFile(System.getProperty("user.dir")+"/ek.properties");
		} catch (IllegalArgumentException | IOException | IllegalAccessException e) {
			logger.fatal("Problem in loading properties file",e);
			return;
		}
		
		EKUtils gnsServiceUtils = new EKUtilsDesktop(prop); 
		
		logger.info("EK properties: "+ prop);
		
        EKHandler  gnsServiceHandler = new EKHandler(gnsServiceUtils, prop);
        gnsServiceHandler.start();
		
        /*
		 * Now register for a shutdown hook. Thus, when this process is terminated,
		 * This hook will be called which will remove the GUID from the GNS server 
		*/
		Runtime.getRuntime().addShutdownHook(gnsServiceHandler.getShutDownHook());
		logger.debug("Registered shutdown hook");
	
	}
	
}
