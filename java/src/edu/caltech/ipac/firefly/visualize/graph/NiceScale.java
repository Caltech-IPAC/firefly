package edu.caltech.ipac.firefly.visualize.graph;

import edu.caltech.ipac.firefly.util.MinMax;

/**
 * From
 * http://www.trollop.org/2011/03/15/algorithm-for-optimal-scaling-on-a-chart-axis/
 *
 * Based on Nice Numbers algorithm by Andrew Glassner
 * 
 * @author tatianag
 *         $Id: NiceScale.java,v 1.2 2012/10/11 21:32:11 tatianag Exp $
 */
public class NiceScale {

    private double minPoint;
    private double maxPoint;
    private double maxTicks;
    private double tickSpacing;
    private double niceMin;
    private double niceMax;

    public NiceScale(MinMax minMax, int maxTicks) {
        this(minMax.getMin(), minMax.getMax(), maxTicks);
    }

    /**
     * Instantiates a new instance of the NiceScale class.
     *
     * @param min the minimum data point on the axis
     * @param max the maximum data point on the axis
     */
    public NiceScale(double min, double max, double maxTicks) {
        // make sure min != max
        if (min == max) {
            double newmin, newmax;
            if (min == 0d) {
                newmin = 1d; newmax = 1d;
            } else {
                newmin = min-min/2d;
                newmax = max+max/2d;
            }
            min = newmin; max = newmax;
        }
        this.minPoint = min;
        this.maxPoint = max;
        this.maxTicks = maxTicks;
        calculate();
    }

    /**
     * Calculate and update values for tick spacing and nice
     * minimum and maximum data points on the axis.
     */
    private void calculate() {
        double range = niceNum(maxPoint - minPoint, false);
        this.tickSpacing = niceNum(range / (maxTicks - 1), true);
        this.niceMin =
            Math.floor(minPoint / tickSpacing) * tickSpacing;
        this.niceMax =
            Math.ceil(maxPoint / tickSpacing) * tickSpacing;
    }

    /**
     * Returns a "nice" number approximately equal to range Rounds
     * the number if round = true Takes the ceiling if round = false.
     *
     * @param range the data range
     * @param round whether to round the result
     * @return a "nice" number to be used for the data range
     */
    private double niceNum(double range, boolean round) {
        double exponent; /** exponent of range */
        double fraction; /** fractional part of range */
        double niceFraction; /** nice, rounded fraction */

        exponent = Math.floor(Math.log10(range));
        fraction = range / Math.pow(10, exponent);

        if (round) {
                if (fraction < 1.5)
                    niceFraction = 1;
                else if (fraction < 3)
                    niceFraction = 2;
                else if (fraction < 7)
                    niceFraction = 5;
                else
                    niceFraction = 10;
        } else {
                if (fraction <= 1)
                    niceFraction = 1;
                else if (fraction <= 2)
                    niceFraction = 2;
                else if (fraction <= 5)
                    niceFraction = 5;
                else
                    niceFraction = 10;
        }

        return niceFraction * Math.pow(10, exponent);
    }

    /**
     * Sets the minimum and maximum data points for the axis.
     *
     * @param minPoint the minimum data point on the axis
     * @param maxPoint the maximum data point on the axis
     */
    public void setMinMaxPoints(double minPoint, double maxPoint) {
        this.minPoint = minPoint;
        this.maxPoint = maxPoint;
        calculate();
    }

    /**
     * Sets maximum number of tick marks we're comfortable with
     *
     * @param maxTicks the maximum number of tick marks for the axis
     */
    public void setMaxTicks(double maxTicks) {
        this.maxTicks = maxTicks;
        calculate();
    }

    /**
     * Gets the tick spacing.
     *
     * @return the tick spacing
     */
    public double getTickSpacing() {
        return tickSpacing;
    }

    /**
     * Gets the "nice" minimum data point.
     *
     * @return the new minimum data point for the axis scale
     */
    public double getNiceMin() {
        return niceMin;
    }

    /**
     * Gets the "nice" maximum data point.
     *
     * @return the new maximum data point for the axis scale
     */
    public double getNiceMax() {
        return niceMax;
    }

    /**
     * Gets format string adequate for the tick spacing
     * @return
     */
    public String getFormatString() {
        String format;
        if (tickSpacing == 0) return "0";
        double exponent = Math.floor(Math.log10(tickSpacing));
        if (exponent >= 0) {
            format = "0";
        } else {
            format = "0.0";
            for (int i=1; exponent+i<0; i++) {
                format += "0";
            }
        }
        return format;
    }
}