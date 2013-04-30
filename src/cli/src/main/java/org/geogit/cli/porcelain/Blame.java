/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.cli.porcelain;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.fusesource.jansi.Ansi.Color.GREEN;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jline.console.ConsoleReader;

import org.fusesource.jansi.Ansi;
import org.geogit.api.GeoGIT;
import org.geogit.api.RevCommit;
import org.geogit.api.porcelain.BlameOp;
import org.geogit.api.porcelain.BlameReport;
import org.geogit.cli.AnsiDecorator;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Shows information about the commits and authors that have modified the current attributes of a
 * given feature
 * 
 */
@Parameters(commandNames = "blame", commandDescription = "Shows information about authors of modifications for a single feature")
public class Blame implements CLICommand {

    /**
     * The path to the element to analyze.
     */
    @Parameter(description = "<path>")
    private List<String> paths = new ArrayList<String>();

    @Parameter(names = { "--porcelain" }, description = "Use porcelain output format")
    private boolean porcelain = false;

    /**
     * @param cli
     * @see org.geogit.cli.CLICommand#run(org.geogit.cli.GeogitCLI)
     */
    @Override
    public void run(GeogitCLI cli) throws Exception {
        checkState(cli.getGeogit() != null, "Not a geogit repository: " + cli.getPlatform().pwd());
        checkArgument(paths.size() < 2, "Only one path allowed");
        checkArgument(!paths.isEmpty(), "A path must be specified");

        ConsoleReader console = cli.getConsole();
        GeoGIT geogit = cli.getGeogit();

        String path = paths.get(0);

        BlameReport report = geogit.command(BlameOp.class).setPath(path).call();

        Map<String, RevCommit> changes = report.getChanges();
        Iterator<String> iter = changes.keySet().iterator();
        while (iter.hasNext()) {
            String attrib = iter.next();
            RevCommit commit = changes.get(attrib);
            if (porcelain) {
                StringBuilder sb = new StringBuilder();
                sb.append(attrib).append(" ");
                sb.append(commit.getId().toString()).append(" ");
                sb.append(commit.getAuthor().getName().or("")).append(" ");
                sb.append(commit.getAuthor().getEmail().or("")).append(" ");
                sb.append(Long.toString(commit.getAuthor().getTimestamp())).append(" ");
                sb.append(Integer.toString(commit.getAuthor().getTimeZoneOffset()));
                console.println(sb.toString());
            } else {
                Ansi ansi = AnsiDecorator.newAnsi(console.getTerminal().isAnsiSupported());
                ansi.fg(GREEN).a(attrib + ": ").reset();
                ansi.a(commit.getId().toString().substring(0, 7)).a(" ");
                ansi.a(commit.getAuthor().getName().or("")).a(" ");
                ansi.a(commit.getAuthor().getEmail().or("")).a(" ");
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                String date = formatter.format(new Date(commit.getAuthor().getTimestamp()
                        + commit.getAuthor().getTimeZoneOffset()));
                ansi.a(date);
                console.println(ansi.toString());
            }
        }
    }
}
