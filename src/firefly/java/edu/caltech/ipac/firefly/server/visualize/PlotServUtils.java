/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.astro.net.TargetNetwork;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.cache.UserCache;
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
import javax.swing.Icon;
import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
/**
 * User: roby
 * Date: Sep 23, 2009
 * Time: 9:46:23 AM
 */


/**
 * @author Trey Roby
 */
public class PlotServUtils {

    public static final String FUNCTION = "Function: ";
    private static final Logger.LoggerImpl _statsLog= Logger.getLogger(Logger.VIS_LOGGER);
    private static final Logger.LoggerImpl _log= Logger.getLogger();
    private static final int PLOT_FULL_WIDTH = -25;
    private static final int PLOT_FULL_HEIGHT = -25;
    private static int _nameCnt=1;

    private static final String JPG_NAME_EXT=FileUtil.jpg;
    private static final String PNG_NAME_EXT=FileUtil.png;
//    private static final String GIF_NAME_EXT=FileUtil.gif;
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


    static void writeThumbnail(ImagePlot plot, ActiveFitsReadGroup frGroup, File f, int thumbnailSize) throws IOException, FitsException {
        ImagePlot tPlot= (ImagePlot)plot.makeSharedDataPlot(frGroup);
        int div= Math.max( plot.getPlotGroup().getGroupImageWidth(), plot.getPlotGroup().getGroupImageHeight() );

        tPlot.getPlotGroup().setZoomTo(thumbnailSize/(float)div);

        int ext= f.getName().endsWith(JPG_NAME_EXT) ? PlotOutput.JPEG : PlotOutput.PNG;

        new PlotOutput(tPlot,frGroup).writeThumbnail(f,ext);
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
        int outType= (plot.getPlotGroup().getZoomFact()<.55) ? PlotOutput.JPEG : PlotOutput.PNG;
        if (fullScreen) {
            results= po.writeTilesFullScreen(imagefileDir, root,PlotOutput.PNG, tileCnt>0);
        }
        else {
            results= po.writeTiles(imagefileDir, root,outType,tileCnt);
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



    static PlotImages makeImageTilesNoCreation(File      imagefileDir,
                                               String    root,
                                               float     zfact,
                                               int       screenWidth,
                                               int       screenHeight) {

        List<PlotOutput.TileFileInfo> results;
        int outType= (zfact<.55) ? PlotOutput.JPEG : PlotOutput.PNG;
        results= PlotOutput.makeTilesNoCreation(imagefileDir, zfact, root,outType, screenWidth, screenHeight);
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
        File f= File.createTempFile(FileUtil.getBase(fname)+"-rot-north",
                                    "."+FileUtil.FITS,
                                    ServerContext.getVisSessionDir());
        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(f), (int) FileUtil.MEG);
        ImagePlot.writeFile(stream, new FitsRead[]{northFR});
        FileUtil.silentClose(stream);
        return f;
    }


    public static File createRotatedFile(File originalFile, FitsRead originalFR, double angle) throws FitsException,
                                                                                                      IOException,
                                                                                                      GeomException {
        FitsRead rotateFR= FitsRead.createFitsReadRotated(originalFR, angle);
        String fname= originalFile.getName();
        String angleStr= String.format("%2f", angle);
        File f= File.createTempFile(FileUtil.getBase(fname)+"-rot-"+angleStr,
                                    "."+FileUtil.FITS,
                                    ServerContext.getVisSessionDir());
        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(f), (int) FileUtil.MEG);
        ImagePlot.writeFile(stream, new FitsRead[]{rotateFR});
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
        ImagePlot.writeFile(stream, new FitsRead[]{rotateFR});
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
        createFullTile(plot, frGroup,f);
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


    public static boolean isValidForDownload(File f) {
        return (ServerContext.convertToFile(f.getPath())!=null);
    }

    static File createFullTile(ImagePlot plot, ActiveFitsReadGroup frGroup, File f) throws IOException {
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


    static File createOneTile(ImagePlot plot, ActiveFitsReadGroup frGroup, File f, int x, int y, int width, int height) throws IOException {
        return createOneTile(plot,frGroup,f,x,y,width,height,null,null,null,null);
    }


    static File createOneTile(ImagePlot plot,
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

        po.writeTile(f, ext, x, y, width, height, null);
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

    public static void updateProgress(ProgressStat pStat) {
        Cache cache= UserCache.getInstance();
        if (pStat.getId()!=null) cache.put(new StringKey(pStat.getId()), pStat);
    }

    public static void updateProgress(String key, ProgressStat.PType type, String progressMsg) {
        if (key!=null) {
            updateProgress(new ProgressStat(key,type,progressMsg));
        }

    }

    public static void updateProgress(WebPlotRequest r, ProgressStat.PType type, String progressMsg) {
        if (r!=null) {
            String key= r.getProgressKey();
            if (key!=null) updateProgress(new ProgressStat(key,type,progressMsg));
        }
    }

    public static Header getTopFitsHeader(File f) {
        Header header= null;
        try {
            Fits fits= new Fits(f);
            header=  fits.getHDU(0).getHeader();
            fits.getStream().close();
        } catch (FitsException e) {
            // quite fail
        } catch (IOException e) {
            // quite fail
        }
        return header;
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

    public static String getDateValueFromServiceFits(String headerKey, Header header) {
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

    static void writeImage(BufferedImage image, File f) throws IOException {
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
        File f= new File(dir,nameBase + "-" + _nameCnt +"-"+ _hostname+ _pngNameExt);
        f= FileUtil.createUniqueFileFromFile(f);
        _nameCnt++;
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
}

