/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import java.util.Date;
import java.util.Iterator;

import jline.Terminal;
import jline.console.ConsoleReader;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.geogit.api.GeoGIT;
import org.geogit.api.RevCommit;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.AnsiDecorator;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.cli.Platform;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterators;

/**
 *
 */
@Service
@Scope(value = "prototype")
@Parameters(commandNames = "log", commandDescription = "Show commit logs")
public class Log extends AbstractCommand implements CLICommand {

    @Parameter(names = { "--max-count", "-n" }, description = "Maximum number of commits to log.")
    public Integer limit;

    @Parameter(names = "--skip", description = "Skip number commits before starting to show the commit output.")
    private Integer skip;

    @Parameter(names = "--since", description = "Maximum number of commits to log")
    private String since;

    @Parameter(names = "--until", description = "Maximum number of commits to log")
    private String until;

    private String paths;

    @Parameter(names = "--color", description = "Whether to apply colored output. Possible values are auto|never|always.", converter = ColorArg.Converter.class)
    private ColorArg color = ColorArg.auto;

    @Override
    public void runInternal(GeogitCLI cli) {
        final Platform platform = cli.getPlatform();
        Preconditions.checkState(cli.getGeogit() != null, "Not a geogit repository: "
                + platform.pwd().getAbsolutePath());

        final GeoGIT geogit = cli.getGeogit();
        Iterator<RevCommit> log;
        try {
            log = geogit.log().call();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        if (skip != null) {
            Iterators.skip(log, skip.intValue());
        }
        if (limit != null) {
            log = Iterators.limit(log, limit.intValue());
        }

        cli.getPlatform();
        ConsoleReader console = cli.getConsole();
        Terminal terminal = console.getTerminal();
        try {
            if (log.hasNext()) {
                while (log.hasNext()) {
                    RevCommit commit = log.next();

                    boolean useColor;
                    switch (color) {
                    case never:
                        useColor = false;
                        break;
                    case always:
                        useColor = true;
                        break;
                    default:
                        useColor = terminal.isAnsiSupported();
                    }
                    Ansi ansi = AnsiDecorator.newAnsi(useColor);

                    ansi.a("Commit:  ").fg(Color.YELLOW).a(commit.getId().toString()).reset()
                            .newline();
                    ansi.a("Author:  ").fg(Color.GREEN).a(commit.getAuthor()).reset().newline();
                    ansi.a("Date:    (").fg(Color.RED)
                            .a(estimateSince(platform, commit.getTimestamp())).reset().a(") ")
                            .a(new Date(commit.getTimestamp())).newline();
                    ansi.a("Subject: ").a(commit.getMessage()).newline();

                    console.print(ansi.toString());

                    if (log.hasNext()) {
                        console.println("");
                    }
                }
            } else {
                console.println("No commits to show");
            }
            console.flush();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private String estimateSince(Platform platform, long timestamp) {
        long now = platform.currentTimeMillis();
        long diff = now - timestamp;
        final long seconds = 1000;
        final long minutes = seconds * 60;
        final long hours = minutes * 60;
        final long days = hours * 24;
        final long weeks = days * 7;
        final long months = days * 30;
        final long years = days * 365;

        if (diff > years) {
            return diff / years + " years ago";
        }
        if (diff > months) {
            return diff / months + " months ago";
        }
        if (diff > weeks) {
            return diff / weeks + " weeks ago";
        }
        if (diff > days) {
            return diff / days + " days ago";
        }
        if (diff > hours) {
            return diff / hours + " hours ago";
        }
        if (diff > minutes) {
            return diff / minutes + " minutes ago";
        }
        if (diff > seconds) {
            return diff / seconds + " seconds ago";
        }
        return "just now";
    }
}
