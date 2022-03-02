/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import java.io.Serializable;

/**
 * @author Trey Roby
 */
public class WebFitsData implements Serializable {

    private final double dataMin;
    private final double dataMax;
    private final long   fitsFileSize;
    private final String fluxUnits;

    public WebFitsData(double dataMin,
                       double dataMax,
                       long   fitsFileSize,
                       String fluxUnits ) {
        this.dataMin= dataMin;
        this.dataMax= dataMax;
        this.fitsFileSize= fitsFileSize;
        this.fluxUnits= fluxUnits;

    }

    public double getDataMin()      { return dataMin; }
    public double getDataMax()      { return dataMax; }

    /**
     * Return the size of the fits file this image was created from
     * @return size of the file
     */
    public long   getFitsFileSize() { return fitsFileSize; }
    
    /**
     * get units that this flux data is in.
     * @return String the units.
     */
    public String getFluxUnits() { return fluxUnits; }

    @Override
    public String toString() {
        String SPLIT_TOKEN= "--WebFitsData--";
        return dataMin + SPLIT_TOKEN + dataMax + SPLIT_TOKEN + fitsFileSize + SPLIT_TOKEN + fluxUnits;
    }
}

