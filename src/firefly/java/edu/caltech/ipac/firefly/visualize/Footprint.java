/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.ShapeDataObj;
import edu.caltech.ipac.firefly.visualize.draw.ShapeDataObj.ShapeType;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * The class footprint define combination of shapes acting as multiple markers
 * grouped
 * 
 * @author Emmanuel Joliet
 */
public class Footprint extends CircularMarker {

	public static ShapeType borderFootprint, shapeCam, shapeSpec;

	static {
		borderFootprint = ShapeType.Rectangle;
		shapeCam = ShapeType.Rectangle;
		shapeSpec = ShapeType.Line;
	}

	/**
	 * Instruments defined in footprint (in screen 'pixel' metric)
	 * 
	 * @author ejoliet
	 *
	 */
	public enum INSTRUMENT {

		CAM1(shapeCam, new int[] { 0, 0 }, new int[] { 50, 30 }), 
		CAM2(shapeCam, new int[] { 150, 0 }, //x,y center of rect
				new int[] { 80, 40 });//w,h
//		SPEC1(shapeSpec, new int[] { -30,0 }, //line centered on -30,0 relative to center of circle identifying FoV
//				new int[] { 0, 0, 20, 20 });//xi,yi line tuple if centered on FoV
		
		int[] offset;
		private ShapeType shape;
		private int[] geom;// either radius if 1 element, 2 element would be a
							// rectangle: w,h, more than 2, a polygon:
							// x1,y1,x2,y2,x3,y3,etc...

		INSTRUMENT(ShapeType shape, int[] offset, int[] geom) {
			this.offset = offset;
			this.shape = shape;
			this.geom = geom;
		}

		int[] getOffsetRelativeToFoVCenter() {
			return offset; // center of the shape offset from center of FoV
		}

		ShapeType getShape() {
			return shape;
		}

		int[] getGeometry() {
			return geom;
		}

	};

	private ArrayList<DrawObj> lst;

	public Footprint() {
		super(20);// circle center represents the main reference shape as main
					// 'marker' around multi-shape footprint
		lst = new ArrayList<>();
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

		// 2. Define screen footprint with origin 0,0:
		defineFootprint(plot);
		for (DrawObj drawObj : lst) {
			if (drawObj instanceof ShapeDataObj) {
				ShapeDataObj shapeDataObj = (ShapeDataObj) drawObj;
				if (shapeDataObj.getShape().equals(ShapeType.Circle)) {
					continue;// don't move again circle, only crosshair and
								// rectangles
				}
				// 3. Translate others shapes but circle
				shapeDataObj.translateTo(plot, userXy);
			}
		}

		// moveFootprintDataTo(cpt, plot);
	}

