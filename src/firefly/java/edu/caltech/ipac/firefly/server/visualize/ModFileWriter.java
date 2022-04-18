/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Trey Roby
 */
abstract class ModFileWriter {

    private static final Logger.LoggerImpl _log= Logger.getLogger();
    private final Band _band;
    private final File _targetFile;
    private final boolean _markAsOriginal;

    private ModFileWriter(Band band, File targetFile, boolean markAsOriginal) {
        _band= band;
        _targetFile= targetFile;
        _markAsOriginal= markAsOriginal;
    }

    protected File getTargetFile() { return _targetFile; }

    /**
     * Write the fits file and update data structures
     * @param state the PlotState to update
     */
    public void writeFile(PlotState state) {
        PlotStateUtil.setWorkingFitsFile(state, _targetFile, _band);
        if (_markAsOriginal) PlotStateUtil.setOriginalFitsFile(state, _targetFile, _band);
        write();
    }

    public boolean getCreatesOnlyOneImage() { return true; }

    protected abstract void write();

    static class GeomFileWriter extends ModFileWriter {

        private final FitsRead _fr;

        GeomFileWriter(File templateFile, int idx, FitsRead fr, Band band, boolean markAsOriginal) {
            super(band, makeFile(templateFile, idx), markAsOriginal);
            _fr= fr;
        }

        static File makeFile(File templateFile, int idx) {
            String geomTmp= templateFile.getName();
            try {
                return File.createTempFile(geomTmp + "-"+idx +"-geomed", "."+FileUtil.FITS,
                        ServerContext.getVisSessionDir());
            } catch (IOException e) {
                return new File(ServerContext.getVisSessionDir(), geomTmp + "-"+idx +"-geomed." + FileUtil.FITS);
            }
        }

        protected void write() {
            File f= getTargetFile();
            try {
                OutputStream os= new BufferedOutputStream(new FileOutputStream(f), 1024*16);
                _fr.writeSimpleFitsFile(os);
                FileUtil.silentClose(os);
                _fr.clearHDU();
            } catch (Exception e) {
                _log.warn(e,"geom write failed", "geom file: "+f.getPath());
            }
        }
    }

}

