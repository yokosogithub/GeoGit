<<<<<<< .merge_file_RIWSib
/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.cli;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jline.Terminal;
import jline.console.ConsoleReader;
import jline.console.completer.AggregateCompleter;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;

import org.geogit.api.GeoGIT;
import org.geogit.api.Ref;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.RefParse;
import org.geogit.repository.Hints;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.io.Files;

/**
 * Provides the ability to execute several commands in succession without re-initializing GeoGit or
 * the command line interface.
 */
public class GeogitConsole {
    /**
     * Entry point for the Geogit console.
     * 
     * @param args unused
     */
    public static void main(String... args) {
        try {
            if (args.length == 1) {
                new GeogitConsole().runFile(args[0]);
            } else if (args.length == 0) {
                new GeogitConsole().run();
            } else {
                System.out.println("Too many arguments.\nUsage: geogit-console [batch_file]");
            }
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private void runFile(String filename) throws IOException {
        final File file = new File(filename);
        if (!file.exists()) {
            System.out.println("The specified batch file does not exist");
            return;
        }
        List<String> lines = Files.readLines(file, Charsets.UTF_8);

        InputStream in = System.in;
        OutputStream out = System.out;
        ConsoleReader consoleReader = new ConsoleReader(in, out);
        consoleReader.setAutoprintThreshold(20);
        consoleReader.setPaginationEnabled(true);
        // needed for CTRL+C not to let the console broken
        consoleReader.getTerminal().setEchoEnabled(true);

        final GeogitCLI cli = new GeogitCLI(consoleReader);
        cli.tryConfigureLogging();
        GeogitCLI.addShutdownHook(cli);

        try {
            for (String line : lines) {
                if (line.trim().length() == 0) {
                    continue;
                }
                if (line.trim().startsWith("#")) {// comment
                    continue;
                }
                String[] args = ArgumentTokenizer.tokenize(line);
                cli.execute(args);
            }

        } finally {
            Terminal terminal = consoleReader.getTerminal();
            try {
                cli.close();
            } finally {
                try {
                    terminal.restore();
                    consoleReader.shutdown();
                } catch (Exception e) {
                    throw Throwables.propagate(e);
                }
            }
        }

    }

    /**
     * @throws IOException
     * 
     */
    private void run() throws IOException {

        InputStream in = System.in;
        OutputStream out = System.out;
        ConsoleReader consoleReader = new ConsoleReader(in, out);
        consoleReader.setAutoprintThreshold(20);
        consoleReader.setPaginationEnabled(true);
        consoleReader.setHistoryEnabled(true);
        // needed for CTRL+C not to let the console broken
        consoleReader.getTerminal().setEchoEnabled(true);

        final GeogitCLI cli = new GeogitCLI(consoleReader);
        cli.tryConfigureLogging();

        final JCommander globalCommandParser = cli.newCommandParser();

        final Map<String, JCommander> commands = globalCommandParser.getCommands();

        List<Completer> completers = new ArrayList<Completer>(commands.size());
        for (Map.Entry<String, JCommander> entry : commands.entrySet()) {

            String commandName = entry.getKey();
            JCommander commandParser = entry.getValue();

            List<ParameterDescription> parameters = commandParser.getParameters();
            List<String> options = new ArrayList<String>(parameters.size());
            for (ParameterDescription pd : parameters) {
                String longestName = pd.getLongestName();
                options.add(longestName);
            }
            Collections.sort(options);

            ArgumentCompleter commandCompleter = new ArgumentCompleter(new StringsCompleter(
                    commandName), new StringsCompleter(options));
            completers.add(commandCompleter);
        }

        completers.add(new StringsCompleter("exit", "clear"));

        Completer completer = new AggregateCompleter(completers);
        consoleReader.addCompleter(completer);

        GeogitCLI.addShutdownHook(cli);

        setPrompt(cli);
        cli.close();

        try {
            runInternal(cli);
        } finally {
            Terminal terminal = consoleReader.getTerminal();
            try {
                cli.close();
            } finally {
                try {
                    terminal.restore();
                    consoleReader.shutdown();
                } catch (Exception e) {
                    throw Throwables.propagate(e);
                }
            }
        }
    }

    /**
     * Sets the command prompt
     * 
     * @throws IOException
     */
    private void setPrompt(GeogitCLI cli) throws IOException {

        String currentDir = new File(".").getCanonicalPath();
        String currentHead = "";
        GeoGIT geogit;
        try {
            geogit = cli.newGeoGIT(Hints.readOnly());
        } catch (Exception e) {
            geogit = null;
        }
        if (geogit != null) {
            try {
                Optional<Ref> ref = geogit.command(RefParse.class).setName(Ref.HEAD).call();
                if (ref.isPresent()) {
                    if (ref.get() instanceof SymRef) {
                        currentHead = ((SymRef) ref.get()).getTarget();
                        int idx = currentHead.lastIndexOf("/");
                        if (idx != -1) {
                            currentHead = currentHead.substring(idx + 1);
                        }
                    } else {
                        currentHead = ref.get().getObjectId().toString().substring(0, 7);
                    }
                    currentHead = " (" + currentHead + ")";
                }
            } finally {
                geogit.close();
            }
        }
        String prompt = "(geogit):" + currentDir + currentHead + " $ ";
        cli.getConsole().setPrompt(prompt);
    }

    private void runInternal(final GeogitCLI cli) throws IOException {

        final ConsoleReader consoleReader = cli.getConsole();
        while (true) {
            String line = consoleReader.readLine();
            if (line == null) {
                return;
            }
            if (line.trim().length() == 0) {
                continue;
            }

            String[] args = ArgumentTokenizer.tokenize(line);

            if (args != null && args.length == 1 && "exit".equals(args[0])) {
                return;
            }
            if (args != null && args.length == 1 && "clear".equals(args[0])) {
                consoleReader.clearScreen();
                consoleReader.redrawLine();
                continue;
            }

            cli.execute(args);
            setPrompt(cli);// in case HEAD has changed
            cli.close();
        }
    }

}
=======
/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jline.Terminal;
import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;
import jline.console.completer.AggregateCompleter;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;

import org.geogit.api.GeoGIT;
import org.geogit.api.Ref;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.RefParse;
import org.geogit.repository.Hints;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;

/**
 * Provides the ability to execute several commands in succession without re-initializing GeoGit or
 * the command line interface.
 */
public class GeogitConsole {

    private boolean interactive;

    /**
     * Entry point for the Geogit console.
     * 
     * @param args unused
     */
    public static void main(String... args) {
        try {
            if (args.length == 1) {
                new GeogitConsole().runFile(args[0]);
            } else if (args.length == 0) {
                new GeogitConsole().run();
            } else {
                System.out.println("Too many arguments.\nUsage: geogit-console [batch_file]");
            }
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private void runFile(String filename) throws IOException {
        final File file = new File(filename);
        if (!file.exists()) {
            System.out.println("The specified batch file does not exist");
            return;
        }

        // take the input from the console with its input stream directly from the file
        InputStream in = new FileInputStream(file);
        try {
            interactive = false;
            run(in, System.out);
        } finally {
            in.close();
        }
    }

    /**
     * @throws IOException
     * 
     */
    private void run() throws IOException {
        // interactive will be false if stdin/stdout is redirected
        interactive = null != System.console();
        run(System.in, System.out);
    }

    private void run(final InputStream in, final OutputStream out) throws IOException {
        final Terminal terminal;
        if (interactive) {
            terminal = null;/* let jline select an appropriate one */
        } else {
            // no colors in output
            terminal = new UnsupportedTerminal();
        }
        ConsoleReader consoleReader = new ConsoleReader(in, out, terminal);
        consoleReader.setAutoprintThreshold(20);
        consoleReader.setPaginationEnabled(interactive);
        consoleReader.setHistoryEnabled(interactive);
        // needed for CTRL+C not to let the console broken
        consoleReader.getTerminal().setEchoEnabled(interactive);

        final GeogitCLI cli = new GeogitCLI(consoleReader);
        cli.tryConfigureLogging();
        if (interactive) {
            addCommandCompleter(consoleReader, cli);
        } else {
            // no progress percent in output
            cli.disableProgressListener();
        }

        GeogitCLI.addShutdownHook(cli);

        setPrompt(cli);
        cli.close();

        try {
            runInternal(cli);
        } finally {
            try {
                cli.close();
            } finally {
                try {
                    if (interactive) {
                        terminal.restore();
                    }
                    consoleReader.shutdown();
                } catch (Exception e) {
                    throw Throwables.propagate(e);
                }
            }
        }
    }

    private void addCommandCompleter(ConsoleReader consoleReader, final GeogitCLI cli) {
        final JCommander globalCommandParser = cli.newCommandParser();

        final Map<String, JCommander> commands = globalCommandParser.getCommands();

        List<Completer> completers = new ArrayList<Completer>(commands.size());
        for (Map.Entry<String, JCommander> entry : commands.entrySet()) {

            String commandName = entry.getKey();
            JCommander commandParser = entry.getValue();

            List<ParameterDescription> parameters = commandParser.getParameters();
            List<String> options = new ArrayList<String>(parameters.size());
            for (ParameterDescription pd : parameters) {
                String longestName = pd.getLongestName();
                options.add(longestName);
            }
            Collections.sort(options);

            ArgumentCompleter commandCompleter = new ArgumentCompleter(new StringsCompleter(
                    commandName), new StringsCompleter(options));
            completers.add(commandCompleter);
        }

        completers.add(new StringsCompleter("exit", "clear"));

        Completer completer = new AggregateCompleter(completers);
        consoleReader.addCompleter(completer);
    }

    /**
     * Sets the command prompt
     * 
     * @throws IOException
     */
    private void setPrompt(GeogitCLI cli) throws IOException {
        if (!interactive) {
            return;
        }
        String currentDir = new File(".").getCanonicalPath();
        String currentHead = "";
        GeoGIT geogit;
        try {
            geogit = cli.newGeoGIT(Hints.readOnly());
        } catch (Exception e) {
            geogit = null;
        }
        if (geogit != null) {
            try {
                Optional<Ref> ref = geogit.command(RefParse.class).setName(Ref.HEAD).call();
                if (ref.isPresent()) {
                    if (ref.get() instanceof SymRef) {
                        currentHead = ((SymRef) ref.get()).getTarget();
                        int idx = currentHead.lastIndexOf("/");
                        if (idx != -1) {
                            currentHead = currentHead.substring(idx + 1);
                        }
                    } else {
                        currentHead = ref.get().getObjectId().toString().substring(0, 7);
                    }
                    currentHead = " (" + currentHead + ")";
                }
            } finally {
                geogit.close();
            }
        }
        String prompt = "(geogit):" + currentDir + currentHead + " $ ";
        cli.getConsole().setPrompt(prompt);
    }

    private void runInternal(final GeogitCLI cli) throws IOException {

        final ConsoleReader consoleReader = cli.getConsole();
        while (true) {
            String line = consoleReader.readLine();
            if (line == null) {
                // EOF / CTRL-D
                return;
            }
            if (line.trim().length() == 0) {
                continue;
            }
            if (line.trim().startsWith("#")) {// comment
                continue;
            }

            String[] args = ArgumentTokenizer.tokenize(line);

            if (interactive && args != null && args.length == 1) {
                if ("exit".equals(args[0])) {
                    return;
                }
                if ("clear".equals(args[0])) {
                    consoleReader.clearScreen();
                    consoleReader.redrawLine();
                    continue;
                }
            }

            cli.execute(args);
            setPrompt(cli);// in case HEAD has changed
            cli.close();
        }
    }

}
>>>>>>> .merge_file_zb8fFa
