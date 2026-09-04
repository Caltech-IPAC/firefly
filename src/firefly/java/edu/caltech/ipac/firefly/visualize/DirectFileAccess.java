package edu.caltech.ipac.firefly.visualize;

import java.io.Serializable;

/**
 * @author Trey Roby
 *
 */
public record DirectFileAccess  (
        int hduNumber, boolean cube, int cubeLength, int planeNumber,
        long dataOffset, int bitpix, int naxis1, int naxis2, int naxis3, double cdelt2,
        String bunit, double bscale, double bzero, String blankValue, String origin, PalomarDirectMod palomar
) implements Serializable {
    public record PalomarDirectMod ( double expTime, double imageZPt, double airMass, double extinct) implements Serializable {}
}