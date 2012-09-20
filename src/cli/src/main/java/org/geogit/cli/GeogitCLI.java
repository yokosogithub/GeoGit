/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.cli;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import javax.annotation.Nullable;

import jline.console.ConsoleReader;
import jline.console.CursorBuffer;

import org.geogit.api.DefaultPlatform;
import org.geogit.api.GeoGIT;
import org.geogit.api.Platform;
import org.geogit.api.plumbing.ResolveGeogitDir;
import org.geogit.di.GeogitModule;
import org.geogit.storage.bdbje.JEStorageModule;
import org.geotools.util.DefaultProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Throwables;
import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.util.Modules;

/**
 * Command Line Interface for geogit.
 * <p>
 * Looks up and executes {@link CLICommand} implementations provided by any {@link Guice}
 * {@link Module} that implements {@link CLIModule} declared in any classpath's
 * {@code META-INF/services/com.google.inject.Module} file.
 */
public class GeogitCLI {

    private Injector injector;

    private Platform platform;

    private GeoGIT geogit;

    private ConsoleReader consoleReader;

    private DefaultProgressListener progressListener;

    /**
     * @param console
     */
    public GeogitCLI(final ConsoleReader consoleReader) {
        this.consoleReader = consoleReader;
        this.platform = new DefaultPlatform();

        Iterable<CLIModule> plugins = ServiceLoader.load(CLIModule.class);
        injector = Guice.createInjector(plugins);
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        checkNotNull(platform);
        this.platform = platform;
    }

    /**
     * Provides a GeoGIT facade configured for the current repository if inside a repository,
     * {@code null} otherwise.
     * <p>
     * Note the repository is lazily loaded and cached afterwards to simplify the execution of
     * commands or command options that do not need a live repository.
     * 
     * @return
     */
    public synchronized GeoGIT getGeogit() {
        if (geogit == null) {
            GeoGIT geogit = loadRepository();
            setGeogit(geogit);
        }
        return geogit;
    }

    public void setGeogit(@Nullable GeoGIT geogit) {
        this.geogit = geogit;
    }

    /**
     * Loads the repository _if_ inside a geogit repository and returns a configured {@link GeoGIT}
     * facade.
     * 
     * @return a geogit for the current repository or {@code null} if not inside a geogit repository
     *         directory.
     */
    private GeoGIT loadRepository() {
        Injector inj = Guice.createInjector(Modules.override(new GeogitModule()).with(
                new JEStorageModule()));
        GeoGIT geogit = new GeoGIT(inj, platform.pwd());

        if (null != geogit.command(ResolveGeogitDir.class).call()) {
            geogit.getRepository();
            return geogit;
        }

        return null;
    }

    public ConsoleReader getConsole() {
        return consoleReader;
    }

    public void close() {
        if (geogit != null) {
            geogit.close();
            geogit = null;
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        Logging.ALL.forceMonolineConsoleOutput();
        // TODO: revisit in case we need to grafefully shutdown upon CTRL+C
        // Runtime.getRuntime().addShutdownHook(new Thread() {
        // @Override
        // public void run() {
        // System.err.println("Shutting down...");
        // System.err.flush();
        // }
        // });

        ConsoleReader consoleReader;
        try {
            consoleReader = new ConsoleReader(System.in, System.out);
            // needed for CTRL+C not to let the console broken
            consoleReader.getTerminal().setEchoEnabled(true);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

        int exitCode = 0;
        GeogitCLI cli = null;
        try {
            cli = new GeogitCLI(consoleReader);
            cli.execute(args);
        } catch (Exception e) {
            exitCode = -1;
            try {
                if (e instanceof ParameterException) {
                    consoleReader.println(e.getMessage() + ". See geogit --help.");
                    consoleReader.flush();
                } else if (e instanceof IllegalArgumentException
                        || e instanceof IllegalStateException) {
                    consoleReader.println(e.getMessage());
                    consoleReader.flush();
                } else {
                    e.printStackTrace();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        } finally {
            try {
                if (cli != null) {
                    cli.close();
                }
            } finally {
                try {
                    consoleReader.getTerminal().restore();
                } catch (Exception e) {
                    e.printStackTrace();
                    exitCode = -1;
                }
            }
        }
        System.exit(exitCode);
    }

    public Collection<Key<?>> findCommands() {
        Map<Key<?>, Binding<?>> commands = injector.getBindings();
        return commands.keySet();
    }

    public JCommander newCommandParser() {
        JCommander jc = new JCommander(this);
        jc.setProgramName("geogit");
        for (Key<?> cmd : findCommands()) {
            Object obj = injector.getInstance(cmd);
            if (obj instanceof CLICommand) {
                jc.addCommand(obj);
            }
        }
        return jc;
    }

    /**
     * @param args
     */
    public void execute(String... args) throws Exception {
        JCommander jc = newCommandParser();
        jc.parse(args);
        final String parsedCommand = jc.getParsedCommand();
        if (null == parsedCommand) {
            jc.usage();
        } else {
            JCommander jCommander = jc.getCommands().get(parsedCommand);
            List<Object> objects = jCommander.getObjects();
            CLICommand cliCommand = (CLICommand) objects.get(0);
            cliCommand.run(this);
            getConsole().flush();
        }
    }

    public synchronized ProgressListener getProgressListener() {
        if (this.progressListener == null) {

            this.progressListener = new DefaultProgressListener() {

                private final Platform platform = getPlatform();

                private final ConsoleReader console = getConsole();

                private final NumberFormat fmt = NumberFormat.getPercentInstance();

                private final long delayMillis = 300;

                private volatile long lastRun = platform.currentTimeMillis();

                @Override
                public void complete() {
                    // avoid double logging if caller missbehaves
                    if (super.isCompleted()) {
                        return;
                    }
                    super.complete();
                    super.dispose();
                    try {
                        log(100f);
                        console.println();
                        console.flush();
                    } catch (IOException e) {
                        Throwables.propagate(e);
                    }
                }

                @Override
                public void progress(float percent) {
                    super.progress(percent);
                    long currentTimeMillis = platform.currentTimeMillis();
                    if ((currentTimeMillis - lastRun) > delayMillis) {
                        lastRun = currentTimeMillis;
                        log(percent);
                    }
                }

                private void log(float percent) {
                    CursorBuffer cursorBuffer = console.getCursorBuffer();
                    cursorBuffer.clear();
                    cursorBuffer.write(fmt.format(percent / 100f));
                    try {
                        console.redrawLine();
                        console.flush();
                    } catch (IOException e) {
                        Throwables.propagate(e);
                    }
                }
            };

        }
        return this.progressListener;
    }
}
