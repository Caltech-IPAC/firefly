package edu.caltech.ipac.firefly.visualize.draw;

import edu.caltech.ipac.firefly.visualize.ScreenPt;
import edu.caltech.ipac.firefly.visualize.VisUtil;
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
public class DirectionArrowDataObj extends DrawObj {


    private final Pt _startPt;
    private final Pt _endPt;
    private final String _text;

     public DirectionArrowDataObj(WorldPt startPt, WorldPt endPt, String text) {
         super();
         _startPt= startPt;
         _endPt= endPt;
         _text= text;
    }

    public DirectionArrowDataObj(ScreenPt startPt, ScreenPt endPt, String text) {
        super();
        _startPt= startPt;
        _endPt= endPt;
        _text= text;
    }


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public Pt getStartPt() { return _startPt; }
    public Pt getEndPt() { return _endPt; }

    public String getText() {return _text;}

    public double getScreenDist(WebPlot plot, ScreenPt pt)
            throws ProjectionException {
        double dist = -1;

        ScreenPt testPt= null;

        if (plot!=null) {
            testPt= plot.getScreenCoords(_startPt);
        }
        else if (_startPt instanceof ScreenPt) {
            testPt= (ScreenPt)_startPt;
        }


        if (testPt != null) {
            double dx= pt.getIX() - testPt.getIX();
            double dy= pt.getIY() - testPt.getIY();
            dist= Math.sqrt(dx*dx + dy*dy);
        }

        return dist;
    }

    @Override
    public Pt getCenterPt() { return  _startPt; }

    protected boolean getSupportsWebPlot() { return false; }

    public void draw(Graphics jg, WebPlot p, boolean front, AutoColor ac) throws UnsupportedOperationException {
        throw new UnsupportedOperationException ("this type only supports drawing with WebPlot");
    }

    public void draw(Graphics jg, boolean front, AutoColor ac) throws UnsupportedOperationException {
        jg.deleteShapes(getShapes());
        Shapes shapes= drawDirectionArrow(jg,ac);
        setShapes(shapes);
    }





    private Shapes drawDirectionArrow(Graphics jg, AutoColor ac) {

        List<Shape> sList= new ArrayList<Shape>(10);
        Shape s;

        ScreenPt pt1= (ScreenPt)_startPt;
        ScreenPt pt2= (ScreenPt)_endPt;
        String color=  calculateColor(ac);

        VisUtil.NorthEastCoords ret= VisUtil.getArrowCoords(pt1.getIX(), pt1.getIY(), pt2.getIX(), pt2.getIY());

        s= jg.drawLine(color, false, ret.x1, ret.y1, ret.x2, ret.y2);
        sList.add(s);
        s= jg.drawLine(color, false, 2, ret.barbX1,ret.barbY1, ret.barbX2,ret.barbY2);
        sList.add(s);

        s= jg.drawText(color, "9px", ret.textX,ret.textY, _text);
        sList.add(s);

        return new Shapes(s);
    }



//    private Shapes drawDirectionArrow(Graphics jg, AutoColor ac) {
//
//        List<Shape> sList= new ArrayList<Shape>(10);
//        Shape s;
//
//        ScreenPt pt1= (ScreenPt)_startPt;
//        ScreenPt pt2= (ScreenPt)_endPt;
//        String color=  calculateColor(ac);
//        String text= _text;
//
//        int x1= pt1.getIX();
//        int y1= pt1.getIY();
//        int x2= pt2.getIX();
//        int y2= pt2.getIY();
//        //------------
//
//        double barb_length = 10;
//
//        /* compute shaft angle from arrowhead to tail */
//        int delta_y = y2 - y1;
//        int delta_x = x2 - x1;
//        double shaft_angle = Math.atan2(delta_y, delta_x);
//        double barb_angle = shaft_angle - 20 * Math.PI / 180; // 20 degrees from shaft
//        double barbX = x2 - barb_length * Math.cos(barb_angle);  // end of barb
//        double barbY = y2 - barb_length * Math.sin(barb_angle);
//
//        float extX= x2+6;
//        float extY= y2+6;
//
//        int diffX= x2-x1;
//        int mult= ((y2<y1)? -1 : 1);
//        if (diffX==0) {
//            extX= x2;
//            extY= y2 + mult*14;
//        }
//        else {
//            float slope= ((float)y2-y1) / ((float)x2-x1);
//            if (slope>=3 || slope<=-3) {
//                extX= x2;
//                extY= y2 + mult*14;
//            }
//            else if (slope<3 || slope>-3) {
//                extY= y2-6;
//                if (x2<x1) {
//                    extX= x2- 8;
//                }
//                else {
//                    extX= x2+ 2;
//                }
//            }
//
//        }
//        s= jg.drawLine(color, false, x1, y1, x2, y2);
//        sList.add(s);
//        s= jg.drawLine(color, false, 2, x2, y2, (int) barbX,(int) barbY);
//        sList.add(s);
//
//        s= jg.drawText(color, "9px", (int)extX,(int)extY, text);
//        sList.add(s);
//
//        return new Shapes(s);
//    }



}