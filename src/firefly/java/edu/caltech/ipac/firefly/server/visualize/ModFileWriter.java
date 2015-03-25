/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 2/17/11
 * Time: 12:05 PM
 */


import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.visualize.plot.FitsRead;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

/**
* @author Trey Roby
*/
abstract class ModFileWriter implements Runnable {

    private static final Logger.LoggerImpl _log= Logger.getLogger();
    private final Band _band;
    private final File _targetFile;
    private final boolean _markAsOriginal;

    ModFileWriter(Band band, File targetFile, boolean markAsOriginal) {
        _band= band;
        _targetFile= targetFile;
        _markAsOriginal= markAsOriginal;
    }

    protected File getTargetFile() { return _targetFile; }
    protected boolean doThread() { return true; }

    /**
     * Start the file writing in separate thread.  This method also update the working fits file to the name that is about
     * to be created.
     * @param state the PlotState to update
     */
    public void go(PlotState state) {
        VisContext.setWorkingFitsFile(state,_targetFile,_band);
        if (_markAsOriginal) {
            VisContext.setOriginalFitsFile(state, _targetFile, _band);
        }
        if (doThread()) {
            Thread thread= new Thread(this);
            thread.setDaemon(true);
            thread.start();
        }
    }

    public void run() {
        try { TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) { /*ignore*/ }

        write();
    }

    public boolean getCreatesOnlyOneImage() { return true; }

    protected abstract void write();

    static class UnzipFileWriter extends ModFileWriter {

        private File _checkFile;
        private final boolean _unzipNecessary;

        UnzipFileWriter(Band band, File checkFile) {
            super(band, PlotServUtils.findWorkingFitsName(checkFile),true);
            _checkFile= checkFile;
            String ext= FileUtil.getExtension(_checkFile);
            _unzipNecessary= (ext!=null && ext.equalsIgnoreCase(FileUtil.GZ));
        }

        protected boolean doThread() { return _unzipNecessary; }

        protected void write() {
            try {
                int buffer= (int)FileUtil.MEG;
                FileUtil.gUnzipFile(_checkFile, getTargetFile(),buffer);
            } catch (IOException e) {
                _log.warn(e,"zip expand failed",
                          "zip file: "+_checkFile.getPath());
            }
        }

        public boolean getCreatesOnlyOneImage() { return false; }
    }






    static class GeomFileWriter extends ModFileWriter {

        private final FitsRead _fr;

        GeomFileWriter(File templatefile, int idx, FitsRead fr, Band band, boolean markAsOriginal) {
            super(band, makeFile(templatefile, idx), markAsOriginal);
            _fr= fr;
        }

        public GeomFileWriter(File f, FitsRead fr, Band band) {
            super(band, f, true);
            _fr= fr;
        }

        static File makeFile(File templateFile, int idx) {
            String geomTmp= templateFile.getName();
            File f;
            try {
                f=  File.createTempFile(geomTmp + "-"+idx +"-geomed",
                                            "."+FileUtil.FITS,
                                            VisContext.getVisSessionDir());
            } catch (IOException e) {
                f= new File(VisContext.getVisSessionDir(),
                                  geomTmp + "-"+idx +"-geomed." + FileUtil.FITS);
            }
            return f;
        }

