/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

public class Rotate
{
    /**
     * DM-8845
     * While working on this ticket, the algorithm is added for later references.  From the code, this
     * class  first calculates the rotation matrices for any given World Point.  For rotation, it rotates
     * the world point to a position where (lon = 180-worldPt.lon) and lat = -worldPt.lat.  Thus the result
     * is the point (-1, 0, 0).  Once the rotation matrix is calculated, it is used for rotating other points.
     *
     *
     *
     *
     * Basic rotation
     * A basic rotation (also called elemental rotation) is a rotation about one of the axes of a
     * Coordinate system. The following three basic rotation matrices rotate vectors by an angle θ
     * about the x, y, or z axis, in three dimensions, using the right-hand rule—which codifies their
     * alternating signs. (The same matrices can also represent a clockwise rotation of the axes.[nb 1])
     *
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
     *      0              0                1
     *  }
     *
     *
     *
     *  For column vectors, each of these basic vector rotations appears counter-clockwise when the axis about which
     *  they occur points toward the observer, the coordinate system is right-handed, and the angle θ is positive.
     *  Rz, for instance, would rotate toward the y-axis a vector aligned with the x-axis, as can easily be checked
     *  by operating with Rz on the vector (1,0,0).
     *
     *
     */
    public static boolean debug = false;

static final double dtr = Math.PI / 180;
static final double rtd = 180 / Math.PI;

public static final double Plus90[][] = 
    {  { 0.000000000, 0.000000000, 1.000000000 },
       { 0.000000000, 1.000000000, 0.000000000 },
       { -1.000000000, 0.000000000, 0.000000000}};

public static final double Minus90[][] = 
    {  { 0.000000000, 0.000000000, -1.000000000 },
       { 0.000000000, 1.000000000, 0.000000000 },
       { 1.000000000, 0.000000000, 0.000000000}};

public static final double Plus180[][] = 
    {  { -1.000000000, 0.000000000, 0.000000000 },
       { 0.000000000, -1.000000000, 0.000000000 },
       { 0.000000000, 0.000000000, 1.000000000}};

private double final_rotation[][];
private double final_unrotation[][];


public static void main(String[] args) 
{
    debug = true;
    if (args.length != 2)
    {
	System.out.println("Usage java Rotate <lon> <lat>");
	System.exit(1);
    }
    double lon = Double.valueOf(args[0]);
    double lat = Double.valueOf(args[1]);
    WorldPt input = new WorldPt(lon, lat);

    Rotate rotate = new Rotate();
    rotate.compute_rotation_angles(input);


    WorldPt final_rotated_point = rotate.do_rotate(input);
    System.out.println("final rotated lon = " + final_rotated_point.getLon() + 
	"  lat = " + final_rotated_point.getLat());
    WorldPt final_unrotated_point = rotate.do_unrotate(final_rotated_point);
    System.out.println("final unrotated lon = " + final_unrotated_point.getLon() + 
	"  lat = " + final_unrotated_point.getLat());
}

/**
 * This is compute the z rotation
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
 */
public void compute_rotation_angles(WorldPt point)
{
    double lon = point.getLon();
    double lat = point.getLat();
    double z_rotation[][] = new double[3][3];
    double z_unrotation[][] = new double[3][3];
    double z_rotation_angle = (180.0 - lon) * dtr;
    double z_unrotation_angle = -(180.0 - lon) * dtr;
    /* rotation around z axis */
    z_rotation[0][0] = Math.cos(z_rotation_angle);
    z_rotation[0][1] = -Math.sin(z_rotation_angle);
    z_rotation[0][2] = 0.0;
    z_rotation[1][0] = Math.sin(z_rotation_angle);
    z_rotation[1][1] = Math.cos(z_rotation_angle);
    z_rotation[1][2] = 0.0;
    z_rotation[2][0] = 0.0;
    z_rotation[2][1] = 0.0;
    z_rotation[2][2] = 1.0;
    z_unrotation[0][0] = Math.cos(z_unrotation_angle);
    z_unrotation[0][1] = -Math.sin(z_unrotation_angle);
    z_unrotation[0][2] = 0.0;
    z_unrotation[1][0] = Math.sin(z_unrotation_angle);
    z_unrotation[1][1] = Math.cos(z_unrotation_angle);
    z_unrotation[1][2] = 0.0;
    z_unrotation[2][0] = 0.0;
    z_unrotation[2][1] = 0.0;
    z_unrotation[2][2] = 1.0;



    /*
    WorldPt z_rotated_point = this.do_rotate(point, z_rotation);
    System.out.println("z rotated lon = " + z_rotated_point.getLon() + 
	"  lat = " + z_rotated_point.getLat());
    */

    double y_rotation[][] = new double[3][3];
    double y_unrotation[][] = new double[3][3];
    double y_rotation_angle = - lat * dtr;
    double y_unrotation_angle = lat * dtr;
    /* rotation around y axis */
    y_rotation[0][0] = Math.cos(y_rotation_angle);
    y_rotation[0][1] = 0.0;
    y_rotation[0][2] = Math.sin(y_rotation_angle);
    y_rotation[1][0] = 0.0;
    y_rotation[1][1] = 1.0;
    y_rotation[1][2] = 0.0;
    y_rotation[2][0] = -Math.sin(y_rotation_angle);
    y_rotation[2][1] = 0.0;
    y_rotation[2][2] = Math.cos(y_rotation_angle);
    y_unrotation[0][0] = Math.cos(y_unrotation_angle);
    y_unrotation[0][1] = 0.0;
    y_unrotation[0][2] = Math.sin(y_unrotation_angle);
    y_unrotation[1][0] = 0.0;
    y_unrotation[1][1] = 1.0;
    y_unrotation[1][2] = 0.0;
    y_unrotation[2][0] = -Math.sin(y_unrotation_angle);
    y_unrotation[2][1] = 0.0;
    y_unrotation[2][2] = Math.cos(y_unrotation_angle);

    /*
    WorldPt y_rotated_point = this.do_rotate(z_rotated_point, y_rotation);
    System.out.println("y rotated lon = " + y_rotated_point.getLon() + 
	"  lat = " + y_rotated_point.getLat());

    WorldPt y_unrotated_point = this.do_rotate(y_rotated_point, y_unrotation);
    System.out.println("y unrotated lon = " + y_unrotated_point.getLon() + 
	"  lat = " + y_unrotated_point.getLat());
    WorldPt z_unrotated_point = this.do_rotate(y_unrotated_point, z_unrotation);
    System.out.println("z unrotated lon = " + z_unrotated_point.getLon() + 
	"  lat = " + z_unrotated_point.getLat());
    */
    


    /* for final rotation, matrix multiply the z and y rotation matrices */
    final_rotation = new double[3][3];
    final_unrotation = new double[3][3];
    final_rotation = matrix_mult(y_rotation, z_rotation);
    final_unrotation = matrix_mult(z_unrotation, y_unrotation);
}

private double [][] matrix_mult(double[][] a, double [][] b)
{
    double [][] c = new double[a.length][a[0].length];

    for (int i = 0; i < 3; i++)
    {
	for (int j = 0; j < 3; j++)
	{
	    for (int k = 0; k < 3; k++)
	    {
		c[i][j] += a[i][k] * b[k][j];
	    }
	}
    }

    /*
    int i = 0;
    while (i < a.length)
    {
	int j = 0;
	while (j < a[0].length)
	{
	    c[i][j] = a[i][j] * b[i][j];
	    j++;
	}
	i++;
    }
    */
    return c;
}


public WorldPt do_unrotate(WorldPt coord)
{
    return do_rotate(coord, this.final_unrotation);
}

public WorldPt do_rotate(WorldPt coord)
{
    return do_rotate(coord, this.final_rotation);
}

private WorldPt do_rotate(WorldPt coord, double A[][])
{
    int             i, j;
    double          ra, dec, lat, lon;


    if (debug)
    {
    System.out.println("rotate input lon = " + coord.getLon() + "  lat = " + coord.getLat());
    }
    lon = coord.getLon() * dtr;
    lat = coord.getLat() * dtr;

    double x[] = new double[3]; 
    x[0] = Math.cos(lon) * Math.cos(lat);
    x[1] = Math.sin(lon) * Math.cos(lat);
    x[2] = Math.sin(lat);
    double xp[] = new double[3]; 

    for (i = 0; i < 3; ++i)
    {
	xp[i] = 0.;
	for (j = 0; j < 3; ++j)
	{
	    xp[i] += A[i][j] * x[j];
	}
    }

    if (xp[2] > 1.)
	xp[2] = 1.;

    if (xp[2] < -1.)
	xp[2] = -1.;

    dec = Math.asin(xp[2]);
    ra = Math.atan2(xp[1], xp[0]);

    if (ra < 0.)
	ra += 2.* Math.PI;

    if (debug)
    {
    System.out.println("   rotate output lon = " + ra/dtr + "  lat = " + dec/dtr);
    }
    WorldPt result = new WorldPt(ra / dtr,  dec / dtr);
    return result;

}
}
