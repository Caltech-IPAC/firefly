package edu.caltech.ipac.firefly.visualize.draw;

import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.WebPlot;

import java.util.List;
/**
 * User: roby
 * Date: Jul 16, 2010
 * Time: 12:51:58 PM
 */


/**
* @author Trey Roby
*/
public interface DataConnection {

    enum SelectSupport {YES,NO,TOO_BIG}
    /**
     * Get the title that the users sees for this data.
     * @param plot
     * @return the title
     */
    String getTitle(WebPlot plot);

    /**
     * Get the objects to draw.
     * @param rebuild a hint that something has change so the data should be rebuilt
     * @param plot the plot that that is going to be drawn on.  This parameter could be null if getHasPerPlotData
     *             returns false.  It is only set it getHasPerPlotData returns true. When getHasPerPlotData is false then
     *             the data is expected to be the same for all the plots.
     * @return
     */
    List<DrawObj> getData(boolean rebuild, WebPlot plot);
    List<DrawObj> getHighlightData(WebPlot p);

    public DrawConnector getDrawConnector();
    public int size();
    public boolean isActive();
    public boolean isDataVisible();
    public void setHighlightedIdx(int idx);
    public void setSelectedIdx(Integer... idx);
    public List<Integer> getSelectedIdx();

    public void showDetails(int x, int y, int index);
    public void hideDetails();
    public boolean getSupportsHighlight();
    public SelectSupport getSupportsAreaSelect();
    public int getSelectedCount();
    public boolean getSupportsFilter();
    public boolean getSupportsMouse();
    public boolean getOnlyShowIfDataIsVisible();
    public boolean isPriorityLayer();
    public List<String> getDefaultSubgroupList();
    public boolean getOKForSubgroups();

    /**
     * return true if the data is different for every WebPlot. Return false if the data is the same for every plot.
     * This method should return false in most cases.
     * @return true if the data is the same, false if the data is different per plot.
     */
    public boolean getHasPerPlotData();

    /**
     * return true if the data is only that of point data such as a catalog or a artifact.  The DrawObj array returned
     * in getData should only the PointDataObj. This is a hint about the data.
     * The drawing can be more efficient when isPointData returns true.
     * @return true if only point data, false otherwise
     */
    public boolean isPointData();
    /**
     * return true if this DataConnection servers a large amount of data. This is a hint about the data.
     * @return true if the data size is very large
     */
    public boolean isVeryLargeData();


    public WebEventManager getEventManager();
    public String getInitDefaultColor();
    public String getHelpLine();

    /**
     * Used if the data connection needs to make a server call to get the data
     * @return the loader
     */
    public AsyncDataLoader getAsyncDataLoader();

    public void filter(Integer... idx);
}




