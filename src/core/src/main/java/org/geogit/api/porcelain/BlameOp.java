/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import java.util.Iterator;
import java.util.Map;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.plumbing.DiffFeature;
import org.geogit.api.plumbing.ResolveFeatureType;
import org.geogit.api.plumbing.ResolveObjectType;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.plumbing.diff.AttributeDiff;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.diff.FeatureDiff;
import org.geogit.api.porcelain.BlameException.StatusCode;
import org.geogit.di.CanRunDuringConflict;
import org.opengis.feature.type.PropertyDescriptor;

import com.google.common.base.Optional;
import com.google.common.base.Suppliers;

/**
 * Creates a report that contains information about who was the last to change each attribute in a
 * feature
 * 
 */
@CanRunDuringConflict
public class BlameOp extends AbstractGeoGitOp<BlameReport> {

    private String path;

    private ObjectId commit;

    /**
     * Sets the path of the feature to use
     * 
     * @param String path
     * @return
     */
    public BlameOp setPath(String path) {
        this.path = path;
        return this;
    }

    /**
     * Sets the commit to blame from
     * 
     * @param ObjectId commit
     * @return
     */
    public BlameOp setCommit(ObjectId commit) {
        this.commit = commit;
        return this;
    }

    @Override
    public BlameReport call() {
        String fullPath = (commit != null ? commit.toString() : Ref.HEAD) + ":" + path;
        Optional<ObjectId> id = command(RevParse.class).setRefSpec(fullPath).call();
        if (!id.isPresent()) {
            throw new BlameException(StatusCode.FEATURE_NOT_FOUND);
        }
        TYPE type = command(ResolveObjectType.class).setObjectId(id.get()).call();
        if (!type.equals(TYPE.FEATURE)) {
            throw new BlameException(StatusCode.PATH_NOT_FEATURE);
        }
        Optional<RevFeatureType> featureType = command(ResolveFeatureType.class).setRefSpec(path)
                .call();

        BlameReport report = new BlameReport(featureType.get());

        Iterator<RevCommit> log = command(LogOp.class).addPath(path).setUntil(commit).call();
        RevCommit commit = log.next();

        while (!report.isComplete()) {
            if (!log.hasNext()) {
                String refSpec = commit.getId().toString() + ":" + path;
                RevFeature feature = command(RevObjectParse.class).setRefSpec(refSpec)
                        .call(RevFeature.class).get();
                report.setFirstVersion(feature, commit);
                break;
            }
            RevCommit commitB = log.next();
            Iterator<DiffEntry> diffs = command(DiffOp.class).setNewVersion(commit.getId())
                    .setOldVersion(commitB.getId()).setReportTrees(false).call();

            while (diffs.hasNext()) {
                DiffEntry diff = diffs.next();
                if (path.equals(diff.newPath())) {
                    if (diff.isAdd()) {
                        String refSpec = commit.getId().toString() + ":" + path;
                        RevFeature feature = command(RevObjectParse.class).setRefSpec(refSpec)
                                .call(RevFeature.class).get();
                        report.setFirstVersion(feature, commit);
                        break;
                    }
                    FeatureDiff featureDiff = command(DiffFeature.class)
                            .setNewVersion(Suppliers.ofInstance(diff.getNewObject()))
                            .setOldVersion(Suppliers.ofInstance(diff.getOldObject())).call();
                    Map<PropertyDescriptor, AttributeDiff> attribDiffs = featureDiff.getDiffs();
                    Iterator<PropertyDescriptor> iter = attribDiffs.keySet().iterator();
                    while (iter.hasNext()) {
                        PropertyDescriptor key = iter.next();
                        Optional<?> value = attribDiffs.get(key).getNewValue();
                        String attribute = key.getName().toString();
                        report.addDiff(attribute, value, commit);
                    }
                }

            }
            commit = commitB;
        }
        return report;
    }
}
