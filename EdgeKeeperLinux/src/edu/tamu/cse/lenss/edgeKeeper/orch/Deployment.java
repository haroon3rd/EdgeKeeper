package edu.tamu.cse.lenss.edgeKeeper.orch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.log4j.Logger;

public class Deployment {
	
	static final Logger logger = Logger.getLogger(Deployment.class);	
	
	
	public Deployment() {
		
	}
	
	public void deploy(File deployPath, List<String> deployList)
	{
		for (String list : deployList)
		{
			if (isValidConfig(deployPath, list))
			{
				logger.info("docker-compose -f " + System.getProperty("user.dir") + "/" + deployPath.getName() + "/" + list + " up");
				System.out.println("docker-compose -f " + System.getProperty("user.dir") + File.separatorChar + deployPath.getName() + "/" + list + " up");
				Thread thread = new Thread(){
					public void run(){
						try {
							Process process = Runtime.getRuntime().exec("docker-compose -f " + System.getProperty("user.dir") + File.separatorChar + deployPath.getName() + "/" + list + " up");
							printResults(process);
							System.out.println(process.waitFor() + " ####");
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						System.out.println("The thread run is complete..");//
					}
				};

				thread.start();
				//Process process = Runtime.getRuntime().exec("docker-compose up");
				logger.info("Deployemnt of \"" + list + "\" has been applied.");
				System.out.println("Deployemnt of \"" + list + "\" has been applied.");	
			}
		}
		//System.out.println("I am out of for loop now.");
	}
	
	public static void printResults(Process process) throws IOException {
	    BufferedReader readOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
	    BufferedReader readError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
	    String line = "";
	    while ((line = readOutput.readLine()) != null) {
	    	logger.info(line);
	        System.out.println(line);
	    }
	    
	    while ((line = readError.readLine()) != null) {
	    	int intIndex = line.indexOf("ERROR");
	    	if(intIndex == - 1) {
				System.out.println("Valid Compose file");
			} else {
				System.out.println("Found Error in Compose file.");
			}
	        System.out.println(line);
	    }
	}
	
	public boolean isValidConfig(File checkPath,String checkList) {
		try {
			Process process = Runtime.getRuntime().exec("docker-compose -f " + checkPath + File.separatorChar + checkList + " config");
			printResults(process);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	
	

}
