package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.firefly.visualize.VisUtil;

import java.util.ArrayList;
import java.util.List;


/** Find central point and radius of the minimum circle  
/* enclosing all of the input points 
<p>
The following excerpt from 
<code> <a href="http://www.delphiforfun.org/programs/circle_covering_points.htm">http://www.delphiforfun.org/programs/circle_covering_points.htm</a>
explains the algorithm


<p><font color="#000000">The most quoted algorithm
for solving the problem is by&nbsp; professors&nbsp; Elzinga and Hearn who in 1972 published&nbsp;</font>
a geometric algorithm for solving this problem and proved the correctness of the
algorithm.&nbsp; <code><i><b>(D. Jack Elzinga, Donald W.
Hearn, &quot;Geometrical Solutions for some minimax location problems,&quot;&nbsp;
Transportation Science, 6, (1972), pp 379 - 394.)<font color="#003399"><br>
</font>
</b></i></code></p>
<h4>Elzinga-Hearn Algorithm</h4>
<h4><code><i><b>Algorithm extracted from: <a href="http://www.eng.clemson.edu/~pmdrn/Dearing/location/minimax.pdf">http://www.eng.clemson.edu/~pmdrn/Dearing/location/minimax.pdf</a>
{no author information available}<br>
</b></i></code></h4>
<ol>
  <li>
    <p ALIGN="LEFT"><i>Choose any two points, P</i><b><sub>i</sub></b><i> and P</i><b><sub>j</sub></b></li>
  <li>
    <p ALIGN="LEFT"><i>Construct the circle whose center is at the midpoint of
    the line connecting P</i><b><sub>i</sub></b><i> and P</i><b><sub>j</sub></b><i>
    and which passes through P</i><b><sub>i </sub></b><i>and P</i><b><sub>j</sub></b><i>.&nbsp;
    If this circle contains all points, then the center of the circle is the
    optimal X. Otherwise, choose a point P</i><b><sub>k</sub></b><i> outside the circle.&nbsp;</i></li>
  <li>
    <p ALIGN="LEFT"><i>If the triangle determined by P</i><b><sub>i</sub>,</b><i>
    P</i><b><sub>j</sub></b><i> and P</i><b><sub>k</sub></b><i> is a right triangle or an obtuse triangle, rename the two points
    opposite the right angle or the obtuse angle as P</i><b><sub>i</sub></b><i> and
    P</i><b><sub>j</sub></b><i>&nbsp;
    and go to step 2.&nbsp; Otherwise, the three points determine an acute triangle.
    Construct the circle passing through the three points. (The center is the
    intersection of the perpendicular bisectors of two sides of the triangle.)
    If the circle contains all the points, stop, else, go to 4.</i></li>
  <li>
    <p ALIGN="LEFT"><i>Choose some point P</i><b><sub>l</sub></b><i> not in the circle, and let Q be
    the point among {P</i><b><sub>i</sub></b><i>, P</i><b><sub>j</sub></b><i>, P</i><b><sub>k</sub></b><i>} that is greatest distance
    from Pl. Extend the diameter (from the circle center) through the point Q into a line
    that divides the plane into two half planes. Let the point R be the
    point among {P</i><b><sub>i</sub></b><i>, P</i><b><sub>j</sub></b><i>, P</i><b><sub>k</sub></b><i>} that is in the half plane
    opposite P</i><b><sub>l</sub></b><i>. With the points Q, R, and P</i><b><sub>l</sub></b><i>, go to
    step 3.</i></li>
</ol>



 */

public class CentralPoint
{


static final double rtd=180./Math.PI;
static final double dtr=Math.PI/180.;
static boolean debug = false;

static void usage()
{

   System.out.println("Usage: CentralPoint");


   System.exit(0);
}


