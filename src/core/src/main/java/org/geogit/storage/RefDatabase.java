/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.util.Map;

/**
 * @author groldan
 * 
 */
public interface RefDatabase {

    public abstract void create();

    public abstract void close();

    /**
     * @param name the name of the ref (e.g. {@code "refs/remotes/origin"}, etc).
     * @return the ref, or {@code null} if it doesn't exist
     */
    public abstract String getRef(String name);

    /**
     * @param name the name of the symbolic ref (e.g. {@code "HEAD"}, etc).
     * @return the ref, or {@code null} if it doesn't exist
     */
    public abstract String getSymRef(String name);

    /**
     * @param ref
     * @return {@code null} if the ref didn't exist already, its old value otherwise
     */
    public abstract String putRef(String refName, String refValue);

    public abstract String putSymRef(String name, String val);

    /**
     * @param refName the name of the ref to remove (e.g. {@code "HEAD"},
     *        {@code "refs/remotes/origin"}, etc).
     * @return the value of the ref before removing it, or {@code null} if it didn't exist
     */
    public abstract String remove(String refName);

    /**
     * @return all known references under the "refs" namespace (i.e. not top level ones like HEAD,
     *         etc), key'ed by ref name
     */
    public abstract Map<String, String> getAll();

}