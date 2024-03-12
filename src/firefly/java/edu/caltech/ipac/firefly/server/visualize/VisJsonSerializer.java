/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize;


import edu.caltech.ipac.firefly.data.BandInfo;
import edu.caltech.ipac.firefly.data.RelatedData;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.BandState;
import edu.caltech.ipac.firefly.visualize.CreatorResults;
import edu.caltech.ipac.firefly.visualize.DirectFitsAccessData;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebFitsData;
import edu.caltech.ipac.firefly.visualize.WebPlotHeaderInitializer;
import edu.caltech.ipac.firefly.visualize.WebPlotInitializer;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.draw.AreaStatisticsUtil;
import edu.caltech.ipac.visualize.plot.PixelValue;
import edu.caltech.ipac.visualize.plot.RangeValues;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.util.Cursor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Trey Roby
 * Date: 10/12/15
 */
public class VisJsonSerializer {

    public static String createResultJson(WebPlotResult res) {
        JSONArray wrapperAry= new JSONArray();
        addJsonObj(wrapperAry, createJsonResultItem(res));
        return wrapperAry.toString();
    }

    public static String createPlotGroupResultJson(List<WebPlotResult> resList, String requestKey) {
        JSONArray ary= new JSONArray();
        for(WebPlotResult res : resList) addJsonObj(ary, createJsonResultItem(res));
        JSONObject map = new JSONObject();
        putBool(map, "success", true);
        putJsonAry(map, "data", ary);
        putStr(map,"requestKey", requestKey);
        return map.toString();
    }


    private static JSONObject createJsonPixelResultObj(PixelValue.Result r) {
        JSONObject jsonPixelResult= new JSONObject();
        putStrNotNull(jsonPixelResult,"status",r.status());
        putStrNotNull(jsonPixelResult,"type",r.type());
        putStrNotNull(jsonPixelResult,"valueBase10",r.valueBase10());
        putStrNotNull(jsonPixelResult,"valueBase16",r.valueBase16());
        return jsonPixelResult;
    }


    public static String createPixelResultJson(List<PixelValue.Result> resList, Band[] bands, int stateCnt) {
        JSONObject data= new JSONObject();
        int resultCnt=0;
        for (;resultCnt<resList.size() && resultCnt<bands.length; resultCnt++){
            putJsonObj(data,bands[resultCnt].toString(),createJsonPixelResultObj(resList.get(resultCnt)));
        }

        if (stateCnt>1) {
            for(int i=1; (i<stateCnt);i++) {
                putJsonObj(data,"overlay-"+(i-1), createJsonPixelResultObj(resList.get(resultCnt++)));
            }
        }

        JSONObject map = new JSONObject();
        putBool(map, "success", true);
        putJsonObj(map, "data", data);
        JSONArray wrapperAry= new JSONArray();
        addJsonObj(wrapperAry,map);
        return wrapperAry.toJSONString();
    }



    public static PlotState deserializePlotStateFromString(String s) {
        if (s==null) return null;
        try {
            Object obj= new JSONParser().parse(s);
            if (obj instanceof JSONObject jo) return deserializePlotState(jo);
            return null;
        } catch (ParseException e){
            return null;
        }
    }

