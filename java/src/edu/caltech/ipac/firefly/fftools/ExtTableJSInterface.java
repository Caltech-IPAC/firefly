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
import edu.caltech.ipac.firefly.ui.table.TablePreviewEventHub;
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

    private static final String ON_ROWHIGHLIGHT_CHANGE = TablePreviewEventHub.ON_ROWHIGHLIGHT_CHANGE.getName();
    private static final String ON_TABLE_SHOW = TablePreviewEventHub.ON_TABLE_SHOW.getName();

    public static void fireExtTableEvent(TableCtx tableCtx,
                                         String evName,
                                         TableData.Row<String>row) {
        tableCtx.setRow(row);
        Name name= null;
        if (evName!=null) {
            if (evName.equals(ON_ROWHIGHLIGHT_CHANGE)) {
                name= TablePreviewEventHub.ON_ROWHIGHLIGHT_CHANGE;
            }
            else if (evName.equals(ON_TABLE_SHOW)) {
                name= TablePreviewEventHub.ON_TABLE_SHOW;
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
        public boolean hasAccess() { return true; }
    }
}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
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
