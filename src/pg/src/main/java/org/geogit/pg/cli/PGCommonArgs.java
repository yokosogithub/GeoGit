/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.pg.cli;

import com.beust.jcommander.Parameter;

/**
 *
 */
public class PGCommonArgs {

    @Parameter(names = "--host", description = "Machine name or IP address to connect to. Default: localhost")
    public String host = "localhost";

    @Parameter(names = "--port", description = "Port number to connect to.  Default: 5432.")
    public Integer port = 5432;

    @Parameter(names = "--schema", description = "The database schema to access.  Default: public")
    public String schema = "public";

    @Parameter(names = "--database", description = "The databse to connect to.  Default: database")
    public String database = "database";

    @Parameter(names = "--user", description = "User name.  Default: postgres")
    public String username = "postgres";

    @Parameter(names = "--password", description = "Password.  Default: <no password>")
    public String password = "";

}
