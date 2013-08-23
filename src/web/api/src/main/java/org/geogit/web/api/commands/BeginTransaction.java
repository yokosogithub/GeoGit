package org.geogit.web.api.commands;

import org.geogit.api.GeoGIT;
import org.geogit.api.GeogitTransaction;
import org.geogit.api.plumbing.TransactionBegin;
import org.geogit.web.api.AbstractWebAPICommand;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.CommandSpecException;
import org.geogit.web.api.ResponseWriter;

/**
 * The interface for the TransactionBegin operation in GeoGit.
 * 
 * Web interface for {@link TransactionBegin}
 */

public class BeginTransaction extends AbstractWebAPICommand {

    /**
     * Runs the command and builds the appropriate response.
     * 
     * @param context - the context to use for this command
     * 
     * @throws CommandSpecException
     */
    @Override
    public void run(CommandContext context) {
        if (this.getTransactionId() != null) {
            throw new CommandSpecException("Tried to start a transaction within a transaction.");
        }
        final GeoGIT geogit = context.getGeoGIT();

        final GeogitTransaction transaction = geogit.command(TransactionBegin.class).call();

        context.setResponseContent(new CommandResponse() {

            @Override
            public void write(ResponseWriter out) throws Exception {
                out.start();
                out.writeTransactionId(transaction.getTransactionId());
                out.finish();
            }
        });
    }

}
