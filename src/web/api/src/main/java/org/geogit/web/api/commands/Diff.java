package org.geogit.web.api.commands;

import java.util.Iterator;

import org.geogit.api.GeoGIT;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.porcelain.DiffOp;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.CommandSpecException;
import org.geogit.web.api.ResponseWriter;
import org.geogit.web.api.WebAPICommand;

/**
 * Interface for the Diff operation in GeoGit.
 * 
 * Web interface for {@link DiffOp}
 */

public class Diff implements WebAPICommand {
    private String oldRefSpec;

    private String newRefSpec;

    private String pathFilter;

    private boolean showGeometryChanges = false;

    /**
     * Mutator for the oldRefSpec variable
     * 
     * @param oldRefSpec - the old ref spec to diff against
     */
    public void setOldRefSpec(String oldRefSpec) {
        this.oldRefSpec = oldRefSpec;
    }

    /**
     * Mutator for the newRefSpec variable
     * 
     * @param newRefSpec - the new ref spec to diff against
     */
    public void setNewRefSpec(String newRefSpec) {
        this.newRefSpec = newRefSpec;
    }

    /**
     * Mutator for the pathFilter variable
     * 
     * @param pathFilter - a path to filter the diff by
     */
    public void setPathFilter(String pathFilter) {
        this.pathFilter = pathFilter;
    }

    /**
     * Mutator for only displaying geometry changes.
     * 
     * @param showGeometryChanges whether or not to only display geometry changes
     */
    public void setShowGeometryChanges(boolean showGeometryChanges) {
        this.showGeometryChanges = showGeometryChanges;
    }

    /**
     * Runs the command and builds the appropriate response
     * 
     * @param context - the context to use for this command
     * 
     * @throws CommandSpecException
     */
    @Override
    public void run(CommandContext context) {
        if (oldRefSpec == null || oldRefSpec.trim().isEmpty()) {
            throw new CommandSpecException("No old ref spec");
        }

        final GeoGIT geogit = context.getGeoGIT();

        final Iterator<DiffEntry> diff = geogit.command(DiffOp.class).setOldVersion(oldRefSpec)
                .setNewVersion(newRefSpec).setFilter(pathFilter).call();

        context.setResponseContent(new CommandResponse() {
            @Override
            public void write(ResponseWriter out) throws Exception {
                out.start();
                if (showGeometryChanges) {
                    out.writeGeometryChanges(geogit, diff);
                } else {
                    out.writeDiffEntries("diff", 0, -1, diff);
                }
                out.finish();
            }
        });
    }
}
