/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data;

import edu.caltech.ipac.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Date: Apr 27, 2011
*
* @author loi
* @version $Id: Alert.java,v 1.2 2011/05/02 18:15:37 loi Exp $
*/
public class Alert implements Serializable {
    private String url;
    private String msg;
    private boolean isNew;
    private long lastModDate;

    public Alert() {}

    public Alert(String msg, long lastModDate) {
        this.msg = msg;
        this.lastModDate = lastModDate;
    }

    public Alert(String url, String title, boolean isNew) {
        this.url = url;
        this.msg = title;
        this.isNew = isNew;
    }

    public String getUrl() {
        return url;
    }

    public String getMsg() {
        return msg;
    }

    public boolean isNew() {
        return isNew;
    }

    public long getLastModDate() {
        return lastModDate;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    public void setLastModDate(long lastModDate) {
        this.lastModDate = lastModDate;
    }

    @Override
    public String toString() {
        return String.valueOf(url) + "||" + String.valueOf(msg) + "||" + String.valueOf(isNew);
    }

    public static Alert parse(String s) {
        if (!StringUtils.isEmpty(s)) {
            String[] parts = s.trim().split("\\|\\|", 3);
            String url = parts.length > 0 ? parts[0].trim() : null;
            String title = parts.length > 1 ? parts[1].trim() : "";
            boolean isNew = parts.length > 2 ? Boolean.valueOf(parts[2]) : true;
            return new Alert(url, title, isNew);
        }
        return null;
    }

    public static List<Alert> parseList(String s) {
        if (!StringUtils.isEmpty(s)) {
            List<Alert> l = new ArrayList<Alert>();
            String[] ary = s.split(";");
            if (ary.length > 0) {
                for (String ss : ary) {
                    Alert a = parse(ss);
                    if (a != null) {
                        l.add(a);
                    }
                }
            }
            return l;
        }
        return null;
    }
}
