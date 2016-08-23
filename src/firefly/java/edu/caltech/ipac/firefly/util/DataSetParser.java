/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.util;

import com.google.gwt.core.client.GWT;
import edu.caltech.ipac.firefly.data.table.BaseTableColumn;
import edu.caltech.ipac.firefly.data.table.BaseTableData;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.visualize.conv.CoordUtil;
import edu.caltech.ipac.util.StringTokenizer;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public static DataSet parse(RawDataSet raw) {
//        GwtUtil.showScrollingDebugMsg("start parsing raw .. ");
        DataSet dataset = new DataSet();
        dataset.setMeta(raw.getMeta());
        dataset.setStartingIdx(raw.getStartingIndex());
        dataset.setTotalRows(raw.getTotalRows());

        if (raw.getDataSetString() == null || raw.getDataSetString().length() == 0) return dataset;

        dataset.setColumns(parseColumns(raw.getDataSetString()));
        dataset.setModel(parseTableModel(dataset, raw.getDataSetString()));

        dataset.getMeta().setAttributes(dataset.getModel().getAttributes());


        String relCols = dataset.getMeta().getAttribute(RELATED_COLS_TAG);
        if (relCols != null && !StringUtils.isEmpty(relCols)) {
            dataset.getMeta().setRelatedCols(StringUtils.asList(relCols, ","));
        }

        String grpByCols = dataset.getMeta().getAttribute(GROUPBY_COLS_TAG);
        if (grpByCols != null && !StringUtils.isEmpty(grpByCols)) {
            dataset.getMeta().setGroupByCols(StringUtils.asList(grpByCols, ","));
        }

        for (TableDataView.Column c : dataset.getColumns()) {
            if ("RA".equals(String.valueOf(c.getUnits())) || "DEC".equals(String.valueOf(c.getUnits()))) {
                c.setWidth(13);
            }
        }

        // modify column's attributes based on table's attributes
        for(TableDataView.Column c : dataset.getColumns() ) {

            Map<String, String> attribs = dataset.getMeta().getAttributes();
            String label = attribs.get( makeAttribKey(LABEL_TAG, c.getName()) );
            if (label != null && !StringUtils.isEmpty(label)) {
                c.setTitle(label);
            }

            String desc = attribs.get( makeAttribKey(DESC_TAG, c.getName()) );
            if (desc != null && !StringUtils.isEmpty(desc)) {
                c.setShortDesc(desc);
            }

            String vis = attribs.get( makeAttribKey(VISI_TAG, c.getName()) );
            if (vis != null) {
                if (vis.equals(VISI_HIDDEN)) {
                    c.setHidden(true);
                    c.setVisible(false);
                } else if (vis.equals(VISI_HIDE)) {
                    c.setHidden(false);
                    c.setVisible(false);
                }
            }

            String width = attribs.get( WIDTH_TAG.replaceAll("@", c.getName()) );
            if (width != null) {
                try {
                    int w =Integer.parseInt(width.trim());
                    c.setWidth(w);
                } catch(NumberFormatException nfe) {
                    //do nothing, use default width
                }
            }

            String prefWidth = attribs.get( PREF_WIDTH_TAG.replaceAll("@", c.getName()) );
            if (prefWidth != null) {
                try {
                    int w =Integer.parseInt(prefWidth.trim());
                    c.setPrefWidth(w);
                } catch(NumberFormatException nfe) {
                    //do nothing, use default width
                }
            }

            String sortable = attribs.get( makeAttribKey(SORTABLE_TAG, c.getName()) );
            if (sortable != null && !Boolean.parseBoolean(sortable)) {
                c.setSortable(false);
            }

            String unit = attribs.get( makeAttribKey(UNIT_TAG, c.getName()) );
            if (unit != null && !StringUtils.isEmpty(unit)) {
                c.setUnits(unit);
            }

            String enumVals = attribs.get( makeAttribKey(ITEMS_TAG, c.getName()) );
            if (enumVals != null && !StringUtils.isEmpty(enumVals)) {
                c.setEnums(enumVals.split(","));
            }

            String sortBy = attribs.get( makeAttribKey(SORT_BY_TAG, c.getName()) );
            if (sortBy != null && !StringUtils.isEmpty(sortBy)) {
                c.setSortByCols(StringUtils.split(sortBy, ","));
            }
        }

        return dataset;
    }

    private static TableData parseTableModel(DataSet dataset, String lines) {

        ArrayList<String[]> data = new ArrayList<String[]>();
        ArrayList<String[]> attribs = new ArrayList<String[]>();

        StringTokenizer tokenizer = new StringTokenizer(lines, "\n");
        while (tokenizer.hasMoreToken()) {
            String s = tokenizer.nextToken();
            if (s.startsWith("\\")) {
                String[] kv = s.substring(1).split("=", 2);
                String[] ktp = StringUtils.trim(kv[0]).split("\\s+", 2);
                String val = kv.length == 1 ? "" : StringUtils.trim(kv[1]);
                String key = ktp.length == 1 ? ktp[0] : ktp[1];
                attribs.add(new String[]{key, val});

            } else if (s.startsWith("|")) {
//                if (columns.size() == 0) {
//                    StringTokenizer cols = new StringTokenizer(s, "|");
//                    while(cols.hasMoreToken()) {
//                        String c = cols.nextToken();
//                        if (!StringUtils.isEmpty(c)) {
//                            columns.add(c.trim());
//                        }
//                    }
//                }
            } else {
                String[] row = getData(dataset.getColumns(), s, true);
                data.add(row);
            }
        }

        ArrayList<String> columns = new ArrayList<String>();
        for(TableDataView.Column col : dataset.getColumns()) {
            columns.add(col.getName());
        }
        
        BaseTableData model = new BaseTableData(columns.toArray(new String[columns.size()]));
        model.setHasAccessCName(dataset.getMeta().getAttribute(TableMeta.HAS_ACCESS_CNAME));
        for(String[] a : attribs) {
            model.setAttribute(a[0], a[1]);
        }

        for(String[] d : data) {
            model.addRow(d);
        }

        return model;
    }

    private static TableDataView.Column[] parseColumns(String lines) {

        ArrayList<TableDataView.Column> columns = new ArrayList<TableDataView.Column>();
        int headerLineIdx = 0;
        StringTokenizer tokenizer = new StringTokenizer(lines, "\n");
        while (tokenizer.hasMoreToken()) {
            String line = tokenizer.nextToken();
//        for (String line : lines) {
            if (line.startsWith("|")) {
                if (headerLineIdx == 0) {       // name
                    StringTokenizer cols = new StringTokenizer(line, "|");
                    while(cols.hasMoreToken()) {
                        String col = cols.nextToken();
                        if (!StringUtils.isEmpty(col)) {
                            BaseTableColumn c = new BaseTableColumn(StringUtils.trim(col));
                            c.setWidth(col.length());
                            columns.add(c);

                            //TODO: remove this code when DB is updated with proper 'format' info
                            if (col.startsWith("raj2000")) {
                                c.setUnits("RA");
                            } else if (col.startsWith("decj2000")) {
                                c.setUnits("DEC");
                            }
                        }
                    }
                } else if (headerLineIdx == 1) { // type
                    int c = -1;
                    StringTokenizer cols = new StringTokenizer(line, "|");
                    while(cols.hasMoreToken()) {
                        String u = cols.nextToken();
                        if (!StringUtils.isEmpty(u)) {
                            columns.get(c).setType(StringUtils.trim(u));
                        }
                        c++;
                    }
                } else if (headerLineIdx == 2) { // units
                    int c = -1;
                    StringTokenizer cols = new StringTokenizer(line, "|");
                    while(cols.hasMoreToken()) {
                        String u = cols.nextToken();
                        if (!StringUtils.isEmpty(u)) {
                            u = StringUtils.trim(u);
                            columns.get(c).setUnits(u);
                            if (u.equalsIgnoreCase("HTML")) {
                                columns.get(c).setSortable(false);
                            }
                        }
                        c++;
                    }
                }
                headerLineIdx++;
            } else if (line.startsWith("\\")) {
            } else {
                String[] data = getData(columns, line, false);
                for(int i = 0; i < columns.size(); i++) {
                    TableDataView.Column c = columns.get(i);
                    String s = data[i];
                    if(s.startsWith(" ") && s.endsWith(" ")) {
                        c.setAlign(TableDataView.Align.CENTER);
                    } else if (s.startsWith(" ")) {
                        c.setAlign(TableDataView.Align.RIGHT);
                    } else {
                        c.setAlign(TableDataView.Align.LEFT);
                    }
                }
                break;
            }
        }

        return columns.toArray(new TableDataView.Column[columns.size()]);
    }

    private static String[] getData(List<TableDataView.Column> columns, String line, boolean doTrim) {
        String[] data = new String[columns.size()];
        int beg, end = 0;
        for(int i = 0; i < columns.size(); i++) {
            TableDataView.Column c = columns.get(i);
            beg = end + 1;
            end = beg + c.getWidth();
            if (end > line.length()) {
                data[i] = line.substring(beg);
            } else {
                data[i] = line.substring(beg, end);
            }
            if (!StringUtils.isEmpty(data[i]) && !StringUtils.isEmpty(c.getUnits())) {
                try {
                    if (c.getUnits().equals("RA")) {
                        double d = Double.parseDouble(data[i].trim());
                        data[i] = CoordUtil.convertLonToString(d, true);
                    } else if (c.getUnits().equals("DEC")) {
                        double d = Double.parseDouble(data[i].trim());
                        data[i] = CoordUtil.convertLatToString(d, true);
                    }
                } catch (Exception e) {
                    GWT.log("error in parsing RA/DEC values:" + data[i], e);
                }
            }
            
            if (doTrim) {
                data[i] = StringUtils.trim(data[i]);
            }
        }
        return data;
    }


}
