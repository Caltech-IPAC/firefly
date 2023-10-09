/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.plotdata.FitsReadFactory;
import edu.caltech.ipac.visualize.plot.plotdata.GeomException;
import edu.caltech.ipac.visualize.plot.projection.ProjectionUtil;
import nom.tam.fits.FitsException;

import java.io.IOException;
import java.io.Serializable;

/**
 * @author Trey Roby
 * Date: 5/5/15
 */
public class ActiveFitsReadGroup implements Serializable {

    public static final int LENGTH= 3;
    private final FitsRead[] fitsReadAry= new FitsRead[LENGTH];
    private int refBandIdx = 0;

    public void setFitsRead(Band band, FitsRead fr) {
        if (band.getIdx()==0 && fr==null) Logger.info("Setting null for band 0");
        boolean allNull= true;
        for(var testFr : fitsReadAry) {
            if (testFr != null) {
                allNull = false;
                break;
            }
        }
        if (allNull) refBandIdx = band.getIdx();
        fitsReadAry[band.getIdx()]= fr;
    }

    public FitsRead getFitsRead(Band band) { return fitsReadAry[band.getIdx()]; }
    public FitsRead getRefFitsRead() { return fitsReadAry[refBandIdx]; }
    public FitsRead getNoBandFitsRead() { return fitsReadAry[Band.NO_BAND.getIdx()];}
    public FitsRead[] getFitsReadAry() { return fitsReadAry; }

    public FitsRead findFirst() {
        for(FitsRead fr : fitsReadAry) {
            if (fr!=null) return fr;
        }
        return null;
    }

    public Band get3cRefBand() {
        for(Band b : new Band[] {Band.RED,Band.GREEN,Band.BLUE}) {
            if (b.getIdx()==refBandIdx) return b;
        }
        return Band.RED;
    }



    public void setThreeColorBandIn(FitsRead fitsRead, Band band) throws GeomException, FitsException, IOException {
        if (band==Band.NO_BAND) throw new IllegalArgumentException("band must be RED, GREEN, or BLUE");
        FitsRead refFitsRead= fitsReadAry[refBandIdx];
        var fr= (refFitsRead==fitsRead || ProjectionUtil.isSameProjection(refFitsRead, fitsRead)) ?
                fitsRead : FitsReadFactory.createFitsReadWithGeom(  fitsRead, refFitsRead, false);
        setFitsRead(band,fr);
    }
}
