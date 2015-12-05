/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util.ipactable;

import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.util.*;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.*;

/**
 *
 * @author loi
 * @version $Id: IpacTableParser.java,v 1.18 2011/12/08 19:34:02 loi Exp $
 */
public class JsonTableUtil {

    /**
     * convert data to JSON TableModel. Use toJSONString() to turn it into a String.
     * @param page
     * @param meta
     * @param request
     * @return
     * @throws IOException
     */
    public static JSONObject toJsonTableModel(DataGroupPart page, TableMeta meta, TableServerRequest request) throws IOException {

        if (meta == null) {
            meta = new TableMeta("unknown");
        }
        if (page.getTableDef().getAttributes() != null) {
            // merge tableDef's attributes with tablemeta's.
            for (DataGroup.Attribute attr : page.getTableDef().getAttributes()) {
                // meta override tabledef's.
                if (!StringUtils.isEmpty(attr.getValue()) && !attr.isComment() && !meta.contains(attr.getKey())) {
                    meta.setAttribute(attr.getKey(), attr.getValue());
                }
            }
        }

        String tblId = request == null ? meta.getSource() :
                        request.containsParam(TableServerRequest.TBL_ID) ? request.getParam(TableServerRequest.TBL_ID) :
                        request.getRequestId();

        JSONObject tableModel = new JSONObject();
        tableModel.put("tbl_id", tblId);
        tableModel.put("title", page.getData().getTitle());
        tableModel.put("type", guessType(meta));
        tableModel.put("totalRows", page.getRowCount());

        if (page.getData() != null) {
            tableModel.put("tableData", toJsonTableData(page.getData(), page.getTableDef(), meta));
        }
        if (meta != null) {
            tableModel.put("tableMeta", toJsonTableMeta(meta));
        }
        if (request != null) {
            tableModel.put("request", toJsonTableRequest(request));
        }

        return tableModel;
    }

    /**
     * convert to JSON TableRequest
     * @param req
     * @return
     */
    public static JSONObject toJsonTableRequest(TableServerRequest req) {
        JSONObject treq = new JSONObject();
        treq.put(TableServerRequest.ID_KEY, req.getRequestId());
        treq.put(TableServerRequest.START_IDX, req.getStartIndex());
        treq.put(TableServerRequest.PAGE_SIZE, req.getPageSize());
        if (req.getFilters() != null) {
            treq.put( TableServerRequest.FILTERS, StringUtils.toString(req.getFilters(), ",") );
        }
        if (req.getMeta() != null) {
            for (String key : req.getMeta().keySet()) {
                treq.put( key, req.getMeta().get(key) );
            }
        }
        JSONObject params = new JSONObject();
        for (Param p : req.getParams()) {
            params.put(p.getName(), p.getValue());
        }
        treq.put("params", params);

        return treq;
    }

    /**
     * convert to JSON TableData
     * @param data
     * @param tableDef
     * @return
     */
    public static JSONObject toJsonTableData(DataGroup data, TableDef tableDef, TableMeta meta) {
        List<List<String>> tableData = new ArrayList<List<String>>();
        for (int i = 0; i < data.size(); i ++) {
            List<String> row = new ArrayList<String>();
            for (Object o : data.get(i).getData()) {
                row.add(String.valueOf(o));
            }
            tableData.add(row);
        }

        JSONObject tdata = new JSONObject();
        tdata.put("columns", toJsonTableColumn(tableDef, meta));
        tdata.put("data", tableData);
        return tdata;
    }

    /**
     * convert to JSON TableMeta
     * @param meta
     * @return
     */
    public static JSONObject toJsonTableMeta(TableMeta meta) {
        JSONObject tmeta = new JSONObject();
        tmeta.put("source", meta.getSource());
        tmeta.put("fileSize", meta.getFileSize());
        tmeta.put("isFullyLoaded", meta.isLoaded());
        if (meta.getRelatedCols() != null) {
            tmeta.put("relatedCols", StringUtils.toString(meta.getRelatedCols(), ","));
        }
        if (meta.getGroupByCols() != null) {
            tmeta.put("groupByCols", StringUtils.toString(meta.getGroupByCols(), ","));
        }
        if (meta.getAttributes() != null) {
            for (String key : meta.getAttributes().keySet())  {
                tmeta.put(key, meta.getAttribute(key));
            }
        }
        return tmeta;
    }

//====================================================================
//
//====================================================================

    private static List<JSONObject> toJsonTableColumn(TableDef tableDef, TableMeta meta) {

        ArrayList<JSONObject> cols = new ArrayList<JSONObject>();
        for (DataType dt : tableDef.getCols()) {
            JSONObject c = new JSONObject();
            c.put("name", dt.getKeyName());
            if (!StringUtils.isEmpty(dt.getTypeDesc())) {
                c.put("type", dt.getTypeDesc());
            }
            if (!StringUtils.isEmpty(dt.getDataUnit())) {
                c.put("units", dt.getDataUnit());
            }
            if (!StringUtils.isEmpty(dt.getShortDesc())) {
                c.put("desc", dt.getShortDesc());
            }

            boolean sortable = StringUtils.getBoolean(meta.getAttribute(DataSetParser.makeAttribKey(
                            DataSetParser.SORT_BY_TAG, dt.getKeyName())), true);
            c.put("sortable", sortable);

            String visikey = DataSetParser.makeAttribKey(DataSetParser.SORT_BY_TAG, dt.getKeyName());
            String visibility = meta.contains(visikey) ? meta.getAttribute(visikey) : "show";
            c.put("visibility", visibility);

            String sortByKey = DataSetParser.makeAttribKey(DataSetParser.SORT_BY_TAG, dt.getKeyName());
            if ( meta.contains(sortByKey) ) {
                c.put("sortByCols", StringUtils.asList(meta.getAttribute(sortByKey), ",") );
            }
            String prefWidth = DataSetParser.makeAttribKey(DataSetParser.PREF_WIDTH_TAG, dt.getKeyName());
            if ( meta.contains(prefWidth) ) {
                c.put("prefWidth", meta.getAttribute(prefWidth));
            }
            cols.add(c);
        }
        return cols;
    }


    /**
     * get the type of data this table contains based on its meta information
     * @param meta
     * @return
     */
    private static Object guessType(TableMeta meta) {
        return "table";
    }


}
