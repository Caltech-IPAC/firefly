/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import java.util.ArrayList;
import java.util.List;

import edu.caltech.ipac.firefly.visualize.FootprintFactory.FOOTPRINT;
import edu.caltech.ipac.firefly.visualize.FootprintFactory.INSTRUMENTS;
import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.DrawSymbol;
import edu.caltech.ipac.firefly.visualize.draw.FootprintObj;
import edu.caltech.ipac.firefly.visualize.draw.PointDataObj;
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
	private ArrayList<Region> footprintRegions;
	private RegionConnection regConnection;
	private boolean isDefined = false;
	private double rotationAngle;
	private WorldPt center;
	private FOOTPRINT fp;
	private FootprintFactory footprintFactory;
	private INSTRUMENTS inst;

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
		this.inst = null;//all footprint.
		isDefined = false;
	}

	/**
	 * Define footprint from a well-known instrument {@link FOOTPRINT}
	 * 
	 * @param center2
	 * @param plot
	 * @param fp
	 *            the footprint
	 */
	public FootprintDs9(WorldPt center2, WebPlot plot, FOOTPRINT fp, INSTRUMENTS inst) {
		super(center2, plot, 20);
		this.inst = inst;
		lst = new ArrayList<DrawObj>();
		this.center = center2;
		this.fp = fp;
		isDefined = true;
		buildInitialFootprint(plot);
	}

	@Override
	public void move(WorldPt userXy, WebPlot plot) {
		// 1. First take care of the footprint border limits by moving and
		// building
		// a circle shape:
		synchronized (plot) {

			super.move(userXy, plot);
			// Keep center at class scope
			ScreenPt cpt = null;
			if(userXy==null){
	    		cpt = getCenter(plot);
	    	}
	        else{
	        	cpt= plot.getScreenCoords(userXy);
	        }

			if (cpt == null)
				return;
			
			center = userXy;

			// 2. Move the shapes toward the center:
			moveFootprint(plot, center, false);
		}
	}

	/**
	 * @return rotated value in radians of the footprint
	 */
	public double getRotAngle() {
		return this.rotationAngle;
	}
	
	/**
	 * @param rotationAngle in radians
	 */
	public void setRotationAngle(double rotationAngle) {
		this.rotationAngle = rotationAngle;
	}
	
	@Override
	public void setEndPt(WorldPt endPt, WebPlot plot) {
		// Called from rotate mode - should be anything that user rotate any 4
		// corners

		// Calculate the rotation angle....
		ScreenPt screenCenter = plot.getScreenCoords(center);

		if (super.endPt == null)
			return;

		ScreenPt ep = plot.getScreenCoords(endPt);
		ScreenPt sp = screenCenter;// plot.getScreenCoords(getStartPt());

		double xdiff = ep.getX() - sp.getX();
		double ydiff = ep.getY() - sp.getY();
		
		//Keep the rotation angle to class scope so it can be used when moving 
		rotationAngle = Math.atan2(ydiff, xdiff); // radians!
		
		// ScreenPt center = getCenter(plot);// this takes the center from last
		// move
		// WorldPt wc = plot.getWorldCoords(center);

		// .... and move the marker/footprint around center
		moveFootprint(plot, center, true);

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
//		super.adjustStartEnd(plot);
//		move(plot.getWorldCoords(getCenter(plot)), plot);
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
		footprintFactory.setWebPlot(plot);
		lst.clear();
		DrawObj circleObj = super.getShape().get(0);
		
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
		// Build jwst from 0,0 resulting polygons
		WorldPt centerOffset = plot.getWorldCoords(new ScreenPt(0,0));
		
		if(inst==null){ // Full focalplane
			footprintRegions = (ArrayList<Region>) footprintFactory.getFootprintAsRegions(fp, centerOffset, false);// getFootprintRegions
		}else{
			footprintRegions = (ArrayList<Region>) footprintFactory.getFootprintAsRegions(fp, inst, centerOffset, true);// getFootprintRegions
		}

		//addCrossHair(footprintRegions, plot);
		
		footprintRegions.trimToSize();
		
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
		int hair = (int) (5 * plot.getZoomFact());
		RegionLines xcross = new RegionLines(plot.getWorldCoords(new ScreenPt(-hair, 0)),
				plot.getWorldCoords(new ScreenPt(hair, 0)));
		
		RegionLines ycross = new RegionLines(plot.getWorldCoords(new ScreenPt(0, -hair)),
				plot.getWorldCoords(new ScreenPt(0, hair)));
		footprintRegions2.add(xcross);
		footprintRegions2.add(ycross);
		
	}

	public WorldPt getRelativeInstrumentCenter(){
		return footprintFactory.getWorldCoordCenter();
	}
	
	public double[] getOffsetCenter(){
		return footprintFactory.getOffsetCenter();
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
	public void moveFootprint(WebPlot plot, WorldPt wpt, boolean rotate) {

		if (!isDefined) { // don't build me again please!
			buildInitialFootprint(plot);
			isDefined = true;
		}
		synchronized (lst) {
			// Need to recreate draw objects from previous state/position
			lst.clear();
			List<DrawObj> shape = super.getShape();
			
			updateFootprint(wpt);// made possible because initial reference is 0,0
			
			ShapeDataObj maincircleObj = (ShapeDataObj) shape.get(0);
			maincircleObj.setLineWidth(-1);
			String color = maincircleObj.getColor(); //Take color from the drawer and from the main circle ('Circular marker')
			lst.add(0,maincircleObj); // main circle marker with updated
											// position and radius.
			// Footprint region are not modified - need to call every time to
			// translate and rotate the underline shapes DrawObj objects.
			for (Region r : footprintRegions) {
				DrawObj drawObj = regConnection.makeRegionDrawObject(r, plot, false);
				drawObj.setColor(color);
				// Translate footprint - SHOULD be footprintobj or shapedataObj
				// with at least 2 points to call translateTo method correctly -
				// boxes or other shapes are not ok.
				//drawObj.translateTo(plot, wpt);
				if (rotationAngle!=0) {
					drawObj.rotateAround(plot, rotationAngle, wpt);
				}
				if (drawObj != null){
					lst.add(drawObj);
				}
			}
			
			PointDataObj xhair = new PointDataObj(wpt, DrawSymbol.CROSS);
			xhair.setColor(color);//"blue"
			xhair.setSize(10);
			lst.add(xhair);
		}		
	}
	
	/**
	 * Recompute the footprint originally based on ra,dec = 0,0 for a different point of reference.
	 * @param newRef
	 */
	private void updateFootprint(WorldPt newRef) {
		if(inst==null){ // Full focalplane
			footprintRegions = (ArrayList<Region>) footprintFactory.getFootprintAsRegions(fp, newRef, false);// getFootprintRegions
		}else{
			footprintRegions = (ArrayList<Region>) footprintFactory.getFootprintAsRegions(fp, inst, newRef, true);// getFootprintRegions
		}

		//addCrossHair(footprintRegions, plot);
		
		footprintRegions.trimToSize();
		
		regConnection = new RegionConnection(footprintRegions);
	}

	/**
	 * Rotate around current center
	 * 
	 * @param plot
	 *            {@link WebPlot} to get image zoom and height
	 */
	public void rotFootprint(WebPlot plot) {
		moveFootprint(plot, center, true);
	}
}
