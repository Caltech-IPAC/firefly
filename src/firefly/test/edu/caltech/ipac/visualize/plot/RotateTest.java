package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.firefly.ConfigTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * Created by zhang on 3/24/17.
 * This file contains the algorithm and matrix calculations.  They are kept for reference only.
 * The  testDo_rotate() and  testUnDo_rotate() are disabled since they are not needed.  But
 * in case the Rotate.java is modified, they can be used to as a  cross check since the matrix
 * calculation was directly written based on the algorithm. 
 *
 */
public class RotateTest extends ConfigTest{
    static final double degToArc = Math.PI / 180;
    static final double delta = 1.0e-10;
    private WorldPt point;
    Rotate rotate;
    @Before
    public void setUp()  {

       rotate = new Rotate();
       point = new WorldPt(146.1243333, 69.3023056); //get this point in m81 image in IRSA search
    }
    @After
    /**
     * Release the memories
     */
    public void tearDown() {

        rotate=null;
        point=null;
    }


    /** https://en.wikipedia.org/wiki/Rotation_matrix#General_rotations
     *
     * This is compute the z rotation (ange about the z axis)
       *
     *  Rx(angle)  =  {
     *      1              0                0
     *      0         cos(angle)     - sin(angle)
     *
     *      0         sin(angle)     cos(angle)
     *
     *  }
     *
     *  Ry(angle)  =  {
     *
     *      cos(angle)     0       sin(angle)
     *      0              1                0
     *    - sin(angle)      0        cos(angle)
     *
     *  }
     *
     * Rz(angle)  =  {
     *      cos(angle)     - sin(angle)     0
     *      sin(angle)     cos(angle)       0
     *      0              0                 1
     *  }
     *

     *  zVector = {
     *            0
     *            0
     *            1
     *            }
     *
     *
     *
     */
    private double[][] getRotationMatrix(double angleInArc, String coordinate){

        double[][] matrix = new double[3][3];
        switch (coordinate){
            case "z":
                matrix[0][0] = Math.cos(angleInArc);
                matrix[0][1] = -Math.sin(angleInArc);
                matrix[0][2] = 0.0;
                matrix[1][0] = Math.sin(angleInArc);
                matrix[1][1] = Math.cos(angleInArc);
                matrix[1][2] = 0.0;
                matrix[2][0] = 0.0;
                matrix[2][1] = 0.0;
                matrix[2][2] = 1.0;
                break;
            case "y":
                matrix[0][0] = Math.cos(angleInArc);
                matrix[0][1] =0.0;
                matrix[0][2] = Math.sin(angleInArc);
                matrix[1][0] = 0.0;
                matrix[1][1] = 1.0;
                matrix[1][2] = 0.0;
                matrix[2][0] = -Math.sin(angleInArc);
                matrix[2][1] =0.0;
                matrix[2][2] = Math.cos(angleInArc);
                break;
            case "x":
                matrix[0][0] = 1.0;
                matrix[0][1] =0.0;
                matrix[0][2] = 0.0;
                matrix[1][0] = 0.0;
                matrix[1][1] =  Math.cos(angleInArc) ;
                matrix[1][2] =  -Math.sin(angleInArc);
                matrix[2][0] = 0.0;
                matrix[2][1] = Math.sin(angleInArc);
                matrix[2][2] = Math.cos(angleInArc);
                break;

        }
        return matrix;
    }


    private double [][] multiplyMatrix(double[][] a, double [][] b) {

        double [][] c = new double[a.length][a[0].length];

        for (int i = 0; i < 3; i++) {

                for (int j = 0; j < 3; j++) {
                    {
                        for (int k = 0; k < 3; k++) {
                            c[i][j] += a[i][k] * b[k][j];
                        }
                    }
                }
            }
        return c;
     }
    private double[][] getRotationMatrix(double yRotationAngle, double zRotationAngle, String rotationDirection ){

        double[][] zRotaionMatrix = getRotationMatrix(zRotationAngle, "z");;
        double[][] yRotationMatrix =  getRotationMatrix(yRotationAngle, "y");

        if (rotationDirection.equalsIgnoreCase("counterClockWise")) {
            return multiplyMatrix(yRotationMatrix, zRotaionMatrix);
        }
        else {
            return multiplyMatrix( zRotaionMatrix, yRotationMatrix);
        }

    }


