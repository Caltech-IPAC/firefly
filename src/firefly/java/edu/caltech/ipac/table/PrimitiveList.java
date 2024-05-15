/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.table;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;

/**
 * Date: 6/15/18
 *
 * @author loi
 * @version $Id: $
 */
public interface PrimitiveList {
    Class getDataClass();
    Object get(int idx);
    void set(int idx, Object val);
    int size();
    void clear();
    void trimToSize();

    default void add(Object val) {
        set(size(), val);
    }

    default void checkType(Object val) {
        if (val != null && !val.getClass().isAssignableFrom(getDataClass())) {
            throw new RuntimeException(String.format("Type mismatch(%s): expecting %s but found %s", val, getDataClass(), val.getClass()));
        }
    }

    /**
     * returns a new capacity base on the given parameters
     */
    default int newCapacity(int minCapacity, int oldCapacity) {
        // overflow-conscious code
        int newCapacity = oldCapacity + Math.min(oldCapacity >> 1, 100000);     // setting a newCapacity size limit to ensure HUGE table do not require unnecessary amount of memory to load.
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        return newCapacity;

    }

    public static class Objects implements PrimitiveList {
        private ArrayList<Object> data;

        public Objects() { this(1000); }

        public Objects(int initCapacity) {
            this.data = new ArrayList<>(initCapacity);
        }

        public Class getDataClass() {
            return Object.class;
        }

        public Object get(int idx) {
            return data.get(idx);
        }

        public void set(int idx, Object val) {
            if (idx >= data.size()) {
                data.add(idx, val);
            } else {
                data.set(idx, val);
            }
        }

        public int size() {
            return data.size();
        }

        public void clear() { data.clear(); }

        public void trimToSize() { data.trimToSize(); }
    }

    abstract class NullablePrimitiveList implements PrimitiveList{
        private Class clz;
        private BitSet nulls = new BitSet();
        private int size;

        public NullablePrimitiveList(Class clz) {
            this.clz = clz;
        }

        public Class getDataClass() {
            return this.clz;
        }

        public Object get(int idx) {
            if (nulls.length() > idx && nulls.get(idx)) return null;
            return getImpl(idx);
        }

        public void set(int idx, Object val) {
            checkType(val);
            ensureCapacity(idx);
            if (val == null) {
                nulls.set(idx, true);
            } else {
                nulls.set(idx, false);
                setImpl(idx, val);
            }
            if (idx >= size()) size = idx+1;
        }

        public int size() {
            return size;
        }

        public void clear() {
            nulls.clear();
            clearImpl();
        }

        abstract public Object getImpl(int idx);
        abstract public void setImpl(int idx, @NotNull Object val);
        abstract public void clearImpl();
        abstract protected void ensureCapacity(int minCapacity);
    }

    class Doubles extends NullablePrimitiveList {
        private double[] data;

        public Doubles() { this(1000); }

        public Doubles(int initCapacity) {
            super(Double.class);
            data  = new double[initCapacity];
        }

        public Object getImpl(int idx) { return data[idx]; }
        public void setImpl(int idx, Object val) { data[idx] = (double) val; }
        public void clearImpl() { data = null; }

        /**
         * ensure data have the given minimum capacity
         * @param minCapacity
         */
        protected void ensureCapacity(int minCapacity) {
            if (minCapacity >= data.length) {
                data = Arrays.copyOf(data, newCapacity(minCapacity, data.length));
            }
        }

        public void trimToSize() {
            if (size() != data.length) {
                data = Arrays.copyOf(data, size());
            }
        }
    }

    class Floats extends NullablePrimitiveList {
        private float[] data;

        public Floats() { this(1000); }

        public Floats(int initCapacity) {
            super(Float.class);
            data  = new float[initCapacity];
        }

        public Object getImpl(int idx) { return data[idx]; }
        public void setImpl(int idx, Object val) { data[idx] = (float) val; }
        public void clearImpl() { data = null; }

