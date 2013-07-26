/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.cli.commands;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.geogit.cli.CLICommand;
import org.geogit.cli.CommandFailedException;
import org.geogit.cli.GeogitCLI;
import org.geogit.geotools.plumbing.ExportOp;
import org.geogit.geotools.plumbing.GeoToolsOpException;
import org.geogit.geotools.porcelain.AbstractShpCommand;
import org.geogit.osm.internal.MappedFeature;
import org.geogit.osm.internal.Mapping;
import org.geogit.osm.internal.MappingRule;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.vividsolutions.jts.geom.Point;

/**
 * Exports OSM into a shapefile, using a data mapping
 * 
 * @see ExportOp
 */
@Parameters(commandNames = "export-shp", commandDescription = "Export OSM data to shapefile, using a data mapping")
public class OSMExportShp extends AbstractShpCommand implements CLICommand {

    @Parameter(description = "<shapefile>", arity = 2)
    public List<String> args;

    @Parameter(names = { "--overwrite", "-o" }, description = "Overwrite output file")
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

        if (args == null || args.isEmpty() || args.size() != 1) {
            printUsage();
            throw new CommandFailedException();
        }

        String shapefile = args.get(0);

        File file = new File(shapefile);
        if (file.exists() && !overwrite) {
            cli.getConsole().println("The selected shapefile already exists. Use -o to overwrite");
            throw new CommandFailedException();
        }

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

        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put(ShapefileDataStoreFactory.URLP.key, new File(shapefile).toURI().toURL());
        params.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, Boolean.TRUE);
        ShapefileDataStore dataStore = (ShapefileDataStore) dataStoreFactory
                .createNewDataStore(params);
        dataStore.createSchema(outputFeatureType);

        final String typeName = dataStore.getTypeNames()[0];
        final SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);
        if (featureSource instanceof SimpleFeatureStore) {
            final SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
            ExportOp op = cli.getGeogit().command(ExportOp.class).setFeatureStore(featureStore)
                    .setPath(path).setFeatureTypeConversionFunction(function);
            try {
                op.setProgressListener(cli.getProgressListener()).call();
                cli.getConsole().println("OSM data exported successfully to " + shapefile);
            } catch (GeoToolsOpException e) {
                cli.getConsole().println("Could not export. Error:" + e.statusCode.name());
                file.delete();
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
