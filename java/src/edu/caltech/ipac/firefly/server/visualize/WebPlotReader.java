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


    public static Map<Band, FileReadInfo[]> readFiles(String workingCtxStr,
                                                      Map<Band, FileData> fitsFiles,
                                                      WebPlotRequest req,
                                                      String addDateTitleStr)
            throws IOException,
                   FitsException,
                   FailedRequestException,
                   GeomException {

        Map<Band, FileReadInfo[]> retMap = new LinkedHashMap<Band, FileReadInfo[]>();
        for (Map.Entry<Band, FileData> entry : fitsFiles.entrySet()) {
            Band band = entry.getKey();
            FileReadInfo info[] = readOneFits(workingCtxStr, entry.getValue(), band, req, addDateTitleStr);
            VisContext.shouldContinue(workingCtxStr);
            retMap.put(band, info);
        }
        return retMap;
    }

    /**
     * @param workingCtxStr ctx string
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
    public static FileReadInfo[] readOneFits(String workingCtxStr, FileData fd, Band band, WebPlotRequest req)
            throws IOException,
                   FitsException,
                   FailedRequestException,
                   GeomException {
        return readOneFits(workingCtxStr, fd, band, req, null);
    }

    /**
     * @param fd   the file to read
     * @param band which band
     * @return the ReadInfo[] object
     * @throws java.io.IOException        any io problem
     * @throws nom.tam.fits.FitsException problem reading the fits file
     * @throws edu.caltech.ipac.client.net.FailedRequestException
     *                                    any other problem
     * @throws edu.caltech.ipac.visualize.plot.GeomException
     *                                    problem reprojecting
     */
    private static FileReadInfo[] readOneFits(String workingCtxStr,
                                              FileData fd,
                                              Band band,
                                              WebPlotRequest req,
                                              String addDateTitleStr)
            throws IOException,
                   FitsException,
                   FailedRequestException,
                   GeomException {

        File originalFile = fd.getFile();
        String uploadedName= null;
        FitsRead frAry[];
        boolean isBlank= false;


        if (VisContext.isInUploadDir(originalFile)) {
            String fileKey= VisContext.replaceWithPrefix(originalFile);
            Cache cache= CacheManager.getCache(Cache.TYPE_HTTP_SESSION);
            UploadFileInfo fi= (UploadFileInfo)cache.get(new StringKey(fileKey));
            if (fi!=null) uploadedName= fi.getFileName();
        }


        if (fd.isBlank())  {
            frAry= PlotServUtils.createBlankFITS(req);
            originalFile= null;
            isBlank= true;
        }
        else {
            frAry= PlotServUtils.readFits(originalFile);
        }
        VisContext.shouldContinue(workingCtxStr);

        FileReadInfo retval[] = new FileReadInfo[frAry.length];

        for (int i = 0; (i < frAry.length); i++) {
            ImageModRet imRet= applyPreModifications(req,frAry,i,band,originalFile,isBlank);
            int imageIdx= imRet.imageIdx;
            ModFileWriter modFileWriter= imRet.modFileWriter;

            if (i == 0 && !isBlank && modFileWriter == null && isUnzipNecesssary(originalFile) ) {
                modFileWriter = new ModFileWriter.UnzipFileWriter(band, originalFile);
            }

            String dateStr= (addDateTitleStr!=null) ? getDateValue(addDateTitleStr, frAry[i]) : "";

            retval[i]= new FileReadInfo(originalFile, frAry[i], band, imageIdx, fd.getDesc(), dateStr,
                                        uploadedName, modFileWriter);
        }

        return retval;
    }

    private static ImageModRet applyPreModifications(WebPlotRequest req,
                                                     FitsRead[] frAry,
                                                     int i,
                                                     Band band,
                                                     File originalFile,
                                                     boolean isBlank)  throws IOException,
                                                                              FitsException,
                                                                              FailedRequestException,
                                                                              GeomException {

        if (req==null || isBlank) return new ImageModRet(i,null);

        ModFileWriter modFileWriter = null;
        int imageIdx = i;
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
            modFileWriter = new ModFileWriter.RotateFileWriter(originalFile, imageIdx, frAry[i],
                                                               band, req.getRotationAngle(), false);
        }

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
                modFileWriter = new ModFileWriter.RotateFileWriter(originalFile, imageIdx, frAry[i],
                                                                   band, req.getRotationAngle(), true);
            }
        }

        if (req.getPostCropAndCenter()) {
            WorldPt wpt = VisUtil.convert(getCropCenter(req), req.getPostCropAndCenterType());
            double size = getImageSize(req) / 2.;
            FitsRead fr = CropAndCenter.do_crop(frAry[i], wpt.getLon(), wpt.getLat(), size);
            frAry[i] = fr;
            modFileWriter = new ModFileWriter.CropAndCenterFileWriter(originalFile, imageIdx, frAry[i],
                                                                      band, wpt, size);
        }
        return new ImageModRet(imageIdx, modFileWriter);
    }

    private static String getDateValue(String addDateTitleStr, FitsRead fr) {
        String retval = "";
        if (addDateTitleStr!=null && addDateTitleStr.contains(";")) {
            String dateAry[]= addDateTitleStr.split(";");
            String dateValue;
            long currentYear = Math.round(Math.floor(System.currentTimeMillis()/1000/3600/24/365.25) +1970);
            long year;
            String key = dateAry[0];
            dateValue= fr.getHDU().getHeader().getStringValue(key);
            if (key.equals("ORDATE")) {
                if (dateValue.length()>5) {
                    dateValue= dateValue.subSequence(0,2)+"-"+dateValue.subSequence(2,4)+"-"+
                            dateValue.subSequence(4,6);
                    year = 2000+Integer.parseInt(dateValue.subSequence(0,2).toString());
                    if (year > currentYear) {
                        dateValue = "19"+dateValue;
                    } else {
                        dateValue = "20"+dateValue;
                    }
                }
            } else if (key.equals("DATE-OBS")) {
                dateValue = dateValue.split("T")[0];
                if (dateValue.contains("/")) {
                    String newDate = "";
                    for (String v: dateValue.split("/")) {
                        if (newDate.length()==0) {
                            newDate = v;
                        } else {
                            newDate = v + "-" + newDate;
                        }
                    }
                    year = 2000+Integer.parseInt(newDate.subSequence(0,2).toString());
                    if (year > currentYear) {
                        dateValue = "19"+newDate;
                    } else {
                        dateValue = "20"+newDate;
                    }
                }
            } else if (key.equals("MIDOBS")) {
                dateValue = dateValue.split("T")[0];
            } else if (key.equals("DATEIRIS")) {
                dateValue = "1983";
                /*year = 2000+Integer.parseInt(dateValue.subSequence(0,2).toString());
                if (year > currentYear) {
                    dateValue = "19"+dateValue;
                } else {
                    dateValue = "20"+dateValue;
                }
                dateValue=dateValue.replaceAll("/","-");*/
            }
            retval = (dateAry[1]+":"+dateValue);
        }
        return retval;
    }

    static boolean isUnzipNecesssary(File f) {
        String ext = FileUtil.getExtension(f);
        return (ext != null && ext.equalsIgnoreCase(FileUtil.GZ));
    }

    static boolean canRotate(FitsRead fr) {

        int projType = fr.getProjectionType();
        return (projType != Projection.AITOFF &&
                projType != Projection.UNRECOGNIZED &&
                projType != Projection.UNSPECIFIED);
    }

    static void validateAccess(Map<Band, FileData> fitsFiles) throws FailedRequestException {
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
                    throw new FailedRequestException("File too big, exceeds size " + sStr,
                            "file is to large to read, exceeds size:"  +sStr+", " +  concatFileNames(fitsFiles));
                }
            }
        }
    }

    static String concatFileNames(Map<Band, FileData> fitsFiles) {
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

    private static class ImageModRet {
        public final int imageIdx;
        public final ModFileWriter modFileWriter;
        private ImageModRet(int imageIdx, ModFileWriter modFileWriter) {
            this.imageIdx = imageIdx;
            this.modFileWriter = modFileWriter;
        }
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

