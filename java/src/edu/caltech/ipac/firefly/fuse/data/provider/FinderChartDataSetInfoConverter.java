package edu.caltech.ipac.firefly.fuse.data.provider;
/**
 * User: roby
 * Date: 7/25/14
 * Time: 12:45 PM
 */


import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.fuse.data.BaseImagePlotDefinition;
import edu.caltech.ipac.firefly.fuse.data.ImagePlotDefinition;
import edu.caltech.ipac.firefly.fuse.data.config.FinderChartRequestUtil;
import edu.caltech.ipac.firefly.fuse.data.config.SelectedRowData;
import edu.caltech.ipac.firefly.util.Dimension;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.firefly.fuse.data.DatasetInfoConverter.DataVisualizeMode.FITS;
import static edu.caltech.ipac.firefly.fuse.data.DatasetInfoConverter.DataVisualizeMode.FITS_3_COLOR;

/**
 * @author Trey Roby
 */
public class FinderChartDataSetInfoConverter extends AbstractDataSetInfoConverter {


    private BaseImagePlotDefinition imDef= null;
    private static NumberFormat _nf   = NumberFormat.getFormat("#.######");

    private enum ID { DSS1_BLUE, DSS1_RED, DSS2_BLUE, DSS2_RED, DSS2_IR,
                      SDSS_U, SDSS_G, SDSS_R, SDSS_I, SDSS_Z,
                      TWOMASS_J, TWOMASS_H,  TWOMASS_K,
                      WISE_1, WISE_2, WISE_3, WISE_4,
                      IRAS_12,IRAS_25,IRAS_60, IRAS_100 }

    private enum ID3 { DSS1_3,
                       SDSS_3,
                       TWOMASS_3,
                       WISE_3,
                       IRAS_3}

    private List<String> idList= asIDList();
    private Dimension dimension= new Dimension(200,200);


    public FinderChartDataSetInfoConverter() {
        super(Arrays.asList(FITS,FITS_3_COLOR), "target");
    }



    public ImagePlotDefinition getImagePlotDefinition() {
        if (imDef==null) {
            imDef= new FCImagePlotDefinition(idList,
                                             as3ColorIDList(),
                                             makeViewerToLayerMap());
        }
        return imDef;
    }


