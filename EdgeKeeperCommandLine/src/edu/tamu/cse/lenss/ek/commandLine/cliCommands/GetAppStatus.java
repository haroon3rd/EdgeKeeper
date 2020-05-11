package edu.tamu.cse.lenss.ek.commandLine.cliCommands;

import java.io.IOException;
import java.util.concurrent.Callable;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.ek.commandLine.MasterCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "get_app_status", 
        description = "Fetch the Health of an application running on a device")
public class GetAppStatus implements Callable<Void> {
	@Parameters(index = "0") String TargetGUID;
	@Parameters(index = "1") String AppName;

    @ParentCommand MasterCommand parent;
    
    public GetAppStatus(){}

    public Void call() throws IOException {
    	parent.out.println(EKClient.getAppStatus(TargetGUID, AppName));
        return null;
    }
}