    // main is for testing only
    public static void main(String[] args)
    {

    boolean pole = false;
    debug = true;

    List<ImageCorners> corners = new ArrayList<ImageCorners>();
    if (pole)
    {
	ImageCorners image0_corners = new ImageCorners(
	    new WorldPt(0,   85),
	    new WorldPt(90,  85),
	    new WorldPt(180, 85),
	    new WorldPt(270, 85));
	corners.add(image0_corners);
	ImageCorners image1_corners = new ImageCorners(
	    new WorldPt(45,  89),
	    new WorldPt(80,  89),
	    new WorldPt(80,  80),
	    new WorldPt(45,  80));
	corners.add(image1_corners);
	ImageCorners image2_corners = new ImageCorners(
	    new WorldPt(135, 89),
	    new WorldPt(180, 89),
	    new WorldPt(180, 80),
	    new WorldPt(135, 80));
	//corners.add(image2_corners);
    }
    else
    {
	ImageCorners image0_corners = new ImageCorners(
	    new WorldPt(10, 50),
	    new WorldPt(10, 10),
	    new WorldPt(50, 10),
	    new WorldPt(50, 50));
	corners.add(image0_corners);
	ImageCorners image1_corners = new ImageCorners(
	    new WorldPt(30, 60),
	    new WorldPt(60, 30),
	    new WorldPt(90, 60),
	    new WorldPt(60, 80));
	corners.add(image1_corners);
	ImageCorners image2_corners = new ImageCorners(
	    new WorldPt(30, 20),
	    new WorldPt(30, 70),
	    new WorldPt(80, 70),
	    new WorldPt(80, 20));
	//corners.add(image2_corners);
	ImageCorners image3_corners = new ImageCorners(
	    new WorldPt(95, 20),
	    new WorldPt(95, 40),
	    new WorldPt(105, 40),
	    new WorldPt(105, 20));
	corners.add(image3_corners);
    }


	// make a list P of all the points
	ArrayList<WorldPt> P = new ArrayList<WorldPt>();
	for(ImageCorners c : corners)
	{
	    System.out.println("\nRBH Next Polygon");
	    /* c is one polygon */
	    for (WorldPt point: c.getCorners())
	    {
		System.out.println("  lon = " + point.getLon() + 
		    "  lat = " + point.getLat());
		P.add(point);
	    }
	}


	CentralPoint central_point = new CentralPoint();
	Circle circle = null;
	try
	{
	    circle = central_point.find_circle(P);
	}
	catch (CircleException ce)
	{
	    System.out.println("got CircleException :\n" + ce);
	}
	WorldPt center = circle.getCenter();
	double radius = circle.getRadius();
	System.out.println("center is at lon = " + center.getLon() +
	    "  lat = " + center.getLat() + "  radius = " + radius);
	

	System.out.println("  ****   DISTANCES  ***");
	for (WorldPt point : P)
	{
	    double a_distance = central_point.computeDistance(center.getLon(),
		center.getLat(), point.getLon(), point.getLat());
	    double exact_distance = VisUtil.computeDistance(center.getLon(), center.getLat(),
														point.getLon(), point.getLat());
	    System.out.printf(
	    "lon = %9.5f lat = %8.4f  crude dist = %9.5f  exact_dist = %9.5f\n",
	    point.getLon(), point.getLat(), a_distance, exact_distance);
	    //System.out.println("  lon = " + point.getLon() + 
	//	"  lat = " + point.getLat() + "  distance = " + a_distance +
	//	"  exact_distance = " + exact_distance);
	}
    }

