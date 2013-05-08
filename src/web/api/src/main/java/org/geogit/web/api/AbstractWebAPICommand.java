package org.geogit.web.api;

import java.util.UUID;

import org.geogit.api.CommandLocator;
import org.geogit.api.GeogitTransaction;

/**
 * An abstract command that allows WebAPICommands to support long transactions.
 */

public class AbstractWebAPICommand implements WebAPICommand {

    private UUID transactionId = null;

    /**
     * Accessor for the transactionId
     * 
     * @return the id of the transaction to run commands off of
     */
    public UUID getTransactionId() {
        return transactionId;
    }

    /**
     * Mutator for the transactionId
     * 
     * @param transactionId - the transaction id to run commands off of
     */
    public void setTransactionId(String transactionId) {
        if (transactionId != null) {
            this.transactionId = UUID.fromString(transactionId);
        }
    }

    /**
     * This function either builds a GeoGitTransaction to run commands off of if there is a
     * transactionId to build off of or the GeoGit commandLocator otherwise.
     * 
     * @param context - the context to get the information needed to get the commandLocator
     * @return
     */
    public CommandLocator getCommandLocator(CommandContext context) {
        if (transactionId != null) {
            return new GeogitTransaction(context.getGeoGIT().getCommandLocator(), context
                    .getGeoGIT().getRepository(), transactionId);
        }
        return context.getGeoGIT().getCommandLocator();
    }

    /**
     * Empty override.
     */
    @Override
    public void run(CommandContext context) {

    }

}
