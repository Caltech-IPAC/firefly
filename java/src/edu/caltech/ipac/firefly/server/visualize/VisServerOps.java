package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.firefly.data.BandInfo;
import edu.caltech.ipac.firefly.data.DataEntry;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.multipart.UploadFileInfo;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.CreatorResults;
import edu.caltech.ipac.firefly.visualize.FileAndHeaderInfo;
import edu.caltech.ipac.firefly.visualize.InsertBandInitializer;
import edu.caltech.ipac.firefly.visualize.PlotImages;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.StretchData;
import edu.caltech.ipac.firefly.visualize.WebPlotInitializer;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.firefly.visualize.draw.StaticDrawInfo;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.RegionFactory;
import edu.caltech.ipac.util.RegionParser;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.util.dd.Region;
import edu.caltech.ipac.visualize.controls.PlottingUtil;
import edu.caltech.ipac.visualize.draw.AreaStatisticsDialog;
import edu.caltech.ipac.visualize.draw.ColorDisplay;
import edu.caltech.ipac.visualize.draw.HistogramDisplay;
import edu.caltech.ipac.visualize.draw.Metric;
import edu.caltech.ipac.visualize.draw.Metrics;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.CropFile;
import edu.caltech.ipac.visualize.plot.FitsRead;
import edu.caltech.ipac.visualize.plot.GeomException;
import edu.caltech.ipac.visualize.plot.HistogramOps;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
import edu.caltech.ipac.visualize.plot.MiniFitsHeader;
import edu.caltech.ipac.visualize.plot.PixelValue;
import edu.caltech.ipac.visualize.plot.PixelValueException;
import edu.caltech.ipac.visualize.plot.PlotGroup;
import edu.caltech.ipac.visualize.plot.WorldPt;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.IndexColorModel;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static edu.caltech.ipac.firefly.visualize.Band.NO_BAND;
import static edu.caltech.ipac.visualize.DefaultMouseReadoutHandler.WhichReadout.LEFT;
import static edu.caltech.ipac.visualize.DefaultMouseReadoutHandler.WhichReadout.RIGHT;
/**
 * User: roby
 * Date: Aug 7, 2008
 * Time: 1:12:19 PM
 */


/**
 * @author Trey Roby
 */
public class VisServerOps {


    public static final long THUMBNAIL_MAX= 10*FileUtil.MEG;

    private static final Logger.LoggerImpl _log= Logger.getLogger();

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    static {
        VisContext.init();
    }





    /**
     * create a new 3 color plot
     * note - createPlot does a free resources
     * @return PlotCreationResult the results
     */
    public static WebPlotResult create3ColorPlot(WebPlotRequest redR,
                                                      WebPlotRequest greenR,
                                                      WebPlotRequest blueR) {
        WebPlotResult retval;
        try {
            WebPlotInitializer wpInit[]=  WebPlotFactory.createNew(null, redR,greenR,blueR);
            retval= makeNewPlotResult(wpInit);
            VisContext.deletePlotCtx(VisContext.getPlotCtx(null));
            Counters.getInstance().incrementVis("New 3 Color Plots");
        } catch (Exception e) {
            retval = createError("on createPlot", null, new WebPlotRequest[] {redR,greenR,blueR}, e);
        }
        return retval;
    }

    /**
     * create a new plot
     * note - createPlot does a free resources
     * @return PlotCreationResult the results
     */
    public static WebPlotResult createPlot(WebPlotRequest request) {

        WebPlotResult retval;
        try {
            WebPlotInitializer wpInit[]=  WebPlotFactory.createNew(null, request);
            retval= makeNewPlotResult(wpInit);
            VisContext.deletePlotCtx(VisContext.getPlotCtx(null));
            Counters.getInstance().incrementVis("New Plots");
        } catch (Exception e) {
            retval =createError("on createPlot", null, new WebPlotRequest[] {request}, e);
        }
        return retval;
    }

    public static WebPlotResult recreatePlot(PlotState state, boolean forceOneImage) throws FailedRequestException, GeomException {
        return recreatePlot(state,null, forceOneImage);
    }


    public static WebPlotResult recreatePlot(PlotState state,
                                             String newPlotDesc,
                                             boolean forceOneImage ) throws FailedRequestException, GeomException {
        WebPlotInitializer wpInitAry[]= WebPlotFactory.recreate(state,forceOneImage);
        if (newPlotDesc!=null) {
            for(WebPlotInitializer wpInit : wpInitAry) {
                wpInit.setPlotDesc(newPlotDesc);
            }
        }
        Counters.getInstance().incrementVis("New Plots");
        return makeNewPlotResult(wpInitAry);
    }


    public static WebPlotResult checkPlotProgress(String progressKey) {
        Cache cache= CacheManager.getCache(Cache.TYPE_HTTP_SESSION);
        String progressStr= (String)cache.get(new StringKey(progressKey));
        WebPlotResult retval;
        if (progressStr!=null) {
            retval= new WebPlotResult(null);
            retval.putResult(WebPlotResult.STRING,new DataEntry.Str(progressStr));
        }
        else {
            retval= WebPlotResult.makeFail("Not found", null,null);
        }
        return retval;
    }

    public static boolean deletePlot(String ctxStr) {
        PlotClientCtx ctx= VisContext.getPlotCtx(ctxStr);
        if (ctx!=null) {
            VisContext.deletePlotCtx(ctx);
            File delFile;
//            PlotImages images= ctx.getImages();
//            if (images!=null) {
//                for(PlotImages.ImageURL image : ctx.getImages()) {
//                    delFile=  VisContext.convertToFile(image.getURL());
//                    delFile.delete();
//                }
//            }


            List<PlotImages> allImages= ctx.getAllImagesEveryCreated();
            for(PlotImages images : allImages) {
                for(PlotImages.ImageURL image : images) {
                    delFile=  VisContext.convertToFile(image.getURL());
                    delFile.delete(); // if the file does not exist, I don't care
                }
                String thumbUrl= images.getThumbnail()!=null ? images.getThumbnail().getURL(): null;
                if (thumbUrl!=null) {
                    delFile=  VisContext.convertToFile(images.getThumbnail().getURL());
                    delFile.delete(); // if the file does not exist, I don't care
                }
            }

        }
        return true;
    }


