package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.user.client.ui.Widget;

/**
 * @author Trey Roby
 * @version $Id: Graphics.java,v 1.7 2012/02/10 19:51:31 roby Exp $
 */
public interface Graphics {

    public static final int DEF_WIDTH= 2;



    public Shape drawLine(String color,
                         int sx,
                         int sy,
                         int ex,
                         int ey);

    public Shape drawLine(String color,
                         int lineWidth,
                         int sx,
                         int sy,
                         int ex,
                         int ey);

    public Shape drawRec(String color,
                        int lineWidth,
                        int x,
                        int y,
                        int width,
                        int height);


    public Shape fillRec(String color,
                         int x,
                         int y,
                         int width,
                         int height);



    public Shape drawText(String color,
                          String size,
                          int x,
                          int y,
                          String text);

    public Shape drawText(String color,
                          String fontFamily,
                          String size,
                          String fontWeight,
                          String fontStyle,
                          int x,
                          int y,
                          String text);

    public Shape drawCircle(String color,
                            int lineWidth,
                            int x,
                            int y,
                            int radius);

    public void deleteShapes(Shapes shapes);
    public void clear();
    public void paint();
    public void setDrawingAreaSize(int width, int height);
    public boolean getSupportsPartialDraws();
    public Widget getWidget();


}