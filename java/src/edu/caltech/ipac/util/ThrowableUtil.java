package edu.caltech.ipac.util;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.lang.reflect.Method;

/**
 * A class with static methods that contains throwable related routines.
 * @author S. Monkewitz
 */
@IgnoreStackEntry
public class ThrowableUtil {

    /**
     * Return a String containing a Throwable's stack trace. If
     * for whatever reason this can't be successfully done, the
     * method returns a null String.
     *
     * @param th     The Throwable for which a stack trace is needed
     * @return       A string containing the stack trace for the Throwable
     *               passed to this method
     */
    public static String getStackTraceAsString(Throwable th)
    {
        String s = null;
        PrintWriter pw = null;
        try
        {
            StringWriter sw = new StringWriter();
            pw = new PrintWriter(sw);
            th.printStackTrace(pw);
            s = sw.getBuffer().toString();
        }
        catch(Exception ex) {}
        finally
        {
            FileUtil.silentClose(pw);
        }
        return s;
    }

    public static Throwable getInitialCause(Throwable t) {
        while (t!=null && t.getCause()!=null) {
            t= t.getCause();
        }
        return t;
    }


    /**
     * Return a StackTraceElement that is the method that call the class
     * that is passed as a parameter.  
     * Search through the stack until you find the first
     * method that is not in the passed class that called it.
     *
     * @param c The class the had a method called in it
     * @return StackTraceElement  The element form the stack trace      
     *               that was the caller of the method in this class
     */
    public static StackTraceElement getWhoCalled(Class c) {
        Throwable         t     = new Throwable();
        StackTraceElement eAry[]= t.getStackTrace();
//        String            cName = c.getName();
//        String            meCName= ThrowableUtil.class.getName();
        boolean           done  = false;
        StackTraceElement retval= null;

        boolean ignore;
        Class<?> tmpClass;

        for(int i= 0; (i<eAry.length && !done); i++) {
            try {
               tmpClass= Class.forName(eAry[i].getClassName());
               ignore= tmpClass.isAnnotationPresent(IgnoreStackEntry.class);
                if (!ignore) {
                    ignore= ignoreMethod(tmpClass, eAry[i].getMethodName());
                }
            } catch (ClassNotFoundException e) {
                ignore= false;
            }

           if (!ignore) {
                 done  = true;
                 retval= eAry[i];
           }
        }
        return retval;
    }


    /**
     * Return a StackTraceElement[] that is the method that call the class
     * that is passed as a parameter.  
     * Search through the stack until you find the first
     * method that is not in the passed class that called it.
     *
     * @param c The class the had a method called in it
     * @return StackTraceElement  The element form the stack trace      
     *               that was the caller of the method in this class
     */
    public static StackTraceElement[] getStackTraceForWhoCalled(Class c) {
        Throwable         t         = new Throwable();
        StackTraceElement eAry[]    = t.getStackTrace();
        //String            cName     = c.getName();
        //String            meCName   = ThrowableUtil.class.getName();
        boolean           done      = false;
        StackTraceElement retval[]  = null;
        int               startIdx  = 0;
        boolean ignore;
        Class<?> tmpClass;

        for(int i= 0; (i<eAry.length && !done); i++) {

            try {
                tmpClass= Class.forName(eAry[i].getClassName());
                ignore= tmpClass.isAnnotationPresent(IgnoreStackEntry.class);
                if (!ignore) {
                   ignore= ignoreMethod(tmpClass, eAry[i].getMethodName());
                }
            } catch (ClassNotFoundException e) {
                ignore= false;
            }
            if (!ignore) {
                 done  = true;
                 startIdx= i;
           }
        }

        if (done) {
           retval= new StackTraceElement[eAry.length - startIdx];
           for(int i= 0; (i<retval.length); i++) {
               retval[i]= eAry[i+startIdx];
           }
        }

        return retval;

    }

    private static boolean ignoreMethod(Class<?> c, String methodName) {
        Method methods[]= c.getMethods();
        boolean ignore= false;
        for(Method m: methods) {
            ignore= false;
            if (methodName.equals(m.getName())) {
                ignore= m.isAnnotationPresent(IgnoreStackEntry.class);
                break;
            }
        }
        return ignore;
    }

    public static void main(String args[]) {
        getWhoCalled(ThrowableUtil.class);
    }

}


