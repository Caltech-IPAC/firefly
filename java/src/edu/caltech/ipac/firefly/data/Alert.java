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
    private String title;
    private boolean isNew;
    private long lastModDate;

    public Alert() {}

    public Alert(String url, String title, boolean isNew) {
        this.url = url;
        this.title = title;
        this.isNew = isNew;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
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

    public void setTitle(String title) {
        this.title = title;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    public void setLastModDate(long lastModDate) {
        this.lastModDate = lastModDate;
    }

    @Override
    public String toString() {
        return String.valueOf(url) + "||" + String.valueOf(title) + "||" + String.valueOf(isNew);
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
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/
