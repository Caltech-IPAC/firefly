/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util;


import java.util.HashMap;
import java.util.Map;

/**
 * Date: Oct 13, 2008
 *
 * @author loi
 * @version $Id: StopWatch.java,v 1.6 2010/10/20 20:12:50 loi Exp $
 */
public class StopWatch {

    public enum Unit {
        SECONDS(1000), MINUTES(60*1000), HOURS(60*60*1000);
        private double numMilliSec;

        Unit(long numMilliSec) {
            this.numMilliSec = numMilliSec;
        }

        public double convert(long milliSec) {
            return milliSec / numMilliSec;
        }
    };

    private static ThreadLocal<StopWatch> stopWatch = new ThreadLocal<StopWatch>(){
                        protected StopWatch initialValue() {
                            return new StopWatch();
                        }
                    };
    private Map<String, Tracker> logs = new HashMap<String, Tracker>();
    private Logger.LoggerImpl logger = Logger.getLogger();

    private StopWatch() {
    }

    public static void clear() {
        if (stopWatch != null) stopWatch.remove();
    }

    public static StopWatch getInstance() {
        return stopWatch.get();
    }

    /**
     * StopWatch is designed to only work in debug mode.  This function enable it to be turned on regardless of debug mode.
     * @return
     */
    public StopWatch enable() {
        return this;
    }

    public StopWatch setLogger(Logger.LoggerImpl logger) {
        this.logger = logger;
        return this;
    }

    public StopWatch start(String desc) {
        Tracker l = getTracker(desc);
        if (l == null) {
            l = new Tracker(desc, logger);
            logs.put(desc, l);
        }
        l.starts();
        return this;
    }

    public StopWatch stop(String desc) {
        Tracker l = getTracker(desc);
        if (l != null) {
            l.stops();
        }
        return this;
    }

    public StopWatch printLog(String desc) {
        return printLog(desc, Unit.SECONDS);
    }

    public StopWatch printLog(String desc, Unit unit) {

        if (getTracker(desc) != null) {
            getTracker(desc).printLog(unit);
        }
        return this;
    }

    public Tracker getTracker(String desc) {
        return logs.get(desc);
    }


//====================================================================
//
//====================================================================

    public static class Tracker {

        private String desc;
        private long totalTime = 0;
        private long starts;
        private long numStops = 0;
        private long elapsed = 0;
        private boolean isRunning = false;
        private Logger.LoggerImpl logger;

        Tracker(String desc, Logger.LoggerImpl logger) {
            this.desc = desc;
            this.logger = logger;
        }

        public long starts() {
            starts = System.currentTimeMillis();
            isRunning = true;
            return starts;
        }

        public long stops() {
            long t = System.currentTimeMillis();
            long elapse = t - starts;
            totalTime += elapse;
            numStops ++;
            isRunning = false;
            elapsed = elapse;
            return elapsed;
        }

        public double getTotalTime(Unit unit) {
            return unit.convert(totalTime);
        }

        public double getElapsedTime(Unit unit) {
            return unit.convert(elapsed);
        }

        public double getAvgTime(Unit unit) {
            return getTotalTime(unit)/numStops;
        }

        public void printLog(Unit unit) {
            if (isRunning) stops();
            if (numStops == 1) {
                logger.trace(String.format("%s ran with elapsed time of %.4f %s", desc, getElapsedTime(unit),  unit.name()));
            } else {
                logger.trace(String.format("%s ran %d times.", desc, numStops),
                             String.format("Elapsed Time: %.4f %s.", getElapsedTime(unit), unit.name()),
                             String.format("Total time is %.4f %s", getTotalTime(unit), unit.name()),
                             String.format("Avg time is %.4f %s.", getAvgTime(unit), unit.name()));
            }
        }
    }

}