    private static JSONObject createJsonResultItem(WebPlotResult res) {

        JSONObject map = new JSONObject();
        putBool(map, "success", res.success());
        String requestKey= res.requestKey()==null?"":res.requestKey();
        if (res.success()) {
            if (res.containsKey(WebPlotResult.PLOT_CREATE)) {
                CreatorResults cr= (CreatorResults)res.getResult(WebPlotResult.PLOT_CREATE);
                JSONArray ary = new JSONArray();
                for(WebPlotInitializer wpInit : cr.wpInitAry()) {
                    addJsonObj(ary,VisJsonSerializer.serializeWebPlotInitializer(wpInit));
                }
                putJsonAry(map, WebPlotResult.PLOT_CREATE, ary);
                putJsonObj(map, WebPlotResult.PLOT_CREATE_HEADER, VisJsonSerializer.serializeWebPlotHeaderInitializer(cr.wpHeader()));
            }
            if (res.containsKey(WebPlotResult.STRING)) {
                putWpResultStr(res,map,WebPlotResult.STRING);
            }
            if (res.containsKey(WebPlotResult.REGION_FILE_NAME)) {
                putWpResultStr(res,map,WebPlotResult.REGION_FILE_NAME);
            }
            if (res.containsKey(WebPlotResult.DATA_HISTOGRAM)) {
                int[] intAry= (int[])res.getResult(WebPlotResult.DATA_HISTOGRAM);
                JSONArray ary = new JSONArray();
                for(int v : intAry) ary.add(v);
                putJsonAry(map,WebPlotResult.DATA_HISTOGRAM, ary);
            }
            if (res.containsKey(WebPlotResult.DATA_BIN_MEAN_ARRAY)) {
                double[] dAry= (double[])res.getResult(WebPlotResult.DATA_BIN_MEAN_ARRAY);
                JSONArray ary = new JSONArray();
                for(double v : dAry) ary.add(v);
                putJsonAry(map, WebPlotResult.DATA_BIN_MEAN_ARRAY, ary);
            }
            if (res.containsKey(WebPlotResult.DATA_BIN_COLOR_IDX)) {
                byte[] bAry = (byte[]) res.getResult(WebPlotResult.DATA_BIN_COLOR_IDX);
                JSONArray ary = new JSONArray();
                for(byte b : bAry) ary.add(b);
                putJsonAry(map, WebPlotResult.DATA_BIN_COLOR_IDX, ary);
            }
            if (res.containsKey(WebPlotResult.BAND_INFO)) {
                BandInfo bi= (BandInfo)res.getResult(WebPlotResult.BAND_INFO);
                putJsonObj(map, WebPlotResult.BAND_INFO, bandInfoDeepSerialize(bi));
            }
            if (res.containsKey(WebPlotResult.REGION_DATA)) {
                String s= res.getStrResult(WebPlotResult.REGION_DATA);
                JSONArray ary = new JSONArray();
                if (s!=null && s.length()>2) {
                    String[] regAry = s.substring(1, s.length() - 1).split("--STR--");
                    Collections.addAll(ary, regAry);
                }
                putJsonAry(map,WebPlotResult.REGION_DATA, ary);
            }
            if (res.containsKey(WebPlotResult.REGION_ERRORS)) {
                putWpResultStr(res,map,WebPlotResult.REGION_ERRORS);
            }
            if (res.containsKey(WebPlotResult.TITLE)) {
                putWpResultStr(res,map,WebPlotResult.TITLE);
            }
            if (res.containsKey(WebPlotResult.RESULT_ARY)) {
                WebPlotResult[] resultAry= (WebPlotResult[])res.getResult(WebPlotResult.RESULT_ARY);
                JSONArray jResAry= new JSONArray();
                for(WebPlotResult r : resultAry) addJsonObj(jResAry, createJsonResultItem(r));
                putJsonAry(map,WebPlotResult.RESULT_ARY, jResAry);

            }
            putStr(map, "requestKey", requestKey);

        }
        else {
            putStr(map, "briefFailReason", res.briefFailReason());
            putStr(map, "userFailReason", res.userFailReason());
            putStr(map, "detailFailReason", res.detailFailReason());
            putStr(map, "requestKey", requestKey);
            putStr(map, "plotId", res.plotId());
        }

        JSONObject wrapperObj= new JSONObject();
        putBool(wrapperObj, "success", true);
        putJsonObj(wrapperObj, "data", map);


        return wrapperObj;

    }

    private static JSONObject bandInfoDeepSerialize(BandInfo bi) {
        var metMap= bi.metricsMap();
        JSONObject jo= new JSONObject();
        for(var entry : metMap.entrySet()) {
            putJsonObj(jo, entry.getKey().toString(), convertMetrics(entry.getValue()));
        }
        return jo;
    }


    private static JSONObject convertMetrics(Map<AreaStatisticsUtil.Metrics, AreaStatisticsUtil.Metric> metricMap) {
        JSONObject jo= new JSONObject();
        for(var entry : metricMap.entrySet()) {
            putJsonObj(jo, entry.getKey().toString(), convertOneMetric(entry.getValue()));
        }
        return jo;
    }

    private static JSONObject convertOneMetric(AreaStatisticsUtil.Metric metric) {
        JSONObject jo= new JSONObject();
        putStr(jo, "desc", metric.desc());
        putDouble(jo, "value", metric.value());
        putStr(jo, "units", metric.units());
        if (metric.imageWorkSpacePt()!=null) putStr(jo, "ip", metric.imageWorkSpacePt().toString());
        return jo;
    }


