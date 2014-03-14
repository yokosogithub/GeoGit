/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.fs;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.geogit.api.Platform;
import org.geogit.api.plumbing.ResolveGeogitDir;
import org.geogit.api.porcelain.ConfigException;
import org.geogit.api.porcelain.ConfigException.StatusCode;
import org.geogit.storage.ConfigDatabase;

import com.google.common.base.Optional;
import com.google.inject.Inject;

public class IniFileConfigDatabase implements ConfigDatabase {
    private Platform platform;

    private INIFile local;

    private INIFile global;

    @Inject
    public IniFileConfigDatabase(final Platform platform) {
        this.platform = platform;
        this.local = new INIFile() {
            @Override
            public File iniFile() {
                final Optional<URL> url = new ResolveGeogitDir(platform).call();

                if (!url.isPresent()) {
                    throw new ConfigException(StatusCode.INVALID_LOCATION);
                }

                /*
                 * See http://weblogs.java.net/blog/kohsuke/archive/2007/04/how_to_convert.html for
                 * explanation on this idiom.
                 */
                File localConfigFile;
                try {
                    localConfigFile = new File(new File(url.get().toURI()), "config");
                } catch (URISyntaxException e) {
                    localConfigFile = new File(url.get().getPath(), "config");
                }

                return localConfigFile;
            }
        };
        this.global = new INIFile() {
            @Override
            public File iniFile() {
                File home = platform.getUserHome();

                if (home == null) {
                    throw new ConfigException(StatusCode.USERHOME_NOT_SET);
                }

                File globalConfig = new File(home.getPath(), ".geogitconfig");
                try {
                    globalConfig.createNewFile();
                } catch (IOException e) {
                    throw new ConfigException(e, StatusCode.CANNOT_WRITE);
                }
                return globalConfig;
            }
        };
    }

    public Optional<String> get(String key) {
        try {
            String[] parsed = parse(key);
            Optional<String> result = local.get(parsed[0], parsed[1]);
            if (result.isPresent() && result.get().length() > 0) {
                return result;
            } else {
                return Optional.absent();
            }
        } catch (StringIndexOutOfBoundsException e) {
            throw new ConfigException(e, StatusCode.SECTION_OR_KEY_INVALID);
        } catch (IllegalArgumentException e) {
            throw new ConfigException(e, null);
        } catch (IOException e) {
            throw new ConfigException(e, null);
        }
    }

    public <T> Optional<T> get(String key, Class<T> c) {
        Optional<String> text = get(key);
        if (text.isPresent()) {
            return Optional.of(cast(c, text.get()));
        } else {
            return Optional.absent();
        }
    }

    public Optional<String> getGlobal(String key) {
        try {
            String[] parsed = parse(key);
            Optional<String> result = global.get(parsed[0], parsed[1]);
            if (result.isPresent() && result.get().length() > 0) {
                return result;
            } else {
                return Optional.absent();
            }
        } catch (StringIndexOutOfBoundsException e) {
            throw new ConfigException(e, StatusCode.SECTION_OR_KEY_INVALID);
        } catch (IllegalArgumentException e) {
            throw new ConfigException(e, null);
        } catch (IOException e) {
            throw new ConfigException(e, null);
        }
    }

    public <T> Optional<T> getGlobal(String key, Class<T> c) {
        Optional<String> text = getGlobal(key);
        if (text.isPresent()) {
            return Optional.of(cast(c, text.get()));
        } else {
            return Optional.absent();
        }
    }

    public Map<String, String> getAll() {
        try {
            return local.getAll();
        } catch (StringIndexOutOfBoundsException e) {
            throw new ConfigException(e, StatusCode.SECTION_OR_KEY_INVALID);
        } catch (IllegalArgumentException e) {
            throw new ConfigException(e, null);
        } catch (IOException e) {
            throw new ConfigException(e, null);
        }
    }

    public Map<String, String> getAllGlobal() {
        try {
            return global.getAll();
        } catch (StringIndexOutOfBoundsException e) {
            throw new ConfigException(e, StatusCode.SECTION_OR_KEY_INVALID);
        } catch (IllegalArgumentException e) {
            throw new ConfigException(e, null);
        } catch (IOException e) {
            throw new ConfigException(e, null);
        }
    }

    public Map<String, String> getAllSection(String section) {
        try {
            return local.getSection(section);
        } catch (StringIndexOutOfBoundsException e) {
            throw new ConfigException(e, StatusCode.SECTION_OR_KEY_INVALID);
        } catch (IllegalArgumentException e) {
            throw new ConfigException(e, null);
        } catch (IOException e) {
            throw new ConfigException(e, null);
        }
    }

