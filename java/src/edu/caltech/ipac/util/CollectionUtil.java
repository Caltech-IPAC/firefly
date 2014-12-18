package edu.caltech.ipac.util;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Utility class dealing with Collection.
 *
 * @author Loi Ly
 * @version $id:$
 */
public class CollectionUtil {


    /**
     * Same as {@link #toString(Collection,String) toString}
     * separated by ", ".
     * 
     * @param c
     * @return
     */
    public static String toString(Collection c) {
        return toString(c, ", ");
    }

    public static String toString(Object[] objs) {
        return toString(Arrays.asList(objs));
    }

//    public static String toString(Collection c, String separatedBy) {
//        if (c != null) {
//            return toString(c, separatedBy, null);
//        } else  {
//            return "";
//        }
//    }

    public static String toString(Object[] objs, String separatedBy) {
        if (objs != null) {
            return toString(Arrays.asList(objs), separatedBy);
        } else {
            return "";
        }
    }

    /**
     * Returns a String representation of this collection.
     * The items are converted to strings using String.format(frmtPattern).
     * If frmtPattern is given, the items will be formatted as
     * String.format(Object, frmtPattern), else it will use String.valueOf(Object).
     * If separatedby is given, the items will be separated by that string.
     *
     * @param c
     * @param separatedBy
     * @return
     */
    public static String toString(Collection c, String separatedBy/*, String frmtPattern*/) {

        if (c == null || c.size() == 0) return "";

//        boolean usePattern = frmtPattern != null;
        StringBuffer sb = new StringBuffer();
        for (Iterator itr = c.iterator(); itr.hasNext(); ) {
            Object o = itr.next();
//            String s = usePattern ? String.format(frmtPattern, o) : String.valueOf(o);
            String s = String.valueOf(o);
            sb.append(s);
            if (separatedBy != null && itr.hasNext()) {
                sb.append(separatedBy);
            }
        }

        return sb.toString();
    }

    public static boolean isEmpty(Collection c) {
        return c == null || c.size() == 0;
    }


//    public static String toString(Object[] objs, String separatedBy, String frmtPattern) {
//        return toString(Arrays.asList(objs), separatedBy, frmtPattern);
//    }

    /**
     * takes an int array and returns a List of Integer backed by the array
     * @param a
     * @return
     */
    public static List<Integer> asList(final int[] a) {
        return new AbstractList<Integer>() {
            public Integer get(int i) { return a[i]; }
            // Throws NullPointerException if val == null
            public Integer set(int i, Integer val) {
                Integer oldVal = a[i];
                a[i] = val;
                return oldVal;
            }
            public int size() { return a.length; }
        };
    }

    /**
     * takes a float array and returns a List of Float backed by the array
     * @param a
     * @return
     */
    public static List<Float> asList(final float[] a) {
        return new AbstractList<Float>() {
            public Float get(int i) { return a[i]; }
            // Throws NullPointerException if val == null
            public Float set(int i, Integer val) {
                Float oldVal = a[i];
                a[i] = val;
                return oldVal;
            }
            public int size() { return a.length; }
        };
    }

