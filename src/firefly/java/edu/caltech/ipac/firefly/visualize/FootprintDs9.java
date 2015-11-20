/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.visualize.FootprintFactory.FOOTPRINT;
import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.FootprintObj;
import edu.caltech.ipac.firefly.visualize.draw.RegionConnection;
import edu.caltech.ipac.firefly.visualize.draw.ShapeDataObj;
import edu.caltech.ipac.util.dd.Region;
import edu.caltech.ipac.util.dd.RegionLines;
import edu.caltech.ipac.visualize.plot.WorldPt;

/**
 * The class footprint define combination of shapes acting as multiple markes
 * grouped
 * 
 * @author Emmanuel Joliet
 */
public class FootprintDs9 extends CircularMarker {

	private List<DrawObj> lst;
	private List<Region> footprintRegions;
	private RegionConnection regConnection;
	private boolean isDefined = false;
	private double rotationAngle;
	private WorldPt center;
	private FOOTPRINT fp;
	private FootprintFactory footprintFactory;

	/**
	 * Define footprint JWST by default with no particular center
	 */
	public FootprintDs9() {
		super(20);// circle center represents the main reference shape as main
					// 'marker' around multi-shape footprint
		lst = new ArrayList<DrawObj>();
		this.fp = FOOTPRINT.JWST;
		isDefined = false;
	}
	
	public FootprintDs9(FOOTPRINT fp) {
		super(20);// circle center represents the main reference shape as main
					// 'marker' around multi-shape footprint
		lst = new ArrayList<DrawObj>();
		this.fp = fp;
		isDefined = false;
		GwtUtil.logToServer(Level.INFO, "FootprintDs9(Footprint) - with FP: " + fp.name());
	}

	/**
	 * Define footprint from a well-known instrument {@link FOOTPRINT}
	 * 
	 * @param center2
	 * @param plot
	 * @param fp
	 *            the footprint
	 */
	public FootprintDs9(WorldPt center2, WebPlot plot, FOOTPRINT fp) {
		super(center2, plot, 20);
		lst = new ArrayList<DrawObj>();
		this.center = center2;
		this.fp = fp;
		isDefined = false;
		GwtUtil.logToServer(Level.INFO,
				"FootprintDs9(WorldPt, WebPlot, Footprint) - center =" + center.toString() + " with FP: " + fp.name());
	}

	@Override
	public void move(WorldPt userXy, WebPlot plot) {
		// 1. First take care of the footprint border limits by moving and
		// building
		// a circle shape:
		super.move(userXy, plot);

		// Keep center at class scope
		ScreenPt cpt = plot.getScreenCoords(userXy);
		if (cpt == null)
			return;
//		center= VisUtil.calculatePosition(userXy, getStartPt().getLon()*3600, getStartPt().getLat()*3600);
		center = userXy;
		
		// 2. Move the shapes toward the center:
		moveFootprint(plot, center);
	}

	/**
	 * @return rotated value in degree of the footprint
	 */
	public double getRotAngle() {
		return this.rotationAngle;
	}

