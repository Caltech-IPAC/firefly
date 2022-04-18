/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.RelatedData;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebFitsData;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.Histogram;
import edu.caltech.ipac.visualize.plot.ImageMask;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.RangeValues;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.plotdata.GeomException;
import nom.tam.fits.FitsException;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Trey Roby
 *
 * Edit history
 *
 * 1/3/17
 * LZ: DM-8648
 */
public class ImagePlotCreator {

    private static final Logger.LoggerImpl _log= Logger.getLogger();

    static PlotInfo[] makeAllNoBand(PlotState[] stateAry, WebPlotReader.FileReadInfo[] readAry) throws FitsException {
         // never use this method with three color plots

         PlotInfo[] piAry= new PlotInfo[readAry.length];
         WebPlotReader.FileReadInfo readInfo;

         for(int i= 0; (i<readAry.length); i++)  {
             readInfo= readAry[i];
             WebPlotRequest req= stateAry[i].getWebPlotRequest();

             boolean notify= readAry.length<5 || i % ((readAry.length/10)+1)==0;
             if (notify) notifyCreating(req,i,readAry.length);

             ActiveFitsReadGroup frGroup= new ActiveFitsReadGroup();
             frGroup.setFitsRead(readInfo.band(),readInfo.fitsRead());
             ImagePlot plot= createImagePlot(stateAry[i], frGroup, readInfo.band(),readInfo.dataDesc(),readAry.length>1);
             WebFitsData wfData= makeWebFitsData(frGroup, readInfo.band(),readInfo.originalFile());
             piAry[i]= new PlotInfo(stateAry[i],plot, readInfo.fileInfo(), frGroup,
                     readInfo.dataDesc(), readInfo.relatedData(),
                     Collections.singletonMap(Band.NO_BAND,wfData),
                     Collections.emptyMap());
         }

         return piAry;
     }

     private static void notifyCreating(WebPlotRequest req, int cnt, int totLength) {
         if (totLength > 3) {
             PlotServUtils.updatePlotCreateProgress(req, ProgressStat.PType.CREATING,
                     PlotServUtils.CREATING_MSG + ": " + (cnt + 1) + " of " + totLength);
         } else {
             PlotServUtils.updatePlotCreateProgress(req, ProgressStat.PType.CREATING, PlotServUtils.CREATING_MSG);
         }
     }

    static PlotInfo makeOneImagePerBand(PlotState state,
                                        Map<Band, WebPlotReader.FileReadInfo[]> readInfoMap)
            throws FailedRequestException, FitsException, GeomException, IOException {


        ImagePlot plot= null;
        boolean first= true;
        Map<Band,WebFitsData> wfDataMap= new LinkedHashMap<>(5);
        Map<Band,ModFileWriter> fileWriterMap= new LinkedHashMap<>(5);
        ActiveFitsReadGroup frGroup= new ActiveFitsReadGroup();
        List<RelatedData> relatedData= null;
        FileInfo fileInfo= null;

        for(Map.Entry<Band, WebPlotReader.FileReadInfo[]> entry :  readInfoMap.entrySet()) {
            Band band= entry.getKey();
            WebPlotReader.FileReadInfo[] readInfoAry= entry.getValue();
            int imageIdx= state.getImageIdx(band);
            if (state.getPrimaryRequest().containsParam(WebPlotRequest.MULTI_IMAGE_IDX)) {
                if (imageIdx>=readInfoAry.length) {
                    throw new FailedRequestException("Plot Failed", "Could not find extension number "+imageIdx+
                            ". The file contains only "+readInfoAry.length + " image extension" );
                }
            }
            WebPlotReader.FileReadInfo readInfo= readInfoAry[imageIdx];
            frGroup.setFitsRead(band,readInfo.fitsRead());
            if (first) {
                fileInfo= readInfo.fileInfo();
                plot= createImagePlot(state,frGroup,band, readInfo.dataDesc(),false);
                if (state.isThreeColor()) {
                    plot.setThreeColorBand(readInfo.fitsRead(), band,frGroup);
                    stretchBand(band,state,plot,frGroup);
                }
                first= false;
                relatedData= readInfo.relatedData();
            }
            else {
                ModFileWriter mfw= createBand(state,plot,readInfo,frGroup);
                if (mfw!=null) fileWriterMap.put(band,mfw);
            }
            WebFitsData wfData= makeWebFitsData(frGroup, readInfo.band(), readInfo.originalFile());
            wfDataMap.put(band, wfData);
        }
        String desc= make3ColorDataDesc(readInfoMap);

        if (first) _log.error("something is wrong, plot not setup correctly - no color bands specified");
        return new PlotInfo(state,plot,fileInfo, frGroup, desc, relatedData, wfDataMap, fileWriterMap);
     }

