/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.astro.net.TargetNetwork;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.cache.UserCache;
import edu.caltech.ipac.firefly.server.events.FluxAction;
import edu.caltech.ipac.firefly.server.events.ServerEventManager;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotImages;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.RequestType;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.draw.FixedObjectGroup;
import edu.caltech.ipac.visualize.draw.GridLayer;
import edu.caltech.ipac.visualize.draw.ScalableObjectPosition;
import edu.caltech.ipac.visualize.draw.VectorObject;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.Circle;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.FitsRead;
import edu.caltech.ipac.visualize.plot.GeomException;
import edu.caltech.ipac.visualize.plot.ImageMask;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.PlotGroup;
import edu.caltech.ipac.visualize.plot.RangeValues;
import edu.caltech.ipac.visualize.plot.WorldPt;
import edu.caltech.ipac.visualize.plot.output.PlotOutput;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
/**
 * User: roby
 * Date: Sep 23, 2009
 * Time: 9:46:23 AM
 */


/**
 * @author Trey Roby
 */
public class PlotServUtils {

    private static final Logger.LoggerImpl _statsLog= Logger.getLogger(Logger.VIS_LOGGER);
    private static final Logger.LoggerImpl _log= Logger.getLogger();
    private static final int PLOT_FULL_WIDTH = -25;
    private static final int PLOT_FULL_HEIGHT = -25;
    private static final AtomicLong _nameCnt= new AtomicLong(0);

    private static final String JPG_NAME_EXT=FileUtil.jpg;
    private static final String PNG_NAME_EXT=FileUtil.png;
    private static final String _hostname;
    private static final String _pngNameExt="." + PNG_NAME_EXT;

    public static final String STARTING_READ_MSG = "Retrieving Data";
    public static final String READ_PERCENT_MSG = "Retrieving ";
    public static final String ENDING_READ_MSG = "Loading Data";
    public static final String CREATING_MSG =  "Creating Images";
    public static final String PROCESSING_MSG =  "Processing Images";

    static {
        _hostname= FileUtil.getHostname();
    }

    static void createThumbnail(ImagePlot plot,
                                ActiveFitsReadGroup frGroup,
                                PlotImages images,
                                boolean justName,
                                int     thumbnailSize) throws IOException, FitsException {

        PlotGroup plotGroup= plot.getPlotGroup();
        float saveZLevel= plotGroup.getZoomFact();
        int div= Math.max( plotGroup.getGroupImageWidth(),
                           plotGroup.getGroupImageHeight() );
        float tZoomLevel= thumbnailSize /(float)div;
        plotGroup.setZoomTo(tZoomLevel);

        File f= new File(ServerContext.getVisSessionDir(),images.getTemplateName()+"_thumb" +"."+JPG_NAME_EXT);
        String relFile= ServerContext.replaceWithUsersBaseDirPrefix(f);

        if (!justName) new PlotOutput(plot,frGroup).writeThumbnail(f,PlotOutput.JPEG);

        PlotImages.ThumbURL tn= new PlotImages.ThumbURL(relFile,plot.getScreenWidth(),plot.getScreenHeight());
        images.setThumbnail(tn);

        plotGroup.setZoomTo(saveZLevel);
    }


    private static void writeThumbnail(ImagePlot plot, ActiveFitsReadGroup frGroup, File f, int thumbnailSize) throws IOException, FitsException {
        ImagePlot tPlot= (ImagePlot)plot.makeSharedDataPlot(frGroup);
        int div= Math.max(plot.getPlotGroup().getGroupImageWidth(), plot.getPlotGroup().getGroupImageHeight());

        tPlot.getPlotGroup().setZoomTo(thumbnailSize/(float)div);

        int ext= f.getName().endsWith(JPG_NAME_EXT) ? PlotOutput.JPEG : PlotOutput.PNG;

        new PlotOutput(tPlot,frGroup).writeThumbnail(f, ext);
        tPlot.freeResources();
    }

