package edu.tamu.cse.lenss.ek.commandLine.cliCommands;

import java.io.IOException;
import java.util.concurrent.Callable;

import edu.tamu.cse.lenss.ek.commandLine.MasterCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "history", 
        description = "Shows history of commands")
public class History implements Callable<Void> {

    @ParentCommand MasterCommand parent;
    
    public History(){}

    public Void call() throws IOException {
        parent.out.println(parent.reader.getHistory().toString());
        return null;
    }
}
