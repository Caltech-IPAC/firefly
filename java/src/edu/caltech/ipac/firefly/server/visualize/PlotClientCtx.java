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
/**
 * User: roby
 * Date: Mar 3, 2008
 * Time: 1:32:08 PM
 */


/**
 * @author Trey Roby
 */
public class PlotClientCtx implements Serializable {

    private static final String  HOST_NAME= FileUtil.getHostname();
    private static final long VERY_SHORT_HOLD_TIME= 5*1000;
    private static final long SHORT_HOLD_TIME= 15*1000;
    private static final long LONG_HOLD_TIME= 30*1000;
    private static final AtomicLong _cnt= new AtomicLong(0);

    private final String _key;
    private volatile transient PlotImages _images= null;
    private volatile transient PlotState _state= null;
    private volatile transient ImagePlot _plot= null;
    private volatile transient long _holdTime= -1;
    private volatile transient List<PlotImages> _allImagesList= new ArrayList<PlotImages>(10);
    private final AtomicLong _lastTime= new AtomicLong(System.currentTimeMillis());
    private final List<Integer> _previousZoomList= new ArrayList<Integer>(15);

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

    public synchronized ImagePlot getPlot() { return _plot; }
    public synchronized void setPlot(ImagePlot plot) {
        _plot= plot;
        updateAccessTime();
    }

    public synchronized PlotImages getImages() { return _images; }
    public synchronized void setImages(PlotImages images) {
        _images= images;
        updateAccessTime();
        _allImagesList.add(_images);
    }

    public List<PlotImages> getAllImagesEveryCreated() {
        return _allImagesList;
    }

    public void updateAccessTime() {
        _lastTime.set(System.currentTimeMillis());
    }

    public String getKey() { return _key; }

    public synchronized void setPlotState(PlotState state) {
        _state= state;
        updateAccessTime();
    }
    public synchronized PlotState getPlotState() { return _state; }

    public void addZoomLevel(float zfact) {
        int entry= (int)(zfact*1000) ;
        if (!_previousZoomList.contains(entry)) _previousZoomList.add(entry );
    }

    public boolean containsZoom(float zfact) {
        return _previousZoomList.contains((int)(zfact*1000) );
    }

    /**
     * Resources for this context will be free. This will allow a lot of memory to be gc'd. When force is false a
     * test is performed to see how long the data has been in memory since it was used. If the use was recent then the
     * resources are not freed.
     * @param force resources will be freed no mater the access time.
     */
    public void freeResources(boolean force) {
        synchronized (this) {
            computeHoldTime();
            if (_plot!=null) {
                long idleTime= System.currentTimeMillis() - _lastTime.get();
                boolean doFree= force || (idleTime > _holdTime);
                if (doFree) {
                    Logger.debug("freeing memory for ctx: " + getKey());
                    PlotGroup group= _plot.getPlotGroup();
                    PlotView pv=(group!=null) ? group.getPlotView() : null;
                    _plot.freeResources();
                    if (group!=null) group.freeResources();
                    if (pv!=null) pv.freeResources();
                    _plot= null;
                }
            }
        }

    }

    public void extractColorInfo() {
        RangeValues rv;
        if (_plot.isThreeColor()) {
            for(int i=0; i<3; i++) {
                if (_plot.isColorBandInUse(i)) {
                    FitsRead fr= _plot.getHistogramOps(i).getFitsRead();
                    rv= fr.getRangeValues();
                    _state.setRangeValues(rv, PlotServUtils.cnvtBand(i));

                }
            }
        }
        else {
            rv= _plot.getFitsRead().getRangeValues();
            _state.setRangeValues(rv, Band.NO_BAND);
            int id= _plot.getImageData().getColorTableId();
            if (id==-1) id= 0;
            _state.setColorTableId(id);
        }
    }



//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void computeHoldTime() {
        if (_holdTime<0 && _plot!=null) {
            long length= 0;
            for(Band band : _state.getBands()) {
                File f= VisContext.getWorkingFitsFile(_state,band);
                if (f!=null) length+= f.length();
            }

            if (length < FileUtil.MEG) {
                _holdTime= VERY_SHORT_HOLD_TIME;
            }
            else if (length < (20*FileUtil.MEG) ) {
                _holdTime= SHORT_HOLD_TIME;
            }
            else {
                _holdTime= LONG_HOLD_TIME;
            }
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