    /**
     * Create the ImagePlot.  If this is the first time the plot has been created the compute the
     * appropriate zoom, otherwise using the zoom level in the PlotState object.
     * Using the FitsRead in the PlotData object.  Record the zoom level in the PlotData object.
     * @param state plot state
     * @param frGroup fits read group
     * @param dataDesc plot description
     * @return the image plot object
     * @throws nom.tam.fits.FitsException if creating plot fails
     */
    static ImagePlot createImagePlot(PlotState state,
                                     ActiveFitsReadGroup frGroup,
                                     Band band,
                                     String dataDesc,
                                     boolean    isMultiImage) throws FitsException {

        ImagePlot plot;
        RangeValues rv= state.getRangeValues();
        float zoomLevel= state.getWebPlotRequest().getInitialZoomLevel();
        WebPlotRequest request= state.getPrimaryRequest();
        if (rv==null) {
            rv= FitsRead.getDefaultRangeValues();
            state.setRangeValues(rv,state.firstBand());
        }

        if (request.containsParam(WebPlotRequest.PLOT_AS_MASK) && request.containsParam(WebPlotRequest.MASK_COLORS)) {
            plot= makeMaskImagePlot(frGroup, zoomLevel, request, rv);
            int rWidth= request.getMaskRequiredWidth();
            int rHeight= request.getMaskRequiredHeight();
            if (plot.getImageDataWidth()!=rWidth || plot.getImageDataWidth()!=rHeight) {
                String primDim= rWidth+"x"+rHeight;
                String overDim= plot.getImageDataWidth()+"x"+plot.getImageDataHeight();
                _log.warn( "Mask Overlay does not match the primary plot dimensions ("+ overDim+" vs "+primDim+")");
            }
        }
        else { // standard
            plot= makeImagePlot( frGroup, zoomLevel, state.isThreeColor(), request.getInitialColorTable(),
                                               band, rv);
        }

        plot.setPlotDesc( PlotServUtils.makePlotDesc(state, frGroup, dataDesc, isMultiImage));
        return plot;
    }

    private static ImagePlot makeImagePlot(ActiveFitsReadGroup frGroup,
                                   float     initialZoomLevel,
                                   boolean   threeColor,
                                   int       colorTableId,
                                   Band      band,
                                   RangeValues stretch) throws FitsException {
        return new ImagePlot(frGroup,initialZoomLevel, threeColor, band, colorTableId, stretch);
    }

    private static ImagePlot makeMaskImagePlot(ActiveFitsReadGroup frGroup,
                                       float               initialZoomLevel,
                                       WebPlotRequest      request,
                                       RangeValues         stretch) throws FitsException {

        ImageMask[] maskDef= createMaskDefinition(request);
        return new ImagePlot(frGroup,initialZoomLevel, sortImageMaskArrayInIndexOrder(maskDef) , stretch);
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
        for (ImageMask imageMask : imageMasks) {
            unsortedMap.put(imageMask.getIndex(), imageMask);
        }

        Map<Integer, ImageMask> treeMap = new TreeMap<>(unsortedMap);
        return treeMap.values().toArray(new ImageMask[0]);
    }

    private static ImageMask[] createMaskDefinition(WebPlotRequest r) {
        List<String> maskColors= r.getMaskColors();
        Color[] cAry= new Color[maskColors.size()];
        List<ImageMask> masksList=  new ArrayList<>();
        int bits= r.getMaskBits();
        int colorIdx= 0;
        for(String htmlColor : maskColors) {
            cAry[colorIdx++]= PlotServUtils.convertColorHtmlToJava(htmlColor);
        }
        colorIdx= 0;

        for(int j= 0; (j<31); j++) {
            if (((bits>>j) & 1) != 0) {
                Color c= (colorIdx<cAry.length) ? cAry[colorIdx] : Color.pink;
                colorIdx++;
                masksList.add(new ImageMask(j,c));
            }
        }
        return masksList.toArray(new ImageMask[0]);
    }

