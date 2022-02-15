/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.download;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.CacheManager;

import java.io.File;

/**
 * This class is provides an interface for classes that use both server and client cache.  One will use one when running
 * on the server and the other when running as a client.
 * @author Trey Roby
 */
public class CacheHelper {

    private static File    _cacheDir= null;
    private final static Cache fileCache= CacheManager.getCache(Cache.TYPE_PERM_FILE);
    public static void setCacheDir(File dir) { _cacheDir= dir; }

    public static File makeFitsFile(BaseNetParams params) { return makeFile(params.getUniqueString()+ ".fits"); }
    public static File makeFile(String name) { return new File(getDir(),name); }
    public static File makeFile(File dir, String name) { return new File(dir!=null ? dir : getDir(),name); }

    public static File getDir() {
        if (!_cacheDir.exists()) _cacheDir.mkdirs();
        return _cacheDir;
    }

    public static void putFileInfo(CacheKey key, FileInfo fileInfo) { fileCache.put(key,fileInfo); }

    public static FileInfo getFileInfo(CacheKey key)   {
        try {
            Object cacheObj= fileCache.get(key);
            return (cacheObj instanceof FileInfo) ? (FileInfo)cacheObj : null;
        } catch (Exception e) {
            try {
                fileCache.put(key,null); // clean out the bad entry
            } catch (Exception ignore) {}
            return null;
        }
    }
}

