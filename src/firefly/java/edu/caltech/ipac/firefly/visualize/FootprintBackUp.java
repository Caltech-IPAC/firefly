/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import java.util.ArrayList;
import java.util.List;

import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.ShapeDataObj;
import edu.caltech.ipac.visualize.plot.WorldPt;

/**
 * The class footprint define combination of shapes acting as multiple markes
 * grouped
 * 
 * @author Emmanuel Joliet
 */
public class FootprintBackUp extends CircularMarker {

//	private ArrayList<Marker> shapes;
	int r;
	private ArrayList<DrawObj> lst;
	private int rectW=50;
	private int rectH=30;
	public FootprintBackUp() {
		super(80);// circle center represents the main reference shape as main 'marker' around multi-shape footprint
		r = 80;
		lst = new ArrayList<>();
	}

	@Override
	public void move(WorldPt center, WebPlot plot) {
		super.move(center, plot);
		moveFootprintDataTo(center,plot);
	}

	private void moveFootprintDataTo(WorldPt center, WebPlot plot) {
		lst.clear();
		
		//Circle main reference around footprint - limit the footprint and serves as corner limit too (+title label)
		lst.addAll(super.getShape()); // should be line-width 0 so is not seen or saved!
		
		
		// Actual footprint is here:
        ScreenPt cpt= plot.getScreenCoords(center);
        int deltax= (int)rectW/2;
        int deltay= (int)rectH/2;
        ScreenPt sp= new ScreenPt(cpt.getIX()-deltax, cpt.getIY()-deltay);
        ScreenPt ep= new ScreenPt(cpt.getIX()+deltax, cpt.getIY()+deltay);
        WorldPt pt0 = plot.getWorldCoords(sp);
        WorldPt pt1 = plot.getWorldCoords(ep);
        
		lst.add(ShapeDataObj.makeRectangle(pt0, pt1));
		
		//beautifyMe(lst);
		lst.trimToSize();
	}

	private void beautifyMe(ArrayList<ShapeDataObj> lst2) {
		// TODO Auto-generated method stub
		lst2.get(0).setLineWidth(0);
	}

	//
//	@Override
//	public boolean contains(ScreenPt pt, WebPlot plot) {
//		// check if point is in footprint shapse
//		for (Marker marker : shapes) {
//			if (marker.contains(pt, plot)) {
//				return true;
//			}
//		}
//
//		return false;
//	}
//
//	@Override
//	public void setEndPt(WorldPt endPt, WebPlot plot) {
//		// propagate endPt to all markers
//		for (Marker marker : shapes) {
//			marker.setEndPt(endPt, plot);
//		}
//	}
//
//	@Override
//	public void adjustStartEnd(WebPlot plot) {
//		for (Marker marker : shapes) {
//			marker.adjustStartEnd(plot);
//		}
//	}
//
//	@Override
//	public void setTitle(String title) {
//		// Set title on the centre marker
//		this.shapes.get(0).setTitle(title);
//	}
//
//	@Override
//	public void setTitleCorner(Corner c) {
//		// Set title corner on the centre marker
//		this.shapes.get(0).setTitleCorner(c);
//	}
//
	@Override
	public List<DrawObj> getShape() {
		
		return lst;
	}
//	
//    public boolean isReady() {
//        return (startPt!=null && endPt!=null);
//    }
}
