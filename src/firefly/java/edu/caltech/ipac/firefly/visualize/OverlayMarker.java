/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import java.util.List;

import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.ShapeDataObj;
import edu.caltech.ipac.visualize.plot.WorldPt;

/**
 * Interface for defining what a marker should be overlaid
 * 
 * @author ejoliet
 *
 */
public interface OverlayMarker {

	public enum Corner {
		NE, NW, SE, SW
	};

	public void move(WorldPt center, WebPlot plot);

	/**
	 * Test if the screen point on the plot contains the marker
	 * 
	 * @param pt
	 *            x,y point to test
	 * @param plot
	 *            plot to test
	 * @return true if the screen x,y coordinate is in the marker at the plot
	 */
	public boolean contains(ScreenPt pt, WebPlot plot);

	public ScreenPt getCenter(WebPlot plot);

	public int getCenterDistance(ScreenPt pt, WebPlot plot);

	public void adjustStartEnd(WebPlot plot);

//	public boolean containsSquare(ScreenPt pt, WebPlot plot);

	public String getTitle();

	public void setTitle(String title);

	public String getFont();

	public void setTitleCorner(Corner c);

	public Corner getTextCorner();

//	public void setIsShown(boolean isDisp);
//	
//	public boolean isShown();
	
	public boolean isReady();

	public WorldPt getStartPt();

	public WorldPt getEndPt();

	public void setEndPt(WorldPt endPt, WebPlot plot);

	// public OffsetScreenPt getTitlePtOffset();

	public void setEditCorner(Corner corner, WebPlot plot);

	public MinCorner getMinCornerDistance(ScreenPt pt, WebPlot plot);

	public ScreenPt getCorner(Corner corner, WebPlot plot);

	public static class MinCorner {
		private final Corner corner;
		private final int distance;

		public MinCorner(Corner corner, int distance) {
			this.corner = corner;
			this.distance = distance;
		}

		public int getDistance() {
			return distance;
		}

		public Corner getCorner() {
			return corner;
		}
	}

	/**
	 * @return the shape to be draw (affine transform should be done before calling this!)
	 */
	public List<DrawObj> getShape();
}
