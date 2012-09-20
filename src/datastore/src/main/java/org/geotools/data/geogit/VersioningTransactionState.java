/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.data.geogit;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.geogit.api.GeoGIT;
import org.geogit.api.NodeRef;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.CommitStateResolver;
import org.geogit.api.porcelain.NothingToCommitException;
import org.geogit.repository.StagingArea;
import org.geogit.repository.WorkingTree;
import org.geogit.storage.hessian.GeoToolsRevFeature;
import org.geotools.data.Transaction;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.identity.FeatureId;
import org.opengis.util.ProgressListener;

import com.google.common.base.Function;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Lists;

@SuppressWarnings("rawtypes")
public class VersioningTransactionState implements Transaction.State {

    public static final VersioningTransactionState VOID = new VersioningTransactionState(null) {

        @Override
        public List<FeatureId> stageInsert(final Name typeName, FeatureCollection affectedFeatures)
                throws Exception {
            return Collections.emptyList();
        }

        @Override
        public void stageUpdate(final Name typeName, final FeatureCollection affectedFeatures)
                throws Exception {
        }

        @Override
        public void stageDelete(Name typeName, Filter filter, FeatureCollection affectedFeatures)
                throws Exception {
        }

        @Override
        public void stageRename(final Name typeName, final String oldFid, final String newFid) {
        }

    };

    static {
        // only set resolver if not overriden by application
        CommitStateResolver current = GeoGIT.getCommitStateResolver();
        if (GeoGIT.DEFAULT_COMMIT_RESOLVER.equals(current)) {
            GeoGIT.setCommitStateResolver(new GeoToolsCommitStateResolver());
        }
    }

    private static final ProgressListener NULL_PROGRESS_LISTENER = new NullProgressListener();

    private static final Logger LOGGER = Logging.getLogger(VersioningTransactionState.class);

    private Transaction transaction;

    private GeoGIT geoGit;

    private String id;

    private Set<Name> changedTypes;

    public VersioningTransactionState(final GeoGIT geoGit) {
        this.geoGit = geoGit;
        this.id = UUID.randomUUID().toString();
        this.changedTypes = Collections.synchronizedSet(new HashSet<Name>());
    }

    @Override
    public void setTransaction(final Transaction transaction) {
        if (transaction != null) {
            // configure
            if (this.transaction == null) {
                this.transaction = transaction;
            } else {
                LOGGER.fine("Transaction being hot replaced!");
                this.transaction = transaction;
            }
        } else {
            // Any thing to close() or cleanup?
            this.transaction = null;
        }
    }

    @Override
    public void addAuthorization(String AuthID) throws IOException {
        // no security hooks provided for transaction state locking
    }

    @Override
    public void commit() throws IOException {
        LOGGER.info("Committing changeset " + id);
        try {
            GeoToolsCommitStateResolver.CURRENT_TRANSACTION.set(transaction);
            // final NodeRef branch =
            // geoGit.checkout().setName(transactionID).call();
            // commit to the branch
            RevCommit commit = null;
            // checkout master
            // final NodeRef master = geoGit.checkout().setName("master").call();
            // merge branch to master
            // MergeResult mergeResult = geoGit.merge().include(branch).call();
            // TODO: check mergeResult is success?
            // geoGit.branchDelete().setName(transactionID).call();
            try {
                // geoGit.add().call();
                CommitOp commitOp = geoGit.commit();
                commit = commitOp.call();
                LOGGER.info("New commit: " + commit);
            } catch (NothingToCommitException emptyCommit) {
                LOGGER.warning("GeoGit threw NothingToCommitException, this may be due to the transaction having not changed the repository contents");
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(e);
            }
        } finally {
            GeoToolsCommitStateResolver.CURRENT_TRANSACTION.remove();
        }
    }

    @Override
    public void rollback() throws IOException {
        geoGit.getRepository().getIndex().reset();
    }

    /**
     * @param transactionID
     * @param typeName
     * @param affectedFeatures
     * @return the list of feature ids of the inserted features, in the order they were added
     * @throws Exception
     */
    public List<FeatureId> stageInsert(final Name typeName, FeatureCollection affectedFeatures)
            throws Exception {
        return stageInsert(typeName, affectedFeatures, false);
    }

    private static Function<NodeRef, FeatureId> NodeRefToFeatureId = new Function<NodeRef, FeatureId>() {

        private final FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory();

        @Override
        public FeatureId apply(NodeRef input) {
            return filterFactory.featureId(input.getPath(), input.getObjectId().toString());
        }

    };

    public List<FeatureId> stageInsert(final Name typeName, FeatureCollection affectedFeatures,
            final boolean forceUseProvidedFIDs) throws Exception {

        changedTypes.add(typeName);

        WorkingTree workingTree = geoGit.getRepository().getWorkingTree();
        List<NodeRef> inserted = new LinkedList<NodeRef>();

        int collectionSize = affectedFeatures.size();
        String treePath = typeName.getLocalPart();
        final FeatureIterator features = affectedFeatures.features();
        try {
            Iterator<RevFeature> iterator = new RevFeatureIterator(features);
            workingTree.insert(treePath, iterator, forceUseProvidedFIDs, NULL_PROGRESS_LISTENER,
                    inserted, collectionSize);
        } finally {
            features.close();
        }
        geoGit.add().call();
        return Lists.transform(inserted, NodeRefToFeatureId);
    }

    public void stageUpdate(final Name typeName, final FeatureCollection newValues)
            throws Exception {

        changedTypes.add(typeName);

        WorkingTree workingTree = geoGit.getRepository().getWorkingTree();

        final FeatureIterator features = newValues.features();
        try {
            Integer size = newValues.size();
            String treePath = typeName.getLocalPart();
            workingTree.update(treePath, new RevFeatureIterator(features), NULL_PROGRESS_LISTENER,
                    size);
        } finally {
            features.close();
        }
        geoGit.add().call();
    }

    public void stageDelete(final Name typeName, final Filter filter,
            final FeatureCollection affectedFeatures) throws Exception {

        changedTypes.add(typeName);

        WorkingTree workingTree = geoGit.getRepository().getWorkingTree();
        QName qName = new QName(typeName.getNamespaceURI(), typeName.getLocalPart());

        final FeatureIterator features = affectedFeatures.features();
        try {
            workingTree.delete(qName, filter, new RevFeatureIterator(features));
        } finally {
            features.close();
        }

        geoGit.add().call();

    }

    private static class RevFeatureIterator extends AbstractIterator<RevFeature> implements
            Iterator<RevFeature> {

        private FeatureIterator features;

        public RevFeatureIterator(final FeatureIterator features) {
            this.features = features;
        }

        @Override
        protected RevFeature computeNext() {
            if (!features.hasNext()) {
                return super.endOfData();
            }
            return new GeoToolsRevFeature(features.next());
        }
    }

    public void stageRename(final Name typeName, final String oldFid, final String newFid) {

        StagingArea index = geoGit.getRepository().getIndex();

        final String localPart = typeName.getLocalPart();

        String from = NodeRef.appendChild(localPart, oldFid);
        String to = NodeRef.appendChild(localPart, newFid);

        index.renamed(from, to);
    }

}
