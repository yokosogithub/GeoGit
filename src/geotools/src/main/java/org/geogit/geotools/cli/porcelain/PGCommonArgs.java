/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.geotools.cli.porcelain;

import com.beust.jcommander.Parameter;

/**
 * Common arguments for PostGIS porcelain commands.
 * 
 */
public class PGCommonArgs {

    /**
     * Machine name or IP address to connect to. Default: localhost
     */
    @Parameter(names = "--host", description = "Machine name or IP address to connect to. Default: localhost")
    public String host = "localhost";

    /**
     * Port number to connect to. Default: 5432
     */
    @Parameter(names = "--port", description = "Port number to connect to.  Default: 5432")
    public Integer port = 5432;

    /**
     * The database schema to access. Default: public
     */
    @Parameter(names = "--schema", description = "The database schema to access.  Default: public")
    public String schema = "public";

    /**
     * The database to connect to. Default: database
     */
    @Parameter(names = "--database", description = "The database to connect to.  Default: database")
    public String database = "database";

    /**
     * User name. Default: postgres
     */
    @Parameter(names = "--user", description = "User name.  Default: postgres")
    public String username = "postgres";

    /**
     * Password. Default: <no password>
     */
    @Parameter(names = "--password", description = "Password.  Default: <no password>")
    public String password = "";

}
