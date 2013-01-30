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

public class Diff implements WebAPICommand {
    private String oldRefSpec;

    private String newRefSpec;

    private String pathFilter;

    public void setOldRefSpec(String oldRefSpec) {
        this.oldRefSpec = oldRefSpec;
    }

    public void setNewRefSpec(String newRefSpec) {
        this.newRefSpec = newRefSpec;
    }

    public void setPathFilter(String pathFilter) {
        this.pathFilter = pathFilter;
    }

    @Override
    public void run(CommandContext context) {
        if (oldRefSpec == null || oldRefSpec.trim().isEmpty()) {
            throw new CommandSpecException("No old ref spec");
        }
        if (newRefSpec == null || newRefSpec.trim().isEmpty()) {
            throw new CommandSpecException("No new ref spec");
        }
        final GeoGIT geogit = context.getGeoGIT();

        final Iterator<DiffEntry> diff = geogit.command(DiffOp.class).setOldVersion(oldRefSpec)
                .setNewVersion(newRefSpec).setFilter(pathFilter).call();

        context.setResponseContent(new CommandResponse() {
            @Override
            public void write(ResponseWriter out) throws Exception {
                out.start();
                out.writeDiffEntries("diff", 0, -1, diff);
                out.finish();
            }
        });
    }
}
