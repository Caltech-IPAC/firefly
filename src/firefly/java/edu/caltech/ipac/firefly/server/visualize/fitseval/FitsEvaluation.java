/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize.fitseval;
/**
 * User: roby
 * Date: 7/5/18
 * Time: 9:31 AM
 */


import edu.caltech.ipac.firefly.data.RelatedData;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.plotdata.FitsReadFactory;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Trey Roby
 */
public class FitsEvaluation {

    public static interface Eval {
        List<RelatedData> evaluate(File f, FitsRead[] frAry, BasicHDU[] HDUs, int fitsReadIndex, int hduIndex, WebPlotRequest req);
    }

    static private List<Eval> evalList= new ArrayList<>();

    static {
        evalList.add(new MaskEval());
        evalList.add(new SpectralCubeEval());
    }

    public static FitsDataEval readAndEvaluate(File f, boolean clearHdu, WebPlotRequest req) throws FitsException  {
        return readAndEvaluate(new Fits(f), f, clearHdu, req);
    }

    public static FitsDataEval readAndEvaluate(Fits fits, File f, boolean clearHdu, WebPlotRequest req) throws FitsException  {
        try  {
            BasicHDU[] HDUs = fits.read();
            if (HDUs == null || HDUs.length==0) throw new FitsException("Bad format in FITS file");
            FitsRead frAry[] = FitsReadFactory.createFitsReadArray(HDUs, clearHdu);
            FitsDataEval fitsDataEval= new FitsDataEval(frAry);
            if (frAry.length >1) { // Do evaluation
                for(int i= 0; i<frAry.length; i++) {
                    if (frAry[i].getPlaneNumber()==0) {
                        for (Eval e : evalList) {
                            List<RelatedData> rdList= e.evaluate(f, frAry, HDUs, i, frAry[i].getHduNumber(), req);
                           // if (rdList!=null) {
                                fitsDataEval.addAllRelatedData(i,rdList);
                           // }
                        }
                    }
                    else { // if cube, then duplicate related data for the other planes, todo: improve this
                        fitsDataEval.addAllRelatedData(i,fitsDataEval.getRelatedData(i-1));
                    }
                }
            }
            return fitsDataEval;
        } finally {
            try {
                if (fits!=null && fits.getStream()!=null) fits.getStream().close();
            } catch (IOException e) {
                // do nothing
            }
        }
    }


}
