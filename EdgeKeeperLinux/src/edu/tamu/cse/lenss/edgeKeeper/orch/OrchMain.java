package edu.tamu.cse.lenss.edgeKeeper.orch;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

/**
 * A Container Orchestration Class, written by Amran Haroon.
 *
 * This class has methods to read docker-compose files, validate it and launch containers using
 * docker built in commands 'docker-compose' and 'docker'.
 * 
 *
 */

public class OrchMain {

	/*
	 * public static void configLoggerWithPropertiesFile() { // First configure
	 * Log4j configuration try { System.setProperty("log4j.configuration", new
	 * File(".", File.separatorChar+"ContainerLogs.properties").toURL().toString());
	 * //System.setProperty("java.util.logging.config.file", new File(".",
	 * File.separatorChar+"logging.properties").toURL().toString()); } catch
	 * (MalformedURLException e2) { // TODO Auto-generated catch block
	 * e2.printStackTrace(); } }
	 */


	public static void configLoggerWithPropertiesFile() {
		// First configure Log4j configuration
		try {
			System.setProperty("log4j.configuration", new File(".", File.separatorChar+"log4jOrch.properties").toURI().toURL().toString());
			//System.setProperty("java.util.logging.config.file", new File(".", File.separatorChar+"logging.properties").toURL().toString());
		} catch (MalformedURLException e2) {
			System.out.println("Here I am, This is me!!");// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	}

	//	static final Logger logger = Logger.getLogger(OrchMain.class);
	static Deployment deployments = new Deployment();
	static YamlParser ymlparser = new YamlParser();
	static Monitor monitor = new Monitor();
	
	static List<String> deploymentList = null;
	static List<String> containerList = null;
	/**
	 * Method main.
	 * 
	 * @param args
	 *            String[]
	 */

	public static void main(String[] args) {
		BasicConfigurator.configure();
		configLoggerWithPropertiesFile();
		Logger logger = Logger.getLogger(OrchMain.class);
		// Declare yesterday date with format as yyyyMMdd
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		// Calendar yesterdayCal = Calendar.getInstance();

		// Declare today date with format as yyyyMMdd
		Calendar todayCal = Calendar.getInstance();
		String todayDate = dateFormat.format(todayCal.getTime());

		// Declaration of folder path
		File file = new File("./Containers/");

		// yesterdayCal.add(Calendar.DATE, -1);
		// String yesterdayDate = dateFormat.format(yesterdayCal.getTime());
		
		if(file.exists()) {
			try {
				deploymentList = filelist(file);
				logger.info("Container deployment list: " + deploymentList);
			} catch (Exception e) {
				logger.error("Error in reading file lists, with error on: " + todayDate, e);
				//                logger.info(e);
			}
		}
		else {
			logger.info("Container deployment Folder \"" + file + "\" does not exist.");
			//        	System.out.println("Container deployment Folder \"" + file + "\" does not exist.");
		}

		if(deploymentList!=null) {
			try {
				containerList = ymlparser.convertYamlToContainers(file, deploymentList);
				System.out.println("Deployed Containers = " + containerList);
				deployments.deploy(file, deploymentList);				
				monitor.monitor(containerList);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			logger.info("Deployment list is empty.");
			//        	System.out.println("Empty deployment list.");
		}
	}

	/**
	 * Method checkFileExist.
	 * 
	 * @param File folder
	 *            
	 */

	public static List<String> filelist(File folder)
	{
		List<String> validFileList= new ArrayList<>();
		File[] listOfFiles = folder.listFiles();

		for (File file : listOfFiles)
		{
			if (file.isFile() && (file.getName().indexOf('.') !=-1))
			{
				String[] filename = file.getName().split("\\.(?=[^\\.]+$)"); //split filename from it's extension
				if(filename[0].matches("\\S+") && (filename[1].equalsIgnoreCase("yml") || filename[1].equalsIgnoreCase("yaml" ))) { //matching defined filename
					//logger.info("Valid deployment : "+filename[0]+"."+filename[1]);
					//logger.info("Valid file exist: "+filename[0]+"."+filename[1]); // match occures.Apply any condition what you need
					validFileList.add(filename[0] +"."+filename[1]);
				}
			}
		}
		return validFileList;
	}
}
