package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.CssColor;

/**
 * User: roby
 * Date: Dec 15, 2008
 * Time: 1:15:52 PM
 */
public abstract class CanvasShape extends Shape<Context2d> {

    private final CssColor _color;
    private final boolean _front;
    private final int  _lineWidth;


    public CanvasShape(CssColor color, boolean front, int lineWidth) {
        _color= color;
        _front= front;
        _lineWidth= lineWidth;
    }

    public abstract void draw(Context2d surfaceWidget);

    public CssColor getColor() { return _color; }
    public boolean isFront() { return _front; }
    public int getLineWidth() { return _lineWidth; }
}
