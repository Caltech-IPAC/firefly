/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;

import java.io.Serializable;

/**
 * @author Trey Roby
 * Date: 5/5/15
 */
public class ActiveFitsReadGroup implements Serializable {

    public static final int LENGTH= 3;
    private final FitsRead[] fitsReadAry= new FitsRead[LENGTH];

    public void setFitsRead(Band band, FitsRead fr) {
        if (band.getIdx()==0 && fr==null) Logger.info("Setting null for band 0");
        fitsReadAry[band.getIdx()]= fr;
    }

    public FitsRead getFitsRead(Band band) { return fitsReadAry[band.getIdx()]; }
    public FitsRead[] getFitsReadAry() { return fitsReadAry; }
}
