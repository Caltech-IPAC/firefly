package edu.caltech.ipac.util;

/**
 * This class handles the equal comparision of two objects of
 * Object, String, float, double, etc.
 *
 * @author Michael Nguyen
 */

public class ComparisonUtil {
    /**
     * Compare two Objects. The object may be null.
     *
     * @param o1 the first Object
     * @param o2 the second Object
     * @return boolean
     */
    public static boolean equals(Object o1, Object o2) {
        if((o1==null) && (o2==null)) {
             return true;
        }
        else if((o1!=null) && (o2!=null)) {
            boolean v= o1.equals(o2);
            return v;
        }
        else {
            return false;
        }
    }

    /**
     * Compare two Object arrays. The object may be null.
     *
     * @param o1 the first Object
     * @param o2 the second Object
     * @return boolean
     */
    public static boolean equals(Object o1[], Object o2[]) {
        if((o1==null) && (o2==null)) {
            return true;
        }
        else if((o1!=null) && (o2!=null)) {
            boolean retval=(o1.length==o2.length);
            for(int i=0; (retval && i<o1.length); i++) {
                retval=equals(o1[i], o2[i]);
            }
            return retval;
        }
        else {
            return false;
        }
    }

    /**
     * Compare two String objects. The object may be null.
     *
     * @param s1 the first String object
     * @param s2 the second String object
     * @return boolean
     */
    public static boolean equals(String s1, String s2) {
        if((s1==null) && (s2==null))
            return true;
        else if((s1!=null) && (s2!=null))
            return s1.equals(s2);
        else
            return false;
    }

    /**
     * Compare two float objects. The object may be NULL_FLOAT
     *
     * @param f1 the first float object
     * @param f2 the second float object
     * @return boolean
     */
    public static boolean equals(float f1, float f2) {
        if(Float.isNaN(f1) && Float.isNaN(f2))
            return true;
        if(!Float.isNaN(f1) && !Float.isNaN(f2))
            return (f1==f2);
        else
            return false;
    }
    /**
     * Compare two float objects with a precision. The object may be NULL_FLOAT
     *
     * @param f1 the first float object
     * @param f2 the second float object
     * @param precision number of decimal places to compare to
     * @return boolean
     */
    public static boolean equals(float f1, float f2, int precision) {
        if(Float.isNaN(f1) && Float.isNaN(f2))
            return true;
        if(!Float.isNaN(f1) && !Float.isNaN(f2))
            return modValue(f1,precision)==modValue(f2,precision);
        else
            return false;
    }

    /**
     * Compare two float objects  to a epsilon. The object may be NULL_FLOAT
     *
     * @param f1 the first float object
     * @param f2 the second float object
     * @param epsilon If the difference of the two number is less than this value then they are considered equal
     * @return boolean
     */
    public static boolean equals(float f1, float f2, float epsilon) {
        if(Float.isNaN(f1) && Float.isNaN(f2)) {
            return true;
        }
        if(!Float.isNaN(f1) && !Float.isNaN(f2)) {
            double diff=f1-f2;
            return Math.abs(diff) < epsilon;
        }
        else {
            return false;
        }
    }

    /**
     * Compare two double objects. The object may be NULL_DOUBLE
     *
     * @param d1 the first double object
     * @param d2 the second double object
     * @return boolean
     */
    public static boolean equals(double d1, double d2) {
        if(Double.isNaN(d1) && Double.isNaN(d2))
            return true;
        if(!Double.isNaN(d1) && !Double.isNaN(d2))
            return (d1==d2);
        else
            return false;
    }

    /**
     * Compare two double objects with a precision. The object may be NULL_DOUBLE
     *
     * @param d1 the first double object
     * @param d2 the second double object
     * @param precision number of decimal places to compare to
     * @return boolean
     */
    public static boolean equals(double d1, double d2, int precision) {
        if(Double.isNaN(d1) && Double.isNaN(d2))
            return true;
        if(!Double.isNaN(d1) && !Double.isNaN(d2))
            return modValue(d1,precision)==modValue(d2,precision);
        else
            return false;
    }