    public static String[] getFileFlux(FileAndHeaderInfo fileAndHeader[], ImagePt ipt) {
        String retval[]= new String[fileAndHeader.length];
        try {
            int i= 0;
            for(FileAndHeaderInfo fap : fileAndHeader) {
                File f= VisContext.convertToFile(fap.getfileName());
                retval[i++]= getFluxFromFitsFile(f, fap.getHeader(), ipt)+"";
            }
        } catch (IOException e) {
            retval= new String[] {PlotState.NO_CONTEXT};
        }
        return retval;
    }


    public static double getFluxValue(PlotState state,
                                      Band band,
                                      ImagePt ipt)
                                                   throws IOException {
        if (state==null) throw new IllegalArgumentException("state must not be null");
        double retval;
        if (!isPlotValid(state)) {
            FileAndHeaderInfo fap= state.getFileAndHeaderInfo(band);
            File f= VisContext.convertToFile(fap.getfileName());
            retval= getFluxFromFitsFile(f,fap.getHeader(), ipt);
        }
        else {
            boolean revalidated= false;
            PlotClientCtx ctx= VisContext.getPlotCtx(state.getContextString());
            if (ctx!=null) revalidated= PlotServUtils.revalidatePlot(ctx);
            if (revalidated) {
                ImagePlot plot= ctx.getPlot();
                try {
                    retval= plot.getFlux(PlotServUtils.cnvtBand(band), plot.getImageWorkSpaceCoords(ipt));
                } catch (PixelValueException e) {
                    retval= Double.NaN;
                }
            }
            else {
                throw new IOException("no context");
            }
        }
        return retval;
    }

    public static WebPlotResult getFlux(PlotState state, ImagePt ipt) {
        WebPlotResult retval;
        try {
            Band bands[]= state.getBands();
            double fluxes[]= new double[bands.length];
            for(int i= 0; (i<bands.length); i++) {
                fluxes[i]= getFluxValue(state, bands[i], ipt);
            }
            retval= new WebPlotResult(state.getContextString());


            retval.putResult(WebPlotResult.FLUX_VALUE,
                    new DataEntry.DoubleArray(fluxes));
        } catch (IOException e) {
            retval= createError("on getFlux", state, e);
        }
        return retval;
    }



    public static WebPlotResult addColorBand(PlotState state,
                                      WebPlotRequest bandRequest,
                                      Band band) {
        WebPlotResult retval;
        try {
            PlotClientCtx ctx= prepare(state);
            InsertBandInitializer init;
            init= WebPlotFactory.addBand(ctx.getPlot(),state,bandRequest,band);
            retval= new WebPlotResult(ctx.getKey());
            retval.putResult(WebPlotResult.INSERT_BAND_INIT,init);
            Counters.getInstance().incrementVis("3 Color Band");

        } catch (Exception e) {
            retval= createError("on addColorBand", state, e);
        }
        return retval;

    }


    public static WebPlotResult deleteColorBand(PlotState state, Band band) {
        WebPlotResult retval;
        try {
            PlotClientCtx ctx= prepare(state);
            ctx.getPlot().removeThreeColorBand(PlotServUtils.cnvtBand(band));
            state.clearBand(band);
            state.setWebPlotRequest(null,band);
            PlotImages images= reviseImageFile(state,ctx);
            retval= new WebPlotResult(ctx.getKey());
            retval.putResult(WebPlotResult.PLOT_IMAGES,images);
            retval.putResult(WebPlotResult.PLOT_STATE,state);
        } catch (Exception e) {
            retval= createError("on deleteColorBand", state, e);
        }
        return retval;
    }

    public static WebPlotResult changeColor(PlotState state, int colorTableId) {
        WebPlotResult retval;
        try {
            PlotClientCtx ctx= prepare(state);
            PlotServUtils.statsLog("color", "new_color_id", colorTableId, "old_color_id", state.getColorTableId());
            ctx.getPlot().getImageData().setColorTableId(colorTableId);
            state.setColorTableId(colorTableId);
            PlotImages images= reviseImageFile(state,ctx);
            retval= new WebPlotResult(ctx.getKey());
            retval.putResult(WebPlotResult.PLOT_IMAGES,images);
            retval.putResult(WebPlotResult.PLOT_STATE,state);
            Counters.getInstance().incrementVis("Color change");
            PlotServUtils.createThumbnail(ctx.getPlot(),images,false,state.getThumbnailSize());
        } catch (Exception e) {
            retval= createError("on changeColor", state, e);
        }
        return retval;
    }

    public static WebPlotResult validateCtx(PlotState state) {
        try {
            PlotClientCtx ctx= prepare(state);
            return new WebPlotResult(ctx.getKey());
        } catch (FailedRequestException e) {
            return createError("Context validation failed", state, e);
        }
    }

    public static WebPlotResult recomputeStretch(PlotState state, StretchData[] stretchData) {
        return recomputeStretch(state, stretchData,true);
    }

    public static WebPlotResult recomputeStretch(PlotState state,
                                                 StretchData[] stretchData,
                                                 boolean recreateImages) {
        WebPlotResult retval;
        try {
            PlotClientCtx ctx= prepare(state);
            PlotServUtils.statsLog("stretch");
            PlotImages images= null;
            retval= new WebPlotResult(ctx.getKey());
            ImagePlot plot= ctx.getPlot();
            if (stretchData.length==1 && stretchData[0].getBand()==NO_BAND) {
                plot.getHistogramOps(ImagePlot.NO_BAND).recomputeStretch(stretchData[0].getRangeValues());
                state.setRangeValues(stretchData[0].getRangeValues(),Band.NO_BAND);
                retval.putResult(WebPlotResult.PLOT_STATE,state);
                images= reviseImageFile(state,ctx);
                retval.putResult(WebPlotResult.PLOT_IMAGES,images);
            }
            else if (plot.isThreeColor()) {
                for(StretchData sd : stretchData) {
                    int bandIdx= PlotServUtils.cnvtBand(sd.getBand());
                    HistogramOps ops= plot.getHistogramOps(bandIdx);
                    plot.setThreeColorBandVisible(bandIdx,sd.isBandVisible());
                    state.setBandVisible(sd.getBand(),sd.isBandVisible());
                    if (sd.isBandVisible() && ops!=null) {
                        ops.recomputeStretch(sd.getRangeValues());
                        state.setRangeValues(sd.getRangeValues(),sd.getBand());
                        state.setBandVisible(sd.getBand(),sd.isBandVisible());
                    }
                }
                if (recreateImages) {
                    images= reviseImageFile(state,ctx);
                    retval.putResult(WebPlotResult.PLOT_IMAGES,images);
                }
                retval.putResult(WebPlotResult.PLOT_STATE,state);

            }
            else {
                FailedRequestException fe= new FailedRequestException(
                        "Some Context wrong, isThreeColor()==true && only band passed is NO_BAND");
                retval= createError("on recomputeStretch", state, fe);
            }
            if (images!=null) PlotServUtils.createThumbnail(plot,images,false,state.getThumbnailSize());
            Counters.getInstance().incrementVis("Stretch change");
        } catch (Exception e) {
            retval= createError("on recomputeStretch", state, e);
        }
        return retval;
    }


