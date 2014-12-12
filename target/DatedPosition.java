package edu.caltech.ipac.target;

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
        * Return the right acension (degrees) in J2000 coordnates for this position.
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
