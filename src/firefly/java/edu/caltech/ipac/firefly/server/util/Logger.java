/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util;

import edu.caltech.ipac.firefly.server.RequestOwner;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.IgnoreStackEntry;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.ThrowableUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.util.Supplier;

import static edu.caltech.ipac.util.StringUtils.applyIfNotEmpty;

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

    private enum Type { NORMAL(""), BRIEF("brief."), STATISTICS("statistics.");
                            String prefix;
                            Type(String prefix) {this.prefix = prefix;}
                        }

    private static final String ARY_SEP = "\n" + AppProperties.getProperty("ArrayRenderer.padding", "    ");

    static {
        applyIfNotEmpty(AppProperties.getProperty("logger.level"), l -> {
            Configurator.setLevel("edu.caltech.ipac", Level.toLevel(l));
            Configurator.setLevel("org.springframework.jdbc", Level.toLevel(l));
        });
        applyIfNotEmpty(AppProperties.getProperty("logger.ipac.level"), l -> {
            Configurator.setLevel("edu.caltech.ipac", Level.toLevel(l));
        });
        applyIfNotEmpty(AppProperties.getProperty("logger.jdbc.level"), l -> {
            Configurator.setLevel("org.springframework.jdbc", Level.toLevel(l));
        });
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

    /**
     * Programmatically send all logs to System.out, but have it turned off.
     * Anywhere in your code, use setLogLevel() to control what get printed to console.
     */
    private static void initConsoleLog() {
        try {
            ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
            builder.add(builder.newAppender("console", "CONSOLE").addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT));
            builder.add(builder.newRootLogger(Level.OFF).add(builder.newAppenderRef("console")));
            Configurator.initialize(builder.build());
        }catch (Exception e) {
            System.err.println("Failed to initialize Log4J framework");
        }

        try {
            java.util.logging.Logger root = java.util.logging.LogManager.getLogManager().getLogger("");
            root.setLevel(java.util.logging.Level.OFF);
        } catch (Exception e) {
            System.err.println("Failed to initialize Java logging framework");
        }
    }

    public static void setLogLevel(Level level) {
        setLogLevel(level, null);
    }

    public static void setLogLevel(Level level, String logger) {
        Appender console = ((LoggerContext) LogManager.getContext(false)).getConfiguration().getAppender("console");
        if (console == null) {
            initConsoleLog();
        }

        logger = StringUtils.isEmpty(logger) ? "" : logger;
        if (logger.length() == 0) {
            Configurator.setRootLevel(level);
        } else {
            Configurator.setLevel(logger, level);
        }
        java.util.logging.Level jl = level == Level.ERROR ? java.util.logging.Level.SEVERE :
                    level == Level.WARN ? java.util.logging.Level.WARNING :
                    level == Level.INFO ? java.util.logging.Level.INFO :
                    level == Level.DEBUG ? java.util.logging.Level.FINE :
                    level == Level.TRACE ? java.util.logging.Level.FINER :
                                java.util.logging.Level.OFF;
        java.util.logging.Logger.getLogger(logger).setLevel(jl);
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
        private static final String CLASS_NAME = Logger.class.getName();
        private final String name;

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

        public void trace(String... msgs) { log(Type.NORMAL, Level.TRACE, msgs); }
        /**
         * @deprecated
         * Same as debug().  Use debug() instead.
         */
        @Deprecated
        public void briefDebug(String msgs) { debug(msgs); }

        public void debug(String... msgs) { log(Type.NORMAL, Level.DEBUG, msgs); }

        /**
         * @deprecated
         * Same as info().  Use info() instead.
         */
        @Deprecated
        public void briefInfo(String msgs) { info(msgs); }

        public void info(String... msgs) { log(Type.NORMAL, Level.INFO, msgs);}

        public void warn(String... msgs) { warn(null, msgs); }

        public void warn(Throwable t, String... msgs) { log(Type.NORMAL, Level.WARN, t, msgs); }

        public void error(String... msgs) { error(null, msgs); }

        public void error(Throwable t, String... msgs) { log(Type.NORMAL, Level.ERROR, t, msgs); }

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
        public void stats(String function, Object... msgs) {
            log(Type.STATISTICS, Level.FATAL, null, () -> {
                if (msgs == null || msgs.length == 0) {
                    return "";
                }
                RequestOwner ro = ServerContext.getRequestOwner();
                double respTime = (System.currentTimeMillis() - ro.getStartTime().getTime())/1000.0;

                StringBuilder sb = new StringBuilder();
                for(int i = 0; i < msgs.length; i++) {
                    if (i > 0) {
                        sb.append(" | ");
                    }
                    sb.append(msgs[i]);
                    if (i+1 < msgs.length) {
                        Object val = msgs[++i];
                        if (val != null) {
                            if (val instanceof Float || val instanceof Double) {
                                sb.append(":").append(String.format("%.3f", val));
                            }
                            else {
                                sb.append(":").append(val);
                            }
                        }
                    }
                }
                return String.format(FMT, function, ro.getRemoteIP(), respTime, sb);
            });
        }
        private static final String FMT = "%-20s %-15s %8.3f -- %s";

    //====================================================================
    //
    //====================================================================

        private void log(Type type, Level level, String... msgs) {
            log(type, level, null, msgs);
        }

        private void log(Type type, Level level, Throwable t, String... msgs) {
            log(type, level, t, () -> {
                if (msgs == null) return "";
                return msgs.length == 1 ? msgs[0] :
                       CollectionUtil.toString(msgs, type.equals(Type.NORMAL) ? ARY_SEP : ", ");
            });
        }

        private void log(Type type, Level level, Throwable t, Supplier<String> msg) {
            org.apache.logging.log4j.Logger logger = getLogger(type);
            if (logger != null) {
                logger.log(level, msg, t);
            }
        }

        private org.apache.logging.log4j.Logger getLogger(Type type) {
            org.apache.logging.log4j.Logger logger = LogManager.getLogger(type.prefix + name);
            if (logger == null) {
                System.out.println("Logger not found:" + type.prefix + name);
            }
            return logger;
        }

    }



}
