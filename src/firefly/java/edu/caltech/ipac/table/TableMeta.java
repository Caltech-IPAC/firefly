/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
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
public class TableMeta implements Serializable {

    public static final String VISI_TAG = "col.@.visibility";
    public static final String TYPE_TAG = "col.@.type";
    public static final String LABEL_TAG = "col.@.label";
    public static final String WIDTH_TAG = "col.@.width";
    public static final String PREF_WIDTH_TAG = "col.@.prefWidth";
    public static final String SDESC_TAG = "col.@.ShortDescription";        // for backward compatibility only.
    public static final String DESC_TAG = "col.@.desc";
    public static final String NULL_STR_TAG = "col.@.nullString";
    public static final String UNIT_TAG = "col.@.units";
    public static final String FORMAT_TAG = "col.@.format";
    public static final String FORMAT_DISP_TAG = "col.@.fmtDisp";
    public static final String SORTABLE_TAG = "col.@.sortable";
    public static final String FILTERABLE_TAG = "col.@.filterable";
    public static final String FIXED_TAG = "col.@.fixed";
    public static final String SORT_BY_TAG = "col.@.sortByCols";
    public static final String ENUM_VALS_TAG = "col.@.enumVals";
    public static final String PRECISION_TAG = "col.@.precision";
    public static final String UCD_TAG = "col.@.UCD";
    public static final String UTYPE_TAG = "col.@.utype";
    public static final String XTYPE_TAG = "col.@.xtype";
    public static final String REF_TAG = "col.@.ref";
    public static final String MIN_VALUE_TAG = "col.@.minValue";
    public static final String MAX_VALUE_TAG = "col.@.maxValue";
    public static final String VALUE_TAG = "col.@.value";
    public static final String LINKS_TAG = "col.@.links";
    public static final String ARY_SIZE_TAG = "col.@.arraySize";
    public static final String CELL_RENDERER = "col.@.cellRenderer";

    public static final String TBL_RELATED_COLS = "tbl.relatedCols";    // rows where relatedCols are equal will be highlighted in a preset color

    public static final String TBL_RESOURCES = "tbl.resources";
    public static final String TBL_LINKS = "tbl.links";
    public static final String TBL_GROUPS = "tbl.groups";

    public static final String RESULTSET_ID = "resultSetID";            // this meta if exists contains the ID of the resultset returned.
    public static final String RESULTSET_REQ = "resultSetRequest";      // this meta if exists contains the Request used to create this resultset.


    public static final String ID = "ID";
    public static final String REF = "ref";
    public static final String UCD = "ucd";
    public static final String UTYPE = "utype";
    public static final String XTYPE = "xtype";
    public static final String DESC = "description";
    public static final String NAME = "name";
    public static final String DERIVED_FROM = "DERIVED_FROM";

    public static final String DOCLINK = "doclink.url";
    public static final String DOCLINK_DESC = "doclink.desc";
    public static final String DOCLINK_LABEL = "doclink.label";



    private Map<String, DataGroup.Attribute> attributes = new HashMap<>();                      // including keywords and meta added during processing
    private List<DataGroup.Attribute> keywords = new ArrayList<>();                             // meta from the original source

    public static String makeAttribKey(String tag, String colName) {
        return tag.replaceFirst("@", colName);
    }

    public static DataGroup.Attribute makeAttribute(String tag, String colName, String val) {
        return new DataGroup.Attribute(makeAttribKey(tag, colName), val);
    }


    public String getTblId() {
        return getAttribute(TableServerRequest.TBL_ID);
    }

    public void setTblId(String tblId) {
        setAttribute(TableServerRequest.TBL_ID, tblId);
    }

    public TableMeta clone() {
        TableMeta newMeta = new TableMeta();
        newMeta.attributes.putAll(attributes);
        newMeta.keywords.addAll(keywords);
        return newMeta;
    }

    public boolean isEmpty() {
        return attributes.size() == 0 && keywords.size() == 0;
    }

//===============================================================================================================
//  Attributes:  keywords without comments and duplicates, plus any additional attributes added during processing
//===============================================================================================================

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

    public void setAttributes(Map<String, String> metas) {
        attributes.clear();
        if (metas != null) {
            metas.forEach((k,v) -> setAttribute(k, v));
        }
    }

    public String getAttribute(String key, boolean ignoreCase) {
        DataGroup.Attribute val = null;
        if (ignoreCase) {
            String kk = attributes.keySet().stream().filter(k -> k.equalsIgnoreCase(key)).findFirst().orElse(null);
            if (kk != null) val = attributes.get(kk);
        } else {
            val = attributes.get(key);
        }
        return val == null ? null : val.getValue();
    }

    public String getAttribute(String key) {
        return getAttribute(key, false);
    }

    /**
     * @param key   if null, meta will be treated as a comment
     * @param value meta value
     */
    public void setAttribute(String key, String value) {
        DataGroup.Attribute att = new DataGroup.Attribute(key, value, false);
        if (!StringUtils.isEmpty(key)) {
            attributes.put(key, att);
        }
    }

    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    public boolean contains(String key) {
        return attributes.containsKey(key);
    }

//=============================================================================
// Keywords:  list of meta info from source, including duplicates and comments
//=============================================================================

    /**
     * @return all attributes including comments and duplicated keyed values.
     */
    public List<DataGroup.Attribute> getKeywords() {
        return keywords;
    }

    public void addKeyword(String key, String value) {
        DataGroup.Attribute kw = new DataGroup.Attribute(key, value, true);
        keywords.add(kw);
        if (!kw.isComment()) {
            attributes.put(kw.getKey(), kw);
        }
    }

    public void setKeywords(Collection<DataGroup.Attribute> keywords) {
        this.keywords.clear();
        if (keywords != null) {
            keywords.forEach(att -> addKeyword(att.getKey(), att.getValue()));
        }
    }

//====================================================================
//  convenience getter/setter
//====================================================================

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

