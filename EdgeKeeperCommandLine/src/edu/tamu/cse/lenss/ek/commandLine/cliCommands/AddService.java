package edu.tamu.cse.lenss.ek.commandLine.cliCommands;

import java.io.IOException;
import java.util.concurrent.Callable;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKProperties;
import edu.tamu.cse.lenss.ek.commandLine.MasterCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "add_service", 
        description = "Register the target device with the service and corrsponding duty")
public class AddService implements Callable<Void> {
	@Parameters(index = "0") String serviceName;
	@Parameters(index = "1") String duty;

    @ParentCommand MasterCommand parent;
    
    public AddService(){}

    public Void call() throws IOException {
    	parent.out.println(EKClient.addService(serviceName, duty));
        return null;
    }
}
