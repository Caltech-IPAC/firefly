/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;
/**
 * User: roby
 * Date: 10/18/13
 * Time: 11:51 AM
 */


/**
 * @author Trey Roby
 */
public interface PlotContainer extends Iterable<Plot> {

    void freeResources();

}

