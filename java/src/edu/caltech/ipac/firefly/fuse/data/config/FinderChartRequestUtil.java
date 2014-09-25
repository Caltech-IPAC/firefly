package edu.caltech.ipac.firefly.fuse.data.config;
/**
 * User: roby
 * Date: 9/2/14
 * Time: 10:36 AM
 */


import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.HashMap;

/**
 * @author Trey Roby
 */
public class FinderChartRequestUtil {


    private static HashMap<WebPlotRequest.ServiceType, String> serviceTitleMap = null;
    private static HashMap<WebPlotRequest.ServiceType, String> bandMap = null;
    private static HashMap<WebPlotRequest.ServiceType, String[]> comboMap = null;
//    private static HashMap<WebPlotRequest.ServiceType, String[]> comboMap = null;

    /**
     * Finder Chart services
     * combo string format: option;title
     */
    private final static String dssCombo[]={
            "poss1_blue;DSS1 Blue",
            "poss1_red;DSS1 Red",
            "poss2ukstu_blue;DSS2 Blue",
            "poss2ukstu_red;DSS2 Red",
            "poss2ukstu_ir;DSS2 IR",
            /*"quickv;Quick-V Survey",
            "phase2_gsc2;HST Phase 2 Target Positioning(GSC 2)",
            "phase2_gsc1;HST Phase 1 Target Positioning(GSC 1)",
            "all;The best of a combined list of all plates"*/};

    private final static String twoMassCombo[] = {
            "j;J",
            "h;H",
            "k;K", };

    private final static String issaCombo[]= {
            "12;12 microns",
            "25;25 microns",
            "60;60 microns",
            "100;100 microns"};

    private final static String irisCombo[]= {
            "12;12 microns",
            "25;25 microns",
            "60;60 microns",
            "100;100 microns"};

    private final static String msxCombo[] = {
            "3;A (8.28 microns)",
            "4;C (12.13 microns)",
            "5;D (14.65 microns)",
            "6;E (21.3 microns)"};

    private final static String wiseCombo[]={
            "3a.1;w1",
            "3a.2;w2",
            "3a.3;w3",
            "3a.4;w4"};

    private final static String sDssCombo[]={
            "u;u","g;g","r;r","i;i","z;z"
    };

    private static HashMap<String, Integer> thumbnailSizeMap = new HashMap<String, Integer>() {
        {
            put("small",128);
            put("medium",192);
            put("large",256);
        }
    };




    public static WebPlotRequest makeWebPlotRequest(WorldPt pt,
                                                    float radius,
                                                    int width,
                                                    String band,
                                                    String expandedTitle,
                                                    WebPlotRequest.ServiceType service ) {

        WebPlotRequest wpReq= getWebPlotRequest(service, band, pt, radius);
//        wpReq.setExpandedTitle(expandedTitle);
        wpReq.setZoomType(ZoomType.TO_WIDTH);
        wpReq.setZoomToWidth(width);
        wpReq.setZoomToHeight(width); // set width and height the same
        wpReq.setPostCropAndCenter(true);
        wpReq.setRotateNorth(true);
        wpReq.setSaveCorners(true);
        wpReq.setInitialColorTable(1);
        wpReq.setHideTitleDetail(true);
        wpReq.setPreferenceColorKey("FcColorKey");
        wpReq.setTitleOptions(WebPlotRequest.TitleOptions.SERVICE_OBS_DATE);
        wpReq.setTitle(getComboTitle(band));
        return wpReq;
    }


