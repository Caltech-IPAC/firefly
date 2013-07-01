package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.user.client.ui.Widget;

/**
 * @author Trey Roby
 * @version $Id: Graphics.java,v 1.7 2012/02/10 19:51:31 roby Exp $
 */
public interface Graphics {

    public static final int DEF_WIDTH= 2;

    public void drawLine(String color,
                         int sx,
                         int sy,
                         int ex,
                         int ey);

    public void drawLine(String color,
                         int lineWidth,
                         int sx,
                         int sy,
                         int ex,
                         int ey);

    public void drawRec(String color,
                        int lineWidth,
                        int x,
                        int y,
                        int width,
                        int height);


    public void fillRec(String color,
                        int x,
                        int y,
                        int width,
                        int height);



    public void drawText(String color,
                         String size,
                         int x,
                         int y,
                         String text);

    public void drawText(String color,
                         String fontFamily,
                         String size,
                         String fontWeight,
                         String fontStyle,
                         int x,
                         int y,
                         String text);

    public void drawCircle(String color,
                           int lineWidth,
                           int x,
                           int y,
                           int radius);

    public void clear();
    public void paint();
    public void setDrawingAreaSize(int width, int height);
    public Widget getWidget();


}