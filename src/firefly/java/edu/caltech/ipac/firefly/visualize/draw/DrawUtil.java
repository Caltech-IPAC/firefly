/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.draw;

/**
 * User: roby
 * Date: Nov 11, 2009
 * Time: 1:28:05 PM
 */


import com.google.gwt.user.client.ui.HTML;
import edu.caltech.ipac.firefly.ui.GwtUtil;

/**
 * @author Trey Roby
 */
public class DrawUtil {

    public static void drawHandledLine(Graphics g, String color, int sx, int sy, int ex, int ey) {
        drawHandledLine(g,color,sx,sy,ex,ey,false);
    }

    public static void drawHandledLine(Graphics g, String color, int sx, int sy, int ex, int ey, boolean onlyAddToPath) {
        float slope= Float.NaN;

        if (ex-sx!=0) slope= (ey-sy) / (ex-sx);
        int x, y;
        if (!onlyAddToPath) g.beginPath(color,3);

        if (Float.isNaN(slope)) {// vertical
            y= (sy < ey) ? sy+5 : sy-5;
            g.pathMoveTo(sx,sy);
            g.pathLineTo(sx,y);
//            g.drawLine(color, 3, sx, sy, sx, y);

            y= (sy < ey) ? ey-5 : ey+5;
            g.pathMoveTo(ex,ey);
            g.pathLineTo(ex,y);
//            g.drawLine(color, 3, ex, ey, ex, y);

        }
        else if (Math.abs(sx-ex) > Math.abs(sy-ey)) {  // horizontal
            x= (sx < ex) ? sx+5 : sx-5;
            y= (int)(slope * (x - sx) + sy);

//            g.drawLine(color, 3, sx, sy, x, y);
            g.pathMoveTo(sx,sy);
            g.pathLineTo(x,y);

            x= (sx < ex) ? ex-5 : ex+5;
            y= (int)(slope * (x - ex) + ey);
//            g.drawLine(color, 3, ex, ey, x, y);
            g.pathMoveTo(ex,ey);
            g.pathLineTo(x,y);
        }
        else {

            y= (sy < ey) ? sy+5 : sy-5;
            x= (int)((y-sy)/slope + sx);
//            g.drawLine(color, 3, sx, sy, x, y);
            g.pathMoveTo(sx,sy);
            g.pathLineTo(x,y);


            y= (sy < ey) ? ey-5 : ey+5;
            x= (int)((y-ey)/slope + ex);
//            g.drawLine(color, 3, ex, ey, x, y);
            g.pathMoveTo(ex,ey);
            g.pathLineTo(x,y);

        }
        if (!onlyAddToPath) g.drawPath();
    }

    public static void drawInnerRecWithHandles(Graphics g, String color, int lineWidth, int inX1, int inY1, int inX2, int inY2) {

        int x0= Math.min(inX1,inX2)+lineWidth;
        int y0= Math.min(inY1,inY2)+lineWidth;
        int width= Math.abs(inX1-inX2)-(2*lineWidth);
        int height= Math.abs(inY1-inY2)-(2*lineWidth);
        g.drawRec(color, lineWidth, x0,y0,width,height);
        int x2= x0+width;
        int y2= y0+height;

        int x1= x0+width;
        int y1= y0;

        int x3= x0;
        int y3= y0+height;

        g.beginPath(color,3);

        DrawUtil.drawHandledLine(g, color, x0,y0,x1,y1,true);
        DrawUtil.drawHandledLine(g, color, x1,y1,x2,y2, true);
        DrawUtil.drawHandledLine(g, color, x2,y2,x3,y3,true);
        DrawUtil.drawHandledLine(g, color, x3,y3,x0,y0,true);

        g.drawPath();
    }

    public static HTML makeDrawLabel(String color,
                         String fontFamily,
                         String size,
                         String fontWeight,
                         String fontStyle,
                         String text) {
        HTML label= new HTML(text);
        GwtUtil.setStyles(label,
                          "color", color,
                          "fontFamily", fontFamily,
                          "fontSize", size,
                          "fontWeight", fontWeight,
                          "fontStyle", fontStyle,
                          "backgroundColor", "white",
                          "MozBorderRadius", "5px",
                          "borderRadius", "5px",
                          "webkitBorderRadius", "5px");
        return label;

    }
}

