package edu.caltech.ipac.firefly.fuse.data.provider;
/**
 * User: roby
 * Date: 7/25/14
 * Time: 12:45 PM
 */


import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.fuse.data.DatasetInfoConverter;
import edu.caltech.ipac.firefly.fuse.data.ImagePlotDefinition;
import edu.caltech.ipac.firefly.fuse.data.PlotData;
import edu.caltech.ipac.firefly.fuse.data.config.SelectedRowData;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.ui.creator.drawing.ActiveTargetLayer;
import edu.caltech.ipac.firefly.ui.creator.drawing.DatasetDrawingLayerProvider;
import edu.caltech.ipac.firefly.ui.creator.eventworker.ActiveTargetCreator;
import edu.caltech.ipac.firefly.ui.creator.eventworker.EventWorker;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.util.Dimension;
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
    private final PlotData dynPlotData= new PlotData(null,false,true,false);
    private final DynImagePlotDefinition imagePlotDefinition= new DynImagePlotDefinition();



    public PlotData getDynamicData() {
        return dynPlotData;
    }

    public Set<DataVisualizeMode> getDataVisualizeModes() { return new HashSet<DataVisualizeMode>(); }
    public boolean isSupport(DataVisualizeMode mode) { return false; }

    public void update(SelectedRowData selRowData, AsyncCallback<String> cb) {
        cb.onSuccess("ok");
    }

    public PlotData getPlotData() {
        return dynPlotData;
    }

    public List<WebPlotRequest> getSpectrumRequest(SelectedRowData selRowData) { return null; }


    public ImagePlotDefinition getImagePlotDefinition() { return imagePlotDefinition; }

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

    public List<DatasetDrawingLayerProvider> initArtifactLayers(EventHub hub) { return null; }


    public class DynImagePlotDefinition implements ImagePlotDefinition {
        public int getImageCount() {
            return 0;
        }

        public List<String> getViewerIDs(SelectedRowData selData) {
            return Collections.emptyList();
        }

        public List<String> get3ColorViewerIDs(SelectedRowData selData) {
            return Collections.emptyList();
        }

        public Map<String, List<String>> getViewerToDrawingLayerMap() {
            return new HashMap<String, List<String>>(0);
        }

        public String  getGridLayout() {
            return ImagePlotDefinition.AUTO_GRID_LAYOUT;
        }

        public List<String> getAllBandOptions(String viewerID) { return null; }

        public Dimension getImagePlotDimension() { return null; }

        public void setBandOptions(String viewerID, Map<Band, String> ops) { }

        public Map<Band, String> getBandOptions(String viewerID) { return null; }
    }

}

