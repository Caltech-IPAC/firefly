/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.table;

import java.util.ArrayList;
import java.util.Arrays;

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

    default int getIntValue(Object val) {
        if (val instanceof Integer) return (int)val;
        else if (val instanceof Byte) return (int)(Byte)val;
        else if (val instanceof Short) return (int)(Short)val;
        throw new RuntimeException(String.format("%s is with type %s, can not be assigned to Integer", val, getDataClass()));
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

    public static class Doubles implements PrimitiveList {
        private double[] data;
        private int size;

        public Doubles() { this(1000); }

        public Doubles(int initCapacity) {
            data  = new double[initCapacity];
        }

        public Class getDataClass() {
            return Double.class;
        }

        public Object get(int idx) {
            return Double.isNaN(data[idx]) ? null : data[idx];
        }

        public void set(int idx, Object val) {
            checkType(val);
            ensureCapacity(idx);
            data[idx] = val == null ? Double.NaN : (double) val;
            if (idx >= size()) size = idx+1;
        }

        public int size() {
            return size;
        }

        public void clear() {
            size = 0;
            data = null;
        }

        /**
         * ensure data have the given minimum capacity
         * @param minCapacity
         */
        private void ensureCapacity(int minCapacity) {
            if (minCapacity >= data.length) {
                data = Arrays.copyOf(data, newCapacity(minCapacity, data.length));
            }
        }

        public void trimToSize() {
            if (size < data.length) {
                data = (size == 0) ? null : Arrays.copyOf(data, size);
            }
        }
    }

    public static class Floats implements PrimitiveList {
        private float[] data;
        private int size;

        public Floats() { this(1000); }

        public Floats(int initCapacity) {
            data  = new float[initCapacity];
        }

        public Class getDataClass() {
            return Float.class;
        }

        public Object get(int idx) {
            return Float.isNaN(data[idx]) ? null : data[idx];
        }

        public void set(int idx, Object val) {
            checkType(val);
            ensureCapacity(idx);
            data[idx] = val == null ? Float.NaN : (float) val;
            if (idx >= size()) size = idx+1;
        }

        public int size() {
            return size;
        }

        public void clear() {
            size = 0;
            data = null;
        }

        private void ensureCapacity(int minCapacity) {
            if (minCapacity >= data.length) {
                data = Arrays.copyOf(data, newCapacity(minCapacity, data.length));
            }
        }

        public void trimToSize() {
            if (size < data.length) {
                data = (size == 0) ? null : Arrays.copyOf(data, size);
            }
        }
    }

    public static class Longs implements PrimitiveList {
        private long[] data;
        private int size;

        public Longs() { this(1000); }

        public Longs(int initCapacity) {
            data  = new long[initCapacity];
        }

        public Class getDataClass() {
            return Long.class;
        }

        public Object get(int idx) {
            return data[idx] == Long.MIN_VALUE ? null : data[idx];
        }

        public void set(int idx, Object val) {
            checkType(val);
            ensureCapacity(idx);
            data[idx] = val == null ? Long.MIN_VALUE : (long) val;
            if (idx >= size()) size = idx+1;
        }

        public int size() {
            return size;
        }

        public void clear() {
            size = 0;
            data = null;
        }

        private void ensureCapacity(int minCapacity) {
            if (minCapacity >= data.length) {
                data = Arrays.copyOf(data, newCapacity(minCapacity,data.length));
            }
        }

        public void trimToSize() {
            if (size < data.length) {
                data = (size == 0) ? null : Arrays.copyOf(data, size);
            }
        }
    }

    public static class Integers implements PrimitiveList {
        private int[] data = null;
        private int size;

        public Integers() { this(1000); }

        public Integers(int initCapacity) {
            data  = new int[initCapacity];
        }

        public Class getDataClass() {
            return Integer.class;
        }

        public Object get(int idx) {
            return data[idx] == Integer.MIN_VALUE ? null : data[idx];
        }

        public void set(int idx, Object val) {
            int ival = val == null ? Integer.MIN_VALUE : getIntValue(val);
            ensureCapacity(idx);
            data[idx] = ival;
            if (idx >= size()) size = idx+1;
        }

        public int size() {
            return size;
        }

        public void clear() {
            size = 0;
            data = null;
        }

        private void ensureCapacity(int minCapacity) {
            if (minCapacity >= data.length) {
                data = Arrays.copyOf(data, newCapacity(minCapacity, data.length));
            }
        }

        public void trimToSize() {
            if (size < data.length) {
                data = (size == 0) ? null : Arrays.copyOf(data, size);
            }
        }
    }

    public static class Booleans implements PrimitiveList {
        private boolean[] data = null;
        private int size;

        public Booleans() { this(1000); }

        public Booleans(int initCapacity) {
            data  = new boolean[initCapacity];
        }


        public Class getDataClass() {
            return Boolean.class;
        }

        public Object get(int idx) {
            return data[idx];
        }

        public void set(int idx, Object val) {
            checkType(val);
            ensureCapacity(idx);
            data[idx] = val != null && (boolean) val;
            if (idx >= size()) size = idx+1;
        }

        public int size() {
            return size;
        }

        public void clear() {
            size = 0;
            data = null;
        }

        private void ensureCapacity(int minCapacity) {
            if (minCapacity >= data.length) {
                data = Arrays.copyOf(data, newCapacity(minCapacity, data.length));
            }
        }

        public void trimToSize() {
            if (size < data.length) {
                data = (size == 0) ? null : Arrays.copyOf(data, size);
            }
        }
    }

}


