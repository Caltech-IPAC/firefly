/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

/**
 * @author Trey Roby
 */
public record WebFitsData(double dataMin, double dataMax, double largeBinPercent, long fitsFileSize, String fluxUnits) { }