    public Circle find_circle(List<WorldPt> P) throws CircleException
    {
	if (debug)
	{
	    System.out.println("entering CentralPoint.find_circle()  P.size() = " + P.size());
	}

	if (true)
	{
	/* THROW AWAY code follows - 11/27/11, no longer THROW AWAY */
	/* find extremes of ra and dec */
	WorldPt one_point = P.get(0);
	double max_ra = one_point.getLon();
	double min_ra = max_ra;
	double max_dec = one_point.getLat();
	double min_dec = max_dec;
	for (int i = 1; i < P.size(); i++)
	{
	    one_point = P.get(i);
	    if (one_point.getLon() > max_ra)
		max_ra = one_point.getLon();
	    else if (one_point.getLon() < min_ra)
		min_ra = one_point.getLon();
	    if (one_point.getLat() > max_dec)
		max_dec = one_point.getLat();
	    else if (one_point.getLat() < min_dec)
		min_dec = one_point.getLat();
	}
	double center_ra = (max_ra + min_ra) / 2;
	double center_dec = (max_dec + min_dec) / 2;
	if (debug)
	{
	System.out.println("max_ra = " + max_ra + "  min_ra = " + min_ra +
	    "  max_dec = " + max_dec + "  min_dec = " + min_dec +
	    "  center_ra = " + center_ra + "  center_dec = " + center_dec);
	}

	/* set radius to the maximum distance to a point */
	one_point = P.get(0);
	double radius = computeDistance(one_point.getLon(), 
	    one_point.getLat(), center_ra, center_dec);
	for (int i = 1; i < P.size(); i++)
	{
	    one_point = P.get(i);
	    double new_radius = computeDistance(one_point.getLon(), 
	    	one_point.getLat(), center_ra, center_dec);
	    if (new_radius > radius)
		radius = new_radius;
	}

	WorldPt center_point = new WorldPt(center_ra, center_dec);
	return new Circle(center_point, radius);

	/* end of THROW AWAY code */
	}

	/* start Elzinga-Hearn Algorithm */

	int next_step = 1;
	int i = 0;
	int j = 0;
	int k = 0;
	int l = 0;
	WorldPt center = null;
	double radius = Double.NaN;
	double center_ra = Double.NaN;
	double center_dec = Double.NaN;

	double ra_i = Double.NaN; 
	double dec_i = Double.NaN;
	double ra_j = Double.NaN;
	double dec_j = Double.NaN;
	double ra_k = Double.NaN;
	double dec_k = Double.NaN;


	boolean found = false;

	while (!found)
	{
	switch(next_step)
	{
	case 1:
	    /* step 1 */
	    /* choose points 0 and 1 */
	    i = 0;
	    j = 1;
	    next_step = 2;
	    break;

	case 2:
	    /* step 2 */
	    /* construct the circle defined by these two points */
	    ra_i = P.get(i).getLon();
	    dec_i = P.get(i).getLat();
	    ra_j = P.get(j).getLon();
	    dec_j = P.get(j).getLat();
	    double distance = 
		computeDistance(ra_i, dec_i, ra_j, dec_j);
	    if (debug)
	    {
	    System.out.println("distance = " + distance);
	    System.out.println("ra_i = " + ra_i + "  dec_i = " + dec_i +
		"  ra_j = " + ra_j + "  dec_j = " + dec_j);
	    }

	    if (false)
	    {
		double pa = VisUtil.getPositionAngle(ra_i, dec_i, ra_j, dec_j);
		center = posdis(ra_i, dec_i, distance/2, pa);
		center_ra = center.getLon();
		center_dec = center.getLat();
	    }
	    if (true)
	    {
		center_ra = (ra_i + ra_j) / 2;
		center_dec = (dec_i + dec_j) / 2;
		center = new WorldPt(center_ra, center_dec);
	    }

	    radius = computeDistance(
		center_ra, center_dec, ra_i, dec_i);
	    if (debug)
	    {
	    System.out.println("step 2 circle center_ra = " + center_ra +
	    "  center_dec = " + center_dec + "  radius = " + radius);
	    }

	    /* see if all points are within this circle */
	    found = true;
	    for (k = 0; k < P.size(); k++)
	    {
		if ((k == i) || (k == j))
		    continue;
		WorldPt one_point = P.get(k);
		double this_distance = 
		    computeDistance(one_point.getLon(), 
		    one_point.getLat(), center_ra, center_dec);
		if (debug)
		{
		System.out.println("testing point k = " + k + 
		    "  this_distance = " + this_distance);
		}
		if (this_distance > radius)
		{
		    /* k is a point outside of circle */
		    if (debug)
		    {
		    System.out.println("  k = " + k + "  is outside the circle");
		    }
		    found = false;
		    break;
		}
	    }
	    if (found)
		return new Circle(center, radius);
	    else
		next_step = 3;
	    break;
	case 3:
	    /* step 3 */
	    /* is triangle a right triangle or an obtuse triangle */
	    ra_i = P.get(i).getLon();
	    dec_i = P.get(i).getLat();
	    ra_j = P.get(j).getLon();
	    dec_j = P.get(j).getLat();
	    ra_k = P.get(k).getLon();
	    dec_k = P.get(k).getLat();

	    double side_ij = computeDistance(ra_i, dec_i, ra_j, dec_j);
	    double side_jk = computeDistance(ra_j, dec_j, ra_k, dec_k);
	    double side_ki = computeDistance(ra_k, dec_k, ra_i, dec_i);
	    /* formula for angle */
	    /* cos(A) = (b^2 + c^2 - a^2) / 2*b*c    */
	    /* But we don't really need to compute the acos().  All we need */
	    /* is to know if the angle is >= 90.  We can tell that by */
	    /* looking at the sign of the argument to the acos() function. */
	    /* If the sign is negative, then the angle is > 90 degrees */
	    double argument_I = 
		(side_ij * side_ij + side_ki * side_ki - side_jk * side_jk) /
		(2 * side_ij * side_ki);
	    /*
	    double argument = argument_I;
	    if (argument > 1.0)
		argument = 1.0;
	    if (argument < -1.0)
		argument = -1.0;
	    double angle_I = Math.acos(argument);
	    */

	    double argument_J = 
		(side_ij * side_ij + side_jk * side_jk - side_ki * side_ki) /
		(2 * side_ij * side_jk);
	    /*
	    argument = argument_J;
	    if (argument > 1.0)
		argument = 1.0;
	    if (argument < -1.0)
		argument = -1.0;
	    double angle_J = Math.acos(argument);
	    */

	    double argument_K = 
		(side_jk * side_jk + side_ki * side_ki - side_ij * side_ij) /
		(2 * side_jk * side_ki);
	    /*
	    argument = argument_K;
	    if (argument > 1.0)
		argument = 1.0;
	    if (argument < -1.0)
		argument = -1.0;
	    double angle_K = Math.acos(argument);
	    */
	    if (debug)
	    {

	    System.out.println("i = " + i + "  j = " + j + "  k = " + k);
	    System.out.println("ra_i = " + ra_i + 
	    "  dec_i = " + dec_i +
	    "  ra_j = " + ra_j +
	    "  dec_j = " + dec_j +
	    "  ra_k = " + ra_k +
	    "  dec_k = " + dec_k );
	    System.out.println("side_ij = " + side_ij + "  side_jk = " + side_jk +
	    "  side_ki = " + side_ki );
	    }
	    if (argument_I <= 0.0)
	    {
		if (debug)
		{
		System.out.println("angle_I is greater than 90");
		}
		i = k;
		next_step = 2;
	    }
	    else if (argument_J <= 0.0)
	    {
		if (debug)
		{
		System.out.println("angle_J is greater than 90");
		}
		j = k;
		next_step = 2;
	    }
	    else if (argument_K <= 0.0)
	    {
		if (debug)
		{
		System.out.println("angle_k is greater than 90");
		}
		next_step = 2;
	    }
	    else
	    {
		if (debug)
		{
		System.out.println("calling find_center");  
		}
		Circle circle = find_center(ra_i, dec_i, ra_j, dec_j, ra_k, dec_k);
		center = circle.getCenter();
		center_ra = center.getLon();
		center_dec = center.getLat();
		radius = circle.getRadius();

		double distance_i = computeDistance(center_ra, center_dec,
		    ra_i, dec_i);
		double distance_j = computeDistance(center_ra, center_dec,
		    ra_j, dec_j);
		double distance_k = computeDistance(center_ra, center_dec,
		    ra_k, dec_k);
		if (debug)
		{
		System.out.println("find_center results:  center_ra = " +
		    center_ra + "  center_dec = " + center_dec +
		    "  radius = " + radius);  
		System.out.println("  distance_i = " + distance_i +
		    "  distance_j = " + distance_j + "  distance_k = " + distance_k);
		}
		/* see if all points are within this circle */
		found = true;
		for (l = 0; l < P.size(); l++)
		{
		    if ((l == i) || (l == j) || (l == k))
			continue;
		    WorldPt one_point = P.get(l);
		    double this_distance = 
			computeDistance(one_point.getLon(), 
			one_point.getLat(), center_ra, center_dec);
		    if (debug)
		    {
		    System.out.println("testing point l = " + l + 
			"  this_distance = " + this_distance);
		    }
		    if ((this_distance > radius) && (!CombinePolygons.eps_compare(this_distance, radius)))
		    {
			/* l is a point outside of circle by more than eps */
			if (debug)
			{
			System.out.println("  l = " + l + "  is outside the circle");
			}
			found = false;
			break;
		    }
		}
		if (found)
		    return new Circle(center, radius);
		else
		    next_step = 4;

	    }
	    break;
	case 4:
	    if (debug)
	    {
	    System.out.println("entering step 4");
	    }
	    /* find the point farthest from l */
	    int m;
	    int q;
	    WorldPt point_l = P.get(l);
	    double ra_l = point_l.getLon();
	    double dec_l = point_l.getLat();
	    double greatest_distance = 0.0;
	    double ra_q = Double.NaN;
	    double dec_q = Double.NaN;

	    if (computeDistance(ra_l, dec_l, ra_i, dec_i) >
		computeDistance(ra_l, dec_l, ra_j, dec_j))
	    {
		q = i;
		ra_q = ra_i;
		dec_q = dec_i;
	    }
	    else
	    {
		q = j;
		ra_q = ra_j;
		dec_q = dec_j;
	    }
	    if (computeDistance(ra_l, dec_l, ra_k, dec_k) >
		computeDistance(ra_l, dec_l, ra_q, dec_q))
	    {
		q = k;
		ra_q = ra_k;
		dec_q = dec_k;
	    }
	    if (debug)
	    {
	    System.out.println("most distant point q = " + q);
	    }

	    /* now find half plane */
	    if (q == i)
	    {
		if (sameside(center_ra, center_dec, ra_q, dec_q, ra_l, dec_l,
		    ra_j, dec_j))
		{
		    if (debug)
		    {
		    System.out.println("picking k (in opposite halfplane");
		    }
		    j = k;
		}
		else
		{ 
		    if (debug)
		    {
		    System.out.println("picking j (in opposite halfplane");
		    }
		    j = j;
		}
	    }
	    else if (q == j)
	    {
		if (sameside(center_ra, center_dec, ra_q, dec_q, ra_l, dec_l,
		    ra_k, dec_k))
		{
		    if (debug)
		    {
		    System.out.println("picking i (in opposite halfplane");
		    }
		    j = i;
		}
		else
		{ 
		    if (debug)
		    {
		    System.out.println("picking k (in opposite halfplane");
		    }
		    j = k;
		}
	    }
	    else if (q == k)
	    {
		if (sameside(center_ra, center_dec, ra_q, dec_q, ra_l, dec_l,
		    ra_j, dec_j))
		{
		    if (debug)
		    {
		    System.out.println("picking i (in opposite halfplane");
		    }
		    j = i;
		}
		else
		{ 
		    if (debug)
		    {
		    System.out.println("picking j (in opposite halfplane");
		    }
		    j = j;
		}
	    }
	    i = q;
	    k = l;

	    next_step = 3;
	    break;
	}
	}


	//WorldPt center_point = new WorldPt(center_ra, center_dec);
	return new Circle(center, radius);


    }

