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
import edu.caltech.ipac.firefly.visualize.DirectFileAccess;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.UTCTimeUtil;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.plotdata.FitsReadUtil;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static edu.caltech.ipac.visualize.plot.ImageHeader.AIRMASS;
import static edu.caltech.ipac.visualize.plot.ImageHeader.EXPTIME;
import static edu.caltech.ipac.visualize.plot.ImageHeader.EXTINCT;
import static edu.caltech.ipac.visualize.plot.ImageHeader.IMAGEZPT;
import static edu.caltech.ipac.visualize.plot.ImageHeader.ORIGIN;
import static edu.caltech.ipac.visualize.plot.ImageHeader.PALOMAR_ID;
import static edu.caltech.ipac.visualize.plot.plotdata.FitsReadFactory.BAD_FORMAT_MSG;
import static edu.caltech.ipac.visualize.plot.plotdata.FitsReadUtil.SPOT_BP;
import static edu.caltech.ipac.visualize.plot.plotdata.FitsReadUtil.SPOT_HS;
import static edu.caltech.ipac.visualize.plot.plotdata.FitsReadUtil.SPOT_OFF;

/**
 * @author Trey Roby
 */
public class FitsCacher {

    private static final Cache<Object> memCache= CacheManager.getVisMemCache();
    private static final Cache<Object> fileInfoCache= CacheManager.getLocal();
    private static final Map<CacheKey, Object> activeRequest = new ConcurrentHashMap<>(61);
    private static final Logger.LoggerImpl _log = Logger.getLogger();
    private static final String directFileAccessPrefix= "directFileAccess--";

    static FitsDataEval readFits(File fitsFile) throws FitsException, IOException {
        return readFits(getFileInfoFromCache(fitsFile),null, true, false);
    }

