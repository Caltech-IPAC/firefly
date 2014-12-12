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
