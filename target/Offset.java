package edu.caltech.ipac.target;


import java.io.Serializable;

/**
 *
 * One offsets in a cluster. 
 *
 * @author Xiuqin Wu
 */
public class Offset implements Location,
                               Cloneable, 
                               Serializable {

	/**
	 * offset in Ra(east) in sky coordinate or 
	 * v axis in array coordinate [arcsec]
	 */
	private final double deltaRaV;

	/**
	 * offset in Declination(north) in sky coordinate
	 * w axis in array coordinate [arcsec]
	 */
	private final double deltaDecW;

	private final Location location;


    /**
     * Construct a Offset
     * @param location this location that this offset is for
     * @param deltaRaV the offset in the ra(lon) direction
     * @param deltaDecW the offset in the dec(lat) direction
     */
	public Offset(Location location, double deltaRaV, double deltaDecW) {
        this.location = location;
		this.deltaRaV = deltaRaV;
		this.deltaDecW = deltaDecW;
	}


	/**
	 * return the deltaRaV,  user's input for RA(EAST) offset 
	 */
	public double getDeltaRaV() { return deltaRaV; }


	/**
	 * return the deltaDecW,  user's input for Dec(NORTH) offset 
	 */
	public double getDeltaDecW() { return deltaDecW; }

    /**
     * Get the location that this offset is for
     * @return a Location associated with this offset
     */
	public Location getLocation() { return location; }

	/**
	 * Implementation of the Cloneable interface
	 */
	public Object clone() {
       return new Offset(location, deltaRaV, deltaDecW);
	}

	public boolean equals(Object o) {
       boolean retval= false;
       if (o==this) {
          retval= true;
       }
       else if (o!=null && o instanceof Offset) {
          Offset other= (Offset)o;
          retval=  (deltaRaV ==other.deltaRaV &&
                    deltaDecW ==other.deltaDecW);
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
