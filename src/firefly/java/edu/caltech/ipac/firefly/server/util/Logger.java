/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util;

import edu.caltech.ipac.util.ClientLog;
import edu.caltech.ipac.firefly.server.RequestOwner;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.IgnoreStackEntry;
import edu.caltech.ipac.util.ThrowableUtil;
import org.apache.log4j.Level;
import org.apache.log4j.or.ObjectRenderer;

/**
 * Date: Dec 10, 2008
 *
 * @author loi
 * @version $Id: Logger.java,v 1.16 2012/05/31 00:34:28 loi Exp $
 */
@IgnoreStackEntry
public class Logger {
    public static final String SEARCH_LOGGER = "search";
    public static final String DOWNLOAD_LOGGER = "dl";
    public static final String VIS_LOGGER = "visu";
    public static final String INFO_LOGGER = "info";

    private enum Type { NORMAL(""), BRIEF("brief."), STATISTICS("statistics.");
                            String prefix;
                            private Type(String prefix) {this.prefix = prefix;}
                        }

    /**
     * use static accessors methods
     */
    private Logger() {}

//====================================================================
//  static accessors methods
//====================================================================
    public static LoggerImpl getLogger() {
        return new LoggerImpl();
    }

    public static LoggerImpl getLogger(Class clsname) {
        return new LoggerImpl(clsname.getName());
    }

    public static LoggerImpl getLogger(String name) {
        return new LoggerImpl(name);
    }

    public static void debug(String... msgs) {
        getLogger().debug(msgs);
    }

    public static void briefDebug(String msg) {
        getLogger().briefDebug(msg);
    }

    public static void info(String... msg) {
        getLogger().info(msg);
    }

    public static void briefInfo(String msgs) {
        getLogger().briefInfo(msgs);
    }

    public static void warn(String... msgs) {
        getLogger().warn(msgs);
    }

    public static void warn(Throwable t, String... msgs) {
        getLogger().warn(t, msgs);
    }

    public static void error(String... msgs) {
        getLogger().error(msgs);
    }

    public static void error(Throwable t, String... msgs) {
        getLogger().error(t, msgs);
    }

    public static void stats(String loggerName, String function, Object... msgs) {
        getLogger(loggerName).stats(function, msgs);
    }


//====================================================================
//  Custom Renderer
//====================================================================

    public static class VerboseMessage {
        String[] msgs;

        public VerboseMessage(String[] msgs) {
            this.msgs = msgs;
        }

        public String toString() {
            return CollectionUtil.toString(msgs);
        }
    }

    /**
     * use string '[Ljava.lang.String' to define in your configuration
     */
    public static class ArrayRenderer implements ObjectRenderer {
        private String padding = AppProperties.getProperty("ArrayRenderer.padding", "    ");

        public String doRender(Object o) {

            if(o instanceof VerboseMessage) {
                String[] msgs = ((VerboseMessage)o).msgs;
                StringBuffer sb = new StringBuffer();
                for(int i = 0; i < msgs.length; i++) {
                    Object m = msgs[i];
                    if (i > 0) sb.append("\n").append(padding);
                    sb.append(String.valueOf(m));
                }
                return sb.toString();
            }
            return String.valueOf(o);
        }
    }

    /**
     * use string '[Ljava.lang.String' to define in your configuration
     */
    public static class WrappedArrayRenderer implements ObjectRenderer {
        private static final String PRE_STR   = "   ";
        private static final String BEGIN_STR = "  _________________";
        private static final String END_STR   = "  ~~~~~~~~~~~~~~~~~";

        public String doRender(Object o) {
            Object[] msgs = o.getClass().isArray() ? (Object[])o : new Object[]{o};
            StringBuffer sb = new StringBuffer("\n").append(BEGIN_STR);
            for(Object m : msgs) {
                sb.append(PRE_STR).append("\n").append(String.valueOf(m));
            }
            sb.append(END_STR);
            return sb.toString();
        }
    }

    private static void doLog(String msg, boolean isStatic, int stackCount) {
        if (stackCount > 0) {
            doLog(msg, isStatic, stackCount-1);
        } else {
            if (isStatic) {
                Logger.getLogger("test").info(msg);
                Logger.getLogger("test").briefInfo(msg);
                Logger.getLogger(DOWNLOAD_LOGGER).stats("test", msg);
            } else {
                Logger.info(msg);
                Logger.briefInfo(msg);
                Logger.stats(DOWNLOAD_LOGGER, "test", msg);
            }
        }
    }

    public static void main(String[] args) {
        long ctime;

        for(int j = 0; j < 5; j++) {

            int count = j*20;
            System.out.println("\nfor stack count:" + count);
            
            ctime= System.currentTimeMillis();
            for(int i = 0; i < 1000; i++) {
                doLog("just a test", true, count);
            }
            System.out.println("static elapsed time:" + (System.currentTimeMillis() - ctime));

            ctime= System.currentTimeMillis();
            for(int i = 0; i < 1000; i++) {
                doLog("just a test", false, count);
            }
            System.out.println("dynamic elapsed time:" + (System.currentTimeMillis() - ctime));

        }
    }

//====================================================================
//
//====================================================================

