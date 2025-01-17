/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.cache;

import edu.caltech.ipac.firefly.data.FileInfo;

import java.io.File;
import java.util.List;
import java.util.function.Predicate;

/**
 * Date: Jul 7, 2008
 *
 * @author loi
 * @version $Id: Cache.java,v 1.4 2009/06/23 18:57:17 loi Exp $
 */
public interface Cache<T> {

    void put(CacheKey key, T value);
    void put(CacheKey key, T value, int lifespanInSecs);
    T get(CacheKey key);
    boolean isCached(CacheKey key);
    int getSize();

    /**
     * returns a list of keys in this cache as string.
     * @return
     */
    List<String> getKeys();

    /**
     * Set a get validator for this cache.  The validator will be called before returning the value from the cache.
     * If the value failed the validation, it will be removed from the cache and null will be returned.
     * @return this cache
     */
    default Cache<T> validateOnGet(Predicate<T> validator) {return this;}

    interface Provider {
        <T> Cache<T> getCache(String type);
    }

//====================================================================
//  Predefined validators
//====================================================================
    Predicate<File> fileCheck = (f) -> f.canRead();
    Predicate<FileInfo> fileInfoCheck = (fi) -> fi.getFile() != null && fi.getFile().canRead();

}
