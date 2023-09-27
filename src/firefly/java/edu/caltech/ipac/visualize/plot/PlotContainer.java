/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;
/**
 * User: roby
 * Date: 10/18/13
 * Time: 11:51 AM
 */


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Trey Roby
 */
public class PlotContainer implements Iterable<ImagePlot> {
    private final List<ImagePlot> plots       = new ArrayList<>();
    public List<ImagePlot> getPlotList() { return plots; }
    public Iterator<ImagePlot> iterator() { return plots.iterator(); }
}

