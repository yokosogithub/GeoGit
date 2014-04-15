/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.plumbing.merge;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.plumbing.DiffFeature;
import org.geogit.api.plumbing.DiffTree;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.ResolveObjectType;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.diff.AttributeDiff;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.diff.FeatureDiff;
import org.geogit.repository.DepthSearch;
import org.geogit.repository.Repository;
import org.opengis.feature.type.PropertyDescriptor;

import com.google.common.base.Optional;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

/**
 * Reports conflicts between changes introduced by a given commit and the last commit of the current
 * head. That should give information about whether the specified commit can be applied safely on
 * the current branch without overwriting changes. It classifies the changes of the commit in
 * conflicting or unconflicting, so they can be applied partially
 */
public class ReportCommitConflictsOp extends AbstractGeoGitOp<MergeScenarioReport> {

    private RevCommit commit;

    private Repository repository;

    @Inject
    public ReportCommitConflictsOp(Repository repository) {
        this.repository = repository;
    }

    /**
     * @param commit the commit with the changes to apply {@link RevCommit}
     */
    public ReportCommitConflictsOp setCommit(RevCommit commit) {
        this.commit = commit;
        return this;
    }

    @Override
    public MergeScenarioReport call() {

        MergeScenarioReport report = new MergeScenarioReport();

        ObjectId parentCommitId = ObjectId.NULL;
        if (commit.getParentIds().size() > 0) {
            parentCommitId = commit.getParentIds().get(0);
        }
        ObjectId parentTreeId = ObjectId.NULL;
        if (repository.commitExists(parentCommitId)) {
            parentTreeId = repository.getCommit(parentCommitId).getTreeId();
        }
        // get changes
        Iterator<DiffEntry> diffs = command(DiffTree.class).setOldTree(parentTreeId)
                .setNewTree(commit.getTreeId()).setReportTrees(true).call();

        while (diffs.hasNext()) {
            DiffEntry diff = diffs.next();
            String path = diff.oldPath() == null ? diff.newPath() : diff.oldPath();
            Optional<RevObject> obj = command(RevObjectParse.class).setRefSpec(
                    Ref.HEAD + ":" + path).call();
            switch (diff.changeType()) {
            case ADDED:
                if (obj.isPresent()) {
                    TYPE type = command(ResolveObjectType.class).setObjectId(
                            diff.getNewObject().objectId()).call();
                    if (TYPE.TREE.equals(type)) {
                        NodeRef headVersion = command(FindTreeChild.class).setChildPath(path)
                                .setParent(repository.getOrCreateHeadTree()).call().get();
                        if (!headVersion.getMetadataId()
                                .equals(diff.getNewObject().getMetadataId())) {
                            report.addConflict(new Conflict(path, ObjectId.NULL, diff
                                    .getNewObject().getMetadataId(), headVersion.getMetadataId()));
                        }
                    } else {
                        if (!obj.get().getId().equals(diff.newObjectId())) {
                            report.addConflict(new Conflict(path, ObjectId.NULL,
                                    diff.newObjectId(), obj.get().getId()));
                        }
                    }
                } else {
                    report.addUnconflicted(diff);
                }
                break;
            case REMOVED:
                if (obj.isPresent()) {
                    if (obj.get().getId().equals(diff.oldObjectId())) {
                        report.addUnconflicted(diff);
                    } else {
                        report.addConflict(new Conflict(path, diff.oldObjectId(), ObjectId.NULL,
                                obj.get().getId()));
                    }
                }
                break;
            case MODIFIED:
                TYPE type = command(ResolveObjectType.class).setObjectId(
                        diff.getNewObject().objectId()).call();
                if (TYPE.TREE.equals(type)) {
                    // TODO:see how to do this. For now, we will pass any change as a conflicted
                    // one
                    report.addUnconflicted(diff);
                } else {
                    String refSpec = Ref.HEAD + ":" + path;
                    obj = command(RevObjectParse.class).setRefSpec(refSpec).call();
                    if (!obj.isPresent()) {
                        // git reports this as a conflict but does not mark as conflicted, just adds
                        // the missing file.
                        // We add it and consider it unconflicted
                        report.addUnconflicted(diff);
                        break;
                    }
                    RevFeature feature = (RevFeature) obj.get();
                    DepthSearch depthSearch = new DepthSearch(repository.getObjectDatabase());
                    Optional<NodeRef> noderef = depthSearch
                            .find(this.getWorkTree().getTree(), path);
                    RevFeatureType featureType = command(RevObjectParse.class)
                            .setObjectId(noderef.get().getMetadataId()).call(RevFeatureType.class)
                            .get();
                    ImmutableList<PropertyDescriptor> descriptors = featureType.sortedDescriptors();
                    FeatureDiff featureDiff = command(DiffFeature.class)
                            .setOldVersion(Suppliers.ofInstance(diff.getOldObject()))
                            .setNewVersion(Suppliers.ofInstance(diff.getNewObject())).call();
                    Set<Entry<PropertyDescriptor, AttributeDiff>> attrDiffs = featureDiff
                            .getDiffs().entrySet();
                    RevFeature newFeature = command(RevObjectParse.class)
                            .setObjectId(diff.newObjectId()).call(RevFeature.class).get();
                    boolean ok = true;
                    for (Iterator<Entry<PropertyDescriptor, AttributeDiff>> iterator = attrDiffs
                            .iterator(); iterator.hasNext() && ok;) {
                        Entry<PropertyDescriptor, AttributeDiff> entry = iterator.next();
                        AttributeDiff attrDiff = entry.getValue();
                        PropertyDescriptor descriptor = entry.getKey();
                        switch (attrDiff.getType()) {
                        case ADDED:
                            if (descriptors.contains(descriptor)) {
                                ok = false;
                            }
                            break;
                        case REMOVED:
                        case MODIFIED:
                            if (!descriptors.contains(descriptor)) {
                                ok = false;
                                break;
                            }
                            for (int i = 0; i < descriptors.size(); i++) {
                                if (descriptors.get(i).equals(descriptor)) {
                                    Optional<Object> value = feature.getValues().get(i);
                                    Optional<Object> newValue = newFeature.getValues().get(i);
                                    if (!newValue.equals(value)) { // if it's going to end up
                                                                   // setting the same value, it is
                                                                   // compatible, so no need to
                                                                   // check
                                        if (!attrDiff.canBeAppliedOn(value)) {
                                            ok = false;
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    if (ok) {
                        report.addUnconflicted(diff);
                    } else {
                        report.addConflict(new Conflict(path, diff.oldObjectId(), diff
                                .newObjectId(), obj.get().getId()));
                    }
                }

                break;
            }

        }

        return report;

    }
}
