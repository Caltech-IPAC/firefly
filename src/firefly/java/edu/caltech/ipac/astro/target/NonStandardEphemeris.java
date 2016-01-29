/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.target;

import edu.caltech.ipac.util.ComparisonUtil;

/**
 * ******************************************************************
 * Moving target with non-standard ephemeris.
 */
public class NonStandardEphemeris implements Ephemeris, java.io.Serializable {


    private int naifID = Integer.MIN_VALUE;

    /**
     * Ask X. Wu about member documentation @serial
     */
    private final String epoch;

    /**
     * Ask X. Wu about member documentation @serial
     */
    private final String t;

    /**
     * Eccentricity @serial
     */
    private final double e;

    /**
     * Perihelion distance @serial
     */
    private final double q;

    /**
     * Inclination of orbit @serial
     */
    private final double i;

    /**
     * Ask X. Wu about member documentation @serial
     */
    private final double littleOmega;

    /**
     * Ask X. Wu about member documentation @serial
     */
    private final double bigOmega;


    /**
     * Initialization constructor
     */
    public NonStandardEphemeris( String epoch, String t, double e, double q, 
			      double i, double littleOmega, double bigOmega ) {
        this.epoch = epoch;
        this.t = t;
        this.e = e;
        this.q = q;
        this.i = i;
       this.littleOmega = littleOmega;
        this.bigOmega = bigOmega;
    }

    /**
     * Return the dummy NAIF Id for the ephemeris
     */
    public int getNaifID() {
        return naifID;
    }

    public void setNaifID(int naifID) {
	this.naifID = naifID;
    }

    /**
     * Return the epoch
     */
    public String getEpoch() {
        return epoch;
    }


    /**
     * Get t
     */
    public String getT() {
        return t;
    }


    /**
     * Get e
     */
    public double getE() {
        return e;
    }


    /**
     * Get q
     */
    public double getQ() {
        return q;
    }

    /**
     * Get i
     */
    public double getI() {
        return i;
    }

    /**
     * Get little omega
     */
    public double getLittleOmega() {
        return littleOmega;
    }

    /**
     * Get big omega
     */
    public double getBigOmega() {
        return bigOmega;
    }

    /**
     * Implementation of the Cloneable interface
     */
    public Object clone() {
        return new NonStandardEphemeris( epoch, t, e, q, i, 
                                         littleOmega, bigOmega );
    }

    public boolean equals(Object o) {
       boolean retval= false;
       if (o==this) {
          retval= true;
       }
       else if (o!=null && o instanceof NonStandardEphemeris) {
          NonStandardEphemeris nse= (NonStandardEphemeris)o;
          retval=  ComparisonUtil.equals(epoch,nse.epoch) &&
                   ComparisonUtil.equals(t,nse.t) &&
                   (e == nse.e) &&
                   (q == nse.q) &&
                   (i == nse.i) &&
                   (littleOmega == nse.littleOmega) &&
                   (bigOmega == nse.bigOmega);
       }
       return retval;
    }
}

