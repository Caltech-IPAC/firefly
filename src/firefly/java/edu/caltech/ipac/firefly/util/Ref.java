/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.util;

/**
 * A convenience class to do lazy-assigned on an object declared as final.
 * Date: Aug 11, 2010
 * @author loi
 * @version $Id: Ref.java,v 1.1 2010/08/12 19:04:09 loi Exp $
 */
public class Ref<T> {
    private T source;

    public Ref() {}

    public Ref(T source) {
        this.source = source;
    }

    public void set(T source) {
        this.source = source;
    }

    public T get() {
        return source;
    }

    public boolean has() {
        return source != null;
    }

    public String toString() {
        return source == null ? super.toString() : source.toString();
    }

    public boolean equals(Object obj) {
        return source == null ? super.equals(obj) : source.equals(obj);
    }

    public int hashCode() {
        return source == null ? super.hashCode() : source.hashCode();
    }
}
