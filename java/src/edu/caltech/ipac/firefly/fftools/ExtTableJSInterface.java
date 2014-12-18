package edu.caltech.ipac.firefly.fftools;
/**
 * User: roby
 * Date: 7/5/12
 * Time: 9:56 AM
 */


import edu.caltech.ipac.firefly.data.JscriptRequest;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.ui.previews.TableCtx;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class ExtTableJSInterface {

    private static final String ON_ROWHIGHLIGHT_CHANGE = EventHub.ON_ROWHIGHLIGHT_CHANGE.getName();
    private static final String ON_TABLE_SHOW = EventHub.ON_TABLE_SHOW.getName();

    public static void fireExtTableEvent(TableCtx tableCtx,
                                         String evName,
                                         TableData.Row<String>row) {
        tableCtx.setRow(row);
        Name name= null;
        if (evName!=null) {
            if (evName.equals(ON_ROWHIGHLIGHT_CHANGE)) {
                name= EventHub.ON_ROWHIGHLIGHT_CHANGE;
            }
            else if (evName.equals(ON_TABLE_SHOW)) {
                name= EventHub.ON_TABLE_SHOW;
            }
            if (name!=null) {
                FFToolEnv.getHub().getEventManager().fireEvent(
                        new WebEvent(tableCtx, name,tableCtx));
            }
            else {
                FFToolEnv.logDebugMsg("event name: " +evName+ " is not supported, table ID: " + tableCtx.getId());
            }
        }
        else {
            FFToolEnv.logDebugMsg("null event passed to fireExtTableEvent, table ID: " + tableCtx.getId());
        }
    }

    public static void fireExtTableEvent(String id, String evName, TableData.Row<String>row) {

        TableCtx tableCtx= findExternalTable(id);
        if (tableCtx!=null) {
            fireExtTableEvent(tableCtx, evName, row);
        }
        else {
            FFToolEnv.logDebugMsg("Could not find external table with ID: " + id+ ", did you bind it?");
        }
    }


    public static void fireExtTableEvent(String id, String evName, JscriptRequest jsRowEntries) {
        Map<String,String> rowEntries= jsRowEntries.asMap();
        ExternalRow row= new ExternalRow(rowEntries);
        fireExtTableEvent(id,evName, row);
    }

    public static void bindExtTable(String id, boolean hasPreview, boolean hasCoverage) {
        TableCtx t=new TableCtx(id);
        FFToolEnv.getHub().bind(t);
        if (hasPreview)  t.getMeta().put(CommonParams.HAS_PREVIEW_DATA, "true");
        if (hasCoverage) t.getMeta().put(CommonParams.HAS_COVERAGE_DATA, "true");
    }

    public static void addExtTableMeta(String id, String key, String value) {
        TableCtx tableCtx= findExternalTable(id);
        if (tableCtx!=null) {
            tableCtx.getMeta().put(key,value);
        }
        else {
            FFToolEnv.logDebugMsg("Could not find external table with ID: " + id);
        }
    }


//    public static void setExtTableMeta(String id, JscriptRequest jspr) {
//        TableCtx tableCtx= findExternalTable(id);
//        if (tableCtx!=null) {
//            Map<String,String> paramMap= jspr.asMap();
//            tableCtx.setMeta(paramMap);
//        }
//        else {
//            FFToolEnv.logDebugMsg("Could not find external table with ID: " + id);
//        }
//
//    }


    private static TableCtx findExternalTable(String id) {
        TableCtx retval= null;
        if (!StringUtils.isEmpty(id)) {
            for (TableCtx t:FFToolEnv.getHub().getExternalTables()) {
                if (id.equals(t.getId())) {
                    retval= t;
                    break;
                }
            }
        }
        return retval;
    }

//======================================================================
//------------------ Inner Classes -------------------------------------
//======================================================================

    private static class ExternalRow implements TableData.Row<String> {

        private final List<String> keys;
        private final List<String> values;
        private int rowIdx;

        public ExternalRow(List<String> keys, List<String> values) {
            this.values= values;
            this.keys= keys;
        }


        public ExternalRow(Map<String,String> entries) {
            List<String> values= new ArrayList<String>(entries.size());
            List<String> keys= new ArrayList<String>(entries.size());
            for(Map.Entry<String,String> entry : entries.entrySet()) {
                keys.add(entry.getKey());
                values.add(entry.getValue());
            }
            this.keys= keys;
            this.values= values;
        }


        public List<String> getColumnNames() {
            return keys;
        }

        public String getValue(int colIdx) { return values.get(colIdx); }

        public void setValue(int colIdx, String value) { throw new IllegalArgumentException("not implemented"); }

        public String getValue(String colName) {
            String retval= null;
            int idx= keys.indexOf(colName);
            if (idx>-1 && idx<values.size())  retval= values.get(idx);
            return retval;
        }

        public void setValue(String colName, String value) {
            throw new IllegalArgumentException("not implemented");
        }

        public Map<String, String> getValues() {
            Map<String,String> map= new HashMap<String,String>(keys.size()+21);
            for(int  i= 0; i<keys.size(); i++) {
                if (i<values.size()) {
                    map.put(keys.get(i), values.get(i));
                }
                else {
                    map.put(keys.get(i), null);
                }
            }
            return map;
        }

        public int size() { return values.size(); }

        public int getRowIdx() {
            return rowIdx;
        }

        public void setRowIdx(int idx) {
            rowIdx = idx;
        }

        public boolean hasAccess() { return true; }
    }
}

