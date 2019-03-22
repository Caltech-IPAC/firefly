package edu.caltech.ipac.table;

import java.io.Serializable;
import edu.caltech.ipac.util.StringUtils;

/**
 * Object representing the link info., like LINK element under the TABLE element or FIELD element in VOTable
 * Created by cwang on 9/28/18.
 */
public class LinkInfo  implements Serializable, Cloneable {
    private String ID;            // LINK ID
    private String value;
    private String href;     // LINK href
    private String title;    // LINK title
    private String contentRole;   // LINK content-role
    private String contentType;   // LINK content-type
    private String action;

    public LinkInfo() {}

    public LinkInfo(String ID, String value, String href, String title, String contentRole, String contentType, String action) {
        this.ID = ID;
        this.value = value;
        this.href = href;
        this.title = title;
        this.contentRole = contentRole;
        this.contentType = contentType;
        this.action = action;
    }

    public void setHref(String href) { this.href = href; }
    public String getHref() {
        return href;
    }

    public void setTitle(String title) { this.title = title; }
    public String getTitle() { return title; }

    public void setRole(String role) {
        this.contentRole = role;
    }
    public String getRole() {
        return contentRole;
    }

    public void setType(String type) {
        this.contentType = type;
    }
    public String getType() {
        return contentType;
    }

    public void setID(String id) {
        this.ID = id;
    }
    public String getID() {
        return ID;
    }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action;}

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public String toString() {
        return StringUtils.isEmpty(this.contentType) ? this.href : (this.contentType+", "+this.href);
    }

}
