package edu.tamu.cse.lenss.edgeKeeper.orch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.log4j.Logger;


/**
 * A Container AutoScaler Class, written by Amran Haroon.
 *
 * This class has methods to 'update' running containers using
 * docker built in commands 'docker update'.
 * 
 *
 */


public class AutoPilot {
	static final Logger logger = Logger.getLogger(AutoPilot.class);

	
	public void autoScale(String container, double memory, double cpu)
	{
		//logger.info("docker-compose -f " + System.getProperty("user.dir") + "/" + deployPath.getName() + "/" + list + " up");
		//System.out.println("docker-compose -f " + System.getProperty("user.dir") + File.separatorChar + deployPath.getName() + "/" + list + " up");
		Thread thread = new Thread(){
			public void run(){
				while(true) {
					
						if (isRunning(container))
						{
							try {
								Process process = Runtime.getRuntime().exec("docker update --cpu-shares " + cpu + " -m " + memory + " " + container);
								printResults(process, container);
								//System.out.println(process.waitFor() + " ####");
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
//					try {
//						sleep(2000);
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//					System.out.println("The thread run is complete..");
				}
			}
		};

		thread.start();
	}


	private boolean isRunning(String container) {
		// TODO Auto-generated method stub
		/*
		 * Code to check if the referred container is running or not
		 * return a boolean (True/False).
		 */
		return true;
	}


	public static void printResults(Process process, String name) throws IOException {
		BufferedReader readOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
		BufferedReader readError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
		String line = "";
		while ((line = readOutput.readLine()) != null) {
				//System.out.println(line);
				logger.info(line);
		}

		while ((line = readError.readLine()) != null) {
			int intIndex = line.indexOf("ERROR");
			if(intIndex == - 1) {
				System.out.println("Valid Container update command");
			} else {
				System.out.println("Error while updating containers.");
			}
			System.out.println(line);
		}
	}
	
}