        protected void ensureCapacity(int minCapacity) {
            if (minCapacity >= data.length) {
                data = Arrays.copyOf(data, newCapacity(minCapacity, data.length));
            }
        }

        public void trimToSize() {
            if (size() != data.length) {
                data = Arrays.copyOf(data, size());
            }
        }
    }

    class Longs extends NullablePrimitiveList {
        private long[] data;

        public Longs() { this(1000); }

        public Longs(int initCapacity) {
            super(Long.class);
            data  = new long[initCapacity];
        }

        public Object getImpl(int idx) {
            return data[idx];
        }
        public void setImpl(int idx, Object val) { data[idx] = (long) val; }
        public void clearImpl() { data = null; }

        protected void ensureCapacity(int minCapacity) {
            if (minCapacity >= data.length) {
                data = Arrays.copyOf(data, newCapacity(minCapacity,data.length));
            }
        }

        public void trimToSize() {
            if (size() != data.length) {
                data = Arrays.copyOf(data, size());
            }
        }
    }

    class Integers extends NullablePrimitiveList {
        private int[] data = null;

        public Integers() { this(1000); }

        public Integers(int initCapacity) {
            super(Integer.class);
            data  = new int[initCapacity];
        }

        public Object getImpl(int idx) {
            return data[idx];
        }
        public void setImpl(int idx, Object val) { data[idx] = (int)val; }
        public void clearImpl() { data = null; }

        protected void ensureCapacity(int minCapacity) {
            if (minCapacity >= data.length) {
                data = Arrays.copyOf(data, newCapacity(minCapacity, data.length));
            }
        }

        public void trimToSize() {
            if (size() != data.length) {
                data = Arrays.copyOf(data, size());
            }
        }
    }

    class Shorts extends NullablePrimitiveList {
        private short[] data = null;

        public Shorts() { this(1000); }

        public Shorts(int initCapacity) {
            super(Short.class);
            data  = new short[initCapacity];
        }

        public Object getImpl(int idx) {
            return data[idx];
        }
        public void setImpl(int idx, Object val) { data[idx] = (short)val; }
        public void clearImpl() { data = null; }

        protected void ensureCapacity(int minCapacity) {
            if (minCapacity >= data.length) {
                data = Arrays.copyOf(data, newCapacity(minCapacity, data.length));
            }
        }

        public void trimToSize() {
            if (size() != data.length) {
                data = Arrays.copyOf(data, size());
            }
        }
    }

    class Bytes extends NullablePrimitiveList {
        private byte[] data = null;

        public Bytes() { this(1000); }

        public Bytes(int initCapacity) {
            super(Byte.class);
            data  = new byte[initCapacity];
        }

        public Object getImpl(int idx) {
            return data[idx];
        }
        public void setImpl(int idx, Object val) { data[idx] = (byte)val; }
        public void clearImpl() { data = null; }

        protected void ensureCapacity(int minCapacity) {
            if (minCapacity >= data.length) {
                data = Arrays.copyOf(data, newCapacity(minCapacity, data.length));
            }
        }

        public void trimToSize() {
            if (size() != data.length) {
                data = Arrays.copyOf(data, size());
            }
        }
    }

    class Booleans extends NullablePrimitiveList {
        private boolean[] data = null;

        public Booleans() { this(1000); }

        public Booleans(int initCapacity) {
            super(Boolean.class);
            data  = new boolean[initCapacity];
        }

        public Object getImpl(int idx) {
            return data[idx];
        }
        public void setImpl(int idx, Object val) { data[idx] = (boolean) val;}
        public void clearImpl() { data = null; }

        protected void ensureCapacity(int minCapacity) {
            if (minCapacity >= data.length) {
                data = Arrays.copyOf(data, newCapacity(minCapacity, data.length));
            }
        }

        public void trimToSize() {
            if (size() != data.length) {
                data = Arrays.copyOf(data, size());
            }
        }
    }

}


