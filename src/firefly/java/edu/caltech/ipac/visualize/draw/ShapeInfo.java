/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.draw;

import java.awt.Shape;
import java.awt.Color;

/**
 * Class to hold information about a shape.  It currently holds shape and 
 * color.
 *
 * @author Trey Roby
 * @version $Id: ShapeInfo.java,v 1.2 2005/12/08 22:31:32 tatianag Exp $
 *
 */
public class ShapeInfo {
    private Shape _shape= null;
    private Color _color= null;

    public ShapeInfo(Shape shape, Color color) {
       _shape= shape;
       _color= color;
    }

    public Shape getShape() { return _shape; }
    public Color getColor() { return _color; }
    public void  setColor(Color c) { _color =c; }
}



