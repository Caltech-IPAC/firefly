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
public abstract class AbstractDataSetInfoConverter implements DatasetInfoConverter {


    private final Set<DataVisualizeMode> modeList;
    private ActiveTargetLayer targetLayer= null;
    private final String activeTargetLayerName;
    private PlotData dynPlotData= null;


    protected AbstractDataSetInfoConverter(List<DataVisualizeMode> modeList, PlotData dynPlotData) {
        this(modeList, dynPlotData, "target");
    }


    protected AbstractDataSetInfoConverter(List<DataVisualizeMode> modeList, PlotData dynPlotData, String activeTargetLayerName) {
        this.modeList= Collections.unmodifiableSet(new HashSet<DataVisualizeMode>(modeList));
        this.activeTargetLayerName= activeTargetLayerName;
        this.dynPlotData= dynPlotData;
    }

    public void update(SelectedRowData selRowData, AsyncCallback<String> callback) {
        dynPlotData.setSelectedRowData(selRowData);
        callback.onSuccess("ok");
    }

    public PlotData getPlotData() {
        return dynPlotData;
    }

    public Set<DataVisualizeMode> getDataVisualizeModes() { return modeList; }
    public boolean isSupport(DataVisualizeMode mode) { return modeList.contains(mode); }


    public List<WebPlotRequest> getSpectrumRequest(SelectedRowData selRowData) { return null; }

    public ImagePlotDefinition getImagePlotDefinition() { return null; }

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






}

