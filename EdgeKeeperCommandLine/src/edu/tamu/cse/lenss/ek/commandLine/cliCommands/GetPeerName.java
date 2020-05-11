package edu.tamu.cse.lenss.ek.commandLine.cliCommands;

import java.io.IOException;
import java.util.concurrent.Callable;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.ek.commandLine.MasterCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "get_peer_name", 
        description = "Fetch peer HostName that registers for the corresponding service with the provided duty")
public class GetPeerName implements Callable<Void> {
	@Parameters(index = "0") String serviceName;
	@Parameters(index = "1") String duty;

    @ParentCommand MasterCommand parent;
    
    public GetPeerName(){}

    public Void call() throws IOException {
    	parent.out.println(EKClient.getPeerNames(serviceName, duty));
        return null;
    }
}

