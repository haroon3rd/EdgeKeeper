package edu.tamu.cse.lenss.ek.commandLine.cliCommands;

import java.io.IOException;
import java.util.concurrent.Callable;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.ek.commandLine.MasterCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "get_ip_by_name", 
        description = "Fetch the IP addresses corrsponding to a HostName")
public class GetIPbyName implements Callable<Void> {
	@Parameters(index = "0") String HostName;

    @ParentCommand MasterCommand parent;
    
    public GetIPbyName(){}

    public Void call() throws IOException {
    	parent.out.println(EKClient.getIPbyName(HostName));
        return null;
    }
}

