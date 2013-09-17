package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotImages;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.visualize.plot.FitsRead;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.PlotGroup;
import edu.caltech.ipac.visualize.plot.PlotView;
import edu.caltech.ipac.visualize.plot.RangeValues;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
/**
 * User: roby
 * Date: Mar 3, 2008
 * Time: 1:32:08 PM
 */


/**
 * @author Trey Roby
 */
public class PlotClientCtx implements Serializable {

    private static final String HOST_NAME= FileUtil.getHostname();
    private static final long   VERY_SHORT_HOLD_TIME= 5*1000;
    private static final long   SHORT_HOLD_TIME= 15*1000;
    private static final long   LONG_HOLD_TIME= 30*1000;
    private static final AtomicLong _cnt= new AtomicLong(0);

    private final String _key;
//    private volatile transient ImagePlot _plot= null;
    private volatile transient long _holdTime= -1;
    private volatile transient List<PlotImages> _allImagesList= new ArrayList<PlotImages>(10);
    private volatile long _lastTime;           // this is not worth locking, an overwrite if not big deal

    private final List<Integer> _previousZoomList= new ArrayList<Integer>(15);
    private final AtomicReference<ImagePlot> _plot= new AtomicReference<ImagePlot>(null);
    private final AtomicReference<PlotImages>_images= new AtomicReference<PlotImages>(null);
    private final AtomicReference<PlotState>_state= new AtomicReference<PlotState>(null);

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public PlotClientCtx () {
        long cnt= _cnt.incrementAndGet();
        _key = "WebPlot-"+HOST_NAME+"--"+cnt;
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public ImagePlot getPlot() { return _plot.get(); }
    public void setPlot(ImagePlot p) {
        _plot.set(p);
        updateAccessTime();
        if (p!=null) initHoldTime();
    }

    public PlotImages getImages() { return _images.get(); }
    public void setImages(PlotImages images) {
        _images.set(images);
        updateAccessTime();
        _allImagesList.add(images);
    }

    public List<PlotImages> getAllImagesEveryCreated() { return _allImagesList; }

    public void updateAccessTime() { _lastTime= System.currentTimeMillis(); }

    public String getKey() { return _key; }

    public void setPlotState(PlotState state) {
        _state.set(state);
        updateAccessTime();
    }
    public PlotState getPlotState() { return _state.get(); }

    public void addZoomLevel(float zfact) {
        int entry= (int)(zfact*1000) ;
        if (!_previousZoomList.contains(entry)) _previousZoomList.add(entry );
    }

    public boolean containsZoom(float zfact) { return _previousZoomList.contains((int)(zfact*1000) ); }

    /**
     * Resources for this context will be free. This will allow a lot of memory to be gc'd. When force is false a
     * test is performed to see how long the data has been in memory since it was used. If the use was recent then the
     * resources are not freed.
     * @param force resources will be freed no mater the access time.
     */
    public void freeResources(boolean force) {
        ImagePlot p= _plot.get();
        if (p!=null) {
            long idleTime= System.currentTimeMillis() - _lastTime;
            boolean doFree= force || (idleTime > _holdTime);
            if (doFree) {
                Logger.debug("freeing memory for ctx: " + getKey());
                PlotGroup group= p.getPlotGroup();
                PlotView pv=(group!=null) ? group.getPlotView() : null;
                p.freeResources();
                if (group!=null) group.freeResources();
                if (pv!=null) pv.freeResources();
                _plot.set(null);
            }
        }
    }

    public void extractColorInfo() {
        RangeValues rv;
        ImagePlot p= _plot.get();
        PlotState state= _state.get();
        if (p!=null) {
            if (p.isThreeColor()) {
                for(int i=0; i<3; i++) {
                    if (p.isColorBandInUse(i)) {
                        FitsRead fr= p.getHistogramOps(i).getFitsRead();
                        rv= fr.getRangeValues();
                        state.setRangeValues(rv, PlotServUtils.cnvtBand(i));

                    }
                }
            }
            else {
                rv= p.getFitsRead().getRangeValues();
                state.setRangeValues(rv, Band.NO_BAND);
                int id= p.getImageData().getColorTableId();
                if (id==-1) id= 0;
                state.setColorTableId(id);
            }
        }
    }



//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void initHoldTime() {  // this should only happen one time after a valid plot
        if (_holdTime<0) {
            long length= 0;
            long ht;
            PlotState state= _state.get();
            for(Band band : state.getBands()) {
                File f= VisContext.getWorkingFitsFile(state,band);
                if (f!=null) length+= f.length();
            }

            if (length < FileUtil.MEG)            ht= VERY_SHORT_HOLD_TIME;
            else if (length < (20*FileUtil.MEG) ) ht= SHORT_HOLD_TIME;
            else                                  ht= LONG_HOLD_TIME;

            _holdTime= ht;
        }
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
