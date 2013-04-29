package org.geogit.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map.Entry;

import org.ini4j.Profile.Section;
import org.ini4j.Wini;

import com.google.common.base.Throwables;

public class IniRepositoryFilter extends RepositoryFilter {

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

    private void parseFilter(String featureType, Section attributes) {
        if (featureType != null) {
            String type = attributes.get("type");
            String filter = attributes.get("filter");
            addFilter(featureType, type, filter);
        }
    }
}
