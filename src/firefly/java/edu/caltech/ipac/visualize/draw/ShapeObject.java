/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.draw;

/**
 * The ShapeObject represents a shape, displayed on a plot
 */
public interface ShapeObject {

    public LineShape   getLineShape();

    public StringShape getStringShape();
}
