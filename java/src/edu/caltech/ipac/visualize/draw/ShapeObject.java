/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.visualize.plot.PlotContainer;
import edu.caltech.ipac.visualize.plot.PlotPaintListener;
import edu.caltech.ipac.visualize.plot.PlotViewStatusListener;

/**
 * The ShapeObject represents a shape, displayed on a plot
 */
public interface ShapeObject extends PlotViewStatusListener, PlotPaintListener {

    public LineShape   getLineShape();

    public StringShape getStringShape();

    public void addPlotView(PlotContainer container);

    public void removePlotView(PlotContainer container);
}
