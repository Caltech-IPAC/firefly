package edu.caltech.ipac.firefly.fuse.data;
/**
 * User: roby
 * Date: 7/24/14
 * Time: 1:07 PM
 */


import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.fuse.data.config.SelectedRowData;
import edu.caltech.ipac.firefly.ui.creator.drawing.ActiveTargetLayer;
import edu.caltech.ipac.firefly.ui.creator.drawing.DatasetDrawingLayerProvider;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;

import java.util.List;
import java.util.Set;

/**
 * @author Trey Roby
 */
public interface DatasetInfoConverter {

    /**
     * notes- FITS_SINGLE and FITS_GROUP are mutually exclusive, group can do single but will continued to be displayed
     * with the proper overlay for its elements in the group. e.g. 2mass j will have 2 mass J artifacts
     */
    public enum DataVisualizeMode { NONE, SPECTRUM, FITS, FITS_3_COLOR}

    public Set<DataVisualizeMode> getDataVisualizeModes();
    public boolean isSupport(DataVisualizeMode mode);

    public ImagePlotDefinition getImagePlotDefinition();

    public void update(SelectedRowData selRowData, AsyncCallback<String> callback);

    public PlotData getPlotData();

    public List<WebPlotRequest> getSpectrumRequest(SelectedRowData selRowData);

    public ActiveTargetLayer initActiveTargetLayer();
    public List<DatasetDrawingLayerProvider> initArtifactLayers(EventHub hub);

}

