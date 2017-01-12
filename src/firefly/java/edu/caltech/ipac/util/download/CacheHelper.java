/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.download;

import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;

import java.io.File;
/**
 * User: roby
 * Date: Mar 3, 2009
 * Time: 2:59:08 PM
 */

/**
 * This class is provide an interface for classes that use both server and client cache.  One will use one when running
 * on the server and the other when running as a client.
 * @author Trey Roby
 */
public class CacheHelper {

    private static Cache   _fileCache= null;
    private static Cache   _objCache= null;
    private static File    _cacheDir= null;
    private static boolean _supportsLifespan = false;

    public static void setFileCache(Cache cache) { _fileCache= cache; }
    public static void setObjectCache(Cache cache) { _objCache= cache; }
    public static void setCacheDir(File dir) { _cacheDir= dir; }

    public static Cache getFileCache() {
        return _fileCache;
    }

    public static void putFile(CacheKey key, Object value) {
        getFileCache().put(key,value);
    }

    public static void putFile(CacheKey key, Object value, int lifespanInSecs) {
        if (lifespanInSecs>0 && _supportsLifespan) {
            getFileCache().put(key,value,lifespanInSecs);
        }
        else {
            getFileCache().put(key,value);
        }
    }

    public static void putObj(CacheKey key, Object value) {
        getObjectCache().put(key,value);

    }
    public static void putObj(CacheKey key, Object value, int lifespanInSecs) {
        getObjectCache().put(key,value,lifespanInSecs);
    }

    public static void setSupportsLifespan(boolean support)  { _supportsLifespan = support;}

    public static Cache getObjectCache() { return _objCache; }

    public static File makeFitsFile(BaseNetParams params) {
        return makeFile(params.getUniqueString()+ ".fits");
    }

    public static File makeFile(String name) { return new File(getDir(),name); }
    public static File makeFile(File dir, String name) { return new File(dir!=null ? dir : getDir(),name); }

    public static File getDir() { return _cacheDir; }

    public static Object getObj(CacheKey key)   {
        Cache cache= getObjectCache();
        return cache.get(key);
    }

    public static File getFile(CacheKey key)   {
        Cache cache= getFileCache();
        Object cacheObj= cache.get(key);
        File f;

        if (cacheObj==null){
            f= null;
        }
        else if (cacheObj instanceof File) {
            f= (File)cacheObj;
        }
        else if (cacheObj instanceof FileData[]){
            FileData fData[]= (FileData[])cacheObj;
            f= fData[0].getFile();
        }
        else if (cacheObj instanceof FileData){
            f= ((FileData)cacheObj).getFile();
        }
        else if (cacheObj instanceof String){
            f= new File((String)cacheObj);
        }
        else {
            f= null;
            Assert.argTst(false, "expected type file, found: " +cacheObj.getClass().getName() );
        }



        // Special case for fits files
        if (cacheObj==null && key instanceof BaseNetParams) {
            BaseNetParams params= (BaseNetParams)key;
            f= CacheHelper.makeFitsFile(params);
            if (!f.canRead() || f.length()<300) {
                f=null;
            }
        }

        return f;
    }


    public static FileData getFileData(CacheKey key)   {
        Cache cache= getFileCache();
        Object cacheObj= cache.get(key);
        FileData fd;

        if (cacheObj==null){
            fd= null;
        }
        else if (cacheObj instanceof FileData[]){
            FileData fData[]= (FileData[])cacheObj;
            fd= fData[0];
        }
        else if (cacheObj instanceof FileData){
            fd= (FileData)cacheObj;
        }
        else {
            fd= null;
            Assert.argTst(false, "expected type FileData, found: " +cacheObj.getClass().getName() );
        }
        return fd;
    }
}

