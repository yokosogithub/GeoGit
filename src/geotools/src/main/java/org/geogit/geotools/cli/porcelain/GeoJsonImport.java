/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.geotools.cli.porcelain;

import java.io.IOException;
import java.util.List;

import org.geogit.api.RevFeatureType;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.cli.CLICommand;
import org.geogit.cli.CommandFailedException;
import org.geogit.cli.GeogitCLI;
import org.geogit.cli.InvalidParameterException;
import org.geogit.geotools.plumbing.GeoToolsOpException;
import org.geogit.geotools.plumbing.ImportOp;
import org.geotools.data.DataStore;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.geogit.api.ProgressListener;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;

@Parameters(commandNames = "import", commandDescription = "Import GeoJSON")
public class GeoJsonImport extends AbstractGeoJsonCommand implements CLICommand {

    /**
     * GeoJSON files to import.
     */
    @Parameter(description = "<geojson> [<geojson>]...")
    List<String> geoJSONList;

    /**
     * do not replace or delete features
     */
    @Parameter(names = { "--add" }, description = "Do not replace or delete features on the destination path, but just add new ones")
    boolean add;

    /**
     * Set the path default feature type to the the feature type of imported features, and modify
     * existing features to match it
     */
    @Parameter(names = { "--alter" }, description = "Set the path default feature type to the the feature type of imported features, and modify existing features to match it")
    boolean alter;

    /**
     * Destination path to add features to. Only allowed when importing a single table
     */
    @Parameter(names = { "-d", "--dest" }, description = "Path to import to")
    String destTable;

    /**
     * Name to use for geometry attribute, replacing the default one ("geometry")
     */
    @Parameter(names = { "--geom-name" }, description = "Name to use for geometry attribute, replacing the default one ('geometry')")
    String geomName;

    /**
     * Name to use for geometry attribute, replacing the default one ("geometry")
     */
    @Parameter(names = { "--geom-name-auto" }, description = "Uses the name of the geometry descriptor in the destination feature type")
    boolean geomNameAuto;

    /**
     * The attribute to use to create the feature Id
     */
    @Parameter(names = { "--fid-attrib" }, description = "Use the specified attribute to create the feature Id")
    String fidAttribute;

    @Override
    protected void runInternal(GeogitCLI cli) throws InvalidParameterException,
            CommandFailedException, IOException {
        checkParameter(geoJSONList != null && !geoJSONList.isEmpty(), "No GeoJSON specified");
        checkParameter(geomName == null || !geomNameAuto,
                "Cannot use --geom-name and --geom-name-auto at the same time");

        for (String geoJSON : geoJSONList) {
            DataStore dataStore = null;
            try {
                dataStore = getDataStore(geoJSON);
            } catch (InvalidParameterException e) {
                cli.getConsole().println(
                        "The GeoJSON file '" + geoJSON + "' could not be found, skipping...");
                continue;
            }
            if (fidAttribute != null) {
                AttributeDescriptor attrib = dataStore.getSchema(dataStore.getNames().get(0))
                        .getDescriptor(fidAttribute);
                if (attrib == null) {
                    throw new InvalidParameterException(
                            "The specified attribute does not exist in the selected GeoJSON file");
                }
            }
            if (geomNameAuto) {
                String destPath = destTable;
                if (destPath == null) {
                    destPath = dataStore.getSchema(dataStore.getNames().get(0)).getTypeName();
                }
                Optional<RevFeatureType> ft = cli.getGeogit().command(RevObjectParse.class)
                        .setRefSpec("WORK_HEAD:" + destPath).call(RevFeatureType.class);
                // If there is previous data in the destination tree, we try to get the name of the
                // geom attribute.
                // If the destination tree does not exist, we use the default name for the geometry
                // attribute
                if (ft.isPresent()) {
                    GeometryDescriptor geomDescriptor = ft.get().type().getGeometryDescriptor();
                    if (geomDescriptor != null) {
                        geomName = geomDescriptor.getLocalName();
                    }
                }
            }
            try {
                cli.getConsole().println("Importing from GeoJSON " + geoJSON);

                ProgressListener progressListener = cli.getProgressListener();
                cli.getGeogit().command(ImportOp.class).setAll(true).setTable(null).setAlter(alter)
                        .setOverwrite(!add).setDestinationPath(destTable).setDataStore(dataStore)
                        .setFidAttribute(fidAttribute).setGeometryNameOverride(geomName)
                        .setProgressListener(progressListener).call();

                cli.getConsole().println(geoJSON + " imported successfully.");

            } catch (GeoToolsOpException e) {
                switch (e.statusCode) {
                case NO_FEATURES_FOUND:
                    throw new CommandFailedException("No features were found in the GeoJSON file.",
                            e);
                case UNABLE_TO_GET_NAMES:
                    throw new CommandFailedException(
                            "Unable to get feature types from the GeoJSON file.", e);
                case UNABLE_TO_GET_FEATURES:
                    throw new CommandFailedException(
                            "Unable to get features from the GeoJSON file.", e);
                case UNABLE_TO_INSERT:
                    throw new CommandFailedException(
                            "Unable to insert features into the working tree.", e);
                default:
                    throw new CommandFailedException("Import failed with exception: "
                            + e.statusCode.name(), e);
                }
            }
        }

    }
}
