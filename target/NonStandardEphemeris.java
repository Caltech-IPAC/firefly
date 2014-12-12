package edu.caltech.ipac.target;

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
     * Implemenation of the Clonable interface
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

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
