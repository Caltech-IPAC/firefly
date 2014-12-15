package edu.caltech.ipac.target;

/**
 * Util class for Position and Offsets calculation.
 * All the methods here are public istatic
 * @author Tatiana Goldina
 * @author Xiuqin Wu
 */
public class PositionUtil {

    /**
       This method calcualte position2 given Offsets with
      respect to position1.
       Position2 will have the same proper motion as position1.
       -xiuqin Wu

       @param pos1 of the first target in J2000 [degrees]
       @param  offset of the second target with respect to the first target
               [arcsec]
       @return  position of the second target in J2000 [degrees]
    */
    public static PositionJ2000 calculatePosition(PositionJ2000 pos1,
                                                  Offset offset) {
        double ra = Math.toRadians(pos1.getLon());
        double dec = Math.toRadians(pos1.getLat());
        double de = Math.toRadians(offset.getDeltaRaV()/3600.0); // east
        double dn = Math.toRadians(offset.getDeltaDecW())/3600.0; // north

        double cos_ra,sin_ra,cos_dec,sin_dec;
        double cos_de,sin_de,cos_dn,sin_dn;
        double rhat[] = new double[3];
        double shat[] = new double[3];
        double uhat[] = new double[3];
        double uxy;
        double ra2, dec2;

        cos_ra  = Math.cos(ra);
        sin_ra  = Math.sin(ra);
        cos_dec = Math.cos(dec);
        sin_dec = Math.sin(dec);

        cos_de = Math.cos(de);
        sin_de = Math.sin(de);
        cos_dn = Math.cos(dn);
        sin_dn = Math.sin(dn);


        rhat[0] = cos_de * cos_dn;
        rhat[1] = sin_de * cos_dn;
        rhat[2] = sin_dn;

        shat[0] = cos_dec * rhat[0] - sin_dec * rhat[2];
        shat[1] = rhat[1];
        shat[2] = sin_dec * rhat[0] + cos_dec * rhat[2];

        uhat[0] = cos_ra * shat[0] - sin_ra * shat[1];
        uhat[1] = sin_ra * shat[0] + cos_ra * shat[1];
        uhat[2] = shat[2];

        uxy = Math.sqrt(uhat[0] * uhat[0] + uhat[1] * uhat[1]);
        if (uxy>0.0)
           ra2 = Math.atan2(uhat[1],uhat[0]);
        else
           ra2 = 0.0;
        dec2 = Math.atan2(uhat[2],uxy);

        ra2  = Math.toDegrees(ra2);
        dec2 = Math.toDegrees(dec2);

        if (ra2 < 0.0) ra2 +=360.0;

        /*
        System.out.println("PositionUtil: " );
        System.out.println("ra: " + ra2);
        System.out.println("dec: " + dec2);
        */
        return (new PositionJ2000((float)ra2, (float)dec2,
                                   pos1.getProperMotion()) );

    }

    /**
     * Find the corners of a bounding box given the center and the radius
     * of a circle
     *
     * @param center the center of the circle
     * @param radius in arcsec
     * @return
     */
    public static Corners getCorners(PositionJ2000 center, double radius) {
        Offset left =  new Offset(center, +radius, 0.0);
        Offset right = new Offset(center, -radius, 0.0);
        Offset up =    new Offset(center, 0.0, +radius);
        Offset down =  new Offset(center, 0.0, -radius);
        PositionJ2000 pos_left = calculatePosition(center, left);
        PositionJ2000 pos_right = calculatePosition(center, right);
        PositionJ2000 pos_up = calculatePosition(center, up);
        PositionJ2000 pos_down = calculatePosition(center, down);
        PositionJ2000 upperLeft = new PositionJ2000(pos_left.getRa(), pos_up.getDec());
        PositionJ2000 upperRight = new PositionJ2000(pos_right.getRa(), pos_up.getDec());
        PositionJ2000 lowerLeft = new PositionJ2000(pos_left.getRa(), pos_down.getDec());
        PositionJ2000 lowerRight = new PositionJ2000(pos_right.getRa(), pos_down.getDec());

        return new Corners(upperLeft, upperRight, lowerLeft, lowerRight);
    }

