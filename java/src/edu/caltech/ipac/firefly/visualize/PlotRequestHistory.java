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
