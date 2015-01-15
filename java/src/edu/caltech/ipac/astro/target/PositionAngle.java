/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.target;


/**
 *
 */
public class PositionAngle implements java.io.Serializable {
    // class variable definitions //

    /**
     * RA sent to spacecraft [deg] @serial
     */
    private final double raJ2000;

    /**
     * Declination sent to spacecraft [deg] @serial
     */
    private final double decJ2000;

    /**
     * Position angle  @serial
     */
    private final double position_angle;


    /**
     * Initialization constructor
     */
    public PositionAngle( double raJ2000, 
                          double decJ2000, 
                          double position_angle) {
        this.raJ2000 = raJ2000;
        this.decJ2000 = decJ2000;
        this.position_angle = position_angle;
    }

    /**
     *Return the right acension (degrees) in J2000 coordnates for this position.
     */
    public double getRaJ2000() { return raJ2000; }

    /**
     * Return the declination (degrees) in J2000 coordinates for this position.
     */
    public double getDecJ2000() { return decJ2000; }
    /**
     * Get the position angle.
     */
    public double getPositionAngle( ) { return this.position_angle; }


	/**
	 * Perform a deep copy of this object.
	 * @see Cloneable
	 * @return Object reference to the cloned object
	 */
    public Object clone() {
	return new PositionAngle(raJ2000, decJ2000, position_angle);
    }

    public boolean equals(Object o) {
       boolean retval= false;
       if (o==this) {
          retval= true;
       }
       else if (o!=null && o instanceof PositionAngle) {
          PositionAngle pa= (PositionAngle)o;
              retval= (raJ2000==pa.raJ2000   &&
                       decJ2000==pa.decJ2000 &&
                       position_angle==pa.position_angle);

       }
       return retval;
    }

}

