/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.api.Ref;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.ResolveGeogitDir;
import org.geogit.api.plumbing.UpdateRef;
import org.geogit.api.plumbing.UpdateSymRef;
import org.geogit.di.CanRunDuringConflict;
import org.geogit.di.PluginDefaults;
import org.geogit.di.VersionedFormat;
import org.geogit.repository.Repository;
import org.geogit.repository.RepositoryConnectionException;
import org.geogit.storage.ConfigDatabase;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Creates or "initializes" a repository in the {@link Platform#pwd() working directory}.
 * <p>
 * This command tries to find an existing {@code .geogit} repository directory in the current
 * directory's hierarchy. It is safe to call it from inside a directory that's a child of a
 * repository.
 * <p>
 * If no repository directory is found, then a new one is created on the current directory.
 * 
 * @see ResolveGeogitDir
 * @see RefParse
 * @see UpdateRef
 * @see UpdateSymRef
 */
@CanRunDuringConflict
public class InitOp extends AbstractGeoGitOp<Repository> {

    private Platform platform;

    private Injector injector;

    private Map<String, String> config;
    
    private PluginDefaults defaults;

    private String filterFile;

    @Nullable
    private File targetDir;

    /**
     * Constructs a new {@code InitOp} with the specified parameters.
     * 
     * @param platform where to get the current directory from
     * @param injector where to get the repository from (with auto-wired dependencies) once ensured
     *        the {@code .geogit} repository directory is found or created.
     */
    @Inject
    public InitOp(Platform platform, Injector injector, PluginDefaults defaults) {
        checkNotNull(platform);
        checkNotNull(injector);
        checkNotNull(defaults);
        this.platform = platform;
        this.injector = injector;
        this.defaults = defaults;
        this.config = Maps.newTreeMap();
    }

    public InitOp setConfig(Map<String, String> suppliedConfiguration) {
        this.config = ImmutableMap.copyOf(suppliedConfiguration);
        return this;
    }

    public InitOp setTarget(File targetRepoDirectory) {
        this.targetDir = targetRepoDirectory;
        return this;
    }

    public InitOp setFilterFile(String filterFile) {
        this.filterFile = filterFile;
        return this;
    }

    /**
     * Executes the Init operation.
     * 
     * @return the initialized repository
     * @throws IllegalStateException if a repository cannot be created on the current directory or
     *         re-initialized in the current dir or one if its parents as determined by
     *         {@link ResolveGeogitDir}
     */
    @Override
    public Repository call() {
        final File workingDirectory = platform.pwd();
        checkState(workingDirectory != null, "working directory is null");

        final File targetDir = this.targetDir == null ? workingDirectory : this.targetDir;
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new IllegalArgumentException("Can't create directory "
                    + targetDir.getAbsolutePath());
        }
        Repository repository;
        try {
            platform.setWorkingDir(targetDir);
            repository = callInternal();
        } finally {
            // restore current directory
            platform.setWorkingDir(workingDirectory);
        }
        return repository;
    }

    private Repository callInternal() {
        final File workingDirectory = platform.pwd();
        final Optional<URL> repoUrl = new ResolveGeogitDir(platform).call();

        final boolean repoExisted = repoUrl.isPresent();
        final File envHome;
        if (repoExisted) {
            // we're at either the repo working dir or a subdirectory of it
            try {
                envHome = new File(repoUrl.get().toURI());
            } catch (URISyntaxException e) {
                throw Throwables.propagate(e);
            }
        } else {
            envHome = new File(workingDirectory, ".geogit");
            if (!envHome.mkdirs()) {
                throw new RuntimeException("Unable to create geogit environment at '"
                        + envHome.getAbsolutePath() + "'");
            }
        }

        Map<String, String> effectiveConfigBuilder = Maps.newTreeMap();
        if (config != null) {
            effectiveConfigBuilder.putAll(config);
        }
        addDefaults(defaults, effectiveConfigBuilder);

        if (filterFile != null) {
            try {
                final String FILTER_FILE = "filter.ini";

                File oldFilterFile = new File(filterFile);
                if (!oldFilterFile.exists()) {
                    throw new FileNotFoundException("No filter file found at " + filterFile + ".");
                }

                Optional<URL> envHomeURL = new ResolveGeogitDir(platform).call();
                Preconditions.checkState(envHomeURL.isPresent(), "Not inside a geogit directory");
                final URL url = envHomeURL.get();
                if (!"file".equals(url.getProtocol())) {
                    throw new UnsupportedOperationException(
                            "Sparse clone works only against file system repositories. "
                                    + "Repository location: " + url.toExternalForm());
                }

                File repoDir;
                try {
                    repoDir = new File(url.toURI());
                } catch (URISyntaxException e) {
                    throw new IllegalStateException("Unable to access directory "
                            + url.toExternalForm(), e);
                }

                File newFilterFile = new File(repoDir, FILTER_FILE);

                Files.copy(oldFilterFile, newFilterFile);
                effectiveConfigBuilder.put("sparse.filter", FILTER_FILE);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to copy filter file at path " + filterFile
                        + " to the new repository.", e);
            }
        }

        try {
            Preconditions.checkState(envHome.toURI().toURL()
                    .equals(new ResolveGeogitDir(platform).call().get()));
        } catch (MalformedURLException e) {
            Throwables.propagate(e);
        }

        Repository repository;
        try {
            if (!repoExisted) {
                ConfigDatabase configDB = injector.getInstance(ConfigDatabase.class);
                try {
                    for (Entry<String, String> pair : effectiveConfigBuilder.entrySet()) {
                        String key = pair.getKey();
                        String value = pair.getValue();
                        configDB.put(key, value);
                    }
                    repository = injector.getInstance(Repository.class);
                    repository.configure();
                } catch (RepositoryConnectionException e) {
                    throw new IllegalStateException(
                            "Unable to initialize repository for the first time: " + e.getMessage(),
                            e);
                }
            } else {
                repository = injector.getInstance(Repository.class);
            }
            try {
                repository.open();
            } catch (RepositoryConnectionException e) {
                throw new IllegalStateException("Error opening repository databases: "
                        + e.getMessage(), e);
            }
            createSampleHooks(envHome);
        } catch (ConfigException e) {
            throw e;
        } catch (RuntimeException e) {
            Throwables.propagateIfInstanceOf(e, IllegalStateException.class);
            throw new IllegalStateException("Can't access repository at '"
                    + envHome.getAbsolutePath() + "'", e);
        }

        if (!repoExisted) {
            try {
                createDefaultRefs();
            } catch (IllegalStateException e) {
                Throwables.propagate(e);
            }
        }
        return repository;
    }

    private void createSampleHooks(File envHome) {
        File hooks = new File(envHome, "hooks");
        hooks.mkdirs();
        if (!hooks.exists()) {
            throw new RuntimeException();
        }
        try {
            copyHookFile(hooks.getAbsolutePath(), "pre_commit.js.sample");
            // TODO: add other example hooks
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    private void copyHookFile(String folder, String file) throws IOException {
        URL url = Resources.getResource("org/geogit/api/hooks/" + file);
        OutputStream os = new FileOutputStream(new File(folder, file).getAbsolutePath());
        Resources.copy(url, os);
        os.close();
    }

    private void addDefaults(PluginDefaults defaults, Map<String, String> configProps) {
        Optional<VersionedFormat> refs = defaults.getRefs();
        Optional<VersionedFormat> objects = defaults.getObjects();
        Optional<VersionedFormat> staging = defaults.getStaging();
        Optional<VersionedFormat> graph = defaults.getGraph();
        if (refs.isPresent()) {
            configProps.put("storage.refs", refs.get().getFormat());
            configProps.put(refs.get().getFormat() + ".version", refs.get().getVersion());
        }
        if (objects.isPresent()) {
            configProps.put("storage.objects", objects.get().getFormat());
            configProps.put(objects.get().getFormat() + ".version", objects.get().getVersion());
        }
        if (staging.isPresent()) {
            configProps.put("storage.staging", staging.get().getFormat());
            configProps.put(staging.get().getFormat() + ".version", staging.get().getVersion());
        }
        if (graph.isPresent()) {
            configProps.put("storage.graph", graph.get().getFormat());
            configProps.put(graph.get().getFormat() + ".version", graph.get().getVersion());
        }
    }

    private void createDefaultRefs() {
        Optional<Ref> master = command(RefParse.class).setName(Ref.MASTER).call();
        Preconditions.checkState(!master.isPresent(), Ref.MASTER + " was already initialized.");
        command(UpdateRef.class).setName(Ref.MASTER).setNewValue(ObjectId.NULL)
                .setReason("Repository initialization").call();

        Optional<Ref> head = command(RefParse.class).setName(Ref.HEAD).call();
        Preconditions.checkState(!head.isPresent(), Ref.HEAD + " was already initialized.");
        command(UpdateSymRef.class).setName(Ref.HEAD).setNewValue(Ref.MASTER)
                .setReason("Repository initialization").call();

        Optional<Ref> workhead = command(RefParse.class).setName(Ref.WORK_HEAD).call();
        Preconditions
                .checkState(!workhead.isPresent(), Ref.WORK_HEAD + " was already initialized.");
        command(UpdateRef.class).setName(Ref.WORK_HEAD).setNewValue(ObjectId.NULL)
                .setReason("Repository initialization").call();

        Optional<Ref> stagehead = command(RefParse.class).setName(Ref.STAGE_HEAD).call();
        Preconditions.checkState(!stagehead.isPresent(), Ref.STAGE_HEAD
                + " was already initialized.");
        command(UpdateRef.class).setName(Ref.STAGE_HEAD).setNewValue(ObjectId.NULL)
                .setReason("Repository initialization").call();

    }
}
