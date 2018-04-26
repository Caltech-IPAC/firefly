/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util;

import edu.caltech.ipac.astro.DataGroupQueryStatement;
import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.astro.net.NedParams;
import edu.caltech.ipac.astro.net.SimbadParams;
import edu.caltech.ipac.astro.net.TargetNetwork;
import edu.caltech.ipac.astro.target.IpacTableTargetsParser;
import edu.caltech.ipac.astro.target.NedAttribute;
import edu.caltech.ipac.astro.target.PositionJ2000;
import edu.caltech.ipac.astro.target.SimbadAttribute;
import edu.caltech.ipac.astro.target.Target;
import edu.caltech.ipac.astro.target.TargetFixedSingle;
import edu.caltech.ipac.astro.target.TargetList;
import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.core.background.PackageProgress;
import edu.caltech.ipac.firefly.data.*;
import edu.caltech.ipac.firefly.data.table.*;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.firefly.server.util.ipactable.JsonTableUtil;
import edu.caltech.ipac.firefly.server.util.ipactable.TableDef;
import edu.caltech.ipac.util.*;
import edu.caltech.ipac.util.decimate.DecimateKey;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

import static edu.caltech.ipac.firefly.core.background.BackgroundStatus.*;
import static edu.caltech.ipac.firefly.core.background.BackgroundStatus.ACTIVE_REQUEST_CNT;
import static edu.caltech.ipac.firefly.core.background.BackgroundStatus.RESPONSE_CNT;
import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_ID;

/**
 * Date: Jul 14, 2008
 *
 * @author loi
 * @version $Id: QueryUtil.java,v 1.32 2012/11/03 02:20:23 tlau Exp $
 */
public class QueryUtil {
    public static final Logger.LoggerImpl LOGGER = Logger.getLogger();

    private static final int DECI_DEF_MAX_POINTS = AppProperties.getIntProperty("decimation.def.max.points", 100000);
    public static final int DECI_ENABLE_SIZE = AppProperties.getIntProperty("decimation.enable.size", 5000);

    public static String makeUrlBase(String url) {

        if (StringUtils.isEmpty(url)) return "";

        if (url.toLowerCase().startsWith("http"))
            return url;
        else {
            return "http://" + url.trim();
        }
    }

    public static DownloadRequest convertToDownloadRequest(String dlReqStr, String searchReqStr, String selInfoStr) {
        DownloadRequest retval = new DownloadRequest(convertToServerRequest(searchReqStr), null, null);
        retval.setSelectionInfo(SelectionInfo.parse(selInfoStr));

        if (!StringUtils.isEmpty(dlReqStr)) {
            try {
                JSONObject jsonReq = (JSONObject) new JSONParser().parse(dlReqStr);
                for (Object key : jsonReq.keySet()) {
                    Object val = jsonReq.get(key);
                    retval.setParam(String.valueOf(key), String.valueOf(val));
                }
            } catch (ParseException e) {
                LOGGER.error(e);
            }
        }
        return retval;
    }

    /**
     * convert a json request to a TableServerRequest
     * @see JsonTableUtil#toJsonTableRequest for the reverse
     * @param searchReqStr
     * @return
     */
    public static TableServerRequest convertToServerRequest(String searchReqStr) {
        TableServerRequest retval = new TableServerRequest();
        if (!StringUtils.isEmpty(searchReqStr)) {
            try {
                JSONObject jsonReq = (JSONObject) new JSONParser().parse(searchReqStr);
                for (Object key : jsonReq.keySet()) {
                    Object val = jsonReq.get(key);
                    String skey = String.valueOf(key);
                    if (skey.equals(TableServerRequest.META_INFO)) {
                        Map meta = (Map) val;
                        for (Object mk : meta.keySet()) {
                            String mkey = String.valueOf(mk);
                            if (mkey.equals(TableServerRequest.SELECT_INFO)) {
                                retval.setSelectInfo(SelectionInfo.parse(String.valueOf(meta.get(mkey))));
                            } else {
                                retval.setMeta(mkey, String.valueOf(meta.get(mk)));
                            }
                        }
                    } else {
                        retval.setTrueParam(skey, String.valueOf(val));
                    }

                }
            } catch (ParseException e) {
                LOGGER.error(e);
            }
        }
        if (retval.getTblId() == null) {
            retval.setTblId(retval.getParam(TBL_ID));
        }

        return retval;
    }

