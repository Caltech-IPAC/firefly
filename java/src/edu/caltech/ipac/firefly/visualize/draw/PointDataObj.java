package edu.caltech.ipac.firefly.visualize.draw;

import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.visualize.ScreenPt;
import edu.caltech.ipac.firefly.visualize.ViewPortPt;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.util.dd.Region;
import edu.caltech.ipac.util.dd.RegionPoint;
import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * User: roby
 * Date: Jun 18, 2008
 * Time: 12:35:43 PM
 */
public class PointDataObj extends DrawObj {

    private static final int DEFAULT_SIZE= 4;
    private final Pt _pt;
    private DrawSymbol _symbol = DrawSymbol.X;
    private DrawSymbol _highlightSymbol = DrawSymbol.SQUARE_X;
    private String _text= null;
    private int size= DEFAULT_SIZE;

     public PointDataObj(WorldPt pt) {
         super();
        _pt= pt;
    }

    public PointDataObj(ScreenPt pt) {
        super();
        _pt= pt;
    }

    public void setSymbol(DrawSymbol s) { _symbol = s; }
    public DrawSymbol getSymbol() { return _symbol; }

    public void setHighlightSymbol(DrawSymbol s) { _highlightSymbol = s; }
    public DrawSymbol getHighlightSymbol() { return _highlightSymbol; }

    public void setText(String text) { _text= text; }
    public String getText(String text) { return _text; }

    public void setSize(int size) { this.size = size; }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public Pt getPos() { return _pt; }


    public double getScreenDist(WebPlot plot, ScreenPt pt)
            throws ProjectionException {
        double dist = -1;

        ScreenPt testPt= null;

        if (plot!=null) {
            testPt= plot.getScreenCoords(_pt);
        }
        else if (_pt instanceof ScreenPt) {
            testPt= (ScreenPt)_pt;
        }


        if (testPt != null) {
            double dx= pt.getIX() - testPt.getIX();
            double dy= pt.getIY() - testPt.getIY();
            dist= Math.sqrt(dx*dx + dy*dy);
        }

        return dist;
    }

    @Override
    public Pt getCenterPt() { return _pt; }


    public void draw(Graphics jg, WebPlot p, AutoColor ac, boolean useStateColor) throws UnsupportedOperationException {
        jg.deleteShapes(getShapes());
        Shapes shapes= drawPt(jg,p,ac,useStateColor);
        setShapes(shapes);
    }

    public void draw(Graphics g, AutoColor ac, boolean useStateColor) throws UnsupportedOperationException {
        throw new UnsupportedOperationException ("this type only supports drawing with WebPlot");
    }



    private Shapes drawPt(Graphics jg, WebPlot plot, AutoColor auto, boolean useStateColor) {
        Shapes retval= null;
        String color;
        try {
            Pt ipt= _pt;
            if (plot.pointInPlot(ipt)) {
                color= calculateColor(auto,useStateColor);
                int x= 0;
                int y= 0;
                boolean draw= false;

                if (_pt instanceof ScreenPt) {
                    x= ((ScreenPt)_pt).getIX();
                    y= ((ScreenPt)_pt).getIY();
                    draw= true;
                }
                else if (_pt instanceof WorldPt) {
                    ViewPortPt pt=plot.getViewPortCoords(ipt);
                    if (plot.pointInViewPort(pt)) {
                        x= pt.getIX();
                        y= pt.getIY();
                        draw= true;
                    }
                }
                else {
                    WebAssert.argTst(false, "should never happen");
                }

                if (draw) {
                    DrawSymbol s= _symbol;
                    if (useStateColor && isHighlighted()) s= _highlightSymbol;
                    retval= drawSymbolOnPlot(jg, x,y, s,color);
                    if (_text!=null) {
                        Shape textShape= jg.drawText(color,"9px",x+5,y,_text);
                        retval= retval.concat(textShape);
                    }
                }
            }
        } catch (ProjectionException e) {
            retval= null;
        }
        return retval;
    }


    private Shapes drawSymbolOnPlot(Graphics jg,
                                    int x,
                                    int y,
                                    DrawSymbol shape,
                                    String color) {
        Shapes retval;
        switch (shape) {
            case X :
                retval= drawX(jg, x, y, color);
                break;
            case EMP_CROSS :
                retval= drawEmpCross(jg, x, y, color, "white");
                break;
            case CROSS :
                retval= drawCross(jg, x, y, color);
                break;
            case SQUARE :
                retval= drawSquare(jg, x, y, color);
                break;
            case SQUARE_X :
                retval= drawSquareX(jg, x, y, color);
                break;
            case DIAMOND :
                retval= drawDiamond(jg, x, y, color);
                break;
            case DOT :
                retval= drawDot(jg, x, y, color);
                break;
            case CIRCLE :
                retval= drawCircle(jg, x, y, color);
                break;
            default :
                retval= null;
                assert false; // if more shapes are added they must be added here
                break;
        }
        return retval;
    }




