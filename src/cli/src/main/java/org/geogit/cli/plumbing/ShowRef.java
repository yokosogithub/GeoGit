/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.cli.plumbing;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.List;

import jline.console.ConsoleReader;

import org.geogit.api.GeoGIT;
import org.geogit.api.Ref;
import org.geogit.api.plumbing.ForEachRef;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;

/**
 * Displays a list of refs in a repository
 * 
 */
@Parameters(commandNames = "show-ref", commandDescription = "Shows a list of refs")
public class ShowRef implements CLICommand {

    /**
     * The path to the element to display. Accepts all the notation types accepted by the RevParse
     * class
     */
    @Parameter(description = "<pattern>")
    private List<String> patterns = new ArrayList<String>();

    /**
     * @param cli
     * @see org.geogit.cli.CLICommand#run(org.geogit.cli.GeogitCLI)
     */
    @Override
    public void run(GeogitCLI cli) throws Exception {
        checkState(cli.getGeogit() != null, "Not a geogit repository: " + cli.getPlatform().pwd());

        ConsoleReader console = cli.getConsole();
        GeoGIT geogit = cli.getGeogit();

        ForEachRef op = geogit.command(ForEachRef.class);
        if (!patterns.isEmpty()) {
            Predicate<Ref> filter = new Predicate<Ref>() {
                @Override
                public boolean apply(Ref ref) {
                    for (String pattern : patterns) {
                        if (ref != null && ref.getName().endsWith("/" + pattern)) {
                            return true;
                        }
                    }
                    return false;
                }
            };
            op.setFilter(filter);
        }
        ImmutableSet<Ref> refs = op.call();

        for (Ref ref : refs) {
            console.println(ref.getObjectId() + " " + ref.getName());
        }
    }

}
