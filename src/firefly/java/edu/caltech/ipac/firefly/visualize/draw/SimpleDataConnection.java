/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.draw;

import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.WebPlot;

import java.util.List;
/**
 * User: roby
 * Date: Jul 16, 2010
 * Time: 12:52:28 PM
 */


/**
* @author Trey Roby
*/
public abstract class SimpleDataConnection implements DataConnection {

    private final String _title;
    private final String _helpLine;
    private final String _defColor;

    public SimpleDataConnection(String title, String helpLine) {  this(title,helpLine,null); }


    public SimpleDataConnection(String title, String helpLine, String initDefColor) {
        _title= title;
        _helpLine= helpLine;
        _defColor= initDefColor;
    }

    public String getTitle(WebPlot plot) { return _title;}
    public int size() { return 1; }
    public boolean isActive() { return true; }
    public boolean isDataVisible() { return true; }
    public void setHighlightedIdx(int idx) {}

    public void setSelectedIdx(Integer... idx) { }
    public List<Integer> getSelectedIdx() { return null; }
    public void showDetails(int x, int y, int index) {  }
    public void hideDetails() {  }
    public WebEventManager getEventManager() { return null; }
    public boolean getSupportsHighlight() { return false; }
    public SelectSupport getSupportsAreaSelect() { return SelectSupport.NO; }
    public int getSelectedCount() { return 0; }

    public ActionReporter getActionReporter() { return null; }

    public boolean getSupportsFilter() { return false; }

    public boolean isPriorityLayer() { return false; }

    public boolean getSupportsMouse() { return true; }
    public boolean getOnlyShowIfDataIsVisible() { return false; }

    public boolean getHasPerPlotData() { return false; }
    public boolean isPointData() { return false; }
    public boolean isVeryLargeData() { return false; }

    public DrawConnector getDrawConnector() { return null; }

    public  abstract List<DrawObj> getData(boolean rebuild, WebPlot plot);

    public List<DrawObj> getHighlightData(WebPlot p) { return null; }

    public String getInitDefaultColor() { return _defColor; }

    public List<String> getDefaultSubgroupList() { return null; }

    public boolean getOKForSubgroups() { return true; }

    public String getHelpLine() { return _helpLine; }

    public AsyncDataLoader getAsyncDataLoader() { return null; }

    public void filter(Integer... idx) { }
}