    public Shapes drawX(Graphics jg, int x, int y, String color) {
        List<Shape> sList= new ArrayList<Shape>(10);
        sList.add(jg.drawLine( color,  1,x-size,y-size, x+size, y+size));
        sList.add(jg.drawLine( color,  1, x-size,y+size, x+size, y-size));
        return new Shapes(sList);
    }

    public Shapes drawSquareX(Graphics jg, int x, int y, String color) {
        Shapes s= drawX(jg,x,y,color);
        Shapes s2= drawSquare(jg,x,y,color);
        s.concat(s2);
        return s;
    }

    public Shapes drawSquare(Graphics jg, int x, int y, String color) {
        List<Shape> sList= new ArrayList<Shape>(10);
        sList.add(jg.drawRec(color, 1, x-size,y-size, 2*size, 2*size));


        return new Shapes(sList);
    }

    public Shapes drawCross(Graphics jg, int x, int y, String color) {
        List<Shape> sList= new ArrayList<Shape>(10);
        sList.add(jg.drawLine( color, 1, x-size,y, x+size, y));
        sList.add(jg.drawLine( color, 1, x,y-size, x, y+size));
        return new Shapes(sList);
    }


    public Shapes drawEmpCross(Graphics jg, int x, int y, String color1, String color2) {
        List<Shape> sList= new ArrayList<Shape>(10);
        sList.add(jg.drawLine( color1, 1, x-size,y, x+size, y));
        sList.add(jg.drawLine( color1, 1, x,y-size, x, y+size));


        sList.add(jg.drawLine( color2, 1, x-(size+1),y, x-(size+2), y));
        sList.add(jg.drawLine( color2, 1, x+(size+1),y, x+(size+2), y));

        sList.add(jg.drawLine( color2, 1, x,y-(size+1), x, y-(size+2)));
        sList.add(jg.drawLine( color2, 1, x,y+(size+1), x, y+(size+2)));


        return new Shapes(sList);
    }


    public Shapes drawDiamond(Graphics jg, int x, int y, String color) {
        List<Shape> sList= new ArrayList<Shape>(10);
        sList.add(jg.drawLine( color, 1, x,y-size, x+size, y));
        sList.add(jg.drawLine( color, 1, x+size, y, x, y+size));
        sList.add(jg.drawLine( color, 1, x, y+size, x-size,y));
        sList.add(jg.drawLine( color, 1, x-size,y, x,y-size));
        return new Shapes(sList);
    }

    public static Shapes drawDot(Graphics jg, int x, int y, String color) {
        List<Shape> sList= new ArrayList<Shape>(10);
        sList.add(jg.drawLine( color, 2, x-1,y, x+1, y));
        return new Shapes(sList);
    }

    public Shapes drawCircle(Graphics jg, int x, int y, String color) {
        List<Shape> sList= new ArrayList<Shape>(10);
        sList.add(jg.drawCircle( color, 1, x,y,size+2));
        return new Shapes(sList);
    }

    @Override
    public List<Region> toRegion(WebPlot plot, AutoColor ac) {
        Region r;
        WorldPt wp= WebPlot.getWorldPtRepresentation(_pt);
        switch (_symbol) {
            case X :
                r= new RegionPoint(wp, RegionPoint.PointType.X,size);
                break;
            case EMP_CROSS :
            case CROSS :
                r= new RegionPoint(wp, RegionPoint.PointType.Cross,size);
                break;
            case SQUARE :
                r= new RegionPoint(wp, RegionPoint.PointType.Box,size);
                break;
            case DIAMOND :
                r= new RegionPoint(wp, RegionPoint.PointType.Diamond,size);
                break;
            case DOT :
                r= new RegionPoint(wp, RegionPoint.PointType.Box,2);
                break;
            case CIRCLE :
                r= new RegionPoint(wp, RegionPoint.PointType.Circle,size);
                break;
            default :
                r= null;
                assert false; // if more shapes are added they must be added here
                break;
        }
        r.getOptions().setColor(calculateColor(ac,false));
        return Arrays.asList(r);
    }



}
