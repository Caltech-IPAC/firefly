package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 2/17/11
 * Time: 3:33 PM
 */


import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.WebFitsData;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.visualize.plot.FitsRead;
import edu.caltech.ipac.visualize.plot.GeomException;
import edu.caltech.ipac.visualize.plot.HistogramOps;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.RangeValues;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class ImagePlotCreator {

    private static final Logger.LoggerImpl _log= Logger.getLogger();

    static ImagePlotInfo[] makeAllNoBand(String workingCtxStr,
                                         PlotState stateAry[],
                                         FileReadInfo[] readAry,
                                         ZoomChoice zoomChoice) throws FailedRequestException,
                                                                       FitsException,
                                                                       GeomException,
                                                                       IOException {
         // never use this method with three color plots

         ImagePlotInfo piAry[]= new ImagePlotInfo[readAry.length];
         FileReadInfo readInfo;
         Map<Band,WebFitsData> wfDataMap= new LinkedHashMap<Band,WebFitsData>(5);
         for(int i= 0; (i<readAry.length); i++)  {
             readInfo= readAry[i];
             ImagePlot plot= createImagePlot(stateAry[i],readInfo,zoomChoice);
             WebFitsData wfData= makeWebFitsData(plot,readInfo.getBand(),readInfo.getOriginalFile());
             wfDataMap.put(Band.NO_BAND,wfData);
             Map<Band,ModFileWriter> fileWriterMap= new LinkedHashMap<Band,ModFileWriter>(1);
             if (readInfo.getModFileWriter()!=null) fileWriterMap.put(Band.NO_BAND,readInfo.getModFileWriter());
             piAry[i]= new ImagePlotInfo(stateAry[i],plot, readInfo.getDataDesc(), wfDataMap,fileWriterMap);
             VisContext.shouldContinue(workingCtxStr);
         }

         return piAry;
     }

    static ImagePlotInfo makeOneImagePerBand(String workingCtxStr,
                                             PlotState state,
                                             Map<Band, FileReadInfo[]> readInfoMap,
                                             ZoomChoice zoomChoice)  throws FailedRequestException,
                                                                            FitsException,
                                                                            GeomException,
                                                                            IOException {


         ImagePlotInfo retval;
         ImagePlot plot= null;
         boolean first= true;
         Map<Band,WebFitsData> wfDataMap= new LinkedHashMap<Band,WebFitsData>(5);
         Map<Band,ModFileWriter> fileWriterMap= new LinkedHashMap<Band,ModFileWriter>(5);
         for(Map.Entry<Band,FileReadInfo[]> entry :  readInfoMap.entrySet()) {
             Band band= entry.getKey();
             FileReadInfo readInfoAry[]= entry.getValue();
             FileReadInfo readInfo= readInfoAry[state.getImageIdx(band)];
             int bIdx= PlotServUtils.cnvtBand(readInfo.getBand());
             if (first) {
                 plot= createImagePlot(state,readInfo,zoomChoice);
                 if (state.isThreeColor()) {
                     plot.setThreeColorBand(readInfo.getFitsRead(),bIdx);
                     plot.setThreeColorBandVisible(bIdx, state.isBandVisible(readInfo.getBand()));
                 }
                 first= false;
             }
             else {
                 ModFileWriter mfw= createBand(state,plot,readInfo);
                 if (mfw!=null) {
                     fileWriterMap.put(band,mfw);
                 }
                 else if (readInfo.getModFileWriter()!=null) {
                     fileWriterMap.put(band,mfw);
                 }
             }
             WebFitsData wfData= makeWebFitsData(plot, readInfo.getBand(), readInfo.getOriginalFile());
             wfDataMap.put(band, wfData);
             VisContext.shouldContinue(workingCtxStr);
         }
         String desc= make3ColorDataDesc(readInfoMap);
         retval= new ImagePlotInfo(state,plot,desc, wfDataMap,fileWriterMap);

         if (first) _log.error("something is wrong, plot not setup correctly - no color bands specified");
         return retval;
     }

    /**
     * Create the ImagePlot.  If this is the first time the plot has been created the compute the
     * appropriate zoom, otherwise using the zoom level in the PlotState object.
     * Using the FitsRead in the PlotData object.  Record the zoom level in the PlotData object.
     * @param state plot state
     * @param readInfo the object containing the fits file read information
     * @return the image plot object
     * @throws nom.tam.fits.FitsException if creating plot fails
     */
    static ImagePlot createImagePlot(PlotState state,
                                     FileReadInfo readInfo,
                                     ZoomChoice zoomChoice) throws FitsException {

        RangeValues rv= state.getRangeValues(state.firstBand());
        if (rv==null) {
            rv= FitsRead.getDefaultFutureStretch();
            state.setRangeValues(rv,state.firstBand());
        }

        // todo uncomment
//        state.setDecimationLevel(pd._band,pd._decimation);
        float zoomLevel= zoomChoice.getZoomLevel();

        ImagePlot plot= PlotServUtils.makeImagePlot( readInfo.getFitsRead(), zoomLevel,
                                                     state.isThreeColor(),
                                                     state.getColorTableId(), rv);


        if (state.isNewPlot()) { // new plot requires computing the zoom level

            zoomLevel= computeZoomLevel(plot,zoomChoice);
            plot.getPlotGroup().setZoomTo(zoomLevel);
            state.setZoomLevel(zoomLevel);
        }

        state.setZoomLevel(zoomLevel);
        initPlotTitle(state,plot,readInfo.getDataDesc(),readInfo.getDateString());
        return plot;
    }

    public static ModFileWriter createBand(PlotState state, ImagePlot plot, FileReadInfo readInfo)
                                                                throws FitsException,
                                                                       IOException,
                                                                       GeomException {
        ModFileWriter retval= null;
        int bIdx= PlotServUtils.cnvtBand(readInfo.getBand());
        plot.addThreeColorBand(readInfo.getFitsRead(),bIdx);
        plot.setThreeColorBandVisible(bIdx, state.isBandVisible(readInfo.getBand()));
        HistogramOps histOps= plot.getHistogramOps(bIdx);
        FitsRead tmpFR= histOps.getFitsRead();
        if (tmpFR!=readInfo.getFitsRead() && readInfo.getWorkingFile()!=null) { // testing to see it the fits read got geomed when the band was added
            state.setImageIdx(0, readInfo.getBand());
            retval = new ModFileWriter.GeomFileWriter(readInfo.getWorkingFile(),0,tmpFR,readInfo.getBand(),false);
        }

        RangeValues rv= state.getRangeValues(readInfo.getBand());
        if (rv==null) {
            rv= FitsRead.getDefaultFutureStretch();
            state.setRangeValues(rv, readInfo.getBand());
        }
        histOps.recomputeStretch(rv,true);
        return retval;
    }

    static void initPlotTitle(PlotState state, ImagePlot plot, String dataDesc, String dateStr) {
        WebPlotRequest req= state.getWebPlotRequest(state.firstBand());
        if (req.containsParam(WebPlotRequest.HEADER_FOR_TITLE)) {
            Header header= plot.getFitsRead().getHeader();
            HeaderCard card= header.findCard(req.getHeaderKeyForTitle());
            String hTitle= card!=null ? card.getValue() : "";
            plot.setPlotDesc(hTitle);
        }
        else if (req.getUseDataDescForTitle()) {
            String base= req.getTitle()==null ? "" : req.getTitle();
            plot.setPlotDesc(base+ dataDesc);
        }
        else if (req.containsParam(WebPlotRequest.ADD_DATE_TITLE)) {
            plot.setPlotDesc(req.getTitle()+" "+dateStr);
        }
        else {
            plot.setPlotDesc("");
        }
    }


    private static String make3ColorDataDesc(Map<Band, FileReadInfo[]> readInfoMap) {

        StringBuffer desc= new StringBuffer(100);
        desc.append("3 Color: ");
        for(Map.Entry<Band,FileReadInfo[]> entry : readInfoMap.entrySet()) {
            desc.append(readInfoMap.get(entry.getKey())[0].getDataDesc());
        }
        return desc.toString();
    }



    static float computeZoomLevel(ImagePlot plot, ZoomChoice zoomChoice) {
        int width=  plot.getImageDataWidth();
        int height= plot.getImageDataHeight();
        float retval= zoomChoice.getZoomLevel();
        if (zoomChoice.isSmartZoom()) {
            retval= computeSmartZoom(width,height,zoomChoice.getZoomType());
        }
        else if (zoomChoice.getZoomType()== ZoomType.TO_WIDTH) {
            retval= (float)zoomChoice.getWidth() / (float)width ;
            if (retval>8) retval=8;
        }
        else if (zoomChoice.getZoomType()== ZoomType.FULL_SCREEN) {
            retval= VisUtil.getEstimatedFullZoomFactor(VisUtil.FullType.WIDTH_HEIGHT,width, height,
                                                       zoomChoice.getWidth(), zoomChoice.getHeight());
            if (retval>8) retval=8;
        }
        else if (zoomChoice.getZoomType()== ZoomType.ARCSEC_PER_SCREEN_PIX) {
            retval= (float)plot.getPixelScale() / zoomChoice.getArcsecPerScreenPix();
        }
        return retval;
    }

    static float computeSmartZoom(int width, int height, ZoomType zoomType) {
        float zoomLevel;
        if (width> 6200 || height> 6200 )      zoomLevel= 1F/32F;
        else if (width> 2800 || height> 2800 ) zoomLevel= 1F/16F;
        else if (width> 2000 || height> 2000 ) zoomLevel= 1F/8F;
        else if (width> 1200 || height> 1200 ) zoomLevel= 1F/4F;
        else if (width> 500 || height> 500 )   zoomLevel= 1F/2F;
        else if (width< 100 && height< 100 )   zoomLevel= 4F;
        else if (width< 30 && height< 30 )     zoomLevel= 4F;
        else                  zoomLevel= 1;

        width*= zoomLevel;
        height*= zoomLevel;
        switch (zoomType) {
            case SMART_SMALL:
                if (width>600 || height > 600) zoomLevel/=4;
                else if (width>200 || height > 200) zoomLevel/=2;
                break;
            case SMART_LARGE:
                if (width<400 || height < 400) zoomLevel/=2;
                zoomLevel*=2;
                break;
        }

        return zoomLevel;
    }

    public static WebFitsData makeWebFitsData(ImagePlot plot, Band band, File f) {
        long fileLength= (f!=null && f.canRead()) ? f.length() : 0;
        int idx= PlotServUtils.cnvtBand(band);
        HistogramOps ops= plot.getHistogramOps(idx);
        return new WebFitsData( ops.getDataMin(), ops.getDataMax(),
                                fileLength, plot.getFluxUnits(idx));
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
