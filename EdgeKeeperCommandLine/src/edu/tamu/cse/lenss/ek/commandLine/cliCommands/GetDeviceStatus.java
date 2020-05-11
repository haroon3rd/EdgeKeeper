package edu.tamu.cse.lenss.ek.commandLine.cliCommands;

import java.io.IOException;
import java.util.concurrent.Callable;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.ek.commandLine.MasterCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "get_device_status", 
        description = "Fetch the Health of a device")
public class GetDeviceStatus implements Callable<Void> {
	@Parameters(index = "0") String TargetGUID;

    @ParentCommand MasterCommand parent;
    
    public GetDeviceStatus(){}

    public Void call() throws IOException {
    	parent.out.println(EKClient.getDeviceStatus(TargetGUID));
        return null;
    }
}
