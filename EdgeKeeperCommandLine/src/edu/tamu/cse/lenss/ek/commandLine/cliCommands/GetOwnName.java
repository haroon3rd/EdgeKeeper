package edu.tamu.cse.lenss.ek.commandLine.cliCommands;

import java.io.IOException;
import java.util.concurrent.Callable;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.ek.commandLine.MasterCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

@Command(name = "get_own_name", 
        description = "Fetches the Devices's HostName")
public class GetOwnName implements Callable<Void> {
	
    @ParentCommand MasterCommand parent;
    
    public GetOwnName(){}

    public Void call() throws IOException {
    	parent.out.println(EKClient.getOwnAccountName());
        return null;
    }
}
