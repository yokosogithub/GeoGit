/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geogit.storage.memory;

import java.util.List;
import java.util.Map;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevObject.TYPE;
import org.geogit.storage.RefDatabase;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 *
 */
public class HeapRefDatabase implements RefDatabase {

    private Map<String, Ref> refs;

    /**
     * 
     * @see org.geogit.storage.RefDatabase#create()
     */
    @Override
    public void create() {
        if (refs == null) {
            refs = Maps.newTreeMap();

            final String headRefName = Ref.HEAD;
            condCreate(headRefName, TYPE.COMMIT);
            final String master = Ref.MASTER;
            condCreate(master, TYPE.COMMIT);
        }
    }

    private void condCreate(final String refName, TYPE type) {
        Ref child = refs.get(refName);
        if (null == child) {
            put(new Ref(refName, ObjectId.NULL, type));
        }
    }

    /**
     * 
     * @see org.geogit.storage.RefDatabase#close()
     */
    @Override
    public void close() {
        if (refs != null) {
            refs.clear();
            refs = null;
        }
    }

    /**
     * @param name
     * @return
     * @see org.geogit.storage.RefDatabase#getRef(java.lang.String)
     */
    @Override
    public Ref getRef(String name) {
        return refs.get(name);
    }

    /**
     * @param prefix
     * @return
     * @see org.geogit.storage.RefDatabase#getRefs(java.lang.String)
     */
    @Override
    public List<Ref> getRefs(String prefix) {
        List<Ref> matches = Lists.newLinkedList();
        for (Ref ref : refs.values()) {
            if (ref.getName().startsWith(prefix)) {
                matches.add(ref);
            }
        }
        return matches;
    }

    /**
     * @param oid
     * @return
     * @see org.geogit.storage.RefDatabase#getRefsPontingTo(org.geogit.api.ObjectId)
     */
    @Override
    public List<Ref> getRefsPontingTo(ObjectId oid) {
        List<Ref> matches = Lists.newLinkedList();
        for (Ref ref : refs.values()) {
            if (ref.getObjectId().equals(oid)) {
                matches.add(ref);
            }
        }
        return matches;
    }

    /**
     * @param ref
     * @return
     * @see org.geogit.storage.RefDatabase#put(org.geogit.api.Ref)
     */
    @Override
    public boolean put(Ref ref) {
        Ref existing = refs.get(ref.getName());
        if (existing != null && existing.equals(ref)) {
            return false;
        }
        refs.put(ref.getName(), ref);
        return true;
    }

}