	@Override
	public void setEndPt(WorldPt endPt, WebPlot plot) {
		// Called from rotate mode - should be anything that user rotate any 4
		// corners

		// Calculate the rotation angle....
		ScreenPt screenCenter = plot.getScreenCoords(center);

		GwtUtil.logToServer(Level.INFO, "setEndPt - center (x,y)=" + center.getX() + ", " + center.getY());
		GwtUtil.logToServer(Level.INFO, "setEndPt - center (x,y)=" + screenCenter.getX() + ", " + screenCenter.getY());

		if (super.endPt == null)
			return;

		GwtUtil.logToServer(Level.INFO,
				"setEndPt - startPt (lon,lat)=" + getStartPt().getX() + ", " + getStartPt().getY());
		GwtUtil.logToServer(Level.INFO, "setEndPt - endPt (lon,lat)=" + endPt.getLon() + ", " + endPt.getLat());

		ScreenPt ep = plot.getScreenCoords(endPt);
		ScreenPt sp = screenCenter;// plot.getScreenCoords(getStartPt());

		GwtUtil.logToServer(Level.INFO, "setEndPt - center (x,y)=" + sp.getX() + ", " + sp.getY());
		GwtUtil.logToServer(Level.INFO, "setEndPt - endPt (x,y)=" + ep.getX() + ", " + ep.getY());
		int xdiff = ep.getIX() - sp.getIX();
		int ydiff = ep.getIY() - sp.getIY();
		
		//Keep the rotation angle to class scope so it can be used when moving 
		rotationAngle = Math.atan2(ydiff, xdiff); // radians!
		
		GwtUtil.logToServer(Level.INFO,
				"setEndPt - angle rotated rad=" + rotationAngle + ", deg: " + Math.toDegrees(rotationAngle));

		// ScreenPt center = getCenter(plot);// this takes the center from last
		// move
		// WorldPt wc = plot.getWorldCoords(center);

		// .... and move the marker/footprint around center
		moveFootprint(plot, center);

		// if (!isDefined) { // don't build me again please!
		// buildInitialFootprint(plot);
		// isDefined = true;
		// }
		// synchronized (lst) {
		//
		//
		// //We want to keep circle size and not changing the circle size
		// for (DrawObj drawObj : lst) {
		// if (drawObj instanceof FootprintObj) {
		// lst.remove(drawObj);
		// }
		// }
		//
		// for (Region r : footprintRegions) {
		// DrawObj drawObj = regConnection.makeRegionDrawObject(r, plot, false);
		// drawObj.setColor("blue");
		// ((FootprintObj) drawObj).translateTo(plot, center);
		// ((FootprintObj) drawObj).rotateAround(plot, rotationAngle, center);
		// if (drawObj != null)
		// lst.add(drawObj);
		// }
		// }
	}

	//
	@Override
	public void adjustStartEnd(WebPlot plot) {
		//super.adjustStartEnd(plot);
		//move(center, plot);
		GwtUtil.logToServer(Level.INFO,
				"adjustStartEnd(WebPlot) - center =" + center.toString() );
	}

	@Override
	public List<DrawObj> getShape() {

		return lst;
	}

	/**
	 * Builds initial footprint based on regions using {@link FootprintFactory}
	 * FIXME: regions should come from hardcoded file but gwt client not
	 * supporting java.io.*..., now using hardcoded STC in FootprintFactory
	 * 
	 * @param plot
	 * @param fp
	 *            the footprint enum {@link FOOTPRINT}
	 */
	public void buildInitialFootprint(WebPlot plot) {
		footprintFactory = new FootprintFactory();
		lst.clear();
		DrawObj circleObj = super.getShape().get(0);
		
//		lst.add(0, ShapeDataObj.makeCircle(pt.(), getEndPt()));
		lst.add(0, circleObj);// add circle central marker

		/*
		 * Examples
		 * 
		 * // ScreenPt scrFpCenter = super.getCenter(plot);// screen center x,y
		 * of of circle-footprint // center = plot.getWorldCoords(scrFpCenter);
		 * // Build polygon to build a footprint drawObj RegionLines lines1 =
		 * new RegionLines(plot.getWorldCoords(new ScreenPt(50, -40)),
		 * plot.getWorldCoords(new ScreenPt(150, -40)), plot.getWorldCoords(new
		 * ScreenPt(150, 40)), plot.getWorldCoords(new ScreenPt(50, 40)));
		 * RegionLines lines2 = new RegionLines(plot.getWorldCoords(new
		 * ScreenPt(-50, -30)), plot.getWorldCoords(new ScreenPt(-50, -90)),
		 * plot.getWorldCoords(new ScreenPt(50, -90)), plot.getWorldCoords(new
		 * ScreenPt(50, -30)));
		 * 
		 * // Arbitrary shape based on screen pixels. RegionLines lines3 = new
		 * RegionLines(
		 * 
		 * plot.getWorldCoords(new ScreenPt(30, -20)), plot.getWorldCoords(new
		 * ScreenPt(100, -20)), plot.getWorldCoords(new ScreenPt(100, 70)),
		 * plot.getWorldCoords(new ScreenPt(80, 70)), plot.getWorldCoords(new
		 * ScreenPt(80,20)), plot.getWorldCoords(new ScreenPt(30,20))
		 * 
		 * ); //Box around target m34 - wcs j2000 double[] pol = new double[] {
		 * 40.50, 42.74, 40.55, 42.74, 40.55, 42.94, 40.50, 42.94
		 * 
		 * }; WorldPt[] pts = new WorldPt[pol.length/2]; for (int i = 0; i <
		 * pol.length/2; i++) { pts[i]=new WorldPt(pol[2*i], pol[2*i+1]);
		 * ScreenPt screenCoords = plot.getScreenCoords(pts[i]);
		 * GwtUtil.logToServer(Level.INFO, "buildFootprint wpt =" +i+" "+
		 * pts[i].getX() + ", " + pts[i].getY());
		 * GwtUtil.logToServer(Level.INFO, "buildFootprint wpt (x,y)=" +i+" "+
		 * screenCoords.getX() + ", " + screenCoords.getY()); } RegionLines
		 * polygon1 = new RegionLines(pts);
		 * 
		 * RegionLines boxCentered = new RegionLines(plot.getWorldCoords(new
		 * ScreenPt(-50, -50)), plot.getWorldCoords(new ScreenPt(-50, 50)),
		 * plot.getWorldCoords(new ScreenPt(50, 50)), plot.getWorldCoords(new
		 * ScreenPt(50, -50)));
		 * 
		 */
		GwtUtil.logToServer(Level.INFO,
				"buildFootprint(WebPlot) - center =" + center.toString() );
		// Build jwst from 0,0 resulting polygons (FIXME: does it make sense?)
		WorldPt centerOffset = plot.getWorldCoords(new ScreenPt(0,0));
//		WorldPt newCenter = VisUtil.calculatePosition(centerOffset, center.getLon()*3600, center.getLat()*3600);
//		
		GwtUtil.logToServer(Level.INFO,
				"buildFootprint(WebPlot) - center offset screen(0,0) =" + centerOffset.toString() );
		footprintRegions = footprintFactory.getFootprintAsRegions(fp, centerOffset);// getFootprintRegions

		addCrossHair(footprintRegions, plot);

		regConnection = new RegionConnection(footprintRegions);
	}

