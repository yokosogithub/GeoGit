<<<<<<< .merge_file_13bhZk
/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.geotools.cli.porcelain;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geogit.api.GeoGIT;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.ResolveTreeish;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.cli.CLICommand;
import org.geogit.cli.CommandFailedException;
import org.geogit.cli.GeogitCLI;
import org.geogit.cli.InvalidParameterException;
import org.geogit.geotools.plumbing.ExportDiffOp;
import org.geogit.geotools.plumbing.ExportOp;
import org.geogit.geotools.plumbing.GeoToolsOpException;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;

/**
 * Exports features from a feature type into a shapefile.
 * 
 * @see ExportOp
 */
@Parameters(commandNames = "export-diff", commandDescription = "Export changed features to Shapefile")
public class ShpExportDiff extends AbstractShpCommand implements CLICommand {

    @Parameter(description = "<commit1> <commit2> <path> <shapefile>", arity = 4)
    public List<String> args;

    @Parameter(names = { "--overwrite", "-o" }, description = "Overwrite output files")
    public boolean overwrite;

    @Parameter(names = { "--old" }, description = "Export features from the old version instead of the most recent one")
    public boolean old;

    /**
     * Executes the export command using the provided options.
     */
    @Override
    protected void runInternal(GeogitCLI cli) throws IOException {
        if (args.size() != 4) {
            printUsage(cli);
            throw new CommandFailedException();
        }

        String commitOld = args.get(0);
        String commitNew = args.get(1);
        String path = args.get(2);
        String shapefile = args.get(3);

        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

        File file = new File(shapefile);
        if (file.exists() && !overwrite) {
            throw new CommandFailedException(
                    "The selected shapefile already exists. Use -o to overwrite");
        }

        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put(ShapefileDataStoreFactory.URLP.key, file.toURI().toURL());
        params.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, Boolean.FALSE);
        params.put(ShapefileDataStoreFactory.ENABLE_SPATIAL_INDEX.key, Boolean.FALSE);

        ShapefileDataStore dataStore = (ShapefileDataStore) dataStoreFactory
                .createNewDataStore(params);

        SimpleFeatureType outputFeatureType;
        try {
            outputFeatureType = getFeatureType(path, cli);
        } catch (GeoToolsOpException e) {
            cli.getConsole().println("No features to export.");
            return;
        }

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.add("geogit_fid", String.class);
        for (AttributeDescriptor descriptor : outputFeatureType.getAttributeDescriptors()) {
            builder.add(descriptor);
        }
        builder.setName(outputFeatureType.getName());
        builder.setCRS(outputFeatureType.getCoordinateReferenceSystem());
        outputFeatureType = builder.buildFeatureType();

        dataStore.createSchema(outputFeatureType);