    static PlotImages writeImageTiles(File      imagefileDir,
                                      String    root,
                                      ImagePlot plot,
                                      ActiveFitsReadGroup frGroup,
                                      boolean   fullScreen,
                                      int tileCnt) throws IOException {

        PlotOutput po= new PlotOutput(plot,frGroup);
        List<PlotOutput.TileFileInfo> results;
        if (fullScreen) {
            results= po.writeTilesFullScreen(imagefileDir, root,PlotOutput.PNG, plot.isUseForMask(), tileCnt>0);
        }
        else {
            results= po.writeTiles(imagefileDir, root,PlotOutput.PNG,plot.isUseForMask(),tileCnt);
        }
        PlotImages images= new PlotImages(root,results.size(), plot.getScreenWidth(), plot.getScreenHeight(), plot.getZoomFactor());
        PlotImages.ImageURL imageURL;
        String relFile;
        int idx= 0;
        for(PlotOutput.TileFileInfo info : results) {
            relFile= ServerContext.replaceWithUsersBaseDirPrefix(info.getFile());
            imageURL= new PlotImages.ImageURL(relFile,
                                              info.getX(), info.getY(),
                                              info.getWidth(), info.getHeight(),
                                              idx++,
                                              info.isCreated());
            images.add(imageURL);
        }
        return images;
    }



    static PlotImages defineTiles(File imagefileDir,
                                  String root,
                                  float zfact,
                                  int screenWidth,
                                  int screenHeight) {

        List<PlotOutput.TileFileInfo> results;
        results= PlotOutput.defineTiles(imagefileDir, zfact, root, PlotOutput.PNG, screenWidth, screenHeight);
        PlotImages images= new PlotImages(root,results.size(), screenWidth, screenHeight, zfact);
        PlotImages.ImageURL imageurl;
        String relFile;
        int idx= 0;
        for(PlotOutput.TileFileInfo info : results) {
            relFile= ServerContext.replaceWithUsersBaseDirPrefix(info.getFile());
            imageurl= new PlotImages.ImageURL(relFile,
                                              info.getX(), info.getY(),
                                              info.getWidth(), info.getHeight(),
                                              idx++, false);
            images.add(imageurl);
        }
        return images;
    }



    public static File createRotatedFile(FitsRead originalFR,
                                         String originalFileStr,
                                         String workingFileStr,
                                         PlotState.RotateType rotateType,
                                         double angle,
                                         CoordinateSys rotNorthType) throws FitsException, IOException, GeomException {

        String fStr = originalFileStr != null ? originalFileStr : workingFileStr;
        File originalFile = ServerContext.convertToFile(fStr);
        boolean rotateNorth = (rotateType == PlotState.RotateType.NORTH);
        File f = rotateNorth ? createRotateNorthFile(originalFile, originalFR, rotNorthType) :
                               createRotatedAngleFile(originalFile, originalFR, angle);
        return f;
    }

