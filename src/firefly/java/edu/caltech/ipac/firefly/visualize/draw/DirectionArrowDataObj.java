/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.draw;

import edu.caltech.ipac.firefly.visualize.ScreenPt;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.util.dd.Region;
import edu.caltech.ipac.util.dd.RegionLines;
import edu.caltech.ipac.util.dd.RegionText;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
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

    public double getScreenDist(WebPlot plot, ScreenPt pt) {
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

    public void draw(Graphics graphics, WebPlot p, DrawingDef def, boolean useStateColor, boolean onlyAddToPath) throws UnsupportedOperationException {
        throw new UnsupportedOperationException ("this type only supports drawing with WebPlot");
    }

    public void draw(Graphics graphics, DrawingDef def, boolean useStateColor, boolean onlyAddToPath) throws UnsupportedOperationException {
        drawDirectionArrow(graphics, def,useStateColor);
    }





    private void drawDirectionArrow(Graphics graphics, DrawingDef def, boolean useStateColor) {
        if (getShadow()!=null  && graphics instanceof AdvancedGraphics) {
            ((AdvancedGraphics)graphics).setShadowForNextDraw(getShadow());
        }

        if (getTranslation()!=null  && graphics instanceof AdvancedGraphics) {
            ((AdvancedGraphics)graphics).setTranslationForNextDraw(getTranslation());
        }


        ScreenPt pt1= (ScreenPt)_startPt;
        ScreenPt pt2= (ScreenPt)_endPt;
        String color=  calculateColor(def,useStateColor);

        VisUtil.NorthEastCoords ret= VisUtil.getArrowCoords(pt1.getIX(), pt1.getIY(), pt2.getIX(), pt2.getIY());

        List<ScreenPt> drawList= new ArrayList<ScreenPt>(4);
        drawList.add(new ScreenPt(ret.x1,ret.y1));
        drawList.add(new ScreenPt(ret.x2,ret.y2));
        drawList.add(new ScreenPt(ret.barbX2,ret.barbY2));

//        graphics.drawLine(color, ret.x1, ret.y1, ret.x2, ret.y2);
//        graphics.drawLine(color, 2, ret.barbX1, ret.barbY1, ret.barbX2, ret.barbY2);
        graphics.drawPath(color,2,drawList,false);

        int transX= 0;
        int transY= 0;
        if (getTranslation()!=null  && graphics instanceof AdvancedGraphics) {
            transX= getTranslation().getIX();
            transY= getTranslation().getIY();
        }

        graphics.drawText(color, "9px", ret.textX+transX, ret.textY+transY, _text);
        graphics.drawText(color, "9px", ret.textX+transX, ret.textY+transY, _text);
    }

    @Override
    public List<Region> toRegion(WebPlot plot, DrawingDef def) {
        ScreenPt pt1= (ScreenPt)_startPt;
        ScreenPt pt2= (ScreenPt)_endPt;
        String color=  calculateColor(def,false);
        VisUtil.NorthEastCoords ret= VisUtil.getArrowCoords(pt1.getIX(), pt1.getIY(), pt2.getIX(), pt2.getIY());
        RegionLines line1= new RegionLines(new WorldPt(ret.x1,ret.y1, CoordinateSys.SCREEN_PIXEL),
                                           new WorldPt(ret.x2,ret.y2, CoordinateSys.SCREEN_PIXEL) );

        RegionLines line2= new RegionLines(new WorldPt(ret.barbX1,ret.barbY1, CoordinateSys.SCREEN_PIXEL),
                                           new WorldPt(ret.barbX2,ret.barbY2, CoordinateSys.SCREEN_PIXEL) );
        RegionText text= new RegionText(new WorldPt(ret.textX,ret.textY, CoordinateSys.SCREEN_PIXEL));

        line1.getOptions().setColor(color);
        line2.getOptions().setColor(color);
        text.getOptions().setColor(color);
        text.getOptions().setText(_text);

        return Arrays.asList(line1,line2,text);
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