        final String typeName = dataStore.getTypeNames()[0];
        final SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);
        if (!(featureSource instanceof SimpleFeatureStore)) {
            throw new CommandFailedException("Could not create feature store.");
        }
        final SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

        ExportDiffOp op = cli.getGeogit().command(ExportDiffOp.class).setFeatureStore(featureStore)
                .setPath(path).setOldRef(commitOld).setNewRef(commitNew).setUseOld(old)
                .setTransactional(false);
        try {
            op.setProgressListener(cli.getProgressListener()).call();
        } catch (IllegalArgumentException iae) {
            throw new org.geogit.cli.InvalidParameterException(iae.getMessage(), iae);
        } catch (GeoToolsOpException e) {
            file.delete();
            switch (e.statusCode) {
            case MIXED_FEATURE_TYPES:
                throw new CommandFailedException(
                        "Error: The selected tree contains mixed feature types.", e);
            default:
                throw new CommandFailedException("Could not export. Error:" + e.statusCode.name(),
                        e);
            }
        }
        cli.getConsole().println(path + " exported successfully to " + shapefile);

    }

    private SimpleFeatureType getFeatureType(String path, GeogitCLI cli) {

        checkParameter(path != null, "No path specified.");

        String refspec;
        if (path.contains(":")) {
            refspec = path;
        } else {
            refspec = "WORK_HEAD:" + path;
        }

        checkParameter(!refspec.endsWith(":"), "No path specified.");

        final GeoGIT geogit = cli.getGeogit();

        Optional<ObjectId> rootTreeId = geogit.command(ResolveTreeish.class)
                .setTreeish(refspec.split(":")[0]).call();

        checkParameter(rootTreeId.isPresent(), "Couldn't resolve '" + refspec
                + "' to a treeish object");

        RevTree rootTree = geogit.getRepository().getTree(rootTreeId.get());
        Optional<NodeRef> featureTypeTree = geogit.command(FindTreeChild.class)
                .setChildPath(refspec.split(":")[1]).setParent(rootTree).setIndex(true).call();

        checkParameter(featureTypeTree.isPresent(), "pathspec '" + refspec.split(":")[1]
                + "' did not match any valid path");

        Optional<RevObject> revObject = cli.getGeogit().command(RevObjectParse.class)
                .setObjectId(featureTypeTree.get().getMetadataId()).call();
        if (revObject.isPresent() && revObject.get() instanceof RevFeatureType) {
            RevFeatureType revFeatureType = (RevFeatureType) revObject.get();
            if (revFeatureType.type() instanceof SimpleFeatureType) {
                return (SimpleFeatureType) revFeatureType.type();
            } else {
                throw new InvalidParameterException(
                        "Cannot find feature type for the specified path");
            }
        } else {
            throw new InvalidParameterException("Cannot find feature type for the specified path");
        }

    }
}
=======
/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.geotools.cli.porcelain;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.geogit.api.GeoGIT;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.ResolveTreeish;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.cli.CLICommand;
import org.geogit.cli.CommandFailedException;
import org.geogit.cli.GeogitCLI;
import org.geogit.cli.InvalidParameterException;
import org.geogit.geotools.plumbing.ExportDiffOp;
import org.geogit.geotools.plumbing.ExportOp;
import org.geogit.geotools.plumbing.GeoToolsOpException;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.Feature;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Function;
import com.google.common.base.Optional;

/**
 * Exports features from a feature type into a shapefile.
 * 
 * @see ExportOp
 */
@Parameters(commandNames = "export-diff", commandDescription = "Export changed features to Shapefile")
public class ShpExportDiff extends AbstractShpCommand implements CLICommand {

    @Parameter(description = "<commit1> <commit2> <path> <shapefile>", arity = 4)
    public List<String> args;

    @Parameter(names = { "--overwrite", "-o" }, description = "Overwrite output files")
    public boolean overwrite;

    @Parameter(names = { "--old" }, description = "Export features from the old version instead of the most recent one")
    public boolean old;

    /**
     * Executes the export command using the provided options.
     */
    @Override
    protected void runInternal(GeogitCLI cli) throws IOException {
        if (args.size() != 4) {
            printUsage(cli);
            throw new CommandFailedException();
        }

        String commitOld = args.get(0);
        String commitNew = args.get(1);
        String path = args.get(2);
        String shapefile = args.get(3);

        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

        File file = new File(shapefile);
        if (file.exists() && !overwrite) {
            throw new CommandFailedException(
                    "The selected shapefile already exists. Use -o to overwrite");
        }

        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put(ShapefileDataStoreFactory.URLP.key, file.toURI().toURL());
        params.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, Boolean.FALSE);
        params.put(ShapefileDataStoreFactory.ENABLE_SPATIAL_INDEX.key, Boolean.FALSE);

        ShapefileDataStore dataStore = (ShapefileDataStore) dataStoreFactory
                .createNewDataStore(params);

        SimpleFeatureType outputFeatureType;
        try {
            outputFeatureType = getFeatureType(path, cli);
        } catch (GeoToolsOpException e) {
            cli.getConsole().println("No features to export.");
            return;
        }

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.add("geogit_fid", String.class);
        for (AttributeDescriptor descriptor : outputFeatureType.getAttributeDescriptors()) {
            builder.add(descriptor);
        }
        builder.setName(outputFeatureType.getName());
        builder.setCRS(outputFeatureType.getCoordinateReferenceSystem());
        outputFeatureType = builder.buildFeatureType();

