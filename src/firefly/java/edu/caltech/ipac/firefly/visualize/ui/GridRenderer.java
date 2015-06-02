/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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


    void reinitGrid(Map<String,MiniPlotWidget> mpwMap, List<XYPlotWidget> xyList);
    void setShowMask(List<String> showMask);
    void setDimension(Dimension dim);
    void clear();
    void onResize();
    Widget getWidget();
    Element getMaskingElement(String key);
    void postPlotting();
    void showPrimaryOnly(boolean primaryOnly);

}

