package edu.caltech.ipac.firefly.fuse.data;
/**
 * User: roby
 * Date: 7/24/14
 * Time: 1:07 PM
 */


import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.fuse.data.config.SelectedRowData;
import edu.caltech.ipac.firefly.ui.creator.drawing.ActiveTargetLayer;
import edu.caltech.ipac.firefly.ui.creator.drawing.DatasetDrawingLayerProvider;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;

import java.util.List;
import java.util.Map;
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
    public enum GroupMode { ROW_ONLY, WHOLE_GROUP}

    public Set<DataVisualizeMode> getDataVisualizeModes();
    public boolean isSupport(DataVisualizeMode mode);
    public boolean is3ColorOptional();
    public DynamicPlotData getDynamicData();

    public ImagePlotDefinition getImagePlotDefinition();

    public void getImageRequest(SelectedRowData selRowData,
                                GroupMode       mode,
                                AsyncCallback<Map<String,WebPlotRequest>> callback);

    /**
     *
     * @param selRowData
     * @param bandOptions the band options map, pass null for defaults
     * @param callback
     */
    public void getThreeColorPlotRequest(SelectedRowData selRowData,
                                         Map<Band,String> bandOptions,
                                         AsyncCallback<Map<String,List<WebPlotRequest>>> callback);

    public void getSpectrumRequest(SelectedRowData selRowData, AsyncCallback<List<WebPlotRequest>> callback);
    public void getCoverageInfo(TableMeta tableMeta, AsyncCallback<CoverageInfo> callback);

    public ActiveTargetLayer initActiveTargetLayer();
    public List<DatasetDrawingLayerProvider> initArtifactLayers();

    public static class CoverageInfo {}


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
