/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import java.io.File;
import java.net.URI;
import java.util.Properties;

import org.geogit.api.GeoGIT;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.cli.Platform;
import org.geogit.repository.Repository;
import org.geogit.storage.RepositoryDatabase;
import org.geogit.storage.bdbje.EntityStoreConfig;
import org.geogit.storage.bdbje.EnvironmentBuilder;
import org.geogit.storage.bdbje.JERepositoryDatabase;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.sleepycat.je.Environment;

/**
 *
 */
@Service
@Scope(value = "prototype")
@Parameters(commandNames = "init", commandDescription = "Create an empty geogit repository or reinitialize an existing one")
public class Init extends AbstractCommand implements CLICommand {

    @Parameter(names = "location", description = "Repository location (directory).", required = false)
    private URI location;

    @Override
    public void runInternal(GeogitCLI cli/* TODO , ProgressListener progress */) {

        Platform platform = cli.getPlatform();
        File envHome = new File(platform.pwd(), ".geogit");
        envHome.mkdirs();
        if (!envHome.exists()) {
            throw new RuntimeException("Unable to create geogit environment at '"
                    + envHome.getAbsolutePath() + "'");
        }

        File repositoryHome = new File(envHome, "objects");
        File indexHome = new File(envHome, "index");
        repositoryHome.mkdir();
        indexHome.mkdir();

        EntityStoreConfig config = new EntityStoreConfig();
        config.setCacheMemoryPercentAllowed(50);
        EnvironmentBuilder esb = new EnvironmentBuilder(config);
        Properties bdbEnvProperties = null;
        Environment environment;
        environment = esb.buildEnvironment(repositoryHome, bdbEnvProperties);

        Environment stagingEnvironment;
        stagingEnvironment = esb.buildEnvironment(indexHome, bdbEnvProperties);

        RepositoryDatabase repositoryDatabase = new JERepositoryDatabase(environment,
                stagingEnvironment);

        Repository repository = new Repository(repositoryDatabase, envHome);

        repository.create();

        GeoGIT geogit = new GeoGIT(repository);
        cli.setGeogit(geogit);

        System.err.println("Repository created at " + envHome.getAbsolutePath());
    }

}