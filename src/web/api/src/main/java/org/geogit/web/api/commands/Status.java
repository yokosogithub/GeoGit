package org.geogit.web.api.commands;

import org.geogit.api.GeoGIT;
import org.geogit.api.Ref;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.DiffIndex;
import org.geogit.api.plumbing.DiffWorkTree;
import org.geogit.api.plumbing.RefParse;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.ResponseWriter;
import org.geogit.web.api.WebAPICommand;

import com.google.common.base.Optional;

/**
 * Web version of the Status operation in GeoGit's CLI. Lists the current branch as well as the
 * current staged and unstaged changes.
 * 
 * Web implementation of {@link Status}
 */
public class Status implements WebAPICommand {

    int offset = 0;

    int limit = -1;

    /**
     * Mutator for the offset variable
     * 
     * @param offset - the offset to start listing staged and unstaged changes
     */
    public void setOffset(int offset) {
        this.offset = offset;
    }

    /**
     * Mutator for the limit variable
     * 
     * @param limit - the number of staged and unstaged changes to make
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     * Runs the command builds the appropriate command
     * 
     * @param context - the context to use for this command
     */
    @Override
    public void run(CommandContext context) {
        final GeoGIT geogit = context.getGeoGIT();

        final String pathFilter = null;
        final Optional<Ref> currHead = geogit.command(RefParse.class).setName(Ref.HEAD).call();

        context.setResponseContent(new CommandResponse() {
            @Override
            public void write(ResponseWriter writer) throws Exception {
                writer.start();
                if (!currHead.isPresent()) {
                    writer.writeErrors("Repository has no HEAD.");
                } else {
                    if (currHead.get() instanceof SymRef) {
                        final SymRef headRef = (SymRef) currHead.get();
                        writer.writeHeaderElements("branch", Ref.localName(headRef.getTarget()));
                    }
                }

                writer.writeStaged(geogit.command(DiffIndex.class).addFilter(pathFilter), offset,
                        limit);
                writer.writeUnstaged(geogit.command(DiffWorkTree.class).setFilter(pathFilter),
                        offset, limit);

                writer.finish();
            }
        });

    }

}
