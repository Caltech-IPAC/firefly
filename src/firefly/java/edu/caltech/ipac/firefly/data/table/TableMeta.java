/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.table;

import edu.caltech.ipac.util.HandSerialize;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.decimate.DecimateKey;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Date: Oct 21, 2008
 *
 * @author loi
 * @version $Id: TableMeta.java,v 1.20 2012/11/30 22:19:38 loi Exp $
 */
public class TableMeta implements Serializable, HandSerialize {

    private final static String SPLIT_TOKEN = "--TableMeta--";
    private final static String ELEMENT_TOKEN = "--TMElement--";


    public static final String HAS_ACCESS_CNAME = "hasAccessCName";
    public static final String SHOW_UNITS = "show-units";
    public static final String PARAM_SEP = "&";

    private String source;
    private long fileSize;
    private boolean isFullyLoaded;
    private List<String> relatedCols;
    private List<String> groupByCols;
    private Map<String, String> attributes = new HashMap<String, String>();


    public TableMeta() {
        this(null);
    }

    public TableMeta(String source) {
        this(source, null, null);
    }

    public TableMeta(String source, String raColumnName, String decColumnName) {
        this.source = source;
        if (!StringUtils.isEmpty(raColumnName) && !StringUtils.isEmpty(decColumnName)) {
            LonLatColumns center = new LonLatColumns(raColumnName, decColumnName, CoordinateSys.EQ_J2000);
            setCenterCoordColumns(center);
        }
        isFullyLoaded = true;
    }

    public TableMeta clone() {
        TableMeta newMeta = new TableMeta();
        newMeta.source = source;
        newMeta.fileSize = fileSize;
        newMeta.isFullyLoaded = isFullyLoaded;
        newMeta.relatedCols = relatedCols == null ? null : new ArrayList<String>(relatedCols);
        newMeta.groupByCols = groupByCols == null ? null : new ArrayList<String>(groupByCols);
        newMeta.attributes.putAll(attributes);
        return newMeta;
    }

    /**
     * the source of this DataSet.  It could be a file on the server; a url used to create this DataSet, or an
     * identifier known by the server.
     *
     * @return return the identifier
     */
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public List<String> getRelatedCols() {
        if (relatedCols == null) {
            relatedCols = new ArrayList<String>();
        }
        return relatedCols;
    }

    public void setRelatedCols(List<String> relatedCols) {
        this.relatedCols = relatedCols;
    }

    public List<String> getGroupByCols() {
        if (groupByCols == null) {
            groupByCols = new ArrayList<String>();
        }
        return groupByCols;
    }

    public void setGroupByCols(List<String> groupByCols) {
        this.groupByCols = groupByCols;
    }

    public void setCenterCoordColumns(LonLatColumns centerColumns) {
        setLonLatColumnAttr(MetaConst.CENTER_COLUMN, centerColumns);
    }

    public LonLatColumns getCenterCoordColumns() {
        return getLonLatColumnAttr(attributes, MetaConst.CENTER_COLUMN);
    }

    public static LonLatColumns getCenterCoordColumns(Map<String, String> map) {
        return getLonLatColumnAttr(map, MetaConst.CENTER_COLUMN);
    }

    public void setLonLatColumnAttr(String key, LonLatColumns llc) {
        setAttribute(key, llc.toString());
    }

    public LonLatColumns getLonLatColumnAttr(String key) {
        return getLonLatColumnAttr(attributes, key);
    }

    public DecimateKey getDecimateKey() {
        DecimateKey retval= null;
        String keyStr = attributes.get(DecimateKey.DECIMATE_KEY);
        if (keyStr!=null) {
            retval= DecimateKey.parse(keyStr);
        }
        return retval;

    }


    public static LonLatColumns getLonLatColumnAttr(Map<String, String> map, String key) {
        String cStr = map.get(key);
        LonLatColumns retval = null;
        if (cStr != null) {
            retval = LonLatColumns.parse(cStr);
        }
        return retval;
    }


    public void setCorners(LonLatColumns... corners) {
        StringBuffer sb = new StringBuffer(corners.length * 15);
        for (int i = 0; (i < corners.length); i++) {
            sb.append(corners[i].toString());
            if (i < corners.length - 1) sb.append(",");
        }
        setAttribute(MetaConst.ALL_CORNERS, sb.toString());
    }

    public LonLatColumns[] getCorners() {
        return getCorners(attributes, MetaConst.ALL_CORNERS);
    }

    public static LonLatColumns[] getCorners(Map<String, String> map, String key) {
        LonLatColumns retval[] = null;
        String cStr = map.get(key);
        if (cStr != null) {
            String sAry[] = cStr.split(",");
            retval = new LonLatColumns[sAry.length];
            int i = 0;
            for (String s : sAry) {
                retval[i++] = LonLatColumns.parse(s);
            }
        }
        return retval;

    }


