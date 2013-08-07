package edu.caltech.ipac.firefly.visualize.graph;

/**
 * @author tatianag
 *         $Id: $
 */
public interface Scale {
    public double getScaled(double val);
    public double getUnscaled(double val);
}