    public static BackgroundStatus convertToBackgroundStatus(String jsonBgStatus) {
        BackgroundStatus retval = new BackgroundStatus();
        if (!StringUtils.isEmpty(jsonBgStatus)) {
            try {
                JSONObject jsonReq = (JSONObject) new JSONParser().parse(jsonBgStatus);
                for (Object key : jsonReq.keySet()) {
                    String skey = String.valueOf(key);
                    Object val = jsonReq.get(key);
                    if (val != null) {
                        if (skey.equals(BackgroundStatus.ITEMS.substring(0, BackgroundStatus.ITEMS.length()-1))) {
                            parseBgItems(retval, val.toString());
                        } else {
                            retval.setParam(skey, val.toString());
                        }
                    }
                }
            } catch (ParseException e) {
                LOGGER.error(e);
            }
        }
        return retval;
    }

    public static JSONObject convertToJsonObject(BackgroundStatus bgStat) {
        List<String> intParams = Arrays.asList(MESSAGE_CNT, PACKAGE_CNT, TOTAL_BYTES, RESPONSE_CNT, ACTIVE_REQUEST_CNT);

        JSONObject rval = new JSONObject();
        Map<String, String> params = bgStat.getParams();
        if (params != null && params.size() > 0) {
            for(Map.Entry<String,String> p :  Collections.unmodifiableSet(params.entrySet())) {
                String key = p.getKey();
                Object val = p.getValue();
                if (key.startsWith(ITEMS)) {
                    val = convertToJsonObject(PackageProgress.parse(p.getValue()));
                } else if (intParams.contains(key)) {
                    val = bgStat.getIntParam(key);
                }
                rval.put(key, val);
            }
        }
        return rval;
    }

    public static JSONArray toJsonArray(List values) {
        JSONArray jAry = new JSONArray();
        for (Object v : values) {
            if (v instanceof List) {

            } else if(v instanceof Map) {
                jAry.add(toJsonObject((Map) v));
            } else {
                jAry.add(v);
            }
        }
        return jAry;
    }

    public static String toJsonString(Param ...params) {
        HashMap<String, String> map = new HashMap<>();
        if (params != null) {
            Arrays.stream(params).forEach(p -> map.put(p.getName(), p.getValue()));
        }
        return toJsonString(map);
    }

    public static String toJsonString(Map values) {
        return toJsonObject(values).toJSONString();
    }

    public static JSONObject toJsonObject(Map values) {

        JSONObject jObj = new JSONObject();
        for (Object k : values.keySet()) {
            String name = String.valueOf(k);
            Object v = values.get(k);
            if (v instanceof List) {
                jObj.put(name, toJsonArray((List) v));
            } else if(v instanceof Map) {
                jObj.put(name, toJsonObject((Map) v));
            } else {
                jObj.put(name, v);
            }
        }
        return jObj;
    }

    public static JSONObject convertToJsonObject(PackageProgress progress) {
        JSONObject rval = new JSONObject();
        rval.put("totalFiles", progress.getTotalFiles());
        rval.put("totalBytes", progress.getTotalByes());
        rval.put("processedFiles", progress.getProcessedFiles());
        rval.put("processedBytes", progress.getProcessedBytes());
        rval.put("finalCompressedBytes", progress.getFinalCompressedBytes());
        rval.put("url", progress.getURL());
        return rval;
    }

    private static void parseBgItems(BackgroundStatus retval, String val) throws ParseException {
        JSONArray items = (JSONArray) new JSONParser().parse(val);
        for (int i = 0; i < items.size(); i++) {
            JSONObject item = (JSONObject) items.get(i);
            PackageProgress pp = new PackageProgress(
                    getInt(item.get("totalFiles")),
                    getInt(item.get("processedFiles")),
                    getLong(item.get("totalBytes")),
                    getLong(item.get("processedBytes")),
                    getLong(item.get("finalCompressedBytes")),
                    String.valueOf(item.get("url"))
                );
            retval.setParam(BackgroundStatus.ITEMS+i, pp.serialize());
        }
    }

    public static String encode(String s) {
        if (s == null) return "";
        try {
            return URIUtil.encodeQuery(s);
        } catch (URIException e) {
            return e.getMessage();
        }
    }

    public static String encodeUrl(String url, List<Param> params) {
        String qStr = params == null ? "" : "?" +
                      params.stream()
                              .filter(p -> !StringUtils.isEmpty(p.getName()))
                              .map(p -> p.getName() + "=" + encode(p.getValue()))
                              .collect(Collectors.joining("&"));
        return url + qStr;
    }

