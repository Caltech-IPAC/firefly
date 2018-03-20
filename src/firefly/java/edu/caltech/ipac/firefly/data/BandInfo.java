/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data;

import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.visualize.draw.Metric;
import edu.caltech.ipac.visualize.draw.Metrics;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: balandra
 * Date: Apr 9, 2010
 * Time: 10:13:40 AM
 * To change this template use File | Settings | File Templates.
 */
public class BandInfo implements Serializable {

    private HashMap<Band, HashMap<Metrics, Metric>> metricsMap;

    public BandInfo(HashMap<Band, HashMap<Metrics, Metric>> mMap){
        this.metricsMap = mMap;

    }

    public HashMap<Band, HashMap<Metrics, Metric>> getMetricsMap() {
        return metricsMap;
    }
}
