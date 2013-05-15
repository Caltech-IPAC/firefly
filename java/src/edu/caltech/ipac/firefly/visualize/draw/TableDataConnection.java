package edu.caltech.ipac.firefly.visualize.draw;

import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.util.StringUtils;

import java.util.List;
/**
 * User: roby
 * Date: Jul 16, 2010
 * Time: 12:52:28 PM
 */


/**
* @author Trey Roby
*/
public abstract class TableDataConnection implements DataConnection {

    private final TablePanel _table;
    private final boolean _supportsSelection;
    private final boolean _supportsMouse;
    private final boolean _onlyIfTabActive;
    private final String _helpLine;

    public TableDataConnection(TablePanel table,String helpLine) {this(table,helpLine, true, true, false); }

    public TableDataConnection(TablePanel table,
                               String helpLine,
                               boolean supportsSelection,
                               boolean supportsMouse,
                               boolean onlyIfTabActive) {
        _table= table;
        _helpLine= helpLine;
        _supportsSelection = supportsSelection;
        _supportsMouse = supportsMouse;
        _onlyIfTabActive = onlyIfTabActive;
    }

    public TablePanel getTable() { return _table; }

    public String getTitle(WebPlot plot) {
            return !StringUtils.isEmpty(_table.getShortDesc()) ? _table.getShortDesc() : _table.getName();
    }
    public int size() { return _table.getRowCount(); }
    public boolean isActive() { return true; }
    public void setHighlightedIdx(int... idx) { _table.setHighlightRows(true, idx); }

    public int[] getHighlightedIdx() {
        Integer rows[]= _table.getTable().getHighlightRowIdxs();
        int retval[]= new int[rows.length];
        for(int i=0; (i<rows.length); i++)  retval[i]= rows[0];
        return retval;
    }

    public void showDetails(int x, int y, int index) {  }
    public void hideDetails() {  }
    public WebEventManager getEventManager() { return _table.getEventManager(); }

    public boolean getSupportsSelection() { return _supportsSelection; }
    public boolean getSupportsMouse() { return _supportsMouse; }
    public boolean getOnlyIfDataVisible() { return _onlyIfTabActive; }

    public boolean getHasPerPlotData() { return false; }
    public boolean isPointData() { return false; }

    public  abstract List<DrawObj> getData(boolean rebuild, WebPlot plot);

    public DrawConnector getDrawConnector() { return null; }

    public String getInitDefaultColor() { return null; }

    public String getHelpLine() { return _helpLine; }

    public boolean isDataVisible() { return GwtUtil.isOnDisplay(_table); }

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