    static FitsDataEval readFits(FileInfo fitsFileInfo, WebPlotRequest req, boolean useCache, boolean clearHdu) throws FitsException, IOException {
        File fitsFile= fitsFileInfo.getFile();
        FitsDataEval fitsDataEval= null;

        if (useCache) fitsDataEval= getFromLargeMemCache(fitsFileInfo);
        if (fitsDataEval!=null) return fitsDataEval; // check first without any locking
          // if we are going to read the file then we might have multiple readers,
          // we want to lock here to give the second one a chance to get it from cache
          // because the first reader will read and cache it.
        try {
            Object lockKey= activeRequest.computeIfAbsent(fitsFileInfo, k -> new Object());
            synchronized (lockKey) {
                fitsDataEval= getFromLargeMemCache(fitsFileInfo);
                if (fitsDataEval!=null) return fitsDataEval;

                try {
                    prepareLargeMemCacheSpace(fitsFileInfo);
                    long start = System.currentTimeMillis();
                    fitsDataEval= FitsEvaluation.readAndEvaluate(fitsFile, clearHdu, req);
                    fitsDataEval.addRelatedDataToAllImages(fitsFileInfo.getRelatedData());
                    addToLargeMemCache(fitsFileInfo, fitsDataEval); // already holding lockKey, don't re-acquire/remove it
                    addFileInfoToCache(fitsFileInfo);
                    logTime(fitsFileInfo, System.currentTimeMillis() - start);
                    return fitsDataEval;
                } catch (FitsException e) {
                    File dir= fitsFile.getParentFile();
                    if ( e.getMessage().equals(BAD_FORMAT_MSG) &&
                            (dir.equals(ServerContext.getVisCacheDir()) ||  dir.equals(ServerContext.getUploadDir())) ) {    // if in cache or upload dir, rename the file
                        String newF= fitsFile.getAbsolutePath()+"--bad-file";
                        var ignore= fitsFile.renameTo(new File(newF));
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
        FitsDataEval fitsDataEval= FitsEvaluation.readAndEvaluateFits(fits, cachePath, true, null);
        addToLargeMemCacheWithLock(new FileInfo(cachePath),fitsDataEval);
        return fitsDataEval;
    }

    /**
     *  add the FitsRead to the cache before the file is written, use the file name only for caching
     *  FitsRead is assumed to be an uncompressed image with no related data
     */
    public static void addFitsReadToLargeMemCache(File f, FitsRead fr) {
        if (f==null) return;
        addToLargeMemCacheWithLock(getFileInfoFromCache(f), new FitsDataEval(new FitsRead[]{fr},null));
    }

    /**
     * Add fitsDataEval to cache
     * entry point for callers that don't already hold the per-file lock
     */
    private static void addToLargeMemCacheWithLock(FileInfo fitsFileInfo, FitsDataEval fitsDataEval) {
        Object lockKey= activeRequest.computeIfAbsent(fitsFileInfo, k -> new Object());
        try {
            synchronized (lockKey) {
                addToLargeMemCache(fitsFileInfo, fitsDataEval);
            }
        } finally {
            activeRequest.remove(fitsFileInfo);
        }
    }

    /**
     * Add fitsDataEval to cache
     * call directly only if there is already a lock acquired
     */
    private static void addToLargeMemCache(FileInfo fitsFileInfo, FitsDataEval fitsDataEval) {
        memCache.put(fitsFileInfo, fitsDataEval);
        FitsRead[] inFrAry= fitsDataEval.getFitReadAry();
        var dfaList= Arrays.stream(inFrAry)
                .filter( (fr) -> !fr.isCube() || fr.getPlaneNumber()==1)
                .map(FitsCacher::makeDirectFileAccessData).toList();
        fileInfoCache.put(makeDirectFileAccessKey(fitsFileInfo), dfaList);
    }

    private static StringKey makeDirectFileAccessKey(FileInfo fi) {
        return new StringKey(directFileAccessPrefix+fi.getUniqueString());
    }

    /**
     * use in cases when the underlying object has changed size
     */
    public static void refreshCache(File fitsFile) {
        FileInfo fitsFileInfo= getFileInfoFromCache(fitsFile);
        FitsDataEval fitsDataEval= getFromLargeMemCache(fitsFileInfo);
        if (fitsDataEval==null) return;
        addToLargeMemCacheWithLock(fitsFileInfo,fitsDataEval);
    }

    private static void prepareLargeMemCacheSpace(FileInfo fitsFileInfo) {
        memCache.put(fitsFileInfo, (HasSizeOf) () -> fitsFileInfo.getFile().length()); //force the cache to make space
        memCache.remove(fitsFileInfo);
    }

    private static void logTime(FileInfo fitsFileInfo, long time) {
        String timeStr = UTCTimeUtil.getHMSFromMills(time);
        File f= fitsFileInfo.getFile();
        _log.info("Read Fits: " + timeStr + ", " + FileUtil.getSizeAsString(f.length()) + ": " + f.getName());
    }

    static boolean isCached(File fitsFile) {
        return getFromLargeMemCache(getFileInfoFromCache(fitsFile))!=null;
    }

    public static boolean isDirectFileAccessCached(File fitsFile) {
        FileInfo fi= getFileInfoFromCache(fitsFile);
        return fileInfoCache.get(makeDirectFileAccessKey(fi))!=null;
    }

    public static List<DirectFileAccess> getDirectFileAccessFromCache(File fitsFile) {
        FileInfo fi= getFileInfoFromCache(fitsFile);
        return (List<DirectFileAccess>)fileInfoCache.get(makeDirectFileAccessKey(fi));
    }

    public static List<DirectFileAccess> confirmAndGetDirectFileAccess(File fitsFile) throws IOException {
        if (!FitsCacher.isDirectFileAccessCached(fitsFile)) {
            FitsCacher.readFits(fitsFile).getFitReadAry(); // forces the fits file to re-read and will put DirectFileAccess in cache
        }
        var retList=  FitsCacher.getDirectFileAccessFromCache(fitsFile);
        return retList!=null ?  retList : Collections.emptyList();
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

    private static FitsDataEval getFromLargeMemCache(FileInfo key) {
        if (!memCache.isCached(key)) return null;
        if (memCache.get(key) instanceof FitsDataEval fitsDataInfo) {
            return fitsDataInfo;
        }
        else {
            memCache.remove(key);
            return null;
        }
    }

    static void clearLargeMemCachedHDU(File fitsFile) {
        FileInfo fi= getFileInfoFromCache(fitsFile);
        FitsDataEval fitsDataInfo= getFromLargeMemCache(fi);
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


    private static DirectFileAccess makeDirectFileAccessData(FitsRead fr) {
        Header h= fr.getHeader();
        DirectFileAccess.PalomarDirectMod palomar= null;
        var origin= h.getStringValue(ORIGIN,"");
        if (origin.startsWith(PALOMAR_ID)) {
            var expTime= h.getDoubleValue(EXPTIME);
            var imageZPt= h.getDoubleValue(IMAGEZPT);
            var airMass= h.getDoubleValue(AIRMASS);
            var extinct= h.getDoubleValue(EXTINCT);
            palomar= new DirectFileAccess.PalomarDirectMod(expTime,imageZPt,airMass,extinct);
        }
        var dataOffset= h.getLongValue(SPOT_OFF) + h.getLongValue(SPOT_HS,0);
        var blankVal= FitsReadUtil.getBlankValue(h);
        var blankValStr= Double.isNaN(blankVal) ? "" : blankVal+"";
        return new DirectFileAccess(
                fr.getHduNumber(), fr.isCube(), FitsReadUtil.getNaxis3(h), -1, dataOffset, h.getIntValue(SPOT_BP),
                FitsReadUtil.getNaxis1(h), FitsReadUtil.getNaxis2(h), FitsReadUtil.getNaxis3(h),
                FitsReadUtil.getCdelt2(h), FitsReadUtil.getBUnit(h,"---"),
                FitsReadUtil.getBscale(h), FitsReadUtil.getBzero(h),
                blankValStr, origin, palomar);
    }

}

