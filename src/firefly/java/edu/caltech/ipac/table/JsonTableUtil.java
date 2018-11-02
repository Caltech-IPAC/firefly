/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.util.StringUtils;
import netscape.javascript.JSObject;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static edu.caltech.ipac.util.StringUtils.applyIfNotEmpty;

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

        JSONObject tableModel = toJsonDataGroup(page.getData());
        tableModel.put("totalRows", page.getRowCount());
        if (request != null ){
            tableModel.put("request", toJsonTableRequest(request));
        }

        if (request.getSelectInfo() != null ){
            tableModel.put("selectInfo", request.getSelectInfo().toString());
        }

        if (!StringUtils.isEmpty(page.getErrorMsg())) {
            tableModel.put("error", page.getErrorMsg());
        }
        return tableModel;
    }

    /**
     * Convert the java DataGroup to javascript table model.
     * @param dataGroup
     * @return
     */
    public static JSONObject toJsonDataGroup(DataGroup dataGroup) {

        JSONObject tableModel = new JSONObject();
        TableMeta meta = dataGroup.getTableMeta();

        if (!StringUtils.isEmpty(meta.getAttribute(TableServerRequest.TBL_ID))) {
            tableModel.put("tbl_id",  meta.getAttribute(TableServerRequest.TBL_ID));
        }

        if (!StringUtils.isEmpty(dataGroup.getTitle())) {
            tableModel.put("title", dataGroup.getTitle());
        }

        tableModel.put("type", guessType(meta));
        tableModel.put("tableData", toJsonTableData(dataGroup));

        tableModel.put("allMeta", toJsonTableMeta(meta));

        if (dataGroup.getLinkInfos().size() > 0) {
            tableModel.put("links", toJsonLinkInfos(dataGroup.getLinkInfos()));
        }

        if (dataGroup.getGroupInfos().size() > 0) {
            tableModel.put("groups", toJsonGroupInfos(dataGroup.getGroupInfos()));
        }

        if (dataGroup.getParamInfos().size() > 0) {
            tableModel.put("params", toJsonParamInfos(dataGroup.getParamInfos()));
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
     * @return
     */
    public static JSONObject toJsonTableData(DataGroup data) {

        JSONObject tdata = new JSONObject();

        IpacTableUtil.consumeColumnInfo(data.getDataDefinitions(), data.getTableMeta());

        if (data.getDataDefinitions() != null ) {
            tdata.put("columns", toJsonColumns(data.getDataDefinitions()));
        }

        // clean up all of the column's attributes since we already set it to the columns
        data.getTableMeta().getAttributeList().stream()
                .filter(att -> att.getKey().startsWith("col."))
                .forEach(att -> data.getTableMeta().removeAttribute(att.getKey()));

        if (data.size() > 0) {
            List<List<String>> tableData = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                String[] rowData = data.get(i).getFormatedData();
                tableData.add(Arrays.asList(rowData));
            }
            tdata.put("data", tableData);
        }
        return tdata;
    }

    /**
     * For performance reason, we will send over the list of combined keywords/attributes.
     * tableMeta will be created on the client side to avoid sending duplicates
     *
     * @param tableMeta
     * @return
     */
    public static List<JSONObject> toJsonTableMeta(TableMeta tableMeta) {
        List<JSONObject> allMeta = new ArrayList<>();
        tableMeta.getKeywords().forEach(kw -> allMeta.add(toJsonMetaEntry(kw.getKey(), kw.getValue(), true)));
        tableMeta.getAttributeList().stream()
                .filter(att -> !att.isKeyword())            // only take attributes that is not a keyword.  this will remove all of the keywords we added to attributes.
                .forEach(att -> allMeta.add(toJsonMetaEntry(att.getKey(), att.getValue(), false)));

        return allMeta;
    }


    /**
     * returns the value of the meta info for the given key
     * @param jsonTable     tableModel in the form of a JSONObject
     * @param key
     * @return
     */
    public static String getMetaFromAllMeta(JSONObject jsonTable, String key) {
        List<JSONObject> allMeta = (List<JSONObject>) jsonTable.get("allMeta");
        for (JSONObject m : allMeta) {
            if ( StringUtils.areEqual((String) m.get("key"),key) ) return (String) m.get("value");
        }
        return null;
    }



    /**
     * returns a JSONObject (map) for the given path.
     * This will create new node along the path if one does not exists.
     * Note:  this method may alter your source data.
     * @param source  json object to search
     * @param path
     * @return
     */
    public static JSONObject getPath(JSONObject source, String... path) {
        if (path == null || path.length == 0) {
            return source;
        } else {
            JSONObject current = source;
            for(String p : path) {
                JSONObject next = (JSONObject) current.get(p);
                if (next == null) {
                    next = new JSONObject();
                    current.put(p, next);
                }
                current = next;
            }
            return current;
        }
    }

    /**
     * returns a value for the given path.
     * @param source  json object to search
     * @param path    path to the data
     * @return
     */
    public static Object getPathValue(JSONObject source, String... path) {
        if (path == null || path.length == 0) {
            return source;
        } else {
            Object rval = source;
            try {
                for(int idx = 0; rval !=null && idx < path.length; idx++) {
                    if (rval instanceof Map) {
                        rval = ((Map)rval).get(path[idx]);
                    } else if (rval instanceof List) {
                        rval = ((List)rval).get(Integer.parseInt(path[idx]));
                    } else {
                        rval = null;
                    }
                }
            } catch (Exception e) {
                rval = null;
            }
            return rval;
        }
    }


    //LZ DM-4494
    public static JSONObject toJsonTableModelMap(Map<String, DataGroup> dataMap, TableServerRequest request) throws IOException {
        JSONObject jsoObj = new JSONObject();
        for (Object key : dataMap.keySet()) {
            DataGroup dataGroup = dataMap.get(key);
            DataGroupPart dp = new DataGroupPart(dataGroup, 0, dataGroup.size());
            JSONObject aJsonTable = JsonTableUtil.toJsonTableModel(dp, request);

            jsoObj.put(key, aJsonTable);

        }
        return jsoObj;
    }


//====================================================================
//  private methods
//====================================================================

    /**
     * get the type of data this table contains based on its meta information
     *
     * @param meta
     * @return
     */
    private static Object guessType(TableMeta meta) {
        return "table";
    }

    private static List<JSONObject> toJsonColumns(DataType[] cols) {

        return Arrays.stream(cols).map(dt -> {
            JSONObject c = new JSONObject();

            c.put("name", dt.getKeyName());
            if (dt.getWidth() > 0)
                c.put("width", dt.getWidth());
            if (!StringUtils.isEmpty(dt.getTypeDesc()))
                c.put("type", dt.getTypeDesc());
            if (!StringUtils.isEmpty(dt.getUnits()))
                c.put("units", dt.getUnits());
            if (!StringUtils.isEmpty(dt.getNullString()))
                c.put("nullString", dt.getNullString());
            if (!StringUtils.isEmpty(dt.getDesc()))
                c.put("desc", dt.getDesc());
            if (!StringUtils.isEmpty(dt.getLabel()))
                c.put("label", dt.getLabel());
            if (!StringUtils.isEmpty(dt.getDesc()))
                c.put("desc", dt.getDesc());
            if (dt.getVisibility() != DataType.Visibility.show)
                c.put("visibility", dt.getVisibility().name());
            if (dt.getWidth() > 0)
                c.put("width", dt.getWidth());
            if (dt.getPrefWidth() > 0)
                c.put("prefWidth", dt.getPrefWidth());
            if (!dt.isSortable())
                c.put("sortable", false);
            if (!dt.isFilterable())
                c.put("filterable", false);
            if (!StringUtils.isEmpty(dt.getUnits()))
                c.put("units", dt.getUnits());
            if (!StringUtils.isEmpty(dt.getSortByCols()))
                c.put("sortByCols", dt.getSortByCols());
            if (!StringUtils.isEmpty(dt.getEnumVals()))
                c.put("enumVals", dt.getEnumVals());
            if (!StringUtils.isEmpty(dt.getEnumVals()))
                c.put("enumVals", dt.getEnumVals());

            if (!StringUtils.isEmpty(dt.getID()))
                c.put("ID", dt.getID());
            if (!StringUtils.isEmpty(dt.getPrecision()))
                c.put("precision", dt.getPrecision());
            if (!StringUtils.isEmpty(dt.getUCD()))
                c.put("UCD", dt.getUCD());
            if (!StringUtils.isEmpty(dt.getUType()))
                c.put("utype", dt.getUType());
            if (!StringUtils.isEmpty(dt.getMaxValue()))
                c.put("maxValue", dt.getMaxValue());
            if (!StringUtils.isEmpty(dt.getMinValue()))
                c.put("minValue", dt.getMinValue());
            if (dt.getLinkInfos().size() > 0)
                c.put("links", toJsonLinkInfos(dt.getLinkInfos()));

            if (dt instanceof ParamInfo &&
                 !StringUtils.isEmpty(((ParamInfo) dt).getValue())) {
                c.put("value", ((ParamInfo) dt).getValue());

            }

            return c;
        }).collect(Collectors.toList());
    }

    /**
     * list of Json Object for list of GroupInfo under table model
     * @param groupInfos
     * @return
     */
    private static List<JSONObject> toJsonGroupInfos(List<GroupInfo> groupInfos) {
        List<JSONObject> retval = new ArrayList<>();

        for (GroupInfo gInfo : groupInfos) {
            JSONObject json = new JSONObject();

            applyIfNotEmpty(gInfo.getName(),        v -> json.put("name", v));
            applyIfNotEmpty(gInfo.getDescription(), v -> json.put("desc", v));
            applyIfNotEmpty(gInfo.getID(),          v -> json.put("ID", v));

            if (gInfo.getParamInfos().size() > 0)   json.put("params", toJsonParamInfos(gInfo.getParamInfos()));
            if (gInfo.getParamRefs().size() > 0)    json.put("paramRefs", toJsonRefInfos(gInfo.getParamRefs()));
            if (gInfo.getColumnRefs().size() > 0)   json.put("columnRefs", toJsonRefInfos(gInfo.getColumnRefs()));

            retval.add(json);
        }

        return retval;
    }

    private static List<JSONObject> toJsonRefInfos(List<GroupInfo.RefInfo> refInfos) {

        return refInfos.stream().map(ref -> {
            JSONObject json = new JSONObject();
            applyIfNotEmpty(ref.getRef(),   v -> json.put("ref", v));
            applyIfNotEmpty(ref.getUcd(),   v -> json.put("UCD", v));
            applyIfNotEmpty(ref.getUtype(), v -> json.put("utype", v));
            return json;
        }).collect(Collectors.toList());
    }

    private static List<JSONObject> toJsonParamInfos(List<ParamInfo> paramInfos) {

        List<JSONObject> params = toJsonColumns(paramInfos.toArray(new ParamInfo[0]));

        for (int i = 0; i < paramInfos.size(); i++) {
            String val = paramInfos.get(i).getValue();
            if (val != null)  params.get(i).put("value", paramInfos.get(i).getValue());
        }
        return params;
    }

    /**
     * list of json object for a list of LinkInfo under table model
     * @param linkInfos
     * @return
     */
    private static List<JSONObject> toJsonLinkInfos(List<LinkInfo> linkInfos) {

        return linkInfos.stream().map(link -> {
            JSONObject json = new JSONObject();
            applyIfNotEmpty(link.getID(),       v -> json.put("ID", v));
            applyIfNotEmpty(link.getHref(),     v -> json.put("href", v));
            applyIfNotEmpty(link.getTitle(),    v -> json.put("title", v));
            applyIfNotEmpty(link.getRole(),     v -> json.put("role", v));
            applyIfNotEmpty(link.getType(),     v -> json.put("type", v));
            return json;
        }).collect(Collectors.toList());
    }

    private static JSONObject toJsonMetaEntry(String key, String value, boolean isKeyword) {
        JSONObject entry = new JSONObject();
        entry.put("key", key);
        entry.put("value", value);
        entry.put("isKeyword", isKeyword);
        return entry;
    }

}




