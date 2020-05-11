package edu.tamu.cse.lenss.ek.commandLine.cliCommands;

import java.io.IOException;
import java.util.concurrent.Callable;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.ek.commandLine.CLITerminal;
import edu.tamu.cse.lenss.ek.commandLine.MasterCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
@Command(name = "set_log_level", 
        description = "Set log4j console logging level")
public class SetLogLevel implements Callable<Void> {
	@Parameters(index = "0") String LogLevel;

    @ParentCommand MasterCommand parent;
    
    public SetLogLevel(){}

    public Void call() throws IOException {
    	CLITerminal.initLogger(LogLevel);
    	return null;
    }
}
