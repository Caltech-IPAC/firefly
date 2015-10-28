/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import java.util.ArrayList;
import java.util.List;

import edu.caltech.ipac.firefly.visualize.draw.ShapeDataObj;
import edu.caltech.ipac.visualize.plot.WorldPt;

/**
 * The class footprint define combination of shapes acting as multiple markes
 * grouped
 * 
 * @author Emmanuel Joliet
 */
public class FootprintAsMarkers implements OverlayMarker {

	private ArrayList<Marker> shapes;

	public FootprintAsMarkers() {
		shapes = new ArrayList<Marker>();
		shapes.add(new CircularMarker(60));
		shapes.add(new RectangleMarker(80, 40));

	}

	@Override
	public void move(WorldPt center, WebPlot plot) {
		// move all shapes
		for (Marker marker : shapes) {
			marker.move(center, plot);
		}
	}

	@Override
	public boolean contains(ScreenPt pt, WebPlot plot) {
		// check if point is in footprint shapse
		for (Marker marker : shapes) {
			if (marker.contains(pt, plot)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void setEndPt(WorldPt endPt, WebPlot plot) {
		// propagate endPt to all markers
		for (Marker marker : shapes) {
			marker.setEndPt(endPt, plot);
		}
	}

	@Override
	public void adjustStartEnd(WebPlot plot) {
		for (Marker marker : shapes) {
			marker.adjustStartEnd(plot);
		}
	}

	@Override
	public void setTitle(String title) {
		// Set title on the centre marker
		this.shapes.get(0).setTitle(title);
	}

	@Override
	public void setTitleCorner(Corner c) {
		// Set title corner on the centre marker
		this.shapes.get(0).setTitleCorner(c);
	}

	@Override
	public List<ShapeDataObj> getShape() {
		ArrayList<ShapeDataObj> lst = new ArrayList<ShapeDataObj>();
		for (Marker marker : shapes) {

			lst.addAll(marker.getShape());
			// lst.add(ShapeDataObj.makeCircle(getStartPt(), getEndPt()));
			// lst.add(ShapeDataObj.makeRectangle(getStartPt(), getEndPt()));
		}
		lst.trimToSize();
		return lst;
	}

	public boolean isReady() {
		boolean isReady = true;
		for (Marker marker : shapes) {
			isReady &= marker.getStartPt() != null && marker.getEndPt() != null;
		}
		return isReady;
	}

	@Override
	public ScreenPt getCenter(WebPlot plot) {

		return shapes.get(0).getCenter(plot);// should be the first marker as
												// the center (circle with
												// radius = size of footprint?)
	}

	@Override
	public int getCenterDistance(ScreenPt pt, WebPlot plot) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getTitle() {
		return shapes.get(0).getTitle();
	}

	@Override
	public String getFont() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Corner getTextCorner() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WorldPt getStartPt() {
		throw new RuntimeException(
				"getStartPt() Should not be used on the footprint but rather on the individual markers which is composed of");
	}

	@Override
	public WorldPt getEndPt() {
		throw new RuntimeException(
				"getEndPt() Should not be used on the footprint but rather on the individual markers which is composed of");
	}

	@Override
	public void setEditCorner(Corner corner, WebPlot plot) {
		// TODO Auto-generated method stub

	}

	@Override
	public MinCorner getMinCornerDistance(ScreenPt pt, WebPlot plot) {
		throw new RuntimeException(
				"Not implemented yet");
	}

	@Override
	public ScreenPt getCorner(Corner corner, WebPlot plot) {
		throw new RuntimeException(
				"Not implemented yet");
	}
}