    private static JSONObject serializeWebPlotHeaderInitializer(WebPlotHeaderInitializer wpHeader) {
        if (wpHeader==null) return null;
        JSONObject map = new JSONObject();
        putStr(map,"workingFitsFileStr", wpHeader.workingFitsFileStr());
        putStr(map,"originalFitsFileStr", wpHeader.originalFitsFileStr());
        putStrNotNull(map,"uploadFileNameStr", wpHeader.uploadFileNameStr());
        putBoolIfTrue(map,"threeColor", wpHeader.threeColor());
        if (wpHeader.rv()!=null) putStr(map,"rangeValuesSerialize", wpHeader.rv().serialize());
        putStr(map,"plotRequestSerialize", wpHeader.request().toString());
        putStr(map,"dataDesc", wpHeader.dataDesc());
        putJsonAry(map,"zeroHeaderAry", serializeHeaderAry(wpHeader.zeroHeaderAry()));
        putBoolIfTrue(map,"multiImageFile", wpHeader.multiImageFile());
        if (wpHeader.attributes()!=null) putJsonObj(map,"attributes", new JSONObject(wpHeader.attributes()));
        return map;
    }


    private static JSONObject serializeWebPlotInitializer(WebPlotInitializer wpInit) {
        JSONObject map = new JSONObject();

        if (wpInit.imageCoordSys()!=null) putStr(map,"imageCoordSys", wpInit.imageCoordSys().toString());
        if (wpInit.headerAry()!=null) putJsonAry(map,"headerAry", serializeHeaderAry(wpInit.headerAry()));
        if (wpInit.zeroHeaderAry()!=null) putJsonAry(map,"zeroHeaderAry", serializeHeaderAry(wpInit.zeroHeaderAry()));
        if (wpInit.relatedData()!=null) putJsonAry(map,"relatedData", serializeRelatedDataArray(wpInit.relatedData()));
        putNumOver0(map,"dataWidth", wpInit.dataWidth());
        putNumOver0(map,"dataHeight", wpInit.dataHeight());
        putJsonObj(map, "plotState", serializePlotState(wpInit.plotState()));
        putStrNotNull(map, "desc", wpInit.desc());
        putStrNotNull(map,"dataDesc", wpInit.dataDesc());

        if (wpInit.plotState().isThreeColor()) {
            JSONArray ary= new JSONArray();
            for(WebFitsData wfd : wpInit.fitsData()) addJsonObj(ary,serializeWebFitsData(wfd));
            putJsonAry(map,"fitsData", ary);
        }
        else {
            if (wpInit.fitsData()!=null) putJsonObj(map, "fitsData", serializeWebFitsData(wpInit.fitsData()[0]));
        }
        return map;
    }



    private static JSONArray serializeRelatedDataArray(List<RelatedData> relatedData) {
        if (relatedData==null || relatedData.size()==0) return null;
        JSONArray relatedArray= new JSONArray();
        for(RelatedData r : relatedData) {
            addJsonObj(relatedArray,serializeRelated(r));
        }
        return relatedArray;
    }

    private static JSONObject serializeRelated(RelatedData rData) {
        if (rData==null) return null;
        JSONObject retObj= new JSONObject();
        putStr(retObj,"dataType", rData.getDataType());
        if (!rData.getAvailableMask().isEmpty()) {
            putJsonObj(retObj, "availableMask", new JSONObject(rData.getAvailableMask()));
        }
        putJsonObj(retObj, "searchParams", new JSONObject(rData.getSearchParams()));
        putStr(retObj, "desc", rData.getDesc());
        putStr(retObj, "dataKey", rData.getDataKey());
        putStrNotNull(retObj, "hduName", rData.getHduName());
        if (rData.getHduIdx()>-1) {
            putNum(retObj, "hduIdx", rData.getHduIdx());
            putNum(retObj, "hduVersion", rData.getHduVersion());
            putNum(retObj, "hduLevel", rData.getHduLevel());
        }
        putNum(retObj, "primaryHduIdx", rData.getPrimaryHduIdx());
        return retObj;
    }


    private static JSONArray serializeHeaderAry(Header[] headerAry) {
        if (headerAry==null) return null;
        JSONArray retAry= new JSONArray();
        for (Header header : headerAry) addJsonObj(retAry, serializeHeader(header));
        return retAry;
    }

