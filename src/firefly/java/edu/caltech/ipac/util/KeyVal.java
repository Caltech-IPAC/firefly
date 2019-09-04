/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.util;

import java.io.Serializable;

/**
 * Date: 2019-08-28
 *
 * @author loi
 * @version $Id: $
 */
public class KeyVal<K,V> implements Serializable {

    private K key;
    private V value;


    /**
     * Creates a new KeyVal
     * @param key The key for this KeyVal
     * @param value The value to use for this KeyVal
     */
    public KeyVal(K key, V value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Gets the key for this KeyVal.
     * @return key for this KeyVal
     */
    public K getKey() { return key; }


    /**
     * Gets the value for this KeyVal.
     * @return value for this KeyVal
     */
    public V getValue() { return value; }

    /**
     *  @return <code>String</code> representation of this <code>KeyVal</code>
     */
    @Override
    public String toString() {
        return key + "=" + value;
    }
}
