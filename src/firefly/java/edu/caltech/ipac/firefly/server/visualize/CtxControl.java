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
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Trey Roby
 */
public class CtxControl {
    private static final int root= (int)(System.currentTimeMillis() % 1000);
    private static final Counters counters= Counters.getInstance();
    private static final AtomicLong cnt = new AtomicLong(0);

    public static ActiveCallCtx prepare(PlotState state) throws FailedRequestException {
        if (state==null) throw new FailedRequestException("state cannot but null");
        try {
            return revalidatePlot(state, false);
        } catch (FailedRequestException e) {
            return revalidatePlot(state, true);
        }
    }

    private static ActiveCallCtx revalidatePlot(PlotState state, boolean recreate) throws FailedRequestException {
        try {
            if (recreate) {
                counters.incrementVis("Recreate");
                WebPlotFactory.recreate(state);
            }
            return makeActiveCallCtx(state);
        } catch (IOException|FitsException e) {
            if (recreate) Logger.getLogger().warn(e, "prepare failed on re-validate plot: " + e.getMessage());;
            throw new FailedRequestException(
                    recreate ? "Could not revalidate plot after recreation" : "Could ot revalidate",e);
        }
    }

    private static ActiveCallCtx makeActiveCallCtx(PlotState state) throws FitsException, IOException {
        ActiveFitsReadGroup frGroup= new ActiveFitsReadGroup();
        for(Band band : state.getBands()) {
            File fitsFile=   PlotStateUtil.getWorkingFitsFile(state, band);
            int imageIdx= state.getImageIdx(band);
            if (!FitsCacher.isCached(fitsFile)) counters.incrementVis("FITS re-read");
            FitsRead[] fr= FitsCacher.readFits(fitsFile).getFitReadAry();  //this call should get data from cache if it exist
            frGroup.setFitsRead(band, fr[imageIdx]);
        }
        return new ActiveCallCtx(frGroup);
    }

    public record ActiveCallCtx(ActiveFitsReadGroup fitsReadGroup) { }
    public static String makeCtxString() {return ServerContext.getAppName() +"-" +FileUtil.getHostname()+"-"+root+"-"+ cnt.incrementAndGet();}
}
