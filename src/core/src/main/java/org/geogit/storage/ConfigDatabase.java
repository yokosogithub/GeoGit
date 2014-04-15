/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.util.List;
import java.util.Map;

import org.geogit.api.porcelain.ConfigException;

import com.google.common.base.Optional;

/**
 * Provides an interface for implementations of config databases, which manage GeoGit config files.
 */
public interface ConfigDatabase {

    /**
     * Queries the repository config file for a particular name.
     * 
     * @param key String in "section.key" format to query for
     * @return The value of the key if found, otherwise an empty Optional
     * @throws ConfigException if an error is encountered
     */
    public Optional<String> get(String key);

    /**
     * Queries the global config file for a particular name.
     * 
     * @param key String in "section.key" format to query for
     * @return The value of the key if found, otherwise an empty Optional
     * @throws ConfigException if an error is encountered
     */
    public Optional<String> getGlobal(String key);

    /**
     * Queries the repository config file for a particular name.
     * 
     * @param key String in "section.key" format to query for
     * @param c The type to return the value as
     * @return The value of the key if found, otherwise an empty Optional
     * @throws IllegalArgumentException if unable to return value as type c
     * @throws ConfigException if an error is encountered
     */
    public <T> Optional<T> get(String key, Class<T> c);

    /**
     * Queries the global config file for a particular name.
     * 
     * @param key String in "section.key" format to query for
     * @param c The type to return the value as
     * @return The value of the key if found, otherwise an empty Optional
     * @throws IllegalArgumentException if unable to return value as type c
     * @throws ConfigException if an error is encountered
     */
    public <T> Optional<T> getGlobal(String key, Class<T> c);

    /**
     * Builds and returns a map with all of the values from the repository config file.
     * 
     * @return A map which contains all of the contents of the config file.
     * @throws ConfigException if an error is encountered
     */
    public Map<String, String> getAll();

    /**
     * Builds and returns a map with all of the values from the global config file.
     * 
     * @return A map which contains all of the contents of the config file.
     * @throws ConfigException if an error is encountered
     */
    public Map<String, String> getAllGlobal();

    /**
     * Builds and returns a map with all of the values from the section in the repository config
     * file.
     * 
     * @return A map which contains all of the contents of the given section.
     * @throws ConfigException if an error is encountered
     */
    public Map<String, String> getAllSection(String section);

    /**
     * Builds and returns a map with all of the values from the section in the global config file.
     * 
     * @return A map which contains all of the contents of the given section.
     * @throws ConfigException if an error is encountered
     */
    public Map<String, String> getAllSectionGlobal(String section);

    /**
     * @return A list which contains all of the subsections for a given section in the repository
     *         config file.
     * @throws ConfigException if an error is encountered
     */
    public List<String> getAllSubsections(String section);

    /**
     * @return A list which contains all of the subsections for a given section in the global config
     *         file.
     * @throws ConfigException if an error is encountered
     */
    public List<String> getAllSubsectionsGlobal(String section);

    /**
     * Sets a value in the repository config file
     * 
     * @param key String in "section.key" format to set
     * @param value The value to set
     * @throws ConfigException if an error is encountered
     */
    public void put(String key, Object value);

    /**
     * Sets a value in the global config file
     * 
     * @param key String in "section.key" format to set
     * @param value The value to set
     * @throws ConfigException if an error is encountered
     */
    public void putGlobal(String key, Object value);

    /**
     * Removes a value from the repository config file
     * 
     * @param key String in "section.key" format to set
     * @throws ConfigException if an error is encountered
     */
    public void remove(String key);

    /**
     * Removes a value from the global config file
     * 
     * @param key String in "section.key" format to set
     * @throws ConfigException if an error is encountered
     */
    public void removeGlobal(String key);

    /**
     * Removes a section from the repository config file
     * 
     * @param key String in "section" format to set
     * @throws ConfigException if an error is encountered
     */
    public void removeSection(String key);

    /**
     * Removes a section from the global config file
     * 
     * @param key String in "section" format to set
     * @throws ConfigException if an error is encountered
     */
    public void removeSectionGlobal(String key);

}
