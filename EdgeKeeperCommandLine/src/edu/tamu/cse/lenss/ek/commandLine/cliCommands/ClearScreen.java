package edu.tamu.cse.lenss.ek.commandLine.cliCommands;

import java.io.IOException;
import java.util.concurrent.Callable;

import edu.tamu.cse.lenss.ek.commandLine.MasterCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

/**
 * Command that clears the screen.
 */
@Command(name = "cls", 
		aliases = "clear", 
        description = "Clears the screen")
public class ClearScreen implements Callable<Void> {
    @ParentCommand MasterCommand parent;
    
    public ClearScreen(){}

    public Void call() throws IOException {
        parent.reader.clearScreen();
        return null;
    }
}
