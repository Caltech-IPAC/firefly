/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.visualize.fitseval.FitsDataEval;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.plotdata.GeomException;
import nom.tam.fits.FitsException;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * @author Trey Roby
 *
 * 07/07/15
 * The Crop class is deprecated. The same function is added to CropAndCenter.
 */
public class WebPlotReader {

    private static final Logger.LoggerImpl _log = Logger.getLogger();

    public static Map<Band, FileReadInfo[]> readFiles(Map<Band, FileInfo> fitsFiles, WebPlotRequest req)
            throws IOException,
                   FitsException,
                   FailedRequestException,
                   GeomException {

        Map<Band, FileReadInfo[]> retMap = new LinkedHashMap<>();
        for (Map.Entry<Band, FileInfo> entry : fitsFiles.entrySet()) {
            Band band = entry.getKey();
            FileReadInfo info[] = readOneFits(entry.getValue(), band, req);
            retMap.put(band, info);
        }
        return retMap;
    }


// Unused code commented out 3/5/2019 - keep around for awhile
//    /**
//     * @param fd file data
//     * @param req WebPlotRequest from the search, usually the first
//     * @return the ReadInfo[] object
//     * @throws java.io.IOException        any io problem
//     * @throws nom.tam.fits.FitsException problem reading the fits file
//     * @throws edu.caltech.ipac.util.download.FailedRequestException any other problem
//     * @throws GeomException problem reprojecting
//     */
//    public static Map<Band, FileReadInfo[]> processFitsRead(FileInfo fd, WebPlotRequest req, FitsRead fitsRead, int imageIdx)
//            throws IOException,
//                   FitsException,
//                   FailedRequestException,
//                   GeomException {
//
//        FileReadInfo retval;
//
//        File originalFile = fd.getFile();
//        String uploadedName= null;
//        if (ServerContext.isInUploadDir(originalFile) || originalFile.getName().startsWith("upload_")) {
//            uploadedName= fd.getDesc();
//        }
//
//        ModFileWriter modFileWriter= null;
//        if (req!=null) {
//            WebPlotPipeline.PipelineRet pipeRet= WebPlotPipeline.applyPipeline(req, fitsRead, imageIdx, Band.NO_BAND, originalFile);
//            if (pipeRet.modFileWriter!=null && fitsRead!=pipeRet.fr) {
//                modFileWriter= pipeRet.modFileWriter;
//                FitsCacher.addFitsReadToCache(modFileWriter.getTargetFile(), new FitsRead[]{pipeRet.fr});
//                fitsRead= pipeRet.fr;
//            }
//        }
//        modFileWriter= checkUnzip(imageIdx,Band.NO_BAND,originalFile, modFileWriter);
//
//        retval= new FileReadInfo(originalFile, fitsRead, Band.NO_BAND, imageIdx,
//                                 fd.getDesc(), uploadedName, null, modFileWriter);
//
//        Map<Band, FileReadInfo[]> retMap= new HashMap<>(1);
//        retMap.put(Band.NO_BAND, new FileReadInfo[] {retval});
//
//        return retMap;
//    }




