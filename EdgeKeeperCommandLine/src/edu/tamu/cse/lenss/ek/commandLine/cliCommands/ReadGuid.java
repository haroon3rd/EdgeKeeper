package edu.tamu.cse.lenss.ek.commandLine.cliCommands;

import java.io.IOException;
import java.util.concurrent.Callable;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.ek.commandLine.MasterCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "read_guid", description = "Retrieve the entier record for the GUID")
public class ReadGuid implements Callable<Void> {
	@Parameters(index = "0") String guid;

	@ParentCommand	MasterCommand parent;

	public ReadGuid() {	}

	public Void call() throws IOException {
		parent.out.println(EKClient.readGUID(guid));
		return null;
	}
}
