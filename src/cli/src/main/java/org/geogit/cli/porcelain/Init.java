/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.geogit.api.GeoGIT;
import org.geogit.api.plumbing.ResolveGeogitDir;
import org.geogit.api.porcelain.InitOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.CommandFailedException;
import org.geogit.cli.GeogitCLI;
import org.geogit.cli.annotation.RequiresRepository;
import org.geogit.repository.Repository;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.inject.Injector;

/**
 * This command creates an empty geogit repository - basically a .geogit directory with
 * subdirectories for the object, refs, index, and config databases. An initial HEAD that references
 * the HEAD of the master branch is also created.
 * <p>
 * CLI proxy for {@link InitOp}
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit init [<directory>]}
 * </ul>
 * 
 * @see InitOp
 */
@RequiresRepository(false)
@Parameters(commandNames = "init", commandDescription = "Create an empty geogit repository or reinitialize an existing one")
public class Init extends AbstractCommand implements CLICommand {

    @Parameter(description = "Repository location (directory).", required = false, arity = 1)
    private List<String> location;

    @Parameter(names = { "--config" }, description = "Extra configuration options to set while preparing repository. Separate names from values with an equals sign and delimit configuration options with a colon. Example: storage.objects=bdbje:bdbje.version=0.1")
    private String config;

    /**
     * Executes the init command.
     */
    @Override
    public void runInternal(GeogitCLI cli) throws IOException {
        // argument location if provided, or current directory otherwise
        final File targetDirectory;
        {
            File currDir = cli.getPlatform().pwd();
            if (location != null && location.size() == 1) {
                String target = location.get(0);
                File f = new File(target);
                if (!f.isAbsolute()) {
                    f = new File(currDir, target).getCanonicalFile();
                }
                targetDirectory = f;
            } else {
                targetDirectory = currDir;
            }
        }
        final boolean repoExisted;
        final Repository repository;
        {
            GeoGIT geogit = cli.getGeogit();
            if (geogit == null) {
                Injector geogitInjector = cli.getGeogitInjector();
                geogit = new GeoGIT(geogitInjector);
            }
            repoExisted = determineIfRepoExists(targetDirectory, geogit);
            final Map<String, String> suppliedConfiguration = splitConfig(config);

            try {
                repository = geogit.command(InitOp.class).setConfig(suppliedConfiguration)
                        .setTarget(targetDirectory).call();
            } catch (IllegalArgumentException e) {
                throw new CommandFailedException(e.getMessage(), e);
            } finally {
                geogit.close();
            }
        }

        File repoDirectory;
        try {
            repoDirectory = new File(repository.getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new CommandFailedException("Environment home can't be resolved to a directory", e);
        }
        String message;
        if (repoExisted) {
            message = "Reinitialized existing Geogit repository in "
                    + repoDirectory.getAbsolutePath();
        } else {
            message = "Initialized empty Geogit repository in " + repoDirectory.getAbsolutePath();
        }
        cli.getConsole().println(message);
    }

    private boolean determineIfRepoExists(final File targetDirectory, GeoGIT geogit) {
        final boolean repoExisted;

        final File currentDirectory = geogit.getPlatform().pwd();
        try {
            geogit.getPlatform().setWorkingDir(targetDirectory);
        } catch (IllegalArgumentException e) {
            return false;
        }
        final Optional<URL> currentRepoUrl = geogit.command(ResolveGeogitDir.class).call();
        repoExisted = currentRepoUrl.isPresent();
        geogit.getPlatform().setWorkingDir(currentDirectory);
        return repoExisted;
    }

    public static Map<String, String> splitConfig(final String configArg) {
        Map<String, String> configProps = Maps.newTreeMap();
        if (configArg != null) {
            String[] options = configArg.split(",");
            for (String option : options) {
                String[] kv = option.split("=", 2);
                if (kv.length < 2)
                    continue;
                configProps.put(kv[0], kv[1]);
            }
        }
        return configProps;
    }
}
