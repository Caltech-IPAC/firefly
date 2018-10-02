package edu.caltech.ipac.table;

import java.io.Serializable;
import edu.caltech.ipac.util.StringUtils;

/**
 * Object representing the link info., like LINK element under the TABLE element or FIELD element in VOTable
 * Created by cwang on 9/28/18.
 */
public class LinkInfo  implements Serializable, Cloneable {
    private String href;     // LINK href
    private String title;    // LINK title
    private String contentRole;   // LINK content-role
    private String contentType;   // LINK content-type
    private String ID;            // LINK ID

    public LinkInfo(String href, String title) {
        this.href = href;
        this.title = title;
    }

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

    public String getHref() {
        return href;
    }

    public String getTitle() {
        return title;

    }

    public void setID(String id) {
        this.ID = id;
    }

    public String getID() {
        return ID;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    // for output optional key/value
    public static int outKeyValue(int idx, String quote, String key, String value, StringBuffer sb) {
        if (!StringUtils.isEmpty(value)) {
            if (idx++ > 0) {
                sb.append(", ");
            }
            sb.append(quote).append(key).append(quote).append(":\"").append(value).append("\"");
        }
        return idx;
    }

    private String outString(boolean isJson) {
        String q = isJson ? "\"" : "";
        StringBuffer sb = new StringBuffer("{");

        int idx = 0;
        idx = outKeyValue(idx, q, "ID", getID(), sb);
        idx = outKeyValue(idx, q, "href", getHref(), sb);
        idx = outKeyValue(idx, q, "title", getTitle(), sb);
        idx = outKeyValue(idx, q, "contnet-role", getRole(), sb);
        outKeyValue(idx, q, "content-type", getType(), sb);

        sb.append("}");

        return sb.toString();
    }

    public String toJsonString() {
        return outString(true);
    }

    public String toString() {
        return outString(false);

    }

}
