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
import edu.caltech.ipac.firefly.visualize.WebPlotInitializer;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.RangeValues;
import edu.caltech.ipac.visualize.plot.projection.Projection;
import edu.caltech.ipac.visualize.plot.projection.ProjectionParams;
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




    public static String serializeWebPlotInitializerShallow(WebPlotInitializer wpInit) {
        return wpInit.toString();
    }

    public static JSONObject serializeWebPlotInitializerDeep(WebPlotInitializer wpInit) {
        JSONObject map = new JSONObject();
        map.put("JSON", true);

        map.put("imageCoordSys", wpInit.getCoordinatesOfPlot().toString());
        map.put("projectionJson", serializeProjection(wpInit));
        map.put("relatedData", serializeRelatedDataArray(wpInit.getRelatedData()));
        map.put("dataWidth", wpInit.getDataWidth());
        map.put("dataHeight", wpInit.getDataHeight());
        map.put("imageScaleFactor", wpInit.getImageScaleFactor());
        map.put("initImages", serializePlotImages(wpInit.getInitImages()));
        map.put("plotState", serializePlotState(wpInit.getPlotState()));
        map.put("desc", wpInit.getPlotDesc());
        map.put("dataDesc", wpInit.getDataDesc());

        JSONArray ary= new JSONArray();

        for(WebFitsData wfd : wpInit.getFitsData()) ary.add(serializeWebFitsData(wfd));
        map.put("fitsData", ary);

        return map;
    }

    public static JSONArray serializeRelatedDataArray(List<RelatedData> relatedData) {
        if (relatedData==null || relatedData.size()==0) return null;
        JSONArray relatedArray= new JSONArray();
        for(RelatedData r : relatedData) relatedArray.add(serializeRelated(r));
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

    public static JSONObject serializeProjection(WebPlotInitializer wpInit) {
        Projection proj= wpInit.getProjection();
        if (proj==null) return null;
        JSONObject map = new JSONObject();
        map.put("coorindateSys", proj.getCoordinateSys().toString());
        map.put("header", serializeProjectionParams(proj.getProjectionParams()));
        return map;
    }

    public static JSONObject serializeProjectionParams(ProjectionParams p) {
        JSONObject map = new JSONObject();

        map.put("bitpix", p.bitpix);
        map.put("naxis",  p.naxis);
        map.put("naxis1", p.naxis1);
        map.put("naxis2", p.naxis2);
        map.put("naxis3", p.naxis3);
        map.put("crpix1", p.crpix1);
        map.put("crpix2", p.crpix2);
        map.put("crval1", p.crval1);
        map.put("crval2", p.crval2);
        map.put("cdelt1", p.cdelt1);
        map.put("cdelt2", p.cdelt2);
        map.put("crota1", p.crota1);
        map.put("crota2", p.crota2);
        map.put("file_equinox", p.file_equinox);
        map.put("ctype1", p.ctype1);
        map.put("ctype2", p.ctype2);
        map.put("radecsys", p.radecsys);
        map.put("datamax", p.datamax);
        map.put("datamin", p.datamin);
        map.put("maptype", p.maptype);
        map.put("cd1_1", p.cd1_1);
        map.put("cd1_2", p.cd1_2);
        map.put("cd2_1", p.cd2_1);
        map.put("cd2_2", p.cd2_2);
        map.put("dc1_1", p.dc1_1);
        map.put("dc1_2", p.dc1_2);
        map.put("dc2_1", p.dc2_1);
        map.put("dc2_2", p.dc2_2);
        map.put("using_cd", p.using_cd);
        map.put("plate_ra", p.plate_ra);
        map.put("plate_dec", p.plate_dec);
        map.put("x_pixel_offset", p.x_pixel_offset);
        map.put("y_pixel_offset", p.y_pixel_offset);
        map.put("x_pixel_size", p.x_pixel_size);
        map.put("y_pixel_size", p.y_pixel_size);
        map.put("plt_scale", p.plt_scale);

        map.put("ppo_coeff", makeJAry(p.ppo_coeff));
        map.put("amd_x_coeff", makeJAry(p.amd_x_coeff));
        map.put("amd_y_coeff", makeJAry(p.amd_y_coeff));
        map.put("a_order", p.a_order);
        map.put("ap_order", p.ap_order);
        map.put("b_order", p.b_order);
        map.put("bp_order", p.bp_order);
        map.put("a", makeJAry2d(p.a));
        map.put("ap", makeJAry2d(p.ap));
        map.put("b", makeJAry2d(p.b));
        map.put("bp", makeJAry2d(p.bp));
        map.put("map_distortion", p.map_distortion);
        map.put("keyword", p.keyword);

        for(Map.Entry<String,String> e : p.sendToClientHeaders.entrySet() ) {
            map.put(e.getKey(),e.getValue());
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
        map.put("JSON", true);
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
        map.put("JSON", true);

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
            imageMap.put("xoff", image.getXoff());
            imageMap.put("yoff", image.getYoff());
            imageMap.put("width", image.getWidth());
            imageMap.put("height", image.getHeight());
            imageMap.put("index", image.getIndex());
            imageMap.put("created", image.isCreated());
            imageAry.add(imageMap);
        }
        map.put("images", imageAry);
        return map;
    }

    public static JSONObject serializeWebFitsData(WebFitsData wfData) {
        if (wfData==null) return null;
        JSONObject map = new JSONObject();
        map.put("JSON", true);
        map.put("dataMin", wfData.getDataMin());
        map.put("dataMax", wfData.getDataMax());
        map.put("beta", wfData.getBeta());
        map.put("fluxUnits", wfData.getFluxUnits());
        map.put("getFitsFileSize", wfData.getFitsFileSize());
        return map;
    }

    public static  JSONObject serializePlotState(PlotState s) {
        if (s==null) return null;

        JSONObject map = new JSONObject();

        map.put("JSON", true);
        map.put("multiImage", s.getMultiImageAction().toString());
        map.put("ctxStr", s.getContextString());
        map.put("newPlot", s.isNewPlot());
        map.put("zoomLevel", s.getZoomLevel());
        map.put("threeColor", s.isThreeColor());
        map.put("colorTableId", s.getColorTableId());
        map.put("rotationType", s.getRotateType().toString());
        map.put("rotationAngle", s.getRotationAngle());
        map.put("flippedY", s.isFlippedY());
        map.put("rotaNorthType", s.getRotateNorthType().toString());

        JSONArray list = new JSONArray();
        BandState bandStateAry[]= s.getBandStateAry();
        for(int i= 0; (i< bandStateAry.length); i++) {
            list.add(bandStateAry[i]==null || !bandStateAry[i].hasRequest() ?
                                                   null : serializeBandState(bandStateAry[i]));
        }
        map.put("bandStateAry", list);



        JSONArray outOpList = new JSONArray();
        for(PlotState.Operation op  : s.getOperations()) {
            outOpList.add(op.toString());
        }
        map.put("ops", outOpList);


        return map;
    }



    public static StretchData deserializeStretchDataFromString(String s, boolean asJson) {
        if (s==null) return null;
        if (!asJson) return StretchData.parse(s);
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
        if (!s.contains("JSON")) return PlotState.parse(s);
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
            PlotState.MultiImageAction multiImage= StringUtils.getEnum(getStr(map, "multiImage"),
                    PlotState.MultiImageAction.GUESS);
            PlotState.RotateType rType= StringUtils.getEnum(getStr(map, "rotationType"),
                    PlotState.RotateType.UNROTATE);
            CoordinateSys rNorthType= CoordinateSys.parse(getStr(map,"rotaNorthType"));



            state.setMultiImageAction(multiImage);
            state.setContextString(getStr(map, "ctxStr"));
            state.setNewPlot((Boolean)map.get("newPlot"));
            state.setZoomLevel(getFloat(map,"zoomLevel" ));
            state.setThreeColor((Boolean)map.get("threeColor"));
            state.setColorTableId(getInt(map, "colorTableId"));
            state.setRotateType(rType);
            state.setRotationAngle(getDouble(map, "rotationAngle",true));
            state.setFlippedY((Boolean)map.get("flippedY"));
            state.setRotateNorthType(rNorthType);



            JSONArray pList = (JSONArray)map.get("bandStateAry");
            BandState bandStateAry[]= new BandState[] {null,null,null};
            for(int j= 0; (j<PlotState.MAX_BANDS && j<pList.size());j++) {
                bandStateAry[j]= deserializeBandState((JSONObject)pList.get(j));
            }
            state.setBandStateAry(bandStateAry);

            JSONArray opList = (JSONArray)map.get("ops");
            for(Object oStr : opList) {
                state.addOperation( StringUtils.getEnum(oStr.toString(), PlotState.Operation.ROTATE));
            }
            return state;
        } catch (ClassCastException|IllegalArgumentException  e) {
            return null;
        }
    }


    public static JSONObject serializeBandState(BandState b) {
        JSONObject map = new JSONObject();
        map.put("JSON", true);
        map.put("workingFitsFileStr", b.getWorkingFitsFileStr());
        map.put("originalFitsFileStr", b.getOriginalFitsFileStr());
        map.put("uploadFileNameStr", b.getUploadedFileName());
        map.put("imageIdx", b.getImageIdx());
        map.put("originalImageIdx", b.getOriginalImageIdx());
        map.put("plotRequestSerialize", b.getWebPlotRequestSerialized());
        map.put("rangeValuesSerialize", b.getRangeValuesSerialized());
        map.put("fitsHeader", serializeClientFitsHeader(b.getHeader()));
        map.put("bandVisible", b.isBandVisible());
        map.put("multiImageFile", b.isMultiImageFile());
        map.put("cubeCnt", b.getCubeCnt());
        map.put("cubePlaneNumber", b.getCubePlaneNumber());
        return map;
    }

    public static BandState deserializeBandState(JSONObject map) {
        if (map==null) return null;
        try {
            BandState b= new BandState();
            b.setWorkingFitsFileStr(getStr(map, "workingFitsFileStr"));
            b.setOriginalFitsFileStr(getStr(map,"originalFitsFileStr"));
            b.setUploadedFileName(getStr(map,"uploadFileNameStr",true));
            b.setImageIdx(getInt(map, "imageIdx"));
            b.setOriginalImageIdx(getInt(map,"originalImageIdx"));
            b.setWebPlotRequest(WebPlotRequest.parse(getStr(map, "plotRequestSerialize")));
            b.setRangeValues(RangeValues.parse(getStr(map,"rangeValuesSerialize")));
            b.setFitsHeader(deserializeClientFitsHeader((JSONObject)map.get("fitsHeader")));
            b.setBandVisible((Boolean) map.get("bandVisible"));
            b.setMultiImageFile((Boolean) map.get("multiImageFile"));
            b.setCubeCnt(getInt(map,"cubeCnt"));
            b.setCubePlaneNumber(getInt(map, "cubePlaneNumber"));
            return b;
        } catch (ClassCastException|IllegalArgumentException  e) {
            return null;
        }
    }



    public static JSONObject serializeClientFitsHeader(ClientFitsHeader cfH) {
        if (cfH==null) return null;
        JSONObject map = new JSONObject();
        for(String k : cfH) {
            map.put(k,cfH.getStringHeader(k));
        }
        return map;
    }

    public static ClientFitsHeader deserializeClientFitsHeader(JSONObject map) {
        if (map==null) return null;
        try {
            Map<String,String> tMap= new HashMap<String, String>(30);

            for(Object key : map.keySet()) {
                tMap.put((String)key, (String)map.get(key));
            }
            return new ClientFitsHeader(tMap);
        } catch (ClassCastException|IllegalArgumentException  e) {
            return null;
        }
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

    private static float getFloat(JSONObject j, String key) throws IllegalArgumentException, ClassCastException {
        return getFloat(j,key,false);
    }

    private static float getFloat(JSONObject j, String key, boolean nullAsNan) throws IllegalArgumentException, ClassCastException {
        Number n= (Number)j.get(key);
        if (n==null) {
            if (nullAsNan) return Float.NaN;
            else throw new IllegalArgumentException(key + " must exist");
        }
        else {
            return n.floatValue();
        }
    }

    private static double getDouble(JSONObject j, String key, boolean nullAsNan) throws IllegalArgumentException, ClassCastException {
        Number n= (Number)j.get(key);
        if (n==null) {
            if (nullAsNan) return Double.NaN;
            else throw new IllegalArgumentException(key + " must exist");
        }
        else {
            return n.doubleValue();
        }
    }


}
