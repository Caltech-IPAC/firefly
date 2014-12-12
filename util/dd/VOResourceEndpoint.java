package edu.caltech.ipac.util.dd;

import java.io.Serializable;

/**
 * @author tatianag
 */
public class VOResourceEndpoint implements Serializable {

    String id;
    String title;
    String shortName;
    String url;

    public VOResourceEndpoint () { }

    public VOResourceEndpoint (String id, String title, String shortName, String url) {
        this.id = id;
        this.title = title;
        this.shortName = shortName;
        this.url = url;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }


    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }


    public String getUrl() { return url; }
    public void setUrl(String url) { this.url=url; }

}
