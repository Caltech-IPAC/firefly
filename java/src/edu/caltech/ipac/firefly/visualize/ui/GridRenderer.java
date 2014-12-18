package edu.caltech.ipac.firefly.visualize.ui;
/**
 * User: roby
 * Date: 8/21/14
 * Time: 9:27 AM
 */


import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.util.Dimension;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotWidget;

import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public interface GridRenderer {


    public void reinitGrid(Map<String,MiniPlotWidget> mpwMap, List<XYPlotWidget> xyList);
    public void setShowMask(List<String> showMask);
    public void setDimension(Dimension dim);
    public void clear();
    public void onResize();
    public Widget getWidget();
    public Element getMaskingElement(String key);
    public void postPlotting();

}

