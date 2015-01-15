/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.graph;

/**
 * @author tatianag
 *         $Id: $
 */
public class LinearScale implements Scale {
    public double getScaled(double val) {
        return val;
    }

    public double getUnscaled(double val) {
        return val;
    }

}
