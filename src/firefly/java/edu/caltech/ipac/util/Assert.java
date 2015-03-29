/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util;

import java.util.ArrayList;
import java.util.List;

/**
 * A class the implements an assert mechanism similar to C.  This class
 * contains all static methods which implements various forms to do an assert.
 * An assert is a programmer level sanity check for testing code.
 * This class extents the traditional C assert a little because it also 
 * allows the user to optionally pass a string message that is printed out when
 * the assert fails. <br>
 * Assert has a property associated with it that will control if the assert 
 * does a System.exit at the time of the assert.  The property
 * <code>Assert.exitOnFail</code> should be set to false if you <b>do not</b>
 * want your problem to cause an exit when an Assert happens.  
 * The best way to do this is when you code is in production mode you do a 
 * <code>java -DAssert.exitOnFail=false ...</code> <br>
 * If the SOS_ROOT property is defined then Assert goes into server mode
 * and the rules change a little... to whatever Joe wants ....
 * <p> some examples-
 * <ul>
 * <li><code>Assert.tst(a==b)</code> - Fails if <code>a</code> is not equal to 
 * <code>b</code>.
 * <li><code>Assert.tst(a==b, "I failed" )</code> - 
 *          Fails if <code>a</code> is not equal to <code>b</code> and 
 *          prints out the String.
 * <li>if <code>a</code> is a reference; then <code>Assert.tst(a)</code> 
 *          - Fails if a is <code>null</code>
 * <li>if <code>a</code> is an int; then <code>Assert.tst(a)</code> 
 *          - Fails if a is <code>0</code>
* </ul>
 * @author Trey Roby
 */
@IgnoreStackEntry
public class Assert {

    private static final int ASSERT_TST = 406;
    private static final int PARAM_TST  = 407;

    private static boolean _serverMode            = false;
    private static Logger _logger            = new DefLogger();

    private static final String PAD_STR=
      "                                                                  !!";
    private static final String START_STR=
      "!!________________________________________________________________!!";
    private static final String END_STR=
      "!!~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~!!";
    private static final String ST_STR=
      "!!--------------------- Stack Trace Follows ----------------------!!";
    private static final String LINE_STR=
      "!!----------------------------------------------------------------!!";
                                     
    /** 
     * if SOS_ROOT is defined the go to server mode automaticly
     */
    static {
       final String SOS_ROOT = System.getProperty("SOS_ROOT", "");
       if(!SOS_ROOT.equals("")) {
            setServerMode(true);
       }
    }

    private static void fail(String msg) {
       boolean exitOnFailDefault= !_serverMode;

       boolean exitOnFail= AppProperties.getBooleanProperty(
                               "Assert.exitOnFail", exitOnFailDefault);


        doFail(msg, exitOnFail, ASSERT_TST);

    }

    private static void doFail(String  msg,
                               boolean exitOnFail,
                               int     typeOfTest) {
       RuntimeException t= null;


       if (typeOfTest== ASSERT_TST) {
          t= new AssertException();
       }
       else if (typeOfTest== PARAM_TST) {
          t= new IllegalArgumentException(msg);
       }
       else {
           _logger.log("Assert.doFail: this should never happen");
           System.exit(1);
       }

       StackTraceElement ste= ThrowableUtil.getWhoCalled(Assert.class);
       StackTraceElement sAry[]=
                      ThrowableUtil.getStackTraceForWhoCalled(Assert.class);
       if (!_serverMode || typeOfTest==ASSERT_TST) showMessage(msg,typeOfTest,ste);

       if (sAry!= null) t.setStackTrace(sAry);

       if(typeOfTest==ASSERT_TST) {

            _logger.log(ThrowableUtil.getStackTraceAsString(t));
            if(exitOnFail) {
                System.exit(1);
            }
            else {
                _logger.log("Execution continuing");
            }
       }
       else {
            throw t;
       }
     }


    private static void showMessage(String            msg,
                                    int               typeOfTest,
                                    StackTraceElement ste) {
        List<String> outList= new ArrayList<String>(15);
        String mType= "!";
        if (typeOfTest==ASSERT_TST) {
            mType= "!!                    Assertion failed!";
        }
        else if (typeOfTest== PARAM_TST) {
            mType= "!!                    Parameter mismatch!";
        }
        else {
            _logger.log("Assert.showMessage: this should never happen");
            System.exit(1);
        }
        String cName= ServerStringUtil.getShortClassName(ste.getClassName());
        outList.add(START_STR);
        outList.add(pad(mType) );
        outList.add(pad("!! Class:  " + cName)              );
        outList.add(pad("!! Thread: " + Thread.currentThread().getName()));
        outList.add(pad("!! Method: " + ste.getMethodName()) );
        outList.add(pad("!! File:   " + ste.getFileName()  ) );
        outList.add(pad("!! Line:   " + ste.getLineNumber()) );
        if (msg != null) outList.add(pad("!! " + msg));
        outList.add(END_STR);
        outList.add(ST_STR);
        outList.add(LINE_STR);
        _logger.log(outList.toArray(new String[outList.size()]));
    }




