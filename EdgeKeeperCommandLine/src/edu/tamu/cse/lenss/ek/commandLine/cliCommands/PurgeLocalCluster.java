package edu.tamu.cse.lenss.ek.commandLine.cliCommands;

import java.io.IOException;
import java.util.concurrent.Callable;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.ek.commandLine.MasterCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

@Command(name = "purge_local_cluster", description = "Purge the local Naming cluster. CAUTION: Don't forget to restart all the EdgeKeeper in the cluster.")
public class PurgeLocalCluster implements Callable<Void> {

	@ParentCommand
	MasterCommand parent;

	public PurgeLocalCluster() {
	}

	public Void call() throws IOException {
		parent.out.println(EKClient.purgeNamingCluster());
		return null;
	}
}
