/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.cli.plumbing;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.List;

import jline.console.ConsoleReader;

import org.geogit.api.GeoGIT;
import org.geogit.api.RevObject;
import org.geogit.api.plumbing.CatObject;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.storage.ObjectSerializingFactory;
import org.geogit.storage.ObjectWriter;
import org.geogit.storage.datastream.DataStreamSerializationFactory;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;
import com.google.common.base.Suppliers;

/**
 * The cat commands describes a repository element with no formatting at all. For a more elaborated
 * display, see the {@link show} command.
 * 
 */
@Parameters(commandNames = "cat", commandDescription = "Describes a repository element")
public class Cat implements CLICommand {

    /**
     * The path to the element to display. Accepts all the notation types accepted by the RevParse
     * class
     */
    @Parameter(description = "<path>")
    private List<String> paths = new ArrayList<String>();

    /**
     * Produce binary output instead of text output
     */
    @Parameter(names = { "--binary" }, description = "Produce binary output")
    private boolean binary;

    /**
     * @param cli
     * @see org.geogit.cli.CLICommand#run(org.geogit.cli.GeogitCLI)
     */
    @Override
    public void run(GeogitCLI cli) throws Exception {
        checkState(cli.getGeogit() != null, "Not a geogit repository: " + cli.getPlatform().pwd());
        checkArgument(paths.size() < 2, "Only one refspec allowed");
        checkArgument(!paths.isEmpty(), "A refspec must be specified");

        ConsoleReader console = cli.getConsole();
        GeoGIT geogit = cli.getGeogit();

        String path = paths.get(0);

        Optional<RevObject> obj = geogit.command(RevObjectParse.class).setRefSpec(path).call();
        checkState(obj.isPresent(), "refspec did not resolve to any object.");
        if (binary) {
            ObjectSerializingFactory factory = new DataStreamSerializationFactory();
            ObjectWriter<RevObject> writer = factory.createObjectWriter(obj.get().getType());
            writer.write(obj.get(), System.out);
        } else {
            CharSequence s = geogit.command(CatObject.class)
                    .setObject(Suppliers.ofInstance(obj.get())).call();
            console.println(s);
        }
    }

}
