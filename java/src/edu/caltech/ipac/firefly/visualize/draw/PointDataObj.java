package edu.caltech.ipac.firefly.visualize.draw;

import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.visualize.ScreenPt;
import edu.caltech.ipac.firefly.visualize.ViewPortPt;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
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


    public void draw(Graphics jg, WebPlot p, boolean front, AutoColor ac) throws UnsupportedOperationException {
        jg.deleteShapes(getShapes());
        Shapes shapes= drawPt(jg,p,front,ac);
        setShapes(shapes);
    }

    public void draw(Graphics g, boolean front, AutoColor ac) throws UnsupportedOperationException {
        throw new UnsupportedOperationException ("this type only supports drawing with WebPlot");
    }



    private Shapes drawPt(Graphics jg, WebPlot plot, boolean front, AutoColor auto) {
        Shapes retval= null;
        String color;
        try {
            Pt ipt= _pt;
            if (plot.pointInPlot(ipt)) {
                color= calculateColor(auto);
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
                    retval= drawSymbolOnPlot(jg, x,y, _symbol,color ,front);
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
                                    String color,
                                    boolean front) {
        Shapes retval;
        switch (shape) {
            case X :
                retval= drawX(jg, x, y, color, front);
                break;
            case EMP_CROSS :
                retval= drawEmpCross(jg, x, y, color, "white", front);
                break;
            case CROSS :
                retval= drawCross(jg, x, y, color, front);
                break;
            case SQUARE :
                retval= drawSquare(jg, x, y, color, front);
                break;
            case DIAMOND :
                retval= drawDiamond(jg, x, y, color, front);
                break;
            case DOT :
                retval= drawDot(jg, x, y, color, front);
                break;
            case CIRCLE :
                retval= drawCircle(jg, x, y, color, front);
                break;
            default :
                retval= null;
                assert false; // if more shapes are added they must be added here
                break;
        }
        return retval;
    }




    public Shapes drawX(Graphics jg, int x, int y, String color,boolean front) {
        List<Shape> sList= new ArrayList<Shape>(10);
        sList.add(jg.drawLine( color, front, 1,x-size,y-size, x+size, y+size));
        sList.add(jg.drawLine( color, front, 1, x-size,y+size, x+size, y-size));
        return new Shapes(sList);
    }

    public Shapes drawSquare(Graphics jg, int x, int y, String color,boolean front) {
        List<Shape> sList= new ArrayList<Shape>(10);
        sList.add(jg.drawRec(color,front, 1, x-size,y-size, 2*size, 2*size));


        return new Shapes(sList);
    }

    public Shapes drawCross(Graphics jg, int x, int y, String color,boolean front) {
        List<Shape> sList= new ArrayList<Shape>(10);
        sList.add(jg.drawLine( color, front, 1, x-size,y, x+size, y));
        sList.add(jg.drawLine( color, front, 1, x,y-size, x, y+size));
        return new Shapes(sList);
    }


    public Shapes drawEmpCross(Graphics jg, int x, int y, String color1, String color2, boolean front) {
        List<Shape> sList= new ArrayList<Shape>(10);
        sList.add(jg.drawLine( color1, front, 1, x-size,y, x+size, y));
        sList.add(jg.drawLine( color1, front, 1, x,y-size, x, y+size));


        sList.add(jg.drawLine( color2, front, 1, x-(size+1),y, x-(size+2), y));
        sList.add(jg.drawLine( color2, front, 1, x+(size+1),y, x+(size+2), y));

        sList.add(jg.drawLine( color2, front, 1, x,y-(size+1), x, y-(size+2)));
        sList.add(jg.drawLine( color2, front, 1, x,y+(size+1), x, y+(size+2)));


        return new Shapes(sList);
    }


    public Shapes drawDiamond(Graphics jg, int x, int y, String color,boolean front) {
        List<Shape> sList= new ArrayList<Shape>(10);
        sList.add(jg.drawLine( color, front, 1, x,y-size, x+size, y));
        sList.add(jg.drawLine( color, front, 1, x+size, y, x, y+size));
        sList.add(jg.drawLine( color, front, 1, x, y+size, x-size,y));
        sList.add(jg.drawLine( color, front, 1, x-size,y, x,y-size));
        return new Shapes(sList);
    }

    public static Shapes drawDot(Graphics jg, int x, int y, String color,boolean front) {
        List<Shape> sList= new ArrayList<Shape>(10);
//        sList.add(jg.drawLine( color, front, 1, x-1,y-1, x+1, y-1));
        sList.add(jg.drawLine( color, front, 2, x-1,y, x+1, y));
//        sList.add(jg.drawLine( color, front, 1, x-1,y+1, x+1, y+1));
        return new Shapes(sList);
    }

    public Shapes drawCircle(Graphics jg, int x, int y, String color,boolean front) {
        List<Shape> sList= new ArrayList<Shape>(10);
        sList.add(jg.drawCircle( color, front, 1, x,y,size+2));
        return new Shapes(sList);
    }


}
