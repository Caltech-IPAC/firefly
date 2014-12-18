package edu.caltech.ipac.firefly.visualize;


import edu.caltech.ipac.visualize.plot.WorldPt;

/**
 *
 * One offsets in a cluster. 
 *
 * @author Xiuqin Wu
 */
public class Offset implements Cloneable {

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

	private final WorldPt location;


    /**
     * Construct a Offset
     * @param location this location that this offset is for
     * @param deltaRaV the offset in the ra(lon) direction
     * @param deltaDecW the offset in the dec(lat) direction
     */
	public Offset(WorldPt location, double deltaRaV, double deltaDecW) {
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
	public WorldPt getLocation() { return location; }

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
