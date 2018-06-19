/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Date: Oct 21, 2008
 *
 * @author loi
 * @version $Id: TableMeta.java,v 1.20 2012/11/30 22:19:38 loi Exp $
 */
public class TableMeta implements Serializable {

    public static final String LABEL_TAG = "col.@.Label";
    public static final String VISI_TAG = "col.@.Visibility";
    public static final String WIDTH_TAG = "col.@.Width";
    public static final String PREF_WIDTH_TAG = "col.@.PrefWidth";
    public static final String DESC_TAG = "col.@.ShortDescription";
    public static final String UNIT_TAG = "col.@.Unit";
    public static final String FORMAT_TAG = "col.@.Fmt";     // can be AUTO, NONE or a valid java format string.  defaults to AUTO.
    public static final String FORMAT_DISP_TAG = "col.@.FmtDisp";
    public static final String SORTABLE_TAG = "col.@.Sortable";
    public static final String FILTERABLE_TAG = "col.@.Filterable";
    public static final String ITEMS_TAG = "col.@.Items";
    public static final String SORT_BY_TAG = "col.@.SortByCols";
    public static final String ENUM_VALS_TAG = "col.@.EnumVals";
    public static final String RELATED_COLS_TAG = "col.related";
    public static final String GROUPBY_COLS_TAG = "col.groupby";

    public static final String RESULTSET_ID = "resultSetID";        // this meta if exists contains the ID of the resultset returned.
    public static final String RESULTSET_REQ = "resultSetRequest";      // this meta if exists contains the Request used to create this of the resultset.

    public static final String IS_FULLY_LOADED = "isFullyLoaded";     // do not format data

    /*
      attributes is a key/value map of table meta information.
      keywords is a list of all table meta information including comments and duplicate attribute entries.
     */
    private Map<String, DataGroup.Attribute> attributes = new HashMap<>();
    private List<DataGroup.Attribute> keywords = new ArrayList<>();

    public static String makeAttribKey(String tag, String colName) {
        return tag.replaceFirst("@", colName);
    }

    public void clear() {
        attributes.clear();
        keywords.clear();
    }

    /**
     * Return only the meta that's not a comment.
     * @return all of the meta information excluding comments.
     */
    public Map<String, DataGroup.Attribute> getAttributes() {
        return attributes;
    }

    /**
     * @return Return key/value meta as a list of Attribute.
     */
    public List<DataGroup.Attribute> getAttributeList() {
        return new ArrayList<>(attributes.values());
    }

    /**
     * @return all attributes including comments and duplicated keyed values.
     */
    public List<DataGroup.Attribute> getKeywords() {
        return keywords;
    }

    public void setKeywords(Collection<DataGroup.Attribute> keywords) {
        clear();
        if (keywords != null) {
            keywords.stream()
                    .forEach(att -> setAttribute(att.getKey(), att.getValue()));
        }
    }

    public void setAttributes(Map<String, String> metas) {
        clear();
        metas.entrySet().stream()
                .forEach((e -> setAttribute(e.getKey(), e.getValue())));
    }

    public String getAttribute(String key) {
        DataGroup.Attribute v = attributes.get(key);
        return v == null ? null : v.getValue();
    }

    public boolean contains(String key) {
        return attributes.containsKey(key);
    }


    /**
     * @param key   if null, meta will be treated as a comment
     * @param value meta value
     */
    public void setAttribute(String key, String value) {
        DataGroup.Attribute att = new DataGroup.Attribute(key, value);
        if (!StringUtils.isEmpty(key)) {
            attributes.put(key, att);
        }
        keywords.add(att);
    }

    public void removeAttribute(String key) {
        DataGroup.Attribute val = attributes.remove(key);
        if (val != null) {
            keywords = keywords.stream()
                        .filter(at -> at.getKey() == null || !at.getKey().equals(key))
                        .collect(Collectors.toList());
        }

    }

    public TableMeta clone() {
        TableMeta newMeta = new TableMeta();
        newMeta.attributes.putAll(attributes);
        return newMeta;
    }

    /**
     * @param key  meta key
     * @return  return a comma-separated values as a list of string
     */
    public List<String> getValueAsList(String key) {
        String cols = getAttribute(key);
        return StringUtils.isEmpty(cols) ? new ArrayList<>() : StringUtils.asList(cols, ",");
    }

    /**
     * @param key meta key
     * @param values  Set a list of string as a comma-separated value
     */
    public void setValueAsList(String key, List<String> values) {
        String val = StringUtils.toString(values, ",");
        setAttribute(key, val);
    }

//====================================================================
//  convenience getter/setter
//====================================================================

    public void setAttribute(String key, Number value) {
    setAttribute(key, value.toString());
}

    public void setWorldPtMeta(String key, WorldPt pt) {
        if (pt != null) setAttribute(key, pt.toString());
    }

    public WorldPt getWorldPtMeta(String key) {
        return WorldPt.parse(getAttribute(key));
    }

    public boolean getBooleanMeta(String key) {
        return StringUtils.getBoolean(getAttribute(key));
    }

    public int getIntMeta(String key) {
        return StringUtils.getInt(getAttribute(key));
    }

    public double getDoubleMeta(String key) {
        return StringUtils.getDouble(getAttribute(key));
    }

    public float getFloatMeta(String key) {
        return StringUtils.getFloat(getAttribute(key));
    }

    public Date getDateMeta(String key) {
        return StringUtils.getDate(getAttribute(key));
    }


    public List<String> getRelatedCols() { return getValueAsList(RELATED_COLS_TAG); }
    public void setRelatedCols(List<String> relatedCols) {
        setValueAsList(RELATED_COLS_TAG, relatedCols); }

    public List<String> getGroupByCols() { return getValueAsList(GROUPBY_COLS_TAG); }
    public void setGroupByCols(List<String> groupByCols) { setValueAsList(GROUPBY_COLS_TAG, groupByCols); }

    public void setCenterCoordColumns(LonLatColumns centerColumns) {
        setLonLatColumnAttr(MetaConst.CENTER_COLUMN, centerColumns);
    }

    public void setLonLatColumnAttr(String key, LonLatColumns llc) {
        setAttribute(key, llc.toString());
    }

    public LonLatColumns getLonLatColumnAttr(String key) {
        return getLonLatColumnAttr(attributes, key);
    }

    public static LonLatColumns getLonLatColumnAttr(Map<String, DataGroup.Attribute> map, String key) {
        DataGroup.Attribute att = map.get(key);
        return att == null || StringUtils.isEmpty(att.getValue()) ? null : LonLatColumns.parse(att.getValue());
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

    public static LonLatColumns[] getCorners(Map<String, DataGroup.Attribute> map, String key) {
        LonLatColumns retval[] = null;
        DataGroup.Attribute att = map.get(key);
        if (att != null && !StringUtils.isEmpty(att.getValue())) {
            String sAry[] = att.getValue().split(",");
            retval = new LonLatColumns[sAry.length];
            int i = 0;
            for (String s : sAry) {
                retval[i++] = LonLatColumns.parse(s);
            }
        }
        return retval;

    }


//====================================================================
//  convenience inner class dealing with table meta
//====================================================================

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