	/**
	 * Define footprint relative to start & end world point (circle center by definition)
	 * Need the webplot to convert shapes if zoom or image size changes
	 * 
	 * @param plot
	 */
	private void defineFootprint(WebPlot plot) {
		
		ScreenPt spt= plot.getScreenCoords(getStartPt());
        ScreenPt ept= plot.getScreenCoords(getEndPt());
        if (spt==null || ept==null) return;

        int x1= spt.getIX();
        int y1= spt.getIY();
        int x2= ept.getIX();
        int y2= ept.getIY();
        
        lst.clear();

		// Circle main reference around footprint - limit the footprint and
		// serves as corner limit too (+title label)
		lst.addAll(super.getShape()); // FIXME should be line-width 0 so is not
										// seen
										// or saved!
		// Actual footprint is here:
		// Instruments should be of rectangle shapes for now
		for (INSTRUMENT inst : INSTRUMENT.values()) {
			int[] centerFov = inst.getOffsetRelativeToFoVCenter();
			int[] wh = inst.getGeometry();

			switch (inst.getShape()) {
			case Rectangle:

				// Define rectangle on origin
				ScreenPt p0_zero = new ScreenPt(centerFov[0] - wh[0] / 2, centerFov[1] - wh[1] / 2);
				ScreenPt p1_zero = new ScreenPt(centerFov[0] + wh[0] / 2, centerFov[1] + wh[1] / 2);
				
				ScreenPt p0 = plot.getScreenCoords(p0_zero);
				ScreenPt p1 = plot.getScreenCoords(p1_zero);
		            
				int w = (int) (p1.getX()-p0.getX());
				int h = (int) (p1.getY()-p0.getY());
//				ImagePt sp = plot.getImageCoords(p0);
//				ImagePt ep = plot.getImageCoords(p1);
				WorldPt sp = plot.getWorldCoords(p0);
				//WorldPt ep = plot.getWorldCoords(p1);// FIXME gives the wcs
				// based
				// on the image projection?

				lst.add(ShapeDataObj.makeRectangle(sp, w,h));
				break;
			case Circle:

				break;
			case Line:
//				ScreenPt pl0 = new ScreenPt(centerFov[0] - wh[0] / 2, centerFov[1] - wh[1] / 2);
//				ScreenPt pl1 = new ScreenPt(centerFov[0] + wh[0] / 2, centerFov[1] + wh[1] / 2);
//				lst.add(ShapeDataObj.makeLine(pl0, pl1));
				break;
			case Text:
				break;
			default:
				break;
			}

		}
		// Add cross hair independent of the image zoom.
		// addCrossHair(lst, plot, cpt);
		// Add x/y lines crosshair
		
		ScreenPt chx0 = new ScreenPt(-10, 0);
		ScreenPt chx1 = new ScreenPt(10, 0);
		ImageWorkSpacePt chx00 = plot.getImageWorkSpaceCoords(chx0);
		ImageWorkSpacePt chx01 = plot.getImageWorkSpaceCoords(chx1);
		WorldPt ptx0 = plot.getWorldCoords(chx00);
		WorldPt ptx1 = plot.getWorldCoords(chx01);
		ShapeDataObj xline = ShapeDataObj.makeLine(ptx0, ptx1);
		lst.add(xline);

		ScreenPt chy0 = new ScreenPt(0, -10);
		ScreenPt chy1 = new ScreenPt(0, 10);
		ImageWorkSpacePt chy00 = plot.getImageWorkSpaceCoords(chy0);
		ImageWorkSpacePt chy01 = plot.getImageWorkSpaceCoords(chy1);
		WorldPt pty0 = plot.getWorldCoords(chy00);
		WorldPt pty1 = plot.getWorldCoords(chy01);
		ShapeDataObj yline = ShapeDataObj.makeLine(pty0, pty1);
		yline.setColor("blue");
		xline.setColor("blue");
		yline.setLineWidth(2);
		xline.setLineWidth(2);

		lst.add(yline);

		//beautifyMe(lst);

		lst.trimToSize();
	}
/*
	private void moveFootprintDataTo(ScreenPt cpt, WebPlot plot) {
		lst.clear();

		// Circle main reference around footprint - limit the footprint and
		// serves as corner limit too (+title label)
		lst.addAll(super.getShape()); // FIXME should be line-width 0 so is not
										// seen
										// or saved!
		// Actual footprint is here:
		// Instruments should be of rectangle shapes for now
		for (INSTRUMENTS inst : INSTRUMENTS.values()) {
			int[] centerFov = inst.getCenterPositionRelativeToFoV();
			int[] wh = inst.getGeometry();

			switch (inst.getShape()) {
			case Rectangle:
				addRectangle(lst, centerFov, wh, plot, cpt);
				break;
			case Circle:

				break;
			case Line:
				break;
			case Text:
				break;
			default:
				break;
			}

		}
		// Add cross hair independent of the image zoom.
		addCrossHair(lst, plot, cpt);

		beautifyMe(lst);
		lst.trimToSize();
	}


	private void addRectangle(ArrayList<ShapeDataObj> lst2, int[] centerFov, int[] wh, WebPlot plot, ScreenPt cpt) {
		ScreenPt sp = new ScreenPt(cpt.getIX() + centerFov[0] - wh[0] / 2, cpt.getIY() + centerFov[1] - wh[1] / 2);
		ScreenPt ep = new ScreenPt(cpt.getIX() + centerFov[0] + wh[0] / 2, cpt.getIY() + centerFov[1] + wh[1] / 2);
		WorldPt pt0 = plot.getWorldCoords(sp);
		WorldPt pt1 = plot.getWorldCoords(ep);// FIXME gives the wcs based
												// on the image projection?

		ShapeDataObj makeRectangle = ShapeDataObj.makeRectangle(pt0, pt1);
		lst2.add(makeRectangle);

	}

	private void addCrossHair(ArrayList<ShapeDataObj> lst2, WebPlot plot, ScreenPt cpt) {
		ScreenPt spx = new ScreenPt(cpt.getIX() - 10, cpt.getIY());
		ScreenPt epx = new ScreenPt(cpt.getIX() + 10, cpt.getIY());
		// WorldPt ptx0 = plot.getWorldCoords(spx);
		// WorldPt ptx1 = plot.getWorldCoords(epx);
		ShapeDataObj xline = ShapeDataObj.makeLine(spx, epx);// keep same size
																// no matter the
																// zoom or
																// proyection!
		xline.setLineWidth(2);
		xline.setColor("blue");
		// add horizontal cross hair part
		lst2.add(xline);

		ScreenPt spy = new ScreenPt(cpt.getIX(), cpt.getIY() - 10);
		ScreenPt epy = new ScreenPt(cpt.getIX(), cpt.getIY() + 10);
		// WorldPt pty0 = plot.getWorldCoords(spy);
		// WorldPt pty1 = plot.getWorldCoords(epy);// FIXME gives the wcs based
		// on
		// the image projection?
		ShapeDataObj yline = ShapeDataObj.makeLine(spy, epy); // keep same size
																// no matter the
																// zoom or
																// proyection!
		yline.setLineWidth(2);
		yline.setColor("blue");

		// add vertical cross hair part
		lst2.add(yline);
	}
*/

