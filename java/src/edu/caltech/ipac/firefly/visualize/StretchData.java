package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.visualize.plot.RangeValues;

import java.io.Serializable;
/**
 * User: roby
 * Date: Mar 1, 2010
 * Time: 3:07:35 PM
 */



/**
 * @author Trey Roby
 */
public class StretchData implements Serializable {

    private final static String SPLIT_TOKEN= "--StretchData--";

    private Band _band;
    private RangeValues _rv;
    private boolean _bandVisible;

    private StretchData() {}

    public StretchData(Band band,
                       RangeValues rv,
                       boolean bandVisible) {
        _band= band;
        _rv= rv;
        _bandVisible= bandVisible;
    }

    public Band getBand() { return _band; }
    public RangeValues getRangeValues() { return _rv; }
    public boolean isBandVisible() { return _bandVisible; }

    public String toString() {
        return _band.toString()+SPLIT_TOKEN+_rv.serialize()+SPLIT_TOKEN+_bandVisible;
    }

    public static StretchData parse(String s) {
        if (s==null) return null;
        String sAry[]= s.split(SPLIT_TOKEN,4);
        StretchData retval= null;
        if (sAry.length==3) {
            int i=0;
            Band band= Band.parse(sAry[i++]);
            RangeValues rv= RangeValues.parse(sAry[i++]);
            boolean v= Boolean.parseBoolean(sAry[i++]);
            retval= new StretchData(band,rv,v);
        }
        return retval;
    }

}
