/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.pg.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

/**
 *
 */
public class PGDescribeArgs {

    @ParametersDelegate
    public PGCommonArgs common = new PGCommonArgs();

    @Parameter(names = { "--table", "-t" }, description = "Table to describe.", required = true)
    public String table = "";

}
