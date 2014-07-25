package edu.caltech.ipac.firefly.fuse.data.config;
/**
 * User: roby
 * Date: 7/24/14
 * Time: 1:07 PM
 */


import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.ui.creator.drawing.ActiveTargetLayer;
import edu.caltech.ipac.firefly.ui.creator.drawing.DatasetDrawingLayerProvider;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.visualize.plot.RangeValues;

import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public interface DatasetInfoConverter {

    enum DisplayType { SPECTRUM, FITS, SPECTRUM_FITS, NONE }
    public DisplayType getType();

    public boolean getCanDo3Color(SelectedRowData selRowData);
    public boolean isLockRelated();
    public void getImagePlotInfo(SelectedRowData selRowData, AsyncCallback<ImagePlotInfo> callback);
    public void getRelatedImagePlotInfo(SelectedRowData selRowData, AsyncCallback<List<ImagePlotInfo>> callback);
    public void getThreeColorPlotInfo(SelectedRowData selRowData, AsyncCallback<Image3ColorPlotInfo> callback);
    public void getSpectrumRequest(SelectedRowData selRowData, AsyncCallback<List<WebPlotRequest>> callback);
    public void getCoverageInfo(TableMeta tableMeta, AsyncCallback<CoverageInfo> callback);

    public ActiveTargetLayer makeActiveTargetLayer();
    public List<DatasetDrawingLayerProvider> makeArtifactLayers();



    public static class CoverageInfo {}

    public static class ImagePlotInfo {
        private final WebPlotRequest request;
        private final List<String> drawingLayerIDs;

        public ImagePlotInfo(WebPlotRequest request, List<String> drawingLayerIDs, RangeValues rangeValues, int colorTable) {
            this.request = request;
            this.drawingLayerIDs = drawingLayerIDs;
            if (colorTable>-1) request.setInitialColorTable(colorTable);
            if (rangeValues!=null) request.setInitialRangeValues(rangeValues);
        }

        public WebPlotRequest getRequest() { return request; }
        public List<String> getDrawingLayerIDs() { return drawingLayerIDs; }
    }

    public static class Image3ColorPlotInfo {
        private final Map<Band,WebPlotRequest> requestMap;
        private final List<String> drawingLayerIDs;

        public Image3ColorPlotInfo(Map<Band, WebPlotRequest> requestMap, List<String> drawingLayerIDs, RangeValues rangeValues) {
            this.requestMap = requestMap;
            this.drawingLayerIDs = drawingLayerIDs;
            for(WebPlotRequest req : requestMap.values()) {
                req.setInitialRangeValues(rangeValues);
            }
        }

        public Map<Band, WebPlotRequest> getRequestMap() { return requestMap; }
        public List<String> getDrawingLayerIDs() { return drawingLayerIDs; }
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
