package org.geogit.web.api.commands;

import org.geogit.api.CommandLocator;
import org.geogit.api.Ref;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.porcelain.CheckoutOp;
import org.geogit.web.api.AbstractWebAPICommand;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.CommandSpecException;
import org.geogit.web.api.ResponseWriter;

import com.google.common.base.Optional;

public class CheckoutWebOp extends AbstractWebAPICommand {

    private String branchOrCommit;

    /**
     * Mutator for the branchOrCommit variable
     * 
     * @param commit - the branch or commit to checkout
     */
    public void setName(String branchOrCommit) {
        this.branchOrCommit = branchOrCommit;
    }

    /**
     * Runs the command and builds the appropriate response
     * 
     * @throws CommandSpecException
     */
    @Override
    public void run(CommandContext context) {
        if (this.getTransactionId() == null) {
            throw new CommandSpecException(
                    "No transaction was specified, checkout requires a transaction to preserve the stability of the repository.");
        }
        if (branchOrCommit == null) {
            throw new CommandSpecException("No branch or commit specified for checkout.");
        }

        final CommandLocator geogit = this.getCommandLocator(context);

        Optional<Ref> head = geogit.command(RefParse.class).setName(Ref.HEAD).call();

        if (!head.isPresent()) {
            throw new CommandSpecException("Repository has no HEAD, can't merge.");
        }

        final String target = ((SymRef) head.get()).getTarget();

        geogit.command(CheckoutOp.class).setSource(branchOrCommit).call();

        context.setResponseContent(new CommandResponse() {
            @Override
            public void write(ResponseWriter out) throws Exception {
                out.start();
                out.writeElement("OldTarget", target);
                out.writeElement("NewTarget", branchOrCommit);
                out.finish();
            }
        });
    }
}
