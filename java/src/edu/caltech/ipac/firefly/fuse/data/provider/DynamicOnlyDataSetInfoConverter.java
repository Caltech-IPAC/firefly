package edu.caltech.ipac.firefly.fuse.data.provider;
/**
 * User: roby
 * Date: 7/25/14
 * Time: 12:45 PM
 */


import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.fuse.data.DatasetInfoConverter;
import edu.caltech.ipac.firefly.fuse.data.DynamicPlotData;
import edu.caltech.ipac.firefly.fuse.data.ImagePlotDefinition;
import edu.caltech.ipac.firefly.fuse.data.config.SelectedRowData;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.ui.creator.drawing.ActiveTargetLayer;
import edu.caltech.ipac.firefly.ui.creator.drawing.DatasetDrawingLayerProvider;
import edu.caltech.ipac.firefly.ui.creator.eventworker.ActiveTargetCreator;
import edu.caltech.ipac.firefly.ui.creator.eventworker.EventWorker;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Trey Roby
 */
public class DynamicOnlyDataSetInfoConverter implements DatasetInfoConverter {


    private ActiveTargetLayer targetLayer= null;
    private final String activeTargetLayerName= "target";
    private final DynamicPlotData dynPlotData= new DynamicPlotData();
    private final DynImagePlotDefinition imagePlotDefinition= new DynImagePlotDefinition();


    public DynamicPlotData getDynamicData() {
        return dynPlotData;
    }

    public Set<DataVisualizeMode> getDataVisualizeModes() { return new HashSet<DataVisualizeMode>(); }
    public boolean isSupport(DataVisualizeMode mode) { return false; }

    public boolean is3ColorOptional() { return true; }

    public void getImageRequest(SelectedRowData selRowData, GroupMode mode, AsyncCallback<Map<String, WebPlotRequest>> callback) {
        callback.onSuccess(null);
    }

    public void getThreeColorPlotRequest(SelectedRowData selRowData,
                                         Map<Band, String> bandOptions,
                                         AsyncCallback<Map<String, List<WebPlotRequest>>> callback) {
        callback.onSuccess(null);
    }

    public void getSpectrumRequest(SelectedRowData selRowData, AsyncCallback<List<WebPlotRequest>> callback) {
        callback.onSuccess(null);
    }

    public void getCoverageInfo(TableMeta tableMeta, AsyncCallback<CoverageInfo> callback) {
        callback.onSuccess(null);
    }

    public ImagePlotDefinition getImagePlotDefinition() {
        return imagePlotDefinition;
    }

    public ActiveTargetLayer initActiveTargetLayer() {
        if (targetLayer==null) {
            Map<String,String> m= new HashMap<String, String>(5);
            m.put(EventWorker.ID,activeTargetLayerName);
            m.put(CommonParams.TARGET_TYPE,CommonParams.TABLE_ROW);
            m.put(CommonParams.TARGET_COLUMNS, "in_ra,in_dec");
            targetLayer= (ActiveTargetLayer)(new ActiveTargetCreator().create(m));
            Application.getInstance().getEventHub().bind(targetLayer);
            targetLayer.bind(Application.getInstance().getEventHub());
        }
        return targetLayer;
    }

    public List<DatasetDrawingLayerProvider> initArtifactLayers() { return null; }


    public class DynImagePlotDefinition implements ImagePlotDefinition {
        public int getImageCount() {
            return 0;
        }

        public List<String> getViewerIDs() {
            return Collections.emptyList();
        }

        public List<String> get3ColorViewerIDs() {
            return Collections.emptyList();
        }

        public Map<String, List<String>> getViewerToDrawingLayerMap() {
            return new HashMap<String, List<String>>(0);
        }

        public GridLayoutType getGridLayout() {
            return GridLayoutType.AUTO;
        }

        public List<String> getBandOptions(String viewerID) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Map<Band, String> getBandOptionsDefaults(String viewerID) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
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
