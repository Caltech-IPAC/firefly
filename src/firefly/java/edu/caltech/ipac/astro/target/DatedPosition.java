/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.target;

import java.util.Date;

/**
 * @author roby
 * Date: Nov 5, 2004
 */
public class DatedPosition implements java.io.Serializable {
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
        * Date for which position is valid  @serial
        */
       private final Date date;



       /**
        * Initialization constructor
        */
       public DatedPosition( double raJ2000, double decJ2000, Date date) {
           this.raJ2000 = raJ2000;
           this.decJ2000 = decJ2000;
           this.date = date;
       }

       /**
        * Return the right ascension (degrees) in J2000 coordinates for this position.
        */
       public double getRaJ2000() {
           return raJ2000;
       }

       /**
        * Return the declination (degrees) in J2000 coordinates for this position.
        */
       public double getDecJ2000() {
           return decJ2000;
       }


       /**
        * Get the Date for this position.
        */
       public Date getDate( ) {
           return this.date;
       }


         /**
          * Perform a deep copy of this object.
          * @see Cloneable
          * @return Object reference to the cloned object
          */
       public Object clone() {
         DatedPosition dated_position = new DatedPosition(this.raJ2000,
             this.decJ2000, (Date)this.date.clone());
         return dated_position;
       }


   }