    /**
     * an implementation of the Logger API using log4j.
     */
    @IgnoreStackEntry
    public static class LoggerImpl {
        private static String CLASS_NAME = Logger.class.getName();
        private String name;

        /**
         * use static Logger.getLogger() instead
         */
        LoggerImpl() {
            StackTraceElement ste = ThrowableUtil.getWhoCalled(LoggerImpl.class);
            name = ste.getClassName();
        }

        LoggerImpl(String name) {
            this.name = name;
        }

        public void briefDebug(String msgs) {
            log(Type.BRIEF, Level.DEBUG, msgs);
        }

        public void debug(String... msgs) {
            log(Type.NORMAL, Level.DEBUG, msgs);
        }

        public void briefInfo(String msgs) {
            log(Type.BRIEF, Level.INFO, msgs);
        }

        public void info(String... msgs) {
            log(Type.NORMAL, Level.INFO, msgs);
        }

        public void warn(String... msgs) {
            warn(null, msgs);
        }

        public void warn(Throwable t, String... msgs) {
            log(Type.NORMAL, Level.WARN, t, msgs);
        }

        public void error(String... msgs) {
            error(null, msgs);
        }

        public void error(Throwable t, String... msgs) {
            log(Type.NORMAL, Level.ERROR, t, msgs);
        }

        /**
         * This method is for logging statistic related information.
         * It is recorded at the level 'FATAL' so that it will not be accidentally
         * turned off.
         *
         * Format:
         * - 2 parts:  common information follow by details; separated by '--'
         * - common columns are:  time-stamp category function remote_ip response_time   // these are separated by spaces.
         * - details are key:value pair, separated by ' | '.
         *
         * Message array:
         * - the variable array should be in the form of {key, value, key, value ... etc}
         * - if 'value' is a real number, it will be formated to %.3f(3 decimal places).
         * - if 'value' is null or empty string, only the 'key' portion are logged.  ":" will NOT be added.
         *
         * Usage:
         * <code>
         *    logger.stats("TestQuery", "rows", 1234, "fsize(MB)", 1.234231, "some additional information");
         *
         *    output:
         *    2011/04/22 14:53:34 search TestQuery      127.0.0.1    0.104 -- rows:1234 | fsize(MB):1.234 | some additional information
         * </code>
         *
         *
         *
         * @param function a function name.  It must not contain spaces.  Recommend less than 20 chars.
         * @param msgs the messages to log.  It should be in the form of key, value, key, value etc..
         */
        static String fmt = "%-20s %-15s %8.3f -- %s";
        public void stats(String function, Object... msgs) {
            RequestOwner ro = ServerContext.getRequestOwner();
            double respTime = (System.currentTimeMillis() - ro.getStartTime().getTime())/1000.0;
            log(Type.STATISTICS, Level.FATAL, String.format(fmt, function, ro.getRemoteIP(), respTime, formatMsg(msgs)));
        }

    //====================================================================
    //
    //====================================================================

        private String formatMsg(Object... msgs) {
            if (msgs == null || msgs.length == 0) {
                return "";
            }

            StringBuffer sb = new StringBuffer();
            for(int i = 0; i < msgs.length; i++) {
                if (i > 0) {
                    sb.append(" | ");
                }
                sb.append(String.valueOf(msgs[i]));
                if (i+1 < msgs.length) {
                    Object val = msgs[++i];
                    if (val != null) {
                        String valStr = String.valueOf(val);
                        if (val instanceof Float || val instanceof Double) {
                            valStr = String.format("%.3f", val);
                        }
                        sb.append(":").append(valStr);
                    }
                }
            }
            return sb.toString();
        }


        private void log(Type type, Level level, String... msgs) {
            log(type, level, null, msgs);
        }

        private void log(Type type, Level level, Throwable t, String... msgs) {
            org.apache.log4j.Logger logger = getLogger(type);
            if (logger != null) {
                logger.log(CLASS_NAME, level, getMsg(type, msgs), t);
            }
        }

        private Object getMsg(Type type, String... msgs) {
            if(type.equals(Type.NORMAL)) {
                return new VerboseMessage(msgs);
            } else {
                return  (msgs == null) ? "" : CollectionUtil.toString(msgs);
            }
        }

        private org.apache.log4j.Logger getLogger(Type type) {
            org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(type.prefix + name);
            if (logger == null) {
                System.out.println("Logger not found:" + type.prefix + name);
            }
            return logger;
        }

    }


    @IgnoreStackEntry
    public static class ClientLogImpl implements ClientLog.Logger {

        public void log(boolean showMethodName, ClientLog.MessageType level, String... messages) {

            if(level.equals(ClientLog.MessageType.ERROR)) {
                Logger.error(messages);
            } else if(level.equals(ClientLog.MessageType.WARNING)) {
                Logger.warn(messages);
            } else if (level.equals(ClientLog.MessageType.INFORMATION)) {
                Logger.info(messages);
            } else if (level.equals(ClientLog.MessageType.BRIEF_INFO)) {
                Logger.briefInfo(briefMsg(messages));
            } else {
                Logger.debug(messages);
            }
        }

        private String briefMsg(String... messages) {
            if(messages == null || messages.length == 0) {
                return "";
            } else {
                return CollectionUtil.toString(messages);
            }
        }
    }

}
