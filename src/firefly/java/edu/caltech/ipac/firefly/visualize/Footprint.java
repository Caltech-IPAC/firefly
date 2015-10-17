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
public class Footprint extends Marker {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ArrayList<ShapeDataObj> shapes;

	public Footprint() {
		super();
		shapes = new ArrayList<ShapeDataObj>();
		defineShapes(getStartPt());

	}

	private void readInRegions() {

	}

	private void defineShapes(WorldPt pt) {

		// PolygonMarker = pm = new PolygonMarker(vertices[])
		// add(getCircleMarker(10, 2));
		// add(getCircleMarker(15, 2));
		// add(getCircleMarker(50, 232)); // needs x,y start
		shapes.add(ShapeDataObj.makeRectangle(pt, 20, 5));
		shapes.add(ShapeDataObj.makeCircle(pt, 20));
	}

	private Marker getCircleMarker(int i, int j) {
		// FIXME is returnning several circles, i as radius
		return new CircularMarker(i) {
			//
			// @Override
			// public void move(WorldPt center, WebPlot plot) {
			// // TODO Auto-generated method stub
			// super.move(center, plot);
			// }
			//
		};
	}

	public List<ShapeDataObj> getShapes(WorldPt pt) {
		shapes.clear();
		defineShapes(pt);

		return shapes;
	}

	@Override
	public void move(WorldPt center, WebPlot plot) {
		// This call should move the center of the footprint and rest of

	}
}
