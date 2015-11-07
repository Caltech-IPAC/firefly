/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.draw;

import edu.caltech.ipac.firefly.visualize.ScreenPt;
import edu.caltech.ipac.firefly.visualize.ViewPortPt;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.util.dd.Region;
import edu.caltech.ipac.util.dd.RegionLines;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.List;

/**
 * Draw one or more closed polygons.
 *
 * This class can only be used with a WebPlot, You must use WorldPt
 *
 * @author tatianag, Trey
 * @version $Id: FootprintObj.java,v 1.14 2012/11/30 23:17:01 roby Exp $
 */
public class FootprintObj extends DrawObj {

	public static final int DEF_WIDTH = 1;

	public enum Style {
		STANDARD, HANDLED
	}

	private final List<WorldPt[]> _fpList;
	private Style _style = Style.STANDARD;
	private int lineWidth = DEF_WIDTH;

	/**
	 * pass a array of WorldPt that represents the corners of a Polygon
	 * 
	 * @param ptAry
	 *            the array of points of a polygon
	 *
	 */
	public FootprintObj(WorldPt[] ptAry) {
		super();
		_fpList = new ArrayList<WorldPt[]>(1);
		_fpList.add(ptAry);
	}

	/**
	 * pass a list of polygons, each polygon is an array of WorldPt that are the
	 * corners of the polygon
	 * 
	 * @param ptList
	 *            of list of arrays of points of a polygon
	 *
	 */
	public FootprintObj(List<WorldPt[]> ptList) {
		super();
		_fpList = ptList;
	}

	@Override
	public boolean getCanUsePathEnabledOptimization() {
		return _style == Style.STANDARD && lineWidth == 1;
	}

	@Override
	public int getLineWidth() {
		return DEF_WIDTH;
	}

	private void setLineWidth(int w) {
		lineWidth = w;
	}

	public List<WorldPt[]> getPos() {
		return _fpList;
	}

	public void setStyle(Style s) {
		_style = s;
	}

	public Style getStyle() {
		return _style;
	}

	// public double getScreenDistOLD(WebPlot plot, ScreenPt pt)
	// throws ProjectionException {
	// double minDist= Float.MAX_VALUE;
	//
	//
	// // TODO use distance to line instead of distance to point
	// for (WorldPt ptAry[] : getPos()) {
	// for (WorldPt wpt : ptAry) {
	// ScreenPt testPt= plot.getScreenCoords(wpt);
	// if (testPt != null) {
	// double dx= pt.getIX() - testPt.getIX();
	// double dy= pt.getIY() - testPt.getIY();
	// double dist= Math.sqrt(dx*dx + dy*dy);
	// if (dist > 0 && dist < minDist) {
	// minDist= dist;
	// }
	// }
	// }
	// }
	// return minDist;
	// }

	public double getScreenDist(WebPlot plot, ScreenPt pt) {
		double minDistSq = Double.MAX_VALUE;

		double distSq;
		for (WorldPt ptAry[] : getPos()) {
			ScreenPt last = null;
			int totX = 0;
			int totY = 0;
			for (WorldPt wpt : ptAry) {
				ScreenPt testPt = plot.getScreenCoords(wpt);
				if (last == null)
					last = plot.getScreenCoords(ptAry[ptAry.length - 1]);
				if (testPt != null) {
					distSq = ptSegDistSq(testPt.getIX(), testPt.getIY(), last.getIX(), last.getIY(), pt.getIX(),
							pt.getIY());
					totX += testPt.getIX();
					totY += testPt.getIY();
					if (distSq < minDistSq) {
						minDistSq = distSq;
					}
				}
				last = testPt;
			}
			float aveX = (float) totX / ptAry.length;
			float aveY = (float) totY / ptAry.length;
			distSq = distToPtSq(aveX, aveY, pt.getIX(), pt.getIY());
			if (distSq < minDistSq) {
				minDistSq = distSq;
			}
		}

		return Math.sqrt(minDistSq);
	}

	@Override
	public Pt getCenterPt() {
		double xSum = 0;
		double ySum = 0;
		double xTot = 0;
		double yTot = 0;

		for (WorldPt ptAry[] : getPos()) {
			for (WorldPt wpt : ptAry) {
				xSum += wpt.getX();
				ySum += wpt.getY();
				xTot++;
				yTot++;
			}
		}
		return new WorldPt(xSum / xTot, ySum / yTot);
	}

