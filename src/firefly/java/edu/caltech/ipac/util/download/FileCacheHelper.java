/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.download;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.CacheManager;

import java.io.File;

import static edu.caltech.ipac.util.cache.Cache.fileInfoCheck;

/**
 * Simplify file caching, keep default cache directory, verify that the file exist
 * @author Trey Roby
 */
public class FileCacheHelper {

    private static File cacheDir = ServerContext.getVisCacheDir();
    private final static Cache<FileInfo> fileCache = CacheManager.<FileInfo>getDistributed().validateOnGet(fileInfoCheck);
    public static void setCacheDir(File dir) { cacheDir = dir; }

    public static File makeFile(String name) { return new File(getDir(),name); }
    public static File makeFile(File dir, String name) { return new File(dir!=null ? dir : getDir(),name); }

    public static File getDir() {
        if (!cacheDir.exists()) cacheDir.mkdirs();
        return cacheDir;
    }

    public static void putFileInfo(CacheKey key, FileInfo fileInfo) { fileCache.put(key,fileInfo); }

    public static FileInfo getFileInfo(CacheKey key)   {
        try {
            return fileCache.get(key);
        } catch (Exception e) {
            try {
                fileCache.remove(key); // clean out the bad entry
            } catch (Exception ignore) {}
            return null;
        }
    }
}

