/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.visualize.plot.RangeValues;
/**
 * User: roby
 * Date: Mar 1, 2010
 * Time: 3:07:35 PM
 */



/**
 * @author Trey Roby
 */
public class StretchData {

    private final static String SPLIT_TOKEN= "--StretchData--";

    private Band _band;
    private RangeValues _rv;
    private boolean _bandVisible;

    public StretchData(Band band, RangeValues rv, boolean bandVisible) {
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

}
