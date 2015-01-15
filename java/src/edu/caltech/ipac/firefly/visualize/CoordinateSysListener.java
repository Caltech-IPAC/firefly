/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.visualize.plot.CoordinateSys;

/**
 * @author tatianag
 * @version $Id: CoordinateSysListener.java,v 1.2 2008/10/21 21:41:05 roby Exp $
 */
public interface CoordinateSysListener {
    /**
     * Called when coordinate system is changes
     * @param newCoordSys
     */
    public void onCoordinateSysChange(CoordinateSys newCoordSys);
}