    private static JSONObject serializeHeader(Header header) {
        if (header==null) return null;
        JSONObject map = new JSONObject();
        Cursor<String, HeaderCard> i= header.iterator();
        int pos = 0;
        HeaderCard card;
        while(i.hasNext()) {
            card= i.next();
            pos++;
            JSONObject cardData = new JSONObject();
            putStr(cardData, "value", card.getValue());
            putStr(cardData, "comment", card.getComment());
            putNum(cardData, "idx", pos);

            if (map.containsKey(card.getKey())) {
                Object data= map.get(card.getKey());
                if (data instanceof JSONArray cardDataAry) {
                    addJsonObj(cardDataAry,cardData);
                }
                else if (data instanceof  JSONObject jo) {
                    JSONArray newCardDataAry= new JSONArray();
                    addJsonObj(newCardDataAry, jo);
                    addJsonObj(newCardDataAry,cardData);
                    putJsonAry(map, card.getKey(), newCardDataAry);
                }
            }
            else {
                putJsonObj(map, card.getKey(), cardData);
            }
        }
        return map;
    }

    private static JSONObject serializeWebFitsData(WebFitsData wfData) {
        if (wfData==null) return null;
        JSONObject map = new JSONObject();
        putDoubleNot0(map, "dataMin", wfData.dataMin());
        putDoubleNot0(map,"dataMax", wfData.dataMax());
        putDoubleNot0(map,"largeBinPercent", wfData.largeBinPercent());
        putStr(map, "fluxUnits", wfData.fluxUnits());
        putNum(map, "getFitsFileSize", wfData.fitsFileSize());
        return map;
    }

    private static  JSONObject serializePlotState(PlotState s) {
        if (s==null) return null;
        JSONObject map = new JSONObject();
        putStr(map, "ctxStr", s.getContextString());

        // don't pass if defaulted
        putBoolIfTrue(map,"threeColor", s.isThreeColor());
        if (s.getOperations().size()>0 ) {
            JSONArray outOpList = new JSONArray();
            for(PlotState.Operation op  : s.getOperations()) {
                addStr(outOpList, op.toString());
            }
            putJsonAry(map,"ops", outOpList);
        }

        // band state array
        BandState[] bandStateAry= s.getBandStateAry();
        if (s.isThreeColor()) {
            JSONArray list = new JSONArray();
            for (BandState bandState : bandStateAry) {
                list.add((bandState == null || (!bandState.hasRequest() && bandState.getOriginalFitsFileStr() == null)) ?
                        null : serializeBandState(bandState));
            }
            putJsonAry(map, "bandStateAry", list);
        }
        else  {
            putJsonObj(map, "bandStateAry", serializeBandState(bandStateAry[0]));
        }
        return map;
    }



    private static PlotState deserializePlotState(JSONObject map) {
        try {
            PlotState state= new PlotState();
            state.setContextString(getStr(map, "ctxStr"));


            // optional - default if not passed
            state.setThreeColor(getBoolean(map, "threeColor"));
            JSONArray opList = (JSONArray)map.get("ops");
            if (opList!=null) {
                for(Object oStr : opList) {
                    state.addOperation( StringUtils.getEnum(oStr.toString(), PlotState.Operation.CROP));
                }
            }


            JSONArray pList = (JSONArray)map.get("bandStateAry");
            BandState[] bandStateAry= new BandState[] {null,null,null};
            for(int j= 0; (j<PlotState.MAX_BANDS && j<pList.size());j++) {
                bandStateAry[j]= deserializeBandState((JSONObject)pList.get(j));
            }
            state.setBandStateAry(bandStateAry);

            return state;
        } catch (ClassCastException|IllegalArgumentException  e) {
            return null;
        }
    }


    private static JSONObject serializeBandState(BandState b) {
        JSONObject map = new JSONObject();
        if (b.getWorkingFitsFileStr()!=null) {
            putStr(map,"workingFitsFileStr", b.getWorkingFitsFileStr());
        }
        if (b.getOriginalFitsFileStr()!=null) {
            if (!ComparisonUtil.equals(b.getWorkingFitsFileStr(), b.getOriginalFitsFileStr())) {
                putStr(map,"originalFitsFileStr", b.getOriginalFitsFileStr());
            }
        }
        putStrNotNull(map,"uploadFileNameStr", b.getUploadedFileName());
        putNumOver0(map,"imageIdx", b.getImageIdx());
        putNumOver0(map,"originalImageIdx", b.getOriginalImageIdx());
        putStrNotNull(map, "plotRequestSerialize", b.getWebPlotRequestSerialized());
        putBoolIfTrue(map,"multiImageFile", b.isMultiImageFile());
        putBoolIfTrue(map,"tileCompress", b.isTileCompress());
        putNumOver0(map,"cubeCnt", b.getCubeCnt());
        putNumOver0(map, "cubePlaneNumber", b.getCubePlaneNumber());
        putStrNotNull(map, "rangeValuesSerialize", b.getRangeValuesSerialized());
        return map;
    }

