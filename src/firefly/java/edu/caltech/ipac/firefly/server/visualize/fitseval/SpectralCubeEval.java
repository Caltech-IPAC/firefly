/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize.fitseval;
/**
 * User: roby
 * Date: 7/13/18
 * Time: 10:39 AM
 */


import edu.caltech.ipac.firefly.data.RelatedData;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import nom.tam.fits.BasicHDU;

import java.io.File;
import java.util.List;

/**
 * @author Trey Roby
 */
class SpectralCubeEval implements FitsEvaluation.Eval {
    @Override
    public List<RelatedData> evaluate(File f, FitsRead[] frAry, BasicHDU[] HDUs, int fitsReadIndex, int hduIndex, WebPlotRequest req) {
        if (req!=null && !req.getBooleanParam("SpectralCube")) return null;
        // do spectral cube processing here
        // todo Convert FitsCube to work in this environment
        return null;
    }
}
