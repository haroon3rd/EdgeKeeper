package edu.tamu.cse.lenss.ek.commandLine.cliCommands;

import java.io.IOException;
import java.util.concurrent.Callable;

import edu.tamu.cse.lenss.ek.commandLine.MasterCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.ParentCommand;

@Command(name = "help", 
        description = "Get commands usage")
public class Help implements Callable<Void> {
	
    @ParentCommand MasterCommand parent;
    
    public Help(){}

    public Void call() throws IOException {
    	parent.cmd.usage(parent.out);
    	//parent.cmd.usag
        return null;
    }
}
