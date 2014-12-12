package edu.caltech.ipac.target;


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