    @Override
    public void getImageRequest(SelectedRowData selRowData, GroupMode mode, AsyncCallback<Map<String, WebPlotRequest>> cb) {
        Map<String,WebPlotRequest> map= new LinkedHashMap<String, WebPlotRequest>();
        ServerRequest req= selRowData.getRequest();


        String subSizeStr= req.getParam("subsize");
        String sources= req.getParam("sources");
//        String artifactsWise = req.getParam("wise_artifacts");
//        String artifacts2Mass = req.getParam("twomass_artifacts");
        String thumbnailSize= req.getParam("thumbnail_size");

        // use default if not given
        sources = StringUtils.isEmpty(sources) ? "DSS,SDSS,TWOMASS,WISE" : sources;
        subSizeStr = StringUtils.isEmpty(subSizeStr) ? ".08" : subSizeStr;
//        thumbnailSize = StringUtils.isEmpty(thumbnailSize) ? "medium" : thumbnailSize;
        Float subSize = new Float(subSizeStr);


        double ra= Double.parseDouble(selRowData.getSelectedRow().getValue("ra"));
        double dec= Double.parseDouble(selRowData.getSelectedRow().getValue("dec"));
        WorldPt wp= new WorldPt(ra,dec);
        int width= FinderChartRequestUtil.getPlotWidth(req.getParam("thumbnail_size"));


        String bandStr;
        String bands[]=null;

        for(String idStr : idList) map.put(idStr,null);

        for (String serviceStr: sources.split(",")) {
            serviceStr = serviceStr.trim().equalsIgnoreCase("2mass") ? WebPlotRequest.ServiceType.TWOMASS.name() : serviceStr.toUpperCase();
            WebPlotRequest.ServiceType service = WebPlotRequest.ServiceType.valueOf(serviceStr.toUpperCase());
            String bandKey = FinderChartRequestUtil.getBandKey(service);
            if (bandKey!=null) {
                bandStr = getBandKey(bandKey,req);
                if (bandStr !=null) {
                    bands = bandStr.split(",");
                    for (int i=0;i<bands.length;i++) {
                        bands[i]=getComboPair(service, bands[i]);
                    }
                } else {
                    bands = FinderChartRequestUtil.getServiceComboArray(service);
                }
            }


            for (String band: bands) {
                if (service.equals(WebPlotRequest.ServiceType.WISE)) {
                    if (!band.startsWith("3a.")) band = "3a."+band;
                }
//                if (curTarget.getName()==null || curTarget.getName().length()==0) {
//                        TargetFixedSingle fixedSingle = (TargetFixedSingle)curTarget;
//
//
//                        expanded = String.format("%.6f",fixedSingle.getPosition().getRa())
//                                +"+"+String.format("%.6f",fixedSingle.getPosition().getDec());
//                        expanded = expanded.replaceAll("\\+\\-","\\-");
//                } else {
//                    expanded= curTarget.getName();
//                }
//                expanded += (" "+FinderChartRequestUtil.getServiceTitle(service)+" "+
//                        FinderChartRequestUtil.getComboTitle(band));

                String idStr= getID(service,FinderChartRequestUtil.getComboValue(band));
                WebPlotRequest wpReq= FinderChartRequestUtil.makeWebPlotRequest(wp, subSize, width, band, "", service);
                dimension= new Dimension(width,width); // make width & height the same
                map.put(idStr,wpReq);
            }

        }

        cb.onSuccess(map);
    }

    private String getBandKey(String key, ServerRequest r) {
        String retval= r!=null ? r.getParam(key) : null;
        if (retval==null) {
            if (key.equals("dss_bands")) {
                retval= "poss1_blue,poss1_red,poss2ukstu_blue,poss2ukstu_red,poss2ukstu_ir";
            }
            else if (key.equals("iras_bands")) {
                retval= "12,25,60,100";
            }
            else if (key.equals("twomass_bands")) {
                retval= "j,k,h";
            }
            else if (key.equals("wise_bands")) {
                retval= "1,2,3,4";
            }
            else if (key.equals("SDSS_bands")) {
                retval= "u,g,r,i,z";
            }
        }
        return retval;
    }

    private static List<String> asIDList() {
        List<String> retList= new ArrayList<String>(ID.values().length);
        for(ID id : ID.values()) {
            retList.add(id.toString());
        }
        return retList;
    }

    private static List<String> as3ColorIDList() {
        List<String> retList= new ArrayList<String>(ID3.values().length);
        for(ID3 id : ID3.values()) {
            retList.add(id.toString());
        }
        return retList;
    }

    private static Map<String, List<String>> makeViewerToLayerMap() {
        Map<String, List<String>> map= new HashMap<String, List<String>>(31);
        for(ID id : ID.values()) {
            List<String> list= new ArrayList<String>(5);
            list.add("target");
            map.put(id.toString(), list);
        }

        map.get(ID.WISE_1.toString()).addAll(Arrays.asList("diff_spikes_3_1", "halos_1", "ghosts_1", "latents_1" ));
        map.get(ID.WISE_2.toString()).addAll(Arrays.asList("diff_spikes_3_2", "halos_2", "ghosts_2", "latents_2" ));
        map.get(ID.WISE_3.toString()).addAll(Arrays.asList("diff_spikes_3_3", "halos_3", "ghosts_3", "latents_3" ));
        map.get(ID.WISE_4.toString()).addAll(Arrays.asList("diff_spikes_3_4", "halos_4", "ghosts_4", "latents_4" ));

        map.get(ID.TWOMASS_J.toString()).addAll(Arrays.asList("pers_arti_j", "glint_arti_j" ));
        map.get(ID.TWOMASS_H.toString()).addAll(Arrays.asList("pers_arti_h", "glint_arti_h" ));
        map.get(ID.TWOMASS_K.toString()).addAll(Arrays.asList("pers_arti_k", "glint_arti_k" ));
        return map;
    }