    public static File createRotateNorthFile(File originalFile,
                                             FitsRead originalFR,
                                             CoordinateSys rotateNorthType) throws FitsException,
                                                                                   IOException,
                                                                                   GeomException {
        FitsRead northFR= null;
        if (rotateNorthType.equals(CoordinateSys.GALACTIC)) {
            northFR= FitsRead.createFitsReadNorthUpGalactic(originalFR);
        }
        else if (rotateNorthType.equals(CoordinateSys.EQ_J2000)){
            northFR= FitsRead.createFitsReadNorthUp(originalFR);
        }
        else {
            WebAssert.argTst(false, "only supports galactic and j2000");

        }
        String fname= originalFile.getName();
        File f= File.createTempFile(FileUtil.getBase(fname) + "-rot-north",
                "." + FileUtil.FITS,
                ServerContext.getVisSessionDir());
        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(f), (int) FileUtil.MEG);
        if (northFR!=null) northFR.writeSimpleFitsFile(stream);
        FileUtil.silentClose(stream);
        return f;
    }


    public static File createRotatedAngleFile(File originalFile, FitsRead originalFR, double angle) throws FitsException,
                                                                                                      IOException,
                                                                                                      GeomException {
        FitsRead rotateFR= FitsRead.createFitsReadRotated(originalFR, angle, false);
        String fname= originalFile.getName();
        String angleStr= String.format("%2f", angle);
        File f= File.createTempFile(FileUtil.getBase(fname)+"-rot-"+angleStr,
                                    "."+FileUtil.FITS,
                                    ServerContext.getVisSessionDir());
        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(f), (int) FileUtil.MEG);
        if (rotateFR!=null) rotateFR.writeSimpleFitsFile(stream);
        FileUtil.silentClose(stream);
        return f;
    }

    public static File createFlipYFile(File originalFile, FitsRead originalFR) throws FitsException,
                                                                                      IOException,
                                                                                      GeomException {
        FitsRead rotateFR= FitsRead.createFitsReadFlipLR(originalFR);
        String fname= originalFile.getName();
        String base= FileUtil.getBase(fname);
        int idx= base.indexOf("-flip");
        if (idx>-1) base= base.substring(0,idx);
        File f= File.createTempFile(base+"-flip", "."+FileUtil.FITS,
                                    ServerContext.getVisSessionDir());
        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(f), (int) FileUtil.MEG);
        rotateFR.writeSimpleFitsFile(stream);
        FileUtil.silentClose(stream);
        return f;
    }


    public static long getTileModTime(String fname) {
        File f= ServerContext.convertToFile(fname);
        return f.canRead() ? f.lastModified() : -1;
    }


    public static File createImageFile(ImagePlot plot,
                                       ActiveFitsReadGroup frGroup,
                                       String fname,
                                       int x,
                                       int y,
                                       int width,
                                       int height) throws IOException {

        File f= ServerContext.convertToFile(fname);
        if (!f.canRead()) {
            f= createOneTile(plot, frGroup, f,x,y,width,height);
        }
        return f;
    }

    public static void writeFullImageFileToStream(OutputStream oStream, ImagePlot plot, ActiveFitsReadGroup frGroup) throws IOException {

        File f= getUniquePngFileName("imageDownload", ServerContext.getVisSessionDir());
        createFullTile(plot, frGroup, f);
        FileUtil.writeFileToStream(f, oStream);
    }

    public static File createImageThumbnail(String fname, ImagePlot plot, ActiveFitsReadGroup frGroup, int thumbnailSize)
                                                                    throws FitsException, IOException {
        File f= ServerContext.convertToFile(fname);
        if (!f.canRead()) writeThumbnail(plot,frGroup,f,thumbnailSize);
        return f;
    }


    public static void writeFileToStream(File f, OutputStream oStream)
            throws IOException {
        if (!isValidForDownload(f)) {
            throw new IOException("file is not in a public directory");
        }
        _log.info("Downloading fits file: "+ f.getPath());
        statsLog("download-fits", "fsize(MB)", (float)f.length()/ StringUtils.MEG , "fname", f.getPath());
        FileUtil.writeFileToStream(f,oStream);
    }


    private static boolean isValidForDownload(File f) {
        return (ServerContext.convertToFile(f.getPath())!=null);
    }

    private static File createFullTile(ImagePlot plot, ActiveFitsReadGroup frGroup, File f) throws IOException {
        return  createOneTile(plot,frGroup,f,0,0,PLOT_FULL_WIDTH,PLOT_FULL_HEIGHT);
    }

    static File createFullTile(ImagePlot plot,
                               ActiveFitsReadGroup frGroup,
                               File f,
                               List<FixedObjectGroup> fog,
                               List<VectorObject> vectorList,
                               List<ScalableObjectPosition> scaleList,
                               GridLayer gridLayer) throws IOException {
        return  createOneTile(plot,frGroup,f,0,0,PLOT_FULL_WIDTH,PLOT_FULL_HEIGHT,
                              fog,vectorList, scaleList, gridLayer);
    }


    private static File createOneTile(ImagePlot plot, ActiveFitsReadGroup frGroup, File f, int x, int y, int width, int height) throws IOException {
        return createOneTile(plot,frGroup,f,x,y,width,height,null,null,null,null);
    }


    private static File createOneTile(ImagePlot plot,
                                      ActiveFitsReadGroup frGroup,
                                      File f,
                                      int x,
                                      int y,
                                      int width,
                                      int height,
                                      List<FixedObjectGroup> fogList,
                                      List<VectorObject> vectorList,
                                      List<ScalableObjectPosition> scaleList,
                                      GridLayer gridLayer) throws IOException {

        PlotOutput po= new PlotOutput(plot,frGroup);
        if (fogList!=null) po.setFixedObjectGroupList(fogList);
        if (gridLayer!=null) po.setGridLayer(gridLayer);
        if (vectorList!=null) po.setVectorList(vectorList);
        if (scaleList!=null) po.setScaleList(scaleList);
        int ext= f.getName().endsWith(JPG_NAME_EXT) ? PlotOutput.JPEG : PlotOutput.PNG;
        if (width== PLOT_FULL_WIDTH) width= plot.getScreenWidth();
        if (height== PLOT_FULL_HEIGHT) height= plot.getScreenHeight();

        po.writeTile(f, ext, plot.isUseForMask(),x, y, width, height, null);
        return f;

    }


    static String makeTileBase(PlotState state) {
        File f= null;
        String fName= state.getOriginalFitsFileStr(state.firstBand());
        if (fName!=null)  f= ServerContext.convertToFile(fName);
        String baseStr= null;
        if (f!=null) {
            baseStr= FileUtil.getBase(f);
        }
        else if (state.isThreeColor()) {
            baseStr= "Blank-3color-nobands";
        }
        else if (isBlank(state)) {
            baseStr= "Blank";
        }
        return  baseStr +"-"+ state.getContextString() +"-"+state.serialize().hashCode();
    }

    public static void updatePlotCreateProgress(ProgressStat pStat) {
        Cache cache= UserCache.getInstance();
        CacheKey key= new StringKey(pStat.getId());
        ProgressStat lastPstat= (ProgressStat) cache.get(key);
        boolean fireAction= true;
        if (lastPstat!=null) {
            if (lastPstat.getMessage()!=null && lastPstat.getMessage().equals((pStat.getMessage()))) {
                fireAction= false;
            }

        }
        if (pStat.getId()!=null) cache.put(key, pStat);


        if (fireAction)  {
            ProgressMessage progMsg= getPlotProgressMessage(pStat);
            FluxAction a= new FluxAction("ImagePlotCntlr.PlotProgressUpdate");
            a.setValue(progMsg.message,"message");
            a.setValue(pStat.getId(),"requestKey");
            a.setValue(pStat.getType()==ProgressStat.PType.GROUP,"group");
            a.setValue(progMsg.done,"done");
            a.setValue( pStat.getPlotId(),"plotId");
            ServerEventManager.fireAction(a);
        }
    }

    public static void updatePlotCreateProgress(String key, String plotId, ProgressStat.PType type, String progressMsg) {
        if (key!=null) {
            updatePlotCreateProgress(new ProgressStat(key,plotId, type,progressMsg));
        }

    }

    public static void updatePlotCreateProgress(WebPlotRequest r, ProgressStat.PType type, String progressMsg) {
        if (r!=null) {
            String key= r.getProgressKey();
            String plotId= r.getPlotId();
            if (key!=null) updatePlotCreateProgress(new ProgressStat(key,plotId, type,progressMsg));
        }
    }

    private static Header getTopFitsHeader(File f) {
        try {
            Fits fits= new Fits(f);
            Header header=  fits.getHDU(0).getHeader();
            fits.getStream().close();
            return header;
        } catch (FitsException|IOException  e) {
            return null;
        }
    }


    private static String getServiceDateHeaderKey(WebPlotRequest.ServiceType sType) {
        String header= "none";
        switch (sType) {
            case TWOMASS:
                header= "ORDATE";
                break;
            case DSS:
                header= "DATE-OBS";
                break;
            case WISE:
                header= "MIDOBS";
                break;
            case SDSS:
                header= "DATE-OBS";
                break;
            case IRIS:
                header= "DATEIRIS";
                break;
        }
        return header;
    }

    public static String getDateValueFromServiceFits(WebPlotRequest.ServiceType sType, File f) {
        Header header=  getTopFitsHeader(f);
        if (header!=null) {
            return getDateValueFromServiceFits(getServiceDateHeaderKey(sType), header);
        }
        else {
            return "";
        }
    }

    public static String getDateValueFromServiceFits(WebPlotRequest.ServiceType sType, Header header) {
        return getDateValueFromServiceFits(getServiceDateHeaderKey(sType), header);
    }

    private static String getDateValueFromServiceFits(String headerKey, Header header) {
        long currentYear = Math.round(Math.floor(System.currentTimeMillis()/1000/3600/24/365.25) +1970);
        long year;
        String dateValue= header.getStringValue(headerKey);
        if (headerKey.equals("ORDATE")) {
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
        } else if (headerKey.equals("DATE-OBS")) {
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
        } else if (headerKey.equals("MIDOBS")) {
            dateValue = dateValue.split("T")[0];
        } else if (headerKey.equals("DATEIRIS")) {
            dateValue = "1983";
        }
        return dateValue;
    }

    public static boolean isBlank(PlotState state) {
        return isBlank(state,state.firstBand());
    }

    public static boolean isBlank(PlotState state, Band band) {
        boolean retval= false;
        if (band==null || (state.getWorkingFitsFileStr(band)==null && state.getOriginalFitsFileStr(band)==null)) {
            WebPlotRequest req= state.getWebPlotRequest(band);
            retval= (req.getRequestType()== RequestType.BLANK);
        }
        return retval;
    }


    static File createHistImage(JComponent c,
                                Icon hd,
                                File dir,
                                Color bgColor,
                                String fname) throws IOException {
        int w= c.getWidth();
        int h= c.getHeight();
        BufferedImage cHistImage     = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2= cHistImage.createGraphics();
        g2.setColor(bgColor);
        g2.fillRect(0, 0, w, h);
        hd.paintIcon(c, g2, 0, 0);
        File f= new File(dir,fname);
        writeImage(cHistImage, f);
        return f;
    }

    private static void writeImage(BufferedImage image, File f) throws IOException {
        OutputStream chistOut= new BufferedOutputStream( new FileOutputStream(f),4096);
        Iterator writers = ImageIO.getImageWritersByFormatName("png");
        ImageWriter writer = (ImageWriter)writers.next();
        ImageOutputStream ios = ImageIO.createImageOutputStream(chistOut);
        writer.setOutput(ios);
        ImageWriteParam param= writer.getDefaultWriteParam();
        param.setDestinationType(new ImageTypeSpecifier(image));
        writer.write(image);
    }



    static void statsLog(String function, Object... sAry) {
        _statsLog.stats(function, sAry);
    }

    public static File findWorkingFitsName(File f) {
        String ext= FileUtil.getExtension(f);
        File retval= f;
        if (ext!=null && ext.equalsIgnoreCase(FileUtil.GZ)) {
            retval= new File(ServerContext.getVisSessionDir(), FileUtil.getBase(f.getName()));
        }
        return retval;
    }




    static File getUniquePngFileName(String nameBase, File dir) {
        File f= new File(dir,nameBase + "-" + _nameCnt.incrementAndGet() +"-"+ _hostname+ _pngNameExt);
        f= FileUtil.createUniqueFileFromFile(f);
        return f;
    }

    static ImagePlot makeImagePlot(ActiveFitsReadGroup frGroup,
                                   float     initialZoomLevel,
                                   boolean   threeColor,
                                   Band      band,
                                   int       initColorID,
                                   RangeValues stretch) throws FitsException {
        return new ImagePlot(null, frGroup,initialZoomLevel, threeColor, band, initColorID, stretch);
    }

    /**
     * Sort the imageMask array in the ascending order based on the mask's index (the bit offset)
     * When such mask array passed to create IndexColorModel, the number of the colors can be decided using the
     * masks colors and store the color according to the order of the imageMask in the array.
     *
     * @param imageMasks the mask
     * @return the ImageMask array
     */
    private static ImageMask[] sortImageMaskArrayInIndexOrder(ImageMask[] imageMasks){

        Map<Integer, ImageMask> unsortedMap= new HashMap<>();
        for (int i=0;i<imageMasks.length; i++){
            unsortedMap.put(imageMasks[i].getIndex(), imageMasks[i]);
        }

        Map<Integer, ImageMask> treeMap = new TreeMap<>(unsortedMap);
        return treeMap.values().toArray(new ImageMask[0]);
    }

    static ImagePlot makeMaskImagePlot(ActiveFitsReadGroup frGroup,
                                       float               initialZoomLevel,
                                       WebPlotRequest      request,
                                       RangeValues         stretch) throws FitsException {

         ImageMask maskDef[]= createMaskDefinition(request);
         return new ImagePlot(null, frGroup,initialZoomLevel, sortImageMaskArrayInIndexOrder(maskDef) , stretch);
    }

    public static String convertZoomToString(float level) {
        String retval;
        int zfInt= (int)(level*10000);

        if      (zfInt>=10000) retval= ((int)level)+"x";
        else if (zfInt==312)   retval= "1/32x";
        else if (zfInt==625)   retval= "1/16x";
        else if (zfInt==1250)  retval= "1/8x";
        else if (zfInt==2500)  retval= "1/4x";
        else if (zfInt==5000)  retval= "1/2x";
        else                   retval= String.format("%.3fx", level);

        return retval;
    }

    public static Circle getRequestArea(WebPlotRequest request) {
        Circle retval = null;
        WorldPt wp= request.getWorldPt();

        if (wp==null && request.containsParam(WebPlotRequest.OBJECT_NAME)) {
            String objName= request.getObjectName();
            if (!StringUtils.isEmpty(objName)) {
                try {
                    wp= TargetNetwork.resolveToWorldPt(objName, request.getResolver());
                } catch (Exception e) {
                    wp= null;
                }
            }
        }

        float side = request.getSizeInDeg();
        if (wp != null) retval = new Circle(wp, side);

        return retval;
    }

    private static ImageMask[] createMaskDefinition(WebPlotRequest r) {
        List<String> maskColors= r.getMaskColors();
        Color cAry[]= new Color[maskColors.size()];
        List<ImageMask> masksList=  new ArrayList<ImageMask>();
        int bits= r.getMaskBits();
        int colorIdx= 0;
        for(String htmlColor : maskColors) {
            cAry[colorIdx++]= convertColorHtmlToJava(htmlColor);
        }
        colorIdx= 0;

        for(int j= 0; (j<31); j++) {
            if (((bits>>j) & 1) != 0) {
                Color c= (colorIdx<cAry.length) ? cAry[colorIdx] : Color.pink;
                colorIdx++;
                masksList.add(new ImageMask(j,c));
            }
        }
        return masksList.toArray(new ImageMask[masksList.size()]);
    }

    public static FitsRead[] createBlankFITS(WebPlotRequest r) throws FailedRequestException, IOException {
        FitsRead retval[];
        Circle c=PlotServUtils.getRequestArea(r);
        if (c!=null) {
            int w= r.getBlankPlotWidth();
            int h= r.getBlankPlotHeight();
            if (w>0 && h>0) {
                float asScale= r.getBlankArcsecPerPix();
                if (asScale>0) {
                    double degScale= asScale/3600.0;
                    Fits blankFits = BlankFITS.createBlankFITS(c.getCenter(), w, h, degScale);
                    try {
                        retval = FitsRead.createFitsReadArray(blankFits);
                    } catch (FitsException e) {
                        throw new FailedRequestException("Could not create blank image", "FITS read could not be created",e);
                    } finally {
                        if (blankFits.getStream()!=null) blankFits.getStream().close();
                    }
                }
                else {
                    throw new FailedRequestException("Blank image BlankArcsecPerPix must be greater than 0.");
                }
            }
            else {
                throw new FailedRequestException("Blank image width and height must both be greater than 0.");
            }
        }
        else {
            throw new FailedRequestException("Blank image requires a center position");
        }
        return retval;
    }

    private static Color parseRGB(String color) {
        String rgb = "rgb";
        String rgba = "rgba";
        Color c = null;

        if ((color.startsWith(rgb) || color.startsWith(rgba)) && color.endsWith(")")) {
            int s = color.indexOf('(');
            int e = color.lastIndexOf(')');

            String   rgbStr = color.substring(s+1, e);
            String[] rgbVal = rgbStr.split(",");

            if ((color.startsWith(rgba)&&rgbVal.length == 4) || (rgbVal.length == 3)) {
                int i;
                int[] v = new int[3];

                for (i = 0; i < 3; i++) {
                    try {
                        v[i] = Integer.parseInt(rgbVal[i]);
                    } catch (NumberFormatException ex) {
                        break;
                    }
                }
                if (i == 3) {
                    c = new Color(v[0], v[1], v[2]);  // rgb only
                }
            }
        }
        if (c == null) {
            c = Color.lightGray;
            _log.debug("parseRGB(String color) does not understand " + color + ".  Color.lightGray is assigned.");
        }
        return c;
    }

    public static Color convertColorHtmlToJava(String color) {
        Color c;
        if (edu.caltech.ipac.firefly.visualize.ui.color.Color.isHexColor(color)) {
            int rgb[]=  edu.caltech.ipac.firefly.visualize.ui.color.Color.toRGB(color);
            c= new Color(rgb[0],rgb[1],rgb[2]);
        }
        else if (color.startsWith("rgb")) {
            c = parseRGB(color);
        } else {
            if      (color.equals("black"))   c= Color.black;
            else if (color.equals("aqua"))    c= new Color(0,255,255);
            else if (color.equals("blue"))    c= Color.blue;
            else if (color.equals("cyan"))    c= Color.cyan;
            else if (color.equals("fuchsia")) c= new Color(255,0,255);
            else if (color.equals("gray"))    c= new Color(128,128,128);
            else if (color.equals("green"))   c= new Color(0,128,0);
            else if (color.equals("lime"))    c= Color.green;  // this is correct, lime is 0,255,0
            else if (color.equals("magenta")) c= Color.magenta;
            else if (color.equals("maroon"))  c= new Color(128,0,0);
            else if (color.equals("navy"))    c= new Color(0,0,128);
            else if (color.equals("olive"))   c= new Color(128,128,0);
            else if (color.equals("orange"))  c= Color.orange;
            else if (color.equals("pink"))    c= Color.pink;
            else if (color.equals("purple"))  c= new Color(128,0,128);
            else if (color.equals("red"))     c= Color.red;
            else if (color.equals("silver"))  c= new Color(192,192,192);
            else if (color.equals("teal"))    c= new Color(0,128,128);
            else if (color.equals("white"))   c= Color.white;
            else if (color.equals("yellow"))  c= Color.yellow;
            else {
                // lightGray or white is a better presentation for "unknown" color string. -TLau
                c= Color.lightGray;
                _log.debug("convertColorHtmlToJava(String color) does not understand " + color + ".  Color.lightGray is assigned.");
            }
        }
        return c;
    }

    private static final ProgressMessage EMPTY_MESSAGE= new ProgressMessage("",false);

    static ProgressMessage getPlotProgressMessage(ProgressStat stat) {
        ProgressMessage progMessage = EMPTY_MESSAGE;
        if (stat != null) {
            if (stat.isGroup()) {
                List<String> keyList = stat.getMemberIDList();
                progMessage = (keyList.size() == 1) ?
                        getSingleStatusMessage(keyList.get(0)) :
                        getMultiStatMessage(stat);
            } else {
                progMessage = new ProgressMessage(stat.getMessage(), stat.isDone());
            }
        }
        return progMessage;
    }


    private static ProgressMessage getSingleStatusMessage(String key) {
        ProgressMessage retval = EMPTY_MESSAGE;
        Cache cache = UserCache.getInstance();
        ProgressStat stat = (ProgressStat) cache.get(new StringKey(key));
        if (stat != null) {
            retval = new ProgressMessage(stat.getMessage(), stat.isDone());
        }
        return retval;
    }

    private static ProgressMessage getMultiStatMessage(ProgressStat stat) {
        ProgressMessage retval = null;
        String downloadStr = null;
        Cache cache = UserCache.getInstance();
        List<String> keyList = stat.getMemberIDList();
        ProgressStat statEntry;

        int numSuccess = 0;
        int numDone = 0;
        int total = keyList.size();

        String downloadMsg = null;
        String readingMsg = null;
        String creatingMsg = null;
        ProgressStat.PType ptype;

        for (String key : keyList) {
            statEntry = (ProgressStat) cache.get(new StringKey(key));
            if (statEntry != null) {
                ptype = statEntry.getType();
                if (ptype == ProgressStat.PType.SUCCESS) numSuccess++;
                if (statEntry.isDone()) numDone++;

                switch (ptype) {
                    case DOWNLOADING:
                        downloadMsg = statEntry.getMessage();
                        break;
                    case READING:
                        readingMsg = statEntry.getMessage();
                        break;
                    case CREATING:
                        creatingMsg = statEntry.getMessage();
                        break;
                    case GROUP:
                    case OTHER:
                    case SUCCESS:
                    default:
                        // ignore
                        break;
                }
            }
        }
        if (downloadMsg != null) {
            retval = new ProgressMessage(downloadMsg,false);
        } else {
            retval = new ProgressMessage("Loaded " + numSuccess + " of " + total, numDone==keyList.size());
        }
        return retval;
    }

    public static class ProgressMessage {
        final String message;
        final boolean done;
        ProgressMessage(String message, boolean done) {
            this.message= message;
            this.done= done;
        }
    }
}

