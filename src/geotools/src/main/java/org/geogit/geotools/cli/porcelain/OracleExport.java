/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.geotools.cli.porcelain;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import org.geogit.api.GeoGIT;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.ResolveObjectType;
import org.geogit.api.plumbing.ResolveTreeish;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.RevParse;
import org.geogit.cli.CLICommand;
import org.geogit.cli.CommandFailedException;
import org.geogit.cli.GeogitCLI;
import org.geogit.cli.InvalidParameterException;
import org.geogit.cli.annotation.ReadOnly;
import org.geogit.geotools.plumbing.ExportOp;
import org.geogit.geotools.plumbing.GeoToolsOpException;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeImpl;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;

/**
 * Exports features from a feature type into a Oracle database.
 * 
 * @see ExportOp
 */
@ReadOnly
@Parameters(commandNames = "export", commandDescription = "Export to Oracle")
public class OracleExport extends AbstractOracleCommand implements CLICommand {

    @Parameter(description = "<path> <table>", arity = 2)
    public List<String> args;

    @Parameter(names = { "--overwrite", "-o" }, description = "Overwrite output table")
    public boolean overwrite;

    @Parameter(names = { "--defaulttype" }, description = "Export only features with the tree default feature type if several types are found")
    public boolean defaultType;

    @Parameter(names = { "--alter" }, description = "Export all features if several types are found, altering them to adapt to the output feature type")
    public boolean alter;

    @Parameter(names = { "--featuretype" }, description = "Export only features with the specified feature type if several types are found")
    @Nullable
    public String sFeatureTypeId;

    /**
     * Executes the export command using the provided options.
     * 
     * @param cli
     */
    @Override
    protected void runInternal(GeogitCLI cli) throws IOException {
        if (args.isEmpty()) {
            printUsage();
            throw new CommandFailedException();
        }

        String path = args.get(0);
        String tableName = args.get(1);

        checkParameter(tableName != null && !tableName.isEmpty(), "No table name specified");

        DataStore dataStore = getDataStore();

        ObjectId featureTypeId = null;
        if (!Arrays.asList(dataStore.getTypeNames()).contains(tableName)) {
            SimpleFeatureType outputFeatureType;
            if (sFeatureTypeId != null) {
                // Check the feature type id string is a correct id
                Optional<ObjectId> id = cli.getGeogit().command(RevParse.class)
                        .setRefSpec(sFeatureTypeId).call();
                checkParameter(id.isPresent(), "Invalid feature type reference", sFeatureTypeId);
                TYPE type = cli.getGeogit().command(ResolveObjectType.class).setObjectId(id.get())
                        .call();
                checkParameter(type.equals(TYPE.FEATURETYPE),
                        "Provided reference does not resolve to a feature type: ", sFeatureTypeId);
                outputFeatureType = (SimpleFeatureType) cli.getGeogit()
                        .command(RevObjectParse.class).setObjectId(id.get())
                        .call(RevFeatureType.class).get().type();
                featureTypeId = id.get();
            } else {
                try {
                    SimpleFeatureType sft = getFeatureType(path, cli);
                    outputFeatureType = new SimpleFeatureTypeImpl(new NameImpl(tableName),
                            sft.getAttributeDescriptors(), sft.getGeometryDescriptor(),
                            sft.isAbstract(), sft.getRestrictions(), sft.getSuper(),
                            sft.getDescription());
                } catch (GeoToolsOpException e) {
                    throw new CommandFailedException("No features to export.", e);
                }
            }
            try {
                dataStore.createSchema(outputFeatureType);
            } catch (IOException e) {
                throw new CommandFailedException("Cannot create new table in database", e);
            }
        } else {
            if (!overwrite) {
                throw new CommandFailedException(
                        "The selected table already exists. Use -o to overwrite");
            }
        }

        SimpleFeatureSource featureSource = dataStore.getFeatureSource(tableName);
        if (!(featureSource instanceof SimpleFeatureStore)) {
            throw new CommandFailedException("Can't write to the selected table");
        }
        SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
        if (overwrite) {
            try {
                featureStore.removeFeatures(Filter.INCLUDE);
            } catch (IOException e) {
                throw new CommandFailedException("Error accessing table: " + e.getMessage(), e);
            }
        }
        ExportOp op = cli.getGeogit().command(ExportOp.class).setFeatureStore(featureStore)
                .setPath(path).setFilterFeatureTypeId(featureTypeId).setAlter(alter);
        if (defaultType) {
            op.exportDefaultFeatureType();
        }
        try {
            op.setProgressListener(cli.getProgressListener()).call();
        } catch (IllegalArgumentException iae) {
            throw new org.geogit.cli.InvalidParameterException(iae.getMessage(), iae);
        } catch (GeoToolsOpException e) {
            switch (e.statusCode) {
            case MIXED_FEATURE_TYPES:

                throw new CommandFailedException(
                        "The selected tree contains mixed feature types. Use --defaulttype or --featuretype <feature_type_ref> to export.",
                        e);
            default:
                throw new CommandFailedException("Could not export. Error:" + e.statusCode.name(),
                        e);
            }
        }

        cli.getConsole().println(path + " exported successfully to " + tableName);

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
