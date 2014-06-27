package edu.caltech.ipac.firefly.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Date: 6/12/14
 *
 * @author loi
 * @version $Id: $
 */
public class WspaceMeta implements Serializable {

    public enum Includes {NONE(false,0), NONE_PROPS(true, 0), CHILDREN(false,1), CHILDREN_PROPS(true, 1), ALL(false), ALL_PROPS(true);
        public boolean inclProps = false;
        public int depth = 0;

        Includes(boolean b) {this(b, Integer.MAX_VALUE);}
        Includes(boolean b, int d) {
            inclProps = b;
            depth = d;
        }
    }

    private String wsHome;
    private String relPath;
    private Map<String, String> props;
    private List<WspaceMeta> childNodes;
    private long size = -1;
    private String lastModified;
    private String contentType;

    public WspaceMeta() {}

    public WspaceMeta(String relPath) {
        this(null, relPath, null);
    }

    public WspaceMeta(String home, String relPath) {
        this(home, relPath, null);
    }

    public WspaceMeta(String home, String relPath, Map<String, String> props) {
        setWsHome(home);
        setRelPath(relPath);
        this.props = props;
    }

//====================================================================
//  general info
//====================================================================

    public String getWsHome() {
        return wsHome;
    }

    public void setWsHome(String wsHome) {
        this.wsHome = ensureWsHomePath(wsHome);
    }

    public String getRelPath() {
        return relPath;
    }

    public void setRelPath(String relPath) {
        this.relPath = ensureRelPath(relPath);
    }

    public String getAbsPath() {
        return wsHome + relPath;
    }

    /**
     * returns the name of the file, it's a file type, or "" otherwise.
     * @return
     */
    public String getFileName() {
        int idx = relPath.lastIndexOf("/");
        if (idx < 0) {
            return relPath;
        } else if(idx == relPath.length() -1) {
            return "";
        } else {
            return relPath.substring(idx+1);
        }
    }

    public String getParentPath() {
        String s = relPath;
        if (s.equals("/")) return null;

        if (s.endsWith("/")) {
            s.substring(0, s.length()-1);
        }
        int idx = relPath.lastIndexOf("/");
        if (idx <= 0) {
            return "/";
        } else {
            return relPath.substring(0, idx+1);
        }
    }

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer("-----------------------------\n");
        s.append(wsHome).append(relPath).append("\n");
        s.append("lastModified:").append(lastModified).append("\n");
        s.append("contentType:").append(contentType).append("\n");
        s.append("size:").append(size).append("\n");
        if (getProperties() != null && getProperties().size() > 0) {
            for(String key : getProperties().keySet()) {
                s.append("\t").append(key).append(": ").append(getProperty(key)).append("\n");
            }
        }
        s.append("-----------------------------\n");
        return s.toString();
    }

//====================================================================
//  properties
//====================================================================

    public void setProperties(Map<String, String> props) {
        this.props = props;
    }

    public void setProperty(String keyname, String value) {
        if (props == null) {
            props = new HashMap<String, String>();
        }
        props.put(keyname, value);
    }

    public Map<String, String> getProperties() {
        return props;
    }

    public String getProperty(String keyname) {
        String val = null;
        if (props != null) {
            val = props.get(keyname);
        }
        return val;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getLastModified() {
        return lastModified;
    }

    public long getSize() {
        return size;
    }

    public String getContentType() {
        return contentType;
    }

    //====================================================================
//  nodes graph
//====================================================================

    public boolean hasChildNodes() {
        return childNodes != null && childNodes.size() > 0;
    }

    public List<WspaceMeta> getChildNodes() {
        return childNodes;
    }

    public void setChildNodes(List<WspaceMeta> childNodes) {
        this.childNodes = childNodes;
    }

    public void addChild(WspaceMeta node) {
        if (childNodes == null) {
            childNodes = new ArrayList<WspaceMeta>();
        }
        childNodes.add(node);
    }

    /**
     * return a list of all the nodes under this one in depth first order.
     * @return
     */
    public List<WspaceMeta> getAllNodes() {
        ArrayList<WspaceMeta> retval = new ArrayList<WspaceMeta>();
        addNodesToList(retval, this);
        return retval;
    }

    private void addNodesToList(List<WspaceMeta> inlist, WspaceMeta node) {
        inlist.add(node);
        if (node.hasChildNodes()) {
            for (WspaceMeta child : node.getChildNodes()) {
                addNodesToList(inlist, child);
            }
        }
    }

    public String getNodesAsString() {
        StringBuffer s = new StringBuffer();
        List<WspaceMeta> res = getAllNodes();
        for (WspaceMeta m : res) {
            s.append("\n").append(m.toString());
        }
        return s.toString();
    }

    /**
     * searches this node and all of its descendant to find the
     * first matching path given.
     * @param path
     * @return
     */
    public WspaceMeta find(String path) {
        if (path == null) return null;
        List<WspaceMeta> des = getAllNodes();
        if (des != null) {
            for (WspaceMeta m : des) {
                if (path.equals(m.getRelPath())) {
                    return m;
                }
            }
        }
        return null;
    }

//====================================================================
//
//====================================================================

    /**
     * this make sure the path starts with '/' if not blank, and does not end in '/'.
     */
    public static String ensureWsHomePath(String s) {
        if (s == null || s.equals("/") || s.trim().equals("")) {
            return "";
        }
        s = !s.startsWith("/") ? "/" + s : s;
        s = s.endsWith("/") ? s.substring(0, s.length()-1): s;
        return s;
    }

    /**
     * this make sure the path always starts with '/' .
     */
    public static String ensureRelPath(String s) {
        s = s == null ? "/" : s.trim();
        s = !s.startsWith("/") ? "/" + s : s;
        return s;
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
