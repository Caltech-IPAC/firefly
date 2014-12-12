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
