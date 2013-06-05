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
    public int getHighlightedIdx() { return 0; }
    public void showDetails(int x, int y, int index) {  }
    public void hideDetails() {  }
    public WebEventManager getEventManager() { return null; }
    public boolean getSupportsSelection() { return false; }
    public boolean getSupportsMouse() { return true; }
    public boolean getOnlyIfDataVisible() { return false; }

    public boolean getHasPerPlotData() { return false; }
    public boolean isPointData() { return false; }
    public boolean isVeryLargeData() { return false; }

    public DrawConnector getDrawConnector() { return null; }

    public  abstract List<DrawObj> getData(boolean rebuild, WebPlot plot);

    public String getInitDefaultColor() { return _defColor; }

    public String getHelpLine() { return _helpLine; }

    public AsyncDataLoader getAsyncDataLoader() { return null; }

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
