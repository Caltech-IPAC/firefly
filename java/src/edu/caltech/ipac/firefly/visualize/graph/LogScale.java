/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.graph;

/**
 * @author tatianag
 *         $Id: $
 */
public class LogScale implements Scale {
    public double getScaled(double val) {
        return Math.log10(val);
    }

    public double getUnscaled(double val) {
        return Math.pow(10, val);
    }

}