	/**
	 * Make crosshair polygons (array of points > 2) otherwise it won't
	 * translate or rotate correclty.
	 * 
	 * @param footprintRegions2
	 * @param plot
	 */
	private void addCrossHair(List<Region> footprintRegions2, WebPlot plot) {
		RegionLines xcross = new RegionLines(plot.getWorldCoords(new ScreenPt(-10, 0)),
				plot.getWorldCoords(new ScreenPt(10, 0)));

		RegionLines ycross = new RegionLines(plot.getWorldCoords(new ScreenPt(0, -10)),
				plot.getWorldCoords(new ScreenPt(0, 10)));
		footprintRegions2.add(xcross);
		footprintRegions2.add(ycross);
	}

	private WorldPt getPolygonWroldPtCenter(WorldPt... ptAry) {
		double xSum = 0;
		double ySum = 0;
		double xTot = 0;
		double yTot = 0;
		for (WorldPt wpt : ptAry) {
			xSum += wpt.getX();
			ySum += wpt.getY();
			xTot++;
			yTot++;
		}
		return new WorldPt(xSum / xTot, ySum / yTot);
	}

	/**
	 * Move/rotate shapes polygons, circles, rectangles sucha as {@link FootprintObj}
	 * and {@link ShapeDataObj} (not working for box defined with wp,h,w)
	 * 
	 * @param plot
	 *            {@link WebPlot} to get image zoom and height
	 * @param wpt
	 *            moving reference pointer
	 */
	public void moveFootprint(WebPlot plot, WorldPt wpt) {

		if (!isDefined) { // don't build me again please!
			buildInitialFootprint(plot);
			isDefined = true;
		}
		synchronized (lst) {
			// Need to recreate draw objects from previous state/position
			lst.clear();
			lst.addAll(super.getShape()); // main circle marker with updated
											// position and radius.

			// Footprint region are not modified - need to call every time to
			// translate and rotate the underline shapes DrawObj objects.
			for (Region r : footprintRegions) {
				DrawObj drawObj = regConnection.makeRegionDrawObject(r, plot, false);
				drawObj.setColor("blue");
				// Translate footprint - SHOULD be footprintobj or shapedataObj
				// with at least 2 points to call translateTo method correctly -
				// boxes or other shapes are not ok.
				drawObj.translateTo(plot, wpt);
				drawObj.rotateAround(plot, rotationAngle, wpt);

				if (drawObj != null)
					lst.add(drawObj);
			}
		}		
	}
}
