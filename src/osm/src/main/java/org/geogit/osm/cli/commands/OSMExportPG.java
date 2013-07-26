/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.cli.commands;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import org.geogit.cli.CLICommand;
import org.geogit.cli.CommandFailedException;
import org.geogit.cli.GeogitCLI;
import org.geogit.geotools.plumbing.ExportOp;
import org.geogit.geotools.plumbing.GeoToolsOpException;
import org.geogit.geotools.porcelain.AbstractPGCommand;
import org.geogit.osm.internal.MappedFeature;
import org.geogit.osm.internal.Mapping;
import org.geogit.osm.internal.MappingRule;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.vividsolutions.jts.awt.PointShapeFactory.Point;

/**
 * Exports OSM into a PostGIS database, using a data mapping
 * 
 * @see ExportOp
 */
@Parameters(commandNames = "export-pg", commandDescription = "Export OSM data to a PostGIS database, using a data mapping")
public class OSMExportPG extends AbstractPGCommand implements CLICommand {

    @Parameter(names = { "--overwrite", "-o" }, description = "Overwrite output tables")
    public boolean overwrite;

    @Parameter(names = { "--mapping" }, description = "The file that contains the data mapping to use")
    public String mappingFile;

    /**
     * Executes the export command using the provided options.
     * 
     * @param cli
     */
    @Override
    protected void runInternal(GeogitCLI cli) throws Exception {
        if (cli.getGeogit() == null) {
            cli.getConsole().println("Not a geogit repository: " + cli.getPlatform().pwd());
            return;
        }

        Preconditions.checkNotNull(mappingFile != null, "A data mapping file must be specified");

        final Mapping mapping = Mapping.fromFile(mappingFile);
        List<MappingRule> rules = mapping.getRules();
        Preconditions.checkArgument(!rules.isEmpty(),
                "No rules are defined in the specified mapping");
        Function<Feature, Optional<Feature>> function = new Function<Feature, Optional<Feature>>() {

            @Override
            @Nullable
            public Optional<Feature> apply(@Nullable Feature feature) {
                Optional<MappedFeature> mapped = mapping.map(feature);
                if (mapped.isPresent()) {
                    return Optional.of(mapped.get().getFeature());
                }
                return Optional.absent();
            }

        };

        SimpleFeatureType outputFeatureType = mapping.getRules().get(0).getFeatureType();
        String path = getOriginTreesFromOutputFeatureType(outputFeatureType);
        DataStore dataStore;
        try {
            dataStore = getDataStore();
        } catch (ConnectException e) {
            throw new IllegalStateException("Cannot connect to database: " + e.getMessage(), e);
        }

        String tableName = outputFeatureType.getName().getLocalPart();
        if (Arrays.asList(dataStore.getTypeNames()).contains(tableName)) {
            if (!overwrite) {
                cli.getConsole().println("The selected table already exists. Use -o to overwrite");
                throw new CommandFailedException();
            }
        } else {
            try {
                dataStore.createSchema(outputFeatureType);
            } catch (IOException e) {
                cli.getConsole().println("Cannot create new table in database");
                throw new CommandFailedException();
            }
        }

        final SimpleFeatureSource featureSource = dataStore.getFeatureSource(tableName);

        if (featureSource instanceof SimpleFeatureStore) {
            final SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
            if (overwrite) {
                featureStore.removeFeatures(Filter.INCLUDE);
            }
            ExportOp op = cli.getGeogit().command(ExportOp.class).setFeatureStore(featureStore)
                    .setPath(path).setFeatureTypeConversionFunction(function);
            try {
                op.setProgressListener(cli.getProgressListener()).call();
                cli.getConsole().println("OSM data exported successfully to " + tableName);
            } catch (GeoToolsOpException e) {
                cli.getConsole().println("Could not export. Error:" + e.statusCode.name());
                throw new CommandFailedException();
            }

        } else {
            cli.getConsole().println("Could not create feature store.");
            throw new CommandFailedException();
        }

    }

    private String getOriginTreesFromOutputFeatureType(SimpleFeatureType featureType) {
        GeometryDescriptor descriptor = featureType.getGeometryDescriptor();
        Class<?> clazz = descriptor.getType().getBinding();
        if (clazz.equals(Point.class)) {
            return "node";
        } else {
            return "way";
        }
    }
}
