package org.rogue.geogit.commands;

import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameters;

/**
*
*/
@Parameters(commandNames = "config", commandDescription = "Configure user settings")
public class Config implements CLICommand {

   @Override
   public void run(GeogitCLI cli) throws Exception {
	   System.out.println("ROGUE config command");
   }

}
