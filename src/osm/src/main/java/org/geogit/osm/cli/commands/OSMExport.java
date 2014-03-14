/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.cli.commands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.geogit.api.Bounded;
import org.geogit.api.GeoGIT;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.plumbing.LsTreeOp;
import org.geogit.api.plumbing.LsTreeOp.Strategy;
import org.geogit.api.plumbing.ResolveTreeish;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.RevParse;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.CommandFailedException;
import org.geogit.cli.GeogitCLI;
import org.geogit.cli.annotation.ReadOnly;
import org.geogit.geotools.plumbing.ExportOp;
import org.geogit.osm.internal.EntityConverter;
import org.geogit.osm.internal.OSMUtils;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.osmbinary.file.BlockOutputStream;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlWriter;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.vividsolutions.jts.geom.Envelope;

import crosby.binary.osmosis.OsmosisSerializer;

/**
 * Exports features from a feature type into a shapefile.
 * 
 * @see ExportOp
 */
@ReadOnly
@Parameters(commandNames = "export", commandDescription = "Export to OSM format")
public class OSMExport extends AbstractCommand implements CLICommand {

    @Parameter(description = "<file> [commitish]")
    public List<String> args;

    @Parameter(names = { "--overwrite", "-o" }, description = "Overwrite output file")
    public boolean overwrite;

    @Parameter(names = { "--bbox", "-b" }, description = "The bounding box to use as filter (S W N E).", arity = 4)
    private List<String> bbox;

    private GeoGIT geogit;

    /**
     * Executes the export command using the provided options.
     */
    @Override
    protected void runInternal(GeogitCLI cli) throws IOException {
        if (args.size() < 1 || args.size() > 2) {
            printUsage();
            throw new CommandFailedException();
        }

        checkParameter(bbox == null || bbox.size() == 4, "The specified bounding box is not correct");

        geogit = cli.getGeogit();

        String osmfile = args.get(0);

        String ref = "WORK_HEAD";
        if (args.size() == 2) {
            ref = args.get(1);
            Optional<ObjectId> tree = geogit.command(ResolveTreeish.class).setTreeish(ref).call();
            checkParameter(tree.isPresent(), "Invalid commit or reference: %s", ref);
        }

        File file = new File(osmfile);
        checkParameter(!file.exists() || overwrite,
                "The selected file already exists. Use -o to overwrite");

        Iterator<EntityContainer> nodes = getFeatures(ref + ":node");
        Iterator<EntityContainer> ways = getFeatures(ref + ":way");
        Iterator<EntityContainer> iterator = Iterators.concat(nodes, ways);
        if (file.getName().endsWith(".pbf")) {
            BlockOutputStream output = new BlockOutputStream(new FileOutputStream(file));
            OsmosisSerializer serializer = new OsmosisSerializer(output);
            while (iterator.hasNext()) {
                EntityContainer entity = iterator.next();
                serializer.process(entity);
            }
            serializer.complete();
        } else {
            XmlWriter writer = new XmlWriter(file, CompressionMethod.None);
            while (iterator.hasNext()) {
                EntityContainer entity = iterator.next();
                writer.process(entity);
            }
            writer.complete();
        }

    }

    private Iterator<EntityContainer> getFeatures(String ref) {
        Optional<ObjectId> id = geogit.command(RevParse.class).setRefSpec(ref).call();
        if (!id.isPresent()) {
            return Iterators.emptyIterator();
        }
        LsTreeOp op = geogit.command(LsTreeOp.class).setStrategy(Strategy.DEPTHFIRST_ONLY_FEATURES)
                .setReference(ref);
        if (bbox != null) {
            final Envelope env;
            try {
                env = new Envelope(Double.parseDouble(bbox.get(0)),
                        Double.parseDouble(bbox.get(2)), Double.parseDouble(bbox.get(1)),
                        Double.parseDouble(bbox.get(3)));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Wrong bbox definition");
            }
            Predicate<Bounded> filter = new Predicate<Bounded>() {
                @Override
                public boolean apply(final Bounded bounded) {
                    boolean intersects = bounded.intersects(env);
                    return intersects;
                }
            };
            op.setBoundsFilter(filter);
        }
        Iterator<NodeRef> iterator = op.call();
        final EntityConverter converter = new EntityConverter();
        Function<NodeRef, EntityContainer> function = new Function<NodeRef, EntityContainer>() {

            @Override
            @Nullable
            public EntityContainer apply(@Nullable NodeRef ref) {
                RevFeature revFeature = geogit.command(RevObjectParse.class)
                        .setObjectId(ref.objectId()).call(RevFeature.class).get();
                SimpleFeatureType featureType;
                if (ref.path().startsWith(OSMUtils.NODE_TYPE_NAME)) {
                    featureType = OSMUtils.nodeType();
                } else {
                    featureType = OSMUtils.wayType();
                }
                SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
                RevFeatureType revFeatureType = RevFeatureType.build(featureType);
                List<PropertyDescriptor> descriptors = revFeatureType.sortedDescriptors();
                ImmutableList<Optional<Object>> values = revFeature.getValues();
                for (int i = 0; i < descriptors.size(); i++) {
                    PropertyDescriptor descriptor = descriptors.get(i);
                    Optional<Object> value = values.get(i);
                    featureBuilder.set(descriptor.getName(), value.orNull());
                }
                SimpleFeature feature = featureBuilder.buildFeature(ref.name());
                Entity entity = converter.toEntity(feature, null);
                EntityContainer container;
                if (entity instanceof Node) {
                    container = new NodeContainer((Node) entity);
                } else {
                    container = new WayContainer((Way) entity);
                }

                return container;

            }

        };
        return Iterators.transform(iterator, function);
    }
}
