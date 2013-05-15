package edu.caltech.ipac.firefly.visualize.draw;

import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.util.dd.Region;
import edu.caltech.ipac.util.dd.RegionAnnulus;
import edu.caltech.ipac.util.dd.RegionBox;
import edu.caltech.ipac.util.dd.RegionBoxAnnulus;
import edu.caltech.ipac.util.dd.RegionDimension;
import edu.caltech.ipac.util.dd.RegionFont;
import edu.caltech.ipac.util.dd.RegionLines;
import edu.caltech.ipac.util.dd.RegionOptions;
import edu.caltech.ipac.util.dd.RegionPoint;
import edu.caltech.ipac.util.dd.RegionText;
import edu.caltech.ipac.util.dd.RegionValue;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/**
 * User: roby
 * Date: Jul 16, 2010
 * Time: 12:52:28 PM
 */


/**
* @author Trey Roby
*/
public class RegionConnection implements DataConnection {

    private List<Region> regionList;
    private List<DrawObj> drawData= null;
    private final WebEventManager _evManager = new WebEventManager();
    private static int titleCnt = 1;
    private String title;


    public RegionConnection(List<Region> regionList) {
        this("DS9 Region " + (titleCnt++), regionList);
    }
    public RegionConnection(String title, List<Region> regionList) {
        this.regionList= regionList;
        this.title= title;
    }

    public String getTitle(WebPlot plot) {
            return title;
    }
    public int size() { return regionList.size(); }
    public boolean isActive() { return true; }


    public void showDetails(int x, int y, int index) {  }
    public void hideDetails() {  }
    public WebEventManager getEventManager() { return _evManager; }

    public boolean getSupportsSelection() { return false; }
    public boolean getSupportsMouse() { return false; }
    public boolean getOnlyIfDataVisible() { return true; }

    public boolean getHasPerPlotData() { return true; }
    public boolean isPointData() { return false; }

    public DrawConnector getDrawConnector() { return null; }

    public String getInitDefaultColor() { return "red"; }

    public String getHelpLine() { return "Shows DS9 region data you read in"; }

    public boolean isDataVisible() { return true; }

    public AsyncDataLoader getAsyncDataLoader() { return null; }

    public int[] getHighlightedIdx() {
        List<Integer> hiList= new ArrayList<Integer>(10);
        for(int i= 0; (i<regionList.size()); i++) {
            if (regionList.get(i).isSelected()) hiList.add(i);
        }

        int[] retval= new int[hiList.size()];
        for(int i= 0; (i<retval.length); i++) {
            retval[i]= hiList.get(i);
        }

        return retval;
    }

    public void setHighlightedIdx(int... idx) {
        for(Region r : regionList) { r.setSelected(false); }
        for(int i : idx) {
            if (i<regionList.size()) {
                regionList.get(i).setSelected(true);
            }
        }
        _evManager.fireEvent(new WebEvent<Integer>(this, TablePanel.ON_ROWHIGHLIGHT_CHANGE, idx[0]));
    }

    public List<DrawObj> getData(boolean rebuild, WebPlot plot) {
        drawData= new ArrayList<DrawObj>(regionList.size()*2);
        for(Region r : regionList) {
            DrawObj drawObj=makeRegionDrawObject(r,plot);
            if (drawObj!=null) drawData.add(drawObj);
        }
        return drawData;
    }


    public DrawObj makeRegionDrawObject(Region r, WebPlot plot) {
        DrawObj retval= null;
        if (r.getOptions().isInclude()) {
            if      (r instanceof RegionAnnulus)    retval= makeAnnulus((RegionAnnulus) r, plot);
            else if (r instanceof RegionBox)        retval= makeBox((RegionBox) r, plot);
            else if (r instanceof RegionBoxAnnulus) retval= makeBoxAnnulus((RegionBoxAnnulus) r, plot);
            else if (r instanceof RegionLines)      retval= makeLines((RegionLines) r);
            else if (r instanceof RegionPoint)      retval= makePoint((RegionPoint) r);
            else if (r instanceof RegionText)       retval= makeText((RegionText) r);
        }

        return retval;

    }

