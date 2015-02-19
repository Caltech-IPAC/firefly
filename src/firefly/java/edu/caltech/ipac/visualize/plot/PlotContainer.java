/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;
/**
 * User: roby
 * Date: 10/18/13
 * Time: 11:51 AM
 */


import java.awt.Graphics2D;

/**
 * @author Trey Roby
 */
public interface PlotContainer extends Iterable<Plot> {

    enum StatusChangeType {ADDED, REMOVED}



    public void freeResources();

    /**
     * Add a PlotViewStatusListener.
     * @param l the listener
     */
    public void addPlotViewStatusListener(PlotViewStatusListener l);
    /**
     * Remove a PlotViewStatusListener.
     * @param l the listener
     */
    public void removePlotViewStatusListener(PlotViewStatusListener l);

    /**
     * fire the <code>PlotViewStatusListener</code>s.
     * @param stat a StatusChagenType
     * @param plot that the event is about.
     */
    public void fireStatusChanged(StatusChangeType stat, Plot plot);


    // ========================================================================
    // ----------------- PlotPaintListener listener methods -------------------
    // ========================================================================

//    public int getPlotPaintListenerIdx(PlotPaintListener l);
    /**
     *  -- for testing
     */
//    public void addPlotPaintListenerAtTop(PlotPaintListener l);
    public void addPlotPaintListener(PlotPaintListener l);
    public void removePlotPaintListener(PlotPaintListener l);

    /**
     * Pass a list of PlotPaintListeners and attemp to reorder the
     * the listeners to that order.  Any listener not in the passed
     * list will be put at the end.  Any listener in the passed list
     * not in the listener list will be ignored.
     */
    public void reorderPlotPaintListeners(PlotPaintListener  reorderAry[]);

    public void firePlotPaint(Plot p, Graphics2D g2);

}