    public static void doSort(DataGroup dg, SortInfo sortInfo) {
        if (sortInfo != null) {
            String infoStr = dg.getAttribute(SortInfo.SORT_INFO_TAG) == null ? "" : dg.getAttribute(SortInfo.SORT_INFO_TAG).getValue();
            if (!infoStr.equals(sortInfo.toString())) {
                DataGroupQuery.SortDir sortDir = DataGroupQuery.SortDir.valueOf(sortInfo.getDirection().name());
                DataGroupQuery.sort(dg, sortDir, true, sortInfo.getSortColumnAry());
                dg.addAttribute(SortInfo.SORT_INFO_TAG, sortInfo.toString());
            }
        }
    }

    public static DataGroup doFilter(DataGroup dg, CollectionUtil.Filter<DataObject>... filters) {
        DataGroupQuery query = new DataGroupQuery();
        for(CollectionUtil.Filter<DataObject> f : filters) {
            query.addDataFilters(f);
        }
        dg = query.doQuery(dg);
        dg.addAttribute("filterBy", CollectionUtil.toString(Arrays.asList(filters)));
        return dg;
    }

    /**
     * Converts a list of string into a list of DataGroupQuery.DataFilter using its parseFilter() method.
     * In the process, '!=' is replaced with '!' because internally, DataGroupQuery is using '!' to represent NOT.
     * @param filters
     * @return
     */
    public static CollectionUtil.Filter<DataObject>[] convertToDataFilter(List<String> filters) {
        if (filters == null) return null;

        List<CollectionUtil.Filter<DataObject>> filterList = new ArrayList<CollectionUtil.Filter<DataObject>>();
        for(String cond : filters) {
            CollectionUtil.Filter<DataObject> filter = DataGroupQueryStatement.parseFilter(cond.replaceAll("!=", "!"));
            filterList.add(filter);
        }
        return filterList.toArray(new CollectionUtil.Filter[filterList.size()]);
    }


    public static DataGroupPart convertToDataGroupPart(DataGroup dg, int startIdx, int pageSize) {
        DataGroup page = dg.subset(startIdx, startIdx+pageSize);
        page.setRowIdxOffset(startIdx);
        TableDef tableDef = new TableDef();
        tableDef.addAttributes(dg.getKeywords().toArray(new DataGroup.Attribute[0]));
        tableDef.setStatus(DataGroupPart.State.COMPLETED);
        tableDef.setCols(Arrays.asList(page.getDataDefinitions()));

        return new DataGroupPart(tableDef, dg, startIdx, page.size());
    }

    /**
     * return Float.NaN if val is null or not a float
     * @param val
     * @return
     */
    public static float getFloat(Object val) {
        if (val != null) {
            if (val instanceof Float) {
                return (Float)val;
            } else {
                try {
                    return Float.parseFloat(String.valueOf(val));
                } catch(NumberFormatException ex) {}
            }
        }
        return Float.NaN;

    }

    /**
     * return Double.NaN if val is null or not a double
     * @param val
     * @return
     */
    public static double getDouble(Object val) {
        if (val != null) {
            if (val instanceof Double) {
                return (Double)val;
            } else {
                try {
                    return Double.parseDouble(String.valueOf(val));
                } catch(NumberFormatException ex) {}
            }
        }
        return Double.NaN;

    }