    public static Corners getCorners(double ra, double dec, double radius) {
        return getCorners(new PositionJ2000(ra, dec), radius);
    }

    public static class Corners {
        PositionJ2000 upperLeft;
        PositionJ2000 upperRight;
        PositionJ2000 lowerLeft;
        PositionJ2000 lowerRight;

        public Corners(PositionJ2000 upperLeft, PositionJ2000 upperRight,
                       PositionJ2000 lowerLeft, PositionJ2000 lowerRight) {
            this.upperLeft = upperLeft;
            this.upperRight = upperRight;
            this.lowerLeft = lowerLeft;
            this.lowerRight = lowerRight;
        }

        public PositionJ2000 getUpperLeft() { return upperLeft; }
        public PositionJ2000 getUpperRight() { return upperRight; }
        public PositionJ2000 getLowerLeft() { return lowerLeft; }
        public PositionJ2000 getLowerRight() { return lowerRight; }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("upper_left RA = " + upperLeft.getRa() + "  DEC = ").append(upperLeft.getDec());
            sb.append("upper_right RA = " + upperRight.getRa() + "  DEC = ").append(upperRight.getDec());
            sb.append("lower_left RA = " + lowerLeft.getRa() + "  DEC = ").append(lowerLeft.getDec());
            sb.append("lower_right RA = " + lowerRight.getRa() + "  DEC = ").append(lowerRight.getDec());
            return sb.toString();
        }

        public static void main(String [] args)
        {
            double ra = 10.0;  // degrees
            double dec = 60.0;  // degrees
            double radius = 3600.0;  // arcsec
            System.out.println(getCorners(ra, dec, radius));
        }
    }

    /**
       Used for testing.
       Given J2000 coordinates of 2 positions,
       calculates offset of position2 with respect to position1
       on a specific date.
     */
//    public static void main(String [] args) {
//
//        if ( args.length < 9 ) {
//            System.err.println("Usage: java PositionUtil "+
//                      "ra1 dec1 pmRa1 pmDec1 ra2 dec2 pmRa2 pmDec2 date\n"+
//                      "[ra, dec are in degrees]; "+
//                      "[pmRa, pmDec are in arcsec/yr]; "+
//                      "date is in mm/dd/yyyy format");
//            System.exit(1);
//        }
//
//        double ra1, dec1, ra2, dec2;
//        float pmRa1, pmDec1, pmRa2, pmDec2;
//
//        try {
//
//            ra1 = Double.parseDouble(args[0]);
//            dec1 = Double.parseDouble(args[1]);
//            pmRa1 = Float.parseFloat(args[2]);
//            pmDec1 = Float.parseFloat(args[3]);
//            PositionJ2000 pos1 = new PositionJ2000(ra1, dec1,
//                                       new ProperMotion(pmRa1, pmDec1) );
//
//            ra2 = Double.parseDouble(args[4]);
//            dec2 = Double.parseDouble(args[5]);
//            pmRa2 = Float.parseFloat(args[6]);
//            pmDec2 = Float.parseFloat(args[7]);
//            PositionJ2000 pos2 = new PositionJ2000(ra2, dec2,
//                                       new ProperMotion(pmRa2, pmDec2) );
//
//            DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
//            Date dt = df.parse(args[8]);
//
//            Offset offset =
//                calculateOffset(pos1, pos2, dt);
//
//            System.out.println("On "+df.format(dt)+
//                               " offsetE [arcsec]="+offset.getDeltaRaV()+
//                               ", offsetN [arcsec]="+offset.getDeltaDecW());
//            PositionJ2000 p = calculatePosition(pos1, offset);
//            System.out.println(" ra  ="+p.getLon()+
//                               ",dec ="+p.getLat());
//        } catch (NumberFormatException nex) {
//            System.out.println("Make sure your parameter is valid number.");
//        } catch (Exception ex) {
//            System.out.println("Exception caught: "+ex.getMessage());
//            System.exit(1);
//        }
//        System.exit(0);
//    }

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
