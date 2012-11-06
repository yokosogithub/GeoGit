package org.geogit.storage.fs;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import org.geogit.api.Platform;
import org.geogit.api.plumbing.ResolveGeogitDir;
import org.geogit.api.porcelain.ConfigException;
import org.geogit.api.porcelain.ConfigException.StatusCode;
import org.geogit.storage.ConfigDatabase;
import org.ini4j.Profile.Section;
import org.ini4j.Wini;

import com.google.common.base.Optional;
import com.google.inject.Inject;

/**
 * Provides a means of managing GeoGit configuration options through the use of an INI file.
 * 
 * @see ConfigDatabase
 */
public class IniConfigDatabase implements ConfigDatabase {

    final private Platform platform;

    /**
     * Constructs a new {@code IniConfigDatabase} with the given platform.
     * 
     * @param platform the platform to use
     */
    @Inject
    public IniConfigDatabase(Platform platform) {
        this.platform = platform;
    }

    private class SectionOptionPair {
        String section;

        String option;

        public SectionOptionPair(String key) {
            final int index = key.indexOf('.');

            if (index == -1) {
                throw new ConfigException(StatusCode.SECTION_OR_KEY_INVALID);
            }

            section = key.substring(0, index);
            option = key.substring(index + 1);

            if (section.length() == 0 || option.length() == 0) {
                throw new ConfigException(StatusCode.SECTION_OR_KEY_INVALID);
            }
        }
    }

    private File config() {
        final URL url = new ResolveGeogitDir(platform).call();

        if (url == null) {
            throw new ConfigException(StatusCode.INVALID_LOCATION);
        }

        /*
         * See http://weblogs.java.net/blog/kohsuke/archive/2007/04/how_to_convert.html for
         * explanation on this idiom.
         */
        File f;
        try {
            f = new File(new File(url.toURI()), "config");
        } catch (URISyntaxException e) {
            f = new File(url.getPath(), "config");
        }

        try {
            f.createNewFile();
        } catch (IOException e) {
            throw new ConfigException(e, StatusCode.CANNOT_WRITE);
        }

        return f;
    }

    private File globalConfig() {
        File home = platform.getUserHome();

        if (home == null) {
            throw new ConfigException(StatusCode.USERHOME_NOT_SET);
        }

        File f = new File(home.getPath(), ".geogitconfig");
        try {
            f.createNewFile();
        } catch (IOException e) {
            throw new ConfigException(e, StatusCode.CANNOT_WRITE);
        }
        return f;
    }

    private <T> Optional<T> get(String key, File file, Class<T> c) {
        if (key == null) {
            throw new ConfigException(StatusCode.SECTION_OR_NAME_NOT_PROVIDED);
        }

        final SectionOptionPair pair = new SectionOptionPair(key);
        try {
            final Wini ini = new Wini(file);
            T value = ini.get(pair.section, pair.option, c);

            if (value == null)
                return Optional.absent();

            if (c == String.class && ((String) value).length() == 0)
                return Optional.absent();

            return Optional.of(value);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigException(e, StatusCode.INVALID_LOCATION);
        }
    }

    private Optional<Map<String, String>> getAll(File file) {
        try {
            final Wini ini = new Wini(file);

            Map<String, String> results = new LinkedHashMap<String, String>();

            for (String sectionName : ini.keySet()) {
                Section section = ini.get(sectionName);
                for (String optionKey : section.keySet()) {
                    results.put(sectionName + "." + optionKey, section.get(optionKey));
                }
            }

            if (results.isEmpty())
                return Optional.absent();

            return Optional.of(results);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigException(e, StatusCode.INVALID_LOCATION);
        }
    }

    private void put(String key, Object value, File file) {
        final SectionOptionPair pair = new SectionOptionPair(key);
        try {
            final Wini ini = new Wini(file);
            ini.put(pair.section, pair.option, value);
            ini.store();
        } catch (Exception e) {
            throw new ConfigException(e, StatusCode.INVALID_LOCATION);
        }
    }

    private void remove(String key, File file) {
        final SectionOptionPair pair = new SectionOptionPair(key);
        try {
            final Wini ini = new Wini(file);
            ini.remove(pair.section, pair.option);
            ini.store();
        } catch (Exception e) {
            throw new ConfigException(e, StatusCode.INVALID_LOCATION);
        }
    }

