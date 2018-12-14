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

    /**
     * returns a new capacity base on the given parameters
     */
    default int newCapacity(int minCapacity, int oldCapacity) {
        // overflow-conscious code
        int newCapacity = oldCapacity + (oldCapacity >> 1);
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        return newCapacity;

    }

    public static class Objects implements PrimitiveList {
        private ArrayList<Object> data = new ArrayList<>(100);

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
            if (data == null) {
                data  = new double[100];
            }
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
            if (data == null) {
                data = new float[100];
            }
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
            if (data == null) {
                data = new long[100];
            }
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

        public Class getDataClass() {
            return Integer.class;
        }

        public Object get(int idx) {
            return data[idx] == Integer.MIN_VALUE ? null : data[idx];
        }

        public void set(int idx, Object val) {
            checkType(val);
            ensureCapacity(idx);
            data[idx] = val == null ? Integer.MIN_VALUE : (int) val;
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
            if (data == null) {
                data = new int[100];
            }
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
            if (data == null) {
                data = new boolean[100];
            }
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


