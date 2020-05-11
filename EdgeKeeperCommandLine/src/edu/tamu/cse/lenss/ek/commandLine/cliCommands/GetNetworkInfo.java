package edu.tamu.cse.lenss.ek.commandLine.cliCommands;

import java.io.IOException;
import java.util.concurrent.Callable;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.ek.commandLine.MasterCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "get_network_info", 
        description = "Fetch the Network topology in terms of OLSR JSON Info")
public class GetNetworkInfo implements Callable<Void> {

    @ParentCommand MasterCommand parent;
    
    public GetNetworkInfo(){}

    public Void call() throws IOException {
    	parent.out.println(EKClient.getNetworkInfo().printToString());
        return null;
    }
}

