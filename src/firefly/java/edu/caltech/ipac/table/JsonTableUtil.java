/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.util.StringUtils;
import org.apache.commons.lang.ArrayUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static edu.caltech.ipac.table.TableMeta.DERIVED_FROM;
import static edu.caltech.ipac.util.StringUtils.*;

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
        tableModel.put("totalRows", page.getRowCount());                // override DataGroup size because it may be just one page of data.
        if (request != null ){
            tableModel.put("request", toJsonTableRequest(request));
        }

        if (request.getSelectInfo() != null ){
            tableModel.put("selectInfo", request.getSelectInfo().toString());
        }

        if (!StringUtils.isEmpty(page.getErrorMsg())) {
            tableModel.put("error", page.getErrorMsg());
        }

        if (page.getData().getHighlightedRow() >= 0) {
            tableModel.put("highlightedRow", page.getData().getHighlightedRow());
        }

        return tableModel;
    }



    public static JSONObject toJsonDataGroup(DataGroup dataGroup) {
        return toJsonDataGroup(dataGroup,false);
    }


    /**
     * Convert the java DataGroup to javascript table model.
     * @param dataGroup
     * @return
     */
    public static JSONObject toJsonDataGroup(DataGroup dataGroup, boolean cleanUpStrings) {

        JSONObject tableModel = new JSONObject();
        TableMeta meta = dataGroup.getTableMeta();

        if (!StringUtils.isEmpty(meta.getAttribute(TableServerRequest.TBL_ID))) {
            tableModel.put("tbl_id",  meta.getAttribute(TableServerRequest.TBL_ID));
        }

        if (!StringUtils.isEmpty(dataGroup.getTitle())) {
            tableModel.put("title", dataGroup.getTitle());
        }

        tableModel.put("type", guessType(meta));
        tableModel.put("tableData", toJsonTableData(dataGroup,cleanUpStrings));

        if (meta.getKeywords().size() > 0) {
            List<JSONObject> keywords = new ArrayList<>();
            meta.getKeywords().forEach(kw -> keywords.add(toJsonMetaEntry(kw.getKey(), kw.getValue())));
            tableModel.put("keywords", keywords);
        }

        if (meta.getAttributes().size() > 0) {
            HashMap<String, String> tableMeta = new HashMap<>();
            meta.getAttributes().forEach((k, v) -> tableMeta.put(k, v.getValue()));
            tableModel.put("tableMeta", tableMeta);
        }

        if (dataGroup.getLinkInfos().size() > 0) {
            tableModel.put("links", toJsonLinkInfos(dataGroup.getLinkInfos()));
        }

        if (dataGroup.getGroupInfos().size() > 0) {
            tableModel.put("groups", toJsonGroupInfos(dataGroup.getGroupInfos()));
        }

        if (dataGroup.getParamInfos().size() > 0) {
            tableModel.put("params", toJsonParamInfos(dataGroup.getParamInfos()));
        }

        if (dataGroup.getResourceInfos().size() > 0) {
            tableModel.put("resources", toJsonResourceInfos(dataGroup.getResourceInfos()));
        }

        tableModel.put("totalRows", dataGroup.size());

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

        applyIfNotEmpty(req.getSqlFilter(), v -> treq.put(TableServerRequest.SQL_FILTER, req.getSqlFilter()));
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
    public static JSONObject toJsonTableData(DataGroup data, boolean cleanUpStrings) {

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
            List<List> tableData = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                Object[] rowData = Arrays.stream(data.get(i).getData()).map(d -> mapToJsonAware(d,cleanUpStrings)).toArray();
                tableData.add(Arrays.asList(rowData));
            }
            tdata.put("data", tableData);
        }
        return tdata;
    }

    /**
     * if obj is an array of primitive, return a list of its equivalent Objects
     * @param obj
     * @return
     */
    private static Object mapToJsonAware(Object obj,boolean cleanUpString) {
        if (obj != null) {
            if (obj.getClass().isArray()) {
                // unbox array into primitive
                switch (obj.getClass().getComponentType().getName()) {
                    case "boolean":
                        obj = Arrays.asList(ArrayUtils.toObject((boolean[]) obj));
                        break;
                    case "byte":
                        obj = Arrays.asList(ArrayUtils.toObject((byte[]) obj));
                        break;
                    case "char":
                        obj = Arrays.asList(ArrayUtils.toObject((char[]) obj));
                        break;
                    case "short":
                        obj = Arrays.asList(ArrayUtils.toObject((short[]) obj));
                        break;
                    case "int":
                        obj = Arrays.asList(ArrayUtils.toObject((int[]) obj));
                        break;
                    case "long":
                        obj = Arrays.asList(ArrayUtils.toObject((long[]) obj));
                        break;
                    case "float":
                        obj = Arrays.asList(ArrayUtils.toObject((float[]) obj));
                        break;
                    case "double":
                        obj = Arrays.asList(ArrayUtils.toObject((double[]) obj));
                        break;
                    default:
                        obj = Arrays.asList((Object[])obj);
                }
            } else {
                // if it's an object we have a mapper for, return the mapper
                JSONAware jsonAware = getJsonMapper(obj);
                if (jsonAware != null) {
                    obj = jsonAware;
                }
                else if ((obj instanceof String) && cleanUpString) {
                    obj= ((String)obj).replaceAll("[^\\x01-\\x7F]", " ") ;
                }
            }
        }
        return obj;
    }

    /**
     * returns the value of the meta info for the given key
     * @param jsonTable     tableModel in the form of a JSONObject
     * @param key
     * @return
     */
    public static String getMetaFromAllMeta(JSONObject jsonTable, String key) {
        HashMap<String,String> tableMeta = (HashMap<String, String>) jsonTable.get("tableMeta");
        if (tableMeta != null) {
            return String.valueOf(tableMeta.get(key));
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

            applyIfNotEmpty(dt.getTypeDesc(),   v -> c.put("type", v));
            applyIfNotEmpty(dt.getUnits(),      v -> c.put("units", v));
            applyIfNotEmpty(dt.getNullString(), v -> c.put("nullString", v));
            applyIfNotEmpty(dt.getDesc(),       v -> c.put("desc", v));
            applyIfNotEmpty(dt.getLabel(),      v -> c.put("label", v));
            applyIfNotEmpty(dt.getArraySize(),  v -> c.put("arraySize", v));
            applyIfNotEmpty(dt.getDerivedFrom(),  v -> c.put(DERIVED_FROM, v));

            if (dt.getVisibility() != DataType.Visibility.show)
                c.put("visibility", dt.getVisibility().name());
            if (dt.getPrefWidth() > 0)
                c.put("prefWidth", dt.getPrefWidth());
            if (!dt.isSortable() || dt.isArrayType())       // disable sorting for data array type
                c.put("sortable", false);
            if (!dt.isFilterable() || dt.isArrayType())     // disable filtering for data array type
                c.put("filterable", false);

            if (dt.isFixed())   c.put("fixed", true);

            applyIfNotEmpty(dt.getSortByCols(), v -> c.put("sortByCols", v));
            applyIfNotEmpty(dt.getEnumVals(), v -> c.put("enumVals", v));
            applyIfNotEmpty(dt.getID(), v -> c.put("ID", v));
            applyIfNotEmpty(dt.getPrecision(), v -> c.put("precision", v));
            applyIfNotEmpty(dt.getUCD(), v -> c.put("UCD", v));
            applyIfNotEmpty(dt.getUType(), v -> c.put("utype", v));
            applyIfNotEmpty(dt.getXType(), v -> c.put("xtype", v));
            applyIfNotEmpty(dt.getRef(), v -> c.put("ref", v));
            applyIfNotEmpty(dt.getMaxValue(), v -> c.put("maxValue", v));
            applyIfNotEmpty(dt.getMinValue(), v -> c.put("minValue", v));
            applyIfNotEmpty(dt.getFormat(), v -> c.put("format", v));
            applyIfNotEmpty(dt.getFmtDisp(), v -> c.put("fmtDisp", v));
            applyIfNotEmpty(dt.getDataOptions(), v -> c.put("options", v));
            applyIfNotEmpty(dt.getCellRenderer(), v -> c.put("cellRenderer", v));


            if (dt.getLinkInfos().size() > 0)
                c.put("links", toJsonLinkInfos(dt.getLinkInfos()));

            if (dt instanceof ParamInfo &&
                    ((ParamInfo) dt).getValue() != null) {
                c.put("value", mapToJsonAware( ((ParamInfo) dt).getValue(), false ));

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
            applyIfNotEmpty(gInfo.getUCD(),          v -> json.put("UCD", v));
            applyIfNotEmpty(gInfo.getUtype(),          v -> json.put("utype", v));

            if (gInfo.getParamInfos().size() > 0)   json.put("params", toJsonParamInfos(gInfo.getParamInfos()));
            if (gInfo.getGroupInfos().size() > 0)   json.put("groups", toJsonGroupInfos(gInfo.getGroupInfos()));
            if (gInfo.getParamRefs().size() > 0)    json.put("paramRefs", toJsonRefInfos(gInfo.getParamRefs()));
            if (gInfo.getColumnRefs().size() > 0)   json.put("columnRefs", toJsonRefInfos(gInfo.getColumnRefs()));

            retval.add(json);
        }

        return retval;
    }

    /**
     * The invert of #toJsonGroupInfos().  This convert the JSON groupInfos string into
     * a List of GroupInfo.
     * @param jsonGroupInfos     the string to parse into GroupInfo
     * @return  a List of GroupInfo
     */
    public static List<GroupInfo> toGroupInfos(String jsonGroupInfos) {

        List<GroupInfo> rval = new ArrayList<>();
        if (!isEmpty(jsonGroupInfos)) {
            JSONArray groups = (JSONArray) JSONValue.parse(jsonGroupInfos);
            for (int i = 0; i < groups.size(); i++) {
                JSONObject param = (JSONObject) groups.get(i);
                GroupInfo groupInfo = new GroupInfo();
                applyIfNotEmpty(param.get("ID"),    v -> groupInfo.setID(v.toString()));
                applyIfNotEmpty(param.get("name"),  v -> groupInfo.setName(v.toString()));
                applyIfNotEmpty(param.get("ucd"), v -> groupInfo.setUCD(v.toString()));
                applyIfNotEmpty(param.get("utype"), v -> groupInfo.setUtype(v.toString()));
                applyIfNotEmpty(param.get("desc"), v -> groupInfo.setDescription(v.toString()));

                applyIfNotEmpty(param.get("params"), v -> groupInfo.setParamInfos(toParamInfos(v.toString())));
                applyIfNotEmpty(param.get("groups"), v -> groupInfo.setGroupInfos(toGroupInfos(v.toString())));
                applyIfNotEmpty(param.get("paramRefs"), v -> groupInfo.setParamRefs(toRefInfos(v.toString())));
                applyIfNotEmpty(param.get("columnRefs"), v -> groupInfo.setColumnRefs(toRefInfos(v.toString())));
                rval.add(groupInfo);
            }
            return rval;
        }
        return null;
    }

    /**
     * The invert of #toJsonParamInfos().  This convert the JSON paramInfos string into
     * a List of ParamInfo.
     * @param jsonRefInfos     the string to parse into RefInfo
     * @return  a List of ParamInfo
     */
    public static List<GroupInfo.RefInfo> toRefInfos(String jsonRefInfos) {

        List<GroupInfo.RefInfo> rval = new ArrayList<>();
        if (!isEmpty(jsonRefInfos)) {
            JSONArray params = (JSONArray) JSONValue.parse(jsonRefInfos);
            for (int i = 0; i < params.size(); i++) {
                JSONObject param = (JSONObject) params.get(i);
                GroupInfo.RefInfo refInfo = new GroupInfo.RefInfo();
                applyIfNotEmpty(param.get("ref"),    v -> refInfo.setRef(v.toString()));
                applyIfNotEmpty(param.get("UCD"),    v -> refInfo.setUcd(v.toString()));
                applyIfNotEmpty(param.get("utype"),  v -> refInfo.setUtype(v.toString()));
                rval.add(refInfo);
            }
            return rval;
        }
        return null;
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
            if (paramInfos.get(i).getValue() != null)   params.get(i).put("value", mapToJsonAware(paramInfos.get(i).getValue(), false));
        }
        return params;
    }

    /**
     * The invert of #toJsonParamInfos().  This convert the JSON paramInfos string into
     * a List of ParamInfo.
     * @param jsonParamInfos     the string to parse into ParamInfo
     * @return  a List of ParamInfo
     */
    public static List<ParamInfo> toParamInfos(String jsonParamInfos) {

        List<ParamInfo> rval = new ArrayList<>();
        if (!isEmpty(jsonParamInfos)) {
            JSONArray params = (JSONArray) JSONValue.parse(jsonParamInfos);
            for (int i = 0; i < params.size(); i++) {
                JSONObject param = (JSONObject) params.get(i);
                ParamInfo paramInfo = new ParamInfo();
                applyIfNotEmpty(param.get("ID"),    v -> paramInfo.setID(v.toString()));
                applyIfNotEmpty(param.get("type"),  v -> paramInfo.setDataType(DataType.descToType(v.toString())));
                applyIfNotEmpty(param.get("unit"),  v -> paramInfo.setUnits(v.toString()));
                applyIfNotEmpty(param.get("precision"), v -> paramInfo.setPrecision(v.toString()));
                applyIfNotEmpty(param.get("width"), v -> paramInfo.setWidth( getInt(v, 0)));
                applyIfNotEmpty(param.get("name"),  v -> paramInfo.setKeyName(v.toString()));
                applyIfNotEmpty(param.get("ucd"),   v -> paramInfo.setUCD(v.toString()));
                applyIfNotEmpty(param.get("utype"), v -> paramInfo.setUType(v.toString()));
                applyIfNotEmpty(param.get("arraysize"), v -> paramInfo.setArraySize(v.toString()));
                applyIfNotEmpty(param.get("ref"), v -> paramInfo.setRef(v.toString()));
                applyIfNotEmpty(param.get("value"), v -> paramInfo.setValue(v));

                applyIfNotEmpty(param.get("datatype"), v -> {
                    paramInfo.setTypeDesc(v.toString());
                    paramInfo.setDataType(DataType.descToType(v.toString()));
                });
                rval.add(paramInfo);
            }
            return rval;
        }
        return null;
    }

    /**
     * converts ResourceInfo into JSON
     * @param resourceInfos
     * @return
     */
    public static List<JSONObject> toJsonResourceInfos(List<ResourceInfo> resourceInfos) {

        return resourceInfos.stream().map(ri -> {
            JSONObject json = new JSONObject();
            applyIfNotEmpty(ri.getID(),     v -> json.put("ID", v));
            applyIfNotEmpty(ri.getName(),   v -> json.put("name", v));
            applyIfNotEmpty(ri.getType(),   v -> json.put("type", v));
            applyIfNotEmpty(ri.getUtype(),  v -> json.put("utype", v));
            applyIfNotEmpty(ri.getDesc(),  v -> json.put("desc", v));
            if (ri.getGroups().size() > 0) json.put("groups", toJsonGroupInfos(ri.getGroups()));
            if (ri.getParams().size() > 0) json.put("params", toJsonParamInfos(ri.getParams()));
            if (ri.getInfos().size() > 0)  json.put("infos", ri.getInfos());
            return json;
        }).filter(json -> !json.isEmpty()).collect(Collectors.toList());
    }

    /**
     * The invert of #toJsonResourceInfos().  This convert the JSON resourceInfos string into
     * a List of ResourceInfo.
     * @param jsonResourceInfos     the string to parse into ResourceInfo
     * @return  a List of ResourceInfo
     */
    public static List<ResourceInfo> toResourceInfos(String jsonResourceInfos) {

        List<ResourceInfo> rval = new ArrayList<>();
        if (!isEmpty(jsonResourceInfos)) {
            JSONArray resources = (JSONArray) JSONValue.parse(jsonResourceInfos);
            for (int i = 0; i < resources.size(); i++) {
                JSONObject res = (JSONObject) resources.get(i);
                ResourceInfo resourceInfo = new ResourceInfo();
                applyIfNotEmpty(res.get("ID"),    v -> resourceInfo.setID(v.toString()));
                applyIfNotEmpty(res.get("name"),  v -> resourceInfo.setName(v.toString()));
                applyIfNotEmpty(res.get("type"), v -> resourceInfo.setType(v.toString()));
                applyIfNotEmpty(res.get("utype"), v -> resourceInfo.setUtype(v.toString()));
                applyIfNotEmpty(res.get("desc"), v -> resourceInfo.setDesc(v.toString()));

                applyIfNotEmpty(res.get("groups"), v -> resourceInfo.setGroups(toGroupInfos(v.toString())));
                applyIfNotEmpty(res.get("params"), v -> resourceInfo.setParams(toParamInfos(v.toString())));

                rval.add(resourceInfo);
            }
            return rval;
        }
        return null;
    }

    /**
     * converts LinkInfo into JSON
     * @param linkInfos
     * @return
     */
    public static List<JSONObject> toJsonLinkInfos(List<LinkInfo> linkInfos) {

        return linkInfos.stream().map(link -> {
            JSONObject json = new JSONObject();
            applyIfNotEmpty(link.getID(),       v -> json.put("ID", v));
            applyIfNotEmpty(link.getHref(),     v -> json.put("href", v));
            applyIfNotEmpty(link.getTitle(),    v -> json.put("title", v));
            applyIfNotEmpty(link.getValue(),    v -> json.put("value", v));
            applyIfNotEmpty(link.getRole(),     v -> json.put("role", v));
            applyIfNotEmpty(link.getType(),     v -> json.put("type", v));
            applyIfNotEmpty(link.getAction(),   v -> json.put("action", v));
            return json;
        }).collect(Collectors.toList());
    }

    /**
     * The invert of #toJsonLinkInfos().  This convert the JSON linkInfos string into
     * a List of LinkInfo.
     * @param jsonLinkInfos     the string to parse into LinkInfo
     * @return  a List of LinkInfo
     */
    public static List<LinkInfo> toLinkInfos(String jsonLinkInfos) {

        List<LinkInfo> rval = new ArrayList<>();
        if (!isEmpty(jsonLinkInfos)) {
            JSONArray links = (JSONArray) JSONValue.parse(jsonLinkInfos);
            for (int i = 0; i < links.size(); i++) {
                JSONObject li = (JSONObject) links.get(i);
                LinkInfo linkInfo = new LinkInfo();
                applyIfNotEmpty(li.get("ID"),    v -> linkInfo.setID(v.toString()));
                applyIfNotEmpty(li.get("href"),  v -> linkInfo.setHref(v.toString()));
                applyIfNotEmpty(li.get("title"), v -> linkInfo.setTitle(v.toString()));
                applyIfNotEmpty(li.get("value"), v -> linkInfo.setValue(v.toString()));
                applyIfNotEmpty(li.get("role"),  v -> linkInfo.setRole(v.toString()));
                applyIfNotEmpty(li.get("type"),  v -> linkInfo.setType(v.toString()));
                applyIfNotEmpty(li.get("action"), v -> linkInfo.setAction(v.toString()));
                rval.add(linkInfo);
            }
            return rval;
        }
        return null;
    }

    private static JSONObject toJsonMetaEntry(String key, String value) {
        JSONObject entry = new JSONObject();
        entry.put("key", key);
        entry.put("value", value);
        return entry;
    }


    private static final SimpleDateFormat JSON_DATE = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");       // similar to JSON.stringify()
    private static final DateTimeFormatter DATE_LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd");                 // similar to JSON.stringify()
    public static JSONAware getJsonMapper(Object obj) {

        if (obj instanceof java.sql.Date d) {
            return () -> "\"" + d.toLocalDate().format(DATE_LOCAL) + "\"";
        } else if (obj instanceof LocalDate d) {
            return () -> "\"" + d.format(DATE_LOCAL) + "\"";
        } else if (obj instanceof Date) {
            return () -> "\"" + JSON_DATE.format(obj) + "\"";
        }
        return null;
    }


}




