/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
import edu.caltech.ipac.visualize.plot.ProjectionException;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

/**
 * @author Trey Roby
 * @version $Id: FixedObjectDrawer.java,v 1.11 2012/10/02 23:03:34 roby Exp $
 * @see FixedObjectGroup
 * @see FixedObject
 */
public class FixedObjectDrawer {

    private SkyShape _skyShape;
    private final StringShape _stringShape = new StringShape(4, StringShape.EAST);
    private Color _highLightColor = Color.blue;
    private Color _selectedColor = Color.orange;
    private Color _standardColor = Color.red;
    private final ArrayList<ImagePt> _imageCoordsAry = new ArrayList<ImagePt>(20);
    private final FixedObject _fixedObj;


    FixedObjectDrawer(FixedObject fixedObj) {
        _fixedObj = fixedObj;
        for (int i = 0; (i < 20); i++) _imageCoordsAry.add(null);
    }

    public void drawOnPlot(int idx, Graphics2D g2) {
        //System.out.println("FixedObject: drawOnPlot");
        ImagePt ipt = _imageCoordsAry.get(idx);
        if (ipt != null) {
            ImageWorkSpacePt pt = new ImageWorkSpacePt(ipt.getX(), ipt.getY());
            Assert.tst(pt);
            Color c;
            if (_fixedObj.isSelected()) c = _selectedColor;
            else if (_fixedObj.isHiLighted()) c = _highLightColor;
            else c = _standardColor;

            if (_fixedObj.getShowPoint()) {
                _skyShape.drawStandard(g2, c, pt.getX(), pt.getY());
            }
            boolean showName = _fixedObj.getShowName();
            if (showName) {
                _stringShape.setColor(c);
                _stringShape.draw(g2, pt, 0, _fixedObj.getFullTargetName());
            }
        }
    }


    public void computeTransform(int idx, ImagePlot p) {
        ImagePt pt = null;
        try {
            if (_fixedObj.isWorldCoordSys()) {
                ImageWorkSpacePt ipt = p.getImageCoords(_fixedObj.getPosition());
                pt = new ImagePt(ipt.getX(), ipt.getY());
            } else {
                pt = _fixedObj.getImagePt();
            }
        } catch (ProjectionException e) {
            // just ignore point
        }
        if (_imageCoordsAry.size() <= idx) {
            for (int i = _imageCoordsAry.size(); (i <= idx); i++) {
                _imageCoordsAry.add(null);
            }
        }
        _imageCoordsAry.set(idx, pt);
    }

    public StringShape getStringShape() { return _stringShape; }
    public void setHighLightColor(Color c) { _highLightColor = c; }
    public void setSelectedColor(Color c) { _selectedColor = c; }
    public void setStandardColor(Color c) { _standardColor = c; }
    public void setSkyShape(SkyShape s) { _skyShape = s; }
}