    private static WebPlotRequest getWebPlotRequest(WebPlotRequest.ServiceType service, String band, WorldPt pt, Float radius) {
        WebPlotRequest wpReq=null;
        switch (service) {
            case DSS:
                wpReq= WebPlotRequest.makeDSSRequest(pt, getComboValue(band),radius);
                wpReq.setDrawingSubGroupId("dss");
                break;
            case IRIS:
                wpReq= WebPlotRequest.makeIRISRequest(pt, getComboValue(band),radius);
                wpReq.setDrawingSubGroupId("iris");
                break;
            case ISSA:
                wpReq= WebPlotRequest.makeISSARequest(pt, getComboValue(band),radius);
                wpReq.setDrawingSubGroupId("issa");
                break;
            case MSX:
                wpReq= WebPlotRequest.makeMSXRequest(pt, getComboValue(band),radius);
                wpReq.setDrawingSubGroupId("msx");
                break;
            case SDSS:
                wpReq= WebPlotRequest.makeSloanDSSRequest(pt, getComboValue(band), radius);
                wpReq.setDrawingSubGroupId("sdss");
                break;
            case TWOMASS:
                wpReq= WebPlotRequest.make2MASSRequest(pt, getComboValue(band),radius);
                wpReq.setDrawingSubGroupId("2mass");
                break;
            case WISE:
                String[] pair= getComboValue(band).split("\\.");
                wpReq= WebPlotRequest.makeWiseRequest(pt, pair[0], pair[1], radius);
                wpReq.setDrawingSubGroupId("wise");
                break;
        }
        return wpReq;
    }

    public static String getComboValue(String combo) {
        String sAry[]= combo.split(";");
        return sAry.length>0 ? sAry[0] : combo;
    }
    public static String getComboTitle(String combo) {
        String sAry[]= combo.split(";");
        return sAry.length>1 ? sAry[1] : combo;
    }



    public static String[] getServiceComboArray(WebPlotRequest.ServiceType key) {
        if (comboMap == null) {
            comboMap= new HashMap<WebPlotRequest.ServiceType, String[]>();
            comboMap.put(WebPlotRequest.ServiceType.DSS,dssCombo);
            comboMap.put(WebPlotRequest.ServiceType.IRIS,irisCombo);
            comboMap.put(WebPlotRequest.ServiceType.ISSA,issaCombo);
            comboMap.put(WebPlotRequest.ServiceType.MSX,msxCombo);
            comboMap.put(WebPlotRequest.ServiceType.SDSS,sDssCombo);
            comboMap.put(WebPlotRequest.ServiceType.TWOMASS,twoMassCombo);
            comboMap.put(WebPlotRequest.ServiceType.WISE,wiseCombo);
        }
        return comboMap.get(key);
    }

    public static String getServiceTitle(WebPlotRequest.ServiceType key) {
        if (serviceTitleMap == null) {
            serviceTitleMap= new HashMap<WebPlotRequest.ServiceType, String>();
            serviceTitleMap.put(WebPlotRequest.ServiceType.DSS, WebPlotRequest.ServiceType.DSS.toString());
            serviceTitleMap.put(WebPlotRequest.ServiceType.IRIS, "IRAS (IRIS)");
            serviceTitleMap.put(WebPlotRequest.ServiceType.ISSA, WebPlotRequest.ServiceType.ISSA.toString());
            serviceTitleMap.put(WebPlotRequest.ServiceType.MSX, WebPlotRequest.ServiceType.MSX.toString());
            serviceTitleMap.put(WebPlotRequest.ServiceType.SDSS, WebPlotRequest.ServiceType.SDSS.toString());
            serviceTitleMap.put(WebPlotRequest.ServiceType.TWOMASS, "2MASS");
            serviceTitleMap.put(WebPlotRequest.ServiceType.WISE, WebPlotRequest.ServiceType.WISE.toString());
            serviceTitleMap.put(WebPlotRequest.ServiceType.SDSS, WebPlotRequest.ServiceType.SDSS.toString());
        }

        return serviceTitleMap.get(key);
    }

    public static String getBandKey(WebPlotRequest.ServiceType key) {
        if (bandMap==null) {
            bandMap = new HashMap<WebPlotRequest.ServiceType, String>();
            bandMap.put(WebPlotRequest.ServiceType.DSS, "dss_bands");
            bandMap.put(WebPlotRequest.ServiceType.IRIS, "iras_bands");
            bandMap.put(WebPlotRequest.ServiceType.TWOMASS, "twomass_bands");
            bandMap.put(WebPlotRequest.ServiceType.WISE, "wise_bands");
            bandMap.put(WebPlotRequest.ServiceType.SDSS, "sdss_bands");
        }
        return bandMap.get(key);
    }



    public static int getPlotWidth(String sizeKey) {
        if (sizeKey!=null && thumbnailSizeMap.containsKey(sizeKey))  {
            return thumbnailSizeMap.get(sizeKey);
        }
        else {
            return thumbnailSizeMap.get("medium");
        }
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
