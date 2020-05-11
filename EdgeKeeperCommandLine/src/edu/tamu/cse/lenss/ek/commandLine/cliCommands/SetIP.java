package edu.tamu.cse.lenss.ek.commandLine.cliCommands;

import java.io.IOException;
import java.util.concurrent.Callable;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKProperties;
import edu.tamu.cse.lenss.ek.commandLine.MasterCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "set_ip", 
        description = "Change the IP to connect to EdgeKeeper")
public class SetIP implements Callable<Void> {
	@Parameters(index = "0") String IPAddress;

    @ParentCommand MasterCommand parent;
    
    public SetIP(){}

    public Void call() throws IOException {
    	if(EKProperties.validIP(IPAddress)) 
    		EKClient.SERVER_IP=IPAddress;
    	else
    		parent.out.println("Not a valid IP.");
        return null;
    }
}
