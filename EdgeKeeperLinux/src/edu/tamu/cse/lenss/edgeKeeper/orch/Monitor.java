package edu.tamu.cse.lenss.edgeKeeper.orch;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * A Container Monitoring Class, written by Amran Haroon.
 *
 * This class has methods to 'monitor' running containers and read their stats using
 * docker built in commands.
 * 
 * The methods 'measureCPUShare' and 'measureMEMShare' calculated the average of last 'X'
 * usage using the 'FifoQueue' class.
 *
 */

public class Monitor {

	static final Logger logger = Logger.getLogger(Monitor.class);

	Map<String, FifoQCustomSize<Double>> mapCPUFifoList = new HashMap<>();
	Map<String, FifoQCustomSize<Double>> mapMEMFifoList = new HashMap<>();



	public void monitor(List<String> containerList)
	{	
		try {
			Thread.sleep(containerList.size()*1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for(String container : containerList ) {
			FifoQCustomSize<Double> cpu = new FifoQCustomSize<>(10);
			mapCPUFifoList.put(container,cpu);
			FifoQCustomSize<Double> mem = new FifoQCustomSize<>(10);
			mapMEMFifoList.put(container,mem);
		}
		//logger.info("docker-compose -f " + System.getProperty("user.dir") + "/" + deployPath.getName() + "/" + list + " up");
		//System.out.println("docker-compose -f " + System.getProperty("user.dir") + File.separatorChar + deployPath.getName() + "/" + list + " up");
		Thread thread = new Thread(){
			public void run(){
				while(true) {
					//for (String container : containerList )
					//{
					//if (isDeployed(container))
					//{
					try {
						Process process = Runtime.getRuntime().exec("docker stats --no-stream");
						//Process process = Runtime.getRuntime().exec("docker stats --no-stream " + container);
						//Process process = Runtime.getRuntime().exec("docker stats --format \"table {{.Name}}\\t{{.CPUPerc}}\\t{{.MemUsage}}");
						//parseResults(process, container);
						parseResults(process, null);
						//System.out.println(process.waitFor() + " ####");
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					//}
				}
//				try {
//					sleep(2000);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				System.out.println("The thread run is complete..");
			}
			//}
		};

		thread.start();
		//Process process = Runtime.getRuntime().exec("docker-compose up");
		//logger.info("Deployemnt of \"" + list + "\" has been applied.");
		//System.out.println("Deployemnt of \"" + list + "\" has been applied.");	
		//System.out.println("I am out of for loop now.");
	}


	private boolean isDeployed(String container) {
		// TODO Auto-generated method stub
		return true;
	}


	public void parseResults(Process process, String name) throws IOException {
		BufferedReader readOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
		BufferedReader readError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
		String line = "";
		while ((line = readOutput.readLine()) != null) {
			//if(line.contains(name) && !line.contains("Thread")) {
			if(!line.contains("NAME") && !line.contains("Thread")) {
				//System.out.println(line);
				String[] splitString = line.split("\\s+");
				System.out.println(splitString[1] + "\tCPU: " + splitString[2] + "\tMemory: " + splitString[3] + " of " + splitString[5] );
				//				Double before = mapCPUFifoList.get(splitString[1]).peekLatest();
				//				if (before != null)
				//					System.out.println("Before : " + before);
				System.out.println(splitString[0]);
				mapCPUFifoList.get(splitString[1]).put(Double.valueOf(removePercent(splitString[2])));
				//				Double after = mapCPUFifoList.get(splitString[1]).peekLatest();
				//				if (after != null)
				//					System.out.println("Before : " + after);
				//System.out.println("After : " + mapCPUFifoList.get(splitString[1]).peekOldest());
				mapMEMFifoList.get(splitString[1]).put(Double.valueOf(removeUnit(splitString[3])));
				/*
				 * Write codes here to store these results in an individual data structure
				 * 
				 */

				//logger.info(line);
			}
		}

		while ((line = readError.readLine()) != null) {
			int intIndex = line.indexOf("ERROR");
			if(intIndex == - 1) {
				System.out.println("Valid file with content");
			} else {
				System.out.println("Found Error in Compose file.");
			}
			System.out.println(line);
		}
	}

	private double measureCPUShare(String container, double cpu) {
		// TODO Auto-generated method stub
		/*
		 * Code to calculate average of last 'X' cpu readings.
		 */
		return cpu;
	}

	private double measureMEMShare(String container, double mem) {
		// TODO Auto-generated method stub
		/*
		 * Code to calculate average of last 'X' memory readings.
		 */
		return mem;
	}

	private String removePercent(String str) {
		String result = null;
		if ((str != null) && (str.length() > 1)) {
			result = str.substring(0, str.length() - 1);
		}
		return result;
	}

	private String removeUnit(String str) {
		String result = null;
		if ((str != null) && (str.length() > 3)) {
			result = str.substring(0, str.length() - 3);
		}
		return result;
	}

}
