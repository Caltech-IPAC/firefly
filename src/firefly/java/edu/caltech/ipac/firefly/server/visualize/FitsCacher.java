/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 5/7/15
 * Time: 12:57 PM
 */


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
    private static final Map<CacheKey, Object> activeRequest = new ConcurrentHashMap<CacheKey, Object>(61);
    private static final Logger.LoggerImpl _log = Logger.getLogger();

    static FitsRead[] readFits(File fitsFile) throws FitsException, FailedRequestException, IOException {
        CacheKey key= new StringKey(fitsFile.getPath());
        FitsRead frAry[];

        if (memCache!=null && memCache.isCached(key)) {  // check first with out any locking
            frAry= (FitsRead[])memCache.get(key);
            return frAry;
        }
        else {  // if we are going to read the file then we have multiple readers,
                // we want to lock here to give the second one a change to get it from cache
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
                    if (memCache!=null && memCache.isCached(key)) {
                        frAry= (FitsRead[])memCache.get(key);
                        return frAry;
                    }
                    else {
                        Fits fits= new Fits(fitsFile.getPath());
                        try {
                            long start = System.currentTimeMillis();
                            frAry= FitsRead.createFitsReadArray(fits);
                            if (memCache!=null) memCache.put(key,frAry);
                            long elapse = System.currentTimeMillis() - start;
                            String timeStr= UTCTimeUtil.getHMSFromMills(elapse);
                            _log.briefInfo("Read Fits: "+timeStr+ ", "+
                                                   FileUtil.getSizeAsString(fitsFile.length()) +
                                                   ": "+fitsFile.getName());
                            return frAry;
                        } finally {
                            fits.getStream().close();
                        }
                    }
                }
            }finally {
                activeRequest.remove(key);
            }
        }
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

    static void addFitsReadToCache(String fitsFilePath, FitsRead frAry[]) {
        File f= ServerContext.convertToFile(fitsFilePath,true);
        if (f!=null) {
            CacheKey key= new StringKey(fitsFilePath);
            if (memCache!=null) memCache.put(key,frAry);
        }
    }
}
