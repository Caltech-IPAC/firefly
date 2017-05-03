/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.cache;


import net.sf.ehcache.pool.Size;
import net.sf.ehcache.pool.SizeOfEngine;
import net.sf.ehcache.pool.impl.DefaultSizeOfEngine;

/**
 * @author Trey Roby
 */
public class ObjectSizeEngineWrapper extends DefaultSizeOfEngine {

    public ObjectSizeEngineWrapper() {
        super(1000, false);
    }
    public ObjectSizeEngineWrapper(int maxDepth, boolean abortWhenMaxDepthExceeded) {
        super(maxDepth, abortWhenMaxDepthExceeded);
    }

    public ObjectSizeEngineWrapper(int maxDepth, boolean abortWhenMaxDepthExceeded, boolean silent) {
        super(maxDepth, abortWhenMaxDepthExceeded, silent);
    }

    @Override
    public SizeOfEngine copyWith(int maxDepth, boolean abortWhenMaxDepthExceeded) {
//        return super.copyWith(maxDepth, abortWhenMaxDepthExceeded);
        return new ObjectSizeEngineWrapper(maxDepth, abortWhenMaxDepthExceeded);
    }

    @Override
    public Size sizeOf(Object key, Object value, Object container) {
        return (value instanceof BluffSize) ?
                 new Size(((BluffSize)value).getSize(), true) :
                 super.sizeOf(key,value,container);
    }

    static public class BluffSize {
        private long size;
        public BluffSize(long size) {this.size= size;}
        public long getSize() { return size; }
    }
}

