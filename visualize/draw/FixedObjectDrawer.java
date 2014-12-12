package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.ProjectionException;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;

/**
 * @author Trey Roby
 * @version $Id: FixedObjectDrawer.java,v 1.11 2012/10/02 23:03:34 roby Exp $
 * @see FixedObjectGroup
 * @see FixedObject
 */
public class FixedObjectDrawer {

    private SkyShape _skyShape;
    private SkyShape _lastSkyShape;
    private StringShape _stringShape = new StringShape(4, StringShape.EAST);
    private Rectangle _lastBounds;

    private Color _highLightColor = Color.blue;
    private Color _selectedColor = Color.orange;
    private Color _standardColor = Color.red;
    private ArrayList<ImagePt> _imageCoordsAry =
            new ArrayList<ImagePt>(20);
    private boolean _showNameOnLastDraw;

    private boolean _showFullName = true;
    private FixedObject _fixedObj;


    FixedObjectDrawer(FixedObject fixedObj) {
        _fixedObj = fixedObj;
        _skyShape = SkyShapeFactory.getInstance().getSkyShape("x");
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
                _lastBounds = _skyShape.drawStandard(g2, c, pt.getX(), pt.getY());
            }
            boolean showName = _fixedObj.getShowName();
            if (showName) {
                _stringShape.setColor(c);
                if (_showFullName)
                    _stringShape.draw(g2, pt, 0, _fixedObj.getFullTargetName());
                else
                    _stringShape.draw(g2, pt, 0, _fixedObj.getTargetName());
            }
            _showNameOnLastDraw = showName;
        }
    }

    public Rectangle computeRepair(AffineTransform trans, int idx) {
        ImagePt ipt = _imageCoordsAry.get(idx);
        Rectangle shapeRec = null;
        if (ipt != null) {
            ImageWorkSpacePt pt = new ImageWorkSpacePt(ipt.getX(), ipt.getY());
            shapeRec = _skyShape.computeRepair(trans, pt.getX(), pt.getY());
            if (_lastSkyShape != null && _lastSkyShape != _skyShape) {
                Rectangle lastShapeRec = _lastSkyShape.computeRepair(trans,
                        pt.getX(), pt.getY());
                SwingUtilities.computeUnion(lastShapeRec.x, lastShapeRec.y,
                        lastShapeRec.width, lastShapeRec.height,
                        shapeRec);

            }
            if (_showNameOnLastDraw || _fixedObj.getShowName()) {
                Rectangle strRec = _stringShape.computeRepair(trans, pt, 0,
                        _fixedObj.getFullTargetName());
                if (strRec != null) {
                    SwingUtilities.computeUnion(strRec.x, strRec.y,
                            strRec.width, strRec.height,
                            shapeRec);
                }
            }
            _lastSkyShape = _skyShape;
        }
        return shapeRec;
    }

    public void computeTransform(int idx, Plot p) {
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
//            System.out.println("FixedObject.computeTransform: " + e);
        }
        if (_imageCoordsAry.size() <= idx) {
            for (int i = _imageCoordsAry.size(); (i <= idx); i++) {
                _imageCoordsAry.add(null);
            }
        }
        _imageCoordsAry.set(idx, pt);
    }

    public ImagePt getImagePt(int idx) {
        return _imageCoordsAry.get(idx);
    }

    public void setSkyShape(SkyShape s) {
        _skyShape = s;
    }

    public void setStringShape(StringShape s) {
        _stringShape = s;
    }

    public StringShape getStringShape() { return _stringShape; }

    public void setHighLightColor(Color c) {
        _highLightColor = c;
    }

    public void setSelectedColor(Color c) {
        _selectedColor = c;
    }

    public void setStandardColor(Color c) {
        _standardColor = c;
    }

    public void setShowFullName(boolean s) {
        _showFullName = s;
    }

    public Rectangle getBounds() {
        return _lastBounds;
    }

    public SkyShape getSkyShape() {
        return _skyShape;
    }

    public Color getHighLightColor() {
        return _highLightColor;
    }

    public Color getSelectedColor() {
        return _selectedColor;
    }

    public Color getStandardColor() {
        return _standardColor;
    }

    public boolean getShowFullName() {
        return _showFullName;
    }
}
/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
