package edu.caltech.ipac.firefly.visualize;
/**
 * User: roby
 * Date: 3/25/13
 * Time: 3:29 PM
 */


/**
* @author Trey Roby
*/
public interface PlotWidgetFactory {
    MiniPlotWidget create();
    String getCreateDesc();
    void prepare(MiniPlotWidget mpw, Vis.InitComplete initComplete);
    WebPlotRequest customizeRequest(MiniPlotWidget mpw, WebPlotRequest wpr);
    boolean isPlottingExpanded();
    void delete(MiniPlotWidget mpw);
}

