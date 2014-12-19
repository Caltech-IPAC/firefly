package edu.caltech.ipac.firefly.data.fuse.provider;
/**
 * User: roby
 * Date: 7/25/14
 * Time: 12:45 PM
 */


import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.fuse.BaseImagePlotDefinition;
import edu.caltech.ipac.firefly.data.fuse.ImagePlotDefinition;
import edu.caltech.ipac.firefly.data.fuse.PlotData;
import edu.caltech.ipac.firefly.data.fuse.ServerRequestBuilder;
import edu.caltech.ipac.firefly.data.fuse.config.SelectedRowData;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.visualize.plot.RangeValues;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.firefly.data.fuse.DatasetInfoConverter.DataVisualizeMode.FITS;
import static edu.caltech.ipac.firefly.data.fuse.DatasetInfoConverter.DataVisualizeMode.FITS_3_COLOR;

/**
 * @author Trey Roby
 */
public class WiseDataSetInfoConverter extends AbstractDataSetInfoConverter {

    private enum ID {WISE_1, WISE_2, WISE_3, WISE_4}
    public static final String WISE_3C= "WISE_3C";
    private static final String bandStr[]= {"1", "2", "3","4"};

    private static final List<String> allIDs=  Arrays.asList(ID.WISE_1.name(), ID.WISE_2.name(), ID.WISE_3.name(), ID.WISE_4.name());

    private BaseImagePlotDefinition imDef= null;


    public WiseDataSetInfoConverter() {
        super(Arrays.asList(FITS, FITS_3_COLOR), new PlotData(new WResolver(),true,false,true), "target");
        getPlotData().set3ColorIDOfIDs(WISE_3C, Arrays.asList(ID.WISE_1.name(), ID.WISE_2.name(), ID.WISE_4.name()));
    }

    public ImagePlotDefinition getImagePlotDefinition() {
        if (imDef==null) {

            HashMap<String,List<String>> vToDMap= new HashMap<String,List<String>> (7);
            vToDMap.put(ID.WISE_1.name(), makeOverlayList("1"));
            vToDMap.put(ID.WISE_2.name(), makeOverlayList("2"));
            vToDMap.put(ID.WISE_3.name(), makeOverlayList("3"));
            vToDMap.put(ID.WISE_4.name(), makeOverlayList("4"));

            PlotData pd= getPlotData();
            pd.setTitle(WISE_3C, "WISE 3 Color");
            pd.setTitle(ID.WISE_1.name(), "WISE 1");
            pd.setTitle(ID.WISE_2.name(), "WISE 2");
            pd.setTitle(ID.WISE_3.name(), "WISE 3");
            pd.setTitle(ID.WISE_4.name(), "WISE 4");

            imDef= new WiseBaseImagePlotDefinition(4,
                                            Arrays.asList(ID.WISE_1.name(), ID.WISE_2.name(),ID.WISE_3.name(),ID.WISE_4.name()),
                                            Arrays.asList(WISE_3C),
                                           vToDMap, ImagePlotDefinition.AUTO_GRID_LAYOUT );
        }
        return imDef;
    }


    private static List<String> makeOverlayList(String b) {
        return Arrays.asList("target","diff_spikes_"+b,"halos_"+b,"ghosts_"+b,"latents_"+b);
    }

    private static List<String> getSelectedViewerIDList(ServerRequest request) {

        List<String> retval= null;
        String bOpStr= request.getParam("band");
        if (bOpStr!=null) {
            retval= new ArrayList<String>(4);
            String opAry[]= bOpStr.split(",");
            for(String op : opAry) {
                try {
                    int idx= Integer.parseInt(op);
                    if (idx>0 && idx<=4) retval.add(allIDs.get(idx-1));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (retval==null || retval.size()==0) retval= allIDs;
        return retval;
    }

    private class WiseBaseImagePlotDefinition extends BaseImagePlotDefinition {

        public WiseBaseImagePlotDefinition(int imageCount,
                                           List<String> viewerIDList,
                                           List<String> threeColorViewerIDList,
                                           Map<String, List<String>> viewerToDrawingLayerMap,
                                           String gridLayout) {
            super(imageCount, viewerIDList, threeColorViewerIDList, viewerToDrawingLayerMap, gridLayout);    //To change body of overridden methods use File | Settings | File Templates.
        }

        @Override
        public List<String> getAllBandOptions(String viewerID) {
            return  Arrays.asList(ID.WISE_1.name(), ID.WISE_2.name(),ID.WISE_3.name(),ID.WISE_4.name());
        }

        public List<String> getViewerIDs(SelectedRowData selData) {
            return getSelectedViewerIDList(selData.getRequest());
        }
    }




    private static class WResolver implements PlotData.Resolver {
        private ServerRequestBuilder builder= new ServerRequestBuilder();
        Map<String,ID> bandToID= new HashMap<String, ID>(5);

        private WResolver() {
            builder.setColumnsToUse(Arrays.asList("scan_id", "frame_num", "coadd_id", "in_ra", "in_dec", "image_set"));
            builder.setColorTableID(1);
            builder.setRangeValues(new RangeValues(RangeValues.SIGMA, -2, RangeValues.SIGMA, 10, RangeValues.STRETCH_LINEAR));
            bandToID.put("1", ID.WISE_1);
            bandToID.put("2", ID.WISE_2);
            bandToID.put("3", ID.WISE_3);
            bandToID.put("4", ID.WISE_4);
        }

        public WebPlotRequest getRequestForID(String id, SelectedRowData selData, boolean useWithThreeColor) {
            List<Param> ep= Collections.emptyList();
            String inter= selData.getRequest().getParam("intersect");
            if (inter!=null && inter.equals("OVERLAPS")) {
                builder.setHeaderParams(Arrays.asList("mission", "ImageSet", "ProductLevel"));
            }
            else {
                builder.setHeaderParams(Arrays.asList("mission", "ImageSet", "ProductLevel", "subsize"));
            }

            ID testID= ID.valueOf(id);
            switch (testID) {
                case WISE_1:
                    ep= Arrays.asList(new Param("band", "1"));
                    break;
                case WISE_2:
                    ep= Arrays.asList(new Param("band", "2"));
                    break;
                case WISE_3:
                    ep= Arrays.asList(new Param("band", "3"));
                    break;
                case WISE_4:
                    ep= Arrays.asList(new Param("band", "4"));
                    break;
            }
            WebPlotRequest wpReq= builder.makeServerRequest("ibe_file_retrieve", id, selData, ep);
            if (useWithThreeColor) wpReq.setTitle("Wise 3 Color");
            return wpReq;
        }


        public List<String> getIDsForMode(PlotData.GroupMode mode, SelectedRowData selData) {
            String b= selData.getSelectedRow().getValue("band");
            if (b!=null && Arrays.asList(bandStr).contains(b)) {
                if (mode== PlotData.GroupMode.TABLE_ROW_ONLY) {
                    return Arrays.asList(bandToID.get(b).name());
                }
                else {
                    return getSelectedViewerIDList(selData.getRequest());
                }
            }
            return null;
        }


        public List<String> get3ColorIDsForMode(SelectedRowData selData) {
            return Arrays.asList(WISE_3C);
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
