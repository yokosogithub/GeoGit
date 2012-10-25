package org.geogit.api;

public class Remote {
    private String name;

    private String fetchurl;

    private String pushurl;

    private String fetch;

    public Remote(String name, String fetchurl, String pushurl, String fetch) {
        this.name = name;
        this.fetchurl = fetchurl;
        this.pushurl = pushurl;
        this.fetch = fetch;
    }

    public String getName() {
        return name;
    }

    public String getFetchURL() {
        return fetchurl;
    }

    public String getPushURL() {
        return pushurl;
    }

    public String getFetch() {
        return fetch;
    }
}
