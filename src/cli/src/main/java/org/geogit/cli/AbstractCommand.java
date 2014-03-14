/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.cli;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.IllegalFormatException;

import javax.annotation.Nullable;

import jline.Terminal;

import org.fusesource.jansi.Ansi;
import org.geogit.cli.annotation.RequiresRepository;
import org.geogit.cli.porcelain.ColorArg;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Throwables;

/**
 * A template command.
 * <p>
 * Services provided to subclasses:
 * <ul>
 * <li>Defines {@link RequiresRepository @RequiresRepository(true)} so its inherited by subclasses
 * and they need only to declare it if they don't require a repository present (which is the least
 * of the cases).
 * <li>Out of the box support for the {@code --help} argument
 * <li>Out of the box support for the hidden {@code --color} argument, allowing any command to
 * seamlessly support output text coloring or disabling it (see {@link ColorArg})
 * <li>The {@link #newAnsi(Terminal)} method provides an {@link Ansi} instance configured to support
 * coloring or not depending on the {@link Terminal} capabilities and the value of the
 * {@code --color} argument, if present.
 * </p>
 * 
 */
@RequiresRepository(true)
public abstract class AbstractCommand implements CLICommand {

    @Parameter(names = "--help", help = true, hidden = true)
    public boolean help;

    @Parameter(hidden = true, names = "--color", description = "Whether to apply colored output. Possible values are auto|never|always.", converter = ColorArg.Converter.class)
    public ColorArg color = ColorArg.auto;

    @Override
    public void run(GeogitCLI cli) throws InvalidParameterException, CommandFailedException {
        checkNotNull(cli, "No GeogitCLI provided");
        if (help) {
            printUsage();
            return;
        }

        try {
            runInternal(cli);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    protected Ansi newAnsi(Terminal terminal) {
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
        return ansi;
    }

    protected Ansi newAnsi(Terminal terminal, StringBuilder target) {
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

        Ansi ansi = AnsiDecorator.newAnsi(useColor, target);
        return ansi;
    }

    /**
     * Subclasses shall implement to do the real work, will not be called if the command was invoked
     * with {@code --help}. Also, {@link GeogitCLI#getGeogit() cli.getGeogit()} is guaranteed to be
     * non null (e.g. there's a working repository) if the implementation class is marked with the
     * {@link RequiresRepository @RequiresRepository} annotation.
     * 
     * @throws InvalidParameterException as per {@link CLICommand#run(GeogitCLI)}
     * @throws CommandFailedException as per {@link CLICommand#run(GeogitCLI)}
     * @throws IOException <b>only</b> propagated back if the IOException was thrown while writing
     *         to the {@link GeogitCLI#getConsole() console}.
     * @param cli
     */
    protected abstract void runInternal(GeogitCLI cli) throws InvalidParameterException,
            CommandFailedException, IOException;

    /**
     * Prints the JCommander usage for this command.
     */
    public void printUsage() {
        JCommander jc = new JCommander(this);
        String commandName = this.getClass().getAnnotation(Parameters.class).commandNames()[0];
        jc.setProgramName("geogit " + commandName);
        jc.usage();
    }

    /**
     * Checks the truth of the boolean expression and throws a {@link InvalidParameterException} if
     * its {@code false}.
     * <p>
     * CLI commands may use this helper method to check the validity of user supplied command
     * arguments.
     * 
     * @param expression a boolean expression
     * @param errorMessage the exception message to use if the check fails; will be converted to a
     *        string using {@link String#valueOf(Object)}
     * @throws InvalidParameterException if {@code expression} is false
     */
    public static void checkParameter(boolean expression, @Nullable Object errorMessage) {
        if (!expression) {
            throw new InvalidParameterException(String.valueOf(errorMessage));
        }
    }

    /**
     * /** Checks the truth of the boolean expression and throws a {@link InvalidParameterException}
     * if its {@code false}.
     * <p>
     * CLI commands may use this helper method to check the validity of user supplied command
     * arguments.
     * 
     * @param expression a boolean expression
     * @param errorMessageTemplate a template for the exception message should the check fail. The
     *        message is formed as per {@link String#format(String, Object...)}
     * @param errorMessageArgs the arguments to be substituted into the message template.
     * @throws InvalidParameterException if {@code expression} is {@code false}
     * @throws IllegalFormatException if thrown by {@link String#format(String, Object...)}
     * @throws NullPointerException If the <tt>format</tt> is {@code null}
     * 
     */
    public static void checkParameter(boolean expression, String errorMessageTemplate,
            Object... errorMessageArgs) {
        if (!expression) {
            throw new InvalidParameterException(String.format(errorMessageTemplate,
                    errorMessageArgs));
        }
    }
}
