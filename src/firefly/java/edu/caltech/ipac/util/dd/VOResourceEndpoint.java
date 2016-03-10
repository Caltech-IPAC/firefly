/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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
	private String description;

    public VOResourceEndpoint () { }

    public VOResourceEndpoint (String id, String title, String shortName, String url, String desc) {
        this.id = id;
        this.title = title;
        this.shortName = shortName;
        this.url = url;
        this.description = desc;
    }

	public String getId() { return id; }
    public void setId(String id) { this.id = id; }


    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }


    public String getUrl() { return url; }
    public void setUrl(String url) { this.url=url; }

    public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
