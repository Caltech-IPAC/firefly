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
import edu.caltech.ipac.visualize.plot.Histogram;
import edu.caltech.ipac.visualize.plot.RangeValues;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.plotdata.GeomException;
import nom.tam.fits.FitsException;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.firefly.server.visualize.ProgressStat.PType;
import static edu.caltech.ipac.firefly.visualize.Band.NO_BAND;
import static java.util.Collections.emptyMap;

public class ImagePlotCreator {

    private static final Logger.LoggerImpl _log= Logger.getLogger();

    static PlotInfo[] makeAllNoBand(PlotState[] stateAry, WebPlotReader.FileReadInfo[] readAry) {
        var len= readAry.length;
        var piAry= new PlotInfo[len];
        for(int i= 0; (i<readAry.length); i++)  {
            var notify= len<5 || i % ((len/10)+1)==0;
            if (notify) doNotify(stateAry[i].getWebPlotRequest(),i,len);
            piAry[i]= makeNoBand(stateAry[i],readAry[i]);
        }
        return piAry;
     }

    static PlotInfo[] makeAllNoBandByState(PlotState [] stateAry, WebPlotReader.FileReadInfo[] readInfoAry) {
         var plotInfo = new PlotInfo[stateAry.length];
         for (int i = 0; i < stateAry.length; i++) {
             plotInfo[i] = makeNoBandFromIdx(stateAry[i], readInfoAry);
         }
         return plotInfo;
     }

     static PlotInfo makeNoBandFromIdx(PlotState state, WebPlotReader.FileReadInfo[] readInfoAry) {
         return makeNoBand(state,readInfoAry[state.getImageIdx(NO_BAND)]);
     }

     static PlotInfo makeNoBand(PlotState state, WebPlotReader.FileReadInfo readInfo) {
         ActiveFitsReadGroup frGroup= new ActiveFitsReadGroup();
         frGroup.setFitsRead(readInfo.band(),readInfo.fitsRead());
         confirmRVinState(state);
         WebFitsData wfData= makeWebFitsData(frGroup, readInfo.band(),readInfo.originalFile());
         return PlotInfo.makeStandard(state,readInfo, frGroup, wfData);
     }

     private static void doNotify(WebPlotRequest req, int cnt, int totLength) {
         if (totLength > 3) {
             PlotServUtils.updateProgress(req, PType.CREATING,
                     PlotServUtils.CREATING_MSG + ": " + (cnt + 1) + " of " + totLength);
         } else {
             PlotServUtils.updateProgress(req, PType.CREATING, PlotServUtils.CREATING_MSG);
         }
     }

    static PlotInfo makeOneImagePerBand3Color(PlotState state,
                                              Map<Band, WebPlotReader.FileReadInfo[]> readInfoMap)
            throws FailedRequestException, FitsException, GeomException, IOException {

        boolean first= true;
        Map<Band,WebFitsData> wfDataMap= new LinkedHashMap<>();
        Map<Band,ModFileWriter> fileWriterMap= new LinkedHashMap<>();
        ActiveFitsReadGroup frGroup= new ActiveFitsReadGroup();
        List<RelatedData> relatedData= null;
        FileInfo fileInfo= null;

        for(var entry : readInfoMap.entrySet()) {
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
                confirmRVinState(state);
                frGroup.setThreeColorBandIn(readInfo.fitsRead(),band);
                stretchBand(band,state);
                first= false;
                relatedData= readInfo.relatedData();
            }
            else {
                ModFileWriter mfw= createBand(state,readInfo,frGroup);
                stretchBand(band,state);
                if (mfw!=null) fileWriterMap.put(band,mfw);
            }
            WebFitsData wfData= makeWebFitsData(frGroup, readInfo.band(), readInfo.originalFile());
            wfDataMap.put(band, wfData);
        }
        String desc= make3ColorDataDesc(readInfoMap);

        if (first) _log.error("something is wrong, plot not setup correctly - no color bands specified");
        return PlotInfo.make3C(state,fileInfo, frGroup, desc, relatedData, wfDataMap, fileWriterMap);
     }

     private static void confirmRVinState(PlotState state) {
         RangeValues rv= state.getRangeValues();
         if (rv!=null) return;
         state.setRangeValues(FitsRead.getDefaultFutureStretch(),state.firstBand());
     }

    private static ModFileWriter createBand(PlotState state,
                                           WebPlotReader.FileReadInfo readInfo,
                                           ActiveFitsReadGroup frGroup)
                                                                throws FitsException, IOException, GeomException {
        ModFileWriter retval= null;
        Band band= readInfo.band();
        FitsCacher.clearCachedHDU(readInfo.originalFile());
        frGroup.setThreeColorBandIn(readInfo.fitsRead(),band);
        FitsRead tmpFR= frGroup.getFitsRead(band);
        if (tmpFR!=readInfo.fitsRead() && readInfo.workingFile()!=null) { // testing to see it the fits read got geomed when the band was added
            state.setImageIdx(0, band);
            retval = new ModFileWriter(readInfo.workingFile(),0,tmpFR,readInfo.band());
            FitsCacher.addFitsReadToCache(retval.getTargetFile(), tmpFR);
        }

        return retval;
    }

    private static void stretchBand(Band band, PlotState state) {
        RangeValues rv= state.getRangeValues(band);
        if (rv!=null) return;
        state.setRangeValues(FitsRead.getDefaultFutureStretch(), band);
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
        double dataMax= Double.NaN;
        double largeBinPercent= 0;
        if (!fr.isDeferredRead()) {
            Histogram hist= fr.getHistogram();
            dataMin= hist.getDNMin() * fr.getBscale() + fr.getBzero();
            dataMax= hist.getDNMax() * fr.getBscale() + fr.getBzero();
            largeBinPercent= hist.getLargeBinPercent();
        }
        return new WebFitsData( dataMin, dataMax, largeBinPercent, fileLength, fr.getFluxUnits());
    }

    private static Map<Band,WebFitsData> noBandMap(WebFitsData wfd) { return Collections.singletonMap(NO_BAND,wfd);}

    public record PlotInfo(PlotState state, FileInfo fileInfo,
                           ActiveFitsReadGroup fitsReadGroup, String dataDesc, List<RelatedData> relatedData,
                           Map<Band, WebFitsData> webFitsDataMap, Map<Band, ModFileWriter> fileWriterMap) {

        public static PlotInfo makeStandard(PlotState state,
                                            WebPlotReader.FileReadInfo readInfo,
                                            ActiveFitsReadGroup fitsReadGroup,
                                            WebFitsData webFitsData) {
            return new PlotInfo(state,readInfo.fileInfo(), fitsReadGroup, readInfo.dataDesc(), readInfo.relatedData(),
                    noBandMap(webFitsData), emptyMap());
        }

        public static PlotInfo make3C(PlotState state,
                                      FileInfo fileInfo,
                                      ActiveFitsReadGroup fitsReadGroup,
                                      String dataDesc,
                                      List<RelatedData> relatedData,
                                      Map<Band, WebFitsData> webFitsDataMap,
                                      Map<Band, ModFileWriter> fileWriterMap) {
            return new PlotInfo(state,fileInfo, fitsReadGroup, dataDesc, relatedData, webFitsDataMap, fileWriterMap);
        }
    }
}