    private boolean sameside(double center_ra, double center_dec, 
	double ra_q, double dec_q, double ra_1, double dec_1,
	double ra_2, double dec_2)
    {
	boolean answer;
	double dx = ra_q - center_ra;
	double dy = dec_q - center_dec;
	double dx1 = ra_1 - center_ra;
	double dy1 = dec_1 - center_dec;
	double dx2 = ra_2 - ra_q;
	double dy2 = dec_2 - dec_q;
	double result = (dx*dy1-dy*dx1)*(dx*dy2-dy*dx2);
	if (result > 0)
	    answer = true;
	else
	    answer = false;
	return answer;
    }

    public Circle find_center(double ra_i, double dec_i, double ra_j, 
	double dec_j, double ra_k, double dec_k)  throws CircleException
    {
	double temp;
	double ma, mb;
	double x, y;
	double radius;

	/* avoid infinite slopes or 0 slopes */
	if ((ra_i == ra_j) || (dec_i == dec_j))
	{
	    //swap i and k
	    temp = ra_i;
	    ra_i = ra_k;
	    ra_k = temp;
	    temp = dec_i;
	    dec_i = dec_k;
	    dec_k = temp;
	}
	if (ra_j == ra_k)
	{
	    //swap i and j
	    temp = ra_i;
	    ra_i = ra_j;
	    ra_j = temp;
	    temp = dec_i;
	    dec_i = dec_j;
	    dec_j = temp;
	}

	if (ra_i != ra_j)
	{
	    ma = (dec_j - dec_i) / (ra_j - ra_i);
	}
	else
	{
	    throw new CircleException();
	}
	if (ra_j != ra_k)
	{
	    mb = (dec_k - dec_j) / (ra_k - ra_j);
	}
	else
	{
	    throw new CircleException();
	}
	if ((ma == 0.0) && (mb == 0.0))
	    throw new CircleException();
	if (ma == mb)
	    throw new CircleException();
	x = (ma * mb * (dec_i - dec_k) + mb * (ra_i + ra_j) 
	    - ma * (ra_j + ra_k)) / (2 * (mb - ma));
	if (ma != 0.0)
	{
	    y = -(x-(ra_i + ra_j)/2) / ma + (dec_i + dec_j) / 2;
	}
	else
	{
	    y = - (x-(ra_j + ra_k)/2)/ mb + (dec_j + dec_k) / 2;
	}
	radius = computeDistance(x, y, ra_i, dec_i);
	return new Circle(new WorldPt(x, y), radius);

    }