    /**
     * @param fd file data
     * @param band which band
     * @param req WebPlotRequest from the search, usually the first
     * @return the ReadInfo[] object
     * @throws java.io.IOException        any io problem
     * @throws nom.tam.fits.FitsException problem reading the fits file
     * @throws edu.caltech.ipac.util.download.FailedRequestException
     *                                    any other problem
     * @throws GeomException
     *                                    problem reprojecting
     */
    public static FileReadInfo[] readOneFits(FileInfo fd, Band band, WebPlotRequest req)
            throws IOException,
                   FitsException,
                   FailedRequestException,
                   GeomException {

        FileReadInfo retval[];

        File originalFile = fd.getFile();
        String uploadedName= null;
        // note: add case for upload from workspace
        if (ServerContext.isInUploadDir(originalFile) || originalFile.getName().startsWith("upload_") ||
                originalFile.getName().startsWith("ws-upload")) {
            uploadedName= fd.getDesc();
        }

        boolean usePipeline= needsPipeline(req);
        FitsDataEval fitsDataEval = FitsCacher.readFits(fd, req, !usePipeline, !usePipeline); // turn caching off if I have to use pipeline
        FitsRead frAry[]= fitsDataEval.getFitReadAry();
        retval = new FileReadInfo[frAry.length];

        if (usePipeline) { // if we need to use the pipeline make sure we create a new array
            FitsRead newFrAry[]= new FitsRead[frAry.length];
            int imageIdx;
            for (int i = 0; (i < frAry.length); i++) {
                imageIdx= i;
                WebPlotPipeline.PipelineRet pipeRet= WebPlotPipeline.applyPipeline(req, frAry[i], imageIdx, band, originalFile);
                newFrAry[i]= pipeRet.fr;
                ModFileWriter modFileWriter= checkUnzip(i,band,originalFile, pipeRet.modFileWriter);

                retval[i]= new FileReadInfo(originalFile, newFrAry[i], fd, band, imageIdx,
                        fd.getDesc(), uploadedName, null, modFileWriter);
            }
        }
        else {
            for (int i = 0; (i < frAry.length); i++) {
                retval[i]= new FileReadInfo(originalFile, frAry[i], fd, band, i, fd.getDesc(), uploadedName,
                        fitsDataEval.getRelatedData(i), null);
            }
        }

        return retval;
    }

    private static boolean needsPipeline(WebPlotRequest req) {
        if (req==null) return false;
        return (req.isFlipY() || req.isFlipX() || req.getPostCropAndCenter() || req.getPostCrop() || WebPlotPipeline.isRotation(req));
    }

    private static ModFileWriter checkUnzip(int i, Band band, File originalFile, ModFileWriter modFileWriter)  {
        if (i == 0 &&  modFileWriter == null && isUnzipNecessary(originalFile) ) {

            _log.warn("Setting up unzip ModFileWriter, this procedure is deprecated.",
                      "We should not just unzip the fits files up front.",
                       originalFile.getAbsolutePath());
            modFileWriter = new ModFileWriter.UnzipFileWriter(band, originalFile);
        }
        return modFileWriter;
    }


    private static boolean isUnzipNecessary(File f) {
        String ext = FileUtil.getExtension(f);
        return (ext != null && ext.equalsIgnoreCase(FileUtil.GZ));
    }

    static void validateAccess(Map<Band, FileInfo> fitsFiles) throws FailedRequestException {
        for (FileInfo rf : fitsFiles.values()) {
            if (!rf.isBlank()) {
                File f = rf.getFile();
                if (!f.exists()) {
                    PlotServUtils.statsLog("file-not-found", "fname", f.getPath());
                    throw new FailedRequestException("File does not exist.", "Fits read failed, no read access: " + concatFileNames(fitsFiles));
                }
                if (!f.canRead()) {
                    PlotServUtils.statsLog("cannot-read", "fname", f.getPath());
                    throw new FailedRequestException("File is not readable.", "Fits read failed, no read access: " + concatFileNames(fitsFiles));
                }
                if (f.length()>VisContext.FITS_MAX_SIZE) {
                    PlotServUtils.statsLog("file-too-large", "fname", f.getPath());
                    String sStr= FileUtil.getSizeAsString(VisContext.FITS_MAX_SIZE);
                    throw new FailedRequestException("File is too large, exceeds size: " + sStr + ".",
                            "file is to large to read, exceeds size:"  +sStr+", " +  concatFileNames(fitsFiles));
                }
            }
        }
    }

    private static String concatFileNames(Map<Band, FileInfo> fitsFiles) {
        StringBuilder sb = new StringBuilder(200);
        boolean first = true;
        for (FileInfo rf : fitsFiles.values()) {
            File f = rf.getFile();
            if (!first) sb.append(", ");
            sb.append(f);
            first = false;
        }
        return sb.toString();
    }


}

