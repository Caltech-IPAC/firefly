package edu.caltech.ipac.util;


import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;

@IgnoreStackEntry
public class ClientLog {


    private static ThreadLocal<String> _threadDesc = new ThreadLocal<String>();

    private static final Object LOCK= new Object();
    private static final String PRE_STR   = "     ";
    private static final String BEGIN_STR = "_________________";
    private static final String END_STR   = "~~~~~~~~~~~~~~~~~";
    private static final int PAD_SIZE   = 60;


    private static Logger _logger= new ClientLogger();
    private static PrintStream _out= null;

    public static enum MessageType {INFORMATION, WARNING, ERROR, BRIEF_INFO };
    private static final EnumMap<MessageType, String> _descriptions=
                                  new EnumMap<MessageType, String>(MessageType.class);

    private static final SimpleDateFormat _dateFormat =
             new SimpleDateFormat("MM/d HH:mm:ss");

    static {
        _descriptions.put(MessageType.INFORMATION, "");
        _descriptions.put(MessageType.ERROR, "!!! Error !!!");
        _descriptions.put(MessageType.WARNING, "!!! Warning !!!");
        _descriptions.put(MessageType.BRIEF_INFO, "");
    }

    public static void setLogger(Logger logger) {
        _logger = logger;
    }

    public static void warning(String... s) {
        printMessage(false,MessageType.WARNING, s);
    }
    public static void warning(boolean showMethodName, String... s) {
        printMessage(showMethodName, MessageType.WARNING, s);
    }

    public static void warning(boolean showMethodName, List<String> sList) {
        warning(showMethodName, sList.toArray(new String[sList.size()]));
    }

    public static void message(String... s) {
        printMessage(false, MessageType.INFORMATION, s);
    }

    public static void message(List<String> sList) {
        message(sList.toArray(new String[sList.size()]));
    }

    public static void message(boolean showMethodName, String... s) {
        printMessage(showMethodName,MessageType.INFORMATION, s);
    }

    public static void brief(String s) {
        printMessage(false, MessageType.BRIEF_INFO, s);
    }
    public static void brief(boolean showMethodName, String s) {
        printMessage(showMethodName, MessageType.BRIEF_INFO, s);
    }

    public static void printMessage(boolean      showMethodName,
                                    MessageType  level,
                                    String...    s) {
        _logger.log(showMethodName, level, s);
    }

    public static PrintStream getOutStream() {
        return (_out==null) ?  System.out : _out;
    }

    public static void setOutStream(PrintStream out) { _out= out; }

    public static void setThreadDesc(String desc) {  _threadDesc.set(desc); }


//====================================================================
//
//====================================================================

    @IgnoreStackEntry
    private static class ClientLogger implements Logger {

        public void log(boolean showMethodName, MessageType level, String... s) {
            synchronized (LOCK) {
                StackTraceElement ste= ThrowableUtil.getWhoCalled(ClientLog.class);
                PrintStream out= getOutStream();
                if (ste!=null) {

                    String tName= Thread.currentThread().getName();
                    String cName= StringUtil.getShortClassName(ste.getClassName());
                    String method=  "";
                    String thread=  "";
                    String dateStr= "";
                    if (showMethodName) method= "." + ste.getMethodName() + "()";
                    if (level==MessageType.WARNING || level==MessageType.ERROR) {
                        out.println("");
                    }
                    if (level!=MessageType.BRIEF_INFO) {
                        thread= ":      Thread: "+tName+":";
                        Date   date= new Date();
                        dateStr= PRE_STR + _dateFormat.format(date);
                        out.println(BEGIN_STR + _descriptions.get(level) );
                    }
                    else {
                        out.print("-");
                    }
                    String header = cName+method+ thread;
                    if (header.length()>PAD_SIZE && level!=MessageType.BRIEF_INFO) {
                        out.println(header);
                        out.print(dateStr);
                    }
                    else {
                        if (level!=MessageType.BRIEF_INFO) {
                            header= StringUtil.pad(header, PAD_SIZE);
                        }
                        out.print(header+dateStr);
                    }
                    if (level==MessageType.BRIEF_INFO) {
                        out.println(": " + s[0]);
                    }
                    else {
                        out.println("");
                        if (_threadDesc.get()!=null) {
                            out.println(_threadDesc.get());
                        }
                        for(String mEle : s) {
                            if (mEle!=null)  out.println(PRE_STR + mEle);
                        }
                        out.println(END_STR);
                    }
                    out.flush();
                }
            }
        }
    }


    /**
     * Date: Dec 16, 2008
     *
     * @author loi
     * @version $Id: ClientLog.java,v 1.12 2008/12/17 23:22:29 loi Exp $
     */
    public interface Logger {

        public void log(boolean showMethodName,
                        MessageType  level,
                        String... messages);
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
