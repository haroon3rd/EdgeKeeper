package edu.tamu.cse.lenss.ek.commandLine.cliCommands;

import java.io.IOException;
import java.util.concurrent.Callable;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.ek.commandLine.MasterCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "remove_service", 
	description = "Deregister a service in the EdgeKeeper")
public class RemoveService implements Callable<Void> {
	@Parameters(index = "0")	String serviceName;

	@ParentCommand
	MasterCommand parent;

	public RemoveService() {}

	public Void call() throws IOException {
		parent.out.println(EKClient.removeService(serviceName));
		return null;
	}
}
