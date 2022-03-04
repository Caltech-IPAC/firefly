/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.util.FileUtil;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Trey Roby
 * Date: Mar 3, 2008
 */
public class PlotClientCtx implements Serializable {
    private static final String HOST_NAME= FileUtil.getHostname();
    private static final AtomicLong _cnt= new AtomicLong(0);
    private final String _key;
    private final AtomicReference<PlotState> state = new AtomicReference<>(null);

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public PlotClientCtx () { _key = ServerContext.getAppName() +"-" +HOST_NAME+"-"+_cnt.incrementAndGet(); }
    public String getKey() { return _key; }
    public void setPlotState(PlotState state) { this.state.getAndSet(state); }
    public PlotState getPlotState() { return state.get(); }
}
