/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.cli;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 *
 */
@Service
@Scope(value = "prototype")
@Parameters(commandNames = { "help", "--help" }, commandDescription = "Print this help message, or provide a command name to get help for")
public class Help implements CLICommand {

    @Parameter
    private List<String> parameters = new ArrayList<String>();

    // @Override
    public void run(GeogitCLI cli) {

        JCommander jc = cli.newCommandParser();

        if (parameters.isEmpty()) {
            jc.usage();
        } else {
            String command = parameters.get(0);
            jc.usage(command);
        }
    }

}
