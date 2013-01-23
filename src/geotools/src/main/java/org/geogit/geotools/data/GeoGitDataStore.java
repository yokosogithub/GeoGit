/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.geotools.data;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;

import org.geogit.api.CommandLocator;
import org.geogit.api.GeoGIT;
import org.geogit.api.GeogitTransaction;
import org.geogit.api.NodeRef;
import org.geogit.api.Ref;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.ForEachRef;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.TransactionBegin;
import org.geogit.api.porcelain.AddOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.repository.WorkingTree;
import org.geotools.data.DataStore;
import org.geotools.data.Transaction;
import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.data.store.ContentState;
import org.geotools.feature.NameImpl;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

/**
 *
 */
public class GeoGitDataStore extends ContentDataStore implements DataStore {

    private final GeoGIT geogit;

    /** @see #setBranch(String) */
    private String branch;

    public GeoGitDataStore(GeoGIT geogit) {
        super();
        Preconditions.checkNotNull(geogit);
        Preconditions.checkNotNull(geogit.getRepository(), "No repository exists at %s", geogit
                .getPlatform().pwd());

        this.geogit = geogit;
    }

    /**
     * Instructs the datastore to operate against the specified branch, or against the checked out
     * branch, whatever it is, if the argument is {@code null}.
     * 
     * @param branch the name of the branch to work against, or {@code null} to default to the
     *        currently checked out branch
     * @throws IllegalArgumentException if {@code branchName} is not null and no such branch exists
     *         in the repository
     */
    public void setBranch(@Nullable final String branchName) throws IllegalArgumentException {
        if (branchName != null) {
            Optional<Ref> branchRef = getCommandLocator(null).command(RefParse.class)
                    .setName(branchName).call();
            if (!branchRef.isPresent()) {
                throw new IllegalArgumentException(String.format(
                        "Branch %s does not exist, available branches: %s", branchName,
                        getAvailableBranches()));
            }
            String refName = branchRef.get().getName();
            Preconditions.checkArgument(refName.startsWith(Ref.HEADS_PREFIX),
                    "%s is not a local branch: %s", branchName, refName);
        }
        this.branch = branchName;
    }

    /**
     * @return the configured name of the branch this datastore works against, or {@code null} if no
     *         branch in particular has been set, meaning the data store works against whatever the
     *         currently checked out branch is.
     */
    @Nullable
    public String getConfiguredBranch() {
        return this.branch;
    }

    /**
     * @return the name of the currently checked out branch in the repository, not necessarily equal
     *         to {@link #getConfiguredBranch()}, or {@code null} in the (improbable) case HEAD is
     *         on a dettached state (i.e. no local branch is currently checked out)
     */
    @Nullable
    public String getCheckedOutBranch() {
        Optional<Ref> head = getCommandLocator(null).command(RefParse.class).setName(Ref.HEAD)
                .call();
        if (!head.isPresent()) {
            return null;
        }
        Ref headRef = head.get();
        if (!(headRef instanceof SymRef)) {
            return null;
        }
        String refName = ((SymRef) headRef).getTarget();
        Preconditions.checkState(refName.startsWith(Ref.HEADS_PREFIX));
        String branchName = refName.substring(Ref.HEADS_PREFIX.length());
        return branchName;
    }

    public ImmutableList<String> getAvailableBranches() {
        ImmutableSet<Ref> heads = getCommandLocator(null).command(ForEachRef.class)
                .setPrefixFilter(Ref.HEADS_PREFIX).call();
        List<String> list = Lists.newArrayList(Collections2.transform(heads,
                new Function<Ref, String>() {

                    @Override
                    public String apply(Ref ref) {
                        String branchName = ref.getName().substring(Ref.HEADS_PREFIX.length());
                        return branchName;
                    }
                }));
        Collections.sort(list);
        return ImmutableList.copyOf(list);
    }

    public WorkingTree getWorkingTree(@Nullable Transaction transaction) {
        CommandLocator commandLocator = getCommandLocator(transaction);
        if (commandLocator instanceof GeogitTransaction) {
            return ((GeogitTransaction) commandLocator).getWorkingTree();
        }
        return geogit.getRepository().getWorkingTree();
    }

    public CommandLocator getCommandLocator(@Nullable Transaction transaction) {
        CommandLocator commandLocator = null;

        if (transaction != null && !Transaction.AUTO_COMMIT.equals(transaction)) {
            GeogitTransactionState state;
            state = (GeogitTransactionState) transaction.getState(GeogitTransactionState.class);
            Optional<GeogitTransaction> geogitTransaction = state.getGeogitTransaction();
            if (geogitTransaction.isPresent()) {
                commandLocator = geogitTransaction.get();
            }
        }

        if (commandLocator == null) {
            commandLocator = geogit.getCommandLocator();
        }
        return commandLocator;
    }

    public Name getDescriptorName(NodeRef treeRef) {
        Preconditions.checkNotNull(treeRef);
        Preconditions.checkArgument(TYPE.TREE.equals(treeRef.getType()));
        Preconditions.checkArgument(!treeRef.getMetadataId().isNull(),
                "NodeRef '%s' is not a feature type reference", treeRef.path());

        return new NameImpl(getNamespaceURI(), NodeRef.nodeFromPath(treeRef.path()));
    }

    public NodeRef findTypeRef(Name typeName, @Nullable Transaction tx) {
        Preconditions.checkNotNull(typeName);

        final String localName = typeName.getLocalPart();
        final WorkingTree workingTree = getWorkingTree(tx);
        List<NodeRef> typeRefs = workingTree.getFeatureTypeTrees();
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
    protected ContentState createContentState(ContentEntry entry) {
        return new ContentState(entry);
    }

    @Override
    protected ImmutableList<Name> createTypeNames() throws IOException {
        WorkingTree workingTree = getWorkingTree(null);
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
        return new GeogitFeatureStore(entry);
    }

    @Override
    public void createSchema(SimpleFeatureType featureType) throws IOException {
        GeogitTransaction tx = getCommandLocator(null).command(TransactionBegin.class).call();
        boolean abort = false;
        try {
            String treePath = featureType.getName().getLocalPart();
            WorkingTree workingTree = tx.getWorkingTree();
            workingTree.createTypeTree(treePath, featureType);
            tx.command(AddOp.class).addPattern(treePath).call();
            tx.command(CommitOp.class).setMessage("Created feature type tree " + treePath).call();
            tx.commit();
        } catch (IllegalArgumentException alreadyExists) {
            abort = true;
            throw new IOException(alreadyExists.getMessage(), alreadyExists);
        } catch (Exception e) {
            abort = true;
            throw Throwables.propagate(e);
        } finally {
            if (abort) {
                tx.abort();
            }
        }
    }
}
