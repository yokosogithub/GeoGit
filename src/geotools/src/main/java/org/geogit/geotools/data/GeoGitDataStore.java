<<<<<<< .merge_file_BqYPns
/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
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
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.SymRef;
import org.geogit.api.data.FindFeatureTypeTrees;
import org.geogit.api.plumbing.ForEachRef;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.plumbing.TransactionBegin;
import org.geogit.api.porcelain.AddOp;
import org.geogit.api.porcelain.CheckoutOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.repository.WorkingTree;
import org.geotools.data.DataStore;
import org.geotools.data.Transaction;
import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.data.store.ContentState;
import org.geotools.feature.NameImpl;
import org.opengis.feature.simple.SimpleFeature;
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
 * A GeoTools {@link DataStore} that serves and edits {@link SimpleFeature}s in a geogit repository.
 * <p>
 * Multiple instances of this kind of data store may be created against the same repository,
 * possibly working against different {@link #setHead(String) heads}.
 * <p>
 * A head is any commit in GeoGit.  If a head has a branch pointing at it then
 * the store allows transactions, otherwise no data modifications may be made.
 *
 * A branch in Geogit is a separate line of history that may or may not share a common ancestor with
 * another branch. In the later case the branch is called "orphan" and by convention the default
 * branch is called "master", which is created when the geogit repo is first initialized, but does
 * not necessarily exist.
 * <p>
 * Every read operation (like in {@link #getFeatureSource(Name)}) reads committed features out of
 * the configured "head" branch. Write operations are only supported if a {@link Transaction} is
 * set, in which case a {@link GeogitTransaction} is tied to the geotools transaction by means of a
 * {@link GeogitTransactionState}. During the transaction, read operations will read from the
 * transaction's {@link WorkingTree} in order to "see" features that haven't been committed yet.
 * <p>
 * When the transaction is committed, the changes made inside that transaction are merged onto the
 * actual repository. If any other change was made to the repository meanwhile, a rebase will be
 * attempted, and the transaction commit will fail if the rebase operation finds any conflict. This
 * provides for optimistic locking and reduces thread contention.
 * 
 */
public class GeoGitDataStore extends ContentDataStore implements DataStore {

    private final GeoGIT geogit;

    /** @see #setHead(String) */
    private String refspec;

    /** When the configured head is not a branch, we disallow transactions */
    private boolean allowTransactions = true;

    public GeoGitDataStore(GeoGIT geogit) {
        super();
        Preconditions.checkNotNull(geogit);
        Preconditions.checkNotNull(geogit.getRepository(), "No repository exists at %s", geogit
                .getPlatform().pwd());

        this.geogit = geogit;
    }

    @Override
    public void dispose() {
        super.dispose();
        geogit.close();
    }

    /**
     * @deprecated Use {@link setHead(String)} instead
     */
    @Deprecated
    public void setBranch(@Nullable final String branchName) throws IllegalArgumentException {
        setHead(branchName);
    }

    /**
     * Instructs the datastore to operate against the specified refspec, or against the checked out
     * branch, whatever it is, if the argument is {@code null}.
     *
     * Editing capabilities are disabled if the refspec is not a local branch.
     * 
     * @param refspec the name of the branch to work against, or {@code null} to default to the
     *        currently checked out branch
     * @see #getConfiguredBranch()
     * @see #getOrFigureOutBranch()
     * @throws IllegalArgumentException if {@code refspec} is not null and no such commit exists
     *         in the repository
     */
    public void setHead(@Nullable final String refspec) throws IllegalArgumentException {
        if (refspec != null) {
            Optional<ObjectId> rev = getCommandLocator(null).command(RevParse.class).setRefSpec(refspec).call();
            if (!rev.isPresent()) {
                throw new IllegalArgumentException("Bad ref spec: " + refspec);
            }
            Optional<Ref> branchRef = getCommandLocator(null).command(RefParse.class)
                    .setName(refspec).call();
            if (branchRef.isPresent() && branchRef.get().getName().startsWith(Ref.HEADS_PREFIX)) {
                allowTransactions = true;
            } else {
                allowTransactions = false;
            }
        } else {
            allowTransactions = true; // when no branch name is set we assume we should make transactions against the current HEAD
        }
        this.refspec = refspec;
    }

    /**
     * @deprecated Use getOrFigureOutHead instead.
     */
    @Deprecated
    public String getOrFigureOutBranch() {
        return getOrFigureOutHead();
    }

    public String getOrFigureOutHead() {
        String branch = getConfiguredHead();
        if (branch != null) {
            return branch;
        }
        return getCheckedOutBranch();
    }

    public GeoGIT getGeogit() {
        return geogit;
    }

    /**
     * @deprecated Use getConfiguredHead instead.
     */
    @Deprecated
    public String getConfiguredBranch() {
        return getConfiguredHead();
    }

    /**
     * @return the configured refspec of the commit this datastore works against, or {@code null} if no
     *         head in particular has been set, meaning the data store works against whatever the
     *         currently checked out branch is.
     */
    @Nullable
    public String getConfiguredHead() {
        return this.refspec;
    }

    /**
     * @return whether or not we can support transactions against the configured head
     */
    public boolean isAllowTransactions() {
        return this.allowTransactions;
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
        final List<NodeRef> typeRefs = findTypeRefs(tx);
        Collection<NodeRef> matches = Collections2.filter(typeRefs, new Predicate<NodeRef>() {
            @Override
            public boolean apply(NodeRef input) {
                return NodeRef.nodeFromPath(input.path()).equals(localName);
            }
        });
        switch (matches.size()) {
        case 0:
            throw new NoSuchElementException(String.format("No tree ref matched the name: %s",
                    localName));
        case 1:
            return matches.iterator().next();
        default:
            throw new IllegalArgumentException(String.format(
                    "More than one tree ref matches the name %s: %s", localName, matches));
        }
    }

    @Override
    protected ContentState createContentState(ContentEntry entry) {
        return new ContentState(entry);
    }

    @Override
    protected ImmutableList<Name> createTypeNames() throws IOException {
        List<NodeRef> typeTrees = findTypeRefs(Transaction.AUTO_COMMIT);
        Function<NodeRef, Name> function = new Function<NodeRef, Name>() {
            @Override
            public Name apply(NodeRef treeRef) {
                return getDescriptorName(treeRef);
            }
        };
        return ImmutableList.copyOf(Collections2.transform(typeTrees, function));
    }

    private List<NodeRef> findTypeRefs(@Nullable Transaction tx) {

        final String rootRef = getRootRef(tx);
        CommandLocator commandLocator = getCommandLocator(tx);
        List<NodeRef> typeTrees = commandLocator.command(FindFeatureTypeTrees.class)
                .setRootTreeRef(rootRef).call();
        return typeTrees;
    }

    String getRootRef(@Nullable Transaction tx) {
        final String rootRef;
        if (null == tx || Transaction.AUTO_COMMIT.equals(tx)) {
            rootRef = getOrFigureOutBranch();
        } else {
            rootRef = Ref.WORK_HEAD;
        }
        return rootRef;
    }

    @Override
    protected ContentFeatureSource createFeatureSource(ContentEntry entry) throws IOException {
        return new GeogitFeatureStore(entry);
    }

    /**
     * Creates a new feature type tree on the {@link #getOrFigureOutBranch() current branch}.
     * <p>
     * Implementation detail: the operation is the homologous to starting a transaction, checking
     * out the current/configured branch, creating the type tree inside the transaction, issueing a
     * geogit commit, and committing the transaction for the created tree to be merged onto the
     * configured branch.
     */
    @Override
    public void createSchema(SimpleFeatureType featureType) throws IOException {
        if (!allowTransactions) {
            throw new IllegalStateException("Configured head " + refspec + " is not a branch; transactions are not supported.");
        }
        GeogitTransaction tx = getCommandLocator(null).command(TransactionBegin.class).call();
        boolean abort = false;
        try {
            String treePath = featureType.getName().getLocalPart();
            // check out the datastore branch on the transaction space
            final String branch = getOrFigureOutBranch();
            tx.command(CheckoutOp.class).setForce(true).setSource(branch).call();
            // now we can use the transaction working tree with the correct branch checked out
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
=======
/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
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
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.SymRef;
import org.geogit.api.data.FindFeatureTypeTrees;
import org.geogit.api.plumbing.ForEachRef;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.plumbing.TransactionBegin;
import org.geogit.api.porcelain.AddOp;
import org.geogit.api.porcelain.CheckoutOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.repository.WorkingTree;
import org.geotools.data.DataStore;
import org.geotools.data.Transaction;
import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.data.store.ContentState;
import org.geotools.feature.NameImpl;
import org.opengis.feature.simple.SimpleFeature;
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
 * A GeoTools {@link DataStore} that serves and edits {@link SimpleFeature}s in a geogit repository.
 * <p>
 * Multiple instances of this kind of data store may be created against the same repository,
 * possibly working against different {@link #setHead(String) heads}.
 * <p>
 * A head is any commit in GeoGit. If a head has a branch pointing at it then the store allows
 * transactions, otherwise no data modifications may be made.
 * 
 * A branch in Geogit is a separate line of history that may or may not share a common ancestor with
 * another branch. In the later case the branch is called "orphan" and by convention the default
 * branch is called "master", which is created when the geogit repo is first initialized, but does
 * not necessarily exist.
 * <p>
 * Every read operation (like in {@link #getFeatureSource(Name)}) reads committed features out of
 * the configured "head" branch. Write operations are only supported if a {@link Transaction} is
 * set, in which case a {@link GeogitTransaction} is tied to the geotools transaction by means of a
 * {@link GeogitTransactionState}. During the transaction, read operations will read from the
 * transaction's {@link WorkingTree} in order to "see" features that haven't been committed yet.
 * <p>
 * When the transaction is committed, the changes made inside that transaction are merged onto the
 * actual repository. If any other change was made to the repository meanwhile, a rebase will be
 * attempted, and the transaction commit will fail if the rebase operation finds any conflict. This
 * provides for optimistic locking and reduces thread contention.
 * 
 */
public class GeoGitDataStore extends ContentDataStore implements DataStore {

    private final GeoGIT geogit;

    /** @see #setHead(String) */
    private String refspec;

    /** When the configured head is not a branch, we disallow transactions */
    private boolean allowTransactions = true;

    public GeoGitDataStore(GeoGIT geogit) {
        super();
        Preconditions.checkNotNull(geogit);
        Preconditions.checkNotNull(geogit.getRepository(), "No repository exists at %s", geogit
                .getPlatform().pwd());

        this.geogit = geogit;
    }

    @Override
    public void dispose() {
        super.dispose();
        geogit.close();
    }

    /**
     * @deprecated Use {@link setHead(String)} instead
     */
    @Deprecated
    public void setBranch(@Nullable final String branchName) throws IllegalArgumentException {
        setHead(branchName);
    }

    /**
     * Instructs the datastore to operate against the specified refspec, or against the checked out
     * branch, whatever it is, if the argument is {@code null}.
     * 
     * Editing capabilities are disabled if the refspec is not a local branch.
     * 
     * @param refspec the name of the branch to work against, or {@code null} to default to the
     *        currently checked out branch
     * @see #getConfiguredBranch()
     * @see #getOrFigureOutBranch()
     * @throws IllegalArgumentException if {@code refspec} is not null and no such commit exists in
     *         the repository
     */
    public void setHead(@Nullable final String refspec) throws IllegalArgumentException {
        if (refspec != null) {
            Optional<ObjectId> rev = getCommandLocator(null).command(RevParse.class)
                    .setRefSpec(refspec).call();
            if (!rev.isPresent()) {
                throw new IllegalArgumentException("Bad ref spec: " + refspec);
            }
            Optional<Ref> branchRef = getCommandLocator(null).command(RefParse.class)
                    .setName(refspec).call();
            if (branchRef.isPresent() && branchRef.get().getName().startsWith(Ref.HEADS_PREFIX)) {
                allowTransactions = true;
            } else {
                allowTransactions = false;
            }
        } else {
            allowTransactions = true; // when no branch name is set we assume we should make
                                      // transactions against the current HEAD
        }
        this.refspec = refspec;
    }

    /**
     * @deprecated Use getOrFigureOutHead instead.
     */
    @Deprecated
    public String getOrFigureOutBranch() {
        return getOrFigureOutHead();
    }

    public String getOrFigureOutHead() {
        String branch = getConfiguredHead();
        if (branch != null) {
            return branch;
        }
        return getCheckedOutBranch();
    }

    public GeoGIT getGeogit() {
        return geogit;
    }

    /**
     * @deprecated Use getConfiguredHead instead.
     */
    @Deprecated
    public String getConfiguredBranch() {
        return getConfiguredHead();
    }

    /**
     * @return the configured refspec of the commit this datastore works against, or {@code null} if
     *         no head in particular has been set, meaning the data store works against whatever the
     *         currently checked out branch is.
     */
    @Nullable
    public String getConfiguredHead() {
        return this.refspec;
    }

    /**
     * @return whether or not we can support transactions against the configured head
     */
    public boolean isAllowTransactions() {
        return this.allowTransactions;
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
        final List<NodeRef> typeRefs = findTypeRefs(tx);
        Collection<NodeRef> matches = Collections2.filter(typeRefs, new Predicate<NodeRef>() {
            @Override
            public boolean apply(NodeRef input) {
                return NodeRef.nodeFromPath(input.path()).equals(localName);
            }
        });
        switch (matches.size()) {
        case 0:
            throw new NoSuchElementException(String.format("No tree ref matched the name: %s",
                    localName));
        case 1:
            return matches.iterator().next();
        default:
            throw new IllegalArgumentException(String.format(
                    "More than one tree ref matches the name %s: %s", localName, matches));
        }
    }

    @Override
    protected ContentState createContentState(ContentEntry entry) {
        return new ContentState(entry);
    }

    @Override
    protected ImmutableList<Name> createTypeNames() throws IOException {
        List<NodeRef> typeTrees = findTypeRefs(Transaction.AUTO_COMMIT);
        Function<NodeRef, Name> function = new Function<NodeRef, Name>() {
            @Override
            public Name apply(NodeRef treeRef) {
                return getDescriptorName(treeRef);
            }
        };
        return ImmutableList.copyOf(Collections2.transform(typeTrees, function));
    }

    private List<NodeRef> findTypeRefs(@Nullable Transaction tx) {

        final String rootRef = getRootRef(tx);
        CommandLocator commandLocator = getCommandLocator(tx);
        List<NodeRef> typeTrees = commandLocator.command(FindFeatureTypeTrees.class)
                .setRootTreeRef(rootRef).call();
        return typeTrees;
    }

    String getRootRef(@Nullable Transaction tx) {
        final String rootRef;
        if (null == tx || Transaction.AUTO_COMMIT.equals(tx)) {
            rootRef = getOrFigureOutBranch();
        } else {
            rootRef = Ref.WORK_HEAD;
        }
        return rootRef;
    }

    @Override
    protected ContentFeatureSource createFeatureSource(ContentEntry entry) throws IOException {
        return new GeogitFeatureStore(entry);
    }

    /**
     * Creates a new feature type tree on the {@link #getOrFigureOutBranch() current branch}.
     * <p>
     * Implementation detail: the operation is the homologous to starting a transaction, checking
     * out the current/configured branch, creating the type tree inside the transaction, issueing a
     * geogit commit, and committing the transaction for the created tree to be merged onto the
     * configured branch.
     */
    @Override
    public void createSchema(SimpleFeatureType featureType) throws IOException {
        if (!allowTransactions) {
            throw new IllegalStateException("Configured head " + refspec
                    + " is not a branch; transactions are not supported.");
        }
        GeogitTransaction tx = getCommandLocator(null).command(TransactionBegin.class).call();
        boolean abort = false;
        try {
            String treePath = featureType.getName().getLocalPart();
            // check out the datastore branch on the transaction space
            final String branch = getOrFigureOutBranch();
            tx.command(CheckoutOp.class).setForce(true).setSource(branch).call();
            // now we can use the transaction working tree with the correct branch checked out
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

    // Deliberately leaving the @Override annotation commented out so that the class builds
    // both against GeoTools 10.x and 11.x (as the method was added to DataStore in 11.x)
    // @Override
    public void removeSchema(Name name) throws IOException {
        throw new UnsupportedOperationException(
                "removeSchema not yet supported by geogit DataStore");
    }

    // Deliberately leaving the @Override annotation commented out so that the class builds
    // both against GeoTools 10.x and 11.x (as the method was added to DataStore in 11.x)
    // @Override
    public void removeSchema(String name) throws IOException {
        throw new UnsupportedOperationException(
                "removeSchema not yet supported by geogit DataStore");
    }
}
>>>>>>> .merge_file_1jwBnp
