package edu.tamu.cse.lenss.edgeKeeper.orch;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Logger;

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
	
	static final Logger logger = Logger.getLogger(OrchMain.class);
	static Deployment deployments = new Deployment();
	/**
     * Method main.
     * 
     * @param args
     *            String[]
     */
	
	public static void main(String[] args) {
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
        List<String> deploymentList = null;
        
        if(file.exists()) {
        	try {
            	deploymentList = filelist(file);
            	logger.info("Container deployment list: " + deploymentList);
            } catch (Exception e) {
                logger.error("Error in reading file lists, with error on: " + todayDate, e);
            	System.out.println(e);
            }
        }
        else {
        	logger.info("Container deployment Folder \"" + file + "\" does not exist.");
        	System.out.println("Container deployment Folder \"" + file + "\" does not exist.");
        }
        // checkFileExist();
        
        if(deploymentList!=null)
        	deployments.deploy(file, deploymentList);
        else {
        	logger.info("Deployment list is empty.");
        	System.out.println("Empty deployment list.");
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
					logger.info("Valid deployment : "+filename[0]+"."+filename[1]);
					System.out.println("Valid file exist: "+filename[0]+"."+filename[1]); // match occures.Apply any condition what you need
					validFileList.add(filename[0] +"."+filename[1]);
				}
			}
		}
		return validFileList;
	}
}
