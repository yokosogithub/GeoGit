package org.geogit.api;

/**
 * Internal representation of a GeoGit remote repository.
 * 
 * @author jgarrett
 */
public class Remote {
    private String name;

    private String fetchurl;

    private String pushurl;

    private String fetch;

    /**
     * Constructs a new remote with the given parameters.
     * 
     * @param name the name of the remote
     * @param fetchurl the fetch URL of the remote
     * @param pushurl the push URL of the remote
     * @param fetch the fetch string of the remote
     */
    public Remote(String name, String fetchurl, String pushurl, String fetch) {
        this.name = name;
        this.fetchurl = fetchurl;
        this.pushurl = pushurl;
        this.fetch = fetch;
    }

    /**
     * @return the name of the remote
     */
    public String getName() {
        return name;
    }

    /**
     * @return the fetch URL of the remote
     */
    public String getFetchURL() {
        return fetchurl;
    }

    /**
     * @return the push URL of the remote
     */
    public String getPushURL() {
        return pushurl;
    }

    /**
     * @return the fetch string of the remote
     */
    public String getFetch() {
        return fetch;
    }
}
