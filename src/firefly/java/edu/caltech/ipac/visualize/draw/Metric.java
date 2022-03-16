/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.draw;


import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;

public record Metric(String desc, ImageWorkSpacePt imageWorkSpacePt, double value, String units) {
    public static final double NULL_DOUBLE = Double.NaN;
}
