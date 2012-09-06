/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.cli.spatial;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import jline.console.ConsoleReader;

import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.repository.WorkingTree;
import org.geotools.data.DataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.internal.Maps;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

@Parameters(commandNames = "shpimport", commandDescription = "Imports a shapefile to the working tree")
public class ImportShp extends AbstractCommand implements CLICommand {

    @Parameter(description = "<shapefile> [<shapefile>]...")
    List<String> shapeFile;

    /**
     * @param cli
     * @see org.geogit.cli.CLICommand#run(org.geogit.cli.GeogitCLI)
     */
    @Override
    public void runInternal(GeogitCLI cli) {
        Preconditions.checkState(cli.getGeogit() != null, "Not a geogit repository: "
                + cli.getPlatform().pwd());
        Preconditions.checkState(shapeFile != null && !shapeFile.isEmpty(),
                "No shapefile specified");

        try {
            for (String shp : shapeFile) {
                doImport(cli, shp);
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            try {
                cli.getConsole().flush();
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }
    }

    private void doImport(GeogitCLI cli, String shp) throws Exception {
        final ConsoleReader console = cli.getConsole();
        console.println("Importing shapefile " + shapeFile);

        Map<String, Serializable> params = Maps.newHashMap();
        params.put(ShapefileDataStoreFactory.URLP.key, shp);
        params.put(ShapefileDataStoreFactory.NAMESPACEP.key, "http://www.opengis.net/gml");

        DataStore dataStore = new ShapefileDataStoreFactory().createDataStore(params);
        Name typeName = dataStore.getNames().get(0);
        if (null == typeName.getNamespaceURI()) {
            typeName = new NameImpl("http://www.opengis.net/gml", typeName.getLocalPart());
        }
        System.err.println(typeName);

        WorkingTree workingTree = cli.getGeogit().getRepository().getWorkingTree();

        SimpleFeatureCollection features = dataStore.getFeatureSource(typeName).getFeatures();

        // workingTree.delete(typeName);
        workingTree.insert(features, true, cli.getProgressListener());
    }
}