    private static int getValueInScreenPixel(WebPlot plot, RegionValue value) {
        double retval=1;
        if (value.isWorldCoords()) {
            retval= value.toDegree()*plot.getImagePixelScaleInDeg()*plot.getZoomFact();
        }
        else if (value.getType()==RegionValue.Unit.SCREEN_PIXEL ||
                 value.getType()==RegionValue.Unit.CONTEXT) {
            retval= value.getValue();
        }
        else if (value.getType()== RegionValue.Unit.IMAGE_PIXEL) {
            retval= value.getValue()*plot.getZoomFact();
        }
        return (int)retval;

    }

    private DrawObj makeAnnulus(RegionAnnulus ra, WebPlot plot) {
        DrawObj retval;
        if (ra.isCircle())  {
            retval= ShapeDataObj.makeCircle( ra.getPt(), getValueInScreenPixel(plot,ra.getRadii()[0]));
            retval.setUserColor(ra.getColor());
        }
        else {
            MultiShapeObj multi= new MultiShapeObj();
            for(int i=0; (i<ra.getRadii().length); i++) {
                ShapeDataObj sdO= ShapeDataObj.makeCircle( ra.getPt(), getValueInScreenPixel(plot,ra.getRadii()[i]));
                sdO.setUserColor(ra.getColor());
                multi.addDrawObj(sdO);
            }
            retval= multi;
        }
        return retval;

    }

    private DrawObj makeBox(RegionBox rb, WebPlot plot) {
        RegionDimension dim= rb.getDim();
        ShapeDataObj sdO= ShapeDataObj.makeRectangle(rb.getPt(),
                                           getValueInScreenPixel(plot,dim.getWidth()),
                                           getValueInScreenPixel(plot,dim.getHeight()));
        sdO.setUserColor(rb.getColor());
        return sdO;
    }

    private DrawObj makeBoxAnnulus(RegionBoxAnnulus rb, WebPlot plot) {
        MultiShapeObj multi= new MultiShapeObj();
        for(int i=0; (i<rb.getDim().length); i++) {
            RegionDimension dim= rb.getDim()[i];
            ShapeDataObj sdO=  ShapeDataObj.makeRectangle(rb.getPt(),
                                                          getValueInScreenPixel(plot,dim.getWidth()),
                                                          getValueInScreenPixel(plot,dim.getHeight()) );
            sdO.setUserColor(rb.getColor());
        }
        return multi;
    }

    private DrawObj makeLines(RegionLines rl) {
        DrawObj retval;
        if (rl.isPolygon()) {
            List<WorldPt> wpList= new ArrayList<WorldPt>(rl.getPtAry().length+1);
            wpList.addAll(Arrays.asList(rl.getPtAry()));
            wpList.add(rl.getPtAry()[0]);
            retval= new FootprintObj(wpList.toArray(new WorldPt[wpList.size()]));
            retval.setUserColor(rl.getColor());
        }
        else {
            retval= ShapeDataObj.makeLine(rl.getPtAry()[0], rl.getPtAry()[1]);
            retval.setUserColor(rl.getColor());
        }
        return retval;

    }

    private DrawObj makePoint(RegionPoint rp) {
        PointDataObj ptObj= new PointDataObj(rp.getPt());
        ptObj.setSymbol(convertSymbol(rp.getPointType()));
        if (rp.getPointSize()>0) ptObj.setSize(rp.getPointSize());
        ptObj.setUserColor(rp.getColor());
        return ptObj;

    }

    private DrawObj makeText(RegionText rt) {
        RegionOptions op= rt.getOptions();
        RegionFont font= op.getFont();
        ShapeDataObj retval= ShapeDataObj.makeText(rt.getPt(), op.getText());
        retval.setFontName(font.getName());
        retval.setFontSize(font.getPt()+"pt");
        retval.setFontWeight(font.isBold() ? "bold" : "normal");
        retval.setFontStyle(font.isItalic() ? "italic" : "normal");
        retval.setUserColor(rt.getColor());
        return retval;
    }

    private DrawSymbol convertSymbol(RegionPoint.PointType type) {
        DrawSymbol symbol;
        switch (type) {
            case Circle:    symbol= DrawSymbol.CIRCLE; break;
            case Box:       symbol= DrawSymbol.SQUARE; break;
            case Diamond:   symbol= DrawSymbol.DIAMOND; break;
            case Cross:     symbol= DrawSymbol.CROSS; break;
            case X:         symbol= DrawSymbol.X; break;
            case Arrow:     symbol= DrawSymbol.X; break;
            case BoxCircle: symbol= DrawSymbol.SQUARE; break;
            default:        symbol= DrawSymbol.X; break;
        }
        return symbol;
    }
}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
