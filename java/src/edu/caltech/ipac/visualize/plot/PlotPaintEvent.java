/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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
