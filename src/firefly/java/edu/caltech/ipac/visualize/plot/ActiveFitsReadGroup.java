/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.visualize.plot;
/**
 * User: roby
 * Date: 5/5/15
 * Time: 2:56 PM
 */


import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.Band;

import java.io.Serializable;

/**
 * @author Trey Roby
 */
public class ActiveFitsReadGroup implements Serializable {

    private static final int LENGTH= 3;

    private FitsRead fitsReadAry[]= new FitsRead[LENGTH];
    private boolean inUse[]= new boolean[LENGTH];


    public void setFitsRead(Band band, FitsRead fr) {
        if (band.getIdx()==0 && fr==null) {
            Logger.info("Setting null for band 0");
        }
        fitsReadAry[band.getIdx()]= fr;
        inUse[band.getIdx()]= (fr!=null);
    }

    public FitsRead getFitsRead(Band band) {
        return fitsReadAry[band.getIdx()];
    }

    public FitsRead[] getFitsReadAry() { return fitsReadAry; }


    public void freeResources(boolean freeFRResource) {
        for(int i= 0; (i<fitsReadAry.length);i++) {
            if (fitsReadAry[i]!=null && freeFRResource)  fitsReadAry[i].freeResources();
            fitsReadAry[i]= null;
        }
    }

    public ActiveFitsReadGroup makeCopy() {
        ActiveFitsReadGroup copy= new ActiveFitsReadGroup();
        for(int i=0; i<fitsReadAry.length; i++) {
            copy.fitsReadAry[i]= fitsReadAry[i];
        }
        return copy;
    }
}
