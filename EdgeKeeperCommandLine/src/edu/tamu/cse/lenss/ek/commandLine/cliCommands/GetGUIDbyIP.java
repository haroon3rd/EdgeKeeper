package edu.tamu.cse.lenss.ek.commandLine.cliCommands;

import java.io.IOException;
import java.util.concurrent.Callable;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.ek.commandLine.MasterCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "get_guid_by_ip", 
        description = "Fetch theGUID corresponding to an IP")
public class GetGUIDbyIP implements Callable<Void> {
	@Parameters(index = "0") String ip;

    @ParentCommand MasterCommand parent;
    
    public GetGUIDbyIP(){}

    public Void call() throws IOException {
    	parent.out.println(EKClient.getGUIDbyIP(ip));
        return null;
    }
}

