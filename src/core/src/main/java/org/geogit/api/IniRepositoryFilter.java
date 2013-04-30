/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map.Entry;

import org.ini4j.Profile.Section;
import org.ini4j.Wini;

import com.google.common.base.Throwables;

/**
 * Provides a means of loading a RepositoryFilter from an Ini file.
 * 
 * @see RepositoryFilter
 */
public class IniRepositoryFilter extends RepositoryFilter {

    /**
     * Constructs a new {@code IniRepositoryFilter} from the provided file.
     * 
     * @param filterFile the file with the filter definition
     * @throws FileNotFoundException
     */
    public IniRepositoryFilter(final String filterFile) throws FileNotFoundException {
        File f = new File(filterFile);
        if (f.exists()) {
            try {
                final Wini ini = new Wini(f);

                for (Entry<String, Section> section : ini.entrySet()) {
                    if (section.getValue().getParent() == null) {
                        parseFilter(section.getKey(), section.getValue());
                    }
                }
            } catch (Exception e) {
                Throwables.propagate(e);
            }
        } else {
            throw new FileNotFoundException();
        }
    }

    /**
     * Parses an ini section and adds it as a filter.
     * 
     * @param featureType the feature type
     * @param attributes the ini section
     */
    private void parseFilter(String featureType, Section attributes) {
        if (featureType != null) {
            String type = attributes.get("type");
            String filter = attributes.get("filter");
            addFilter(featureType, type, filter);
        }
    }
}
