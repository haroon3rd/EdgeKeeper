package edu.tamu.cse.lenss.ek.commandLine.cliCommands;

import java.io.IOException;
import java.util.concurrent.Callable;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.ek.commandLine.MasterCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

@Command(name = "get_all_local_guids", description = "Retrieve all the GUIDs connected to the local edge")
public class GetAllGUID implements Callable<Void> {

	@ParentCommand
	MasterCommand parent;

	public GetAllGUID() {
	}

	public Void call() throws IOException {
		parent.out.println(EKClient.getAllLocalGUID());
		return null;
	}
}
