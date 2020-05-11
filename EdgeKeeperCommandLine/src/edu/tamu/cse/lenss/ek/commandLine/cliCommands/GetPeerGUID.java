package edu.tamu.cse.lenss.ek.commandLine.cliCommands;

import java.io.IOException;
import java.util.concurrent.Callable;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.ek.commandLine.MasterCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Parameters;

@Command(name = "get_peer_guid", 
        description = "Fetch peer device GUIDs that registers for the corresponding service with the provided duty")
public class GetPeerGUID implements Callable<Void> {
	@Parameters(index = "0") String serviceName;
	@Parameters(index = "1") String duty;

    @ParentCommand MasterCommand parent;
    
    public GetPeerGUID(){}

    public Void call() throws IOException {
    	parent.out.println(EKClient.getPeerGUIDs(serviceName, duty));
        return null;
    }
}