    public void setAttributes(Map<String, String> attribs) {
        if (attribs == null) {
            attributes.clear();
        } else {
            attributes.putAll(attribs);
        }
    }

//    public void setAttribute(String key, String value) {
//        setAttribute(key, value);
//    }

    public void setAttribute(String key, Number value) {
        setAttribute(key, value.toString());
    }

    public void setAttribute(String key, String value) {
        attributes.put(key, value);
    }
    public void removeAttribute(String key) {
        attributes.remove(key);
    }


    public void setWorldPtAttribute(String key, WorldPt pt) {
        if (pt != null) attributes.put(key, pt.toString());
    }

    public boolean contains(String key) {
        return attributes.containsKey(key);
    }


    public WorldPt getWorldPtAttribute(String key) {
        return WorldPt.parse(getAttribute(key));
    }


    public boolean getBooleanAttribute(String key) {
        return StringUtils.getBoolean(getAttribute(key));
    }

    public int getIntAttribute(String key) {
        return StringUtils.getInt(getAttribute(key));
    }

    public double getDoubleAttribute(String key) {
        return StringUtils.getDouble(getAttribute(key));
    }

    public float getFloatAttribute(String key) {
        return StringUtils.getFloat(getAttribute(key));
    }

    public Date getDateAttribute(String key) {
        return StringUtils.getDate(getAttribute(key));
    }

    public String getAttribute(String key) {
        return attributes.get(key);
    }


    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public boolean isLoaded() {
        return isFullyLoaded;
    }

    public void setIsLoaded(boolean flag) {
        isFullyLoaded = flag;
    }


    public String serialize() {

        StringBuffer sb = new StringBuffer(500);
        sb.append(source).append(SPLIT_TOKEN);
        sb.append(fileSize).append(SPLIT_TOKEN);
        sb.append(isFullyLoaded).append(SPLIT_TOKEN);


        // relatedCols
        sb.append('[');
        if (relatedCols != null) {
            for (String s : relatedCols) sb.append(s).append(ELEMENT_TOKEN);
        }
        sb.append(']').append(SPLIT_TOKEN);


        // groupByCols
        sb.append('[');
        if (groupByCols != null) {
            for (String s : groupByCols) sb.append(s).append(ELEMENT_TOKEN);
        }
        sb.append(']').append(SPLIT_TOKEN);


        // attributes
        sb.append('[');
        if (attributes != null) {
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                sb.append(entry.getKey()).append(ELEMENT_TOKEN);
                sb.append(entry.getValue()).append(ELEMENT_TOKEN);
            }
        }
        sb.append(']').append(SPLIT_TOKEN);

        return sb.toString();
    }

    public static TableMeta parse(String s) {
        if (s == null) return null;
        String sAry[] = s.split(SPLIT_TOKEN, 7);
        TableMeta retval = null;
        if (sAry.length == 7) {
            try {
                int idx = 0;
                retval = new TableMeta();
                retval.source = sAry[idx].equals("null") ? null : sAry[idx];
                idx++;
                retval.fileSize = Long.parseLong(sAry[idx++]);
                retval.isFullyLoaded = Boolean.parseBoolean(sAry[idx++]);
                retval.relatedCols = StringUtils.parseStringList(sAry[idx++], ELEMENT_TOKEN);
                retval.groupByCols = StringUtils.parseStringList(sAry[idx++], ELEMENT_TOKEN);
                retval.attributes = StringUtils.parseStringMap(sAry[idx++], ELEMENT_TOKEN);
            } catch (NumberFormatException e) {
                retval = null;
            }
        }
        return retval;
    }


    public static class LonLatColumns implements Serializable {
        private String lonCol;
        private String latCol;
        private CoordinateSys csys;


        public LonLatColumns() {
        }

        public LonLatColumns(String lonCol, String latCol) {
            this(lonCol, latCol, CoordinateSys.EQ_J2000);
        }

        public LonLatColumns(String lonCol, String latCol, CoordinateSys csys) {
            this.lonCol = lonCol;
            this.latCol = latCol;
            this.csys = csys;
        }

        public String getLonCol() {
            return lonCol;
        }

        public String getLatCol() {
            return latCol;
        }

        public CoordinateSys getCoordinateSys() {
            return csys;
        }

        public String toString() {
            return lonCol + ";" + latCol + ";" + csys.toString();
        }

        public static LonLatColumns parse(String in) {
            if (in == null) return null;

            LonLatColumns retval = null;
            String s[] = in.split(";", 3);
            if (s.length == 3) {
                retval = new LonLatColumns(s[0], s[1], CoordinateSys.parse(s[2]));
            }
            return retval;
        }
    }
}