	public void draw(Graphics jg, WebPlot p, DrawingDef def, boolean useStateColor, boolean onlyAddToPath)
			throws UnsupportedOperationException {
		drawFootprint(jg, p, def, useStateColor, onlyAddToPath);
	}

	public void draw(Graphics g, DrawingDef def, boolean useStateColor, boolean onlyAddToPath)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException("this type only supports drawing with WebPlot");
	}

	private void drawFootprint(Graphics jg, WebPlot plot, DrawingDef def, boolean useStateColor,
			boolean onlyAddToPath) {

		boolean inView = false;
		for (WorldPt ptAry[] : _fpList) {
			for (WorldPt wpt : ptAry) {
				if (wpt != null && plot.pointInViewPort(wpt)) {
					inView = true;
					break;
				}
			}
			if (inView)
				break;
		}

		if (inView) {
			for (WorldPt ptAry[] : _fpList) {
				switch (_style) {
				case STANDARD:
					drawStandardFootprint(jg, ptAry, plot, def, useStateColor, onlyAddToPath);
					break;
				case HANDLED:
					drawHandledFootprint(jg, ptAry, plot, def, useStateColor);
					break;
				default:
					break;
				}
			}
		}
	}

	private void drawStandardFootprint(Graphics g, WorldPt[] ptAry, WebPlot plot, DrawingDef def, boolean useStateColor,
			boolean onlyAddToPath) {

		WorldPt wpt0 = ptAry[ptAry.length - 1];
		String color = calculateColor(def, useStateColor);
		if (!onlyAddToPath)
			g.beginPath(color, lineWidth);
		for (WorldPt wpt : ptAry) {
			ViewPortPt pt0 = plot.getViewPortCoords(wpt0);
			ViewPortPt pt = plot.getViewPortCoords(wpt);
			if (pt0 == null || pt == null)
				return;
			// g.drawLine(color, DEF_WIDTH, pt0.getIX(), pt0.getIY(),
			// pt.getIX(), pt.getIY());
			g.pathMoveTo(pt0.getIX(), pt0.getIY());
			if (!plot.coordsWrap(wpt0, wpt)) {
				g.pathLineTo(pt.getIX(), pt.getIY());
			}
			wpt0 = wpt;
		}
		if (!onlyAddToPath)
			g.drawPath();
	}

	private void drawHandledFootprint(Graphics jg, WorldPt[] ptAry, WebPlot plot, DrawingDef def,
			boolean useStateColor) {
		WorldPt wpt0 = ptAry[ptAry.length - 1];
		for (WorldPt wpt : ptAry) {
			if (!wpt.equals(wpt0)) {
				ViewPortPt pt0 = plot.getViewPortCoords(wpt0);
				ViewPortPt pt = plot.getViewPortCoords(wpt);
				if (pt0 == null || pt == null)
					return;
				wpt0 = wpt;
				String color = calculateColor(def, useStateColor);
				jg.drawLine(color, 1, pt0.getIX(), pt0.getIY(), pt.getIX(), pt.getIY());
				DrawUtil.drawHandledLine(jg, "red", pt0.getIX(), pt0.getIY(), pt.getIX(), pt.getIY());
			}
		}
	}

	@Override
	public boolean getSupportDuplicate() {
		return true;
	}

	@Override
	public DrawObj duplicate() {
		FootprintObj obj = new FootprintObj(_fpList);
		obj.setStyle(_style);
		obj.copySetting(this);
		return obj;
	}

	public static double distToPtSq(float x0, float y0, float x1, float y1) {
		double dx = x1 - x0;
		double dy = y1 - y0;
		return dx * dx + dy * dy;
	}

	@Override
	public List<Region> toRegion(WebPlot plot, DrawingDef def) {
		List<Region> retList = new ArrayList<Region>(_fpList.size());
		String color = calculateColor(def, false);
		for (WorldPt ptAry[] : _fpList) {
			RegionLines rl = new RegionLines(ptAry);
			rl.getOptions().setColor(color);
			retList.add(rl);
		}
		return retList;
	}

	/**
	 * Returns the square of the distance from a point to a line segment. The
	 * distance measured is the distance between the specified point and the
	 * closest point between the specified endpoints. If the specified point
	 * intersects the line segment in between the endpoints, this method returns
	 * 0.0.
	 *
	 * <br>
	 * COPIED from java.awt.geom.Line2D, all documentation of this method is
	 * from Line2D. I did change from doubles to int in the method since I did
	 * not need the double precision <br>
	 *
	 * @param X1,&nbsp;Y1
	 *            the coordinates of the beginning of the specified line segment
	 * @param X2,&nbsp;Y2
	 *            the coordinates of the end of the specified line segment
	 * @param PX,&nbsp;PY
	 *            the coordinates of the specified point being measured against
	 *            the specified line segment
	 * @return a double value that is the square of the distance from the
	 *         specified point to the specified line segment.
	 */
	public static double ptSegDistSq(int X1, int Y1, int X2, int Y2, int PX, int PY) {
		// Adjust vectors relative to X1,Y1
		// X2,Y2 becomes relative vector from X1,Y1 to end of segment
		X2 -= X1;
		Y2 -= Y1;
		// PX,PY becomes relative vector from X1,Y1 to test point
		PX -= X1;
		PY -= Y1;
		int dotprod = PX * X2 + PY * Y2;
		double projlenSq;
		if (dotprod <= 0) {
			// PX,PY is on the side of X1,Y1 away from X2,Y2
			// distance to segment is length of PX,PY vector
			// "length of its (clipped) projection" is now 0.0
			projlenSq = 0.0;
		} else {
			// switch to backwards vectors relative to X2,Y2
			// X2,Y2 are already the negative of X1,Y1=>X2,Y2
			// to get PX,PY to be the negative of PX,PY=>X2,Y2
			// the dot product of two negated vectors is the same
			// as the dot product of the two normal vectors
			PX = X2 - PX;
			PY = Y2 - PY;
			dotprod = PX * X2 + PY * Y2;
			if (dotprod <= 0) {
				// PX,PY is on the side of X2,Y2 away from X1,Y1
				// distance to segment is length of (backwards) PX,PY vector
				// "length of its (clipped) projection" is now 0.0
				projlenSq = 0.0;
			} else {
				// PX,PY is between X1,Y1 and X2,Y2
				// dotprod is the length of the PX,PY vector
				// projected on the X2,Y2=>X1,Y1 vector times the
				// length of the X2,Y2=>X1,Y1 vector
				projlenSq = (double) (dotprod * dotprod) / (double) (X2 * X2 + Y2 * Y2);
			}
		}
		// Distance to line is now the length of the relative point
		// vector minus the length of its projection onto the line
		// (which is zero if the projection falls outside the range
		// of the line segment).
		double lenSq = PX * PX + PY * PY - projlenSq;
		if (lenSq < 0) {
			lenSq = 0;
		}
		return lenSq;
	}

	/**
	 * Build world points translated to apt
	 * 
	 * @param plot
	 * @param apt
	 *            world point to translate to
	 */
	public void translateTo(WebPlot plot, WorldPt apt) {
		// Need to clear fist the original array of point...
		synchronized (_fpList) {
			ScreenPt pt = plot.getScreenCoords(apt);
			WorldPt[] arr = _fpList.get(0);
			WorldPt[] arr2 = new WorldPt[arr.length];

			_fpList.clear();
			int i = 0;
			for (WorldPt wpt : arr) {
				ScreenPt pti = plot.getScreenCoords(wpt);

				int x2 = pti.getIX();
				int y2 = pti.getIY();
				x2 += pt.getIX();
				y2 += pt.getIY();
				arr2[i++] = plot.getWorldCoords(new ScreenPt(x2, y2));
			}
			// ... and recreate the ones translated

			_fpList.add(arr2);
		}
	}

	/**
	 * Rotate all points around a center wc
	 * 
	 * @param plot
	 *            plot object
	 * @param angle
	 *            angle to rotate in radians
	 * @param wc
	 *            center world point to rotate around
	 */
	public void rotateAround(WebPlot plot, double angle, WorldPt wc) {
		ScreenPt center = plot.getScreenCoords(wc);
		synchronized (_fpList) {

			WorldPt[] arr = _fpList.get(0);
			WorldPt[] arr2 = new WorldPt[arr.length];

			_fpList.clear();
			int i = 0;
			for (WorldPt p1 : arr) {
				// TRANSLATE TO ORIGIN
				ScreenPt pti = plot.getScreenCoords(p1);
				double xc = center.getX();
				double x1 = pti.getX() - xc;
				double yc = center.getY();
				double y1 = pti.getY() - yc;

				// APPLY ROTATION
				double temp_x1 = x1 * Math.cos(angle) - y1 * Math.sin(angle);
				double temp_y1 = x1 * Math.sin(angle) + y1 * Math.cos(angle);

				// TRANSLATE BACK
				arr2[i++] = plot.getWorldCoords(new ScreenPt(temp_x1 + xc, temp_y1 + yc));
			}
			_fpList.add(arr2);
		}

	}

}
