package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.servlets.HiPSRetrieve;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.BandState;
import edu.caltech.ipac.firefly.visualize.DirectFileAccess;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.PixelValue;
import edu.caltech.ipac.visualize.plot.WorldPt;
import nom.tam.fits.FitsException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

/**
 * @author Trey Roby
 *
 */
public class FluxValueUtil {

    private static final List<PixelValue.Result> unavailable= asResList(PixelValue.Result.makeUnavailable());
    private static final List<PixelValue.Result> nan= asResList(new PixelValue.Result(PixelValue.Result.STATUS_NAN,"","",""));
    private static final List<PixelValue.Result> retrieving= asResList(PixelValue.Result.makeRetrieving());

    private static List<PixelValue.Result>  asResList(PixelValue.Result v) { return Collections.singletonList(v); }

    private static boolean isValidCubeMatch(DirectFileAccess dfa, int cubePlane) {
        return dfa.cube() && cubePlane >= 0 && cubePlane < dfa.cubeLength();
    }

    public static List<PixelValue.Result> getFluxHiPS(String fitsTileUrl, ImagePt pt, WorldPt wpt, int cubePlane) {
        if (HiPSRetrieve.isHiPSFileCached(fitsTileUrl)) {
            File fitsFile= HiPSRetrieve.getHipsCacheFile(URLDownload.makeURL(fitsTileUrl),null);
            try {
                List<DirectFileAccess> dfaList= FitsCacher.confirmAndGetDirectFileAccess(fitsFile);
                if (dfaList.isEmpty() || dfaList.getFirst() == null) return unavailable;
                DirectFileAccess dfa= dfaList.getFirst();
                if (dfa.cube() && !isValidCubeMatch(dfa, cubePlane)) return unavailable;
                var result= PixelValue.getPixelValue(fitsFile,pt,cubePlane,dfa);
                return asResList(result);
            } catch (IOException | FitsException e) {
                return unavailable;
            }
        }
        else {
            if (HiPSRetrieve.isDeadFitsUrl(fitsTileUrl)) {
                return HiPSRetrieve.getDeadFitsUrlCode(fitsTileUrl)==HTTP_NOT_FOUND
                        ? nan
                        : unavailable;
            }
            HiPSRetrieve.retrieveHiPSTileInBackground(fitsTileUrl);
            return retrieving;
        }
    }

    public static List<PixelValue.Result> getFlux(PlotState[] stateAry, ImagePt ipt) {
        if (stateAry == null || stateAry.length == 0) return Collections.emptyList();
        PlotState primState= stateAry[0];

        // 1. handle primary plot
        var bandStateList = Arrays.stream(primState.getBands()).map(primState::get).toList();

        try {
            CtxControl.confirmFiles(stateAry[0]);
        } catch (FailedRequestException e) {
            return Collections.nCopies(bandStateList.size(), PixelValue.Result.makeUnavailable());
        }
        var baseList= getFileFlux(bandStateList, ipt);
        if (stateAry.length==1) return baseList;

        // 2. if there are overlays - handle them
        List<PixelValue.Result> fluxList= new ArrayList<>(baseList);
        for(int i=1; (i<stateAry.length);i++) {
            fluxList.add(getFileFlux(Collections.singletonList(stateAry[i].get(Band.NO_BAND)), ipt).getFirst());
        }
        return fluxList;
    }

    private static List<PixelValue.Result> getFileFlux(List<BandState> bandStateList, ImagePt ipt) {
        return bandStateList.stream()
                .map (bandState -> {
                    File f= ServerContext.convertToFile(bandState.getWorkingFitsFileStr());
                    try {
                        var hduNum= bandState.getHduNumber();
                        var planeNum= Math.max(0,bandState.getCubePlaneNumber());
                        var dfaList= FitsCacher.confirmAndGetDirectFileAccess(f);
                        var dfa = dfaList.stream()
                                .filter(d -> d.hduNumber() == hduNum)
                                .findFirst()
                                .orElse(null);
                        return dfa!=null
                                ? PixelValue.getPixelValue(f,ipt,planeNum,dfa)
                                : PixelValue.Result.makeUnavailable();
                    } catch (IOException e) {
                        return PixelValue.Result.makeUnavailable();
                    }
                })
                .toList();
    }
}
