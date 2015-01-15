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

    public void setSource(T source) {
        this.source = source;
    }

    public T getSource() {
        return source;
    }

    public boolean hasSource() {
        return source != null;
    }
}
