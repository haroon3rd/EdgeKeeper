package edu.tamu.cse.lenss.ek.commandLine;

import java.io.PrintWriter;

import org.jline.reader.LineReader;
import org.jline.reader.impl.LineReaderImpl;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.UnmatchedArgumentException;

/**
 * Top-level command that just prints help.
 */
@Command(name = "\n", description = "EdgeKeeper interactive shell with auto completion",
        footer = {"", "Press Ctl-D to exit."}
		)
public class MasterCommand implements Runnable {
    public LineReaderImpl reader;
    public PrintWriter out;
	public CommandLine cmd;

    MasterCommand() {}
    
    public void setReader(CommandLine cmd ,LineReader reader){
    	this.cmd = cmd;
        this.reader = (LineReaderImpl)reader;
        out = reader.getTerminal().writer();
    }

    public void run() {
    	//out.println(new CommandLine(this).getUsageMessage());
    	//System.out.println( new CommandLine(this).getSubcommands());
    	//System.out.println( cmd.getUsageMessage());

    }
    
}
