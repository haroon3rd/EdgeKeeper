package edu.tamu.cse.lenss.ek.commandLine.cliCommands;

import java.io.IOException;
import java.util.concurrent.Callable;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.ek.commandLine.MasterCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

@Command(name = "get_merged_guid", description = "Retrieve all the GUIDs including the merged GUIDs from neighbor edge")
public class GetMergedGuid implements Callable<Void> {

	@ParentCommand
	MasterCommand parent;

	public GetMergedGuid() {
	}

	public Void call() throws IOException {
		parent.out.println(EKClient.getMergedGUID());
		return null;
	}
}