        protected void write() {
            try {
                OutputStream os= new BufferedOutputStream(new FileOutputStream(getTargetFile()), 1024*16);
                ImagePlot.writeFile(os, new FitsRead[]{_fr});
                FileUtil.silentClose(os);
            } catch (Exception e) {
                _log.warn(e,"geom write failed",
                          "geom file: "+getTargetFile().getPath());
            }
        }
    }

//    static class RotateFileWriter extends GeomFileWriter {
//        RotateFileWriter(File templateFile, int idx, FitsRead fr, Band band, double angle, boolean markAsOriginal) {
//            super(makeFile(templateFile,idx,angle),fr,band,markAsOriginal);
//        }
//
//        static File makeFile(File templateFile, int idx, double angle) {
//            String geomTmp= templateFile.getName();
//
//            File f;
//            try {
//                String angleDesc= Double.isNaN(angle) ? "north" : angle+"";
//                f= File.createTempFile(geomTmp + "-"+idx +"-rot-"+angleDesc,
//                                            "."+FileUtil.FITS,
//                                            VisContext.getVisSessionDir());
//            } catch (IOException e) {
//                f= new File(VisContext.getVisSessionDir(),
//                                geomTmp + "-"+idx +"-rot-north." + FileUtil.FITS);
//            }
//            return f;
//        }
//    }
//
//    static class CropAndCenterFileWriter extends GeomFileWriter {
//        CropAndCenterFileWriter(File templateFile, int idx, FitsRead fr, Band band, WorldPt wpt, double size) {
//            super(makeFile(templateFile,idx,wpt,size),fr,band,true);
//        }
//
//        static File makeFile(File templateFile, int idx, WorldPt wpt, double size) {
//            String geomTmp= templateFile.getName();
//
//            File f;
//            try {
//                DecimalFormat df = new DecimalFormat("##.##");
//                String cropDesc= (df.format(wpt.getLon())+"+"+df.format(wpt.getLat())+"x"+df.format(size)+"-")
//                        .replaceAll("\\+\\-","\\-");
//                f= File.createTempFile(geomTmp + "-"+idx +"-cropCenter-"+cropDesc,
//                                            "."+FileUtil.FITS,
//                                            VisContext.getVisSessionDir());
//            } catch (IOException e) {
//                f= new File(VisContext.getVisSessionDir(),
//                                geomTmp + "-"+idx +"-cropAndCenter." + FileUtil.FITS);
//            }
//            return f;
//        }
//
//    }



    public static File makeRotFileName(File templateFile, int idx, double angle) {
        String geomTmp= templateFile.getName();

        File f;
        try {
            String angleDesc= Double.isNaN(angle) ? "north" : angle+"";
            f= File.createTempFile(geomTmp + "-"+idx +"-rot-"+angleDesc,
                                   "."+FileUtil.FITS,
                                   VisContext.getVisSessionDir());
        } catch (IOException e) {
            f= new File(VisContext.getVisSessionDir(),
                        geomTmp + "-"+idx +"-rot-north." + FileUtil.FITS);
        }
        return f;
    }


    static File makeCropCenterFileName(File templateFile, int idx, WorldPt wpt, double size) {
        String geomTmp= templateFile.getName();

        File f;
        try {
            DecimalFormat df = new DecimalFormat("##.##");
            String cropDesc= (df.format(wpt.getLon())+"+"+df.format(wpt.getLat())+"x"+df.format(size)+"-")
                    .replaceAll("\\+\\-","\\-");
            f= File.createTempFile(geomTmp + "-"+idx +"-cropCenter-"+cropDesc,
                                   "."+FileUtil.FITS,
                                   VisContext.getVisSessionDir());
        } catch (IOException e) {
            f= new File(VisContext.getVisSessionDir(),
                        geomTmp + "-"+idx +"-cropAndCenter." + FileUtil.FITS);
        }
        return f;
    }

    public static File makeFlipYFileName(File templateFile, int idx) {
        String geomTmp= templateFile.getName();

        File f;
        try {
            f= File.createTempFile(geomTmp+ "-"+idx + "-flipedY-", "."+FileUtil.FITS,
                                   VisContext.getVisSessionDir());
        } catch (IOException e) {
            f= new File(VisContext.getVisSessionDir(), geomTmp +"-"+idx + "-flipedY."+FileUtil.FITS);
        }
        return f;
    }

    public static File makeFlipXFileName(File templateFile, int idx) {
        String geomTmp= templateFile.getName();

        File f;
        try {
            f= File.createTempFile(geomTmp+ "-"+idx + "-flipedX-", "."+FileUtil.FITS,
                                   VisContext.getVisSessionDir());
        } catch (IOException e) {
            f= new File(VisContext.getVisSessionDir(), geomTmp +"-"+idx + "-flipedX."+FileUtil.FITS);
        }
        return f;
    }
}

