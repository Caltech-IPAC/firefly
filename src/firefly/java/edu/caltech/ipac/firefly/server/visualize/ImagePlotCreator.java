/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 2/17/11
 * Time: 3:33 PM
 */


import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.RequestType;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.WebFitsData;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.FitsRead;
import edu.caltech.ipac.visualize.plot.GeomException;
import edu.caltech.ipac.visualize.plot.Histogram;
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

    static ImagePlotInfo[] makeAllNoBand(PlotState stateAry[],
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
             WebPlotRequest req= stateAry[i].getWebPlotRequest();
             if (readAry.length>3) {
                 PlotServUtils.updateProgress(req, ProgressStat.PType.CREATING,
                                              PlotServUtils.CREATING_MSG+": "+ (i+1)+" of "+readAry.length);
             }
             else  {
                 PlotServUtils.updateProgress(req, ProgressStat.PType.CREATING, PlotServUtils.CREATING_MSG);
             }
             ActiveFitsReadGroup frGroup= new ActiveFitsReadGroup();
             frGroup.setFitsRead(readInfo.getBand(),readInfo.getFitsRead());
             ImagePlot plot= createImagePlot(stateAry[i], frGroup, readInfo.getBand(),readInfo.getDataDesc(),zoomChoice, readAry.length>1);
             WebFitsData wfData= makeWebFitsData(plot,frGroup, readInfo.getBand(),readInfo.getOriginalFile());
             wfDataMap.put(Band.NO_BAND,wfData);
             Map<Band,ModFileWriter> fileWriterMap= new LinkedHashMap<Band,ModFileWriter>(1);
             if (readInfo.getModFileWriter()!=null) fileWriterMap.put(Band.NO_BAND,readInfo.getModFileWriter());
             piAry[i]= new ImagePlotInfo(stateAry[i],plot, frGroup,readInfo.getDataDesc(), wfDataMap,fileWriterMap);
         }

         return piAry;
     }

    static ImagePlotInfo makeOneImagePerBand(PlotState state,
                                             Map<Band, FileReadInfo[]> readInfoMap,
                                             ZoomChoice zoomChoice)  throws FailedRequestException,
                                                                            FitsException,
                                                                            GeomException,
                                                                            IOException {


        ImagePlotInfo retval;
        ImagePlot plot= null;
        boolean first= true;
        Map<Band,WebFitsData> wfDataMap= new LinkedHashMap<>(5);
        Map<Band,ModFileWriter> fileWriterMap= new LinkedHashMap<>(5);
        ActiveFitsReadGroup frGroup= new ActiveFitsReadGroup();
        for(Map.Entry<Band,FileReadInfo[]> entry :  readInfoMap.entrySet()) {
            Band band= entry.getKey();
            FileReadInfo readInfoAry[]= entry.getValue();
            int imageIdx= state.getImageIdx(band);
            if (state.getPrimaryRequest().containsParam(WebPlotRequest.MULTI_IMAGE_IDX)) {
                if (imageIdx>=readInfoAry.length) {
                    throw new FailedRequestException("Plot Failed", "Could not find extension number "+imageIdx+
                            ". The file contains only "+readInfoAry.length + " image extension" );
                }
            }
            FileReadInfo readInfo= readInfoAry[imageIdx];
            frGroup.setFitsRead(band,readInfo.getFitsRead());
            if (first) {
                plot= createImagePlot(state,frGroup,band, readInfo.getDataDesc(),zoomChoice,false);
                if (state.isThreeColor()) {
                    plot.setThreeColorBand(state.isBandVisible(band) ? readInfo.getFitsRead() :null,
                            band,frGroup);
                }
                if (readInfo.getModFileWriter()!=null) fileWriterMap.put(band,readInfo.getModFileWriter());
                first= false;
            }
            else {
                ModFileWriter mfw= createBand(state,plot,readInfo,frGroup);
                if (mfw!=null)                              fileWriterMap.put(band,mfw);
                else if (readInfo.getModFileWriter()!=null) fileWriterMap.put(band,readInfo.getModFileWriter());
            }
            WebFitsData wfData= makeWebFitsData(plot, frGroup, readInfo.getBand(), readInfo.getOriginalFile());
            wfDataMap.put(band, wfData);
        }
        String desc= make3ColorDataDesc(readInfoMap);
        retval= new ImagePlotInfo(state,plot,frGroup, desc, wfDataMap,fileWriterMap);

        if (first) _log.error("something is wrong, plot not setup correctly - no color bands specified");
        return retval;
     }

    /**
     * Create the ImagePlot.  If this is the first time the plot has been created the compute the
     * appropriate zoom, otherwise using the zoom level in the PlotState object.
     * Using the FitsRead in the PlotData object.  Record the zoom level in the PlotData object.
     * @param state plot state
     * @param frGroup fits read group
     * @param plotDesc plot description
     * @return the image plot object
     * @throws nom.tam.fits.FitsException if creating plot fails
     */
    static ImagePlot createImagePlot(PlotState state,
                                     ActiveFitsReadGroup frGroup,
                                     Band band,
                                     String plotDesc,
                                     ZoomChoice zoomChoice,
                                     boolean    isMultiImage) throws FitsException {

        ImagePlot plot;
        RangeValues rv= state.getRangeValues();
        float zoomLevel= zoomChoice.getZoomLevel();
        WebPlotRequest request= state.getPrimaryRequest();
        if (rv==null) {
            rv= FitsRead.getDefaultRangeValues();
            state.setRangeValues(rv,state.firstBand());
        }



        if (request.containsParam(WebPlotRequest.PLOT_AS_MASK) && request.containsParam(WebPlotRequest.MASK_COLORS)) {
            plot= PlotServUtils.makeMaskImagePlot(frGroup, zoomLevel, request, rv);
            int rWidth= request.getMaskRequiredWidth();
            int rHeight= request.getMaskRequiredHeight();
            if (plot.getImageDataWidth()!=rWidth || plot.getImageDataWidth()!=rHeight) {
                String primDim= rWidth+"x"+rHeight;
                String overDim= plot.getImageDataWidth()+"x"+plot.getImageDataHeight();
                _log.warn( "Mask Overlay does not match the primary plot dimensions ("+ overDim+" vs "+primDim+")");
            }
        }
        else { // standard
            plot= PlotServUtils.makeImagePlot( frGroup, zoomLevel, state.isThreeColor(),
                                               band, state.getColorTableId(), rv);
        }


        if (state.isNewPlot()) { // new plot requires computing the zoom level

            zoomLevel= computeZoomLevel(plot,zoomChoice);
            plot.getPlotGroup().setZoomTo(zoomLevel);
            state.setZoomLevel(zoomLevel);
        }

        state.setZoomLevel(zoomLevel);
        initPlotTitle(state, plot, frGroup, plotDesc, isMultiImage);
        return plot;
    }

    public static ModFileWriter createBand(PlotState state,
                                           ImagePlot plot,
                                           FileReadInfo readInfo,
                                           ActiveFitsReadGroup frGroup)
                                                                throws FitsException,
                                                                       IOException,
                                                                       GeomException {
        ModFileWriter retval= null;
        Band band= readInfo.getBand();


        plot.setThreeColorBand(state.isBandVisible(readInfo.getBand()) ? readInfo.getFitsRead() :null,
                               readInfo.getBand(),frGroup);
        HistogramOps histOps= plot.getHistogramOps(band,frGroup);
        FitsRead tmpFR= histOps.getFitsRead();
        if (tmpFR!=readInfo.getFitsRead() && readInfo.getWorkingFile()!=null) { // testing to see it the fits read got geomed when the band was added
            state.setImageIdx(0, band);
            retval = new ModFileWriter.GeomFileWriter(readInfo.getWorkingFile(),0,tmpFR,readInfo.getBand(),false);
            FitsCacher.addFitsReadToCache(retval.getTargetFile(), new FitsRead[]{tmpFR});
        }

        RangeValues rv= state.getRangeValues(readInfo.getBand());
        if (rv==null) {
            rv= FitsRead.getDefaultFutureStretch();
            state.setRangeValues(rv, readInfo.getBand());
        }
        histOps.recomputeStretch(rv, true);
        return retval;
    }

    private static void initPlotTitle(PlotState state,
                                      ImagePlot plot,
                                      ActiveFitsReadGroup frGroup,
                                      String dataDesc,
                                      boolean isMultiImage) {

        WebPlotRequest req= state.getWebPlotRequest();
        plot.setPlotDesc("");
        Header header= frGroup.getFitsRead(state.firstBand()).getHeader();


        switch (req.getTitleOptions()) {
            case PLOT_DESC:
                String base= req.getTitle()==null ? "" : req.getTitle();
                plot.setPlotDesc(base+ dataDesc);
                break;
            case NONE: // none is overridden for multi images files
            case FILE_NAME: // file name is further processed on client side
                if (isMultiImage) plot.setPlotDesc(findTitleByHeader(header,state,req));
                break;
            case HEADER_KEY:
                plot.setPlotDesc(findTitleByHeader(header,state,req));
                break;
            case PLOT_DESC_PLUS:
                String s= req.getPlotDescAppend();
                plot.setPlotDesc(req.getTitle()+ (s!=null ? " "+s : ""));
                break;
            case SERVICE_OBS_DATE:
                if (req.getRequestType()== RequestType.SERVICE) {
                    String title= req.getTitle() + ": " +
                                  PlotServUtils.getDateValueFromServiceFits(req.getServiceType(), header);
                    plot.setPlotDesc(title);
                }
                break;
        }
    }

    private static String findTitleByHeader(Header header, PlotState state, WebPlotRequest req) {
        String headerKey[]= new String[] {
                req.getHeaderKeyForTitle(),
                "EXTNAME", "EXTTYPE",
                state.getCubeCnt()>0 ? "PLANE"+state.getImageIdx(state.firstBand()) : null
        };
        return findCard(header, headerKey);
    }

    private static String findCard(Header header, String[] keys) {
        HeaderCard card= null;
        for(String k : keys) {
            if (k!=null) card= header.findCard(k);
            if (card!=null) break;
        }
        return card!=null ? card.getValue() : "";
    }


    private static String make3ColorDataDesc(Map<Band, FileReadInfo[]> readInfoMap) {

        StringBuffer desc= new StringBuffer(100);
        desc.append("3 Color: ");
        for(Map.Entry<Band,FileReadInfo[]> entry : readInfoMap.entrySet()) {
            desc.append(readInfoMap.get(entry.getKey())[0].getDataDesc());
        }
        return desc.toString();
    }



    private static float computeZoomLevel(ImagePlot plot, ZoomChoice zoomChoice) {
        int width=  plot.getImageDataWidth();
        int height= plot.getImageDataHeight();
        float retval= zoomChoice.getZoomLevel();
        if (zoomChoice.getZoomType()== ZoomType.TO_WIDTH) {
            retval= (float)zoomChoice.getWidth() / (float)width ;
            if (zoomChoice.hasMaxZoomLevel()) {
                if (retval>zoomChoice.getMaxZoomLevel()) retval=zoomChoice.getMaxZoomLevel();
            }
        }
        else if (zoomChoice.getZoomType()== ZoomType.FULL_SCREEN || zoomChoice.getZoomType()== ZoomType.TO_WIDTH_HEIGHT) {
            retval= VisUtil.getEstimatedFullZoomFactor(VisUtil.FullType.WIDTH_HEIGHT,width, height,
                                                       zoomChoice.getWidth(), zoomChoice.getHeight());
            if (zoomChoice.hasMaxZoomLevel()) {
                if (retval>zoomChoice.getMaxZoomLevel()) retval=zoomChoice.getMaxZoomLevel();
            }
        }
        else if (zoomChoice.getZoomType()== ZoomType.ARCSEC_PER_SCREEN_PIX) {
            retval= (float)plot.getPixelScale() / zoomChoice.getArcsecPerScreenPix();
        }
        return retval;
    }


    /**
     * 5/13/16 LZ modified to add the beta in the WebFitsData calling parameter
     * @param plot
     * @param frGroup
     * @param band
     * @param f
     * @return
     */
    public static WebFitsData makeWebFitsData(ImagePlot plot, ActiveFitsReadGroup frGroup, Band band, File f) {
        long fileLength= (f!=null && f.canRead()) ? f.length() : 0;
        HistogramOps ops= plot.getHistogramOps(band,frGroup);
        Histogram hist= ops.getDataHistogram();
        return new WebFitsData( ops.getDataMin(hist), ops.getDataMax(hist),ops.getBeta(),
                                fileLength, plot.getFluxUnits(band,frGroup));
    }
}

