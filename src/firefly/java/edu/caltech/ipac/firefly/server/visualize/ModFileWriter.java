/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.plotdata.FitsReadFactory;
import nom.tam.fits.Fits;

import java.io.File;
import java.io.IOException;

/**
 * @author Trey Roby
 */
public class ModFileWriter {

    private final Band band;
    private final File targetFile;
    private final FitsRead fr;

    public ModFileWriter(File templateFile, int idx, FitsRead fr, Band band) {
        this.band = band;
        this.targetFile = makeFile(templateFile, idx);
        this.fr = fr;
    }

    public File getTargetFile() { return targetFile; }

    /**
     * Write the fits file and update data structures
     * @param state the PlotState to update
     */
    public void writeFile(PlotState state, ActiveFitsReadGroup frGroup) {
        PlotStateUtil.setWorkingFitsFile(state, targetFile, band);
        try {
            fr.writeSimpleFitsFile(targetFile);
        } catch (IOException e) {
            Logger.getLogger().warn(e, "Failed to write geom for band "+band, "geom file: "+targetFile.getPath());
        }

        // re-create FitsRead arrays after files are written to disk to ensure correct SPOT_HS headers
        try {
            if (targetFile.exists()) {
                Fits fits = new Fits(targetFile);
                FitsRead[] fitsReadArray = FitsReadFactory.createFitsReadArray(fits);
                frGroup.setFitsRead(band, fitsReadArray[0]);
            }
        } catch (Exception e) {
            Logger.getLogger().warn(e, "Failed to refresh FitsRead for band "+band, "geom file: "+targetFile.getPath());
        }
    }

    private static File makeFile(File templateFile, int idx) {
        String geomTmp= templateFile.getName();
        try {
            return File.createTempFile(geomTmp + "-"+idx +"-geomed", "."+FileUtil.FITS,
                    ServerContext.getVisSessionDir());
        } catch (IOException e) {
            return new File(ServerContext.getVisSessionDir(), geomTmp + "-"+idx +"-geomed." + FileUtil.FITS);
        }
    }
}