package org.geogit.web.api;

/**
 * A basic interface from which all Web API commands will be derived.
 */
public interface WebAPICommand {

    /**
     * Runs the command with the given {@link CommandContext}.
     * 
     * @param context the context for this command
     */
    void run(CommandContext context);

}
