/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.data.BandInfo;
import edu.caltech.ipac.firefly.data.DataEntry;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.cache.UserCache;
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
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.RegionFactory;
import edu.caltech.ipac.util.RegionParser;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.util.dd.Region;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.FileRetrieveException;
import edu.caltech.ipac.visualize.draw.AreaStatisticsUtil;
import edu.caltech.ipac.visualize.draw.ColorDisplay;
import edu.caltech.ipac.visualize.draw.HistogramDisplay;
import edu.caltech.ipac.visualize.draw.Metric;
import edu.caltech.ipac.visualize.draw.Metrics;
import edu.caltech.ipac.visualize.plot.*;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.util.Cursor;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static edu.caltech.ipac.firefly.visualize.Band.NO_BAND;
import static edu.caltech.ipac.visualize.draw.AreaStatisticsUtil.WhichReadout.LEFT;
import static edu.caltech.ipac.visualize.draw.AreaStatisticsUtil.WhichReadout.RIGHT;
/**
 * User: roby
 * Date: Aug 7, 2008
 * Time: 1:12:19 PM
 */


/**
 * @author Trey Roby
 */
public class VisServerOps {

    private static final Logger.LoggerImpl _log= Logger.getLogger();
    private static Counters counters= Counters.getInstance();

    //======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    static {
        VisContext.init();
        VisContext.initCounters();
    }

    /**
     * create a new 3 color plot
     * note - createPlot does a free resources
     * @return PlotCreationResult the results
     */
    public static WebPlotResult create3ColorPlot(WebPlotRequest redR, WebPlotRequest greenR, WebPlotRequest blueR) {
        WebPlotResult retval;
        try {
            WebPlotInitializer wpInit[]=  WebPlotFactory.createNew(null, redR,greenR,blueR);
            retval= makeNewPlotResult(wpInit);
            CtxControl.deletePlotCtx(CtxControl.getPlotCtx(null));
            counters.incrementVis("New 3 Color Plots");
        } catch (Exception e) {
            retval = createError("on createPlot", null, new WebPlotRequest[] {redR,greenR,blueR}, e);
        }
        return retval;
    }


    /**
     * create a group of new plots
     * @return PlotCreationResult the results
     */
    public static WebPlotResult[] createPlotGroup(List<WebPlotRequest> rList, String progressKey) {
        final List<WebPlotResult> resultList= new ArrayList<WebPlotResult>(rList.size());

        List<String> keyList= new ArrayList<String>(rList.size());
        for(WebPlotRequest wpr : rList) {
            if (wpr.getProgressKey()!=null) keyList.add(wpr.getProgressKey());
        }
        PlotServUtils.updateProgress(new ProgressStat(keyList, progressKey));

        ExecutorService executor = Executors.newFixedThreadPool(rList.size());
        boolean allCompleted= false;
        try {
            for (WebPlotRequest r : rList) {
                final WebPlotRequest finalR= r;
                Runnable worker = new Runnable() {
                    public void run() {
                        resultList.add(createPlot(finalR));
                    }
                };
                executor.execute(worker);
            }
            executor.shutdown();
            allCompleted= executor.awaitTermination(500, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executor.shutdownNow();
            if (!allCompleted) {
                 _log.info("ExecutorService thread pool was shut down before all plots could complete, after 500 seconds");
            }
        }

        return resultList.toArray(new WebPlotResult[resultList.size()]);
    }

    /**
     * create a new plot
     * note - createPlot does a free resources
     * @return PlotCreationResult the results
     */
    public static WebPlotResult[] createOneFileGroup(List<WebPlotRequest> rList, String progressKey) {

        List<WebPlotResult> resultList= new ArrayList<WebPlotResult>(rList.size());
        try {
            WebPlotInitializer wpInitAry[]=  WebPlotFactory.createNewGroup(progressKey, rList);
            for(WebPlotInitializer wpInit : wpInitAry) {
                resultList.add(makeNewPlotResult(new WebPlotInitializer[]{wpInit}));
                CtxControl.deletePlotCtx(CtxControl.getPlotCtx(null));
                counters.incrementVis("New Plots");
            }
        } catch (Exception e) {
            for(int i= 0; (i<resultList.size());i++) {
                resultList.add(createError("on createPlot", null, new WebPlotRequest[] {rList.get(i)}, e));
            }
        }
        return resultList.toArray(new WebPlotResult[resultList.size()]);
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
            CtxControl.deletePlotCtx(CtxControl.getPlotCtx(null));
            counters.incrementVis("New Plots");
        } catch (Exception e) {
            retval =createError("on createPlot", null, new WebPlotRequest[] {request}, e);
        }
        return retval;
    }

    public static WebPlotResult recreatePlot(PlotState state) throws FailedRequestException, GeomException {
        return recreatePlot(state,null);
    }


    public static WebPlotResult recreatePlot(PlotState state,
                                             String newPlotDesc) throws FailedRequestException, GeomException {
        WebPlotInitializer wpInitAry[]= WebPlotFactory.recreate(state);
        if (newPlotDesc!=null) {
            for(WebPlotInitializer wpInit : wpInitAry) {
                wpInit.setPlotDesc(newPlotDesc);
            }
        }
        counters.incrementVis("New Plots");
        return makeNewPlotResult(wpInitAry);
    }


    public static WebPlotResult checkPlotProgress(String progressKey) {
        Cache cache= UserCache.getInstance();
        ProgressStat stat= (ProgressStat)cache.get(new StringKey(progressKey));
        WebPlotResult retval;
        String progressStr= null;
        if (stat!=null) {
            if (stat.isGroup()) {
                List<String> keyList= stat.getMemberIDList();
                progressStr= (keyList.size()==1) ?
                             getSingleStatusMessage(keyList.get(0)) :
                             getMultiStatMessage(stat);
            }
            else {
                progressStr= stat.getMessage();
            }
        }

        if (progressStr!=null) {
            retval= new WebPlotResult(null);
            retval.putResult(WebPlotResult.STRING,new DataEntry.Str(progressStr));
        }
        else {
            retval= WebPlotResult.makeFail("Not found", null,null);
        }
        return retval;
    }

    private static String getSingleStatusMessage(String key) {
        String retval= null;
        Cache cache= UserCache.getInstance();
        ProgressStat stat= (ProgressStat)cache.get(new StringKey(key));
        if (stat!=null) {
            retval= stat.getMessage();
        }
        return retval;
    }


    private static String getMultiStatMessage(ProgressStat stat) {
        String retval= null;
        String downloadStr= null;
        Cache cache= UserCache.getInstance();
        List<String> keyList= stat.getMemberIDList();
        ProgressStat statEntry;

        int numDone= 0;
        int total= keyList.size();

        String downloadMsg= null;
        String readingMsg= null;
        String creatingMsg= null;
        ProgressStat.PType ptype;

        for(String key : keyList) {
            statEntry= (ProgressStat)cache.get(new StringKey(key));
            if (statEntry!=null){
                ptype= statEntry.getType();
                if (ptype== ProgressStat.PType.SUCCESS) numDone++;

                switch (ptype) {
                    case DOWNLOADING:
                        downloadMsg= statEntry.getMessage();
                        break;
                    case READING:
                        readingMsg= statEntry.getMessage();
                        break;
                    case CREATING:
                        creatingMsg= statEntry.getMessage();
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
        if (downloadMsg!=null) {
            retval= downloadMsg;
        }
        else {
            retval= "Loaded " + numDone +" of " + total;
        }
        return retval;
    }



    public static boolean deletePlot(String ctxStr) {
        PlotClientCtx ctx= CtxControl.getPlotCtx(ctxStr);
        if (ctx!=null)  CtxControl.deletePlotCtx(ctx);
        return true;
    }


    public static String[] getFileFlux(FileAndHeaderInfo fileAndHeader[], ImagePt ipt) {
        String retval[]= new String[fileAndHeader.length];
        try {
            int i= 0;
            for(FileAndHeaderInfo fap : fileAndHeader) {
                File f= ServerContext.convertToFile(fap.getfileName());
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
        if (!isPlotValid(state)) {  // work directly on the file
            FileAndHeaderInfo fap= state.getFileAndHeaderInfo(band);
            File f= ServerContext.convertToFile(fap.getfileName());
            retval= getFluxFromFitsFile(f,fap.getHeader(), ipt);
        }
        else {
            ActiveCallCtx ctx= null;
            try {  // use the in memory plot object
                ctx= CtxControl.prepare(state);
                ImagePlot plot= ctx.getPlot();
                retval= plot.getFlux(ctx.getFitsReadGroup(), band, plot.getImageWorkSpaceCoords(ipt));
            } catch (FailedRequestException e) {
                throw new IOException(e);
            } catch (PixelValueException e) {
                retval= Double.NaN;
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
        ActiveCallCtx ctx= null;
        try {
            ctx= CtxControl.prepare(state);
            InsertBandInitializer init;
            init= WebPlotFactory.addBand(ctx.getPlot(),state,bandRequest,band, ctx.getFitsReadGroup());
            retval= new WebPlotResult(ctx.getKey());
            retval.putResult(WebPlotResult.INSERT_BAND_INIT,init);
            counters.incrementVis("3 Color Band");

        } catch (Exception e) {
            retval= createError("on addColorBand", state, e);
        }
        return retval;

    }


    public static WebPlotResult deleteColorBand(PlotState state, Band band) {
        WebPlotResult retval;
        try {
            ActiveCallCtx ctx= CtxControl.prepare(state);
            ImagePlot plot= ctx.getPlot();
            plot.removeThreeColorBand(band, ctx.getFitsReadGroup());
            state.clearBand(band);
            state.setWebPlotRequest(null,band);
            PlotImages images= reviseImageFile(state,ctx.getPlotClientCtx(),plot, ctx.getFitsReadGroup());
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
            ActiveCallCtx ctx= CtxControl.prepare(state);
            PlotServUtils.statsLog("color", "new_color_id", colorTableId, "old_color_id", state.getColorTableId());
            ImagePlot plot=  ctx.getPlot();
            plot.getImageData().setColorTableId(colorTableId);
            state.setColorTableId(colorTableId);
            PlotImages images= reviseImageFile(state,ctx.getPlotClientCtx(),plot,ctx.getFitsReadGroup());
            retval= new WebPlotResult(ctx.getKey());
            retval.putResult(WebPlotResult.PLOT_IMAGES,images);
            retval.putResult(WebPlotResult.PLOT_STATE,state);
            counters.incrementVis("Color change");
            PlotServUtils.createThumbnail(plot,ctx.getFitsReadGroup(),images,false,state.getThumbnailSize());
        } catch (Exception e) {
            retval= createError("on changeColor", state, e);
        }
        return retval;
    }

    public static WebPlotResult validateCtx(PlotState state) {
        ActiveCallCtx ctx= null;
        try {
            ctx= CtxControl.prepare(state);
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
        ActiveCallCtx ctx= null;
        try {
            ctx= CtxControl.prepare(state);
            PlotServUtils.statsLog("stretch");
            PlotImages images= null;
            retval= new WebPlotResult(ctx.getKey());
            ImagePlot plot= ctx.getPlot();
            if (stretchData.length==1 && stretchData[0].getBand()==NO_BAND) {
                plot.getHistogramOps(Band.NO_BAND,ctx.getFitsReadGroup()).recomputeStretch(stretchData[0].getRangeValues());
                state.setRangeValues(stretchData[0].getRangeValues(),Band.NO_BAND);
                retval.putResult(WebPlotResult.PLOT_STATE,state);
                images= reviseImageFile(state,ctx.getPlotClientCtx(),plot,ctx.getFitsReadGroup());
                retval.putResult(WebPlotResult.PLOT_IMAGES,images);
            }
            else if (plot.isThreeColor()) {
                for(StretchData sd : stretchData) {
                    HistogramOps ops= plot.getHistogramOps(sd.getBand(),ctx.getFitsReadGroup());
//                    plot.setThreeColorBandVisible(bandIdx,sd.isBandVisible());
                    state.setBandVisible(sd.getBand(),sd.isBandVisible());
                    if (sd.isBandVisible() && ops!=null) {
                        ops.recomputeStretch(sd.getRangeValues());
                        state.setRangeValues(sd.getRangeValues(),sd.getBand());
                        state.setBandVisible(sd.getBand(),sd.isBandVisible());
                    }
                }
                if (recreateImages) {
                    images= reviseImageFile(state,ctx.getPlotClientCtx(),plot,ctx.getFitsReadGroup());
                    retval.putResult(WebPlotResult.PLOT_IMAGES,images);
                }
                retval.putResult(WebPlotResult.PLOT_STATE,state);

            }
            else {
                FailedRequestException fe= new FailedRequestException(
                        "Some Context wrong, isThreeColor()==true && only band passed is NO_BAND");
                retval= createError("on recomputeStretch", state, fe);
            }
            if (images!=null) PlotServUtils.createThumbnail(plot,ctx.getFitsReadGroup(),images,false,state.getThumbnailSize());
            counters.incrementVis("Stretch change");
        } catch (Exception e) {
            retval= createError("on recomputeStretch", state, e);
        }
        return retval;
    }



    public static WebPlotResult crop(PlotState state, ImagePt c1, ImagePt c2, boolean cropMultiAll) {
        WebPlotResult cropResult;
        try {

            Band bands[]= state.getBands();
            WebPlotRequest cropRequest[]= new WebPlotRequest[bands.length];

            for(int i= 0; (i<bands.length); i++) {

                File workingFilsFile= PlotStateUtil.getWorkingFitsFile(state, bands[i]);
                String fName= workingFilsFile.getName();
                File cropFile= File.createTempFile(FileUtil.getBase(fName)+"-crop",
                                                   "."+FileUtil.FITS,
                                                   ServerContext.getVisSessionDir());

                Fits cropFits;
                boolean saveCropFits= true;
                if (state.isMultiImageFile(bands[i])) {
                    if (cropMultiAll) {
                        File originalFile= PlotStateUtil.getOriginalFile(state, bands[i]);
                        CropFile.crop_extensions(originalFile.getPath(),cropFile.getPath(),
                                                 (int) c1.getX(), (int) c1.getY(),
                                                 (int) c2.getX(), (int) c2.getY());
                        cropFits= new Fits(cropFile);
                        saveCropFits= false;
                    }
                    else {
                        Fits fits= new Fits(PlotStateUtil.getWorkingFitsFile(state, bands[i]));
                        cropFits= CropFile.do_crop(fits, state.getImageIdx(bands[i]) + 1,
                                                   (int) c1.getX(), (int) c1.getY(),
                                                   (int) c2.getX(), (int) c2.getY());
                    }
                }
                else {
                    Fits fits= new Fits(PlotStateUtil.getWorkingFitsFile(state, bands[i]));
                    cropFits= CropFile.do_crop(fits, (int) c1.getX(), (int) c1.getY(),
                                               (int) c2.getX(), (int) c2.getY());
                }

                FitsRead fr[]=  FitsCacher.loadFits(cropFits, cropFile);


                if (saveCropFits) {
                    BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(cropFile), 4096);
                    FitsRead.writeFitsFile(stream, fr, cropFits);
                    FileUtil.silentClose(stream);
                }


                String fReq= ServerContext.replaceWithPrefix(cropFile);
                cropRequest[i]= WebPlotRequest.makeFilePlotRequest(fReq,state.getZoomLevel());
                cropRequest[i].setTitle(state.isThreeColor() ?
                                        "Cropped Plot ("+bands[i].toString()+")" :
                                        "Cropped Plot");
                cropRequest[i].setThumbnailSize(state.getThumbnailSize());
                PlotStateUtil.initRequestFromState(cropRequest[i], state, bands[i]);
            }



            WebPlotInitializer wpInitAry[] = (state.isThreeColor() && cropRequest.length==3) ?
                                  WebPlotFactory.createNew(null,  cropRequest[0], cropRequest[1], cropRequest[2]) :
                                  WebPlotFactory.createNew(null, cropRequest[0]);

            int imageIdx= 0;
            for(WebPlotInitializer  wpInit : wpInitAry) {
                PlotState cropState= wpInit.getPlotState();
                cropState.addOperation(PlotState.Operation.CROP);
                cropState.setWorkingFitsFileStr(cropRequest[0].getFileName(), bands[0]);
                for (int i= 0; (i<bands.length); i++) {
                    cropState.setWorkingFitsFileStr(cropRequest[i].getFileName(), bands[i]);
                    if (!cropMultiAll) {
                        cropState.setOriginalImageIdx(state.getOriginalImageIdx(bands[i]), bands[i]) ;
                    }
                    cropState.setImageIdx(imageIdx, bands[i]) ;
                }
                imageIdx++;
            }


            cropResult= makeNewPlotResult(wpInitAry);

            counters.incrementVis("Crop");
            PlotServUtils.statsLog("crop");


        } catch (Exception e) {
            cropResult= createError("on crop", state, e);
        }

        return cropResult;

    }



//    public static WebPlotResult crop_MOSTLY_ORIGINAL(PlotState state, ImagePt c1, ImagePt c2, boolean cropMultiAll) {
//        WebPlotResult cropResult;
//        try {
//            PlotServUtils.statsLog("crop");
//
//            Band bands[]= state.getBands();
//            WebPlotRequest cropRequest[]= new WebPlotRequest[bands.length];
//            boolean multiImage= false;
//
//
//            for(int i= 0; (i<bands.length); i++) {
//
//                File workingFilsFile= VisContext.getWorkingFitsFile(state, bands[i]);
//                String fName= workingFilsFile.getName();
//                File cropFile= File.createTempFile(FileUtil.getBase(fName)+"-crop",
//                                                   "."+FileUtil.FITS,
//                                                   VisContext.getVisSessionDir());
//
//
//
//                Fits cropFits;
//                boolean saveCropFits= true;
//                if (state.isMultiImageFile(bands[i])) {
//                    if (cropMultiAll) {
//                        File originalFile= VisContext.getOriginalFile(state,bands[i]);
//                        CropFile.crop_extensions(originalFile.getPath(),cropFile.getPath(),
//                                                 (int) c1.getX(), (int) c1.getY(),
//                                                 (int) c2.getX(), (int) c2.getY());
//                        cropFits= new Fits(cropFile);
//                        saveCropFits= false;
//                        multiImage= true;
//                    }
//                    else {
//                        Fits fits= new Fits(VisContext.getWorkingFitsFile(state,bands[i]));
//                        cropFits= CropFile.do_crop(fits, state.getImageIdx(bands[i]) + 1,
//                                                   (int) c1.getX(), (int) c1.getY(),
//                                                   (int) c2.getX(), (int) c2.getY());
//                        multiImage= true;
//                    }
//                }
//                else {
//                    Fits fits= new Fits(VisContext.getWorkingFitsFile(state,bands[i]));
//                    cropFits= CropFile.do_crop(fits, (int) c1.getX(), (int) c1.getY(),
//                                               (int) c2.getX(), (int) c2.getY());
//                }
//
//                FitsRead fr[]=  FitsRead.createFitsReadArray(cropFits);
//
//
//                if (saveCropFits) {
//                    BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(cropFile), 4096);
//                    ImagePlot.writeFile(stream, fr);
//                    FileUtil.silentClose(stream);
//                }
//
//
//                String fReq= VisContext.replaceWithPrefix(cropFile);
//                cropRequest[i]= WebPlotRequest.makeFilePlotRequest(fReq,state.getZoomLevel());
//                cropRequest[i].setTitle(state.isThreeColor() ?
//                                        "Cropped Plot ("+bands[i].toString()+")" :
//                                        "Cropped Plot");
//                cropRequest[i].setThumbnailSize(state.getThumbnailSize());
//
//
//            }
//
//            PlotState cropState=  PlotServUtils.createInitializedState(cropRequest,state);
//            cropState.addOperation(PlotState.Operation.CROP);
//            for (int i= 0; (i<bands.length); i++) {
//                cropState.setWorkingFitsFileStr(cropRequest[i].getFileName(), bands[i]);
//                cropState.setOriginalImageIdx(state.getOriginalImageIdx(bands[i]), bands[i]) ;
//                cropState.setImageIdx(0, bands[i]) ;
//            }
////            cropResult= multiImage ? recreatePlot(cropState,"Cropped: "+ ) : recreatePlot(state);
//            cropResult= recreatePlot(cropState, true);  // a crop will probably be less than screen size so force one image
//            Counters.getInstance().incrementVis("Crop");
//
//
//        } catch (Exception e) {
//            cropResult= createError("on crop", state, e);
//        }
//
//        return cropResult;
//
//    }


    public static WebPlotResult flipImageOnY(PlotState state) {
        WebPlotResult flipResult;
        ActiveCallCtx ctx= null;
        try {
            boolean flipped= !state.isFlippedY();
            PlotServUtils.statsLog("flipY");

            ctx= CtxControl.prepare(state);
            Band bands[]= state.getBands();
            WebPlotRequest flipReq[]= new WebPlotRequest[bands.length];


            for (int i= 0; (i<bands.length); i++) {
                Band band= bands[i];

                FitsRead currentFR= ctx.getPlot().getHistogramOps(band,ctx.getFitsReadGroup()).getFitsRead();
                File currentFile= ServerContext.convertToFile(state.getWorkingFitsFileStr(band));
                File f= PlotServUtils.createFlipYFile(currentFile, currentFR);
                String fReq= ServerContext.replaceWithPrefix(f);
                flipReq[i]= WebPlotRequest.makeFilePlotRequest(fReq,state.getZoomLevel());
                flipReq[i].setThumbnailSize(state.getThumbnailSize());


            }

            PlotState flippedState= PlotStateUtil.create(flipReq, state);
            if (flipped) flippedState.addOperation(PlotState.Operation.FLIP_Y);
            else         flippedState.removeOperation(PlotState.Operation.FLIP_Y);
            flippedState.setFlippedY(flipped);

            for (int i= 0; (i<bands.length); i++) {
                flippedState.setWorkingFitsFileStr(flipReq[i].getFileName(), bands[i]);
                flippedState.setOriginalImageIdx(state.getOriginalImageIdx(bands[i]), bands[i]) ;
                flippedState.setImageIdx(0, bands[i]) ;
            }
            flipResult= recreatePlot(flippedState);

            for (Band band : bands) { // mark this request as flipped so recreate works
                flippedState.getWebPlotRequest(band).setFlipY(flipped);
            }
            counters.incrementVis("Flip");


        } catch (Exception e) {
            flipResult= createError("on flipY", state, e);
        }

        return flipResult;
    }

    public static WebPlotResult rotateNorth(PlotState state, boolean north, float newZoomLevel) {
        return north ? rotate(state, PlotState.RotateType.NORTH, Double.NaN, state.getRotateNorthType(),newZoomLevel) :
                       rotate(state, PlotState.RotateType.UNROTATE, Double.NaN, null,newZoomLevel);
    }

    public static WebPlotResult rotateToAngle(PlotState state, boolean rotate, double angle, float newZoomLevel) {
        return rotate ? rotate(state, PlotState.RotateType.ANGLE, angle, null,newZoomLevel) :
                        rotate(state, PlotState.RotateType.UNROTATE, Double.NaN, null,newZoomLevel);
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
        ActiveCallCtx ctx= null;
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

            ctx= CtxControl.prepare(state);

            float newZoomLevel= inZoomLevel >0 ? inZoomLevel : state.getZoomLevel();

            if (rotate || isMultiOperations(state,PlotState.Operation.ROTATE)) {

                Band bands[]= state.getBands();
                WebPlotRequest rotateReq[]= new WebPlotRequest[bands.length];


                for (int i= 0; (i<bands.length); i++) {
                    Band band= bands[i];


                    FitsRead originalFR= ctx.getPlot().getHistogramOps(band,ctx.getFitsReadGroup()).getFitsRead();

                    String fStr= state.getOriginalFitsFileStr(band)!=null ?
                                 state.getOriginalFitsFileStr(band) :
                                 state.getWorkingFitsFileStr(band);
//                    String fStr= state.getWorkingFitsFileStr(band);

                    File originalFile= ServerContext.convertToFile(fStr);
                    File f= rotateNorth ? PlotServUtils.createRotateNorthFile(originalFile,originalFR,rotNorthType) :
                                          PlotServUtils.createRotatedFile(originalFile,originalFR,angle);

                    String fReq= ServerContext.replaceWithPrefix(f);

                    rotateReq[i]= WebPlotRequest.makeFilePlotRequest(fReq,newZoomLevel);
                    rotateReq[i].setThumbnailSize(state.getThumbnailSize());
                    state.setZoomLevel(newZoomLevel);
                }

                PlotState rotateState= PlotStateUtil.create(rotateReq, state);
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
                rotateResult= recreatePlot(rotateState);

                for (int i= 0; (i<bands.length); i++) { // mark this request as rotate north so recreate works
                    rotateState.getWebPlotRequest(bands[i]).setRotateNorth(true);
                }
                counters.incrementVis("Rotate");

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
                PlotState unrotateState= PlotStateUtil.create(unrotateReq, state);
                unrotateState.removeOperation(PlotState.Operation.ROTATE);
                for (Band band : bands) {
                    unrotateState.setWorkingFitsFileStr(state.getOriginalFitsFileStr(band), band);
                    unrotateState.setImageIdx(state.getOriginalImageIdx(band),band);
                    unrotateState.setOriginalImageIdx(state.getOriginalImageIdx(band),band);
                }
                rotateResult= recreatePlot(unrotateState);
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

        ActiveCallCtx ctx= null;
        try {

            ctx= CtxControl.prepare(state);
            ImagePlot plot= ctx.getPlot();
            for(Band band : state.getBands()) {
                FitsRead fr= plot.getHistogramOps(band,ctx.getFitsReadGroup()).getFitsRead();
                DataGroup dg = getFitsHeaders(fr.getHeader(), plot.getPlotDesc());
                RawDataSet rds = QueryUtil.getRawDataSet(dg);

                String string = String.valueOf(plot.getPixelScale());

                File f= PlotStateUtil.getOriginalFile(state, band);
                if (f==null) f= PlotStateUtil.getWorkingFitsFile(state, band);

                string += ";" + StringUtils.getSizeAsString(f.length(), true);

                rawDataMap.put(band, rds);
                stringMap.put(band, string);
            }
            BandInfo bandInfo = new BandInfo(rawDataMap, stringMap, null);

            retValue.putResult(WebPlotResult.BAND_INFO, bandInfo);
            counters.incrementVis("Fits header");

        } catch  (Exception e) {
            retValue =  createError("on getFitsInfo", state, e);
        }

        return retValue;
    }

    public static DataGroup getFitsHeaders(Header headers, String name) {
        //if ( headers.getNumberOfCards() > 0) {
            DataType comment = new DataType("Comments", String.class);
            DataType keyword = new DataType("Keyword", String.class);
            DataType value = new DataType("Value", String.class);
            comment.getFormatInfo().setWidth(80);
            value.getFormatInfo().setWidth(80);
            keyword.getFormatInfo().setWidth(68);
            DataType[] types = new DataType[]{
                               new DataType("#", Integer.class),
                               keyword, value, comment };
            DataGroup dg = new DataGroup("Headers - " + name, types);

             int i=0;
            for (Cursor itr = headers.iterator(); itr.hasNext();) {
                HeaderCard hc = (HeaderCard) itr.next();
                if (hc.isKeyValuePair()) {
                    DataObject row = new DataObject(dg);
                    row.setDataElement(types[0], i++);
                    row.setDataElement(types[1], hc.getKey());
                    row.setDataElement(types[2], hc.getValue());
                    row.setDataElement(types[3], hc.getComment());
                    dg.add(row);
                }
            }
      return dg;
    //}
    }



    private static class UseFullException extends Exception {}


    public static WebPlotResult getFitsHeaderInfo(PlotState state){
        WebPlotResult retValue = new WebPlotResult();

        HashMap<Band, RawDataSet> rawDataMap = new HashMap<Band, RawDataSet>();

        try {
            for(Band band : state.getBands()) {
                File f= PlotStateUtil.getWorkingFitsFile(state, band);
                if (f==null) f= PlotStateUtil.getOriginalFile(state, band);
                Fits fits= new Fits(f);
                BasicHDU hdu[]= fits.read();
                Header header= hdu[0].getHeader();
                if (header.containsKey("EXTEND") && header.getBooleanValue("EXTEND")) {
                    throw new UseFullException();
                }
                else {
                    DataGroup dg = getFitsHeaders(header, "fits data");
                    RawDataSet rds = QueryUtil.getRawDataSet(dg);
                    rawDataMap.put(band, rds);
                }
            }
            BandInfo bandInfo = new BandInfo(rawDataMap, null, null);

            retValue.putResult(WebPlotResult.BAND_INFO, bandInfo);
            counters.incrementVis("Fits header");

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

        ActiveCallCtx ctx= null;
        try{
            ctx= CtxControl.prepare(state);
            ImagePlot plot= ctx.getPlot();

            for(Band band : state.getBands()){
                // modeled after AreaStatisticsUtil.java lines:654 - 721
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
                HashMap<Metrics, Metric> metrics = AreaStatisticsUtil.getStatisticMetrics(plot, ctx.getFitsReadGroup(),
                                                                                          band, shape, newBoundingBox);

                String html;

                String token= "--;;--";

                Metric max = metrics.get(Metrics.MAX);
                ImageWorkSpacePt maxIp = max.getImageWorkSpacePt();
                html = AreaStatisticsUtil.formatPosHtml(LEFT, plot, maxIp);
                html = html + token +  AreaStatisticsUtil.formatPosHtml(RIGHT, plot, maxIp);

                Metric min = metrics.get(Metrics.MIN);
                ImageWorkSpacePt minIp = min.getImageWorkSpacePt();
                html = html + token + AreaStatisticsUtil.formatPosHtml(LEFT, plot, minIp);
                html = html + token + AreaStatisticsUtil.formatPosHtml(RIGHT, plot, minIp);

                Metric centroid = metrics.get(Metrics.CENTROID);
                ImageWorkSpacePt centroidIp = centroid.getImageWorkSpacePt();
                html = html + token + AreaStatisticsUtil.formatPosHtml(LEFT, plot, centroidIp);
                html = html + token + AreaStatisticsUtil.formatPosHtml(RIGHT, plot, centroidIp);

                Metric fwCentroid = metrics.get(Metrics.FW_CENTROID);
                ImageWorkSpacePt fwCentroidIp = fwCentroid.getImageWorkSpacePt();
                html = html + token + AreaStatisticsUtil.formatPosHtml(LEFT, plot, fwCentroidIp);
                html = html + token + AreaStatisticsUtil.formatPosHtml(RIGHT, plot, fwCentroidIp);

                //Add Lon and Lat strings for WorldPt conversion on the client
                Pt pt = null;
                try {
                    pt = plot.getWorldCoords(maxIp);
                } catch (ProjectionException e) {
                    pt= maxIp;
                }
                html = html + token +  String.valueOf(pt.serialize());
//                html = html + ";" +  String.valueOf(pt.getY());
                try {
                    pt = plot.getWorldCoords(minIp);
                } catch (ProjectionException e) {
                    pt= minIp;
                }
                html = html + token +  String.valueOf(pt.serialize());
//                html = html + ";" +  String.valueOf(pt.getY());
                try {
                    pt = plot.getWorldCoords(centroidIp);
                } catch (ProjectionException e) {
                    pt= centroidIp;
                }
                html = html + token +  String.valueOf(pt.serialize());
//                html = html + ";" +  String.valueOf(pt.getY());
                try {
                    pt = plot.getWorldCoords(fwCentroidIp);
                } catch (ProjectionException e) {
                    pt= fwCentroidIp;
                }
                html = html + token +  String.valueOf(pt.serialize());
//                html = html + ";" +  String.valueOf(pt.getY());

                metricsMap.put(band, metrics);
                stringMap.put(band, html);

                //DataEntry.HM da = new DataEntry.HM(metrics);
                //DataEntry.Str str = new DataEntry.Str(html);
            }

            BandInfo bandInfo = new BandInfo(null, stringMap, metricsMap);

            retValue.putResult(WebPlotResult.BAND_INFO, bandInfo);


//            retValue.putResult(WebPlotResult.STRING, str);
//            retValue.putResult(WebPlotResult.METRICS_HASH_MAP, da);
            counters.incrementVis("Area Stat");

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
            PlotClientCtx ctx= CtxControl.getPlotCtx(ctxStr);
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
            counters.incrementVis("Zoom");
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
        retval.putResult(WebPlotResult.PLOT_STATE, state);
        ctx.addZoomLevel(level);
        ctx.setPlotState(state);
        ImagePlot plot= ctx.getCachedPlot();
        if (plot!=null) plot.setZoomTo(level);
        return retval;
    }

    private static WebPlotResult setZoomLevelFull(PlotState state,
                                                  float level,
                                                  boolean temporary,
                                                  boolean fullScreen) throws FailedRequestException,
                                                                             IOException,
                                                                             FitsException {
        WebPlotResult retval;
        ActiveCallCtx ctx= CtxControl.prepare(state);
        ImagePlot plot= ctx.getPlot();
        String details= "From " + PlotServUtils.convertZoomToString(state.getZoomLevel()) +
                " to " + PlotServUtils.convertZoomToString(level);
        PlotServUtils.statsLog("zoom", details);
        PlotImages.ThumbURL thumb= ctx.getPlotClientCtx().getImages().getThumbnail();
        float oldLevel= plot.getPlotGroup().getZoomFact();
        plot.getPlotGroup().setZoomTo(level);
        state.setZoomLevel(level);
        PlotImages images= reviseImageFile(state,ctx.getPlotClientCtx(), plot, ctx.getFitsReadGroup(),
                                           temporary, fullScreen);
        images.setThumbnail(thumb);
        retval= new WebPlotResult(ctx.getKey());
        retval.putResult(WebPlotResult.PLOT_IMAGES,images);
        retval.putResult(WebPlotResult.PLOT_STATE,state);
        if (temporary) {
            plot.getPlotGroup().setZoomTo(oldLevel);
        }
        PlotServUtils.createThumbnail(plot,ctx.getFitsReadGroup(), images,false,state.getThumbnailSize());
        ctx.getPlotClientCtx().addZoomLevel(level);
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
        ActiveCallCtx ctx= null;

        try {
            ctx= CtxControl.prepare(state);
            ImagePlot plot= ctx.getPlot();
            if (band==NO_BAND)  {
                hOps= plot.getHistogramOps(NO_BAND,ctx.getFitsReadGroup());
                int id= plot.getImageData().getColorTableId();
                if (id==0 || id==1) bgColor= new Color(0xCC, 0xCC, 0x99);
            }
            else  {
                hOps= plot.getHistogramOps(band,ctx.getFitsReadGroup());
            }
            Histogram hist= hOps.getDataHistogram();

            dHist= hist.getHistogramArray();
            dHistColors= hOps.getDataHistogramColors(hist,state.getRangeValues(band));

            double meanDataAry[]= new double[dHist.length];
            for(int i= 0; i<meanDataAry.length; i++) {
                meanDataAry[i]= hOps.getMeanValueFromBin(hist,i);
            }


            boolean three= plot.isThreeColor();
            IndexColorModel newColorModel= plot.getImageData().getColorModel();


            HistogramDisplay dataHD= new HistogramDisplay();
            dataHD.setScaleOn2ndValue(true);
            dataHD.setSize(width,height);
            dataHD.setHistogramArray(dHist,dHistColors, newColorModel);
            if (three) {
                dataHD.setColorBand(band);
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



            String templateName= ctx.getPlotClientCtx().getImages().getTemplateName();
            String bandDesc= (band!=Band.NO_BAND) ? band.toString()+"-" : "";
            String dataFname= templateName+ "-dataHist-"+bandDesc + System.currentTimeMillis()+ ".png";
            String cbarFname= templateName+ "-colorbar-"+bandDesc + System.currentTimeMillis()+ ".png";


            File dir= ServerContext.getVisSessionDir();


            File dataFile= PlotServUtils.createHistImage(dataHD, dataHD,dir,bgColor, dataFname);
            File cbarFile= PlotServUtils.createHistImage(colorBarC, colorBarC,dir,bgColor, cbarFname);

            retval= new WebPlotResult(ctx.getKey());
            retval.putResult(WebPlotResult.DATA_HISTOGRAM, new DataEntry.IntArray(dHist));
            retval.putResult(WebPlotResult.DATA_BIN_MEAN_ARRAY,
                             new DataEntry.DoubleArray(meanDataAry));
            retval.putResult(WebPlotResult.DATA_HIST_IMAGE_URL,
                             new DataEntry.Str(ServerContext.replaceWithPrefix(dataFile)));
            retval.putResult(WebPlotResult.CBAR_IMAGE_URL,
                             new DataEntry.Str(ServerContext.replaceWithPrefix(cbarFile)));
            counters.incrementVis("Color change");

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
        ActiveCallCtx ctx;
        try {
            ctx= CtxControl.prepare(state);
            String pngFile= PlotPngCreator.createImagePng(ctx.getPlot(),ctx.getFitsReadGroup(), drawInfoList);
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
            File f= File.createTempFile("regionDownload-",".reg", ServerContext.getVisSessionDir());
            List<String> regOutList= StringUtils.parseStringList(regionData);
            RegionParser rp= new RegionParser();
            rp.saveFile(f, regOutList,"Region file generated by IRSA" );
            String retFile= ServerContext.replaceWithPrefix(f);
            retval = new WebPlotResult();
            retval.putResult(WebPlotResult.REGION_FILE_NAME,  new DataEntry.Str(retFile));
            counters.incrementVis("Region save");
        } catch (Exception e) {
            retval= createError("on getImagePng", null, e);
        }
        return retval;
    }

    public static WebPlotResult getDS9Region(String fileKey) {

        WebPlotResult retval;
        Cache sessionCache= UserCache.getInstance();

        try {
            File regFile = ServerContext.convertToFile(fileKey);
            if (regFile==null || !regFile.canRead()) {
                UploadFileInfo tmp= (UploadFileInfo)(sessionCache.get(new StringKey(fileKey)));
                regFile = tmp.getFile();
            }
            RegionParser parser= new RegionParser();
            RegionFactory.ParseRet r= parser.processFile(regFile);
            retval = new WebPlotResult();
            List<String> rAsStrList= toStringList(r.getRegionList());



            retval.putResult(WebPlotResult.REGION_DATA,
                             new DataEntry.Str(StringUtils.combineStringList(rAsStrList)));
            retval.putResult(WebPlotResult.REGION_ERRORS,
                             new DataEntry.Str(StringUtils.combineStringList(r.getMsgList())));

            UploadFileInfo fi= (UploadFileInfo)sessionCache.get(new StringKey(fileKey));
            String title;
            if (fi!=null) {
                title= fi.getFileName();
            }
            else {
                title= fileKey.startsWith("UPLOAD") ? "Region file" : regFile.getName();
            }
            retval.putResult(WebPlotResult.TITLE, new DataEntry.Str(title));
            PlotServUtils.statsLog("ds9Region", fileKey);
            counters.incrementVis("Region read");
        } catch (Exception e) {
            retval= createError("on getDSRegion", null, e);
        }
        return retval;

    }



    public static synchronized boolean addSavedRequest(String saveKey, WebPlotRequest request) {
        Cache cache= UserCache.getInstance();
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
        Cache cache= UserCache.getInstance();
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
        PlotClientCtx ctx= CtxControl.getPlotCtx(state.getContextString());
        if (ctx!=null) {
            retval= isPlotValid(ctx);
        }
        return retval;
    }

    private static boolean isPlotValid(PlotClientCtx ctx)  {
        return (ctx.getCachedPlot()!=null);
    }


    private static PlotImages reviseImageFile(PlotState state, PlotClientCtx ctx,
                                              ImagePlot plot, ActiveFitsReadGroup frGroup) throws IOException {
        return reviseImageFile(state,ctx,plot,frGroup,false, false);
    }


    private static PlotImages reviseImageFile(PlotState state,PlotClientCtx ctx, ImagePlot plot, ActiveFitsReadGroup frGroup,
                                              boolean temporary, boolean fullScreen) throws IOException {

        String base= PlotServUtils.makeTileBase(state);

        File dir= ServerContext.getVisSessionDir();
        int tileCnt= fullScreen ? 1 : 2;
        PlotImages images= PlotServUtils.writeImageTiles(dir, base, plot, frGroup, fullScreen, tileCnt);
        if (!temporary) ctx.setImages(images);
        return images;
    }


    private static PlotImages reviseImageFileNoCreation(PlotState state,
                                                        PlotClientCtx ctx,
                                                        float zFactor,
                                                        int screenWidth,
                                                        int screenHeight) {
//        String base= PlotServUtils.makeRevisedBase(ctx);
        String base= PlotServUtils.makeTileBase(state);

        File dir= ServerContext.getVisSessionDir();
        PlotImages images= PlotServUtils.makeImageTilesNoCreation(dir, base, zFactor, screenWidth, screenHeight);
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

    private static WebPlotResult createError(String logMsg, PlotState state, Exception e) {
        return createError(logMsg,state, null, e);
    }

    private static WebPlotResult createError(String logMsg, PlotState state, WebPlotRequest reqAry[], Exception e) {
        WebPlotResult retval;
        boolean userAbort= false;
        String progressKey= "";
        if (reqAry!=null) {
            for(int i=0; (i<reqAry.length);i++) {
                if (reqAry[i]!=null) {
                    progressKey= reqAry[i].getProgressKey();
                    break;
                }
            }
        }

        if (e instanceof FileRetrieveException) {
            FileRetrieveException fe= (FileRetrieveException)e;
            retval= WebPlotResult.makeFail("Retrieve failed", "Could not retrieve fits file",fe.getDetailMessage(),progressKey);
            fe.setSimpleToString(true);
        }
        else if (e instanceof FailedRequestException ) {
            FailedRequestException fe= (FailedRequestException)e;
            retval= WebPlotResult.makeFail(fe.getUserMessage(), fe.getUserMessage(),fe.getDetailMessage(),progressKey);
            fe.setSimpleToString(true);
            userAbort= VisContext.PLOT_ABORTED.equals(fe.getDetailMessage());
        }
        else if (e instanceof SecurityException ) {
            retval= WebPlotResult.makeFail("No Access", "You do not have access to this data,",e.getMessage(),progressKey);
        }
        else {
            retval= WebPlotResult.makeFail("Server Error, Please Report", e.getMessage(),null,progressKey);
        }
        List<String> messages= new ArrayList<String>(8);
        messages.add(logMsg);
        if (state!=null) {
            messages.add("Context String: " +state.getContextString());
            try {
                if (state.isThreeColor()) {
                    for(Band band : state.getBands()) {
                        messages.add("Fits Filename (" + band.toString() + "): " + PlotStateUtil.getWorkingFitsFile(state, band));
                    }

                }
                else {
                    messages.add("Fits Filename: " + PlotStateUtil.getWorkingFitsFile(state, NO_BAND));

                }
            }
            catch (Exception ignore) {
                // if anything goes wrong here we have to recover, this is only for logging
            }
            PlotClientCtx ctx= CtxControl.getPlotCtx(state.getContextString());
            if (ctx!=null) ctx.freeResources(PlotClientCtx.Free.ALWAYS);
        }
        if (reqAry!=null) {
            for(WebPlotRequest req : reqAry)   {
                if  (req!=null) messages.add("Request: " + req.prettyString());
            }
        }

//        messages.add(e.toString());
        if (userAbort) {
            _log.info(logMsg+": "+ VisContext.PLOT_ABORTED);
        }
        else if (e instanceof FileRetrieveException) {
            _log.info(logMsg+": "+ ((FileRetrieveException)e).getRetrieveServiceID()+": File retrieve failed");
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


    private static WebPlotResult makeNewPlotResult(WebPlotInitializer wpInit[]) {
        PlotState state= wpInit[0].getPlotState();
        PlotClientCtx ctx= CtxControl.getPlotCtx(state.getContextString());
        WebPlotResult retval= new WebPlotResult(ctx.getKey());
        retval.putResult(WebPlotResult.PLOT_CREATE,new CreatorResults(wpInit));
        return retval;
    }

}

