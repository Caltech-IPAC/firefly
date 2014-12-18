package edu.caltech.ipac.firefly.visualize;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
/**
 * User: roby
 * Date: Apr 28, 2009
 * Time: 2:29:27 PM
 */


/**
 * @author Trey Roby
 */
public class PlotRequestHistory implements Iterable<WebPlotRequest> {


    private static final int DEF_HISTORY_MAX= 10;

    private static PlotRequestHistory _instance= new PlotRequestHistory();
    private int _historyMax= 10;
    private List<WebPlotRequest> _requestHistory=
            new ArrayList<WebPlotRequest>(DEF_HISTORY_MAX);

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    private PlotRequestHistory() {}

    public static PlotRequestHistory instance() { return _instance;  }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public void setHistoryMax(int max) {
        _historyMax= max;
    }

    public void add(WebPlotRequest req) {
        if (_requestHistory.contains(req)) {
            _requestHistory.remove(req);
        }
        if (_requestHistory.size()>=_historyMax) {
            _requestHistory.remove(0);
        }
        _requestHistory.add(req);
    }

    public Iterator<WebPlotRequest> iterator() {
        return _requestHistory.iterator();
    }

    public WebPlotRequest get(int idx) { return _requestHistory.get(idx); }

    public WebPlotRequest getLast() {
        return _requestHistory.get(_requestHistory.size()-1);
    }

    public List<WebPlotRequest> getList() {
        return _requestHistory;
    }

    public int size() { return _requestHistory.size(); }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


// =====================================================================
// -------------------- Factory Methods --------------------------------
// =====================================================================

}