    private double computeDistance(double x1, double y1, double x2, double y2)
    {
	double dist = Math.sqrt((x2-x1) * (x2-x1) + (y2-y1) * (y2-y1));  
	return dist;
    }


/*=================================================================*/
/* posdis  					*/
/* inverse of dispos				*/
/*   input lat, lon and a distance and angle 	*/
/*   output new lat, lon 			*/
/* all inputs and outputs in decimal degrees 	*/
/*=================================================================*/

public WorldPt posdis(double ra, double dec, double dist, double phi)
{
    double tmp, newdec, delta_ra;
    double radian=180./Math.PI;
    double ra1, dec1;

    ra /= radian;
    dec /= radian;
    dist /= radian;
    phi /= radian;

    tmp = Math.cos(dist) * Math.sin(dec) + Math.sin(dist) * Math.cos(dec) * Math.cos(phi);
    //System.out.println("tmp = " + tmp);
    newdec = Math.asin(tmp);
    //System.out.println("newdec = " + newdec);
    dec1 = newdec * radian;

    tmp = Math.cos(dist) * Math.cos(dec) - Math.sin(dist) * Math.sin(dec) * Math.cos(phi);
    //System.out.println("tmp = " + tmp);
    tmp /= Math.cos(newdec);
    //System.out.println("tmp = " + tmp);
    if (tmp > 1.0)
	tmp = 1.0;
    else if (tmp < -1.0)
	tmp = -1.0;
    delta_ra = Math.acos(tmp);
    //System.out.println("delta_ra = " + delta_ra);
    if (Math.sin(phi) < 0.0)
	ra1 = ra - delta_ra;
    else
	ra1 = ra + delta_ra;
    ra1 *= radian;
    return new WorldPt(ra1, dec1);
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
