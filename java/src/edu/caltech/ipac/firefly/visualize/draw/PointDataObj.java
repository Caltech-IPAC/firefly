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

import java.util.Arrays;
import java.util.List;


/**
 * User: roby
 * Date: Jun 18, 2008
 * Time: 12:35:43 PM
 */
public class PointDataObj extends DrawObj {

    public static final int DEFAULT_SIZE= 4;
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


    public void draw(Graphics g, WebPlot p, AutoColor ac, boolean useStateColor) throws UnsupportedOperationException {
        drawPt(g,p,ac,useStateColor);
    }

    public void draw(Graphics g, AutoColor ac, boolean useStateColor) throws UnsupportedOperationException {
        drawPt(g,null,ac,useStateColor);
    }



    private void drawPt(Graphics jg, WebPlot plot, AutoColor auto, boolean useStateColor)
                                                       throws UnsupportedOperationException {
        Pt ipt= _pt;
        if (plot!=null) {
            try {
                if (plot.pointInPlot(ipt)) {
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

                    if (draw)  drawXY(jg,x,y,calculateColor(auto,useStateColor),useStateColor);

                }
            } catch (ProjectionException e) {
                // do nothing
            }
        }
        else  if (_pt instanceof ScreenPt) {
                ScreenPt sp= (ScreenPt)_pt;
                drawXY(jg,sp.getIX(),sp.getIY(),calculateColor(auto,useStateColor), useStateColor);
        }
        else {
            throw new UnsupportedOperationException ("this type only supports drawing with WebPlot or ScreenPt");
        }
    }

    private void drawXY(Graphics g, int x, int y, String color,boolean useStateColor) {
        DrawSymbol s= _symbol;
        if (useStateColor && isHighlighted()) s= _highlightSymbol;
        drawSymbolOnPlot(g, x,y, s,color);
        if (_text!=null) {
            g.drawText(color,"9px",x+5,y,_text);
        }
    }


    private void drawSymbolOnPlot(Graphics jg,
                                    int x,
                                    int y,
                                    DrawSymbol shape,
                                    String color) {
        switch (shape) {
            case X :
                drawX(jg, x, y, color);
                break;
            case EMP_CROSS :
                drawEmpCross(jg, x, y, color, "white");
                break;
            case CROSS :
                drawCross(jg, x, y, color);
                break;
            case SQUARE :
                drawSquare(jg, x, y, color);
                break;
            case SQUARE_X :
                drawSquareX(jg, x, y, color);
                break;
            case DIAMOND :
                drawDiamond(jg, x, y, color);
                break;
            case DOT :
                drawDot(jg, x, y, color);
                break;
            case CIRCLE :
                drawCircle(jg, x, y, color);
                break;
            default :
                assert false; // if more shapes are added they must be added here
                break;
        }
    }




    public void drawX(Graphics jg, int x, int y, String color) {
        jg.drawLine( color,  1,x-size,y-size, x+size, y+size);
        jg.drawLine( color,  1, x-size,y+size, x+size, y-size);
    }

    public void drawSquareX(Graphics jg, int x, int y, String color) {
        drawX(jg,x,y,color);
        drawSquare(jg,x,y,color);
    }

    public void drawSquare(Graphics jg, int x, int y, String color) {
        jg.drawRec(color, 1, x-size,y-size, 2*size, 2*size);
    }

    public void drawCross(Graphics jg, int x, int y, String color) {
        jg.drawLine( color, 1, x-size,y, x+size, y);
        jg.drawLine( color, 1, x,y-size, x, y+size);
    }


    public void drawEmpCross(Graphics jg, int x, int y, String color1, String color2) {
        jg.drawLine( color1, 1, x-size,y, x+size, y);
        jg.drawLine( color1, 1, x,y-size, x, y+size);

        jg.drawLine( color2, 1, x-(size+1),y, x-(size+2), y);
        jg.drawLine( color2, 1, x+(size+1),y, x+(size+2), y);

        jg.drawLine( color2, 1, x,y-(size+1), x, y-(size+2));
        jg.drawLine( color2, 1, x,y+(size+1), x, y+(size+2));
    }


    public void drawDiamond(Graphics jg, int x, int y, String color) {
        jg.drawLine( color, 1, x,y-size, x+size, y);
        jg.drawLine( color, 1, x+size, y, x, y+size);
        jg.drawLine( color, 1, x, y+size, x-size,y);
        jg.drawLine( color, 1, x-size,y, x,y-size);
    }

    public static void drawDot(Graphics jg, int x, int y, String color) {
        jg.drawLine( color, 2, x-1,y, x+1, y);
    }

    public void drawCircle(Graphics jg, int x, int y, String color) {
        jg.drawCircle( color, 1, x,y,size+2);
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
