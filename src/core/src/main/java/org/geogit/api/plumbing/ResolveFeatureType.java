/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.Node;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevTree;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * Resolves the feature type associated to a refspec.
 * 
 * If the refspecs resolves to a tree, it returns the default feature type of the tree. If it
 * resolves to a feature, it returns its feature type.
 * 
 * @see RevParse
 * @see FindTreeChild
 */
public class ResolveFeatureType extends AbstractGeoGitOp<Optional<RevFeatureType>> {

    private String refSpec;

    /**
     * @param treeIshRefSpec a ref spec that resolves to the tree or feature node holding the
     *        {@link Node#getMetadataId() metadataId} of the {@link RevFeatureType} to parse It can
     *        be a full refspec or just a path. In this last case, the path is assumed to refer to
     *        the working tree, and "WORK_HEAD:" is appended to the path to create the full refspec
     * @return
     */
    public ResolveFeatureType setRefSpec(String refSpec) {
        this.refSpec = refSpec;
        return this;
    }

    @Override
    public Optional<RevFeatureType> call() {
        Preconditions.checkState(refSpec != null, "ref spec has not been set.");
        final String fullRefspec;
        if (refSpec.contains(":")) {
            fullRefspec = refSpec;
        } else {
            fullRefspec = Ref.WORK_HEAD + ":" + refSpec;
        }
        final String ref = fullRefspec.substring(0, fullRefspec.indexOf(':'));
        final String path = fullRefspec.substring(fullRefspec.indexOf(':') + 1);

        ObjectId parentId = command(ResolveTreeish.class).setTreeish(ref).call().get();
        RevTree parent = command(RevObjectParse.class).setObjectId(parentId).call(RevTree.class)
                .get();
        Optional<NodeRef> node = command(FindTreeChild.class).setParent(parent).setChildPath(path)
                .setIndex(true).call();
        if (!node.isPresent()) {
            return Optional.absent();
        }
        NodeRef found = node.get();
        ObjectId metadataID = found.getMetadataId();
        Optional<RevFeatureType> ft = command(RevObjectParse.class).setObjectId(metadataID).call(
                RevFeatureType.class);
        return ft;
    }
}