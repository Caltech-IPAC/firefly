/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

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

	public void updateRadius(WebPlot plot, boolean largeChangeOnly);

	public boolean contains(ScreenPt pt, WebPlot plot);

	public ScreenPt getCenter(WebPlot plot);

	public int getCenterDistance(ScreenPt pt, WebPlot plot);

	public void adjustStartEnd(WebPlot plot);

	public boolean containsSquare(ScreenPt pt, WebPlot plot);

	public String getTitle();

	public void setTitle(String title);

	public String getFont();

	public void setTitleCorner(Corner c);

	public Corner getTextCorner();

	public boolean isReady();

	public WorldPt getStartPt();

	public WorldPt getEndPt();

	public void setEndPt(WorldPt endPt, WebPlot plot);

	public OffsetScreenPt getTitlePtOffset();

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
}