    public Map<String, String> getAllSectionGlobal(String section) {
        try {
            return global.getSection(section);
        } catch (StringIndexOutOfBoundsException e) {
            throw new ConfigException(e, StatusCode.SECTION_OR_KEY_INVALID);
        } catch (IllegalArgumentException e) {
            throw new ConfigException(e, null);
        } catch (IOException e) {
            throw new ConfigException(e, null);
        }
    }

    public List<String> getAllSubsections(String section) {
        try {
            return local.listSubsections(section);
        } catch (StringIndexOutOfBoundsException e) {
            throw new ConfigException(e, StatusCode.SECTION_OR_KEY_INVALID);
        } catch (IllegalArgumentException e) {
            throw new ConfigException(e, null);
        } catch (IOException e) {
            throw new ConfigException(e, null);
        }
    }

    public List<String> getAllSubsectionsGlobal(String section) {
        try {
            return global.listSubsections(section);
        } catch (StringIndexOutOfBoundsException e) {
            throw new ConfigException(e, StatusCode.SECTION_OR_KEY_INVALID);
        } catch (IllegalArgumentException e) {
            throw new ConfigException(e, null);
        } catch (IOException e) {
            throw new ConfigException(e, null);
        }
    }

    public void put(String key, Object value) {
        String[] parsed = parse(key);
        try {
            local.set(parsed[0], parsed[1], stringify(value));
        } catch (StringIndexOutOfBoundsException e) {
            throw new ConfigException(e, StatusCode.SECTION_OR_KEY_INVALID);
        } catch (IllegalArgumentException e) {
            throw new ConfigException(e, null);
        } catch (IOException e) {
            throw new ConfigException(e, null);
        }
    }

    public void putGlobal(String key, Object value) {
        String[] parsed = parse(key);
        try {
            global.set(parsed[0], parsed[1], stringify(value));
        } catch (StringIndexOutOfBoundsException e) {
            throw new ConfigException(e, StatusCode.SECTION_OR_KEY_INVALID);
        } catch (IllegalArgumentException e) {
            throw new ConfigException(e, null);
        } catch (IOException e) {
            throw new ConfigException(e, null);
        }
    }

    public void remove(String key) {
        String[] parsed = parse(key);
        try {
            local.remove(parsed[0], parsed[1]);
        } catch (StringIndexOutOfBoundsException e) {
            throw new ConfigException(e, StatusCode.SECTION_OR_KEY_INVALID);
        } catch (IllegalArgumentException e) {
            throw new ConfigException(e, null);
        } catch (IOException e) {
            throw new ConfigException(e, null);
        }
    }

    public void removeGlobal(String key) {
        String[] parsed = parse(key);
        try {
            global.remove(parsed[0], parsed[1]);
        } catch (StringIndexOutOfBoundsException e) {
            throw new ConfigException(e, StatusCode.SECTION_OR_KEY_INVALID);
        } catch (IllegalArgumentException e) {
            throw new ConfigException(e, null);
        } catch (IOException e) {
            throw new ConfigException(e, null);
        }
    }

    public void removeSection(String key) {
        try {
            local.removeSection(key);
        } catch (NoSuchElementException e) {
            throw new ConfigException(e, StatusCode.MISSING_SECTION);
        } catch (StringIndexOutOfBoundsException e) {
            throw new ConfigException(e, StatusCode.SECTION_OR_KEY_INVALID);
        } catch (IllegalArgumentException e) {
            throw new ConfigException(e, null);
        } catch (IOException e) {
            throw new ConfigException(e, null);
        }
    }

    public void removeSectionGlobal(String key) {
        try {
            global.removeSection(key);
        } catch (NoSuchElementException e) {
            throw new ConfigException(e, StatusCode.MISSING_SECTION);
        } catch (StringIndexOutOfBoundsException e) {
            throw new ConfigException(e, StatusCode.SECTION_OR_KEY_INVALID);
        } catch (IllegalArgumentException e) {
            throw new ConfigException(e, null);
        } catch (IOException e) {
            throw new ConfigException(e, null);
        }
    }

    private <T> T cast(Class<T> c, String s) {
        if (String.class.equals(c)) {
            return (T) s;
        }
        if (c.equals(int.class)) {
            return (T) Integer.valueOf(s);
        }
        if (Boolean.class.equals(c)) {
            return (T) Boolean.valueOf(s);
        }
        throw new IllegalArgumentException("Unsupported type: " + c);
    }

    private String stringify(Object o) {
        return o == null ? "" : o.toString();
    }

    private String[] parse(String qualifiedKey) {
        if (qualifiedKey == null) {
            throw new IllegalArgumentException("Config key may not be null.");
        }
        int splitAt = qualifiedKey.lastIndexOf(".");
        return new String[] { qualifiedKey.substring(0, splitAt),
                qualifiedKey.substring(splitAt + 1) };
    }
}
