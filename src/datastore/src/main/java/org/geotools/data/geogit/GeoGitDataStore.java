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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.geogit.api.GeoGIT;
import org.geogit.api.MutableTree;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.UpdateRef;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.ObjectWriter;
import org.geotools.data.DefaultServiceInfo;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureWriter;
import org.geotools.data.LockingManager;
import org.geotools.data.Query;
import org.geotools.data.SchemaNotFoundException;
import org.geotools.data.ServiceInfo;
import org.geotools.data.Transaction;
import org.geotools.data.versioning.VersioningDataStore;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

public class GeoGitDataStore implements VersioningDataStore {

    private static final Logger LOGGER = Logging.getLogger(GeoGitDataStore.class);

    public static final String TYPE_NAMES_REF_TREE = "TYPE_NAMES";

    private static final String NULL_NAMESPACE = "";

    private GeoGIT geogit;

    private final String defaultNamespace;

    public GeoGitDataStore(final GeoGIT repo) throws IOException {
        this(repo, null);
    }

    public GeoGitDataStore(final GeoGIT repo, final String defaultNamespace) throws IOException {
        Preconditions.checkNotNull(repo, "repository");
        this.geogit = repo;
        this.defaultNamespace = defaultNamespace;
        init();
    }

    private void init() throws IOException {
        final ObjectDatabase objectDatabase = geogit.getRepository().getObjectDatabase();

        Ref typesTreeRef = geogit.command(RefParse.class).setName(TYPE_NAMES_REF_TREE).call();
        if (null == typesTreeRef) {
            LOGGER.info("Initializing type name references. Types tree does not exist");
            final RevTree typesTree = objectDatabase.newTree();
            ObjectId typesTreeId;
            try {
                ObjectWriter<RevTree> treeWriter = getGeogit().getRepository().newRevTreeWriter(
                        typesTree);
                typesTreeId = objectDatabase.put(treeWriter);
            } catch (Exception e) {
                throw new IOException(e);
            }
            typesTreeRef = geogit.command(UpdateRef.class).setName(TYPE_NAMES_REF_TREE)
                    .setNewValue(typesTreeId).call();
            LOGGER.info("Type names tree reference initialized");
        } else {
            LOGGER.info("Loading type name references");
            List<Name> names = getNamesInternal();
            LOGGER.fine("Managed types: " + names);
        }

    }

    public GeoGIT getGeogit() {
        return geogit;
    }

    /**
     * @see org.geotools.data.DataAccess#getInfo()
     */
    @Override
    public ServiceInfo getInfo() {
        DefaultServiceInfo si = new DefaultServiceInfo();
        si.setDescription("GeoGIT DataStore");
        si.setTitle("GeoGIT");
        return si;
    }

    /**
     * @see org.geotools.data.DataAccess#dispose()
     */
    @Override
    public void dispose() {
        // TODO Auto-generated method stub

    }

    private List<Name> getNamesInternal() throws IOException {

        final Ref typesTreeRef = geogit.command(RefParse.class).setName(TYPE_NAMES_REF_TREE).call();
        Preconditions.checkState(typesTreeRef != null);

        RevTree namespacesTree = geogit.getRepository().getTree(typesTreeRef.getObjectId());
        Preconditions.checkState(null != namespacesTree, "Referenced types tree does not exist: "
                + typesTreeRef);

        List<Name> names = new ArrayList<Name>();
        for (Iterator<NodeRef> namespaces = namespacesTree.iterator(null); namespaces.hasNext();) {
            final NodeRef namespaceRef = namespaces.next();
            Preconditions.checkState(TYPE.TREE.equals(namespaceRef.getType()));
            final String nsUri = namespaceRef.getName();
            final RevTree typesTree = geogit.getRepository().getTree(namespaceRef.getObjectId());
            for (Iterator<NodeRef> simpleNames = typesTree.iterator(null); simpleNames.hasNext();) {
                final NodeRef typeNameRef = simpleNames.next();
                final String simpleTypeName = typeNameRef.getName();
                names.add(new NameImpl(nsUri, simpleTypeName));
            }
        }

        return names;
    }

    /**
     * @see org.geotools.data.DataAccess#getNames()
     */
    @Override
    public List<Name> getNames() throws IOException {
        return getNamesInternal();
    }

    /**
     * @see org.geotools.data.DataStore#getTypeNames()
     * @see #getNames()
     */
    @Override
    public String[] getTypeNames() throws IOException {
        final List<Name> names = getNames();
        String[] simpleNames = new String[names.size()];
        for (int i = 0; i < names.size(); i++) {
            simpleNames[i] = names.get(i).getLocalPart();
        }
        return simpleNames;
    }

