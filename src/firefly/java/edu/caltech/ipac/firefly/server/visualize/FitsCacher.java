/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.visualize.fitseval.FitsDataEval;
import edu.caltech.ipac.firefly.server.visualize.fitseval.FitsEvaluation;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.cache.ObjectSizeEngineWrapper;
import edu.caltech.ipac.firefly.server.util.Logger;
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

    private static Cache memCache= CacheManager.getCache(Cache.TYPE_VIS_SHARED_MEM);
    private static Cache fileInfoCache= CacheManager.getCache(Cache.TYPE_PERM_SMALL);
    private static final Map<CacheKey, Object> activeRequest = new ConcurrentHashMap<>(61);
    private static final Logger.LoggerImpl _log = Logger.getLogger();

    static FitsDataEval readFits(File fitsFile) throws FitsException, IOException {
        return readFits(getFileInfoFromCache(fitsFile),null, true, false);
    }



    static FitsDataEval readFits(FileInfo fitsFileInfo, WebPlotRequest req, boolean useCache, boolean clearHdu) throws FitsException {
        File fitsFile= fitsFileInfo.getFile();
        FitsDataEval fitsDataEval= null;

        if (useCache) fitsDataEval= getFromCache(fitsFileInfo);
        if (fitsDataEval!=null) return fitsDataEval; // check first with out any locking
          // if we are going to read the file then we might have multiple readers,
          // we want to lock here to give the second one a change to get it from cache
          // if the first reader reads and caches it.
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
                    long elapse = System.currentTimeMillis() - start;
                    logTime(fitsFileInfo, elapse);
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

    private static void addToCache(FileInfo fitsFileInfo, FitsDataEval fitsDataEval) {
        if (memCache != null) memCache.put(fitsFileInfo, fitsDataEval);
    }


    private static void prepareCacheSpace(FileInfo fitsFileInfo) {
        if (memCache != null) {
            memCache.put(fitsFileInfo, new ObjectSizeEngineWrapper.BluffSize(fitsFileInfo.getFile().length()));
            memCache.put(fitsFileInfo, null);
        }
    }
    private static void logTime(FileInfo fitsFileInfo, long time) {
        String timeStr = UTCTimeUtil.getHMSFromMills(time);
        File f= fitsFileInfo.getFile();
        _log.briefInfo("Read Fits: " + timeStr + ", " + FileUtil.getSizeAsString(f.length()) + ": " + f.getName());
    }


    private static FileInfo getFileInfoFromCache(File file) {
        CacheKey fileName= new StringKey(file.getAbsolutePath());
        FileInfo fi;
        if (fileInfoCache!=null && fileInfoCache.isCached(fileName)) {  // check first with out any locking
            Object o= fileInfoCache.get(fileName);
            if (o instanceof FileInfo) {
                fi= (FileInfo) o;
            }
            else {
                fi= new FileInfo(file);
                addFileInfoToCache(fi);
            }
        }
        else {
            fi= new FileInfo(file);
            addFileInfoToCache(fi);
        }
        return fi;
    }

    private static void addFileInfoToCache(FileInfo fitsFileInfo) {
        if (fileInfoCache!=null) {
            fileInfoCache.put(new StringKey(fitsFileInfo.getInternalFilename()), fitsFileInfo);
        }
    }



    private static FitsDataEval getFromCache(CacheKey key) {
        FitsDataEval fitsDataInfo= null;
        if (memCache!=null && memCache.isCached(key)) {  // check first with out any locking
            Object o= memCache.get(key);
            if (o instanceof FitsDataEval) {
                fitsDataInfo= (FitsDataEval)o;
            }
            else {
                memCache.put(key,null);
            }
        }
        return fitsDataInfo;
    }

    static FitsDataEval loadFits(Fits fits, File cachePath) throws FitsException {
        FitsDataEval fitsDataEval= FitsEvaluation.readAndEvaluate(fits, cachePath, true, null);
        if (memCache!=null) memCache.put(new FileInfo(cachePath),fitsDataEval);
        return fitsDataEval;
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



    public static void addFitsReadToCache(File fitsFile, FitsRead frAry[]) {
        addFitsReadToCache(fitsFile.getPath(), frAry);
    }

    private static void addFitsReadToCache(String fitsFilePath, FitsRead frAry[]) {
        File f= ServerContext.convertToFile(fitsFilePath,true);
        if (f!=null) {
            CacheKey key= new StringKey(fitsFilePath);
            if (memCache!=null) memCache.put(key,frAry);
        }
    }
}
