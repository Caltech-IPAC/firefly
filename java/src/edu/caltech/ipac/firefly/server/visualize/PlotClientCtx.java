package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotImages;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.visualize.plot.FitsRead;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.PlotGroup;
import edu.caltech.ipac.visualize.plot.PlotView;
import edu.caltech.ipac.visualize.plot.RangeValues;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
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

    public enum Free {ALWAYS, INFANT, VERY_YOUNG, YOUNG,OLD}  // each mode includes any older mode
    private static final String HOST_NAME= FileUtil.getHostname();
    private static final long   INFANT_HOLD_TIME= 1*1000;
    private static final long   VERY_SHORT_HOLD_TIME= 5*1000;
    private static final long   SHORT_HOLD_TIME= 10*1000;
    private static final long   LONG_HOLD_TIME= 15*1000;
    private static final AtomicLong _cnt= new AtomicLong(0);

    private final String _key;
    private final CacheKey _cacheKey;
//    private volatile transient ImagePlot _plot= null;
    private volatile transient long _minimumHoldTime = -1;
    private volatile transient List<PlotImages> _allImagesList= new ArrayList<PlotImages>(10);
    private volatile long _lastTime;           // this is not worth locking, an overwrite if not big deal

    private final List<Integer> _previousZoomList= new ArrayList<Integer>(15);
//    private final AtomicReference<ImagePlot> _plot= new AtomicReference<ImagePlot>(null);
    private final AtomicReference<PlotImages>_images= new AtomicReference<PlotImages>(null);
    private final AtomicReference<PlotState>_state= new AtomicReference<PlotState>(null);

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public PlotClientCtx () {
        long cnt= _cnt.incrementAndGet();
        _key = "WebPlot-"+HOST_NAME+"--"+cnt;
        _cacheKey= new StringKey(_key);
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public ImagePlot getPlot() {
        Cache memCache= CacheManager.getSharedCache(Cache.TYPE_VIS_SHARED_MEM);
        ImagePlot p= (ImagePlot)memCache.get(_cacheKey);
        return p;
    }


    public void setPlot(ImagePlot p) {
        Cache memCache= CacheManager.getSharedCache(Cache.TYPE_VIS_SHARED_MEM);
        memCache.put(_cacheKey,p);
//        _plot.set(p);
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
    public long getAccessTime() { return _lastTime; }

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

    public void deleteCtx() {
        freeResources(Free.ALWAYS);
        File delFile;
        List<PlotImages> allImages= getAllImagesEveryCreated();
        try {
            for(PlotImages images : allImages) {
                for(PlotImages.ImageURL image : images) {
                    delFile=  VisContext.convertToFile(image.getURL());
                    delFile.delete(); // if the file does not exist, I don't care
                }
                String thumbUrl= images.getThumbnail()!=null ? images.getThumbnail().getURL(): null;
                if (thumbUrl!=null) {
                    delFile=  VisContext.convertToFile(images.getThumbnail().getURL());
                    delFile.delete(); // if the file does not exist, I don't care
                }
            }
        } catch (ConcurrentModificationException e) {
            // just abort, we can get it next time
        }

        _allImagesList= null;
        _previousZoomList.clear();
//        _plot.set(null);
        _images.set(null);
        _state.set(null);
    }





    public boolean freeResources(Free freeType) {
        ImagePlot p= getPlot();
        if (p==null) return true;
        boolean doFree= false;
        long actualHoldTime= 0;
        switch (freeType) {
            case ALWAYS:
                doFree = true;
                break;
            case VERY_YOUNG:
                actualHoldTime = VERY_SHORT_HOLD_TIME;
                break;
            case YOUNG:
                actualHoldTime = _minimumHoldTime;
                break;
            case OLD:
                actualHoldTime = 30 * 60 * 1000; // 30 min
                break;
            case INFANT:
                actualHoldTime = INFANT_HOLD_TIME;
                break;
        }
        if (p!=null) {
            long idleTime= System.currentTimeMillis() - _lastTime;
            if (!doFree)doFree= (idleTime > actualHoldTime);
            if (doFree) {
                //Logger.debug("freeing memory for ctx: " + getKey());
                PlotGroup group= p.getPlotGroup();
                PlotView pv=(group!=null) ? group.getPlotView() : null;
                p.freeResources();
                if (group!=null) group.freeResources();
                if (pv!=null) pv.freeResources();
                Cache memCache= CacheManager.getSharedCache(Cache.TYPE_VIS_SHARED_MEM);
                memCache.put(_cacheKey,null);
            }
        }
        return doFree;
    }

    public void extractColorInfo() {
        RangeValues rv;
        ImagePlot p= getPlot();
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
        if (_minimumHoldTime <0) {
            long ht;
            long mb= getDataSizeMB();
            if (mb < 2)        ht= VERY_SHORT_HOLD_TIME;
            else if (mb < 20 ) ht= SHORT_HOLD_TIME;
            else               ht= LONG_HOLD_TIME;

            _minimumHoldTime = ht;
        }
    }

    /**
     * Get the size of the data files rounded to nearest megabyte. value of 0 would have size but
     * would be small.
     *
     * @return the size of the files associated with this file rounded to nearest MB
     */
    public long getDataSizeK() {
//        if (onlyIfLoaded && _plot.get()==null) return 0;
        PlotState state= _state.get();
        long length= 0;
        for(Band band : state.getBands()) {
            File f= VisContext.getWorkingFitsFile(state,band);
            if (f!=null) length+= f.length();
        }
        return Math.round((double)length/(double)FileUtil.K);
    }


    public long getDataSizeMB() {
        return getDataSizeK()/1024;
    }
}