    public static WebPlotResult crop(PlotState state, ImagePt c1, ImagePt c2) {
        WebPlotResult cropResult;
        try {
            PlotServUtils.statsLog("crop");

            Band bands[]= state.getBands();
            WebPlotRequest cropRequest[]= new WebPlotRequest[bands.length];
            boolean multiImage= false;


            for(int i= 0; (i<bands.length); i++) {
                Fits fits= new Fits(VisContext.getWorkingFitsFile(state,bands[i]));
                Fits cropFits;
                if (state.isMultiImageFile(bands[i])) {
                    cropFits= CropFile.do_crop(fits, state.getImageIdx(bands[i])+1,
                                               (int) c1.getX(), (int) c1.getY(),
                                               (int) c2.getX(), (int) c2.getY());
                    multiImage= true;
                }
                else {
                    cropFits= CropFile.do_crop(fits, (int) c1.getX(), (int) c1.getY(),
                                               (int) c2.getX(), (int) c2.getY());
                }

                FitsRead fr[]=  FitsRead.createFitsReadArray(cropFits);
                File inputFitsFile= VisContext.getWorkingFitsFile(state, bands[i]);

                String fName= inputFitsFile.getName();
                File f= File.createTempFile(FileUtil.getBase(fName)+"-crop",
                                            "."+FileUtil.FITS,
                                            VisContext.getVisSessionDir());

                BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(f), 4096);
                ImagePlot.writeFile(stream, fr);


                String fReq= VisContext.replaceWithPrefix(f);
                cropRequest[i]= WebPlotRequest.makeFilePlotRequest(fReq,state.getZoomLevel());
                cropRequest[i].setTitle(state.isThreeColor() ?
                                        "Cropped Plot ("+bands[i].toString()+")" :
                                        "Cropped Plot");
                cropRequest[i].setThumbnailSize(state.getThumbnailSize());


            }

            PlotState cropState=  PlotServUtils.createInitializedState(cropRequest,state);
            cropState.addOperation(PlotState.Operation.CROP);
            for (int i= 0; (i<bands.length); i++) {
                cropState.setWorkingFitsFileStr(cropRequest[i].getFileName(), bands[i]);
                cropState.setOriginalImageIdx(state.getOriginalImageIdx(bands[i]), bands[i]) ;
                cropState.setImageIdx(0, bands[i]) ;
            }
//            cropResult= multiImage ? recreatePlot(cropState,"Cropped: "+ ) : recreatePlot(state);
            cropResult= recreatePlot(cropState, true);  // a crop will probably be less than screen size so force one image
            Counters.getInstance().incrementVis("Crop");


        } catch (Exception e) {
            cropResult= createError("on crop", state, e);
        }

