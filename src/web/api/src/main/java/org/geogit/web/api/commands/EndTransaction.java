package org.geogit.web.api.commands;

import org.geogit.api.CommandLocator;
import org.geogit.api.GeogitTransaction;
import org.geogit.api.plumbing.TransactionEnd;
import org.geogit.web.api.AbstractWebAPICommand;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.CommandSpecException;
import org.geogit.web.api.ResponseWriter;

/**
 * Interface for the TransactionEnd operation in GeoGit.
 * 
 * Web interface for {@link TransactionEnd}
 */

public class EndTransaction extends AbstractWebAPICommand {

    private boolean cancel;

    /**
     * Mutator for the cancel variable
     * 
     * @param cancel - true to abort all changes made in this transaction
     */
    public void setCancel(boolean cancel) {
        this.cancel = cancel;
    }

    /**
     * Runs the command and builds the appropriate response.
     * 
     * @param context - the context to use for this command
     * 
     * @throws CommandSpecException
     */
    @Override
    public void run(CommandContext context) {
        if (this.getTransactionId() == null) {
            throw new CommandSpecException("There isn't a transaction to end.");
        }

        final CommandLocator transaction = this.getCommandLocator(context);

        final boolean closed = context.getGeoGIT().command(TransactionEnd.class).setCancel(cancel)
                .setTransaction((GeogitTransaction) transaction).call();

        context.setResponseContent(new CommandResponse() {

            @Override
            public void write(ResponseWriter out) throws Exception {
                out.start();
                if (closed) {
                    out.writeTransactionId(null);
                } else {
                    out.writeTransactionId(getTransactionId());
                }
                out.finish();
            }
        });
    }
}