    /**
     * Test a boolean value. The test fails if the boolean is false.
     * @param b the boolean value
     */
    public static void tst(boolean b) { 
        if (!b) fail(null);
    }

    /**
     * Test a boolean value. The test fails if the boolean is false.
     * @param b the boolean value
     * @param msg print out the string if the test fails.
     */
    public static void tst(boolean b, String msg) { 
        if (!b) fail(msg);
    }

    /**
     * Test a long value. The test fails if the long is 0.
     * @param lng the value
     */
    public static void tst(long lng) {
        if (lng == 0L) fail(null);
    }

    /**
     * Test a long value. The test fails if the long is 0.
     * @param lng the value
     * @param msg print out the string if the test fails.
     */
    public static void tst(long lng, String msg) {
        if (lng == 0L) fail(msg);
    }

    /**
     * Test a double value. The test fails if the double is 0.0.
     * @param dbl the value
     */
    public static void tst(double dbl) {
        if (dbl == 0.0) fail(null);
    }

    /**
     * Test a double value. The test fails if the double is 0.0.
     * @param dbl the value
     * @param msg print out the string if the test fails.
     */
    public static void tst(double dbl, String msg) {
        if (dbl == 0.0) fail(msg);
    }

    /**
     * Test a reference value. The test fails if the reference is null.
     * @param ref the reference
     */
    public static void tst(Object ref) {
        if (ref == null) fail(null);
    }

    /**
     * Test a reference value. The test fails if the reference is null.
     * @param ref the reference
     * @param msg print out the string if the test fails.
     */
    public static void tst(Object ref, String msg) {
        if (ref == null) fail(msg);
    }

    /**
     * Test a boolean value. The test fails if the boolean is false.
     * Throw a IllegalArgumentException with thre source as the caller.
     * @param b the boolean value
     * @param msg print out the string if the test fails.
     */
    public static void argTst(boolean b, String msg)
                                  throws IllegalArgumentException {
        if (!b) doFail(msg, false, PARAM_TST);
    }

    /**
     * Test a reference value. The test fails if the reference is null.
     * Throw a IllegalArgumentException with thre source as the caller.
     * @param ref the reference
     * @param msg print out the string if the test fails.
     */
    public static void argTst(Object ref, String msg)
                                     throws IllegalArgumentException {
        argTst(ref!=null, msg);
    }

    /**
     * Test a long value. The test fails if the long is 0.
     * Throw a IllegalArgumentException with thre source as the caller.
     * @param lng the value
     * @param msg print out the string if the test fails.
     */
    public static void argTst(long lng, String msg)
                                   throws IllegalArgumentException {
        argTst(lng==0, msg);
    }

    /**
     * Always fails.
     * @param msg print out the string if the test fails.
     */
    public static void stop(String msg) {
        fail(msg);
    }

    /**
     * Always fails.
     */
    public static void stop() {
        fail(null);
    }

    /**
     * Call this method if you want to put the Assert class in server mode.
     * In server mode the behavior is less drastic when an assert fails.
     * The <code>Assert.exitOnFail</code> property defaults to false.
     * ... and it does whatever else joe wants...
     * @param serverMode true if server mode, false for client
     */
    public static void setServerMode(boolean serverMode) {
        _serverMode= serverMode;
//        _serverModeEnableAssert= Boolean.getBoolean("ENABLE_ASSERT");
    }

    public static void setLogger(Logger logger) {
        _logger= logger;
    }

   public static void main(String args[]) {
      Assert.tst(false);
      _logger.log("Normal Exit");
   }


   private static String pad(String s) {
      return putInto(PAD_STR, s);
   }

   private static String putInto(String baseStr, String s) {
      String retval= s;
      if (s.length() < baseStr.length()-5) {
         int len= s.length();
         StringBuffer sb= new StringBuffer(baseStr);
         for (int i= 0; (i<len); i++) {
               sb.setCharAt(i, s.charAt(i));
         }
         retval= sb.toString();
      }
      return retval;
   } 

   private static class AssertException extends RuntimeException { }


    public interface Logger {
        public void log(String... messages);
    }

    private static class DefLogger implements Logger {
        public void log(String... messages) {
            for(String s : messages) System.out.println(s);
        }
    }


}