    private void removeSection(String key, File file) {
        Wini ini;
        try {
            ini = new Wini(file);
        } catch (Exception e) {
            throw new ConfigException(e, StatusCode.INVALID_LOCATION);
        }

        Section sectionToRemove = ini.get(key);

        if (sectionToRemove == null)
            throw new ConfigException(StatusCode.MISSING_SECTION);

        ini.remove(sectionToRemove);

        try {
            ini.store();
        } catch (Exception e) {
            throw new ConfigException(e, StatusCode.INVALID_LOCATION);
        }
    }

    /**
     * Queries the repository config file for a particular name.
     * 
     * @param key String in "section.key" format to query for
     * @return The value of the key if found, otherwise an empty Optional
     * @throws ConfigException if an error is encountered
     */
    @Override
    public Optional<String> get(String key) {
        return get(key, config(), String.class);
    }

    /**
     * Queries the global config file for a particular name.
     * 
     * @param key String in "section.key" format to query for
     * @return The value of the key if found, otherwise an empty Optional
     * @throws ConfigException if an error is encountered
     */
    @Override
    public Optional<String> getGlobal(String key) {
        return get(key, globalConfig(), String.class);
    }

    /**
     * Queries the repository config file for a particular name.
     * 
     * @param key String in "section.key" format to query for
     * @param c The type to return the value as
     * @return The value of the key if found, otherwise an empty Optional
     * @throws IllegalArgumentException if unable to return value as type c
     * @throws ConfigException if an error is encountered
     */
    @Override
    public <T> Optional<T> get(String key, Class<T> c) {
        return get(key, config(), c);
    }

    /**
     * Queries the global config file for a particular name.
     * 
     * @param key String in "section.key" format to query for
     * @param c The type to return the value as
     * @return The value of the key if found, otherwise an empty Optional
     * @throws IllegalArgumentException if unable to return value as type c
     * @throws ConfigException if an error is encountered
     */
    @Override
    public <T> Optional<T> getGlobal(String key, Class<T> c) {
        return get(key, globalConfig(), c);
    }

    /**
     * Builds and returns a string with all of the values from the global config file.
     * 
     * @return A string which contains all of the contents of the config file.
     * @throws ConfigException if an error is encountered
     */
    @Override
    public Optional<Map<String, String>> getAll() {
        return getAll(config());
    }

    /**
     * Builds and returns a string with all of the values from the global config file.
     * 
     * @return A string which contains all of the contents of the config file.
     * @throws ConfigException if an error is encountered
     */
    @Override
    public Optional<Map<String, String>> getAllGlobal() {
        return getAll(globalConfig());
    }

    /**
     * Sets a value in the repository config file, e.g. put("section.key", "value") Resulting config
     * file would look like: [section] key = value
     * 
     * @param key String in "section.key" format to set
     * @param value The value to set
     * @throws ConfigException if an error is encountered
     */
    @Override
    public void put(String key, Object value) {
        put(key, value, config());
    }

    /**
     * Sets a value in the global config file
     * 
     * @param key String in "section.key" format to set
     * @param value The value to set
     * @throws ConfigException if an error is encountered
     */
    @Override
    public void putGlobal(String key, Object value) {
        put(key, value, globalConfig());
    }

    /**
     * Removes a value from the repository config file
     * 
     * @param key String in "section.key" format to set
     * @throws ConfigException if an error is encountered
     */
    @Override
    public void remove(String key) {
        remove(key, config());
    }

    /**
     * Removes a value from the global config file
     * 
     * @param key String in "section.key" format to set
     * @throws ConfigException if an error is encountered
     */
    @Override
    public void removeGlobal(String key) {
        remove(key, globalConfig());
    }

    /**
     * Removes a section from the repository config file
     * 
     * @param key String in "section" format to set
     * @throws ConfigException if an error is encountered
     */
    @Override
    public void removeSection(String key) {
        removeSection(key, config());
    }

    /**
     * Removes a section from the global config file
     * 
     * @param key String in "section" format to set
     * @throws ConfigException if an error is encountered
     */
    @Override
    public void removeSectionGlobal(String key) {
        removeSection(key, globalConfig());
    }

}