    public static <T> boolean exists(T[] srcAry, T compareTo) {
        if (srcAry == null || compareTo == null) return false;
        for(T s : srcAry) {
            if ( s.equals(compareTo)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Filters the source collection based on the given Filter object.
     * Any items not accepted by the Filter will be removed from the
     * source collection.
     * If a Filter object is not given, it will accept all items in
     * the source collection.
     *
     * @param src
     * @param filter
     */
    public static <T> void filter(Collection<T> src,
                                  Filter<T> filter) {
        filter(src, null, filter);
    }

    public static <T> void filter(Collection<T> src,
                                  Collection<T> dest,
                                  Filter<T>... filters) {
        List<Filter<T>> flist = filters == null || filters.length ==0 ? null : Arrays.asList(filters);
        filter(src, dest, flist);
    }

    /**
     * Filters the source collection based on the given Filter objects.
     * If destination collection is not given, this method will filter
     * on the source collection directly, leaving the source collection
     * with only items that are accepted by the Filter object.
     * If Filter objects are not given, it will accept all items in
     * the source collection.
     *
     * @param src the source collection
     * @param filters the filtering objects
     * @param dest the destination collection
     */
    public static <T> void filter(Collection<T> src,
                                  Collection<T> dest,
                                  List<Filter<T>> filters) {

        if ( src == null ) throw new NullPointerException("The source collection may not be null.");

        int cursorIdx = 0;

        for(Iterator<T> itr = src.iterator(); itr.hasNext(); ) {
            T obj = itr.next();

            if (matches(cursorIdx, obj, filters)) {
                if (dest != null) {
                    dest.add(obj);
                }
            } else {
                if (dest == null) {
                    itr.remove();
                }
            }
            cursorIdx++;
        }

    }

    public static <T> List<T> asList(T... items) {

        ArrayList<T> list = new ArrayList<T>(items.length);
        if (items != null) {
            for (T item : items) {
                list.add(item);
            }
        }
        return list;
    }

    public static <T> boolean matches(int cursorIdx, T src, List<Filter<T>> filters) {
        if ( filters !=  null ) {
            cursorIdx = cursorIdx < 0 ? 0 : cursorIdx;
            for(Filter<T> f : filters) {
                boolean accept = f.isRowIndexBased() ? f.accept(cursorIdx) : f.accept(src);
                if (!accept) {
                    return false;
                }
            }
        }
        return true;
    }


    /**
     * returns a new HashMap containing a shallow clone of the
     * original map.
     * @param origMap
     * @return
     */
    public static <K,V> HashMap<K,V> clone(Map<K,V> origMap) {

        HashMap<K,V> newMap = new HashMap<K,V>();
        if (origMap != null && origMap.size() > 0) {
            newMap.putAll(origMap);
        }
        return newMap;
    }

    public static <T> T findFirst(List<T> list, Filter<T> matcher) {
        if (!isEmpty(list)) {
            for(T item : list) {
                if (matcher.accept(item)) {
                    return item;
                }
            }
        }
        return null;
    }

//=========================================================================
//  inner classes/interfaces
//=========================================================================

    /**
     * Interface for Collection filtering.
     */
    public static interface Filter<T> {
        boolean accept(T obj);
        boolean accept(int rowId);
        boolean isRowIndexBased();
    }

    public static abstract class SimpleFilter<T> implements Filter<T> {
        public boolean accept(int rowId) {
            return false;
        }
        public boolean isRowIndexBased() {
            return false;
        }
    }

    public static abstract class FilterImpl<T> implements Filter<T> {
        int cursorIdx = -1;
        public boolean accept(int rowId) { return false; }
        public boolean isRowIndexBased() { return false; }

        public int getCursorIdx() {
            return cursorIdx;
        }

        public void setCursorIdx(int idx) {
            cursorIdx = idx;
        }
    }

    public static class Condition<T> extends FilterImpl<T> {
        public enum Operator {and, or}
        private Operator operator;
        List<Filter<T>> filters;

        public Condition(Operator operator) {
            this.operator = operator;
            this.filters = new ArrayList<Filter<T>>();
        }

        public Condition(Operator operator, Filter<T>... filters) {
            this.operator = operator;
            this.filters = Arrays.asList(filters);
        }

        public boolean accept(T obj) {
            boolean matches = true;
            if ( filters !=  null && filters.size() > 0) {
                if (filters.size() == 1) {
                    matches = filters.get(0).accept(obj);
                } else {
                    matches = !operator.equals(Operator.or);
                    for(Filter<T> f : filters) {
                        if (operator.equals(Operator.or)) {
                            if (f.accept(obj)) {
                                matches = true;
                                break;
                            }
                        } else if (operator.equals(Operator.and)) {
                            if (!f.accept(obj)) {
                                matches = false;
                                break;
                            }
                        }
                    }
                }
            }
            return matches;
        }

        public void addFilter(Filter<T> filter) {
            filters.add(filter);
        }

        public List<Filter<T>> getFilters() {
            return filters;
        }

        public Operator getOperator() {
            return operator;
        }
    }

//=========================================================================
//  Main method use for unit testing.
//=========================================================================

    public static void main(String[] args) {

        java.util.ArrayList<String> l = new java.util.ArrayList<String>(
                java.util.Arrays.asList("1", "2", "3", "4", "5", "6"));
         Filter<String> f1 = new FilterImpl<String>(){
                public boolean accept(String s) {
                    return Integer.parseInt(s) > 1;
                }
            };
        Filter<String> f2 = new FilterImpl<String>(){
                public boolean accept(String s) {
                    return Integer.parseInt(s) < 5;
                }
            };
        Filter<String> f3 = new FilterImpl<String>(){
                public boolean accept(String s) {
                    return Integer.parseInt(s) > 5;
                }
            };
        Filter<String> f4 = new FilterImpl<String>(){
                public boolean accept(String s) {
                    return Integer.parseInt(s) < 6;
                }
            };
        CollectionUtil.filter(l, new Condition<String>(Condition.Operator.or, f3,
                            new Condition<String>(Condition.Operator.and, f1, f2)));
        System.out.println(l);
    }
}

