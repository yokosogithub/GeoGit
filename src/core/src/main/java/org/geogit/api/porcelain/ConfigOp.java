package org.geogit.api.porcelain;

import java.util.HashMap;
import java.util.Map;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.porcelain.ConfigException.StatusCode;
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
 * @author mfawcett
 * @see ConfigDatabase
 */
public class ConfigOp extends AbstractGeoGitOp<Optional<Map<String, String>>> {

    public enum ConfigAction {
        CONFIG_NO_ACTION, CONFIG_GET, CONFIG_SET, CONFIG_UNSET, CONFIG_REMOVE_SECTION, CONFIG_LIST
    };

    private boolean global;

    private ConfigAction action;

    private String name;

    private String value;

    final private ConfigDatabase config;

    /**
     * @param config where to store the options
     */
    @Inject
    public ConfigOp(ConfigDatabase config) {
        this.config = config;
    }

    /**
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
                if (global) {
                    val = config.getGlobal(name);
                } else {
                    try {
                        val = config.get(name);
                    } catch (Exception e) {
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

            if (global) {
                config.putGlobal(name, value);
            } else {
                config.put(name, value);
            }
            break;
        }
        case CONFIG_UNSET: {
            if (name == null || name.isEmpty())
                throw new ConfigException(StatusCode.SECTION_OR_NAME_NOT_PROVIDED);

            if (global) {
                config.removeGlobal(name);
            } else {
                config.remove(name);
            }
            break;
        }
        case CONFIG_REMOVE_SECTION: {
            if (name == null || name.isEmpty())
                throw new ConfigException(StatusCode.SECTION_OR_NAME_NOT_PROVIDED);

            if (global) {
                config.removeSectionGlobal(name);
            } else {
                config.removeSection(name);
            }
            break;
        }
        case CONFIG_LIST: {
            Optional<Map<String, String>> results;
            if (global) {
                results = config.getAllGlobal();
            } else {
                results = config.getAll();
            }

            return results;
        }
        default:
            throw new ConfigException(StatusCode.OPTION_DOES_N0T_EXIST);
        }

        return Optional.absent();
    }

    public boolean getGlobal() {
        return global;
    }

    public ConfigOp setGlobal(boolean global) {
        this.global = global;
        return this;
    }

    public ConfigAction getAction() {
        return action;
    }

    public ConfigOp setAction(ConfigAction action) {
        this.action = action;
        return this;
    }

    public ConfigOp setName(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return name;
    }

    public ConfigOp setValue(String value) {
        this.value = value;
        return this;
    }

    public String getValue() {
        return value;
    }

}
