package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 2/16/11
 * Time: 3:49 PM
 */


import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.firefly.server.util.multipart.UploadFileInfo;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.visualize.plot.Circle;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.Crop;
import edu.caltech.ipac.visualize.plot.CropAndCenter;
import edu.caltech.ipac.visualize.plot.FitsRead;
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
     * @param band which band
     * @param req WebPlotRequest from the search, usually the first
     * @return the ReadInfo[] object
     * @throws java.io.IOException        any io problem
     * @throws nom.tam.fits.FitsException problem reading the fits file
     * @throws edu.caltech.ipac.client.net.FailedRequestException
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
            if (VisContext.isInUploadDir(originalFile)) {
                String fileKey= VisContext.replaceWithPrefix(originalFile);
                Cache cache= CacheManager.getCache(Cache.TYPE_HTTP_SESSION);
                UploadFileInfo fi= (UploadFileInfo)cache.get(new StringKey(fileKey));
                if (fi!=null) uploadedName= fi.getFileName();
            }

            FitsRead frAry[]= PlotServUtils.readFits(originalFile);
            retval = new FileReadInfo[frAry.length];
            for (int i = 0; (i < frAry.length); i++) {
                imageIdx= i;
                modFileWriter= null;
                if (req!=null) applyPipeline(req, frAry, i, band, originalFile);
                checkUnzip(i,band,originalFile);

                retval[i]= new FileReadInfo(originalFile, frAry[i], band, imageIdx,
                                            fd.getDesc(), uploadedName, modFileWriter);
            }
        }
        VisContext.shouldContinue(workingCtxStr);

        return retval;
    }

    private void applyPipeline(WebPlotRequest req, FitsRead[] frAry, int i, Band band, File originalFile)
                                        throws IOException,
                                               FitsException,
                                               FailedRequestException,
                                               GeomException {
        modFileWriter = null;
        imageIdx = i;
        for(WebPlotRequest.Order order : req.getPipelineOrder()) {
            switch (order) {
                case FLIP_Y:
                    applyFlip(req,frAry,i,band,originalFile);
                    break;
                case ROTATE:
                    applyRotation(req,frAry,i,band,originalFile);
                    break;
                case POST_CROP:
                    applyCrop(req, frAry, i, band, originalFile);
                    break;
                case POST_CROP_AND_CENTER:
                    applyCropAndCenter(req, frAry, i, band, originalFile);
                    break;
            }
        }
    }

    private void checkUnzip(int i, Band band, File originalFile)  {
        if (i == 0 &&  modFileWriter == null && isUnzipNecessary(originalFile) ) {
            modFileWriter = new ModFileWriter.UnzipFileWriter(band, originalFile);
        }
    }




    private void applyRotation(WebPlotRequest req, FitsRead[] frAry, int i, Band band, File originalFile)
                                                  throws FailedRequestException,
                                                         GeomException,
                                                         FitsException,
                                                         IOException {
        if (isRotation(req) && canRotate(frAry[i])) {
            if (req.getRotateNorth()) {
                if (req.getRotateNorthType().equals(CoordinateSys.EQ_J2000)) {
                    frAry[i] = FitsRead.createFitsReadNorthUp(frAry[i]);
                } else if (req.getRotateNorthType().equals(CoordinateSys.GALACTIC)) {
                    frAry[i] = FitsRead.createFitsReadNorthUpGalactic(frAry[i]);
                } else {
                    throw new FailedRequestException("Rotation Failed",
                                                     "Fits read failed, rotation type not supported: " +
                                                             req.getRotateNorthType().toString());
                }
            } else if (req.getRotate()) {
                frAry[i] = FitsRead.createFitsReadRotated(frAry[i], req.getRotationAngle());
            }
            imageIdx = 0;
            File rotFile= ModFileWriter.makeRotFileName(originalFile,imageIdx,req.getRotationAngle());
            modFileWriter = new ModFileWriter.GeomFileWriter(rotFile, frAry[i], band);
        }

    }

    private void applyCrop(WebPlotRequest req, FitsRead[] frAry, int i, Band band, File originalFile)
                                                  throws FailedRequestException,
                                                         GeomException,
                                                         FitsException,
                                                         IOException {
        if (req.getPostCrop()) {
            Fits inFits = frAry[i].getFits();
            Pt pt1;
            Pt pt2;
            if (getCropPt1(req) instanceof WorldPt && getCropPt2(req) instanceof WorldPt) {
                ImagePlot tmpIM = new ImagePlot(null, frAry[i], 1F, false, 0, FitsRead.getDefaultFutureStretch(), false);
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
                Fits cropFits = Crop.do_crop(inFits, (int) pt1.getX(), (int) pt1.getY(),
                                             (int) pt2.getX(), (int) pt2.getY());
                FitsRead fr[] = FitsRead.createFitsReadArray(cropFits);
                frAry[i] = fr[0];
                File rotName= ModFileWriter.makeRotFileName(originalFile,imageIdx, req.getRotationAngle());
                modFileWriter = new ModFileWriter.GeomFileWriter(rotName, frAry[i], band);
            }
        }

    }


    private void applyCropAndCenter(WebPlotRequest req, FitsRead[] frAry, int i, Band band, File originalFile)
                                                     throws  FailedRequestException,
                                                             GeomException,
                                                             FitsException,
                                                             IOException {
        if (req.getPostCropAndCenter()) {
            WorldPt wpt = VisUtil.convert(getCropCenter(req), req.getPostCropAndCenterType());
            double size = getImageSize(req) / 2.;
            FitsRead fr = CropAndCenter.do_crop(frAry[i], wpt.getLon(), wpt.getLat(), size);
            frAry[i] = fr;
            File cropName= ModFileWriter.makeCropCenterFileName(originalFile,imageIdx, wpt,size);
            modFileWriter = new ModFileWriter.GeomFileWriter(cropName,frAry[i],band);
        }
    }

    private void applyFlip(WebPlotRequest req, FitsRead[] frAry, int i, Band band, File originalFile)
                                                        throws  FailedRequestException,
                                                                GeomException,
                                                                FitsException,
                                                                IOException {
        if (req.isFlipY()) {
           frAry[i]= FitsRead.createFitsReadFlipLR(frAry[i]);
            File flipName= ModFileWriter.makeFlipYFileName(originalFile,imageIdx);
            modFileWriter = new ModFileWriter.GeomFileWriter(flipName,frAry[i],band);
        }
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
                    throw new FailedRequestException("Does not exist", "Fits read failed, no read access: " + concatFileNames(fitsFiles));
                }
                if (!f.canRead()) {
                    PlotServUtils.statsLog("cannot-read", "fname", f.getPath());
                    throw new FailedRequestException("Not readable", "Fits read failed, no read access: " + concatFileNames(fitsFiles));
                }
                if (f.length()>VisContext.FITS_MAX_SIZE) {
                    PlotServUtils.statsLog("file-too-large", "fname", f.getPath());
                    String sStr= FileUtil.getSizeAsString(VisContext.FITS_MAX_SIZE);
                    throw new FailedRequestException("File too large, exceeds size: " + sStr,
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

    private Pt getCropPt1(WebPlotRequest r) {
        return r.getCropImagePt1() != null ? r.getCropImagePt1() : r.getCropWorldPt1();
    }

    private Pt getCropPt2(WebPlotRequest r) {
        return r.getCropImagePt2() != null ? r.getCropImagePt2() : r.getCropWorldPt2();
    }

    private WorldPt getCropCenter(WebPlotRequest r) {
        Circle c=PlotServUtils.getRequestArea(r);
        return (c!=null) ? c.getCenter() : null;
    }

    private double getImageSize(WebPlotRequest r) {
        Circle c=PlotServUtils.getRequestArea(r);
        return (c!=null) ? c.getRadius() : 0.0;
    }


}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313)
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
 * HOWEVER USED.
 *
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 *
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
 * OF THE SOFTWARE.
 */

