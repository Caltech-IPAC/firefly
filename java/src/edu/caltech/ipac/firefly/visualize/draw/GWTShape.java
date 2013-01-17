package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.widgetideas.graphics.client.Color;
import com.google.gwt.widgetideas.graphics.client.GWTCanvas;

/**
 * User: roby
 * Date: Dec 15, 2008
 * Time: 1:15:52 PM
 */
public abstract class GWTShape extends Shape<GWTCanvas> {

    private final Color _color;
    private final boolean _front;
    private final int  _lineWidth;


    public GWTShape(Color color,
                    boolean front,
                    int lineWidth) {
        _color= color;
        _front= front;
        _lineWidth= lineWidth;
    }

    public abstract void draw(GWTCanvas surfaceWidget);

    public Color getColor() { return _color; }
    public boolean isFront() { return _front; }
    public int getLineWidth() { return _lineWidth; }
}
