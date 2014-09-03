package edu.caltech.ipac.firefly.fuse.data.provider;
/**
 * User: roby
 * Date: 7/25/14
 * Time: 12:45 PM
 */


import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.fuse.data.BaseImagePlotDefinition;
import edu.caltech.ipac.firefly.fuse.data.ImagePlotDefinition;
import edu.caltech.ipac.firefly.fuse.data.config.SelectedRowData;
import edu.caltech.ipac.firefly.ui.creator.drawing.ActiveTargetLayer;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.visualize.plot.RangeValues;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.firefly.fuse.data.DatasetInfoConverter.DataVisualizeMode.FITS;
import static edu.caltech.ipac.firefly.fuse.data.DatasetInfoConverter.DataVisualizeMode.FITS_3_COLOR;

/**
 * @author Trey Roby
 */
public class WiseDataSetInfoConverter extends AbstractDataSetInfoConverter {


    private BaseImagePlotDefinition imDef= null;
    ActiveTargetLayer targetLayer= null;


    public WiseDataSetInfoConverter() {
        super(Arrays.asList(FITS, FITS_3_COLOR), "target");
    }

    public ImagePlotDefinition getImagePlotDefinition() {
        if (imDef==null) {
            this.setColumnsToUse(Arrays.asList("scan_id", "frame_num", "coadd_id", "in_ra", "in_dec", "image_set"));
            this.setHeaderParams(Arrays.asList("mission","ImageSet","ProductLevel","subsize"));
            this.setColorTableID(1);
            this.setRangeValues(new RangeValues(RangeValues.SIGMA,-2,RangeValues.SIGMA,10,RangeValues.STRETCH_LINEAR));

            HashMap<String,List<String>> vToDMap= new HashMap<String,List<String>> (7);
            vToDMap.put("wise_1", makeOverlayList("1"));
            vToDMap.put("wise_2", makeOverlayList("2"));
            vToDMap.put("wise_3", makeOverlayList("3"));
            vToDMap.put("wise_4", makeOverlayList("4"));


            imDef= new WiseBaseImagePlotDefinition(4,Arrays.asList("wise_1", "wise_2","wise_3" ,"wise_4" ),
                                               Arrays.asList("wise_3color"),
                                           vToDMap, BaseImagePlotDefinition.GridLayoutType.AUTO );
        }
        return imDef;
    }


    @Override
    public void getImageRequest(SelectedRowData selRowData, GroupMode mode, AsyncCallback<Map<String, WebPlotRequest>> cb) {
        Map<String,WebPlotRequest> map= new HashMap<String, WebPlotRequest>(7);
        String b= selRowData.getSelectedRow().getValue("band");
        if (mode==GroupMode.TABLE_ROW_ONLY) {
            WebPlotRequest r= makeServerRequest("ibe_file_retrieve", "WISE Band " + b,
                                                selRowData, Arrays.asList(new Param("band",b)));
            map.put("wise_"+b, r);
        }
        else {
            for(int i= 0; (i<4); i++) {
                b= (i+1)+"";
                WebPlotRequest r= makeServerRequest("ibe_file_retrieve", "WISE Band "+b,
                                                    selRowData,Arrays.asList(new Param("band",b)));
                map.put("wise_"+b, r);
            }

        }
        cb.onSuccess(map);
    }




    private static List<String> makeOverlayList(String b) {
        return Arrays.asList("target","diff_spikes_"+b,"halos_"+b,"ghosts_"+b,"latents_"+b);
    }


    @Override
    public void getThreeColorPlotRequest(SelectedRowData selRowData, Map<Band, String> bandOptions, AsyncCallback<Map<String, List<WebPlotRequest>>> callback) {
        Map<String,List<WebPlotRequest>> map= new HashMap<String, List<WebPlotRequest>>(1);
        String bands[]= {"1","2","4"};
        List<WebPlotRequest> rList= new ArrayList<WebPlotRequest>(3);
        for(String b : bands) {
            rList.add( makeServerRequest("ibe_file_retrieve", "WISE Band "+b,
                                                    selRowData,Arrays.asList(new Param("band",b)))
            );

        }
        map.put("wise_3color", rList);
        callback.onSuccess(map);
    }

    private static class WiseBaseImagePlotDefinition extends BaseImagePlotDefinition {

        public WiseBaseImagePlotDefinition(int imageCount,
                                           List<String> viewerIDList,
                                           List<String> threeColorViewerIDList,
                                           Map<String, List<String>> viewerToDrawingLayerMap,
                                           GridLayoutType gridLayout) {
            super(imageCount, viewerIDList, threeColorViewerIDList, viewerToDrawingLayerMap, gridLayout);    //To change body of overridden methods use File | Settings | File Templates.
        }

        @Override
        public List<String> getBandOptions(String viewerID) {
            return Arrays.asList("Band 1", "Band 2", "Band 3", "Band 4");
        }

        @Override
        public Map<Band, String> getBandOptionsDefaults(String viewerID) {
            HashMap<Band,String> map= new HashMap<Band, String>(5);
            map.put(Band.RED,"Band 1");
            map.put(Band.GREEN,"Band 2");
            map.put(Band.BLUE,"Band 3");
            return map;
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
