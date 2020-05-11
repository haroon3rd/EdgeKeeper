package edu.tamu.cse.lenss.ek.commandLine.cliCommands;

import java.io.IOException;
import java.util.concurrent.Callable;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.ek.commandLine.MasterCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "get_name_by_ip", 
        description = "Fetch the names corrsponding to the IP")
public class GetAccountNamebyIP implements Callable<Void> {
	@Parameters(index = "0") String IP;

    @ParentCommand MasterCommand parent;
    
    public GetAccountNamebyIP(){}

    public Void call() throws IOException {
    	parent.out.println(EKClient.getAccountNamebyIP(IP));
        return null;
    }
}
