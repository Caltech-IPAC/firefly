/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import java.awt.geom.Point2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Area;
import java.util.List;
import java.util.ArrayList;


public class CombinePolygons
{

public static boolean debug = false;

private List<GeneralPath> shapes;   // rotated coordinates
private Circle rotated_circle;      // rotated coordinates
private Rotate rotate;

public CoveragePolygons getCoverage(List<ImageCorners> corners)
    throws CircleException
  {
    if (debug)
    {
	System.out.println("Entering CoveragePolygons.getCoverage()");
	Rotate.debug = true;
    }
    shapes = makeShapes(corners);
    if (shapes == null)
	return null;

    // Create Areas from the shapes, and join the areas.
    Area areaOne = new Area();
    for(GeneralPath one_shape : shapes)
    {
	Area one_area = new Area(one_shape);
	areaOne.add(one_area);  // does the join
    }


    WorldPt first_point = null;
    WorldPt point = null;
    ImageCorners image_corners = null;
    List<ImageCorners> list = new ArrayList<ImageCorners>();  //original coords
    List<WorldPt> rotated_list = new ArrayList<WorldPt>();  //rotated coords
    PathIterator iterator = areaOne.getPathIterator(null);
    double coords[] = new double[6];
    int path_type;
    while (!iterator.isDone())
    {
	path_type = iterator.currentSegment(coords);
	iterator.next();
	switch(path_type)
	{
	    case PathIterator.SEG_MOVETO:
		image_corners = new ImageCorners();
		first_point = new WorldPt(coords[0], coords[1]);
		rotated_list.add(first_point);
		first_point = rotate.do_unrotate(first_point);
		if (debug)
		{
		System.out.println("path_type = SEG_MOVETO");
		System.out.println("  ra = " + first_point.getLon());
		System.out.println("  dec = " + first_point.getLat());
		}
		image_corners.addCorners(first_point);
		break;
	    case PathIterator.SEG_LINETO:
		point = new WorldPt(coords[0], coords[1]);
		rotated_list.add(point);
		point = rotate.do_unrotate(point);
		if (debug)
		{
		System.out.println("path_type = SEG_LINETO");
		System.out.println("  ra = " + point.getLon());
		System.out.println("  dec = " + point.getLat());
		}
		image_corners.addCorners(point);
		break;
	    case PathIterator.SEG_CLOSE:
		if (debug)
		{
		System.out.println("path_type = SEG_CLOSE");
		}
		boolean completed_already = 
		    eps_compare(first_point.getLon(), point.getLon()) &&
		    eps_compare(first_point.getLat(), point.getLat());
		if (!completed_already)
		{
		    image_corners.addCorners(first_point);
		}
		first_point = null;
		list.add(image_corners);
		break;
	    default:
		System.out.println("path_type UNKNOWN");
		break;
	}
    }


    /* now compute surrounding circle */
    CentralPoint central_point = new CentralPoint();
    if (debug)
    {
	central_point.debug = true;
    }
    rotated_circle = central_point.find_circle(rotated_list);
    WorldPt center = rotated_circle.getCenter();
    double radius = rotated_circle.getRadius();
    if (debug)
    {
    System.out.println("rotated circle: center is at lon = " + center.getLon() +
	"  lat = " + center.getLat() + "  radius = " + radius);
    }
    /* now unrotate coords to get circle at original coords */
    point = new WorldPt(center.getLon(), center.getLat());
    point = rotate.do_unrotate(point);
    Circle circle = new Circle(point, radius);

    return new CoveragePolygons(list, circle);
  }

  static boolean eps_compare(double x, double x1)
  {
    /* see if they're within eps */
    double eps = 10e-6;

    if (Math.abs(x1) < eps)
    {
	if (Math.abs(x) < eps)
	    return true;
	else
	    return false;
    }
    if (Math.abs((x - x1)/x1) < eps)
	return true;
    else
	return false;
  }

  /** Only used by CombinePolygonsTest
  */
  List<GeneralPath> getShapes()
  {
      return shapes;
  }

  Circle getCircle()
  {
      return rotated_circle;
  }


private List<GeneralPath> makeShapes(List<ImageCorners> corners)
  {
    double first_ra = Double.NaN;
    double ra ;
    double dec;

    List<GeneralPath> list = new ArrayList<GeneralPath>();
    GeneralPath mShape = new GeneralPath();
    if ((corners != null) && (corners.size() > 0))
    {
	for(ImageCorners c : corners)
	{
	    /* c is one rectangle */
	    if ((c.size() >= 3))  // need 3 points for an area 
	    {
		GeneralPath this_shape = new GeneralPath();
		float ra1 = Float.NaN;
		float dec1 = Float.NaN;
		for (WorldPt point: c.getCorners())
		{
		    /* point is one corner */
		    if (Double.isNaN(first_ra))
		    {
			/* this is our first coordinate */
			/* Compute a rotation which will move the points */
			/* to near 180,0 to avoid curvature effects */
			dec = point.getLat();
			ra = point.getLon();
			rotate = new Rotate();
			rotate.compute_rotation_angles(point);
			first_ra = ra;
		    }

		    float x, y;
		    /* rotate the point */
		    WorldPt rotated_point = rotate.do_rotate(point);
		    x = (float) rotated_point.getLon();
		    y = (float) rotated_point.getLat();
		    if (debug)
		    {
		    System.out.println("rotated x = " + x + "  y = " + y);
		    }

		    Point2D point_2d = this_shape.getCurrentPoint();
		    if (point_2d == null)  // if this is first point 
		    {
			this_shape.moveTo(x, y);
			ra1 = x;
			dec1 = y;
		    }
		    else
		    {
			this_shape.lineTo(x, y);
		    }
		}
		Point2D point_2d = this_shape.getCurrentPoint();
		if (point_2d != null)  // if we got points
		{
		    this_shape.lineTo(ra1, dec1);  // complete the polygon
		    list.add(this_shape);
		}
	    }
	}
	if (Double.isNaN(first_ra))
	{
	    return null;  // got no valid coordinates on any ImageCorners 
	}

	return list;
    }
    else
    {
	/* no corners to process */
	return null;
    }
  }
}
