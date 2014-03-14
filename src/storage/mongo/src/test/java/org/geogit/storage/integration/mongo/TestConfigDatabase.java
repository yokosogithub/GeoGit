/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.integration.mongo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geogit.api.Platform;
import org.geogit.storage.ConfigDatabase;
import org.geogit.storage.fs.IniFileConfigDatabase;

import com.google.inject.Inject;
import com.google.common.base.Optional;

public class TestConfigDatabase implements ConfigDatabase {
    private ConfigDatabase delegate;

    private Map<String, String> overrides = new HashMap<String, String>();

    {
        final IniMongoProperties properties = new IniMongoProperties();
        final String uri = properties.get("mongodb.uri", String.class).or("mongodb://localhost:27017/");
        final String database = properties.get("mongodb.database", String.class).or("geogit");
        overrides.put("mongodb.uri", uri);
        overrides.put("mongodb.database", database);
    }

    @Inject
    public TestConfigDatabase(Platform platform) {
        this.delegate = new IniFileConfigDatabase(platform);
    }

    public Optional<String> get(String key) {
        return Optional.fromNullable(overrides.get(key)).or(delegate.get(key));
    }

    public Optional<String> getGlobal(String key) {
        return delegate.getGlobal(key);
    }

    public <T> Optional<T> get(String key, Class<T> c) {
        if (c.equals(String.class)) {
            Optional<String> res = get(key);
            if (res.isPresent()) {
                return Optional.fromNullable((T) res.get());
            } else {
                return Optional.absent();
            }
        }
        if (c.equals(Integer.class)) {
            Optional<String> res = get(key);
            if (res.isPresent()) {
                return Optional.fromNullable((T) Integer.valueOf(res.get()));
            } else {
                return Optional.absent();
            }
        }
        return delegate.get(key, c);
    }

    public <T> Optional<T> getGlobal(String key, Class<T> c) {
        return delegate.getGlobal(key, c);
    }

    public Map<String, String> getAll() {
        Map<String, String> all = delegate.getAll();
        all.putAll(overrides);
        return all;
    }

    public Map<String, String> getAllGlobal() {
        return delegate.getAllGlobal();
    }

    public Map<String, String> getAllSection(String section) {
        Map<String, String> all = delegate.getAll();
        if (section.equals("mongo"))
            all.putAll(overrides);
        return all;
    }

    public Map<String, String> getAllSectionGlobal(String section) {
        return delegate.getAllSectionGlobal(section);
    }

    public List<String> getAllSubsections(String section) {
        List<String> all = delegate.getAllSubsections(section);
        if (!all.contains("mongo"))
            all.add("mongo");
        return all;
    }

    public List<String> getAllSubsectionsGlobal(String section) {
        return delegate.getAllSubsectionsGlobal(section);
    }

    public void put(String key, Object value) {
        delegate.put(key, value);
    }

    public void putGlobal(String key, Object value) {
        delegate.putGlobal(key, value);
    }

    public void remove(String key) {
        delegate.remove(key);
    }

    public void removeGlobal(String key) {
        delegate.removeGlobal(key);
    }

    public void removeSection(String key) {
        delegate.removeSection(key);
    }

    public void removeSectionGlobal(String key) {
        delegate.removeSectionGlobal(key);
    }
}
