/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.FootprintObj;
import edu.caltech.ipac.firefly.visualize.draw.RegionConnection;
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

	private ArrayList<DrawObj> lst;
	private List<Region> footprintRegions;
	private RegionConnection regConnection;
	private boolean isDefined = false;
	private List<DrawObj> initFootprint;
	private double rotationAngle;
	private WorldPt center;

	public FootprintDs9() {
		super(20);// circle center represents the main reference shape as main
					// 'marker' around multi-shape footprint
		lst = new ArrayList<DrawObj>();
	}

	@Override
	public void move(WorldPt userXy, WebPlot plot) {
		// 1. First take care of the footprint border limits by moving and
		// building
		// a circle shape:
		super.move(userXy, plot);

		// Then build the other shapes of the footprint moved:
		ScreenPt cpt = plot.getScreenCoords(userXy);
		if (cpt == null)
			return;
		center = userXy;
		
		moveFootprint(plot, userXy);
	}

	
	public double getRotAngle(){
		return this.rotationAngle;
	}
	
	@Override
	public void setEndPt(WorldPt endPt, WebPlot plot) {
		// Called from rotate mode - should be anything that user rotate any 4
		// corners
		//super.endPt = endPt; // don't change radius!
//		ScreenPt screenCenter = super.getCenter(plot);
		ScreenPt screenCenter = plot.getScreenCoords(center);
		
		GwtUtil.logToServer(Level.INFO, "setEndPt - center (x,y)=" + center.getX() + ", " + center.getY());
		GwtUtil.logToServer(Level.INFO, "setEndPt - center (x,y)=" + screenCenter.getX() + ", " + screenCenter.getY());
		
		if (super.endPt == null)
			return;

		GwtUtil.logToServer(Level.INFO, "setEndPt - startPt (lon,lat)=" + getStartPt().getX() + ", " + getStartPt().getY()); 
		GwtUtil.logToServer(Level.INFO, "setEndPt - endPt (lon,lat)=" + endPt.getLon() + ", " + endPt.getLat());
		
//		double angleDegrees = VisUtil.computeDistance(center, endPt);
//		GwtUtil.logToServer(Level.INFO, "setEndPt - angular distance [deg]=" + angleDegrees);

		ScreenPt ep = plot.getScreenCoords(endPt);
		ScreenPt sp = screenCenter;//plot.getScreenCoords(getStartPt());
		
		GwtUtil.logToServer(Level.INFO, "setEndPt - center (x,y)=" + sp.getX() + ", " + sp.getY());
		GwtUtil.logToServer(Level.INFO, "setEndPt - endPt (x,y)=" + ep.getX() + ", " + ep.getY());
		int xdiff = ep.getIX() - sp.getIX();
		int ydiff = ep.getIY() - sp.getIY();
		rotationAngle = Math.atan2(ydiff, xdiff); // radians!
		GwtUtil.logToServer(Level.INFO,
				"setEndPt - angle rotated rad=" + rotationAngle + ", deg: " + Math.toDegrees(rotationAngle));

		//ScreenPt center = getCenter(plot);// this takes the center from last move
		//WorldPt wc = plot.getWorldCoords(center);
		
		if (!isDefined) { // don't build me again please!
			buildInitialFootprint(plot);
			isDefined = true;
		}
		
		//We want to keep circle size and not changing the circle size
		for (DrawObj drawObj : lst) {
			if(drawObj instanceof FootprintObj){
				lst.remove(drawObj);
			}
		}

		for (Region r : footprintRegions) {
			DrawObj drawObj = regConnection.makeRegionDrawObject(r, plot, false);
			drawObj.setColor("blue");
			if(rotationAngle!=0){
				((FootprintObj) drawObj).rotateAround(plot, rotationAngle, center); //startPt() is actually the center of the circle defining the footprint limit
			}

			if (drawObj != null)
				lst.add(drawObj);
		}
//		// ROTATE
//		if (!isDefined) { // don't build me again please!
//			buildInitialFootprint(plot);
//			isDefined = true;
//		}
		

//		for (Region r : footprintRegions) {
//			DrawObj drawObj = regConnection.makeRegionDrawObject(r, plot, false);
//			drawObj.setColor("blue");
//			// Translate footprint
//			((FootprintObj) drawObj).rotateAround(plot, rotationAngle, wc);
//
//			if (drawObj != null)
//				lst.add(drawObj);
//		}
	}

	//
	@Override
	public void adjustStartEnd(WebPlot plot) {

		// Not needed? FIXME
		//super.adjustStartEnd(plot);
		ScreenPt screenCenter = super.getCenter(plot);
		center = plot.getWorldCoords(screenCenter);
		
		// // Called from moving it - start - this should be same center as the
		// // circular marker that is extending it.
		// // Sets the center x,y point after drag it.
		GwtUtil.logToServer(Level.INFO, "adjustStartEnd - start=" + getStartPt().getX() + ", " + getStartPt().getY());
		GwtUtil.logToServer(Level.INFO, "adjustStartEnd - end=" + getEndPt().getX() + ", " + getEndPt().getY());
		
		
		GwtUtil.logToServer(Level.INFO, "adjustStartEnd - center (lon,lat)=" + center.getX() + ", " + center.getY());
		GwtUtil.logToServer(Level.INFO, "adjustStartEnd - center (x,y)=" +screenCenter.getX() + ", " + screenCenter.getY());
		
		// ScreenPt pt0 = plot.getScreenCoords(getStartPt());
		// ScreenPt pt1 = plot.getScreenCoords(getEndPt());
		// GwtUtil.logToServer(Level.INFO, "adjustStartEnd - pt0=" + pt0.getX()
		// + ", " + pt0.getY());
		// GwtUtil.logToServer(Level.INFO, "adjustStartEnd - pt1=" + pt1.getX()
		// + ", " + pt1.getY());
		// // WorldPt center = plot.getWorldCoords(getCenter(plot));
		// // for (ShapeDataObj shapeDataObj : lst) {
		// // if (shapeDataObj.getShape().equals(ShapeType.Circle)) {
		// // continue;// don't move again circle, only crosshair and
		// // // rectangles
		// // }
		// // // 3. Translate others shapes but circle
		// //
		// // shapeDataObj.translateTo(plot, center);
		// // }
	}

	@Override
	public List<DrawObj> getShape() {

		return lst;
	}

	/**
	 * Buils initial footpritn based on regions FIXME: regions should come from
	 * hardcoded file but gwt client not supporting java.io.*..., now using
	 * fixed random polygon (2 rectangle actually)
	 * 
	 * @param plot
	 * @throws IOException
	 */
	public void buildInitialFootprint(WebPlot plot) {
		footprintRegions = new ArrayList<>(2);
		
		
		// Build polygon to build a footprint drawObj
		RegionLines lines1 = new RegionLines(plot.getWorldCoords(new ScreenPt(50, -40)),
				plot.getWorldCoords(new ScreenPt(150, -40)), plot.getWorldCoords(new ScreenPt(150, 40)),
				plot.getWorldCoords(new ScreenPt(50, 40)));
		RegionLines lines2 = new RegionLines(plot.getWorldCoords(new ScreenPt(-50, -30)),
				plot.getWorldCoords(new ScreenPt(-50, -90)), plot.getWorldCoords(new ScreenPt(50, -90)),
				plot.getWorldCoords(new ScreenPt(50, -30)));
		
		RegionLines lines3 = new RegionLines(

		plot.getWorldCoords(new ScreenPt(30, -20)), plot.getWorldCoords(new ScreenPt(100, -20)),
				plot.getWorldCoords(new ScreenPt(100, 70)), plot.getWorldCoords(new ScreenPt(80, 70)),
				plot.getWorldCoords(new ScreenPt(80,20)), plot.getWorldCoords(new ScreenPt(30,20))

		);
		
		footprintRegions.add(lines3);
//		footprintRegions.add(lines2);
		regConnection = new RegionConnection(footprintRegions); // FIXME: just a
																// connection to
																// make regions
																// easily -
																// could be in
																// VisUtils
		refreshShapes(plot);
	}

	private void refreshShapes(WebPlot plot) {
		center = plot.getWorldCoords(super.getCenter(plot));
		lst.clear();
		lst.addAll(super.getShape());
		for (Region r : footprintRegions) {
			DrawObj drawObj = regConnection.makeRegionDrawObject(r, plot, false);
			drawObj.setColor("blue");
			if (drawObj != null)
				lst.add(drawObj);
		}
	}

	public void moveFootprint(WebPlot plot, WorldPt wpt) {
		
		if (!isDefined) { // don't build me again please!
			buildInitialFootprint(plot);
			isDefined = true;
		}

		lst.clear();
		lst.addAll(super.getShape());

		
		for (Region r : footprintRegions) {
			DrawObj drawObj = regConnection.makeRegionDrawObject(r, plot, false);
			drawObj.setColor("blue");
//			if(rotationAngle!=0){
//				((FootprintObj) drawObj).rotateAround(plot, rotationAngle, wpt);
//			}
			// Translate footprint
			((FootprintObj) drawObj).translateTo(plot, wpt);

			if (drawObj != null)
				lst.add(drawObj);
		}
	}
}