	private ScreenPt getScreenPointAtZoomLevel(ScreenPt initPt, WebPlot plot) {
		return new ScreenPt((int) ((initPt.getX()) * plot.getZoomFact()),
				(int) ((plot.getImageHeight() - initPt.getY()) * plot.getZoomFact()));
	}

	private void beautifyMe(ArrayList<ShapeDataObj> lst2) {
		// Makes the limit of the footprint not seen:
		lst2.get(0).setLineWidth(2);
	}

	// @Override
	// public boolean contains(ScreenPt pt, WebPlot plot) {
	// // check if point is in footprint shape
	// //don't want to be selected if inside the gre
	//// if(super.contains(pt, plot)){
	//// return true;
	//// }
	// int i=0;
	// for (ShapeDataObj shape : lst) {
	// if(i==0) continue;
	// ScreenPt pt0 = plot.getScreenCoords(shape.getPts()[0]);
	// int x = pt0.getIX();
	// int y = pt0.getIY();
	// ScreenPt pt1 = plot.getScreenCoords(shape.getPts()[1]);
	// int width = Math.abs(pt0.getIX() - pt1.getIX());
	// int height = Math.abs(pt0.getIY() - pt1.getIY());
	//
	// if (pt != null && VisUtil.contains(x, y, width, height, pt.getIX(),
	// pt.getIY())) {
	// return true;
	// }
	// i++;
	// }
	//
	// return false;
	// }