    private static BandState deserializeBandState(JSONObject map) {
        if (map==null) return null;
        try {
            BandState b= new BandState();

            String working= getStr(map, "workingFitsFileStr");
            String original= getStr(map,"originalFitsFileStr", true);
            b.setWorkingFitsFileStr(working);
            b.setOriginalFitsFileStr( original!=null ? original : working);

            b.setUploadedFileName(getStr(map,"uploadFileNameStr",true));
            b.setImageIdx(getInt(map, "imageIdx",0));
            b.setOriginalImageIdx(getInt(map,"originalImageIdx",0));
            b.setWebPlotRequest(WebPlotRequest.parse(getStr(map, "plotRequestSerialize")));
            b.setRangeValues(RangeValues.parse(getStr(map,"rangeValuesSerialize")));
            b.setDirectFileAccessData(deserializeDirectFileAccess((JSONObject)map.get("directFileAccessData")));
            b.setMultiImageFile(getBoolean(map,"multiImageFile"));
            b.setCubeCnt(getInt(map,"cubeCnt",0));
            b.setCubePlaneNumber(getInt(map, "cubePlaneNumber",0));
            b.setTileCompress(getBoolean(map,"tileCompress"));
            return b;
        } catch (ClassCastException|IllegalArgumentException  e) {
            return null;
        }
    }

    static DirectFitsAccessData deserializeDirectFileAccess(JSONObject map) {
        if (map==null) return null;
        Map<String,String> tMap= new HashMap<>(30);
        for(Object key : map.keySet()) tMap.put((String)key, map.get(key)+"");
        return new DirectFitsAccessData(tMap);
    }

    private static String getStr(JSONObject j, String key) throws IllegalArgumentException, ClassCastException {
        return getStr(j,key,false);
    }


    private static String getStr(JSONObject j, String key, boolean acceptNull) throws IllegalArgumentException, ClassCastException {
        String s= (String)j.get(key);
        if (s==null && !acceptNull) throw new IllegalArgumentException(key + " must exist");
        return s;
    }

    private static int getInt(JSONObject j, String key) { return getInt(j,key,0); }

    private static int getInt(JSONObject j, String key, int defValue) {
        try {
            Object o= j.get(key);
            if (o==null) return defValue;
            if (o instanceof Number num) return num.intValue();
            if (!(o instanceof String s)) return defValue;
            return Integer.parseInt(s);
        } catch (Exception e) {
            return defValue;
        }
    }

    private static boolean getBoolean(JSONObject j, String key) throws IllegalArgumentException, ClassCastException {
        return Objects.requireNonNullElse((Boolean)j.get(key), false);
    }

    private static void putStr(JSONObject m, String key, String v) {m.put(key,v);}
    private static void putBool(JSONObject m, String key, boolean v) {m.put(key,v);}
    private static void putDouble(JSONObject m, String key, double v) {m.put(key,v);}
    private static void putJsonObj(JSONObject m, String key, JSONObject v) {m.put(key,v);}
    private static void putJsonAry(JSONObject m, String key, JSONArray v) {m.put(key,v);}
    private static void putNum(JSONObject m, String key, int v) {m.put(key,v);}
    private static void putNum(JSONObject m, String key, long v) {m.put(key,v);}

    private static void addStr(JSONArray  a, String v) {a.add(v);}
    private static void addJsonObj(JSONArray a, JSONObject v) {a.add(v);}

    private static void putDoubleOver0(JSONObject m, String key, double v) {if (v>0.0) putDouble(m,key,v);}
    private static void putDoubleNot0(JSONObject m, String key, double v) {if (v!=0.0) putDouble(m,key,v);}
    private static void putNumOver0(JSONObject m, String key, int v) {if (v>0) putNum(m,key,v);}
    private static void putStrNotNull(JSONObject m, String key, String v) {if (v!=null) putStr(m,key,v);}
    private static void putBoolIfTrue(JSONObject m, String key, boolean v) {if (v) putBool(m,key,v);}
    private static void putWpResultStr(WebPlotResult res, JSONObject m, String key) {
        putStr(m, key, res.getStrResult(key));
    }
}
