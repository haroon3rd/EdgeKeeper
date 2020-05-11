package edu.tamu.cse.lenss.ek.commandLine.cliCommands;

import java.io.IOException;
import java.util.concurrent.Callable;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.ek.commandLine.MasterCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

@Command(name = "get_own_guid", 
        description = "Fetches the Devices's GUID")
public class GetOwnGUID implements Callable<Void> {
	
    @ParentCommand MasterCommand parent;
    
    public GetOwnGUID(){}

    public Void call() throws IOException {
    	parent.out.println(EKClient.getOwnGuid());
        return null;
    }
}
