/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.geotools.data;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import org.geogit.api.GeoGIT;
import org.geogit.api.NodeRef;
import org.geogit.api.RevObject.TYPE;
import org.geogit.repository.Repository;
import org.geogit.repository.WorkingTree;
import org.geotools.data.DataStore;
import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.NameImpl;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;

/**
 *
 */
public class GeoGitDataStore extends ContentDataStore implements DataStore {

    private final GeoGIT geogit;

    public GeoGitDataStore(GeoGIT geogit) {
        super();
        Preconditions.checkNotNull(geogit);
        Preconditions.checkNotNull(geogit.getRepository(), "No repository exists at %s", geogit
                .getPlatform().pwd());

        this.geogit = geogit;
    }

    public GeoGIT getGeogit() {
        return geogit;
    }

    public Name getDescriptorName(NodeRef treeRef) {
        Preconditions.checkNotNull(treeRef);
        Preconditions.checkArgument(TYPE.TREE.equals(treeRef.getType()));
        Preconditions.checkArgument(!treeRef.getMetadataId().isNull(),
                "NodeRef '%s' is not a feature type reference", treeRef.path());

        return new NameImpl(getNamespaceURI(), NodeRef.nodeFromPath(treeRef.path()));
    }

    public NodeRef findTypeRef(Name typeName) {
        Preconditions.checkNotNull(typeName);

        final String localName = typeName.getLocalPart();
        List<NodeRef> typeRefs = geogit.getRepository().getWorkingTree().getFeatureTypeTrees();
        Collection<NodeRef> matches = Collections2.filter(typeRefs, new Predicate<NodeRef>() {
            @Override
            public boolean apply(NodeRef input) {
                return NodeRef.nodeFromPath(input.path()).equals(localName);
            }
        });
        switch (matches.size()) {
        case 0:
            throw new NoSuchElementException();
        case 1:
            return matches.iterator().next();
        default:
            throw new IllegalArgumentException(String.format(
                    "More than one tree ref matches the name %s: %s", localName, matches));
        }
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    protected GeogitTransactionState createContentState(ContentEntry entry) {
        return new GeogitTransactionState(entry);
    }

    @Override
    protected ImmutableList<Name> createTypeNames() throws IOException {
        Repository repository = geogit.getRepository();
        WorkingTree workingTree = repository.getWorkingTree();
        List<NodeRef> typeTrees = workingTree.getFeatureTypeTrees();
        Function<NodeRef, Name> function = new Function<NodeRef, Name>() {
            @Override
            public Name apply(NodeRef treeRef) {
                return getDescriptorName(treeRef);
            }
        };
        return ImmutableList.copyOf(Collections2.transform(typeTrees, function));
    }

    @Override
    protected ContentFeatureSource createFeatureSource(ContentEntry entry) throws IOException {
        return new GeogitFeatureSource(entry);
    }

    @Override
    public void createSchema(SimpleFeatureType featureType) throws IOException {
        Repository repository = geogit.getRepository();
        WorkingTree workingTree = repository.getWorkingTree();
        String treePath = featureType.getName().getLocalPart();
        try {
            workingTree.createTypeTree(treePath, featureType);
        } catch (IllegalArgumentException alreadyExists) {
            throw new IOException(alreadyExists.getMessage(), alreadyExists);
        }
    }
}
