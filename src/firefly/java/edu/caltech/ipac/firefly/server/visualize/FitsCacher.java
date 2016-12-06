/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.UTCTimeUtil;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.plot.FitsRead;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Trey Roby
 */
public class FitsCacher {

    private static Cache memCache= CacheManager.getCache(Cache.TYPE_VIS_SHARED_MEM);
    private static final Map<CacheKey, Object> activeRequest = new ConcurrentHashMap<>(61);
    private static final Logger.LoggerImpl _log = Logger.getLogger();

    static FitsRead[] readFits(File fitsFile) throws FitsException, FailedRequestException, IOException {
        CacheKey key= new StringKey(fitsFile.getPath());
        FitsRead frAry[];

        frAry= getFromCache(key);
        if (frAry!=null) {  // check first with out any locking
            return frAry;
        }
        else {  // if we are going to read the file then we might have multiple readers,
                // we want to lock here to give the second one a change to get it from cache
                // if the first reader reads and caches it.
            try {
                Object lockKey= activeRequest.get(key);
                if (lockKey==null) {
                    lockKey= new Object();
                    activeRequest.put(key,lockKey);
                }
                else {
                    _log.briefInfo("Found lock key for:"+ key);
                }
                synchronized (lockKey) {
                    frAry= getFromCache(key);
                    if (frAry!=null) {
                        return frAry;
                    }
                    else {
                        Fits fits= null;
                        try {
                            fits= new Fits(fitsFile.getPath());
                            long start = System.currentTimeMillis();
                            frAry = FitsRead.createFitsReadArray(fits);
                            if (memCache != null) memCache.put(key, frAry);
                            long elapse = System.currentTimeMillis() - start;
                            String timeStr = UTCTimeUtil.getHMSFromMills(elapse);
                            _log.briefInfo("Read Fits: " + timeStr + ", " +
                                    FileUtil.getSizeAsString(fitsFile.length()) +
                                    ": " + fitsFile.getName());
                            return frAry;
                        } catch (FitsException e) {
                            File dir= fitsFile.getParentFile();
                            if ( dir.equals(ServerContext.getVisCacheDir()) ||      // if in cache or upload dir, rename the file
                                    dir.equals(ServerContext.getVisUploadDir()) ) {
                                String newF= fitsFile.getAbsolutePath()+"--bad-file";
                                fitsFile.renameTo(new File(newF));
                                throw new FitsException("bad fits file renamed to: "+newF,e);
                            }
                            else {
                                throw e;
                            }
                        } finally {
                            if (fits!=null) fits.getStream().close();
                        }
                    }
                }
            }finally {
                activeRequest.remove(key);
            }
        }
    }

    private static FitsRead[] getFromCache(CacheKey key) {
        FitsRead[] frAry= null;
        if (memCache!=null && memCache.isCached(key)) {  // check first with out any locking
            Object o= memCache.get(key);
            if (o instanceof FitsRead[]) {
                frAry= (FitsRead[])o;
            }
            else {
                memCache.put(key,null);
            }
        }
        return frAry;
    }

    static FitsRead[] loadFits(Fits fits, File cachePath) throws FitsException, FailedRequestException, IOException {
      CacheKey key= new StringKey(cachePath.getPath());
      try {
          FitsRead frAry[]= FitsRead.createFitsReadArray(fits);
          if (memCache!=null) memCache.put(key,frAry);
          return frAry;
      } finally {
          if (fits.getStream()!=null) fits.getStream().close();
      }
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
