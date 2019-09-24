/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 10/12/15
 * Time: 4:10 PM
 */


import edu.caltech.ipac.firefly.data.RelatedData;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.BandState;
import edu.caltech.ipac.firefly.visualize.ClientFitsHeader;
import edu.caltech.ipac.firefly.visualize.InsertBandInitializer;
import edu.caltech.ipac.firefly.visualize.PlotImages;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.StretchData;
import edu.caltech.ipac.firefly.visualize.WebFitsData;
import edu.caltech.ipac.firefly.visualize.WebPlotHeaderInitializer;
import edu.caltech.ipac.firefly.visualize.WebPlotInitializer;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.RangeValues;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.util.Cursor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class VisJsonSerializer {


    public static JSONObject serializeWebPlotHeaderInitializer(WebPlotHeaderInitializer wpHeader) {
        JSONObject map = null;
        if (wpHeader!=null) {
            map = new JSONObject();
            map.put("workingFitsFileStr", wpHeader.getWorkingFitsFileStr());
            map.put("originalFitsFileStr", wpHeader.getOriginalFitsFileStr());
            if (wpHeader.getUploadFileNameStr()!=null) map.put("uploadFileNameStr", wpHeader.getUploadFileNameStr());
            if (wpHeader.isThreeColor()) map.put("threeColor", wpHeader.isThreeColor());
            map.put("plotRequestSerialize", wpHeader.getRequest().toString());
            map.put("dataDesc", wpHeader.getDataDesc());
            map.put("zeroHeaderAry", serializeHeaderAry(wpHeader.getZeroHeaderAry()));
            if (wpHeader.getAttributes()!=null) map.put("attributes", new JSONObject(wpHeader.getAttributes()));
        }
        return map;
    }


    public static JSONObject serializeWebPlotInitializerDeep(WebPlotInitializer wpInit) {
        JSONObject map = new JSONObject();

        if (wpInit.getCoordinatesOfPlot()!=null) map.put("imageCoordSys", wpInit.getCoordinatesOfPlot().toString());
        if (wpInit.getHeaderAry()!=null) map.put("headerAry", serializeHeaderAry(wpInit.getHeaderAry()));
        if (wpInit.getZeroHeaderAry()!=null) map.put("zeroHeaderAry", serializeHeaderAry(wpInit.getZeroHeaderAry()));
        if (wpInit.getRelatedData()!=null) map.put("relatedData", serializeRelatedDataArray(wpInit.getRelatedData()));
        if (wpInit.getDataWidth()>=0) map.put("dataWidth", wpInit.getDataWidth());
        if (wpInit.getDataWidth()>=0) map.put("dataHeight", wpInit.getDataHeight());
        map.put("initImages", serializePlotImages(wpInit.getInitImages()));
        map.put("plotState", serializePlotState(wpInit.getPlotState()));
        map.put("desc", wpInit.getPlotDesc());
        if (wpInit.getDataDesc()!=null) map.put("dataDesc", wpInit.getDataDesc());

        JSONArray ary= new JSONArray();

        if (wpInit.getPlotState().isThreeColor()) {
            for(WebFitsData wfd : wpInit.getFitsData()) ary.add(serializeWebFitsData(wfd));
            map.put("fitsData", ary);
        }
        else {
            map.put("fitsData", serializeWebFitsData(wpInit.getFitsData()[0]));
        }

        return map;
    }

    public static JSONArray serializeRelatedDataArray(List<RelatedData> relatedData) {
        if (relatedData==null || relatedData.size()==0) return null;
        JSONArray relatedArray= new JSONArray();
        for(RelatedData r : relatedData) {
            if (r.isSendToClient()) {
                relatedArray.add(serializeRelated(r));
            }
        }
        return relatedArray;
    }

    public static JSONObject serializeRelated(RelatedData rData) {
        if (rData==null) return null;
        JSONObject retObj= new JSONObject();
        retObj.put("dataType", rData.getDataType());
        if (rData.getAvailableMask().size()>0) {
            retObj.put("availableMask", new JSONObject(rData.getAvailableMask()));
        }
        retObj.put("searchParams", new JSONObject(rData.getSearchParams()));
        retObj.put("desc", rData.getDesc());
        retObj.put("dataKey", rData.getDataKey());
        return retObj;
    }

//    public static JSONObject serializeProjection(WebPlotInitializer wpInit) {
//        Projection proj= wpInit.getProjection();
//        if (proj==null) return null;
//        JSONObject map = new JSONObject();
//        map.put("coorindateSys", proj.getCoordinateSys().toString());
//        map.put("header", serializeProjectionParams(proj.getProjectionParams()));
//        return map;
//    }

//    public static JSONObject serializeProjectionParams(ProjectionParams p) {
//        JSONObject map = new JSONObject();
//
//        for(Map.Entry<String,Object> e : p.sendToClientHeaders.entrySet() ) {
//            Object v= e.getValue();
//            String k= e.getKey();
//            if (v==null || v instanceof String || v instanceof Number || v instanceof Boolean) {
//                map.put(k,v);
//            }
//            else if (v instanceof double[]) {
//                map.put(k,makeJAry((double[])v));
//            }
//            else if (v instanceof double[][]) {
//                map.put(k,makeJAry2d((double[][])v));
//            }
//            else {
//                Logger.warn("found unexpected type in serializeProjectionParams: key: "+k+", value: "+ v.toString());
//            }
//        }
//
//        return map;
//
//    }

    public static JSONArray serializeHeaderAry(Header headerAry[]) {
        if (headerAry==null) return null;
        JSONArray retAry= new JSONArray();
        for(int i=0; (i<headerAry.length); i++) retAry.add(serializeHeader(headerAry[i]));
        return retAry;
    }

    public static JSONObject serializeHeader(Header header) {
        if (header==null) return null;
        JSONObject map = new JSONObject();
        Cursor<String, HeaderCard> i= header.iterator();
        int pos = 0;
        for(HeaderCard card= i.next(); i.hasNext();  card= i.next(), pos++) {
            JSONObject mapValue = new JSONObject();
            mapValue.put("value", card.getValue());
            mapValue.put("comment", card.getComment());
            mapValue.put("idx", pos);
            map.put(card.getKey(), mapValue);
        }
        return map;
    }

    public static JSONArray makeJAry(double a[]) {
        if (a==null) return null;
        JSONArray aList= new JSONArray();
        for(double entry : a) aList.add(entry);
        return aList;
    }

    private static JSONArray makeJAry2d(double dAry[][]) {
        if (dAry==null) return null;
        JSONArray aList= new JSONArray();
        for(int j= 0; (j<dAry.length); j++) {
            JSONArray innerAry= new JSONArray();
            aList.add(innerAry);
            for(int i= 0; (i<dAry[j].length); i++) {
                innerAry.add(dAry[j][i]);
            }
        }
        return aList;
    }


    public static JSONObject serializeInsertBandInitializer(InsertBandInitializer bInit) {
        JSONObject map = new JSONObject();
        map.put("initImages", serializePlotImages(bInit.getImages()));
        map.put("plotState", serializePlotState(bInit.getPlotState()));
        map.put("fitsData", serializeWebFitsData(bInit.getFitsData()));
        map.put("band", bInit.getBand().toString());
        map.put("dataDesc", bInit.getDataDesc());
        return map;
    }




    public static JSONObject serializePlotImages(PlotImages pi) {
        if (pi==null) return null;
        JSONObject map = new JSONObject();

        map.put("templateName", pi.getTemplateName());
        map.put("screenWidth", pi.getScreenWidth());
        map.put("screenHeight", pi.getScreenHeight());
        map.put("zfact", pi.getZoomFactor());


        JSONObject thumbMap = new JSONObject();
        PlotImages.ThumbURL thumb= pi.getThumbnail();
        thumbMap.put("url", thumb.getURL());
        thumbMap.put("width", thumb.getWidth());
        thumbMap.put("height", thumb.getHeight());
        map.put("thumbnailImage", thumbMap);


        JSONArray imageAry= new JSONArray();
        JSONObject imageMap;
        for(PlotImages.ImageURL image : pi) {
            imageMap = new JSONObject();

            imageMap.put("url", image.getURL());
            imageMap.put("x", image.getXoff());
            imageMap.put("y", image.getYoff());
            imageMap.put("width", image.getWidth());
            imageMap.put("height", image.getHeight());
//            imageMap.put("index", image.getIndex()); // not used
//            imageMap.put("created", image.isCreated()); // not used
            imageAry.add(imageMap);
        }
        map.put("images", imageAry);
        return map;
    }

    public static JSONObject serializeWebFitsData(WebFitsData wfData) {
        if (wfData==null) return null;
        JSONObject map = new JSONObject();
        map.put("dataMin", wfData.getDataMin());
        map.put("dataMax", wfData.getDataMax());
        map.put("fluxUnits", wfData.getFluxUnits());
        map.put("getFitsFileSize", wfData.getFitsFileSize());
        return map;
    }

    public static  JSONObject serializePlotState(PlotState s) {
        if (s==null) return null;


        JSONObject map = new JSONObject();

        map.put("ctxStr", s.getContextString());
        map.put("zoomLevel", s.getZoomLevel());
        if (s.getColorTableId()>0) map.put("colorTableId", s.getColorTableId());


        // don't pass if defaulted
        if (s.getMultiImageAction()!=PlotState.MultiImageAction.GUESS) map.put("multiImage", s.getMultiImageAction().toString());
        if (!Double.isNaN(s.getRotationAngle())) map.put("rotationAngle", s.getRotationAngle());
        if (s.getRotateType()!=PlotState.RotateType.UNROTATE) map.put("rotationType", s.getRotateType().toString());
        if (s.getRotateNorthType()!=CoordinateSys.EQ_J2000) map.put("rotaNorthType", s.getRotateNorthType().toString());
        if (s.isFlippedY()) map.put("flippedY", true);
        if (s.isThreeColor()) map.put("threeColor", true);
        if (s.getOperations().size()>0 ) {
            JSONArray outOpList = new JSONArray();
            for(PlotState.Operation op  : s.getOperations()) {
                outOpList.add(op.toString());
            }
            map.put("ops", outOpList);

        }


        // band state array
        BandState bandStateAry[]= s.getBandStateAry();
        if (s.isThreeColor()) {
            JSONArray list = new JSONArray();
            for(int i= 0; (i< bandStateAry.length); i++) {
                list.add((bandStateAry[i]==null || (!bandStateAry[i].hasRequest() && bandStateAry[i].getOriginalFitsFileStr()==null)) ?
                        null : serializeBandState(bandStateAry[i]));
            }
            map.put("bandStateAry", list);
        }
        else  {
            map.put("bandStateAry", serializeBandState(bandStateAry[0]));
        }
        return map;
    }



    public static StretchData deserializeStretchDataFromString(String s) {
        if (s==null) return null;
        try {
            JSONParser parser= new JSONParser();
            Object obj= parser.parse(s);
            if (obj!=null && obj instanceof JSONObject) {
                JSONObject sdJson= (JSONObject)obj;
                return new StretchData(
                        Band.parse(getStr(sdJson,"band")),
                        RangeValues.parse(getStr(sdJson,"rv")),
                        (Boolean)sdJson.get("bandVisible"));
            }
        } catch (ParseException e){
            return null;
        }
        return null;
    }





    public static PlotState deserializePlotStateFromString(String s) {
        if (s==null) return null;
        try {
            PlotState state= null;
            JSONParser parser= new JSONParser();
            Object obj= parser.parse(s);
            if (obj!=null && obj instanceof JSONObject) {
                state= deserializePlotState((JSONObject)obj);
            }
            return state;
        } catch (ParseException e){
            return null;
        }
    }

    public static PlotState deserializePlotState(JSONObject map) {
        try {
            PlotState state= new PlotState();
            PlotState.MultiImageAction multiImage= StringUtils.getEnum(getStr(map, "multiImage", true),
                    PlotState.MultiImageAction.GUESS);
            PlotState.RotateType rType= StringUtils.getEnum(getStr(map, "rotationType", true),
                    PlotState.RotateType.UNROTATE);
            String rnType= getStr(map,"rotaNorthType", true);
            CoordinateSys rNorthType= rnType!=null ? CoordinateSys.parse(rnType) : CoordinateSys.EQ_J2000;


            state.setContextString(getStr(map, "ctxStr"));
            state.setZoomLevel(getFloat(map,"zoomLevel" ));
            state.setColorTableId(getInt(map, "colorTableId"));


            // optional - default if not passed
            state.setMultiImageAction(multiImage);
            state.setNewPlot(false);
            state.setRotationAngle(getDouble(map, "rotationAngle",true, Double.NaN));
            state.setRotateNorthType(rNorthType);
            state.setRotateType(rType);
            state.setFlippedY(getBoolean(map, "flippedY",false));
            state.setThreeColor(getBoolean(map, "threeColor",false));
            JSONArray opList = (JSONArray)map.get("ops");
            if (opList!=null) {
                for(Object oStr : opList) {
                    state.addOperation( StringUtils.getEnum(oStr.toString(), PlotState.Operation.ROTATE));
                }
            }


            JSONArray pList = (JSONArray)map.get("bandStateAry");
            BandState bandStateAry[]= new BandState[] {null,null,null};
            for(int j= 0; (j<PlotState.MAX_BANDS && j<pList.size());j++) {
                bandStateAry[j]= deserializeBandState((JSONObject)pList.get(j));
            }
            state.setBandStateAry(bandStateAry);

            return state;
        } catch (ClassCastException|IllegalArgumentException  e) {
            return null;
        }
    }


    public static JSONObject serializeBandState(BandState b) {
        JSONObject map = new JSONObject();
        if (b.getWorkingFitsFileStr()!=null) {
            map.put("workingFitsFileStr", b.getWorkingFitsFileStr());
        }
        if (b.getOriginalFitsFileStr()!=null) {
            if (!ComparisonUtil.equals(b.getWorkingFitsFileStr(), b.getOriginalFitsFileStr())) {
                map.put("originalFitsFileStr", b.getOriginalFitsFileStr());
            }
        }
        if (b.getUploadedFileName()!=null) map.put("uploadFileNameStr", b.getUploadedFileName());
        if (b.getImageIdx()>0) map.put("imageIdx", b.getImageIdx());
        if (b.getOriginalImageIdx()>0) map.put("originalImageIdx", b.getOriginalImageIdx());
        if (b.getWebPlotRequestSerialized()!=null) map.put("plotRequestSerialize", b.getWebPlotRequestSerialized());
        if (b.isMultiImageFile()) map.put("multiImageFile", b.isMultiImageFile());
        if (b.isTileCompress()) map.put("tileCompress", b.isTileCompress());
        if (b.getCubeCnt()>0) map.put("cubeCnt", b.getCubeCnt());
        if (b.getCubePlaneNumber()>0) map.put("cubePlaneNumber", b.getCubePlaneNumber());
        map.put("rangeValuesSerialize", b.getRangeValuesSerialized());
        return map;
    }

    public static BandState deserializeBandState(JSONObject map) {
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
            b.setDirectFileAccessData(deserializeClientFitsHeader((JSONObject)map.get("directFileAccessData")));
            b.setBandVisible(true);
//            b.setMultiImageFile((Boolean) map.get("multiImageFile"));
            b.setMultiImageFile(getBoolean(map,"multiImageFile", false));
            b.setCubeCnt(getInt(map,"cubeCnt",0));
            b.setCubePlaneNumber(getInt(map, "cubePlaneNumber",0));
            b.setTileCompress(getBoolean(map,"tileCompress",false));
            return b;
        } catch (ClassCastException|IllegalArgumentException  e) {
            return null;
        }
    }

    static ClientFitsHeader deserializeClientFitsHeader(JSONObject map) {
        if (map==null) return null;
        Map<String,String> tMap= new HashMap<>(30);
        for(Object key : map.keySet()) tMap.put((String)key, map.get(key)+"");
        return new ClientFitsHeader(tMap);
    }

    private static String getStr(JSONObject j, String key) throws IllegalArgumentException, ClassCastException {
        return getStr(j,key,false);
    }


    private static String getStr(JSONObject j, String key, boolean acceptNull) throws IllegalArgumentException, ClassCastException {
        String s= (String)j.get(key);
        if (s==null) {
            if (!acceptNull) throw new IllegalArgumentException(key + " must exist");
        }
        return s;
    }

    private static int getInt(JSONObject j, String key) throws IllegalArgumentException, ClassCastException {
        Number n= (Number)j.get(key);
        if (n==null) throw new IllegalArgumentException(key + " must exist");
        return n.intValue();
    }

    private static int getInt(JSONObject j, String key, int defValue) throws IllegalArgumentException, ClassCastException {
        Number n= (Number)j.get(key);
        if (n==null) return defValue;
        return n.intValue();
    }

    private static float getFloat(JSONObject j, String key) throws IllegalArgumentException, ClassCastException {
        return getFloat(j,key,false, Float.NaN);
    }

    private static float getFloat(JSONObject j, String key, boolean nullAsNan, float defValue) throws IllegalArgumentException, ClassCastException {
        Number n= (Number)j.get(key);
        if (n==null) {
            if (nullAsNan) return Float.NaN;
            else if (Float.isNaN(defValue)) throw new IllegalArgumentException(key + " must exist");
            else return defValue;
        }
        else {
            return n.floatValue();
        }
    }

    private static double getDouble(JSONObject j, String key, boolean nullAsNan, double defValue) throws IllegalArgumentException, ClassCastException {
        Number n= (Number)j.get(key);
        if (n==null) {
            if (nullAsNan) return Double.NaN;
            else if (Double.isNaN(defValue)) throw new IllegalArgumentException(key + " must exist");
            else return defValue;
        }
        else {
            return n.doubleValue();
        }
    }

    private static boolean getBoolean(JSONObject j, String key, boolean defValue) throws IllegalArgumentException, ClassCastException {
        Boolean b= (Boolean)j.get(key);
        if (b==null) {
            return defValue;
        }
        else {
            return b.booleanValue();
        }
    }

}
