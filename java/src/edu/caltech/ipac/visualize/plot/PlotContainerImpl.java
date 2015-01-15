/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;
/**
 * User: roby
 * Date: 10/18/13
 * Time: 11:51 AM
 */


import edu.caltech.ipac.util.Assert;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Trey Roby
 */
public class PlotContainerImpl implements PlotContainer {

    private List<PlotPaintListener> _plotPaint   = new ArrayList<PlotPaintListener>(5);
    private List<PlotViewStatusListener> _plotStatus  = new ArrayList<PlotViewStatusListener>(5);
    private List<Plot>            _plots       = new ArrayList<Plot>(10);



    public List<Plot> getPlotList() { return _plots; }

    public Iterator<Plot> iterator() { return _plots.iterator(); }

    public void freeResources() {
        _plots.clear();
        _plotPaint.clear();
        _plotStatus.clear();
    }


    /**
     * Add a PlotViewStatusListener.
     * @param l the listener
     */
    public void addPlotViewStatusListener(PlotViewStatusListener l) {
        _plotStatus.add(l);
    }
    /**
     * Remove a PlotViewStatusListener.
     * @param l the listener
     */
    public void removePlotViewStatusListener(PlotViewStatusListener l) {
        _plotStatus.remove(l);
    }

    /**
     * fire the <code>PlotViewStatusListener</code>s.
     * @param stat a StatusChagenType
     * @param plot that the event is about.
     */
    public void fireStatusChanged(StatusChangeType stat, Plot plot) {
        List<PlotViewStatusListener> newlist;
        PlotViewStatusEvent ev= new PlotViewStatusEvent(this, plot);
        synchronized (this) {
            newlist = new ArrayList<PlotViewStatusListener>(_plotStatus);
        }

        switch (stat) {
            case ADDED :
                for(PlotViewStatusListener l: newlist) l.plotAdded(ev);
                break;
            case REMOVED :
                for(PlotViewStatusListener l: newlist) l.plotRemoved(ev);
                break;
            default:
                Assert.argTst(false, stat + " is unknown to this switch");
        }
    }

// ========================================================================
    // ----------------- PlotPaintListener listener methods -------------------
    // ========================================================================

//    public int getPlotPaintListenerIdx(PlotPaintListener l) {
//        return _plotPaint.indexOf(l);
//    }
//    /**
//     *  -- for testing
//     */
//    public void addPlotPaintListenerAtTop(PlotPaintListener l) {
//        _plotPaint.add(0,l);
//    }
    public void addPlotPaintListener(PlotPaintListener l) {
        _plotPaint.add(l);
    }
    public void removePlotPaintListener(PlotPaintListener l) {
        _plotPaint.remove(l);
    }

    /**
     * Pass a list of PlotPaintListeners and attemp to reorder the
     * the listeners to that order.  Any listener not in the passed
     * list will be put at the end.  Any listener in the passed list
     * not in the listener list will be ignored.
     */
    public void reorderPlotPaintListeners(PlotPaintListener  reorderAry[]) {
        List<PlotPaintListener> newList= new LinkedList<PlotPaintListener>();
        for(int j=0; (j<reorderAry.length); j++) newList.add(reorderAry[j]);

        PlotPaintListener ppl;
        for (Iterator<PlotPaintListener> i= newList.iterator(); (i.hasNext()); ) {
            ppl= i.next();
            if (_plotPaint.contains(ppl)) {
                _plotPaint.remove(ppl);
            }
            else {
                i.remove();
            }
        }
        _plotPaint.addAll(0,newList);
    }

    public void firePlotPaint(Plot p, Graphics2D g2) {
        List<PlotPaintListener> newlist;
        PlotPaintListener listener;
        PlotPaintEvent ev;
        synchronized (this) {
            newlist = new ArrayList<PlotPaintListener>(_plotPaint);
        }
        int total= newlist.size();
        int j= 0;

        for(Iterator<PlotPaintListener> i= newlist.iterator(); i.hasNext(); j++) {
            ev= new PlotPaintEvent(this, p, g2, j, total);
            listener = i.next();
            listener.paint(ev);
        }
    }

    public boolean isComplexPaint() {
        boolean retval= false;
        Object o;
        for (Iterator i= _plotPaint.iterator(); (i.hasNext() && !retval); ) {
            o= i.next();
            retval= (o instanceof PlotPaintComplexListener);
        }
        return retval;
    }
}

