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


/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
