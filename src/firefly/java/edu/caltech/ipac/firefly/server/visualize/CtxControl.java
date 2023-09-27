/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import nom.tam.fits.FitsException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Trey Roby
 */
public class CtxControl {
    private static final String ctxStrRoot=
            String.format("%s-%s-%d",ServerContext.getAppName(), FileUtil.getHostname(), System.currentTimeMillis()%999);
    private static final AtomicLong cnt = new AtomicLong(0);

    public static void confirmFiles(PlotState state) throws FailedRequestException {
        if (state==null) throw new FailedRequestException("state cannot but null");
        if (isFitsFilesMissing(state)) revalidatePlot(state,true); //most of the time the files are there so nothing happens
    }

    private static boolean isFitsFilesMissing(PlotState state) {
        return state.isThreeColor() ?
                Arrays.stream(state.getBands())
                        .map( b -> !PlotStateUtil.getWorkingFitsFile(state,b).exists())
                        .reduce(false, (prev,fileMissing) -> (prev||fileMissing) ) :
                !PlotStateUtil.getWorkingFitsFile(state).exists();
    }

    public static ActiveFitsReadGroup prepare(PlotState state) throws FailedRequestException {
        if (state==null) throw new FailedRequestException("state cannot but null");
        try {
            return revalidatePlot(state, false);
        } catch (FailedRequestException e) {
            return revalidatePlot(state, true);
        }
    }

    public static void refreshCache(PlotState state) {
        if (state==null) return;
        Arrays.stream(state.getBands())
                .forEach( b -> FitsCacher.refreshCache(PlotStateUtil.getWorkingFitsFile(state, b)));
    }

    public static String makeCtxString() { return ctxStrRoot+"-"+ cnt.incrementAndGet(); }

    private static ActiveFitsReadGroup revalidatePlot(PlotState state, boolean recreate) throws FailedRequestException {
        try {
            if (recreate) {
                Counters.getInstance().incrementVis("Recreate");
                WebPlotFactory.recreate(state);
            }
            return makeActiveCallCtx(state);
        } catch (IOException|FitsException e) {
            if (recreate) Logger.getLogger().warn(e, "prepare failed on re-validate plot: " + e.getMessage());
            throw new FailedRequestException(
                    recreate ? "Could not revalidate plot after recreation" : "Could ot revalidate",e);
        }
    }

    private static ActiveFitsReadGroup makeActiveCallCtx(PlotState state) throws FitsException, IOException {
        ActiveFitsReadGroup frGroup= new ActiveFitsReadGroup();
        for(Band b : state.getBands()) {
            File fitsFile= PlotStateUtil.getWorkingFitsFile(state, b);
            int imageIdx= state.getImageIdx(b);
            if (!FitsCacher.isCached(fitsFile)) Counters.getInstance().incrementVis("FITS re-read");
            FitsRead[] fr= FitsCacher.readFits(fitsFile).getFitReadAry();  //read fits file or get from cache
            frGroup.setFitsRead(b, fr[imageIdx]);
        }
        return frGroup;
    }
}