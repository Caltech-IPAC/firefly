/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.HasSizeOf;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.visualize.fitseval.FitsDataEval;
import edu.caltech.ipac.firefly.server.visualize.fitseval.FitsEvaluation;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.UTCTimeUtil;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static edu.caltech.ipac.visualize.plot.plotdata.FitsReadFactory.BAD_FORMAT_MSG;

/**
 * @author Trey Roby
 */
public class FitsCacher {

    private static final Cache memCache= CacheManager.getCache(Cache.TYPE_VIS_SHARED_MEM);
    private static final Cache fileInfoCache= CacheManager.getCache(Cache.TYPE_PERM_SMALL);
    private static final Map<CacheKey, Object> activeRequest = new ConcurrentHashMap<>(61);
    private static final Logger.LoggerImpl _log = Logger.getLogger();

    static FitsDataEval readFits(File fitsFile) throws FitsException, IOException {
        return readFits(getFileInfoFromCache(fitsFile),null, true, false);
    }

    static FitsDataEval readFits(FileInfo fitsFileInfo, WebPlotRequest req, boolean useCache, boolean clearHdu) throws FitsException, IOException {
        File fitsFile= fitsFileInfo.getFile();
        FitsDataEval fitsDataEval= null;

        if (useCache) fitsDataEval= getFromCache(fitsFileInfo);
        if (fitsDataEval!=null) return fitsDataEval; // check first without any locking
          // if we are going to read the file then we might have multiple readers,
          // we want to lock here to give the second one a chance to get it from cache
          // because the first reader will read and cache it.
        try {
            Object lockKey= activeRequest.computeIfAbsent(fitsFileInfo, k -> new Object());
            synchronized (lockKey) {
                fitsDataEval= getFromCache(fitsFileInfo);
                if (fitsDataEval!=null) return fitsDataEval;

                try {
                    prepareCacheSpace(fitsFileInfo);
                    long start = System.currentTimeMillis();
                    fitsDataEval= FitsEvaluation.readAndEvaluate(fitsFile, clearHdu, req);
                    fitsDataEval.addRelatedDataToAllImages(fitsFileInfo.getRelatedData());
                    addToCache(fitsFileInfo, fitsDataEval);
                    addFileInfoToCache(fitsFileInfo);
                    logTime(fitsFileInfo, System.currentTimeMillis() - start);
                    return fitsDataEval;
                } catch (FitsException e) {
                    File dir= fitsFile.getParentFile();
                    if ( e.getMessage().equals(BAD_FORMAT_MSG) &&
                            (dir.equals(ServerContext.getVisCacheDir()) ||  dir.equals(ServerContext.getUploadDir())) ) {    // if in cache or upload dir, rename the file
                        String newF= fitsFile.getAbsolutePath()+"--bad-file";
                        fitsFile.renameTo(new File(newF));
                        throw new FitsException("bad fits file renamed to: "+newF,e);
                    }
                    else {
                        throw e;
                    }
                }
            }

        } finally {
            activeRequest.remove(fitsFileInfo);
        }
    }

    /**
     * load the fits, it may not be on disk, so use the file name only for caching
     * fyi - this call is only from crop - todo -  can i remove this function?
     */
    static FitsDataEval loadFits(Fits fits, File cachePath) throws FitsException, IOException {
        FitsDataEval fitsDataEval= FitsEvaluation.readAndEvaluate(fits, cachePath, true, null);
        addToCache(new FileInfo(cachePath),fitsDataEval);
        return fitsDataEval;
    }

    /**
     *  add the FitsRead to the cache before the file is written, use the file name only for caching
     *  FitsRead is assumed to be an uncompressed image with no related data
     */
    public static void addFitsReadToCache(File f, FitsRead fr) {
        if (f==null) return;
        addToCache(getFileInfoFromCache(f), new FitsDataEval(new FitsRead[]{fr},null));
    }

    private static void addToCache(FileInfo fitsFileInfo, FitsDataEval fitsDataEval) {
        memCache.put(fitsFileInfo, fitsDataEval);
    }

    /**
     * use in cases when the underlying object has changed size
     */
    public static void refreshCache(File fitsFile) {
        FileInfo fitsFileInfo= getFileInfoFromCache(fitsFile);
        FitsDataEval fitsDataEval= getFromCache(fitsFileInfo);
        if (fitsDataEval==null) return;
        addToCache(fitsFileInfo,fitsDataEval);
    }

    private static void prepareCacheSpace(FileInfo fitsFileInfo) {
        memCache.put(fitsFileInfo, (HasSizeOf) () -> fitsFileInfo.getFile().length()); //force the cache to make space
        memCache.put(fitsFileInfo, null);
    }

    private static void logTime(FileInfo fitsFileInfo, long time) {
        String timeStr = UTCTimeUtil.getHMSFromMills(time);
        File f= fitsFileInfo.getFile();
        _log.info("Read Fits: " + timeStr + ", " + FileUtil.getSizeAsString(f.length()) + ": " + f.getName());
    }

    static boolean isCached(File fitsFile) {
        return getFromCache(getFileInfoFromCache(fitsFile))!=null;
    }

    private static FileInfo getFileInfoFromCache(File file) {
        CacheKey fileName= new StringKey(file.getAbsolutePath());
        if (!fileInfoCache.isCached(fileName)) return addFileInfoToCache(new FileInfo(file));
        Object o= fileInfoCache.get(fileName);
        return (o instanceof FileInfo fileInfo) ? fileInfo : addFileInfoToCache(new FileInfo(file));
    }

    private static FileInfo addFileInfoToCache(FileInfo fitsFileInfo) {
        fileInfoCache.put(new StringKey(fitsFileInfo.getInternalFilename()), fitsFileInfo);
        return fitsFileInfo;
    }

    private static FitsDataEval getFromCache(FileInfo key) {
        if (!memCache.isCached(key)) return null;
        if (memCache.get(key) instanceof FitsDataEval fitsDataInfo) {
            return fitsDataInfo;
        }
        else {
            memCache.put(key,null);
            return null;
        }
    }

    static void clearCachedHDU(File fitsFile) {
        FileInfo fi= getFileInfoFromCache(fitsFile);
        FitsDataEval fitsDataInfo= getFromCache(fi);
        if (fitsDataInfo==null) return;
        boolean needsReinsert= false;
        for (FitsRead fr : fitsDataInfo.getFitReadAry()) {
            if (fr!=null && fr.hasHdu()) {
                fr.clearHDU();
                needsReinsert= true;
            }
        }
        if (needsReinsert) memCache.put(fi, fitsDataInfo);
    }
}
