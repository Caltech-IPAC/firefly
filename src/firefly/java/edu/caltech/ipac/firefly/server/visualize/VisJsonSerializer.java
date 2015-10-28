/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 10/12/15
 * Time: 4:10 PM
 */


import edu.caltech.ipac.firefly.visualize.BandState;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.firefly.visualize.ClientFitsHeader;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.RangeValues;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class VisJsonSerializer {


    private static final JSONParser parser= new JSONParser();


    public static String serializePlotState(PlotState s, boolean useJson) {
        if (s==null) return null;
        return useJson ? serializePlotState(s) : s.serialize();
    }



    public static String serializePlotState(PlotState s) {

        JSONObject map = new JSONObject();

        map.put("JSON", true);
        map.put("multiImage", s.getMultiImageAction());
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
            list.add(bandStateAry[i]==null ? null : serializeBandState(bandStateAry[i]));
        }
        map.put("bandStateAry", list);



        JSONArray outOpList = new JSONArray();
        for(PlotState.Operation op  : s.getOperations()) {
            outOpList.add(op.toString());
        }
        map.put("ops", outOpList);


        return map.toString();
    }


    public static PlotState deserializePlotState(String s) {
        if (s==null || !s.contains("JSON")) return null;
        PlotState state= null;
        try {
            Object obj= parser.parse(s);
            if (obj!=null && obj instanceof JSONObject) {
                JSONObject map= (JSONObject)obj;
                state= new PlotState();
                PlotState.MultiImageAction multiImage= StringUtils.getEnum(getStr(map, "workingFitsFileStr"),
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
                state.setRotationAngle(getDouble(map, "rotationAngle"));
                state.setFlippedY((Boolean)map.get("flippedY"));
                state.setRotateNorthType(rNorthType);



                JSONArray pList = (JSONArray)map.get("bandStateAry");
                BandState bandStateAry[]= new BandState[] {null,null,null};
                for(int j= 0; (j<PlotState.MAX_BANDS && j<pList.size());j++) {
                    bandStateAry[j]= deserializeBandState((String)pList.get(j));
                }

                JSONArray opList = (JSONArray)map.get("ops");
                for(Object oStr : opList) {
                    state.addOperation( StringUtils.getEnum(oStr.toString(), PlotState.Operation.ROTATE));
                }
            }
        } catch (ParseException e) {
            // return null
        } catch (ClassCastException e) {
            // return null
        } catch (IllegalArgumentException e) {
            // return null
        }
        return state;

    }


    public static String serializeBandState(BandState b) {
        JSONObject map = new JSONObject();
        map.put("JSON", true);
        map.put("workingFitsFileStr", b.getWorkingFitsFileStr());
        map.put("originalFitsFileStr", b.getOriginalFitsFileStr());
        map.put("uploadFileNameStr", b.getUploadedFileName());
        map.put("imageIdx", b.getImageIdx());
        map.put("originalImageIdx ", b.getOriginalImageIdx());
        map.put("plotRequestSerialize", b.getWebPlotRequestSerialized());
        map.put("rangeValuesSerialize", b.getRangeValuesSerialized());
        map.put("fitsHeader", serializeClientFitsHeader(b.getHeader()));
        map.put("bandVisible", b.isBandVisible());
        map.put("multiImageFile ", b.isMultiImageFile());
        map.put("cubeCnt ", b.getCubeCnt());
        map.put("cubePlaneNumber", b.getCubePlaneNumber());
        return map.toString();
    }

    public static BandState deserializeBandState(String s) {
        if (s==null || !s.contains("JSON")) return null;
        BandState b= null;
        try {
            Object obj= parser.parse(s);
            if (obj!=null && obj instanceof JSONObject) {
                JSONObject map= (JSONObject)obj;
                b= new BandState();
                b.setWorkingFitsFileStr(getStr(map, "workingFitsFileStr"));
                b.setOriginalFitsFileStr(getStr(map,"originalFitsFileStr"));
                b.setUploadedFileName(getStr(map,"uploadFileNameStr"));
                b.setImageIdx(getInt(map, "imageIdx"));
                b.setOriginalImageIdx(getInt(map,"originalImageIdx "));
                b.setWebPlotRequest(WebPlotRequest.parse(getStr(map, "plotRequestSerialize")));
                b.setRangeValues(RangeValues.parse(getStr(map,"rangeValuesSerialize")));
                b.setFitsHeader(deserializeClientFitsHeader(getStr(map, "fitsHeader")));
                b.setBandVisible((Boolean) map.get("bandVisible"));
                b.setMultiImageFile((Boolean) map.get("multiImageFile "));
                b.setCubeCnt((Integer)map.get("cubeCnt "));
                b.setCubePlaneNumber(getInt(map, "cubePlaneNumber"));
            }
        } catch (ParseException e) {
            // return null
        } catch (ClassCastException e) {
            // return null
        } catch (IllegalArgumentException e) {
            // return null
        }
        return b;
    }



    public static String serializeClientFitsHeader(ClientFitsHeader cfH) {
        JSONObject map = new JSONObject();
        for(String k : cfH) {
            map.put(k,cfH.getStringHeader(k));
        }
        return map.toString();
    }

    public static ClientFitsHeader deserializeClientFitsHeader(String s) {
        if (s==null) return null;
        try {
            Object obj= parser.parse(s);
            if (obj!=null && obj instanceof JSONObject) {
                JSONObject map= (JSONObject)obj;
                Map<String,String> tMap= new HashMap<String, String>(30);

                for(Object key : map.keySet()) {
                    tMap.put((String)key, (String)map.get(key));
                }
                return new ClientFitsHeader(tMap);
            }
        } catch (ParseException e) {
            // return null
        } catch (ClassCastException e) {
            // return null
        } catch (IllegalArgumentException e) {
            // return null
        }
        return null;
    }



    private static String getStr(JSONObject j, String key) throws IllegalArgumentException, ClassCastException {
        String s= (String)j.get(key);
        if (s==null) throw new IllegalArgumentException(key + " must exist");
        return s;
    }

    private static int getInt(JSONObject j, String key) throws IllegalArgumentException, ClassCastException {
        Number n= (Number)j.get(key);
        if (n==null) throw new IllegalArgumentException(key + " must exist");
        return n.intValue();
    }

    private static float getFloat(JSONObject j, String key) throws IllegalArgumentException, ClassCastException {
        Number n= (Number)j.get(key);
        if (n==null) throw new IllegalArgumentException(key + " must exist");
        return n.floatValue();
    }

    private static double getDouble(JSONObject j, String key) throws IllegalArgumentException, ClassCastException {
        Number n= (Number)j.get(key);
        if (n==null) throw new IllegalArgumentException(key + " must exist");
        return n.doubleValue();
    }


}