        dataStore.createSchema(outputFeatureType);

        final String typeName = dataStore.getTypeNames()[0];
        final SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);
        if (!(featureSource instanceof SimpleFeatureStore)) {
            throw new CommandFailedException("Could not create feature store.");
        }
        final SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

        Function<Feature, Optional<Feature>> function = getTransformingFunction(dataStore
                .getSchema());

        ExportDiffOp op = cli.getGeogit().command(ExportDiffOp.class).setFeatureStore(featureStore)
                .setPath(path).setOldRef(commitOld).setNewRef(commitNew).setUseOld(old)
                .setTransactional(false).setFeatureTypeConversionFunction(function);
        try {
            op.setProgressListener(cli.getProgressListener()).call();
        } catch (IllegalArgumentException iae) {
            throw new org.geogit.cli.InvalidParameterException(iae.getMessage(), iae);
        } catch (GeoToolsOpException e) {
            file.delete();
            switch (e.statusCode) {
            case MIXED_FEATURE_TYPES:
                throw new CommandFailedException(
                        "Error: The selected tree contains mixed feature types.", e);
            default:
                throw new CommandFailedException("Could not export. Error:" + e.statusCode.name(),
                        e);
            }
        }
        cli.getConsole().println(path + " exported successfully to " + shapefile);

    }

    private Function<Feature, Optional<Feature>> getTransformingFunction(
            final SimpleFeatureType featureType) {
        Function<Feature, Optional<Feature>> function = new Function<Feature, Optional<Feature>>() {

            @Override
            @Nullable
            public Optional<Feature> apply(@Nullable Feature feature) {
                SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
                for (Property property : feature.getProperties()) {
                    if (property instanceof GeometryAttribute) {
                        builder.set(featureType.getGeometryDescriptor().getName(),
                                property.getValue());
                    } else {
                        builder.set(property.getName(), property.getValue());
                    }
                }
                Feature modifiedFeature = builder.buildFeature(feature.getIdentifier().getID());
                return Optional.fromNullable(modifiedFeature);
            }

        };

        return function;
    }

    private SimpleFeatureType getFeatureType(String path, GeogitCLI cli) {

        checkParameter(path != null, "No path specified.");

        String refspec;
        if (path.contains(":")) {
            refspec = path;
        } else {
            refspec = "WORK_HEAD:" + path;
        }

        checkParameter(!refspec.endsWith(":"), "No path specified.");

        final GeoGIT geogit = cli.getGeogit();

        Optional<ObjectId> rootTreeId = geogit.command(ResolveTreeish.class)
                .setTreeish(refspec.split(":")[0]).call();

        checkParameter(rootTreeId.isPresent(), "Couldn't resolve '" + refspec
                + "' to a treeish object");

        RevTree rootTree = geogit.getRepository().getTree(rootTreeId.get());
        Optional<NodeRef> featureTypeTree = geogit.command(FindTreeChild.class)
                .setChildPath(refspec.split(":")[1]).setParent(rootTree).setIndex(true).call();

        checkParameter(featureTypeTree.isPresent(), "pathspec '" + refspec.split(":")[1]
                + "' did not match any valid path");

        Optional<RevObject> revObject = cli.getGeogit().command(RevObjectParse.class)
                .setObjectId(featureTypeTree.get().getMetadataId()).call();
        if (revObject.isPresent() && revObject.get() instanceof RevFeatureType) {
            RevFeatureType revFeatureType = (RevFeatureType) revObject.get();
            if (revFeatureType.type() instanceof SimpleFeatureType) {
                return (SimpleFeatureType) revFeatureType.type();
            } else {
                throw new InvalidParameterException(
                        "Cannot find feature type for the specified path");
            }
        } else {
            throw new InvalidParameterException("Cannot find feature type for the specified path");
        }

    }
}
>>>>>>> .merge_file_hc13gk
