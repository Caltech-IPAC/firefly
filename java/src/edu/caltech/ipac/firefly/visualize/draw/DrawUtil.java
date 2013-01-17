package edu.caltech.ipac.firefly.visualize.draw;

import java.util.ArrayList;
import java.util.List;
/**
 * User: roby
 * Date: Nov 11, 2009
 * Time: 1:28:05 PM
 */


/**
 * @author Trey Roby
 */
public class DrawUtil {


    public static Shapes drawHandledLine(Graphics jg, String color, int sx, int sy, int ex, int ey, boolean front) {
        List<Shape> sList= new ArrayList<Shape>(10);
        Shape s;
        float slope= Float.NaN;

        if (ex-sx!=0) slope= (ey-sy) / (ex-sx);
        int x, y;

        if (Float.isNaN(slope)) {
            y= (sy < ey) ? sy+5 : sy-5;
            s= jg.drawLine(color, front, 3, sx, sy, sx, y);
            sList.add(s);

            y= (sy < ey) ? ey-5 : ey+5;
            s= jg.drawLine(color, front, 3, ex, ey, ex, y);
            sList.add(s);

        }
        else if (Math.abs(sx-ex) > Math.abs(sy-ey)) {  // horizontal
            x= (sx < ex) ? sx+5 : sx-5;
            y= (int)(slope * (x - sx) + sy);

            s= jg.drawLine(color, front, 3, sx, sy, x, y);
            sList.add(s);

            x= (sx < ex) ? ex-5 : ex+5;
            y= (int)(slope * (x - ex) + ey);
            s= jg.drawLine(color, front, 3, ex, ey, x, y);
            sList.add(s);
        }
        else {  // vertial

            y= (sy < ey) ? sy+5 : sy-5;
            x= (int)((y-sy)/slope + sx);
            s= jg.drawLine(color, front, 3, sx, sy, x, y);
            sList.add(s);


            y= (sy < ey) ? ey-5 : ey+5;
            x= (int)((y-ey)/slope + ex);
            s= jg.drawLine(color, front, 3, ex, ey, x, y);
            sList.add(s);



        }
        return new Shapes(sList);
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
