package org.geogit.geotools.porcelain;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.net.ConnectException;
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
import com.google.common.base.Preconditions;

/**
 * Exports features from a feature type into a SQL Server database.
 * 
 * @see ExportOp
 */
@Parameters(commandNames = "export", commandDescription = "Export to SQL Server")
public class SQLServerExport extends AbstractSQLServerCommand implements CLICommand {

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
    protected void runInternal(GeogitCLI cli) throws Exception {
        checkState(cli.getGeogit() != null, "Not a geogit repository: " + cli.getPlatform().pwd());

        if (args.isEmpty()) {
            printUsage();
            throw new CommandFailedException();
        }

        String path = args.get(0);
        String tableName = args.get(1);

        checkArgument(tableName != null && !tableName.isEmpty(), "No table name specified");

        DataStore dataStore = null;
        try {
            dataStore = getDataStore();
        } catch (ConnectException e) {
            cli.getConsole().println("Unable to connect using the specified database parameters.");
            throw new CommandFailedException();
        }

        ObjectId featureTypeId = null;
        if (!Arrays.asList(dataStore.getTypeNames()).contains(tableName)) {
            SimpleFeatureType outputFeatureType;
            if (sFeatureTypeId != null) {
                // Check the feature type id string is a correct id
                Optional<ObjectId> id = cli.getGeogit().command(RevParse.class)
                        .setRefSpec(sFeatureTypeId).call();
                Preconditions.checkArgument(id.isPresent(), "Invalid feature type reference",
                        sFeatureTypeId);
                TYPE type = cli.getGeogit().command(ResolveObjectType.class).setObjectId(id.get())
                        .call();
                Preconditions.checkArgument(type.equals(TYPE.FEATURETYPE),
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
                    cli.getConsole().println("No features to export.");
                    throw new CommandFailedException();
                }
            }
            try {
                dataStore.createSchema(outputFeatureType);
            } catch (IOException e) {
                cli.getConsole().println("Cannot create new table in database");
                throw new CommandFailedException();
            }
        } else {
            if (!overwrite) {
                cli.getConsole().println("The selected table already exists. Use -o to overwrite");
                throw new CommandFailedException();
            }
        }

        SimpleFeatureSource featureSource = dataStore.getFeatureSource(tableName);
        if (featureSource instanceof SimpleFeatureStore) {
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
            if (overwrite) {
                featureStore.removeFeatures(Filter.INCLUDE);
            }
            ExportOp op = cli.getGeogit().command(ExportOp.class).setFeatureStore(featureStore)
                    .setPath(path).setFeatureTypeId(featureTypeId).setAlter(alter);
            if (defaultType) {
                op.exportDefaultFeatureType();
            }
            try {
                op.setProgressListener(cli.getProgressListener()).call();
            } catch (GeoToolsOpException e) {
                switch (e.statusCode) {
                case MIXED_FEATURE_TYPES:
                    cli.getConsole()
                            .println(
                                    "The selected tree contains mixed feature types. Use --defaulttype or --featuretype <feature_type_ref> to export.");
                    throw new CommandFailedException();
                default:
                    cli.getConsole().println("Could not export. Error:" + e.statusCode.name());
                    throw new CommandFailedException();
                }
            }

            cli.getConsole().println(path + " exported successfully to " + tableName);
        } else {
            cli.getConsole().println("Can't write to the selected table");
            throw new CommandFailedException();
        }

    }

    private SimpleFeatureType getFeatureType(String path, GeogitCLI cli) {

        checkArgument(path != null, "No path specified.");

        String refspec;
        if (path.contains(":")) {
            refspec = path;
        } else {
            refspec = "WORK_HEAD:" + path;
        }

        checkArgument(!refspec.endsWith(":"), "No path specified.");

        final GeoGIT geogit = cli.getGeogit();

        Optional<ObjectId> rootTreeId = geogit.command(ResolveTreeish.class)
                .setTreeish(refspec.split(":")[0]).call();

        checkState(rootTreeId.isPresent(), "Couldn't resolve '" + refspec + "' to a treeish object");

        RevTree rootTree = geogit.getRepository().getTree(rootTreeId.get());
        Optional<NodeRef> featureTypeTree = geogit.command(FindTreeChild.class)
                .setChildPath(refspec.split(":")[1]).setParent(rootTree).setIndex(true).call();

        checkArgument(featureTypeTree.isPresent(), "pathspec '" + refspec.split(":")[1]
                + "' did not match any valid path");

        Optional<RevObject> revObject = cli.getGeogit().command(RevObjectParse.class)
                .setObjectId(featureTypeTree.get().getMetadataId()).call();
        if (revObject.isPresent() && revObject.get() instanceof RevFeatureType) {
            RevFeatureType revFeatureType = (RevFeatureType) revObject.get();
            if (revFeatureType.type() instanceof SimpleFeatureType) {
                return (SimpleFeatureType) revFeatureType.type();
            } else {
                throw new IllegalArgumentException(
                        "Cannot find feature type for the specified path");
            }
        } else {
            throw new IllegalArgumentException("Cannot find feature type for the specified path");
        }

    }

}