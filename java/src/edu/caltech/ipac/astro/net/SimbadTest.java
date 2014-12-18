package edu.caltech.ipac.astro.net;

import edu.caltech.ipac.astro.simbad.SimbadClient;
import edu.caltech.ipac.astro.simbad.SimbadObject;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

public class SimbadTest {

    private static final float ALLOW_PCT_ERROR = 0.01f;

    private String[] targets;

    String[] headers = {"Target", "RA(4/3/%diff)", "DEC(4/3/%diff)", "PM-RA(4/3/%diff)", "PM-DEC(4/3/%diff)",
                                  "Mag(4/3/%diff)", "BMag(4/3/%diff)", "PARA(4/3/%diff)"};
    String colFmt = "%-6s \t %-32s \t %-32s \t %-32s \t %-32s " +
                         "\t %-32s \t %-32s \t %-32s";
    String decFmt = "%3.6f/%3.6f/%3.6f";
    boolean isVerbose = Boolean.getBoolean("Verbose");

    StopWatch s4Timer = new StopWatch();
    StopWatch s3Timer = new StopWatch();
    ArrayList<String> failedCases = new ArrayList<String>();

    public SimbadTest(String... targets) {
        this.targets = targets;
    }


    public void runTest() throws Exception {

        failedCases.clear();
        s4Timer.starts();
        Simbad4Client s4 = new Simbad4Client();
        s4Timer.stops();

        s3Timer.starts();
        SimbadClient s3 = new SimbadClient();
        s3Timer.stops();

        PrintWriter writer = new PrintWriter(new File("simbad_compare.out"));
        writer.println(String.format(colFmt, (Object[])headers));

        for(String t : targets) {
            s4Timer.starts();
            SimbadObject so4 = s4.searchByName(t);
            s4Timer.stops();

            s3Timer.starts();
            SimbadObject so3 = s3.searchByName(t);
            s3Timer.stops();

            doReport(writer, t, so4, so3);
        }

        writer.println(String.format("\n\nNumber of targets= %d", targets.length));
        writer.println(String.format("Total time for Simbad3= %5.3f seconds", s3Timer.getTotalTime(StopWatch.Unit.SECONDS)));
        writer.println(String.format("Average time for Simbad3= %5.3f seconds", s3Timer.getAvgTime(StopWatch.Unit.SECONDS)));
        writer.println(String.format("Total time for Simbad4= %5.3f seconds", s4Timer.getTotalTime(StopWatch.Unit.SECONDS)));
        writer.println(String.format("Average time for Simbad4= %5.3f seconds", s4Timer.getAvgTime(StopWatch.Unit.SECONDS)));

        writer.println(String.format("\nNumber of cases differ more than +/- %3.2f percent= %d", ALLOW_PCT_ERROR, failedCases.size()));
        writer.println("\n\nFAILED CASES");
        writer.println(String.format(colFmt, (Object[])headers));
        for(String s : failedCases) {
            writer.println(s);
        }

        writer.flush();
        writer.close();


    }

    private void doReport(PrintWriter writer, String target, SimbadObject so4, SimbadObject so3) {
        int i = 1;
        StringBuffer[] results = {new StringBuffer(target), new StringBuffer(), new StringBuffer(), new StringBuffer(),
                                  new StringBuffer(), new StringBuffer(), new StringBuffer(), new StringBuffer()};
        boolean ra = doCompare(so4.getRa(), so3.getRa(), results[i++]);
        boolean dec = doCompare(so4.getDec(), so3.getDec(), results[i++]);
        boolean pmra = doCompare(so4.getRaPM(), so3.getRaPM(), results[i++]);
        boolean pmdec = doCompare(so4.getDecPM(), so3.getDecPM(), results[i++]);
        boolean mag = doCompare(so4.getMagnitude(), so3.getMagnitude(), results[i++]);
        boolean bmag = doCompare(so4.getBMagnitude(), so3.getBMagnitude(), results[i++]);
        boolean para = doCompare(so4.getParallax(), so3.getParallax(), results[i++]);

        String ln = String.format(colFmt, (Object[])results);
        writer.println(ln);
        if (!(ra && dec && pmra && pmdec && mag && bmag && para)) {
            failedCases.add(ln);
        }
    }

    private boolean doCompare(double ra, double ra1, StringBuffer result) {
        double diff = Math.abs(Math.abs(ra) - Math.abs(ra1))/Math.abs(ra1)*100;
        result.append(String.format(decFmt, ra, ra1, diff));
        return Double.isNaN(diff) || diff < ALLOW_PCT_ERROR;
    }


    public static void main(String[] args) {
        try {
            SimbadTest simbadTest = new SimbadTest(args);
            simbadTest.runTest();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static class StopWatch {

        public static enum Unit {
            SECONDS(1000), MINUTES(60*1000), HOURS(60*60*1000);
            private long numMilliSec;

            Unit(long numMilliSec) {
                this.numMilliSec = numMilliSec;
            }

            public double convert(long milliSec) {
                return milliSec / numMilliSec;
            }
        };

        private long totalTime = 0;
        private long starts;
        private long numStops = 0;

        public long starts() {
            starts = System.currentTimeMillis();
            return starts;
        }

        public long stops() {
            long t = System.currentTimeMillis();
            long elapse = t - starts;
            totalTime += elapse;
            numStops ++;
            return elapse;
        }

        public double getTotalTime(Unit unit) {
            return unit.convert(totalTime);
        }

        public double getAvgTime(Unit unit) {
            return getTotalTime(unit)/numStops;
        }
    }

}