        return cropResult;

    }


    public static WebPlotResult flipImageOnY(PlotState state) {
        WebPlotResult flipResult;
        try {
            boolean flipped= !state.isFlippedY();
            PlotServUtils.statsLog("flipY");

            PlotClientCtx ctx= prepare(state,true);
            Band bands[]= state.getBands();
            WebPlotRequest flipReq[]= new WebPlotRequest[bands.length];


            for (int i= 0; (i<bands.length); i++) {
                Band band= bands[i];
                int bIdx= PlotServUtils.cnvtBand(band);

                FitsRead currentFR= ctx.getPlot().getHistogramOps(bIdx).getFitsRead();
                File currentFile= VisContext.convertToFile(state.getWorkingFitsFileStr(band));
                File f= PlotServUtils.createFlipYFile(currentFile, currentFR);
                String fReq= VisContext.replaceWithPrefix(f);
                flipReq[i]= WebPlotRequest.makeFilePlotRequest(fReq,state.getZoomLevel());
                flipReq[i].setThumbnailSize(state.getThumbnailSize());


            }

            PlotState flippedState= PlotServUtils.createInitializedState(flipReq, state);
            if (flipped) flippedState.addOperation(PlotState.Operation.FLIP_Y);
            else         flippedState.removeOperation(PlotState.Operation.FLIP_Y);
            flippedState.setFlippedY(flipped);

            for (int i= 0; (i<bands.length); i++) {
                flippedState.setWorkingFitsFileStr(flipReq[i].getFileName(), bands[i]);
                flippedState.setOriginalImageIdx(state.getOriginalImageIdx(bands[i]), bands[i]) ;
                flippedState.setImageIdx(0, bands[i]) ;
            }
            flipResult= recreatePlot(flippedState, false);

            for (Band band : bands) { // mark this request as flipped so recreate works
                flippedState.getWebPlotRequest(band).setFlipY(flipped);
            }
            Counters.getInstance().incrementVis("Flip");


        } catch (Exception e) {
            flipResult= createError("on flipY", state, e);
        }

        return flipResult;
    }

    public static WebPlotResult rotateNorth(PlotState state, boolean north, float newZoomLevel) {
        return north ? rotate(state, PlotState.RotateType.NORTH, Double.NaN, state.getRotateNorthType(),newZoomLevel) :
                       rotate(state, PlotState.RotateType.UNROTATE, Double.NaN, null,newZoomLevel);
    }

    public static WebPlotResult rotateToAngle(PlotState state, boolean rotate, double angle) {
        return rotate ? rotate(state, PlotState.RotateType.ANGLE, angle, null,-1) :
                        rotate(state, PlotState.RotateType.UNROTATE, Double.NaN, null,-1);
    }

    public static WebPlotResult rotate(PlotState state,
                                       PlotState.RotateType rotateType,
                                       double angle,
                                       CoordinateSys rotNorthType,
                                       float inZoomLevel) {

        WebPlotResult rotateResult;
        boolean rotate= (rotateType!= PlotState.RotateType.UNROTATE);
        boolean rotateNorth= (rotateType== PlotState.RotateType.NORTH);
        boolean multiUnrotate= false;
        try {
            if (rotate) {
                String descStr= rotateType== PlotState.RotateType.NORTH ? "North" : angle+"";
                PlotServUtils.statsLog("rotate", "rotation", descStr);
            }
            else {
                PlotServUtils.statsLog("rotate", "reset");
                angle= 0.0;
                multiUnrotate= true;
            }

            PlotClientCtx ctx= prepare(state,false);

            float newZoomLevel= inZoomLevel >0 ? inZoomLevel : state.getZoomLevel();

            if (rotate || isMultiOperations(state,PlotState.Operation.ROTATE)) {

                Band bands[]= state.getBands();
                WebPlotRequest rotateReq[]= new WebPlotRequest[bands.length];


                for (int i= 0; (i<bands.length); i++) {
                    Band band= bands[i];
                    int bIdx= PlotServUtils.cnvtBand(band);

                    PlotServUtils.revalidatePlot(ctx, PlotServUtils.RevalidateSource.ORIGINAL,true);

                    FitsRead originalFR= ctx.getPlot().getHistogramOps(bIdx).getFitsRead();

                    String fStr= state.getOriginalFitsFileStr(band)!=null ?
                                 state.getOriginalFitsFileStr(band) :
                                 state.getWorkingFitsFileStr(band);

                    File originalFile= VisContext.convertToFile(fStr);
                    File f= rotateNorth ? PlotServUtils.createRotateNorthFile(originalFile,originalFR,rotNorthType) :
                                          PlotServUtils.createRotatedFile(originalFile,originalFR,angle);

                    String fReq= VisContext.replaceWithPrefix(f);

                    rotateReq[i]= WebPlotRequest.makeFilePlotRequest(fReq,newZoomLevel);
                    rotateReq[i].setThumbnailSize(state.getThumbnailSize());
                    state.setZoomLevel(newZoomLevel);


                }

                PlotState rotateState= PlotServUtils.createInitializedState(rotateReq, state);
                rotateState.addOperation(PlotState.Operation.ROTATE);
                for (int i= 0; (i<bands.length); i++) {
                    rotateState.setWorkingFitsFileStr(rotateReq[i].getFileName(), bands[i]);
                    rotateState.setOriginalImageIdx(state.getOriginalImageIdx(bands[i]), bands[i]) ;
                    rotateState.setImageIdx(0, bands[i]) ;
                    if (rotateNorth) {
                        rotateState.setRotateType(PlotState.RotateType.NORTH);
                        rotateState.setRotateNorthType(rotNorthType);
                    }
                    else {
                        if (multiUnrotate) {
                            rotateState.setRotateType(PlotState.RotateType.UNROTATE);
                            rotateState.setRotationAngle(0.0);
                            rotateState.removeOperation(PlotState.Operation.ROTATE);
                        }
                        else {
                            rotateState.setRotateType(PlotState.RotateType.ANGLE);
                            rotateState.setRotationAngle(angle);
                        }
                    }
                }
                rotateResult= recreatePlot(rotateState, inZoomLevel>0); // if inZoomLevel>0 then I am doing a wcs match and the should be fairly small

                for (int i= 0; (i<bands.length); i++) { // mark this request as rotate north so recreate works
                    rotateState.getWebPlotRequest(bands[i]).setRotateNorth(true);
                }
                Counters.getInstance().incrementVis("Rotate");

            }
            else {

                Band bands[]= state.getBands();
                WebPlotRequest unrotateReq[]= new WebPlotRequest[bands.length];
                for (int i= 0; (i<bands.length); i++) {
                    String originalFile= state.getOriginalFitsFileStr(bands[i]);
                    if (originalFile==null) {
                        throw new FitsException("Can't rotate back to original north, " +
                                                        "there is not original file- this image is probably not rotated");
                    }

                    unrotateReq[i]= WebPlotRequest.makeFilePlotRequest(originalFile,newZoomLevel);
                    unrotateReq[i].setThumbnailSize(state.getThumbnailSize());
                    state.setZoomLevel(newZoomLevel);


                }
                PlotState unrotateState= PlotServUtils.createInitializedState(unrotateReq, state);
                unrotateState.removeOperation(PlotState.Operation.ROTATE);
                for (Band band : bands) {
                    unrotateState.setWorkingFitsFileStr(state.getOriginalFitsFileStr(band), band);
                    unrotateState.setImageIdx(state.getOriginalImageIdx(band),band);
                    unrotateState.setOriginalImageIdx(state.getOriginalImageIdx(band),band);
                }
                rotateResult= recreatePlot(unrotateState,false);
            }

        } catch (Exception e) {
            rotateResult= createError("on rotate north", state, e);
        }
        return rotateResult;
    }

   public static WebPlotResult getFitsHeaderInfoFull(PlotState state){
        WebPlotResult retValue = new WebPlotResult();

        HashMap<Band, RawDataSet> rawDataMap = new HashMap<Band, RawDataSet>();
        HashMap<Band, String> stringMap = new HashMap<Band, String>();

        try {

            PlotClientCtx ctx= prepare(state);
            ImagePlot plot= ctx.getPlot();
            for(Band band : state.getBands()) {
                FitsRead fr= plot.getHistogramOps(PlotServUtils.cnvtBand(band)).getFitsRead();
                DataGroup dg = PlottingUtil.showFitsHeaders(fr.getHeader(), plot.getPlotDesc());
                RawDataSet rds = QueryUtil.getRawDataSet(dg);

                String string = String.valueOf(plot.getPixelScale());

                File f= VisContext.getOriginalFile(state,band);
                if (f==null) f= VisContext.getWorkingFitsFile(state,band);

                string += ";" + StringUtils.getSizeAsString(f.length(), true);

                rawDataMap.put(band, rds);
                stringMap.put(band, string);
            }
            BandInfo bandInfo = new BandInfo(rawDataMap, stringMap, null);

            retValue.putResult(WebPlotResult.BAND_INFO, bandInfo);
            Counters.getInstance().incrementVis("Fits header");

        } catch  (Exception e) {
            retValue =  createError("on getFitsInfo", state, e);
        }

        return retValue;
    }



    private static class UseFullException extends Exception {}


    public static WebPlotResult getFitsHeaderInfo(PlotState state){
        WebPlotResult retValue = new WebPlotResult();

        HashMap<Band, RawDataSet> rawDataMap = new HashMap<Band, RawDataSet>();

        try {
            for(Band band : state.getBands()) {
                File f= VisContext.getWorkingFitsFile(state,band);
                if (f==null) f= VisContext.getOriginalFile(state,band);
                Fits fits= new Fits(f);
                BasicHDU hdu[]= fits.read();
                Header header= hdu[0].getHeader();
                if (header.containsKey("EXTEND") && header.getBooleanValue("EXTEND")) {
                    throw new UseFullException();
                }
                else {
                    DataGroup dg = PlottingUtil.showFitsHeaders(header, "fits data");
                    RawDataSet rds = QueryUtil.getRawDataSet(dg);
                    rawDataMap.put(band, rds);
                }
            }
            BandInfo bandInfo = new BandInfo(rawDataMap, null, null);

            retValue.putResult(WebPlotResult.BAND_INFO, bandInfo);
            Counters.getInstance().incrementVis("Fits header");

        } catch  (UseFullException e) {
            retValue= getFitsHeaderInfoFull(state);
        } catch  (Exception e) {
            retValue =  createError("on getFitsInfo", state, e);
        }

        return retValue;
    }





    public static WebPlotResult getAreaStatistics(PlotState state, ImagePt pt1, ImagePt pt2, ImagePt pt3, ImagePt pt4){
        WebPlotResult retValue = new WebPlotResult();

        HashMap<Band, HashMap<Metrics, Metric>> metricsMap = new HashMap<Band, HashMap<Metrics, Metric>>();
        HashMap<Band, String> stringMap = new HashMap<Band, String>();

        try{
            PlotClientCtx ctx= prepare(state);
            ImagePlot plot= ctx.getPlot();

            for(Band band : state.getBands()){
                // modeled after AreaStatisticsDialog.java lines:654 - 721
                Shape shape;
                GeneralPath genPath = new GeneralPath();
                genPath.moveTo((float)pt1.getX(), (float)pt1.getY());

                genPath.lineTo((float)pt4.getX(), (float)pt4.getY());
                genPath.lineTo((float)pt3.getX(), (float)pt3.getY());
                genPath.lineTo((float)pt2.getX(), (float)pt2.getY());
                genPath.lineTo((float)pt1.getX(), (float)pt1.getY());

                shape = genPath;

                Rectangle2D boundingBox = shape.getBounds2D();

                double minX = boundingBox.getMinX();
                double maxX = boundingBox.getMaxX();
                double minY = boundingBox.getMinY();
                double maxY = boundingBox.getMaxY();

                // smallest x and y within the plot is 0
                // biggest x within the plot is plot.getImageDataWidth()-1
                // biggest y within the plot is plot.getImageDataHeight()-1
                // use getImageLocation method, it takes into account offsetX, offsetY
                PlotGroup.ImageLocation imageLocation = plot.getPlotGroup().getImageLocation(plot);
                if (minX >= imageLocation.getMaxX()-1 || maxX <= imageLocation.getMinX() ||
                        minY >= imageLocation.getMaxY()-1 || maxY <= imageLocation.getMinY()) {
                    throw new Exception ("The area and the image do not overlap");
                }
                minX = Math.max(imageLocation.getMinX(), minX);
                maxX = Math.min(imageLocation.getMaxX()-1, maxX);
                minY = Math.max(imageLocation.getMinY(), minY);
                maxY = Math.min(imageLocation.getMaxY()-1, maxY);

                Rectangle2D.Double newBoundingBox = new Rectangle2D.Double(minX, minY, (maxX-minX), (maxY-minY));
                //what to do about selected band?
                HashMap<Metrics, Metric> metrics = AreaStatisticsDialog.getStatisticMetrics(plot, PlotServUtils.cnvtBand(band),
                                                                                            shape, newBoundingBox);

                String html;
                WorldPt wp;

                Metric max = metrics.get(Metrics.MAX);
                ImageWorkSpacePt maxIp = max.getImageWorkSpacePt();
                html = AreaStatisticsDialog.formatPosHtml(LEFT, plot, maxIp);
                html = html + ";" +  AreaStatisticsDialog.formatPosHtml(RIGHT, plot, maxIp);

                Metric min = metrics.get(Metrics.MIN);
                ImageWorkSpacePt minIp = min.getImageWorkSpacePt();
                html = html + ";" + AreaStatisticsDialog.formatPosHtml(LEFT, plot, minIp);
                html = html + ";" + AreaStatisticsDialog.formatPosHtml(RIGHT, plot, minIp);

                Metric centroid = metrics.get(Metrics.CENTROID);
                ImageWorkSpacePt centroidIp = centroid.getImageWorkSpacePt();
                html = html + ";" + AreaStatisticsDialog.formatPosHtml(LEFT, plot, centroidIp);
                html = html + ";" + AreaStatisticsDialog.formatPosHtml(RIGHT, plot, centroidIp);

                Metric fwCentroid = metrics.get(Metrics.FW_CENTROID);
                ImageWorkSpacePt fwCentroidIp = fwCentroid.getImageWorkSpacePt();
                html = html + ";" + AreaStatisticsDialog.formatPosHtml(LEFT, plot, fwCentroidIp);
                html = html + ";" + AreaStatisticsDialog.formatPosHtml(RIGHT, plot, fwCentroidIp);

                //Add Lon and Lat strings for WorldPt conversion on the client
                wp = plot.getWorldCoords(maxIp);
                html = html + ";" +  String.valueOf(wp.getLon());
                html = html + ";" +  String.valueOf(wp.getLat());
                wp = plot.getWorldCoords(minIp);
                html = html + ";" +  String.valueOf(wp.getLon());
                html = html + ";" +  String.valueOf(wp.getLat());
                wp = plot.getWorldCoords(centroidIp);
                html = html + ";" +  String.valueOf(wp.getLon());
                html = html + ";" +  String.valueOf(wp.getLat());
                wp = plot.getWorldCoords(fwCentroidIp);
                html = html + ";" +  String.valueOf(wp.getLon());
                html = html + ";" +  String.valueOf(wp.getLat());

                metricsMap.put(band, metrics);
                stringMap.put(band, html);

                //DataEntry.HM da = new DataEntry.HM(metrics);
                //DataEntry.Str str = new DataEntry.Str(html);
            }

            BandInfo bandInfo = new BandInfo(null, stringMap, metricsMap);

            retValue.putResult(WebPlotResult.BAND_INFO, bandInfo);


//            retValue.putResult(WebPlotResult.STRING, str);
//            retValue.putResult(WebPlotResult.METRICS_HASH_MAP, da);
            Counters.getInstance().incrementVis("Area Stat");

        } catch (Exception e) {
            retValue =  createError("on getStats", state, e);
        }
         return retValue;
    }

    public static boolean ENABLE_FAST_ZOOM= true;

    public static WebPlotResult setZoomLevel(PlotState state, float level, boolean temporary, boolean fullScreen) {
        WebPlotResult retval;

        try {
            String ctxStr= state.getContextString();
            PlotClientCtx ctx= VisContext.getPlotCtx(ctxStr);
            if ( ctx==null  || temporary ||
                 fullScreen || !ENABLE_FAST_ZOOM ) {
                retval= setZoomLevelFull(state, level, temporary, fullScreen);
            }
            else {
                PlotImages images= ctx.getImages();
                float oldLevel= state.getZoomLevel();
                float scale= level/oldLevel;
                int w= Math.round(images.getScreenWidth() * scale);
                int h= Math.round(images.getScreenHeight() * scale);
                retval = setZoomLevelFast(state,ctx,level, w ,h );
            }
            Counters.getInstance().incrementVis("Zoom");
        } catch (Exception e) {
            retval= createError("on setZoomLevel", state, e);
        }
        return retval;
    }

    private static WebPlotResult setZoomLevelFast(PlotState state,
                                                  PlotClientCtx ctx,
                                                  float level,
                                                  int targetWidth,
                                                  int targetHeight) {
        WebPlotResult retval;
        String details= "Fast: From " + PlotServUtils.convertZoomToString(state.getZoomLevel()) +
                " to " + PlotServUtils.convertZoomToString(level);
        PlotServUtils.statsLog("zoom", details);
        PlotImages.ThumbURL thumb= ctx.getImages().getThumbnail();
        state.setZoomLevel(level);
        PlotImages images= reviseImageFileNoCreation(state,ctx, level,targetWidth,targetHeight);
        images.setThumbnail(thumb);
        retval= new WebPlotResult(ctx.getKey());
        retval.putResult(WebPlotResult.PLOT_IMAGES,images);
        retval.putResult(WebPlotResult.PLOT_STATE,state);
        ctx.addZoomLevel(level);
        ctx.setPlotState(state);
        if (ctx.getPlot()!=null) ctx.getPlot().setZoomTo(level);
        return retval;
    }

    private static WebPlotResult setZoomLevelFull(PlotState state,
                                                  float level,
                                                  boolean temporary,
                                                  boolean fullScreen) throws FailedRequestException,
                                                                             IOException,
                                                                             FitsException {
        WebPlotResult retval;
        PlotClientCtx ctx= prepare(state);
        ImagePlot plot= ctx.getPlot();
        String details= "From " + PlotServUtils.convertZoomToString(state.getZoomLevel()) +
                " to " + PlotServUtils.convertZoomToString(level);
        PlotServUtils.statsLog("zoom", details);
        PlotImages.ThumbURL thumb= ctx.getImages().getThumbnail();
        float oldLevel= plot.getPlotGroup().getZoomFact();
        plot.getPlotGroup().setZoomTo(level);
        state.setZoomLevel(level);
        PlotImages images= reviseImageFile(state,ctx, temporary, fullScreen);
        images.setThumbnail(thumb);
        retval= new WebPlotResult(ctx.getKey());
        retval.putResult(WebPlotResult.PLOT_IMAGES,images);
        retval.putResult(WebPlotResult.PLOT_STATE,state);
        if (temporary) {
            plot.getPlotGroup().setZoomTo(oldLevel);
        }
        PlotServUtils.createThumbnail(plot,images,false,state.getThumbnailSize());
        ctx.addZoomLevel(level);
        return retval;
    }


    public static WebPlotResult getColorHistogram(PlotState state,
                                                  Band band,
                                                  int width,
                                                  int height) {
        WebPlotResult retval;
        int dHist[];
        byte dHistColors[];
        Color bgColor= new Color(181,181,181);
        HistogramOps hOps;

        try {
            PlotClientCtx ctx= prepare(state);
            ImagePlot plot= ctx.getPlot();
            if (band==NO_BAND)  {
                hOps= plot.getHistogramOps();
                int id= plot.getImageData().getColorTableId();
                if (id==0 || id==1) bgColor= new Color(0xCC, 0xCC, 0x99);
            }
            else  {
                hOps= plot.getHistogramOps(band.getIdx());
            }

            dHist= hOps.getDataHistogram();
            dHistColors= hOps.getDataHistogramColors();

            double meanDataAry[]= new double[dHist.length];
            for(int i= 0; i<meanDataAry.length; i++) {
                meanDataAry[i]= hOps.getMeanValueFromBin(i);
            }


            boolean three= plot.isThreeColor();
            IndexColorModel newColorModel= plot.getImageData().getColorModel();


            HistogramDisplay dataHD= new HistogramDisplay();
            dataHD.setScaleOn2ndValue(true);
            dataHD.setSize(width,height);
            dataHD.setHistogramArray(dHist,dHistColors, newColorModel);
            if (three) {
                dataHD.setColorBand(PlotServUtils.cnvtBand(band));
            }
            dataHD.setBottomSize(4);

            ColorDisplay colorBarC= new ColorDisplay();
            colorBarC.setSize(width,10);
            if (plot.isThreeColor()) {
                Color color;
                switch (band) {
                    case RED : color= Color.red; break;
                    case GREEN : color= Color.green; break;
                    case BLUE: color= Color.blue; break;
                    default : color= null; break;
                }
                colorBarC.setColor(color);
            }
            else {
                colorBarC.setColor(newColorModel);
            }



            String templateName= ctx.getImages().getTemplateName();
            String dataFname= templateName+ "-dataHist-" + System.currentTimeMillis()+ ".png";
            String cbarFname= templateName+ "-colorbar-" + System.currentTimeMillis()+ ".png";


            File dir= VisContext.getVisSessionDir();


            File dataFile= PlotServUtils.createHistImage(dataHD, dataHD,dir,bgColor, dataFname);
            File cbarFile= PlotServUtils.createHistImage(colorBarC, colorBarC,dir,bgColor, cbarFname);

            retval= new WebPlotResult(ctx.getKey());
            retval.putResult(WebPlotResult.DATA_HISTOGRAM, new DataEntry.IntArray(dHist));
            retval.putResult(WebPlotResult.DATA_BIN_MEAN_ARRAY,
                             new DataEntry.DoubleArray(meanDataAry));
            retval.putResult(WebPlotResult.DATA_HIST_IMAGE_URL,
                             new DataEntry.Str(VisContext.replaceWithPrefix(dataFile)));
            retval.putResult(WebPlotResult.CBAR_IMAGE_URL,
                             new DataEntry.Str(VisContext.replaceWithPrefix(cbarFile)));
            Counters.getInstance().incrementVis("Color change");

        } catch (Exception e) {
            retval= createError("on getColorHistogram", state, e);
        } catch (Throwable e) {
            retval= null;
            e.printStackTrace();
        }
        return retval;
    }


    public static WebPlotResult getImagePng(PlotState state, List<StaticDrawInfo> drawInfoList) {
        WebPlotResult retval;
        try {
            PlotClientCtx ctx= prepare(state);
            PlotPngCreator creator= new PlotPngCreator(ctx,drawInfoList);
            String pngFile= creator.createImagePng();
            retval = new WebPlotResult(ctx.getKey());
            retval.putResult(WebPlotResult.IMAGE_FILE_NAME,  new DataEntry.Str(pngFile));
        } catch (Exception e) {
            retval= createError("on getImagePng", state, e);
        }
        return retval;
    }

    public static WebPlotResult saveDS9RegionFile(String regionData) {
        WebPlotResult retval;
        try {
            File f= File.createTempFile("regionDownload-",".reg", VisContext.getVisSessionDir());
            List<String> regOutList= StringUtils.parseStringList(regionData);
            RegionParser rp= new RegionParser();
            rp.saveFile(f, regOutList,"Region file generated by IRSA" );
            String retFile= VisContext.replaceWithPrefix(f);
            retval = new WebPlotResult();
            retval.putResult(WebPlotResult.REGION_FILE_NAME,  new DataEntry.Str(retFile));
            Counters.getInstance().incrementVis("Region save");
        } catch (Exception e) {
            retval= createError("on getImagePng", null, e);
        }
        return retval;
    }

    public static WebPlotResult getDS9Region(String fileKey) {

        WebPlotResult retval;

        try {
            File regFile = VisContext.convertToFile(fileKey);
            RegionParser parser= new RegionParser();
            RegionFactory.ParseRet r= parser.processFile(regFile);
            retval = new WebPlotResult();
            List<String> rAsStrList= toStringList(r.getRegionList());



            retval.putResult(WebPlotResult.REGION_DATA,
                             new DataEntry.Str(StringUtils.combineStringList(rAsStrList)));
            retval.putResult(WebPlotResult.REGION_ERRORS,
                             new DataEntry.Str(StringUtils.combineStringList(r.getMsgList())));

            Cache cache= CacheManager.getCache(Cache.TYPE_HTTP_SESSION);
            UploadFileInfo fi= (UploadFileInfo)cache.get(new StringKey(fileKey));
            String title= (fi!=null) ? fi.getFileName() : "Region file";
            retval.putResult(WebPlotResult.TITLE, new DataEntry.Str(title));
            PlotServUtils.statsLog("ds9Region", fileKey);
            Counters.getInstance().incrementVis("Region read");
        } catch (Exception e) {
            retval= createError("on getDSRegion", null, e);
        }
        return retval;

    }



    public static synchronized boolean addSavedRequest(String saveKey, WebPlotRequest request) {
        Cache cache= CacheManager.getCache(Cache.TYPE_HTTP_SESSION);
        CacheKey key= new StringKey(saveKey);
        ArrayList<WebPlotRequest> reqList;

        if (cache.isCached(key)) {
            reqList= (ArrayList)cache.get(key);
            reqList.add(request);
            cache.put(key,reqList);
        }
        else {
            reqList= new ArrayList<WebPlotRequest>(10);
            reqList.add(request);
            cache.put(key,reqList);
        }
        return true;
    }

    public static WebPlotResult getAllSavedRequest(String saveKey) {
        Cache cache= CacheManager.getCache(Cache.TYPE_HTTP_SESSION);
        CacheKey key= new StringKey(saveKey);

        WebPlotResult result;
        if (cache.isCached(key)) {
            ArrayList<WebPlotRequest> reqList= (ArrayList)cache.get(key);
            String[] sAry= new String[reqList.size()];
            for(int i= 0; (i<sAry.length); i++) {
                sAry[i]= reqList.get(i).toString();
            }
            result= new WebPlotResult();
            result.putResult(WebPlotResult.REQUEST_LIST, new DataEntry.StringArray(sAry));
        }
        else {
           result= WebPlotResult.makeFail("not request found", null,null);
        }
        return result;
    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

//    private static String lineSepToString(List<String> sList)  {
//        String retval= "";
//        if (sList.size()>0)  {
//            StringBuilder sb= new StringBuilder(sList.size()*50);
//            for(String s : sList) {
//                sb.append(s).append("|");
//            }
//            sb.delete(sb.length()-1,sb.length());
//            retval= sb.toString();
//        }
//        return retval;
//    }

    private static List<String> toStringList(List<Region> rList)  {
        List<String> retval= new ArrayList<String>(rList.size());
        for(Region r : rList)  retval.add(r.serialize());
        return retval;
    }

    private static boolean isPlotValid(PlotState state)  {
        boolean retval= false;
        PlotClientCtx ctx= VisContext.getPlotCtx(state.getContextString());
        if (ctx!=null) {
            retval= isPlotValid(ctx);
        }
        return retval;
    }

    private static boolean isPlotValid(PlotClientCtx ctx)  {
        return (ctx.getPlot()!=null);
    }


    private static PlotImages reviseImageFile(PlotState state, PlotClientCtx ctx) throws IOException {
        return reviseImageFile(state,ctx,false, false);
    }


    private static PlotImages reviseImageFile(PlotState state,PlotClientCtx ctx, boolean temporary, boolean fullScreen) throws IOException {

//        String base= PlotServUtils.makeRevisedBase(ctx);
        String base= PlotServUtils.makeTileBase(state);

        File dir= VisContext.getVisSessionDir();
        int tileCnt= fullScreen ? 1 : 2;
        PlotImages images= PlotServUtils.writeImageTiles(dir, base, ctx.getPlot(), fullScreen, tileCnt);
        if (!temporary) ctx.setImages(images);
        return images;
    }


    private static PlotImages reviseImageFileNoCreation(PlotState state,
                                                        PlotClientCtx ctx,
                                                        float zfact,
                                                        int screenWidth,
                                                        int screenHeight) {
//        String base= PlotServUtils.makeRevisedBase(ctx);
        String base= PlotServUtils.makeTileBase(state);

        File dir= VisContext.getVisSessionDir();
        PlotImages images= PlotServUtils.makeImageTilesNoCreation(dir, base, zfact, screenWidth, screenHeight);
        ctx.setImages(images);
        return images;
    }


    private static double getFluxFromFitsFile(File f,
                                              MiniFitsHeader miniFitsHeader,
                                              ImagePt ipt) throws IOException {
        RandomAccessFile fitsFile= null;
        double val= 0.0;

        try {
            if (f.canRead()) {
                fitsFile= new RandomAccessFile(f, "r");
                if (miniFitsHeader==null) {
                    throw new IOException("Can't read file, MiniFitsHeader is null");
                }
                val= PixelValue.pixelVal(fitsFile,(int)ipt.getX(),(int)ipt.getY(), miniFitsHeader);
            }
            else {
                throw new IOException("Can't read file or it does not exist");

            }
        } catch (PixelValueException e) {
            val= Double.NaN;
        } finally {
            FileUtil.silentClose(fitsFile);
        }
        return val;
    }

    static PlotClientCtx prepare(PlotState state) throws FailedRequestException {
       return prepare(state,true);
    }

    static PlotClientCtx prepare(PlotState state, boolean withRevalidation) throws FailedRequestException {
        PlotClientCtx ctx;
        try {
            String ctxStr= state.getContextString();
            ctx= VisContext.getPlotCtx(ctxStr);
            if (ctx==null) {
                String oldCtxStr= ctxStr;
                ctxStr= PlotServUtils.makePlotCtx();
                ctx= VisContext.getPlotCtx(ctxStr);
                state.setContextString(ctxStr);
                ctx.setPlotState(state);

                _log.info("Plot context not found, creating new context.",
                          "Old context string: " + oldCtxStr,
                          "New context string: " + ctxStr);
                WebPlotFactory.recreate(state,false);
                Counters.getInstance().incrementVis("Revalidate");
//                VisContext.purgeOtherPlots(ctx);
            }
            else {
                ctx.setPlotState(state);
//                VisContext.purgeOtherPlots(ctx);
                boolean success;
                if (withRevalidation) success= PlotServUtils.revalidatePlot(ctx);
                else                  success= PlotServUtils.confirmFileData(ctx);
                if (!success) {
                    WebPlotFactory.recreate(state,false);
                    Counters.getInstance().incrementVis("Revalidate");
                }
            }

        } catch (FailedRequestException e) {
            _log.warn(e, "prepare failed - failed to re-validate plot: " +
                    "User Msg: " + e.getUserMessage(),
                      "Detail Msg: " + e.getDetailMessage());
            throw e;
        } catch (GeomException e) {
            _log.warn(e, "prepare failed - failed to re-validate plot: " + e.getMessage());
            throw new FailedRequestException("prepare failed, geom error", "this should almost never happen", e);
        }
        return ctx;
    }


    private static WebPlotResult createError(String logMsg, PlotState state, Exception e) {
        return createError(logMsg,state, null, e);
    }

    private static WebPlotResult createError(String logMsg, PlotState state, WebPlotRequest reqAry[], Exception e) {
        WebPlotResult retval;
        boolean userAbort= false;
        if (e instanceof FailedRequestException ) {
            FailedRequestException fe= (FailedRequestException)e;
            retval= WebPlotResult.makeFail(fe.getUserMessage(), fe.getUserMessage(),fe.getDetailMessage());
            fe.setSimpleToString(true);
            userAbort= VisContext.PLOT_ABORTED.equals(fe.getDetailMessage());
        }
        else if (e instanceof SecurityException ) {
            retval= WebPlotResult.makeFail("No Access", "You do not have access to this data,",e.getMessage());
        }
        else {
            retval= WebPlotResult.makeFail("Server Error, Please Report", e.getMessage(),null);
        }
        List<String> messages= new ArrayList<String>(8);
        messages.add(logMsg);
        if (state!=null) {
            messages.add("Context String: " +state.getContextString());
            try {
                if (state.isThreeColor()) {
                    for(Band band : state.getBands()) {
                        messages.add("Fits Filename (" + band.toString() + "): " + VisContext.getWorkingFitsFile(state,band));
                    }

                }
                else {
                    messages.add("Fits Filename: " + VisContext.getWorkingFitsFile(state, NO_BAND));

                }
            }
            catch (Exception ignore) {
                // if anything goes wrong here we have to recover, this is only for logging
            }
            PlotClientCtx ctx= VisContext.getPlotCtx(state.getContextString());
            if (ctx!=null) ctx.freeResources(true);
        }
        if (reqAry!=null) {
            for(WebPlotRequest req : reqAry)  messages.add("Request: " + req.prettyString());
        }

//        messages.add(e.toString());
        if (userAbort) {
            _log.info(logMsg+": "+ VisContext.PLOT_ABORTED);
        }
        else {
            _log.warn(e, messages.toArray(new String[messages.size()]));
        }


        return retval;
    }

    private static boolean isMultiOperations(PlotState state, PlotState.Operation op) {
        int multiCnt= state.hasOperation(PlotState.Operation.CROP) ? 2 : 1; // crop does not count in this test
        return (state.getOperations().size()>multiCnt ||
                (state.getOperations().size()==multiCnt && !state.hasOperation(op)));
    }


//    private static void optimizePostCreateResources(WebPlotResult plotCreateResults, PlotState state) {
//        if (plotCreateResults.isSuccess()) {  // if plot creation was successful
//            CreatorResults results= (CreatorResults)plotCreateResults.getResult(WebPlotResult.PLOT_CREATE);
//            if (results.getInitializers()[0].getInitImages().size() <=2) {  // if all the tiles have already been generated
//                if (state.isFilesOriginal()) {  // todo: important check(but i forgot why), document why it is important
//                    String fName= results.getInitializers()[0].getInitImages().getThumbnail().getURL();
//                    PlotClientCtx ctx= VisContext.getPlotCtx(state.getContextString());
//                    ImagePlot plot= ctx.getPlot();
//                    if (PlotServUtils.getTotalSize(state) > THUMBNAIL_MAX) { // if this is a big file then write the thumbnail first
//                        File f= VisContext.convertToFile(fName);
//                        try {
//                            PlotServUtils.writeThumbnail(plot, f);
//                        } catch (Exception e) {
//                            _log.error(e,"Thumbnail creation failed.");
//                        }
//                    }
//                    ctx.freeResources(true);
//                }
//            }
//        }
//    }

    private static WebPlotResult makeNewPlotResult(WebPlotInitializer wpInit[]) {
        PlotState state= wpInit[0].getPlotState();
        PlotClientCtx ctx= VisContext.getPlotCtx(state.getContextString());
        WebPlotResult retval= new WebPlotResult(ctx.getKey());
        retval.putResult(WebPlotResult.PLOT_CREATE,new CreatorResults(wpInit));
        return retval;
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
