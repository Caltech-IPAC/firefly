package edu.caltech.ipac.target;


/**
 * ****************************************************************************************************************************
 * One flux estimate for one point in the sky at one wavelength.
 * <BR>
 * Copyright (C) 1999 California Institute of Technology. All rights reserved.<BR>
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged.
 * <BR>
 * @version $Id: FluxEstimate.java,v 1.2 2005/12/08 22:31:03 tatianag Exp $
 * @author <a href="mailto:jchavez@ipac.caltech.edu?subject=Java Docs">Joe Chavez</a>
 */

public class FluxEstimate implements Cloneable, java.io.Serializable {

    /**
     * Estimated flux density [mjy] @serial
     */
    private final float fluxD;

    /**
     * Wavelength of estimated flux density [micron] @serial
     */
    private final float wavelength;


    /**
     * Initialization constructor.
     * @param fluxD Estimated flux density [mjy]
     * @param wavelength Wavelength of estimated flux density [micron]
     */
    public FluxEstimate( float fluxD, float wavelength ) {
        this.fluxD = fluxD;
        this.wavelength = wavelength;
    }

    /**
     * get flux density
     */
    public float getFluxD() {
        return fluxD;
    }

    /**
     * get wavelength
     */
    public float getWavelength() {
        return wavelength;
    }


    /**
     * Implementation of the Cloneable interface.
     */
    public Object clone() {
        return new FluxEstimate( this.fluxD, this.wavelength );
    }

    public boolean equals(Object o) {
       boolean retval= false;
       if (o==this) {
          retval= true;
       }
       else if (o!=null && o instanceof FluxEstimate) {
          FluxEstimate f= (FluxEstimate)o;
              retval= (fluxD==f.fluxD && wavelength==f.wavelength);
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
