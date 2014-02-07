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
        float slope= Float.NaN;

        if (ex-sx!=0) slope= (ey-sy) / (ex-sx);
        int x, y;

        if (Float.isNaN(slope)) {
            y= (sy < ey) ? sy+5 : sy-5;
            g.drawLine(color, 3, sx, sy, sx, y);

            y= (sy < ey) ? ey-5 : ey+5;
            g.drawLine(color, 3, ex, ey, ex, y);

        }
        else if (Math.abs(sx-ex) > Math.abs(sy-ey)) {  // horizontal
            x= (sx < ex) ? sx+5 : sx-5;
            y= (int)(slope * (x - sx) + sy);

            g.drawLine(color, 3, sx, sy, x, y);

            x= (sx < ex) ? ex-5 : ex+5;
            y= (int)(slope * (x - ex) + ey);
            g.drawLine(color, 3, ex, ey, x, y);
        }
        else {  // vertical

            y= (sy < ey) ? sy+5 : sy-5;
            x= (int)((y-sy)/slope + sx);
            g.drawLine(color, 3, sx, sy, x, y);


            y= (sy < ey) ? ey-5 : ey+5;
            x= (int)((y-ey)/slope + ex);
            g.drawLine(color, 3, ex, ey, x, y);

        }
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

        DrawUtil.drawHandledLine(g, color, x0,y0,x1,y1);
        DrawUtil.drawHandledLine(g, color, x1,y1,x2,y2);
        DrawUtil.drawHandledLine(g, color, x2,y2,x3,y3);
        DrawUtil.drawHandledLine(g, color, x3,y3,x0,y0);
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
