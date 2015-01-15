/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.target;


import java.io.Serializable;

public final class ProperMotion implements Serializable, Cloneable {

    /**
     * Proper motion in RA [arcsec/yr] 
     */
    private final float lonPm;

    /**
     * Proper motion in Dec [arcsec/yr]
     */
    private final float latPm;

    /**
     *
     * @param lonPm Proper motion in lon [arcsec/yr]
     * @param latPm Proper motion in lat [arcsec/yr]
     */
    public ProperMotion(float lonPm, float latPm) {
        this.lonPm = lonPm;
        this.latPm = latPm;
    }

    /**
     * Proper motion in Lon [arcsec/yr]
     */
    public float getLonPm() { return lonPm; }

    /**
     * Proper motion in Lat [arcsec/yr]
     */
    public float getLatPm() { return latPm; }

    public boolean equals(Object o) {
       boolean retval= false;

       if (o==this) {
            retval= true;
       }
       else if (o!=null && o instanceof ProperMotion) {
           ProperMotion pm= (ProperMotion)o;
           retval=  ( lonPm == pm.lonPm && latPm == pm.latPm);
       }
       return retval;
    }

    public Object clone() { return new ProperMotion(lonPm, latPm); }
}
