package edu.tamu.cse.lenss.ek.commandLine.cliCommands;

import java.io.IOException;
import java.util.concurrent.Callable;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.ek.commandLine.MasterCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "get_name_by_guid", 
        description = "Fetch the Name corrsponding to a GUID")
public class GetAccountNamebyGUID implements Callable<Void> {
	@Parameters(index = "0") String GUID;

    @ParentCommand MasterCommand parent;
    
    public GetAccountNamebyGUID(){}

    public Void call() throws IOException {
    	parent.out.println(EKClient.getAccountNamebyGUID(GUID));
        return null;
    }
}
