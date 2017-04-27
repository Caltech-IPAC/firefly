/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.RelatedData;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.Circle;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Trey Roby
 *
 * 07/07/15
 * The Crop class is deprecated. The same function is added to CropAndCenter.
 */
public class WebPlotReader {

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


    /**
     * @param fd file data
     * @param req WebPlotRequest from the search, usually the first
     * @return the ReadInfo[] object
     * @throws java.io.IOException        any io problem
     * @throws nom.tam.fits.FitsException problem reading the fits file
     * @throws edu.caltech.ipac.util.download.FailedRequestException any other problem
     * @throws edu.caltech.ipac.visualize.plot.GeomException problem reprojecting
     */
    public static Map<Band, FileReadInfo[]> processFitsRead(FileInfo fd, WebPlotRequest req, FitsRead fitsRead, int imageIdx)
            throws IOException,
                   FitsException,
                   FailedRequestException,
                   GeomException {

        FileReadInfo retval;

        File originalFile = fd.getFile();
        String uploadedName= null;
        if (ServerContext.isInUploadDir(originalFile) || originalFile.getName().startsWith("upload_")) {
            uploadedName= fd.getDesc();
        }

        ModFileWriter modFileWriter= null;
        if (req!=null) {
            PipelineRet pipeRet= applyPipeline(req, fitsRead, imageIdx, Band.NO_BAND, originalFile);
            if (pipeRet.modFileWriter!=null && fitsRead!=pipeRet.fr) {
                modFileWriter= pipeRet.modFileWriter;
                FitsCacher.addFitsReadToCache(modFileWriter.getTargetFile(), new FitsRead[]{pipeRet.fr});
                fitsRead= pipeRet.fr;
            }
        }
        modFileWriter= checkUnzip(imageIdx,Band.NO_BAND,originalFile, modFileWriter);

        retval= new FileReadInfo(originalFile, fitsRead, Band.NO_BAND, imageIdx,
                                 fd.getDesc(), uploadedName, null, modFileWriter);

        Map<Band, FileReadInfo[]> retMap= new HashMap<>(1);
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
    public static FileReadInfo[] readOneFits(FileInfo fd, Band band, WebPlotRequest req)
            throws IOException,
                   FitsException,
                   FailedRequestException,
                   GeomException {

        FileReadInfo retval[];

        if (fd.isBlank())  {
            retval= new FileReadInfo[] { new FileReadInfo(null, PlotServUtils.createBlankFITS(req)[0],
                                                          band, 0, fd.getDesc(), null, null, null) };
        }
        else {
            File originalFile = fd.getFile();
            String uploadedName= null;
            if (ServerContext.isInUploadDir(originalFile) || originalFile.getName().startsWith("upload_")) {
                uploadedName= fd.getDesc();
            }

            FitsRead frAry[]= FitsCacher.readFits(originalFile);
            retval = new FileReadInfo[frAry.length];

            if (needsPipeline(req)) { // if we need to use the pipeline make sure we create a new array
                FitsRead newFrAry[]= new FitsRead[frAry.length];
                int imageIdx;
                for (int i = 0; (i < frAry.length); i++) {
                    imageIdx= i;
                    PipelineRet pipeRet= applyPipeline(req, frAry[i], imageIdx, band, originalFile);
                    newFrAry[i]= pipeRet.fr;
                    ModFileWriter modFileWriter= checkUnzip(i,band,originalFile, pipeRet.modFileWriter);

                    retval[i]= new FileReadInfo(originalFile, newFrAry[i], band, imageIdx,
                            fd.getDesc(), uploadedName, null, modFileWriter);
                }
            }
            else {
                List<List<RelatedData>> relatedDataList= investigateRelations(fd.getFile(),frAry);
                for (int i = 0; (i < frAry.length); i++) {
                    List<RelatedData> rd= relatedDataList!=null ? relatedDataList.get(i) : null;
                    retval[i]= new FileReadInfo(originalFile, frAry[i], band, i, fd.getDesc(), uploadedName, rd, null);
                }
            }
        }

        return retval;
    }

    private static PipelineRet applyPipeline(WebPlotRequest req, FitsRead fr, int imageIdx, Band band, File originalFile)
                                        throws IOException,
                                               FitsException,
                                               FailedRequestException,
                                               GeomException {
        ModFileWriter modFileWriter = null;
        PipelineRet pipeRet;
        for(WebPlotRequest.Order order : req.getPipelineOrder()) {
            pipeRet= null;
            switch (order) {
                case FLIP_Y:
                    pipeRet= applyFlipYAxis(req, fr, band, imageIdx, originalFile);
                    break;
                case FLIP_X:
                    pipeRet= applyFlipXAxis(req, fr, band, imageIdx, originalFile);
                    break;
                case ROTATE:
                    pipeRet= applyRotation(req,fr,band,imageIdx, originalFile);
                    break;
                case POST_CROP:
                    pipeRet= applyCrop(req, fr, band, imageIdx, originalFile);
                    break;
                case POST_CROP_AND_CENTER:
                    pipeRet= applyCropAndCenter(req, fr, band, imageIdx, originalFile);
                    break;
            }
            if (pipeRet!=null) {
                fr= pipeRet.fr;
                if (pipeRet.modFileWriter!=null) modFileWriter= pipeRet.modFileWriter;
            }
        }
        return new PipelineRet(fr,modFileWriter);
    }

    private static boolean needsPipeline(WebPlotRequest req) {
        if (req==null) return false;
        return (req.isFlipY() || req.isFlipX() || req.getPostCropAndCenter() || req.getPostCrop() || isRotation(req));
    }

    private static ModFileWriter checkUnzip(int i, Band band, File originalFile, ModFileWriter modFileWriter)  {
        if (i == 0 &&  modFileWriter == null && isUnzipNecessary(originalFile) ) {
            modFileWriter = new ModFileWriter.UnzipFileWriter(band, originalFile);
        }
        return modFileWriter;
    }




    private static PipelineRet applyRotation(WebPlotRequest req, FitsRead fr, Band band, int imageIdx, File originalFile)
                                                  throws FailedRequestException,
                                                         GeomException,
                                                         FitsException,
                                                         IOException {
        FitsRead retval= fr;
        ModFileWriter modFileWriter= null;
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
                retval = FitsRead.createFitsReadRotated(fr, req.getRotationAngle(), req.getRotateFromNorth());
            }
            File rotFile= ModFileWriter.makeRotFileName(originalFile,imageIdx,req.getRotationAngle());
            modFileWriter = new ModFileWriter.GeomFileWriter(rotFile, retval, band);
        }
        return new PipelineRet(retval, modFileWriter);

    }

    private static PipelineRet applyCrop(WebPlotRequest req, FitsRead fr, Band band, int imageIdx, File originalFile)
                                                  throws FailedRequestException,
                                                         GeomException,
                                                         FitsException,
                                                         IOException {
        FitsRead retval= fr;
        ModFileWriter modFileWriter= null;
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
                Fits inFits = fr.createNewFits();
                Fits cropFits = CropAndCenter.do_crop(inFits, (int) pt1.getX(), (int) pt1.getY(),
                        (int) pt2.getX(), (int) pt2.getY());
                FitsRead tmpFr[] = FitsRead.createFitsReadArray(cropFits);
                retval = tmpFr[0];
                File rotName= ModFileWriter.makeRotFileName(originalFile,imageIdx, req.getRotationAngle());
                modFileWriter = new ModFileWriter.GeomFileWriter(rotName, retval, band);
            }
        }
        return new PipelineRet(retval, modFileWriter);
    }


    private static PipelineRet applyCropAndCenter(WebPlotRequest req, FitsRead fr, Band band, int imageIdx, File originalFile)
                                                     throws  FailedRequestException,
                                                             GeomException,
                                                             FitsException,
                                                             IOException {
        FitsRead retval= fr;
        ModFileWriter modFileWriter= null;
        if (req.getPostCropAndCenter()) {
            WorldPt wpt = VisUtil.convert(getCropCenter(req), req.getPostCropAndCenterType());
            double size = getImageSize(req) / 2.;
            retval= CropAndCenter.do_crop(fr, wpt.getLon(), wpt.getLat(), size);
            File cropName= ModFileWriter.makeCropCenterFileName(originalFile,imageIdx, wpt,size);
            modFileWriter = new ModFileWriter.GeomFileWriter(cropName,retval,band);
        }
        return new PipelineRet(retval, modFileWriter);
    }

    private static PipelineRet applyFlipYAxis(WebPlotRequest req, FitsRead fr, Band band, int imageIdx, File originalFile)
                                                        throws  FailedRequestException,
                                                                GeomException,
                                                                FitsException,
                                                                IOException {
        FitsRead retval= fr;
        ModFileWriter modFileWriter= null;
        if (req.isFlipY()) {
            retval= FitsRead.createFitsReadFlipLR(fr);
            File flipName= ModFileWriter.makeFlipYFileName(originalFile,imageIdx);
            modFileWriter = new ModFileWriter.GeomFileWriter(flipName,retval,band);
        }
        return new PipelineRet(retval,modFileWriter);
    }

    private static PipelineRet applyFlipXAxis(WebPlotRequest req, FitsRead fr, Band band, int imageIdx, File originalFile)
            throws  FailedRequestException,
                    GeomException,
                    FitsException,
                    IOException {
        FitsRead retval= fr;
        ModFileWriter modFileWriter= null;
        if (req.isFlipX()) {
            retval= new FlipXY(fr,"xAxis").doFlip();
            File flipName= ModFileWriter.makeFlipXFileName(originalFile,imageIdx);
            modFileWriter = new ModFileWriter.GeomFileWriter(flipName,retval,band);
        }
        return new PipelineRet(retval,modFileWriter);
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


    static boolean isRotation(WebPlotRequest r) {
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

    private static class PipelineRet {
        final FitsRead fr;
        final ModFileWriter modFileWriter;

        PipelineRet(FitsRead fr, ModFileWriter modFileWriter) {
            this.fr = fr;
            this.modFileWriter = modFileWriter;
        }
    }

    /**
     * This method attempts to find how data might be related in a multi-extension fits file. I expect it will grow
     * and become more advanced over time.
     * Currently it looks of the extension type marked 'IMAGE' and makes that the based. Any extensions marked 'MASK' or
     * 'VARIANCE' are related data.
     * @param f the fits file name
     * @param frAry the array of FitsRead objects that came from the file.
     * @return the data relations
     */
    private static List<List<RelatedData>> investigateRelations(File f, FitsRead frAry[]) {
        if (frAry.length>1) {
            int imageIdx= -1;
            List<List<RelatedData>> retList= new ArrayList<>(frAry.length);
            for(int i=0; (i<frAry.length); i++) {
                retList.add(null);
                String extType= frAry[i].getExtType();
                if (extType!=null && extType.equalsIgnoreCase("IMAGE")) {
                    imageIdx = i;
                }
            }
            if (imageIdx>-1) {
                List <RelatedData> relatedList= new ArrayList<>();
                for(int i=0; (i<frAry.length); i++) {
                    String extType= frAry[i].getExtType();
                    if (extType!=null) {
                        if (!extType.equalsIgnoreCase("IMAGE")) {
                            if (frAry[i].getExtType().equalsIgnoreCase("MASK")) {
                                RelatedData d= RelatedData.makeMaskRelatedData(f.getAbsolutePath(),
                                        frAry[i].getImageHeader().maskHeaders, i);
                                relatedList.add(d);
                            }
                        }
                        if (extType.equalsIgnoreCase("VARIANCE")) {
                            RelatedData d= RelatedData.makeImageOverlayRelatedData(f.getAbsolutePath(),
                                    "Variance", i);
                            relatedList.add(d);
                        }
                    }
                }
                retList.set(imageIdx,relatedList);
                return retList;
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }

    }

}

