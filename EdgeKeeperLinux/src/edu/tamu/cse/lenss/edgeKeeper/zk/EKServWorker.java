package edu.tamu.cse.lenss.edgeKeeper.zk;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import edu.tamu.cse.lenss.edgeKeeper.server.EKHandler;
import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;

public class EKServWorker extends Thread implements Terminable{

    static final Logger logger = Logger.getLogger(EKServWorker.class);
    Properties cfg;
    EKHandler eventHandler;
    List<Terminable> shutdownHook = new ArrayList<Terminable>();

	private int currentSeq;
	private static AtomicInteger restartSeq = new AtomicInteger(0);

    public EKServWorker (Properties ekProp, EKHandler eventHandler, String ownServerIP){
        logger.debug("***** EKServWorker: entered constructor *****");
        this.cfg = ekProp;
        this.eventHandler = eventHandler;
    }

    @Override
    public void run() {
        logger.debug("***** EKServWorker: entered run() *****");
        currentSeq = restartSeq.incrementAndGet();
        logger.info(currentSeq+" EK replica configuration: " + cfg.toString());
    }

    public void terminate() {
        logger.debug("***** EKServWorker: entered terminate() *****");
    	this.interrupt();
    	for(Terminable t: this.shutdownHook)
    		try {
    			t.terminate();
    		} catch(Exception e) {
    			logger.debug("Problem in terminating "+t.getClass().getSimpleName(), e);
    		}        
            eventHandler.onZKServerStop();
            logger.info(currentSeq+" Terminated "+this.getClass().getName());        
    }

}