	// public ScreenPt getCenter(WebPlot plot) {
	//
	// ScreenPt retval= null;
	// if (startPt!=null && endPt!=null) {
	// ScreenPt pt0= plot.getScreenCoords(startPt);
	// ScreenPt pt1= plot.getScreenCoords(endPt);
	// if (pt0==null || pt1==null) return null;
	//
	// int cx= Math.min(pt0.getIX(),pt1.getIX()) +
	// Math.abs(pt0.getIX()-pt1.getIX())/2;
	// int cy= Math.min(pt0.getIY(),pt1.getIY()) +
	// Math.abs(pt0.getIY()-pt1.getIY())/2;
	// retval= new ScreenPt(cx,cy);
	//
	// }
	// return retval;
	// }
	//
	
	@Override
	public void setEndPt(WorldPt endPt, WebPlot plot) {
		// Called from rotate mode - should be anything that user rotate any 4
		// corners
		super.endPt = endPt; // don't change radius!

		if (super.endPt == null)
			return;
		GwtUtil.logToServer(Level.INFO, "setEndPt - endPt (lon,lat)=" + endPt.getLon() + ", " + endPt.getLat());
		GwtUtil.logToServer(Level.INFO, "setEndPt - endPt (x,y)=" + endPt.getX() + ", " + endPt.getY());
		double angleDegrees = VisUtil.computeDistance(getStartPt(), getEndPt());
		double rad = Math.toRadians(angleDegrees);

		ScreenPt ep = plot.getScreenCoords(getEndPt());
		ScreenPt sp = plot.getScreenCoords(getStartPt());
		
		int xdiff = ep.getIX() - sp.getIX();
		int ydiff = ep.getIY() - sp.getIY();		
		rad = Math.atan2(ydiff, xdiff);
		GwtUtil.logToServer(Level.INFO, "setEndPt - angle rotated rad=" + rad + ", deg: " + Math.toDegrees(rad));
		ScreenPt center = getCenter(plot);
		WorldPt wc = plot.getWorldCoords(center);
		// should we rotate rectangle
		for (DrawObj drawObj : lst) {
			if (drawObj instanceof ShapeDataObj) {
				ShapeDataObj shapeDataObj = (ShapeDataObj) drawObj;

				if (!shapeDataObj.getShape().equals(ShapeType.Rectangle)) {
					continue;// don't rotate circle or crosshair, only rectangle
				}
				shapeDataObj.rotateAround(plot, rad, wc);
			}
		}
	}

	//
	@Override
	public void adjustStartEnd(WebPlot plot) {
		// Called from moving it - start - this should be same center as the
		// circular marker that is extending it.
		// Sets the center x,y point after drag it.
		GwtUtil.logToServer(Level.INFO, "adjustStartEnd - start=" + getStartPt().getX() + ", " + getStartPt().getY());
		GwtUtil.logToServer(Level.INFO, "adjustStartEnd - end=" + getEndPt().getX() + ", " + getEndPt().getY());
		ScreenPt pt0 = plot.getScreenCoords(getStartPt());
		ScreenPt pt1 = plot.getScreenCoords(getEndPt());
		GwtUtil.logToServer(Level.INFO, "adjustStartEnd - pt0=" + pt0.getX() + ", " + pt0.getY());
		GwtUtil.logToServer(Level.INFO, "adjustStartEnd - pt1=" + pt1.getX() + ", " + pt1.getY());
		// WorldPt center = plot.getWorldCoords(getCenter(plot));
		// for (ShapeDataObj shapeDataObj : lst) {
		// if (shapeDataObj.getShape().equals(ShapeType.Circle)) {
		// continue;// don't move again circle, only crosshair and
		// // rectangles
		// }
		// // 3. Translate others shapes but circle
		//
		// shapeDataObj.translateTo(plot, center);
		// }
	}

	//
	// @Override
	// public void setTitle(String title) {
	// // Set title on the centre marker
	// this.shapes.get(0).setTitle(title);
	// }
	//
	// @Override
	// public void setTitleCorner(Corner c) {
	// // Set title corner on the centre marker
	// this.shapes.get(0).setTitleCorner(c);
	// }
	//
	@Override
	public List<DrawObj> getShape() {

		return lst;
	}

}
