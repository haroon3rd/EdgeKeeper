package edu.tamu.cse.lenss.ek.commandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.DefaultHighlighter;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.TerminalBuilder;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.ek.commandLine.cliCommands.GetPeerIP;

import org.jline.terminal.Terminal;
import org.jline.reader.MaskingCallback;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.shell.jline3.PicocliJLineCompleter;

/**
 * This class runsa terminal where a user can run all the commands corresponding to the EKClient manually.
 * This is a test class.
 * @author sbhunia
 *
 */
public class CLITerminal {
	
	/*
	 * This function fetches all the classes under the command class package.
	 */
    static ImmutableSet<ClassInfo> getCommandList() {
    	final ClassLoader loader = Thread.currentThread().getContextClassLoader();
    	// It uses Guava to fetch the list of classes.
    	ClassPath classpath;
		try {
			classpath = ClassPath.from(loader);
			
			String commandPackage = GetPeerIP.class.getPackage().getName();
			
			ImmutableSet<ClassInfo> commandClasses = classpath.getTopLevelClasses(commandPackage);
			
			return commandClasses;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return null;
    }
    
    /*
     * This is the main class function
     */
    public static void main(String[] args) {
    	initLogger("ALL");
        try {
        	
        	//getCommandList();
            // set up the completion
            //CliCommands commands = new CliCommands();
        	MasterCommand commands = new MasterCommand();
            CommandLine cmd = new CommandLine(commands);
            //cmd.addSubcommand(new GetPeerIP());
            
            ImmutableSet<ClassInfo> commandClasses = getCommandList();
            //System.out.println("Possible command classes: "+commandClasses);
            // Now add all the cubcommands the the main command class
            for(ClassInfo subCommandName: commandClasses) {
            	//System.out.println("Possible command "+subCommandName);
	            Object subCommand = Class.forName(subCommandName.getName())
	            		 .getConstructors()[0]
	            		 .newInstance();
	            cmd.addSubcommand(subCommand);
            }
            //This will disable all garbage printouts for empty carriage returns.
            //cmd.setUnmatchedArgumentsAllowed(true);
            
            Terminal terminal = TerminalBuilder.builder().build();
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(new PicocliJLineCompleter(cmd.getCommandSpec()))
                    .parser(new DefaultParser())
                    .history(new DefaultHistory())
                    .highlighter(new DefaultHighlighter())
                    .build();
            commands.setReader(cmd, reader);
            
            String rightPrompt = null;

            // start the shell and process input until the user quits with Ctl-D
            String line;
            while (true) {
                try {
                	String prompt = EKClient.SERVER_IP+":> ";
                	line = reader.readLine(prompt, rightPrompt, (MaskingCallback) null, null);
                    ParsedLine pl = reader.getParser().parse(line, 0);
                    String[] arguments = pl.words().toArray(new String[0]);
                    cmd.execute(arguments);
                } catch (UserInterruptException e) {
                    terminal.writer().println("User Interruption. Press Ctrl+D to exit.");
                } catch (EndOfFileException e) {
                	terminal.writer().println("End of file exception. Exiting");
                	System.exit(0);;
                }                    
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    
	public static void initLogger(String level)  {
        //First let the logger show the messages to System.out
        Logger rootLogger = Logger.getRootLogger();
        rootLogger.removeAllAppenders();
        rootLogger.setLevel(Level.toLevel(level));
        rootLogger.addAppender(new ConsoleAppender(new PatternLayout("[%-5p] %d (%c{1}): %m%n"), "System.out"));
    }
    
    
}