    /**
     * @see org.geotools.data.DataAccess#createSchema(org.opengis.feature.type.FeatureType)
     */
    @Override
    public void createSchema(final SimpleFeatureType featureType) throws IOException {
        Preconditions.checkNotNull(featureType);

        SimpleFeatureType createType = featureType;

        LOGGER.info("Creating FeatureType " + createType.getName());

        if (getNames().contains(createType.getName())) {
            throw new IOException(createType.getName() + " already exists");
        }

        {
            // GeoServer calls createSchema with this namespace but then asks
            // for the one passed in
            // as the DataStore's namespace parameter
            final String ignoreNamespace = "http://www.opengis.net/gml";
            Name name = createType.getName();
            if ((ignoreNamespace.equals(name.getNamespaceURI()) || null == name.getNamespaceURI())
                    && null != defaultNamespace) {
                LOGGER.info("FeatureType to be created has no namespace, assigning DataStore's default: '"
                        + defaultNamespace + "'");

                SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
                builder.setName(createType.getName().getLocalPart());
                builder.setNamespaceURI(defaultNamespace);
                builder.addAll(createType.getAttributeDescriptors());
                createType = builder.buildFeatureType();
            }

        }
        final Name typeName = createType.getName();

        final Ref typesTreeRef = geogit.command(RefParse.class).setName(TYPE_NAMES_REF_TREE).call();
        Preconditions.checkState(typesTreeRef != null);

        final RevTree namespacesRootTree = geogit.getRepository().getTree(
                typesTreeRef.getObjectId());
        Preconditions.checkState(namespacesRootTree != null);

        final String namespace = null == typeName.getNamespaceURI() ? NULL_NAMESPACE : typeName
                .getNamespaceURI();
        final String localName = typeName.getLocalPart();

        try {
            final ObjectId featureTypeBlobId;
            ObjectDatabase objectDatabase = geogit.getRepository().getObjectDatabase();
            featureTypeBlobId = objectDatabase.put(geogit.getRepository()
                    .newSimpleFeatureTypeWriter(createType));

            final List<String> namespaceTreePath = Collections.singletonList(namespace);
            MutableTree namespaceTree = objectDatabase.getOrCreateSubTree(namespacesRootTree,
                    namespaceTreePath);

            namespaceTree.put(new NodeRef(localName, featureTypeBlobId, TYPE.BLOB));

            final MutableTree root = namespacesRootTree.mutable();
            final ObjectId newTypeRefsTreeId;
            newTypeRefsTreeId = objectDatabase.writeBack(root, namespaceTree, namespaceTreePath);

            geogit.command(UpdateRef.class).setName(TYPE_NAMES_REF_TREE)
                    .setNewValue(newTypeRefsTreeId).call();
        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, IOException.class);
            Throwables.propagate(e);
        }
    }

    /**
     * @see org.geotools.data.DataAccess#getSchema(org.opengis.feature.type.Name)
     */
    @Override
    public SimpleFeatureType getSchema(final Name name) throws IOException {
        Preconditions.checkNotNull(name);

        final ObjectDatabase objectDatabase = geogit.getRepository().getObjectDatabase();

        final Ref typesTreeRef = geogit.command(RefParse.class).setName(TYPE_NAMES_REF_TREE).call();
        Preconditions.checkState(typesTreeRef != null);

        final RevTree namespacesRootTree = geogit.getRepository().getTree(
                typesTreeRef.getObjectId());
        Preconditions.checkState(namespacesRootTree != null);

        final String[] path = {
                name.getNamespaceURI() == null ? NULL_NAMESPACE : name.getNamespaceURI(),
                name.getLocalPart() };

        final NodeRef typeRef = objectDatabase.getTreeChild(namespacesRootTree, path);
        if (typeRef == null) {
            throw new SchemaNotFoundException(name.toString());
        }
        Preconditions.checkState(TYPE.BLOB.equals(typeRef.getType()));
        final ObjectId objectId = typeRef.getObjectId();
        final ObjectReader<SimpleFeatureType> reader = getGeogit().getRepository()
                .newSimpleFeatureTypeReader();
        final SimpleFeatureType featureType = objectDatabase.get(objectId, reader);
        return featureType;
    }

    /**
     * @see org.geotools.data.DataStore#getSchema(java.lang.String)
     * @see #getSchema(Name)
     */
    @Override
    public SimpleFeatureType getSchema(final String typeName) throws IOException {
        final List<Name> names = getNames();
        for (Name name : names) {
            if (name.getLocalPart().equals(typeName)) {
                return getSchema(name);
            }
        }
        throw new SchemaNotFoundException(typeName);
    }

    /**
     * @see org.geotools.data.DataStore#getFeatureSource(java.lang.String)
     * @see #getFeatureSource(Name)
     */
    @Override
    public GeoGitFeatureSource getFeatureSource(final String typeName) throws IOException {
        final List<Name> names = getNames();
        for (Name name : names) {
            if (name.getLocalPart().equals(typeName)) {
                return getFeatureSource(name);
            }
        }
        throw new SchemaNotFoundException(typeName);
    }

    /**
     * @see org.geotools.data.DataStore#getFeatureSource(org.opengis.feature.type.Name)
     */
    @Override
    public GeoGitFeatureSource getFeatureSource(final Name typeName) throws IOException {
        final SimpleFeatureType featureType = getSchema(typeName);

        return new GeoGitFeatureStore(featureType, this);
    }

    @Override
    public FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(Query query,
            Transaction transaction) throws IOException {

        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(String typeName,
            Filter filter, Transaction transaction) throws IOException {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(String typeName,
            Transaction transaction) throws IOException {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriterAppend(String typeName,
            Transaction transaction) throws IOException {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public LockingManager getLockingManager() {
        return null;
    }

    /**
     * @see org.geotools.data.DataAccess#updateSchema(org.opengis.feature.type.Name,
     *      org.opengis.feature.type.FeatureType)
     */
    @Override
    public void updateSchema(Name typeName, SimpleFeatureType featureType) throws IOException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * @see org.geotools.data.DataStore#updateSchema(java.lang.String,
     *      org.opengis.feature.simple.SimpleFeatureType)
     */
    @Override
    public void updateSchema(String typeName, SimpleFeatureType featureType) throws IOException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
