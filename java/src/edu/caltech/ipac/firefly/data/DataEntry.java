package edu.caltech.ipac.firefly.data;

import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.visualize.draw.Metric;
import edu.caltech.ipac.visualize.draw.Metrics;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
/**
 * User: roby
 * Date: Apr 13, 2009
 * Time: 3:15:21 PM
 */


/**
 * @author Trey Roby
*/
public interface DataEntry extends Serializable {


    public static class Str implements DataEntry {
        private String _str;
        public Str()  {}
        public Str(String str)  { _str= str; }

        public String getString() { return _str; }
        public String toString() { return _str; }
    }

    public static class HM implements DataEntry {
        private HashMap<Metrics, Metric> _hm;
        public HM()  {}
        public HM(HashMap<Metrics, Metric> hm)  { _hm= hm; }

        public HashMap<Metrics, Metric> getHashMap() { return _hm; }
        public String toString() { return _hm.toString(); }
    }

    public static class Numeric implements DataEntry {
        private Number _number;
        public Numeric()  {}
        public Numeric(Number number)  { _number= number; }

        public Number getNumber() { return _number; }
        public String toString() { return _number.toString(); }
    }

    public static class IntArray implements DataEntry {
        private int _ary[];
        public IntArray()  {}
        public IntArray(int ary[])  { _ary= ary; }

        public int[] getArray() { return _ary; }
        public String toString() { return Arrays.toString(_ary); }
    }

    public static class DoubleArray implements DataEntry {
        private double _ary[];
        public DoubleArray()  {}
        public DoubleArray(double ary[])  { _ary= ary; }

        public double[] getArray() { return _ary; }
        public String toString() { return Arrays.toString(_ary); }
    }

    public static class StringArray implements DataEntry {
        private String _ary[];
        public StringArray()  {}
        public StringArray(String ary[])  { _ary= ary; }

        public String[] getArray() { return _ary; }
        public String toString() { return Arrays.toString(_ary); }
    }


    public static class WP implements DataEntry {
        private WorldPt _wp;
        public WP()  {}
        public WP(WorldPt wp)  { _wp= wp; }

        public WorldPt getWorldPt() { return _wp; }
        public String toString() { return _wp.toString(); }
    }

    public static class RawDataSetResult implements DataEntry {
        private RawDataSet _rawDataSet;
        public RawDataSetResult() {}
        public RawDataSetResult(RawDataSet rawDataSet) { _rawDataSet = rawDataSet; }

        public RawDataSet getRawDataSet() { return _rawDataSet; }
    }
}

