package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.visualize.ScreenPt;

import java.util.List;

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

    public void drawPath(String color,
                         int lineWidth,
                         List<ScreenPt> pts,
                         boolean close);

    public void drawPath(String color,
                         int lineWidth,
                         List<PathType> pts);


    public void beginPath(String color, int lineWidth);
    public void pathMoveTo(int x,int y);
    public void pathLineTo(int x,int y);
    public void drawPath();



    public void clear();
    public void paint();
    public void setDrawingAreaSize(int width, int height);
    public Widget getWidget();

    public static class PathType {
        private final boolean draw;
        private final int x;
        private final int y;

        public PathType(boolean draw, int x, int y) {
            this.draw = draw;
            this.x = x;
            this.y = y;
        }

        public int getX() { return x; }

        public int getY() { return y; }

        public boolean isDraw() { return draw; }
    }

}