    /**
     * This is to test the rotation's Unit vector
     * This method is using the algorithm to calcualte the rotation.
     */
    public void testDo_rotate(){

        double lon = point.getLon();
        double lat = point.getLat();
        //define the angles in the same way as the Rotate.java class
        double zRotationAngle = (180.0 - lon) * degToArc;
        double yRotationAngle = - lat * degToArc;
        testRotation(point, yRotationAngle,zRotationAngle,"counterClockWise");

    }

    public void testDo_unRotate(){

        double lon = point.getLon();
        double lat = point.getLat();
        //define the angles in the same way as the Rotate.java class
        double zUnRotationAngle =-(180.0 - lon) * degToArc;
        double yUnrotationAngle = lat * degToArc;

        testRotation(point, yUnrotationAngle,zUnRotationAngle, "clockWise");
    }


    /**
     * http://mathworld.wolfram.com/SphericalCoordinates.html
     * First convert the spherical coordinates into cartesian coordinates, and then  project
     * them on to the xy plane.
     *
     * Express lon and lat in terms of Cartesian coordinates:
     * In terms of Cartesian coordinates,
     *
     * x = radius_of_world * Math.cos(longitude) * Math.cos(latitude)
     * y = radius_of_world * Math.sin(longitude) * Math.cos(latitude)
     * z = radius_of_world * Math.sin(latitude)
     */
    private void testRotation(WorldPt point, double yRotationAngle, double zRotationAngle, String rotationDirection){
        double lon = point.getLon();
        double lat = point.getLat();


        double[][]  rotationMatrix = getRotationMatrix(yRotationAngle,zRotationAngle,rotationDirection);

        //express the point in spherical coordinate in terms of x-y-z coordinates
        double x[] = new double[3];
        x[0] = Math.cos(lon*degToArc) * Math.cos(lat*degToArc);
        x[1] = Math.sin(lon*degToArc) * Math.cos(lat*degToArc);
        x[2] = Math.sin(lat*degToArc);

        //calculate the rotated vector
        double xp[] = new double[3];
        for (int i = 0; i < 3; ++i){
            xp[i] = 0.;
            for (int j = 0; j < 3; ++j){
                xp[i] += rotationMatrix[i][j] * x[j];
            }
        }

        if (xp[2] > 1.)
            xp[2] = 1.;

        if (xp[2] < -1.)
            xp[2] = -1.;

        double dec = Math.asin(xp[2]);
        double ra = Math.atan2(xp[1], xp[0]);

        if (ra < 0.)
            ra += 2.* Math.PI;


        WorldPt expectedWordPt = new WorldPt(ra / degToArc,  dec / degToArc);

        //compute the matrix
        rotate.compute_rotation_angles(point);

        //compute the rotated value
        WorldPt calcualtedWorldPt = rotationDirection.equalsIgnoreCase("counterClockWise")?rotate.do_rotate(point)
                :rotate.do_unrotate(point);

        Assert.assertEquals(expectedWordPt.getLon(), calcualtedWorldPt.getLon(),delta);
        Assert.assertEquals(expectedWordPt.getLat(), calcualtedWorldPt.getLat(),delta);


    }

    /**
     * From looking at the Rotate algorithm, for any given WorldPt (lon, lat), it rotates
     * (180-lon) and the -lat.
     *
     * Thus, after rotation, the final point is (-1, 0, 0).  The expected value is
     *  dec = Math.asin(xp[2]);
     *  ra = Math.atan2(xp[1], xp[0]);
     *
     */
    @Test
    public void testRotate(){

         //after rotation
         double[] rotatedPoint = {-1, 0, 0};
         double dec = Math.asin(rotatedPoint[2]);
         double ra = Math.atan2(rotatedPoint[1], rotatedPoint[0]);
         WorldPt expectedWordPt = new WorldPt(ra / degToArc,  dec /degToArc);


        //compute the matrix
        rotate.compute_rotation_angles(point);
        //validate do_rotate
        WorldPt calcualtedWorldPt = rotate.do_rotate(point);
        Assert.assertEquals(expectedWordPt.getLon(), calcualtedWorldPt.getLon(),delta);
        Assert.assertEquals(expectedWordPt.getLat(), calcualtedWorldPt.getLat(),delta);

        //validate do unrotate, the calculated point should be the same as input
        WorldPt calcualtedUnrotateWorldPt = rotate.do_unrotate(calcualtedWorldPt);

        Assert.assertEquals(point.getLon(), calcualtedUnrotateWorldPt.getLon(),delta);
        Assert.assertEquals(point.getLat(), calcualtedUnrotateWorldPt.getLat(),delta);

    }



}
