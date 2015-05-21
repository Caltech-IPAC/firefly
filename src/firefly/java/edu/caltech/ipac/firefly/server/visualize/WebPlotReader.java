/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 2/16/11
 * Time: 3:49 PM
 */


import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.Circle;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.Crop;
import edu.caltech.ipac.visualize.plot.CropAndCenter;
import edu.caltech.ipac.visualize.plot.FitsRead;
import edu.caltech.ipac.visualize.plot.FlipXY;
import edu.caltech.ipac.visualize.plot.GeomException;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.WorldPt;
import edu.caltech.ipac.visualize.plot.projection.Projection;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class WebPlotReader {

    private final String workingCtxStr;
    private ModFileWriter modFileWriter = null;
    private int imageIdx;

    /**
     * @param workingCtxStr ctx string
     */
    public WebPlotReader(String workingCtxStr) {
        this.workingCtxStr= workingCtxStr;
    }

    public WebPlotReader() { this(null);  }

    public Map<Band, FileReadInfo[]> readFiles(Map<Band, FileData> fitsFiles, WebPlotRequest req)
            throws IOException,
                   FitsException,
                   FailedRequestException,
                   GeomException {

        Map<Band, FileReadInfo[]> retMap = new LinkedHashMap<Band, FileReadInfo[]>();
        for (Map.Entry<Band, FileData> entry : fitsFiles.entrySet()) {
            Band band = entry.getKey();
            FileReadInfo info[] = readOneFits(entry.getValue(), band, req);
            VisContext.shouldContinue(workingCtxStr);
            retMap.put(band, info);
        }
        return retMap;
    }






    /**
     * @param fd file data
     * @param req WebPlotRequest from the search, usually the first
     * @return the ReadInfo[] object
     * @throws java.io.IOException        any io problem
     * @throws nom.tam.fits.FitsException problem reading the fits file
     * @throws edu.caltech.ipac.util.download.FailedRequestException
     *                                    any other problem
     * @throws edu.caltech.ipac.visualize.plot.GeomException
     *                                    problem reprojecting
     */
    public Map<Band, FileReadInfo[]> processFitsRead(FileData fd, WebPlotRequest req, FitsRead fitsRead, int imageIdx)
            throws IOException,
                   FitsException,
                   FailedRequestException,
                   GeomException {

        FileReadInfo retval;

        File originalFile = fd.getFile();
        String uploadedName= null;
        if (ServerContext.isInUploadDir(originalFile)) {
            uploadedName= fd.getDesc();
        }

        modFileWriter= null;
        this.imageIdx= imageIdx;
        FitsRead inFitsRead= fitsRead;
        if (req!=null) {
            fitsRead= applyPipeline(req, inFitsRead, this.imageIdx, Band.NO_BAND, originalFile);
            if (modFileWriter!=null && inFitsRead!=fitsRead) {
                FitsCacher.addFitsReadToCache(modFileWriter.getTargetFile(), new FitsRead[]{fitsRead});
            }
        }
        checkUnzip(this.imageIdx,Band.NO_BAND,originalFile);

        retval= new FileReadInfo(originalFile, fitsRead, Band.NO_BAND, imageIdx,
                                 fd.getDesc(), uploadedName, modFileWriter);
        VisContext.shouldContinue(workingCtxStr);

        Map<Band, FileReadInfo[]> retMap= new HashMap<Band, FileReadInfo[]>(1);
        retMap.put(Band.NO_BAND, new FileReadInfo[] {retval});

        return retMap;
    }




    /**
     * @param fd file data
     * @param band which band
     * @param req WebPlotRequest from the search, usually the first
     * @return the ReadInfo[] object
     * @throws java.io.IOException        any io problem
     * @throws nom.tam.fits.FitsException problem reading the fits file
     * @throws edu.caltech.ipac.util.download.FailedRequestException
     *                                    any other problem
     * @throws edu.caltech.ipac.visualize.plot.GeomException
     *                                    problem reprojecting
     */
    public FileReadInfo[] readOneFits(FileData fd, Band band, WebPlotRequest req)
            throws IOException,
                   FitsException,
                   FailedRequestException,
                   GeomException {

        FileReadInfo retval[];

        if (fd.isBlank())  {
            retval= new FileReadInfo[] { new FileReadInfo(null, PlotServUtils.createBlankFITS(req)[0],
                                                          band, 0, fd.getDesc(), null, null) };
        }
        else {
            imageIdx= 0;
            File originalFile = fd.getFile();
            String uploadedName= null;
            if (ServerContext.isInUploadDir(originalFile)) {
                uploadedName= fd.getDesc();
            }

            FitsRead frAry[]= FitsCacher.readFits(originalFile);
            retval = new FileReadInfo[frAry.length];
            for (int i = 0; (i < frAry.length); i++) {
                imageIdx= i;
                modFileWriter= null;
                if (req!=null) {
                    frAry[i]= applyPipeline(req, frAry[i], imageIdx, band, originalFile);
                }
                checkUnzip(i,band,originalFile);

                retval[i]= new FileReadInfo(originalFile, frAry[i], band, imageIdx,
                                            fd.getDesc(), uploadedName, modFileWriter);
            }
        }
        VisContext.shouldContinue(workingCtxStr);

        return retval;
    }

    private FitsRead applyPipeline(WebPlotRequest req, FitsRead fr, int imageIdx, Band band, File originalFile)
                                        throws IOException,
                                               FitsException,
                                               FailedRequestException,
                                               GeomException {
        modFileWriter = null;
        this.imageIdx = imageIdx;
        for(WebPlotRequest.Order order : req.getPipelineOrder()) {
            switch (order) {
                case FLIP_Y:
                    fr= applyFlipYAxis(req, fr, band, originalFile);
                    break;
                case FLIP_X:
                    fr= applyFlipXAxis(req, fr, band, originalFile);
                    break;
                case ROTATE:
                    fr= applyRotation(req,fr,band,originalFile);
                    break;
                case POST_CROP:
                    fr= applyCrop(req, fr, band, originalFile);
                    break;
                case POST_CROP_AND_CENTER:
                    fr= applyCropAndCenter(req, fr, band, originalFile);
                    break;
            }
        }
        return fr;
    }

    private void checkUnzip(int i, Band band, File originalFile)  {
        if (i == 0 &&  modFileWriter == null && isUnzipNecessary(originalFile) ) {
            modFileWriter = new ModFileWriter.UnzipFileWriter(band, originalFile);
        }
    }




    private FitsRead applyRotation(WebPlotRequest req, FitsRead fr, Band band, File originalFile)
                                                  throws FailedRequestException,
                                                         GeomException,
                                                         FitsException,
                                                         IOException {
        FitsRead retval= fr;
        if (isRotation(req) && canRotate(fr)) {
            if (req.getRotateNorth()) {
                if (req.getRotateNorthType().equals(CoordinateSys.EQ_J2000)) {
                    retval = FitsRead.createFitsReadNorthUp(fr);
                } else if (req.getRotateNorthType().equals(CoordinateSys.GALACTIC)) {
                    retval = FitsRead.createFitsReadNorthUpGalactic(fr);
                } else {
                    throw new FailedRequestException("Rotation Failed",
                                                     "Fits read failed, rotation type not supported: " +
                                                             req.getRotateNorthType().toString());
                }
            } else if (req.getRotate()) {
                retval = FitsRead.createFitsReadRotated(fr, req.getRotationAngle());
            }
            imageIdx = 0;
            File rotFile= ModFileWriter.makeRotFileName(originalFile,imageIdx,req.getRotationAngle());
            modFileWriter = new ModFileWriter.GeomFileWriter(rotFile, retval, band);
        }
        return retval;

    }

    private FitsRead applyCrop(WebPlotRequest req, FitsRead fr, Band band, File originalFile)
                                                  throws FailedRequestException,
                                                         GeomException,
                                                         FitsException,
                                                         IOException {
        FitsRead retval= fr;
        if (req.getPostCrop()) {
            Pt pt1;
            Pt pt2;
            if (getCropPt1(req) instanceof WorldPt && getCropPt2(req) instanceof WorldPt) {
                ActiveFitsReadGroup frGroup= new ActiveFitsReadGroup();
                frGroup.setFitsRead(Band.NO_BAND,fr);
                ImagePlot tmpIM = new ImagePlot(null, frGroup, 1F, false, Band.NO_BAND, 0, FitsRead.getDefaultFutureStretch());
                try {
                    pt1 = tmpIM.getImageCoords((WorldPt) getCropPt1(req));
                    pt2 = tmpIM.getImageCoords((WorldPt) getCropPt2(req));
                } catch (ProjectionException e) {
                    pt1 = null;
                    pt2 = null;
                }
            } else {
                pt1 = getCropPt1(req);
                pt2 = getCropPt2(req);
            }
            if (pt1 != null && pt2 != null) {
//              Fits inFits = fr.getFits();
                Fits inFits = fr.createNewFits();
                Fits cropFits = Crop.do_crop(inFits, (int) pt1.getX(), (int) pt1.getY(),
                                                     (int) pt2.getX(), (int) pt2.getY());
                FitsRead tmpFr[] = FitsRead.createFitsReadArray(cropFits);
                retval = tmpFr[0];
                File rotName= ModFileWriter.makeRotFileName(originalFile,imageIdx, req.getRotationAngle());
                modFileWriter = new ModFileWriter.GeomFileWriter(rotName, retval, band);
            }
        }
        return retval;

    }


    private FitsRead applyCropAndCenter(WebPlotRequest req, FitsRead fr, Band band, File originalFile)
                                                     throws  FailedRequestException,
                                                             GeomException,
                                                             FitsException,
                                                             IOException {
        FitsRead retval= fr;
        if (req.getPostCropAndCenter()) {
            WorldPt wpt = VisUtil.convert(getCropCenter(req), req.getPostCropAndCenterType());
            double size = getImageSize(req) / 2.;
            retval= CropAndCenter.do_crop(fr, wpt.getLon(), wpt.getLat(), size);
            File cropName= ModFileWriter.makeCropCenterFileName(originalFile,imageIdx, wpt,size);
            modFileWriter = new ModFileWriter.GeomFileWriter(cropName,retval,band);
        }
        return retval;
    }

    private FitsRead applyFlipYAxis(WebPlotRequest req, FitsRead fr, Band band, File originalFile)
                                                        throws  FailedRequestException,
                                                                GeomException,
                                                                FitsException,
                                                                IOException {
        FitsRead retval= fr;
        if (req.isFlipY()) {
            retval= FitsRead.createFitsReadFlipLR(fr);
            File flipName= ModFileWriter.makeFlipYFileName(originalFile,imageIdx);
            modFileWriter = new ModFileWriter.GeomFileWriter(flipName,retval,band);
        }
        return retval;
    }

    private FitsRead applyFlipXAxis(WebPlotRequest req, FitsRead fr, Band band, File originalFile)
            throws  FailedRequestException,
                    GeomException,
                    FitsException,
                    IOException {
        FitsRead retval= fr;
        if (req.isFlipX()) {
            retval= new FlipXY(fr,"xAxis").doFlip();
            File flipName= ModFileWriter.makeFlipXFileName(originalFile,imageIdx);
            modFileWriter = new ModFileWriter.GeomFileWriter(flipName,retval,band);
        }
        return retval;
    }

    private static boolean isUnzipNecessary(File f) {
        String ext = FileUtil.getExtension(f);
        return (ext != null && ext.equalsIgnoreCase(FileUtil.GZ));
    }

    private static boolean canRotate(FitsRead fr) {
        int projType = fr.getProjectionType();
        return (projType != Projection.AITOFF &&
                projType != Projection.UNRECOGNIZED &&
                projType != Projection.UNSPECIFIED);
    }

    public static void validateAccess(Map<Band, FileData> fitsFiles) throws FailedRequestException {
        for (FileData rf : fitsFiles.values()) {
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

    private static String concatFileNames(Map<Band, FileData> fitsFiles) {
        StringBuffer sb = new StringBuffer(200);
        boolean first = true;
        for (FileData rf : fitsFiles.values()) {
            File f = rf.getFile();
            if (!first) sb.append(", ");
            sb.append(f);
            first = false;
        }
        return sb.toString();
    }


    public static boolean isRotation(WebPlotRequest r) {
        return r!=null && ((r.getRotate() && !Double.isNaN(r.getRotationAngle())) || r.getRotateNorth());
    }

    private static Pt getCropPt1(WebPlotRequest r) {
        return r.getCropImagePt1() != null ? r.getCropImagePt1() : r.getCropWorldPt1();
    }

    private static Pt getCropPt2(WebPlotRequest r) {
        return r.getCropImagePt2() != null ? r.getCropImagePt2() : r.getCropWorldPt2();
    }

    private static WorldPt getCropCenter(WebPlotRequest r) {
        Circle c=PlotServUtils.getRequestArea(r);
        return (c!=null) ? c.getCenter() : null;
    }

    private static double getImageSize(WebPlotRequest r) {
        Circle c=PlotServUtils.getRequestArea(r);
        return (c!=null) ? c.getRadius() : 0.0;
    }


}

