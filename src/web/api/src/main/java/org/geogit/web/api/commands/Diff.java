package org.geogit.web.api.commands;

import java.util.Iterator;
import java.util.List;

import org.geogit.api.FeatureBuilder;
import org.geogit.api.GeoGIT;
import org.geogit.api.GeogitSimpleFeature;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.diff.DiffEntry.ChangeType;
import org.geogit.api.porcelain.DiffOp;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.CommandSpecException;
import org.geogit.web.api.ResponseWriter;
import org.geogit.web.api.WebAPICommand;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

/**
 * Interface for the Diff operation in GeoGit.
 * 
 * Web interface for {@link DiffOp}
 */

public class Diff implements WebAPICommand {
    private String oldRefSpec;

    private String newRefSpec;

    private String pathFilter;

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

        final List<GeogitSimpleFeature> features = Lists.newLinkedList();
        final List<ChangeType> changes = Lists.newLinkedList();
        while (diff.hasNext()) {
            DiffEntry diffEntry = diff.next();
            Optional<RevObject> feature = Optional.absent();
            Optional<RevObject> type = Optional.absent();
            if (diffEntry.changeType() == ChangeType.ADDED
                    || diffEntry.changeType() == ChangeType.MODIFIED) {
                feature = geogit.command(RevObjectParse.class).setObjectId(diffEntry.newObjectId())
                        .call();
                type = geogit.command(RevObjectParse.class)
                        .setObjectId(diffEntry.getNewObject().getMetadataId()).call();
            } else if (diffEntry.changeType() == ChangeType.REMOVED) {
                feature = geogit.command(RevObjectParse.class).setObjectId(diffEntry.oldObjectId())
                        .call();
                type = geogit.command(RevObjectParse.class)
                        .setObjectId(diffEntry.getOldObject().getMetadataId()).call();
            }
            if (feature.isPresent() && feature.get() instanceof RevFeature && type.isPresent()
                    && type.get() instanceof RevFeatureType) {
                RevFeature revFeature = (RevFeature) feature.get();
                FeatureBuilder builder = new FeatureBuilder((RevFeatureType) type.get());
                GeogitSimpleFeature simpleFeature = (GeogitSimpleFeature) builder.build(revFeature
                        .getId().toString(), revFeature);
                features.add(simpleFeature);
                changes.add(diffEntry.changeType());
            }
        }

        context.setResponseContent(new CommandResponse() {
            @Override
            public void write(ResponseWriter out) throws Exception {
                out.start();
                out.writeDiffResponse(features.iterator(), changes.iterator());
                out.finish();
            }
        });
    }
}
