package edu.caltech.ipac.firefly.fuse.data;

import edu.caltech.ipac.firefly.fuse.data.config.SelectedRowData;
import edu.caltech.ipac.firefly.util.Dimension;
import edu.caltech.ipac.firefly.visualize.Band;

import java.util.List;
import java.util.Map;

/**
 * User: roby
 * Date: 8/27/14
 * Time: 11:36 AM
 */
public interface ImagePlotDefinition {


    public static final String AUTO_GRID_LAYOUT= "AutoGridLayout";
    public static final String FINDER_CHART_GRID_LAYOUT= "FCGridLayout";
    public static final String SINGLE_ROW_GRID_LAYOUT= "SingleRowGridLayout";

    public int getImageCount();

    /**
     * get an ID for each image type. i.e. 4 id's for wise, 3 id's for two mass, etc.
     * @return the id list, return null it there is no grouping
     * @param selData
     */
    public List<String> getViewerIDs(SelectedRowData selData);

    public List<String> get3ColorViewerIDs(SelectedRowData selData);



    /**
     * get the drawing overlay (EventWorker) id list associated with each image ID.
     * @return map that represents the one to many relationship between an image and its drawing overlays
     * if there is not grouping return a map with one entry.  The key will be ignored.
     */
    public Map<String, List<String>> getViewerToDrawingLayerMap();

    /**
     * How to layout the grid in rows. Each list of IDs is a row. Null means auto
     * @return
     */
    public String getGridLayout();

    public Dimension getImagePlotDimension();

    /**
     * Get the list of Band descriptions for this viewer id. Element 0 is the default.
     * return null it this viewer id is not 3 color
     * To implement, override this method.
     * @param viewerID
     * @return
     */
    public List<String> getAllBandOptions(String viewerID);

    public void setBandOptions(String viewerID, Map<Band,String> ops);

    public Map<Band,String> getBandOptions(String viewerID);


}