    /**
     * Compare two double objects to a epsilon. The object may be NULL_DOUBLE
     *
     * @param d1 the first double object
     * @param d2 the second double object
     * @param epsilon If the difference of the two number is less than this value then they are considered equal
     * @return boolean
     */
    public static boolean equals(double d1, double d2, double epsilon) {
        if(Double.isNaN(d1) && Double.isNaN(d2)) {
            return true;
        }
        if(!Double.isNaN(d1) && !Double.isNaN(d2)) {
            double diff=d1-d2;
            return Math.abs(diff) < epsilon;
        }
        else {
            return false;
        }
    }


    public static int doCompare(boolean v1, boolean v2) {
        int retval=0;
        if(!v1 && v2)      retval=-1;
        else if(v1 && !v2) retval=1;
        return retval;
    }

    public static int doCompare(float v1, float v2) {
        int retval=0;
        if(v1<v2)      retval=-1;
        else if(v1>v2) retval=1;
        return retval;
    }

    public static int doCompare(float v1, float v2, int precision) {
        return doCompare(modValue(v1,precision),modValue(v2,precision));
    }

    public static int doCompare(double v1, double v2) {
        int retval=0;
        if(v1<v2)      retval=-1;
        else if(v1>v2) retval=1;
        return retval;
    }

    public static int doCompare(double v1, double v2, int precision) {
        return doCompare(modValue(v1,precision),modValue(v2,precision));
    }


    public static int doCompare(int v1, int v2) {
        int retval=0;
        if(v1<v2)      retval=-1;
        else if(v1>v2) retval=1;
        return retval;
    }

    public static int doCompare(long v1, long v2) {
        int retval=0;
        if(v1<v2)      retval=-1;
        else if(v1>v2) retval=1;
        return retval;
    }

    public static int doCompare(String s1, String s2) {
        int retval;
        if (s1==null && s2==null) retval=0;
        else if(s1==null)         retval=-1;
        else if(s2==null)         retval=1;
        else                      retval= s1.compareTo(s2);
        return retval;
    }

    public static int doCompare(Number n1, Number n2) {
        int retval;

        if((n1==null) && (n2==null)) {
            retval= 0;
        }
        else if(n1==null) {
            retval=-1;
        }
        else if(n2==null) {
            retval=1;
        }
        else {
            if      (n1 instanceof Double)
                retval= doCompare(n1.doubleValue(), n2.doubleValue() );
            else if (n1 instanceof Float)
                retval= doCompare(n1.floatValue(),  n2.floatValue() );
            else if (n1 instanceof Long)
                retval= doCompare(n1.longValue(),   n2.longValue() );
            else if (n1 instanceof Integer)
                retval= doCompare(n1.intValue(),    n2.intValue() );
            else if (n1 instanceof Byte)
                retval= doCompare(n1.byteValue(),   n2.byteValue() );
            else if (n1 instanceof Short)
                retval= doCompare(n1.shortValue(),  n2.shortValue() );
            else
                retval= doCompare(n1.doubleValue(), n2.doubleValue() );
        }
        return retval;
    }

    public static int doCompare(Number n1, Number n2, int precision) {
        int retval;

        if (n1==null || n2==null || !isFloating(n1)) {
           retval= doCompare(n1,n2);
        }
        else if (n1 instanceof Double) {
            retval= doCompare(n1.doubleValue(), n2.doubleValue(), precision);
        }
        else if (n1 instanceof Float) {
            retval= doCompare(n1.floatValue(), n2.floatValue(), precision);
        }
        else {
            retval= doCompare(n1,n2);
        }
        return retval;
    }

    private static long modValue(double v, int precision) {
        return precision<=0 ? (long)v : (long)(v *Math.pow(10,precision));
    }

    private static long modValue(float v, int precision) {
        return precision<=0 ? (long)v : (long)(v *Math.pow(10,precision));
    }

    private static boolean isFloating(Number n) {
        return (n instanceof Double || n instanceof Float);
    }

}
