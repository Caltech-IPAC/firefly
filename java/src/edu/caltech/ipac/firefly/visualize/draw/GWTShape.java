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
    private final int  _lineWidth;


    public GWTShape(Color color,
                    int lineWidth) {
        _color= color;
        _lineWidth= lineWidth;
    }

    public abstract void draw(GWTCanvas surfaceWidget);

    public Color getColor() { return _color; }
    public int getLineWidth() { return _lineWidth; }
}
