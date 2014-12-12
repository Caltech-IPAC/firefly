package edu.caltech.ipac.visualize.plot;

import java.util.EventObject;
import java.awt.Graphics2D;

/**
 * The event that is passed when ...
 * @author Trey Roby
 */
public class PlotPaintEvent extends EventObject {

    private Graphics2D _g2;
    private int        _idx;
    private int        _total;
    private Plot       _p;

    /**
     * Create a new PlotPaintEvent
     * @param o source of the event.
     * @param g2 the Graphics2D used for rendering
     */
    public PlotPaintEvent (Object    o,
                           Plot       p,
                           Graphics2D g2, 
                           int        idx, 
                           int        total) {
        super(o);
        _g2   = g2;
        _idx  = idx;
        _total= total;
        _p= p;
    }

    public Graphics2D getGraphics()   { return _g2;              }
    public PlotView   getPlotView()   {
        PlotView pv= null;
        if (getSource() instanceof PlotView) {
           pv=  (PlotView)getSource();
        }
        return pv;
    }
    public int        getPaintIndex() { return _idx;             }
    public int        getPaintTotal() { return _total;           }
    public Plot       getPlot()       { return _p; }
}
/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313) 
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
