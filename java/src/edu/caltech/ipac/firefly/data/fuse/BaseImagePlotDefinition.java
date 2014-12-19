package edu.caltech.ipac.firefly.data.fuse;
/**
 * User: roby
 * Date: 7/31/14
 * Time: 3:45 PM
 */


import edu.caltech.ipac.firefly.data.fuse.config.SelectedRowData;
import edu.caltech.ipac.firefly.util.Dimension;
import edu.caltech.ipac.firefly.visualize.Band;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* @author Trey Roby
*/
public class BaseImagePlotDefinition implements ImagePlotDefinition {
    private final int imageCount;
    private final List<String> viewerIDList;
    private final List<String> threeColorViewerIDList;
    private final Map<String, List<String>> viewerToDrawingLayerMap;
    private final String gridLayout;
    private final Map<String, Map<Band,String>> activeBandOps= new HashMap<String, Map<Band, String>>(17);

    public BaseImagePlotDefinition(String viewerID, List<String> drawingLayerList) {
        this.imageCount = 1;
        this.viewerIDList = Arrays.asList(viewerID);
        this.threeColorViewerIDList = null;
        Map<String, List<String>> m= new HashMap<String, List<String>>(1);
        m.put(viewerID, drawingLayerList);
        this.viewerToDrawingLayerMap = m;
        this.gridLayout= AUTO_GRID_LAYOUT;
    }


    public BaseImagePlotDefinition(int imageCount,
                                   List<String> viewerIDList,
                                   List<String> threeColorViewerIDList,
                                   Map<String, List<String>> viewerToDrawingLayerMap,
                                   String gridLayout) {
        this.imageCount = imageCount;
        this.viewerIDList = viewerIDList;
        this.threeColorViewerIDList = threeColorViewerIDList;
        this.viewerToDrawingLayerMap = viewerToDrawingLayerMap;
        this.gridLayout= gridLayout;
    }

    public int getImageCount() { return imageCount; }

    /**
     * get an ID for each image type. i.e. 4 id's for wise, 3 id's for two mass, etc.
     * @return the id list, return null it there is no grouping
     * @param selData
     */
    public List<String> getViewerIDs(SelectedRowData selData) { return viewerIDList;  }

    public List<String> get3ColorViewerIDs(SelectedRowData selData) {
        return threeColorViewerIDList;
    }

    public Dimension getImagePlotDimension() { return null; }


    /**
     * get the drawing overlay (EventWorker) id list associated with each image ID.
     * @return map that represents the one to many relationship between an image and its drawing overlays
     * if there is not grouping return a map with one entry.  The key will be ignored.
     */
    public Map<String, List<String>> getViewerToDrawingLayerMap() { return viewerToDrawingLayerMap;  }

    /**
     * How to layout the grid in rows. Each list of IDs is a row. Null means auto
     * @return
     */
    public String getGridLayout() {
        return gridLayout;
    }


    /**
     * Get the list of Band descriptions for this viewer id. Element 0 is the default.
     * return null it this viewer id is not 3 color
     * To implement, override this method.
     * @param viewerID
     * @return
     */
    public List<String> getAllBandOptions(String viewerID) {
        return null;
    }


    public void setBandOptions(String viewerID, Map<Band, String> ops) {
        activeBandOps.put(viewerID,ops);
    }

    public Map<Band, String> getBandOptions(String viewerID) {
        return activeBandOps.get(viewerID);
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
