/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.visualize.PlotImages;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.PlotContainer;
import edu.caltech.ipac.visualize.plot.PlotGroup;

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

    public enum Free {ALWAYS, OLD}  // each mode includes any older mode
    private static final String HOST_NAME= FileUtil.getHostname();
//    private static final long   VERY_SHORT_HOLD_TIME= 5*1000;
//    private static final long   SHORT_HOLD_TIME= 10*1000;
//    private static final long   LONG_HOLD_TIME= 15*1000;
    private static final AtomicLong _cnt= new AtomicLong(0);

    private volatile transient List<PlotImages> _allImagesList= new ArrayList<>(10);
    private volatile long _lastTime;           // this is not worth locking, an overwrite if not big deal

    private final String _key;
    private final AtomicReference<PlotImages>_images= new AtomicReference<>(null);
    private final AtomicReference<PlotState>_state= new AtomicReference<>(null);

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public PlotClientCtx () {
        _key = "WebPlot-"+ ServerContext.getAppName() +"--" +HOST_NAME+"--"+_cnt.incrementAndGet();
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public ImagePlot getCachedPlot() {
        Cache memCache= CacheManager.getCache(Cache.TYPE_VIS_SHARED_MEM);
        return (ImagePlot)memCache.get(new StringKey(_key));
    }


    public void setPlot(ImagePlot p) {
        Cache memCache= CacheManager.getCache(Cache.TYPE_VIS_SHARED_MEM);
        memCache.put(new StringKey(_key),p);
        updateAccessTime();
    }

    public PlotImages getImages() { return _images.get(); }
    public void setImages(PlotImages images) {
        _images.getAndSet(images);
        updateAccessTime();
        _allImagesList.add(images);
    }

    public void updateAccessTime() { _lastTime= System.currentTimeMillis(); }

    public String getKey() { return _key; }

    public void setPlotState(PlotState state) {
        _state.getAndSet(state);
        updateAccessTime();
    }
    public PlotState getPlotState() { return _state.get(); }

    public void deleteCtx() {
        freeResources(Free.ALWAYS);
        File delFile;
        try {
            for(PlotImages images : _allImagesList) {
                for(PlotImages.ImageURL image : images) {
                    delFile=  ServerContext.convertToFile(image.getURL());
                    delFile.delete(); // if the file does not exist, I don't care
                }
                String thumbUrl= images.getThumbnail()!=null ? images.getThumbnail().getURL(): null;
                if (thumbUrl!=null) {
                    delFile=  ServerContext.convertToFile(images.getThumbnail().getURL());
                    delFile.delete(); // if the file does not exist, I don't care
                }
            }
        } catch (ConcurrentModificationException e) {
            // just abort, we can get it next time
        }

        _allImagesList= null;
        _images.getAndSet(null);
        _state.getAndSet(null);
    }

    public boolean freeResources(Free freeType) {
        ImagePlot p= getCachedPlot();
        if (p==null) return true;
        boolean doFree;
        long holdTime;
        long idleTime= System.currentTimeMillis() - _lastTime;
        switch (freeType) {
            case ALWAYS:
                doFree = true;
                break;
            case OLD:
                holdTime = 30 * 60 * 1000; // 30 min
                doFree= (idleTime > holdTime);
                break;
            default:
                doFree= true;
                break;
        }
        if (doFree) {
            //Logger.debug("freeing memory for ctx: " + getKey());
            PlotGroup group= p.getPlotGroup();
            PlotContainer pv=(group!=null) ? group.getPlotView() : null;
            p.freeResources();
            if (group!=null) group.freeResources();
            if (pv!=null) pv.freeResources();
            Cache memCache= CacheManager.getCache(Cache.TYPE_VIS_SHARED_MEM);
            memCache.put(new StringKey(_key), null);
        }
        return doFree;
    }
}

