/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import java.util.HashMap;
import java.util.Map;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.porcelain.ConfigException.StatusCode;
import org.geogit.di.CanRunDuringConflict;
import org.geogit.storage.ConfigDatabase;

import com.google.common.base.Optional;
import com.google.inject.Inject;

/**
 * Get and set repository or global options
 * <p>
 * You can query/set options with this command. The name is actually the section and key separated
 * by a dot.
 * <p>
 * Global options are usually stored in ~/.geogitconfig. Repository options will be stored in
 * repo/.geogit/config
 * 
 * @see ConfigDatabase
 */
@CanRunDuringConflict
public class ConfigOp extends AbstractGeoGitOp<Optional<Map<String, String>>> {

    /**
     * Enumeration of the possible actions of this command.
     */
    public enum ConfigAction {
        CONFIG_NO_ACTION, CONFIG_GET, CONFIG_SET, CONFIG_UNSET, CONFIG_REMOVE_SECTION, CONFIG_LIST
    };

    /**
     * Enumeration of the possible options to pass to config --list command
     * 
     */
    public enum ConfigScope {
        LOCAL, GLOBAL, DEFAULT
    };

    private ConfigScope scope;

    private ConfigAction action;

    private String name;

    private String value;

    final private ConfigDatabase config;

    /**
     * Constructs a new {@code ConfigOp} with the given {@link ConfigDatabase}.
     * 
     * @param config where to store the options
     */
    @Inject
    public ConfigOp(ConfigDatabase config) {
        this.config = config;
    }

    /**
     * Executes the config command with the specified options.
     * 
     * @return Optional<String> if querying for a value, empty Optional if no matching name was
     *         found or if setting a value.
     * @throws ConfigException if an error is encountered. More specific information can be found in
     *         the exception's statusCode.
     */
    @Override
    public Optional<Map<String, String>> call() {
        switch (action) {
        case CONFIG_GET: {
            if (name == null || name.isEmpty())
                throw new ConfigException(StatusCode.SECTION_OR_NAME_NOT_PROVIDED);

            if (value == null || value.isEmpty()) {
                Optional<String> val = Optional.absent();
                if (scope == ConfigScope.GLOBAL) {
                    val = config.getGlobal(name);
                } else {
                    try {
                        val = config.get(name);
                    } catch (ConfigException e) {
                        if (scope == ConfigScope.LOCAL) {
                            throw new ConfigException(e.statusCode);
                        }
                    }

                    // Fallback on global config file if name wasn't found locally
                    if (!val.isPresent()) {
                        val = config.getGlobal(name);
                    }
                }

                if (val.isPresent()) {
                    Map<String, String> resultMap = new HashMap<String, String>();
                    resultMap.put(name, val.get());
                    return Optional.of(resultMap);
                }
            } else {
                throw new ConfigException(StatusCode.TOO_MANY_ARGS);
            }
            break;
        }
        case CONFIG_SET: {
            if (name == null || name.isEmpty())
                throw new ConfigException(StatusCode.SECTION_OR_NAME_NOT_PROVIDED);

            if (scope == ConfigScope.GLOBAL) {
                config.putGlobal(name, value);
            } else {
                config.put(name, value);
            }
            break;
        }
        case CONFIG_UNSET: {
            if (name == null || name.isEmpty())
                throw new ConfigException(StatusCode.SECTION_OR_NAME_NOT_PROVIDED);

            if (scope == ConfigScope.GLOBAL) {
                config.removeGlobal(name);
            } else {
                config.remove(name);
            }
            break;
        }
        case CONFIG_REMOVE_SECTION: {
            if (name == null || name.isEmpty())
                throw new ConfigException(StatusCode.SECTION_OR_NAME_NOT_PROVIDED);

            if (scope == ConfigScope.GLOBAL) {
                config.removeSectionGlobal(name);
            } else {
                config.removeSection(name);
            }
            break;
        }
        case CONFIG_LIST: {
            Map<String, String> results = null;
            if (scope == ConfigScope.LOCAL) {
                results = config.getAll();
            } else {
                results = config.getAllGlobal();
                if (scope == ConfigScope.DEFAULT) {
                    try {
                        Map<String, String> localresults = config.getAll();

                        results.putAll(localresults);

                    } catch (ConfigException e) {

                    }
                }
            }

            return Optional.of(results);
        }
        default:
            throw new ConfigException(StatusCode.OPTION_DOES_NOT_EXIST);
        }

        return Optional.absent();
    }

    /**
     * @param scope if ConfigScope.CONFIG_GLOBAL, config actions will be executed on the global
     *        configuration file. If ConfigScope.CONFIG_LOCAL or ConfigScope.CONFIG_DEFAULT, then
     *        all actions will be done on the config file in the local repository.
     * @return {@code this}
     */
    public ConfigOp setScope(ConfigScope scope) {
        this.scope = scope;
        return this;
    }

    /**
     * @param action the action to execute when the command is called.
     * @return {@code this}
     */
    public ConfigOp setAction(ConfigAction action) {
        this.action = action;
        return this;
    }

    /**
     * @param name the variable name to act on
     * @return {@code this}
     */
    public ConfigOp setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * @param value the value to set
     * @return {@code this}
     */
    public ConfigOp setValue(String value) {
        this.value = value;
        return this;
    }

}
