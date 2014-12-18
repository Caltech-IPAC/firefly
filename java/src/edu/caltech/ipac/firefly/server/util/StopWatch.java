package edu.caltech.ipac.firefly.server.util;

import edu.caltech.ipac.util.AppProperties;

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

    private static boolean DEBUG_MODE = AppProperties.getBooleanProperty("debug.mode", false);
    private static ThreadLocal<StopWatch> stopWatch = new ThreadLocal<StopWatch>(){
                        protected StopWatch initialValue() {
                            return new StopWatch();
                        }
                    };
    private Map<String, Tracker> logs = new HashMap<String, Tracker>();

    private StopWatch() {
    }

    public static void clear() {
        if (stopWatch != null) stopWatch.remove();
    }

    public static StopWatch getInstance() {
        return stopWatch.get();
    }

    public void start(String desc) {
        if (!DEBUG_MODE) return;        // if not running in debug mode, ignore StopWatch logging

        Tracker l = getTracker(desc);
        if (l == null) {
            l = new Tracker(desc);
            logs.put(desc, l);
        }
        l.starts();
    }

    public void stop(String desc) {
        Tracker l = getTracker(desc);
        if (l != null) {
            l.stops();
        }
    }

    public void printLog(String desc) {
        printLog(desc, Unit.SECONDS);
    }

    public void printLog(String desc, Unit unit) {

        if (!DEBUG_MODE) return;        // if not running in debug mode, ignore StopWatch logging

        if (getTracker(desc) != null) {
            getTracker(desc).printLog(unit);
        }
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

        public Tracker(String desc) {
            this.desc = desc;
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
                Logger.getLogger("StopWatch").info(String.format("%s ran with elapsed time of %.4f %s", desc, getElapsedTime(unit),  unit.name()));
            } else {
                Logger.getLogger("StopWatch").info(String.format("%s ran %d times.", desc, numStops),
                             String.format("Elapsed Time: %.4f %s.", getElapsedTime(unit), unit.name()),
                             String.format("Total time is %.4f %s", getTotalTime(unit), unit.name()),
                             String.format("Avg time is %.4f %s.", getAvgTime(unit), unit.name()));
            }
        }
    }

}
