/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util.ipactable;

import edu.caltech.ipac.firefly.data.DecimateInfo;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.util.*;
import edu.jhu.util.StringUtil;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static edu.caltech.ipac.firefly.util.DataSetParser.*;

/**
 * @author loi
 * @version $Id: IpacTableParser.java,v 1.18 2011/12/08 19:34:02 loi Exp $
 */
public class JsonTableUtil {

    /**
     * convert data to JSON TableModel. Use toJSONString() to turn it into a String.
     *
     * @param page
     * @param request
     * @return
     * @throws IOException
     */
    public static JSONObject toJsonTableModel(DataGroupPart page, TableServerRequest request) throws IOException {

        TableDef meta = page.getTableDef().clone();
        if (request != null && request.getMeta() != null) {
            for (String key : request.getMeta().keySet()) {
                meta.setAttribute(key, request.getMeta(key));
            }
        }

        JSONObject tableModel = new JSONObject();
        if (meta.contains(TableServerRequest.TBL_ID)) {
            tableModel.put("tbl_id",  meta.getAttribute(TableServerRequest.TBL_ID).getValue());
        }
        tableModel.put("title", page.getData().getTitle());
        tableModel.put("type", guessType(meta));
        tableModel.put("totalRows", page.getRowCount());

        if (page.getData() != null ) {
            tableModel.put("tableData", toJsonTableData(page.getData(), page.getTableDef()));
        }
        

        if (meta != null) {
            tableModel.put("tableMeta", toJsonTableMeta(meta));
        }
        if (request != null ){
            tableModel.put("request", toJsonTableRequest(request));
        }

        return tableModel;
    }

    /**
     * convert to JSON TableRequest
     *
     * @param req
     * @return
     */
    public static JSONObject toJsonTableRequest(TableServerRequest req) {
        JSONObject treq = new JSONObject();

        for (Param p : req.getParams()) {
            treq.put(p.getName(), p.getValue());
        }

        treq.put(TableServerRequest.ID_KEY, req.getRequestId());
        treq.put(TableServerRequest.START_IDX, req.getStartIndex());
        treq.put(TableServerRequest.PAGE_SIZE, req.getPageSize());
        if (req.getFilters() != null) {
            treq.put(TableServerRequest.FILTERS, TableServerRequest.toFilterStr(req.getFilters()));
        }
        if (req.getMeta() != null) {
            JSONObject  metaInfo = new JSONObject();
            for (String key : req.getMeta().keySet()) {
                metaInfo.put(key, req.getMeta().get(key));
            }
            treq.put(TableServerRequest.META_INFO, metaInfo);
        }

        return treq;
    }

    /**
     * convert to JSON TableData
     *
     * @param data
     * @param tableDef
     * @return
     */
    public static JSONObject toJsonTableData(DataGroup data, TableDef tableDef) {
        List<List<String>> tableData = new ArrayList<List<String>>();
        for (int i = 0; i < data.size(); i++) {
            List<String> row = new ArrayList<String>();
            for (Object o : data.get(i).getData()) {
                row.add(String.valueOf(o));
            }
            tableData.add(row);
        }

        JSONObject tdata = new JSONObject();

        tdata.put("columns", toJsonTableColumn(data, tableDef));
        tdata.put("data", tableData);
        return tdata;
    }

    /**
     * convert to JSON TableMeta
     *
     * @param meta
     * @return
     */
    public static JSONObject toJsonTableMeta(TableDef meta) {
        JSONObject tmeta = new JSONObject();
        for (DataGroup.Attribute att : meta.getAttributes()) {
            if (!att.isComment()) {
                tmeta.put(att.getKey(), att.getValue());
            }
        }
        return tmeta;
    }

//====================================================================
//
//====================================================================

    private static List<JSONObject> toJsonTableColumn(DataGroup dataGroup, TableDef meta) {

        DataType[] dataTypes = meta.getCols().size() > 0 ? meta.getCols().toArray(new DataType[0]) : dataGroup.getDataDefinitions();

        ArrayList<JSONObject> cols = new ArrayList<JSONObject>();
        for (DataType dt :dataTypes) {
            String cname = dt.getKeyName();
            JSONObject c = new JSONObject();

            c.put("name", cname);
            c.put("width", dt.getFormatInfo().getWidth());
            if (!StringUtils.isEmpty(dt.getTypeDesc())) {
                c.put("type", dt.getTypeDesc());
            }
            if (!StringUtils.isEmpty(dt.getDataUnit())) {
                c.put("units", dt.getDataUnit());
            }
            if (!StringUtils.isEmpty(dt.getShortDesc())) {
                c.put("desc", dt.getShortDesc());
            }

            // modify column's attributes based on meta
            String label = getColAttr(meta, LABEL_TAG, cname);
            if (!StringUtils.isEmpty(label)) {
                c.put("label", label);
            }
            String desc = getColAttr(meta, DESC_TAG, cname);
            if (!StringUtils.isEmpty(desc)) {
                c.put("desc", desc);
            }
            String visibility = getColAttr(meta, VISI_TAG, cname);
            if (!StringUtils.isEmpty(visibility)) {
                c.put("visibility", visibility);
            }
            String width = getColAttr(meta, WIDTH_TAG, cname);
            if (!StringUtils.isEmpty(width)) {
                c.put("width", width);
            }
            String prefWidth = getColAttr(meta, PREF_WIDTH_TAG, cname);
            if (!StringUtils.isEmpty(prefWidth)) {
                c.put("prefWidth", prefWidth);
            }
            String sortable = getColAttr(meta, SORTABLE_TAG, cname);
            if (!StringUtils.isEmpty(sortable)) {
                c.put("sortable", sortable);
            }
            String units = getColAttr(meta, UNIT_TAG, cname);
            if (!StringUtils.isEmpty(units)) {
                c.put("units", units);
            }
            String items = getColAttr(meta, ITEMS_TAG, cname);
            if (!StringUtils.isEmpty(items)) {
                c.put("items", items);
            }
            String sortBy = getColAttr(meta, SORT_BY_TAG, cname);
            if (!StringUtils.isEmpty(sortBy)) {
                c.put("sortByCols", sortBy);
            }
            cols.add(c);
        }
        for (DataGroup.Attribute att :  meta.getAttributes()) {
            // clean up all of the column's attributes since we already set it to the columns
            if (att.getKey().startsWith("col.")) {
                meta.removeAttribute(att.getKey());
            }
        }
        return cols;
    }

    private static String getColAttr(TableDef meta, String tag, String cname) {
        DataGroup.Attribute att = meta.getAttribute(makeAttribKey(tag, cname));
        return (att == null) ? "" : att.getValue();
    }

    /**
     * get the type of data this table contains based on its meta information
     *
     * @param meta
     * @return
     */
    private static Object guessType(TableDef meta) {
        return "table";
    }


    //=============================

    //LZ DM-4494
    public static JSONObject toJsonTableModelMap(Map<String, DataGroup> dataMap, TableServerRequest request) throws IOException {
        JSONObject jsoObj = new JSONObject();
        for (Object key : dataMap.keySet()) {
            DataGroupPart dp = QueryUtil.convertToDataGroupPart(dataMap.get(key), 0, Integer.MAX_VALUE);
            JSONObject aJsonTable = JsonTableUtil.toJsonTableModel(dp, request);

            jsoObj.put(key, aJsonTable);

        }
        return jsoObj;
    }

    //============================= //============================= //============================= //============================= //============================= //=============================



}




