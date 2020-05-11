package edu.tamu.cse.lenss.ek.commandLine.cliCommands;

import java.io.IOException;
import java.util.concurrent.Callable;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.ek.commandLine.MasterCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "get_guid_by_name", 
        description = "Fetch theGUID corresponding to a hostname")
public class GetGUIDbyAccountName implements Callable<Void> {
	@Parameters(index = "0") String HostName;

    @ParentCommand MasterCommand parent;
    
    public GetGUIDbyAccountName(){}

    public Void call() throws IOException {
    	parent.out.println(EKClient.getGUIDbyAccountName(HostName));
        return null;
    }
}
