/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data;

import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.visualize.draw.AreaStatisticsUtil;

import java.util.HashMap;

/**
 * User: balandra
 * Date: Apr 9, 2010
 */
public record BandInfo (HashMap<Band, HashMap<AreaStatisticsUtil.Metrics, AreaStatisticsUtil.Metric>> metricsMap){}