    private static String getComboPair(WebPlotRequest.ServiceType service, String key) {
        if (service.equals(WebPlotRequest.ServiceType.WISE) && key!= null) key = "3a."+key;
        for (String combo: FinderChartRequestUtil.getServiceComboArray(service)) {
            if (key!= null && key.equals(FinderChartRequestUtil.getComboValue(combo))) return combo;
        }
        return "";
    }



    @Override
    public void getThreeColorPlotRequest(SelectedRowData selRowData, Map<Band, String> bandOptions, AsyncCallback<Map<String, List<WebPlotRequest>>> callback) {
        Map<String,List<WebPlotRequest>> map= new LinkedHashMap<String, List<WebPlotRequest>>();
        callback.onSuccess(map);
    }



    private static String getID(WebPlotRequest.ServiceType service, String band) {
        String retID= "";
        switch (service) {
            case IRIS:
            case ISSA :
                if      (band.equals("12"))  retID= ID.IRAS_12.name();
                else if (band.equals("25"))  retID= ID.IRAS_25.name();
                else if (band.equals("60"))  retID= ID.IRAS_60.name();
                else if (band.equals("100")) retID= ID.IRAS_100.name();
                break;
            case DSS:
                if      (band.equals("poss1_blue"))      retID= ID.DSS1_BLUE.name();
                else if (band.equals("poss1_red"))       retID= ID.DSS1_RED.name();
                else if (band.equals("poss2ukstu_blue")) retID= ID.DSS2_BLUE.name();
                else if (band.equals("poss2ukstu_red"))  retID= ID.DSS2_RED.name();
                else if (band.equals("poss2ukstu_ir"))   retID= ID.DSS2_IR.name();
                break;
            case SDSS:
                if      (band.equals("u")) retID= ID.SDSS_U.name();
                else if (band.equals("g")) retID= ID.SDSS_G.name();
                else if (band.equals("r")) retID= ID.SDSS_R.name();
                else if (band.equals("i")) retID= ID.SDSS_I.name();
                else if (band.equals("z")) retID= ID.SDSS_Z.name();
                break;
            case TWOMASS:
                if      (band.equals("j")) retID= ID.TWOMASS_J.name();
                else if (band.equals("h")) retID= ID.TWOMASS_H.name();
                else if (band.equals("k")) retID= ID.TWOMASS_K.name();
                break;
            case WISE:
                if      (band.endsWith("1")) retID= ID.WISE_1.name();
                else if (band.endsWith("2")) retID= ID.WISE_2.name();
                else if (band.endsWith("3")) retID= ID.WISE_3.name();
                else if (band.endsWith("4")) retID= ID.WISE_4.name();
                break;
            case MSX:
            case NONE:
            default:
                retID= ID.TWOMASS_J.name();
                break;
        }
        return retID;
    }



    private class FCImagePlotDefinition extends BaseImagePlotDefinition {

        public FCImagePlotDefinition(List<String> viewerIDList,
                                     List<String> threeColorViewerIDList,
                                     Map<String, List<String>> viewerToDrawingLayerMap) {
            super(10, viewerIDList, threeColorViewerIDList, viewerToDrawingLayerMap,
                  BaseImagePlotDefinition.GridLayoutType.FINDER_CHART );
        }

        @Override
        public List<String> getBandOptions(String viewerID) {
            return null;
        }

        @Override
        public Map<Band, String> getBandOptionsDefaults(String viewerID) {
            return null;
        }


        public Dimension getImagePlotDimension() {
            return dimension;
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
