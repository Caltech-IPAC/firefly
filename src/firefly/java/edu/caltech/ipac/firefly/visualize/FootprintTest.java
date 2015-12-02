/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.firefly.visualize.draw.ShapeDataObj;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.FitsRead;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.RangeValues;
import edu.caltech.ipac.visualize.plot.WorldPt;
import nom.tam.fits.Fits;

/**
 * The class footprint define combination of shapes acting as multiple markes
 * grouped
 * 
 * @author Emmanuel Joliet
 */
public class FootprintTest {

	private static String filename = "./bad.fits";
	private static ImagePlot plot;

	public static void buildPlot() throws Exception {

		Fits fits = new Fits(filename);
		// fits = new Fits("./good.fits");

		FitsRead[] frAry = FitsRead.createFitsReadArray(fits);
		ActiveFitsReadGroup fg = new ActiveFitsReadGroup();
		fg.setFitsRead(Band.RED, frAry[0]);
		plot = new ImagePlot(null, fg, 0, false, Band.RED, 0, new RangeValues());

	}
	public static void main(String[] args) {
		WorldPt sp = new WorldPt(40, 42);
		// 1deg dy
		WorldPt ep = new WorldPt(40, 43);
		WorldPt sj2000 = VisUtil.convertToJ2000(sp);
		WorldPt ej2000 = VisUtil.convertToJ2000(ep);

		System.out.println(sj2000.getY() + ", " + ej2000.getY());

		System.out.println(VisUtil.computeDistance(sj2000, ej2000));

		ShapeDataObj makeRectangle = ShapeDataObj.makeRectangle(new ScreenPt(2,0), new ScreenPt(4, 1));

		Pt centerPt = makeRectangle.getCenterPt();

//		// should be 1,2 but...
//		System.out.println(centerPt.getX() + "," + centerPt.getY());
		
		System.out.println("Shapes points...");
		Pt[] pts = makeRectangle.getPts();
		for (Pt pt : pts) {
			System.out.println(pt.getX() + ", " + pt.getY());
		}

		double angle=-45;
		System.out.println("Rotated points...");
		WorldPt wc = new WorldPt(40, 42);
		
		double[] pt = new double[]{2,0,4,1};
		
//		AffineTransform.getRotateInstance(Math.toRadians(angle), center.getX(), center.getY())
//		  .transform(pt, 0, pt, 0, 2); // specifying to use this double[] to hold coords
//		double newX = pt[0];
//		double newY = pt[1];
//		System.out.println(newX+", "+ newY);
//		double newX2 = pt[2];
//		double newY2 = pt[3];
//		System.out.println(newX2+", "+ newY2);
		
		
		
		
		
		
		ShapeDataObj makeWpRectangle = ShapeDataObj.makeRectangle(new WorldPt(40,40), new WorldPt(42, 42));
		System.out.println("Shapes points...");
		Pt[] wpts = makeWpRectangle.getPts();
		for (Pt pt1 : wpts) {
			System.out.println(pt1.getX() + ", " + pt1.getY());
		}
		wc = new WorldPt(41, 41);
//		WebPlot plot = new WebPlot(new WebPlotInitializer(null, null, null, null, 400, 600, 1, null, "",""));
//		makeWpRectangle.rotateAround(plot, Math.toRadians(180), wc);
//		 wpts = makeWpRectangle.getPts();
//			for (Pt pt1 : wpts) {
//				System.out.println(pt1.getX() + ", " + pt1.getY());
//			}
	}
}