    public static ModFileWriter createBand(PlotState state,
                                           ImagePlot plot,
                                           WebPlotReader.FileReadInfo readInfo,
                                           ActiveFitsReadGroup frGroup)
                                                                throws FitsException,
                                                                       IOException,
                                                                       GeomException {
        ModFileWriter retval= null;
        Band band= readInfo.band();


        FitsCacher.clearCachedHDU(readInfo.originalFile());
        plot.setThreeColorBand(readInfo.fitsRead(), readInfo.band(),frGroup);
        FitsRead tmpFR= frGroup.getFitsRead(band);
        if (tmpFR!=readInfo.fitsRead() && readInfo.workingFile()!=null) { // testing to see it the fits read got geomed when the band was added
            state.setImageIdx(0, band);
            retval = new ModFileWriter.GeomFileWriter(readInfo.workingFile(),0,tmpFR,readInfo.band(),false);
            FitsCacher.addFitsReadToCache(retval.getTargetFile(), new FitsRead[]{tmpFR});
        }

        stretchBand(band,state, plot,frGroup);
        return retval;
    }

    private static void stretchBand(Band band, PlotState state, ImagePlot plot, ActiveFitsReadGroup frGroup) {
        RangeValues rv= state.getRangeValues(band);
        if (rv==null) {
            rv= FitsRead.getDefaultFutureStretch();
            state.setRangeValues(rv, band);
        }
        plot.getImageData().recomputeStretch(frGroup.getFitsReadAry(), band.getIdx(),rv);
    }


    private static String make3ColorDataDesc(Map<Band, WebPlotReader.FileReadInfo[]> readInfoMap) {

        StringBuilder desc= new StringBuilder(100);
        desc.append("3 Color: ");
        for(Map.Entry<Band, WebPlotReader.FileReadInfo[]> entry : readInfoMap.entrySet()) {
            desc.append(readInfoMap.get(entry.getKey())[0].dataDesc());
        }
        return desc.toString();
    }

    /**
     * @param frGroup group
     * @param band which band
     * @param f file
     * @return the data to pass to the client
     */
    private static WebFitsData makeWebFitsData(ActiveFitsReadGroup frGroup, Band band, File f) {
        long fileLength= (f!=null && f.canRead()) ? f.length() : 0;
        FitsRead fr=  frGroup.getFitsReadAry()[band.getIdx()];
        if (fr==null) return null;
        double dataMin= Double.NaN;
        double dataMmax= Double.NaN;
        if (!fr.isDeferredRead()) {
            Histogram hist= fr.getHistogram();
            dataMin= hist.getDNMin() * fr.getBscale() + fr.getBzero();
            dataMmax= hist.getDNMax() * fr.getBscale() + fr.getBzero();
        }
        return new WebFitsData( dataMin, dataMmax, fileLength, fr.getFluxUnits());
    }

    public record PlotInfo(PlotState state, ImagePlot plot, FileInfo fileInfo,
                           ActiveFitsReadGroup fitsReadGroup, String dataDesc, List<RelatedData> relatedData,
                           Map<Band, WebFitsData> webFitsDataMap, Map<Band, ModFileWriter> fileWriterMap,
                           int imageDataWidth, int imageDataHeight, CoordinateSys imageCoordSys) {

        public PlotInfo(PlotState state, ImagePlot plot, FileInfo fileInfo,
                        ActiveFitsReadGroup fitsReadGroup, String dataDesc, List<RelatedData> relatedData,
                        Map<Band, WebFitsData> webFitsDataMap, Map<Band, ModFileWriter> fileWriterMap) {
            this(state,plot,fileInfo, fitsReadGroup, dataDesc, relatedData, webFitsDataMap, fileWriterMap,
                            plot.getImageDataWidth(), plot.getImageDataHeight(), plot.getCoordinatesOfPlot());

        }

    }
}

