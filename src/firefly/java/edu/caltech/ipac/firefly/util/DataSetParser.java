/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.util;

/**
 * Date: Dec 18, 2008
 *
 * @author loi
 * @version $Id: DataSetParser.java,v 1.18 2011/11/11 20:51:10 loi Exp $
 */
public class DataSetParser {
    public static final String FMT_AUTO = "AUTO";     // guess format from data
    public static final String FMT_NONE = "NONE";     // do not format data

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

    public static final String VISI_SHOW = "show";
    public static final String VISI_HIDE = "hide";
    public static final String VISI_HIDDEN = "hidden";      // for application use only.


    public static String makeAttribKey(String tag, String colName) {
        return tag.replaceFirst("@", colName);
    }



}
