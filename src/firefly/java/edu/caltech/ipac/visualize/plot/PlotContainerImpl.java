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
public class PlotContainerImpl implements PlotContainer {

    private List<Plot>            _plots       = new ArrayList<Plot>(10);

    public List<Plot> getPlotList() { return _plots; }

    public Iterator<Plot> iterator() { return _plots.iterator(); }

    public void freeResources() {
        _plots.clear();
    }
}

