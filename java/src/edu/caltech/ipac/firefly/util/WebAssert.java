package edu.caltech.ipac.firefly.util;

import com.google.gwt.user.client.Window;
import edu.caltech.ipac.firefly.ui.GwtUtil;

import java.util.logging.Level;

/**
 * A class the implements an assert mechanism simular to C.  This class 
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
public class WebAssert {

    private static final int ASSERT_TST = 406;
    private static final int PARAM_TST  = 407;

    private static boolean _serverMode            = false;
    private static boolean _serverModeEnableAssert= true;

    private static String PAD_STR= 
      "                                                                  !!";
    private static String START_STR= 
      "!!____________________________________________!!";
    private static String END_STR= 
      "!!~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~!!";
    private static String ST_STR= 
      "!!--Stack Trace Follows ----------------------!!";
    private static String LINE_STR= 
      "!!--------------------------------------------!!";
                                     
    /** 
     * if SOS_ROOT is defined the go to server mode automaticly
     */
    static {
//       final String SOS_ROOT = System.getProperty("SOS_ROOT", "");
//       if(!SOS_ROOT.equals("")) {
//            setServerMode(true);
//       }
    }

    public static void fail(String msg) {

          doFail(msg, false, ASSERT_TST);

       
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
//           System.out.println("Assert.doFail: this should never happen");
//           System.exit(1);
       }

//       StackTraceElement ste= ThrowableUtil.getWhoCalled(Assert.class);
//       StackTraceElement sAry[]=
//                      ThrowableUtil.getStackTraceForWhoCalled(Assert.class);
        StackTraceElement steAry[]= t.getStackTrace();
        StackTraceElement ste= steAry[3];
//        String thisClassName= StringUtils.getShortClassName(WebAssert.class);
//
//        for(int i=0; i<steAry.length; i++) {
//            if (thisClassName.equals(StringUtils.getShortClassName(steAry[i].getClassName()))) {
//                break;
//            }
//            else {
//                ste= steAry[i];
//            }
//        }
        showMessage(msg,typeOfTest,ste);
        GwtUtil.logToServer(Level.INFO, msg,t);

//       if (sAry!= null) t.setStackTrace(sAry);

       if(typeOfTest==ASSERT_TST) {
            t.printStackTrace();
       }
       else {
            throw t;
       }
     }


    private static void showMessage(String            msg,
                                    int               typeOfTest,
                                    StackTraceElement ste) {
        StringBuffer sb= new StringBuffer(200);
        //System.out.println("=================REAL STACK=================");
        //new Exception().printStackTrace();
        String mType= "!";
        if (typeOfTest==ASSERT_TST) {
            mType= "!!                    Assertion failed!";
        }
        else if (typeOfTest== PARAM_TST) {
            mType= "!!                    Parameter mismatch!";
        }
        else {
//            System.out.println("Assert.showMessage: this should never happen");
            //System.exit(1);
        }
        String cName= ste.getClassName();
        sb.append(START_STR).append("\n");
        sb.append(pad(mType) ).append("\n");
        sb.append(pad("!! Class:  " + cName)              ).append("\n");
        sb.append(pad("!! Method: " + ste.getMethodName()) ).append("\n");
        sb.append(pad("!! File:   " + ste.getFileName()  ) ).append("\n");
        sb.append(pad("!! Line:   " + ste.getLineNumber()) ).append("\n");
        if (msg != null) sb.append(pad("!! " + msg)).append("\n");
        sb.append(END_STR).append("\n");


        Window.alert("Assert!!!  " + sb.toString());
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
     */
//    public static void setServerMode(boolean serverMode) {
//        _serverMode= serverMode;
//        _serverModeEnableAssert= Boolean.getBoolean("ENABLE_ASSERT");
//    }



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
}