    /**
     * return Integer.MIN_VALUE if val is null or not an integer
     * @param val
     * @return
     */
    public static int getInt(Object val) {
        if (val != null) {
            if (val instanceof Number) {
                return ((Number)val).intValue();
            } else {
                try {
                    return Integer.parseInt(String.valueOf(val));
                } catch(NumberFormatException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return Integer.MIN_VALUE;
    }

    /**
     * return Long.MIN_VALUE if val is null or not an long
     * @param val
     * @return
     */
    public static long getLong(Object val) {
        if (val != null) {
            if (val instanceof Long) {
                return (Long)val;
            } else {
                try {
                    return Long.parseLong(String.valueOf(val));
                } catch(NumberFormatException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return Long.MIN_VALUE;
    }

    public static String test(Double d) {
        return unchecked(d.toString());
    }

    @SuppressWarnings("unchecked") // at your own risk
    public static <T> T unchecked(Object o) {

            return (T) o;

    }

    public static <T extends ServerRequest> T assureType(Class<T> s, ServerRequest req) {
        if(s.isAssignableFrom(req.getClass())) {
            return (T)req;
        }
        try {
            T sreq = s.newInstance();
            sreq.copyFrom(req);
            sreq.setRequestId(req.getRequestId());
            return sreq;
        } catch (InstantiationException e) {
            Logger.error(e);
        } catch (IllegalAccessException e) {
            Logger.error(e);
        }
        return null;
    }

//    /**
//     * Get target list from uploaded file.  It supports tbl, csv, tsv, Spot target list.
//     * @param ufile File to parse for targets
//     * @return List<Target>
//     * @throws DataAccessException
//     * @throws IOException
//     */
    public static List<TargetFixedSingle> getTargetList(File ufile)
            throws DataAccessException, IOException {
        TargetList targetList = new TargetList();
        String parsingErrors = "";
        ArrayList<TargetFixedSingle> targets = new ArrayList<TargetFixedSingle>();
        final String OBJ_NAME = "objname";
        final String RA = "ra";
        final String DEC= "dec";
        String ra = null, dec = null, name = null;
        try {
            DataGroup dg= DataGroupReader.readAnyFormat(ufile);
            if (dg != null) {
                for (DataType dt: dg.getDataDefinitions()) {
                    if (dt.getKeyName().toLowerCase().equals("object") ||
                            dt.getKeyName().toLowerCase().equals("target")) {
                        dt.setKeyName(OBJ_NAME);
                        break;
                    }
                }
                // Find RA and Dec keys
                for (String key: dg.getKeySet()) {
                    if (key.trim().toLowerCase().equals(RA)) {
                        ra=key;
                    } else if (key.trim().toLowerCase().equals(DEC)) {
                        dec=key;
                    } else if (key.trim().toLowerCase().equals(OBJ_NAME)) {
                        name=key;
                    }
                }

                if (name==null) {
                    if (ra==null || dec==null)
                        throw createEndUserException("IPAC Table file must contain RA and Dec columns.");
                }
                IpacTableTargetsParser.parseTargets(dg, targetList, true);
            }
            if (targetList.size()==0) throw createEndUserException("Unable to uploaded file:" + ufile.getName());

            for (Target t: targetList) {
                //check for invalid targets
                if(t.getCoords() == null || t.getCoords().length() < 1){
                    parsingErrors = parsingErrors + "Invalid Target: " + t.getName() + "<br>";
                } else {
                    if (t instanceof TargetFixedSingle) {
                        targets.add((TargetFixedSingle)t);
                    }
                }
            }

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg==null) msg=e.getCause().getMessage();
            if (msg==null) msg="";
            throw new DataAccessException(
                    new EndUserException("Exception while parsing the uploaded file: <br><i>" + msg + "</i>" ,
                               e.getMessage(), e) );
        }
        return targets;
    }

    /**
     * Get target list from uploaded file.  It supports tbl, csv, tsv, Spot target list.
     * File must contains ra and dec columns.  If not, it must contains either objname, object, or target column
     * for ra/dec target name resolution.
     * @param ufile File to parse for targets
     * @return DataGroup containing original uploaded columns, plus ra and dec if not given.
     * @throws DataAccessException
     * @throws IOException
     */
    public static DataGroup getUploadedTargets(File ufile) throws DataAccessException, IOException {
        final String RA = "ra";
        final String DEC= "dec";
        try {
            List<DataType> newCols = new ArrayList<DataType>();
            newCols.add(new DataType(CatalogRequest.UPDLOAD_ROW_ID, Integer.class));
            DataGroup dg= DataGroupReader.readAnyFormat(ufile);
            if (dg == null) {
                throw createEndUserException("Unable to read file:" + ufile.getName());
            }
            String sourceCName = null;

            // standardize the required columns.
            boolean doRowIdIncrement = (dg.containsKey(CatalogRequest.UPDLOAD_ROW_ID));
            for (DataType dt: dg.getDataDefinitions()) {
                if (dt.getKeyName().toLowerCase().matches("object|target|objname")) {
                    sourceCName = dt.getKeyName();
                } else if (dt.getKeyName().toLowerCase().equals(RA)) {
                    dt.setKeyName(RA);
                } else if (dt.getKeyName().toLowerCase().equals(DEC)) {
                    dt.setKeyName(DEC);
                } else if (doRowIdIncrement && dt.getKeyName().startsWith(CatalogRequest.UPDLOAD_ROW_ID)) {
                    dt.setKeyName(getUploadedCName(dt.getKeyName()));
                }
                DataType ndt = (DataType) dt.clone();
                newCols.add(ndt);
            }
            boolean doNameResolve = false;
            if (!dg.containsKey(RA) || !dg.containsKey(DEC)) {
                if (sourceCName != null) {
                    doNameResolve = true;
                    newCols.add(1, new DataType(DEC, Double.class));
                    newCols.add(1, new DataType(RA, Double.class));
                } else {
                    throw createEndUserException("IPAC Table file must contain RA and Dec columns.");
                }
            }

            DataGroup newdg = new DataGroup("sources", newCols);
            for (DataObject row : dg) {
                DataObject nrow = new DataObject(newdg);
                for (DataType dt : newCols) {
                    if (dt.getKeyName().equals(RA) || dt.getKeyName().equals(DEC)) {
                        if (doNameResolve && nrow.getDataElement(dt) == null) {
                            PositionJ2000 pos = null;
                            Object objname = row.getDataElement(sourceCName);
                            NedAttribute na = TargetNetwork.getNedPosition(new NedParams(String.valueOf(objname)));
                            pos = na.getPosition();
                            if (pos == null) {
                                SimbadAttribute sa = TargetNetwork.getSimbadPosition(new SimbadParams(String.valueOf(objname)));
                                pos = sa.getPosition();
                            }
                            if (pos != null) {
                                nrow.setDataElement(newdg.getDataDefintion(RA), pos.getLon());
                                nrow.setDataElement(newdg.getDataDefintion(DEC), pos.getLat());
                            }
                        } else {
                            if (row.containsKey(dt.getKeyName())) {
                                nrow.setDataElement(dt, row.getDataElement(dt.getKeyName()));
                            }
                        }
                    } else if (dt.getKeyName().equals(CatalogRequest.UPDLOAD_ROW_ID)) {
                        nrow.setDataElement(dt, newdg.size() + 1);
                    } else {
                        nrow.setDataElement(dt, row.getDataElement(dt.getKeyName()));
                    }
                }
                newdg.add(nrow);
            }
            newdg.shrinkToFitData(true);
            return newdg;
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg==null) msg=e.getCause().getMessage();
            if (msg==null) msg="";
            throw new DataAccessException(
                    new EndUserException("Exception while parsing the uploaded file: <br><i>" + msg + "</i>" ,
                            e.getMessage(), e) );
        }
    }

    public static DataAccessException createEndUserException(String msg) {
        return new DataAccessException(new EndUserException(msg, msg) );
    }

    public static DataAccessException createEndUserException(String msg, String details) {
        return new DataAccessException(new EndUserException(msg, details) );
    }

    /**
     * returns 4 columns; x-column, y-column, rowidx, weight, decimate_key
     * @param dg input data group
     * @param decimateInfo DecimateInfo object
     * @return decimated data group
     */
    public static DataGroup doDecimation(DataGroup dg, DecimateInfo decimateInfo) throws DataAccessException {

        double xMax = Double.NEGATIVE_INFINITY, xMin = Double.POSITIVE_INFINITY, yMax = Double.NEGATIVE_INFINITY, yMin = Double.POSITIVE_INFINITY;

        DataType [] dataTypes = dg.getDataDefinitions();
        String xColOrExpr = decimateInfo.getxColumnName();
        String yColOrExpr = decimateInfo.getyColumnName();
        DataObjectUtil.DoubleValueGetter xValGetter = new DataObjectUtil.DoubleValueGetter(dataTypes, xColOrExpr);
        DataObjectUtil.DoubleValueGetter yValGetter = new DataObjectUtil.DoubleValueGetter(dataTypes, yColOrExpr);

        if (!xValGetter.isValid() || !yValGetter.isValid()) {
            System.out.println("QueryUtil.doDecimation: invalid x or y column.");
            throw new DataAccessException("Invalid column or expression");
        }
        boolean sameXY = xColOrExpr.equals(yColOrExpr);

        int maxPoints = decimateInfo.getMaxPoints() == 0 ? DECI_DEF_MAX_POINTS : decimateInfo.getMaxPoints();

        int deciEnableSize = decimateInfo.getDeciEnableSize() > -1 ? decimateInfo.getDeciEnableSize() : DECI_ENABLE_SIZE;
        boolean doDecimation = dg.size() >= deciEnableSize;

        DataType[] columns = new DataType[doDecimation ? 5 : 3];
        Class xColClass = Double.class;
        Class yColClass = Double.class;

        ArrayList<DataGroup.Attribute> colMeta = new ArrayList<>();
        try {
            if (xValGetter.isExpression() || sameXY) {
                columns[0] = new DataType("x", "x", xColClass, DataType.Importance.HIGH, "", false);
            } else {
                columns[0] = dg.getDataDefintion(decimateInfo.getxColumnName()).copyWithNoColumnIdx(0);
                colMeta.addAll(IpacTableUtil.getAllColMeta(dg.getAttributes().values(), decimateInfo.getxColumnName()));
            }

            if (yValGetter.isExpression() || sameXY) {
                columns[1] = new DataType("y", "y", yColClass, DataType.Importance.HIGH, "", false);
            } else {
                columns[1] = dg.getDataDefintion(decimateInfo.getyColumnName()).copyWithNoColumnIdx(1);
                colMeta.addAll(IpacTableUtil.getAllColMeta(dg.getAttributes().values(), decimateInfo.getyColumnName()));
            }

            columns[2] = new DataType("rowidx", Integer.class); // need it to tie highlighted and selected to table
            if (doDecimation) {
                columns[3] = new DataType("weight", Integer.class);
                columns[4] = new DataType(DecimateKey.DECIMATE_KEY, String.class);
            }
            xColClass = columns[0].getDataType();
            yColClass = columns[1].getDataType();
        } catch (Exception e) {

        }


        DataGroup retval = new DataGroup("decimated results", columns);
        retval.setAttributes(colMeta);

        // determine min/max values of x and y
        boolean checkDeciLimits = false;
        double xDeciMax = Double.POSITIVE_INFINITY, xDeciMin = Double.NEGATIVE_INFINITY, yDeciMax = Double.POSITIVE_INFINITY, yDeciMin = Double.NEGATIVE_INFINITY;
        if (!Double.isNaN(decimateInfo.getXMin())) { xDeciMin = decimateInfo.getXMin(); checkDeciLimits = true; }
        if (!Double.isNaN(decimateInfo.getXMax())) { xDeciMax = decimateInfo.getXMax(); checkDeciLimits = true; }
        if (!Double.isNaN(decimateInfo.getYMin())) { yDeciMin = decimateInfo.getYMin(); checkDeciLimits = true; }
        if (!Double.isNaN(decimateInfo.getYMax())) { yDeciMax = decimateInfo.getYMax(); checkDeciLimits = true; }
        int outRows = dg.size();
        for (int rIdx = 0; rIdx < dg.size(); rIdx++) {
            DataObject row = dg.get(rIdx);

            double xval = xValGetter.getValue(row);
            double yval = yValGetter.getValue(row);

            if (Double.isNaN(xval) || Double.isNaN(yval)) {
                outRows--;
                continue;
            }

            if (xval > xMax) { xMax = xval; }
            if (xval < xMin) { xMin = xval; }
            if (yval > yMax) { yMax = yval; }
            if (yval < yMin) { yMin = yval; }

            if (!doDecimation) {
                DataObject retrow = new DataObject(retval);
                retrow.setDataElement(columns[0], convertData(xColClass, xval));
                retrow.setDataElement(columns[1], convertData(yColClass, yval));
                retrow.setDataElement(columns[2], rIdx); // natural index
                retval.add(retrow);
            } else if (checkDeciLimits) {
                if (xval>xDeciMax || xval<xDeciMin || yval>yDeciMax || yval<yDeciMin) {
                    outRows--;
                }
            }
        }

        if (Double.isFinite(xMax)) {
            retval.addAttribute(DecimateInfo.DECIMATE_TAG + ".X-MAX", String.valueOf(xMax));
            retval.addAttribute(DecimateInfo.DECIMATE_TAG + ".X-MIN", String.valueOf(xMin));
            retval.addAttribute(DecimateInfo.DECIMATE_TAG + ".Y-MAX", String.valueOf(yMax));
            retval.addAttribute(DecimateInfo.DECIMATE_TAG + ".Y-MIN", String.valueOf(yMin));
        }

        if (doDecimation) {
            boolean checkLimits = false;
            if (checkDeciLimits) {
                if (xDeciMin > xMin || xDeciMax < xMax || yDeciMin > yMin || yDeciMax < yMax) {
                    checkLimits = true;
                }

                // use boundaries provided in decimate info, if available
                if (!Double.isNaN(decimateInfo.getXMin())) { xMin = xDeciMin; }
                if (!Double.isNaN(decimateInfo.getXMax())) { xMax = xDeciMax; }
                if (!Double.isNaN(decimateInfo.getYMin())) { yMin = yDeciMin; }
                if (!Double.isNaN(decimateInfo.getYMax())) { yMax = yDeciMax; }
            }

            if (outRows < deciEnableSize) {
                // no decimation needed
                // because the number of rows in the output
                // is less than decimation limit

                List<DataGroup.Attribute> attributes = retval.getKeywords();
                retval = new DataGroup("decimated results", new DataType[]{columns[0],columns[1],columns[2]});
                retval.setAttributes(attributes);

                for (int rIdx = 0; rIdx < dg.size(); rIdx++) {
                    DataObject row = dg.get(rIdx);

                    double xval = xValGetter.getValue(row);
                    double yval = yValGetter.getValue(row);

                    if (Double.isNaN(xval) || Double.isNaN(yval)) {
                        outRows--;
                        continue;
                    }

                    if (checkLimits && (xval<xMin || xval>xMax || yval<yMin || yval>yMax)) { continue; }
                    DataObject retrow = new DataObject(retval);
                    retrow.setDataElement(columns[0], convertData(xColClass, xval));
                    retrow.setDataElement(columns[1], convertData(yColClass, yval));
                    retrow.setDataElement(columns[2], rIdx);
                    retval.add(retrow);
                }

            } else {

                java.util.Date startTime = new java.util.Date();

                // determine the number of cells on each axis
                int nXs = (int)Math.sqrt(maxPoints * decimateInfo.getXyRatio());  // number of cells on the x-axis
                int nYs = (int)Math.sqrt(maxPoints/decimateInfo.getXyRatio());  // number of cells on the x-axis

                double xUnit = (xMax - xMin)/nXs;        // the x size of a cell
                double yUnit = (yMax - yMin)/nYs;        // the y size of a cell
                // increase cell size a bit to include max values into grid
                xUnit += xUnit/1000.0/nXs;
                yUnit += yUnit/1000.0/nYs;

                DecimateKey decimateKey = new DecimateKey(xMin, yMin, nXs, nYs, xUnit, yUnit);

                HashMap<String, SamplePoint> samples = new HashMap<>();
                // decimating the data now....
                for (int idx = 0; idx < dg.size(); idx++) {
                    DataObject row = dg.get(idx);

                    double xval = xValGetter.getValue(row);
                    double yval = yValGetter.getValue(row);

                    if (Double.isNaN(xval) || Double.isNaN(yval)) { continue; }

                    if (checkLimits && (xval<xMin || xval>xMax || yval<yMin || yval>yMax)) { continue; }

                    String key = decimateKey.getKey(xval, yval);

                    if (samples.containsKey(key)) {
                        SamplePoint pt = samples.get(key);
                        // representative sample point is a random point from the bin
                        int numRepRows = pt.getRepresentedRows()+1;
                        if (Math.random() < 1d/(double)numRepRows) {
                            String xvalFormatted = xValGetter.getFormattedValue(row);
                            String yvalFormatted = yValGetter.getFormattedValue(row);
                            SamplePoint replacePt = new SamplePoint(xval, xvalFormatted, yval, yvalFormatted, idx, numRepRows);
                            samples.put(key, replacePt);
                        } else {
                            pt.addRepresentedRow();
                        }
                    } else {
                        String xvalFormatted = xValGetter.getFormattedValue(row);
                        String yvalFormatted = yValGetter.getFormattedValue(row);
                        SamplePoint pt = new SamplePoint(xval, xvalFormatted, yval, yvalFormatted, idx);
                        samples.put(key, pt);
                    }
                }

                int minWeight=samples.size(), maxWeight=1, weight;
                for(String key : samples.keySet()) {
                    SamplePoint pt = samples.get(key);
                    DataObject row = new DataObject(retval);
                    weight = pt.getRepresentedRows();
                    if (weight<minWeight) minWeight = weight;
                    if (weight>maxWeight) maxWeight = weight;

                    row.setDataElement(columns[0], convertData(xColClass, pt.getX()));
                    row.setDataElement(columns[1], convertData(yColClass, pt.getY()));
                    row.setDataElement(columns[2], pt.getRowIdx());
                    row.setDataElement(columns[3], weight);
                    row.setDataElement(columns[4], key);
                    retval.add(row);
                }
                String decimateInfoStr = decimateInfo.toString();
                retval.addAttribute(DecimateInfo.DECIMATE_TAG,
                        decimateInfoStr.substring(DecimateInfo.DECIMATE_TAG.length() + 1));
                decimateKey.setCols(decimateInfo.getxColumnName(), decimateInfo.getyColumnName());
                retval.addAttribute(DecimateKey.DECIMATE_KEY,
                        decimateKey.toString());
                retval.addAttribute(DecimateInfo.DECIMATE_TAG + ".X-UNIT", String.valueOf(xUnit));
                retval.addAttribute(DecimateInfo.DECIMATE_TAG + ".Y-UNIT", String.valueOf(yUnit));
                retval.addAttribute(DecimateInfo.DECIMATE_TAG + ".WEIGHT-MIN", String.valueOf(minWeight));
                retval.addAttribute(DecimateInfo.DECIMATE_TAG + ".WEIGHT-MAX", String.valueOf(maxWeight));

                java.util.Date endTime = new java.util.Date();
                Logger.briefInfo(decimateInfoStr + " - took "+(endTime.getTime()-startTime.getTime())+"ms");
            }
        }


        if (xValGetter.isExpression() || sameXY) {
            DataType.FormatInfo fi = columns[0].getFormatInfo();
            fi.setDataFormat(getFormatterString(xMin, xMax, 6));
            columns[0].setFormatInfo(fi);
            retval.addAttribute(DecimateInfo.DECIMATE_TAG + ".X-EXPR", decimateInfo.getxColumnName());
            retval.addAttribute(DecimateInfo.DECIMATE_TAG + ".X-COL", "x");
        }

        if (yValGetter.isExpression() || sameXY) {
            DataType.FormatInfo fi = columns[1].getFormatInfo();
            fi.setDataFormat(getFormatterString(xMin, xMax, 6));
            columns[1].setFormatInfo(fi);
            retval.addAttribute(DecimateInfo.DECIMATE_TAG + ".Y-EXPR", decimateInfo.getyColumnName());
            retval.addAttribute(DecimateInfo.DECIMATE_TAG + ".Y-COL", "y");
        }

        retval.shrinkToFitData();

        return retval;
    }

    private static int getFirstSigDigitPos(double num) {
        return (int)Math.floor(Math.log10(num))+1;
    }

    /*
     * Get printf-style format string, which would preserve certain number of significant digits
     * in the difference between two numbers.
     * @param min - minimum number
     * @param max - maximum number
     * @param numSigDigits - number of the significant digits to preserve in the difference
     */
    private static String getFormatterString(double min, double max, int numSigDigits) {
        if (!Double.isFinite(min)||!Double.isFinite(max)) {
            return "%."+numSigDigits+"g";
        }
        double range = Math.abs(max-min);
        int firstSigDigitPos = 0;
        if (range == 0) {
            if (min == 0) { return "%d"; }
        } else {
            firstSigDigitPos = getFirstSigDigitPos(range);
        }
        int firstSigDigitPosN = getFirstSigDigitPos(Math.max(Math.abs(min), Math.abs(max)));
        int needSigDigits = numSigDigits+(Math.abs(firstSigDigitPosN-firstSigDigitPos));
        return "%."+needSigDigits+"g";
    }

    public static Object convertData(Class dataType, double x) {
        if (dataType.isAssignableFrom(Double.class)) {
            return x;
        } else if (dataType.isAssignableFrom(Float.class)) {
            return (float)x;
        } else if (dataType.isAssignableFrom(Long.class)) {
            return (long)x;
        } else if (dataType.isAssignableFrom(Integer.class)) {
            return (int)x;
        } else {
            return String.valueOf(x);
        }
    }

    /**
     * given a column name, it will return a new column name based on IRSA's uploaded column naming convention.
     * @param cname
     * @return
     */
    public static String getUploadedCName(String cname) {
        String s = cname.replaceFirst("_\\d\\d$", "");
        int seq = getSeqNumber(cname);
        return s + String.format("_%02d", seq+1);
    }

    public static int getSeqNumber(String cname) {
        String s = cname.replaceFirst("_\\d\\d$", "");
        String seqStr = s.equals(cname) ? "" : cname.replaceFirst(s+"_", "");
        int seq = 0;
        if (seqStr.length() > 0) {
            seq = Integer.parseInt(seqStr);
        }
        return seq;
    }

    private static Map<String, String> encodedStringToMap(String str) {
        if (StringUtils.isEmpty(str)) return null;
        HashMap<String, String> map = new HashMap<String, String>();
        for (String entry : str.split("&")) {
            String[] kv = entry.split("=", 2);
            String value = kv.length > 1 ? decode(kv[1].trim()) : "";
            map.put(kv[0].trim(), value);
        }
        return map;
    }

    public static String decode(String str) {
        if (str == null) return "";
        try {
            return URIUtil.decode(str);
        } catch (URIException e) {
            return e.getMessage();
        }

    }

    public static void main(String[] args) {
        try {
            getUploadedTargets(new File("/Users/loi/fc.tbl"));
        } catch (DataAccessException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class SamplePoint {
        double x;
        double y;
        String formattedX;
        String formattedY;
        int rowIdx;
        int representedRows;

        public SamplePoint(double x, String formattedX, double y, String formattedY, int rowIdx) {
            this(x, formattedX, y, formattedY, rowIdx, 1);
        }


        public SamplePoint(double x, String formattedX, double y, String formattedY, int rowIdx, int representedRows) {
            this.x = x;
            this.y = y;
            this.formattedX = formattedX;
            this.formattedY = formattedY;
            this.rowIdx = rowIdx;
            this.representedRows = representedRows;
        }

        public void addRepresentedRow() { representedRows++; }
        public int getRepresentedRows() { return representedRows; }

        public int getRowIdx() { return rowIdx; }
        public double getX() { return x; }
        public double getY() { return y; }
        public String getFormattedX() { return formattedX; }
        public String getFormattedY() { return formattedY; }

    }
}
