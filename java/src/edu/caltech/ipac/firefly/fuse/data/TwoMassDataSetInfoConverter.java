package edu.caltech.ipac.firefly.fuse.data;
/**
 * User: roby
 * Date: 7/25/14
 * Time: 12:45 PM
 */


import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.fuse.data.config.SelectedRowData;
import edu.caltech.ipac.firefly.fuse.data.provider.AbstractDataSetInfoConverter;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.ui.creator.drawing.ActiveTargetLayer;
import edu.caltech.ipac.firefly.ui.creator.eventworker.ActiveTargetCreator;
import edu.caltech.ipac.firefly.ui.creator.eventworker.EventWorker;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
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
public class TwoMassDataSetInfoConverter extends AbstractDataSetInfoConverter {


    private static final String bandStr[]= {"j", "h", "k"};
    private ImagePlotDefinition imDef= null;
    ActiveTargetLayer targetLayer= null;


    public TwoMassDataSetInfoConverter() {
        super(Arrays.asList(FITS, FITS_3_COLOR), "2mass_target");
    }

    public ImagePlotDefinition getImagePlotDefinition(TableMeta meta) {
        if (imDef==null) {
            this.setColorTableID(1);
            this.setRangeValues(new RangeValues(RangeValues.SIGMA,-2,RangeValues.SIGMA,10,RangeValues.STRETCH_LINEAR));

            HashMap<String,List<String>> vToDMap= new HashMap<String,List<String>> (7);
            vToDMap.put("2mass_j", makeOverlayList("J"));
            vToDMap.put("2mass_h", makeOverlayList("H"));
            vToDMap.put("2mass_k", makeOverlayList("K"));

            imDef= new TwoMassPlotDefinition(3,Arrays.asList("2mass_j", "2mass_h", "2mass_k"),
                                             Arrays.asList("2mass-three-color"),
                                             vToDMap,
                                             null
                                             );
        }
        return imDef;
    }


    @Override
    public void getImageRequest(SelectedRowData selRowData, GroupMode mode, AsyncCallback<Map<String, WebPlotRequest>> cb) {
        Map<String,WebPlotRequest> map= new HashMap<String, WebPlotRequest>(7);
        String b= selRowData.getSelectedRow().getValue("band");
        String imageURL= selRowData.getSelectedRow().getValue("download");
        if (mode==GroupMode.ROW_ONLY) {
            WebPlotRequest r= WebPlotRequest.makeURLPlotRequest(imageURL, "2 MASS "+b);
            r.setTitle("2MASS: "+b.toLowerCase());
            r.setZoomType(ZoomType.TO_WIDTH);
            map.put("2mass_"+b.toLowerCase(), r);
        }
        else {
            for(int i= 0; (i<3); i++) {
                b= bandStr[i];
                String workingURL= convertTo(imageURL,b);
                WebPlotRequest r= WebPlotRequest.makeURLPlotRequest(workingURL, "2 MASS " + b);
                r.setTitle("2MASS: "+b);
                r.setZoomType(ZoomType.TO_WIDTH);
                map.put("2mass_"+b, r);
            }

        }
        cb.onSuccess(map);
    }


    public void getThreeColorPlotRequest(SelectedRowData selRowData, Map<Band, String> bandOptions, AsyncCallback<Map<String, List<WebPlotRequest>>> callback) {
        Map<String,List<WebPlotRequest>> map= new HashMap<String, List<WebPlotRequest>>(7);
        String b= selRowData.getSelectedRow().getValue("band");
        String imageURL= selRowData.getSelectedRow().getValue("download");

        List<WebPlotRequest> reqList= new ArrayList<WebPlotRequest>(3);

        String workingURL= convertTo(imageURL,bandStr[0]);
        WebPlotRequest red=WebPlotRequest.makeURLPlotRequest(workingURL, "2 MASS Three Color");
        red.setTitle("2MASS: 3 color");
        red.setZoomType(ZoomType.TO_WIDTH);
        reqList.add(red);

        workingURL= convertTo(imageURL,bandStr[1]);
        WebPlotRequest green=WebPlotRequest.makeURLPlotRequest(workingURL, "2 MASS Three Color");
        green.setTitle("2MASS: 3 color");
        green.setZoomType(ZoomType.TO_WIDTH);
        reqList.add(green);

        workingURL= convertTo(imageURL,bandStr[2]);
        WebPlotRequest blue=WebPlotRequest.makeURLPlotRequest(workingURL, "2 MASS Three Color");
        reqList.add(blue);
        blue.setZoomType(ZoomType.TO_WIDTH);
        blue.setTitle("2MASS: 3 color");

        map.put("2mass-three-color", reqList);
        callback.onSuccess(map);
    }




    private String convertTo(String inurl, String band)  {
        int idx= inurl.indexOf("name=");
        StringBuilder sb= new StringBuilder("");
        if (idx>-1) {
            idx+=5;
            sb.append(inurl);
            sb.setCharAt(idx, band.toLowerCase().charAt(0));
        }
        return sb.toString();
    }


    private static List<String> makeOverlayList(String b) {
        return Arrays.asList("2mass_target");
    }


    public ActiveTargetLayer initActiveTargetLayer() {
        if (targetLayer==null) {
            Map<String,String> m= new HashMap<String, String>(5);
            m.put(EventWorker.ID,"2mass_target");
            m.put(CommonParams.TARGET_TYPE,CommonParams.TABLE_ROW);
            m.put(CommonParams.TARGET_COLUMNS, "center_ra,center_dec");
            targetLayer= (ActiveTargetLayer)(new ActiveTargetCreator().create(m));
            Application.getInstance().getEventHub().bind(targetLayer);
            targetLayer.bind(Application.getInstance().getEventHub());
        }
        return targetLayer;
    }




    private static class TwoMassPlotDefinition extends ImagePlotDefinition{

        public TwoMassPlotDefinition(int imageCount,
                                     List<String> viewerIDList,
                                     List<String> threeColorViewerIDList,
                                     Map<String, List<String>> viewerToDrawingLayerMap,
                                     List<List<String>> gridLayout) {
            super(imageCount, viewerIDList, threeColorViewerIDList, viewerToDrawingLayerMap, gridLayout);    //To change body of overridden methods use File | Settings | File Templates.
        }

        @Override
        public List<String> getBandOptions(String viewerID) {
            return Arrays.asList("J", "H", "K");
        }

        @Override
        public Map<Band, String> getBandOptionsDefaults(String viewerID) {
            HashMap<Band,String> map= new HashMap<Band, String>(5);
            map.put(Band.RED,"J");
            map.put(Band.GREEN,"H");
            map.put(Band.BLUE,"K");
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
