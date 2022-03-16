/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.RelatedData;
import edu.caltech.ipac.firefly.server.ServerContext;
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
import java.util.List;
import java.util.Map;


/**
 * @author Trey Roby
 */
public class WebPlotReader {

    public static Map<Band, FileReadInfo[]> readFiles(Map<Band, FileInfo> fitsFiles, WebPlotRequest req)
            throws IOException, FitsException, FailedRequestException, GeomException {
        Map<Band, FileReadInfo[]> retMap = new LinkedHashMap<>();
        for (var entry : fitsFiles.entrySet()) {
            var band = entry.getKey();
            var fi= entry.getValue();
            var info = needsPipeline(req) ? readWithPipeline(fi,band,req) : readNormal(fi,band,req);
            retMap.put(band, info);
        }
        return retMap;
    }

    /**
     * Read everything from the FITS file. All network image plotting firefly calls will use this code
     */
    private static FileReadInfo[] readNormal(FileInfo fd, Band band, WebPlotRequest req)
                                                    throws IOException, FitsException {
        var fitsDataEval = FitsCacher.readFits(fd, req, true, true);
        var frAry= fitsDataEval.getFitReadAry();
        var retval = new FileReadInfo[frAry.length];
        var workingFile= fitsDataEval.getHduUnCompressedFile()!=null?
                fitsDataEval.getHduUnCompressedFile():fd.getFile();
        for (int i = 0; (i < frAry.length); i++) {
            retval[i]= new FileReadInfo(fd.getFile(), workingFile, frAry[i], fd, band, i,
                    fd.getDesc(), getUploadedName(fd), fitsDataEval.getRelatedData(i));
        }
        return retval;
    }


    /**
     *  read with pipeline - this is not commonly used more from normal firefly calls, since most operations
     *  can be handled on the client
     *  finder chart still uses this code when creating a PDF
     */
    private static FileReadInfo[] readWithPipeline(FileInfo fd, Band band, WebPlotRequest req)
            throws IOException, FitsException, FailedRequestException, GeomException {
        File originalFile = fd.getFile();
        var fitsDataEval = FitsCacher.readFits(fd, req, false, false); // turn caching off if I have to use pipeline
        var frAry= fitsDataEval.getFitReadAry();
        var retval = new FileReadInfo[frAry.length];
        FitsRead[] newFrAry= new FitsRead[frAry.length];
        for (int imageIdx = 0; (imageIdx < frAry.length); imageIdx++) {
            newFrAry[imageIdx]= WebPlotPipeline.applyPipeline(req, frAry[imageIdx]);

            retval[imageIdx]= new FileReadInfo(originalFile, originalFile, newFrAry[imageIdx], fd, band, imageIdx,
                    fd.getDesc(), getUploadedName(fd), null);
        }
        return retval;
    }

    private static String getUploadedName(FileInfo fileInfo) {
        File originalFile = fileInfo.getFile();
        if (ServerContext.isInUploadDir(originalFile) || originalFile.getName().startsWith("upload_") ||
                originalFile.getName().startsWith("ws-upload")) {
            return fileInfo.getDesc();
        }
        return null;
    }

    private static boolean needsPipeline(WebPlotRequest req) {
        if (req==null) return false;
        return (req.isFlipY() || req.isFlipX() || req.getPostCropAndCenter() || req.getPostCrop() || WebPlotPipeline.isRotation(req));
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
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (FileInfo rf : fitsFiles.values()) {
            if (!first) sb.append(", ");
            sb.append(rf.getFile());
            first = false;
        }
        return sb.toString();
    }


    public record FileReadInfo(File originalFile, File workingFile, FitsRead fitsRead,
                                      FileInfo fileInfo, Band band, int originalImageIdx, String dataDesc,
                                      String uploadedName, List<RelatedData> relatedData) { }
}

