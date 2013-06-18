/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.cli;

import org.geogit.cli.CLICommandExtension;
import org.geogit.osm.changeset.cli.CreateOSMChangeset;
import org.geogit.osm.history.cli.OSMHistoryImport;
import org.geogit.osm.in.cli.OSMDownload;
import org.geogit.osm.in.cli.OSMImport;
import org.geogit.osm.map.cli.OSMMap;
import org.geogit.osm.map.cli.OSMUnmap;
import org.geogit.osm.out.cli.OSMExport;
import org.geogit.osm.out.cli.OSMExportPG;
import org.geogit.osm.out.cli.OSMExportSL;
import org.geogit.osm.out.cli.OSMExportShp;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;

/**
 * {@link CLICommandExtension} that provides a {@link JCommander} for osm specific commands.
 * 
 * @see OSMHistoryImport
 */
@Parameters(commandNames = "osm", commandDescription = "GeoGit/OpenStreetMap integration utilities")
public class OSMCommandProxy implements CLICommandExtension {

    @Override
    public JCommander getCommandParser() {
        JCommander commander = new JCommander();
        commander.setProgramName("geogit osm");
        commander.addCommand("import-history", new OSMHistoryImport());
        commander.addCommand("import", new OSMImport());
        commander.addCommand("export", new OSMExport());
        commander.addCommand("download", new OSMDownload());
        commander.addCommand("create-changeset", new CreateOSMChangeset());
        commander.addCommand("map", new OSMMap());
        commander.addCommand("unmap", new OSMUnmap());
        commander.addCommand("export-shp", new OSMExportShp());
        commander.addCommand("export-pg", new OSMExportPG());
        commander.addCommand("export-sl", new OSMExportSL());
        return commander;
    }
}
