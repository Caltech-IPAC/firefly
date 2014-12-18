package edu.caltech.ipac.visualize.draw;


import edu.caltech.ipac.visualize.plot.ImagePt;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;


public class LineShape {

    private static Color  DEF_STANDARD_COLOR= Color.red;
    private Color _color=  DEF_STANDARD_COLOR;
    private int _lineWidth= -1;
    private float _dash[] = null;
    public LineShape() { }

    public LineShape(float dash[]) {
        _dash = new float[dash.length];       
        for (int i=0; i< dash.length; i++) {
            _dash[i]=dash[i];
        }
    }

    public void setColor(Color c) { _color= c; }

    public void setLineWidth(int width) {
        _lineWidth= width;
    }

    public Rectangle draw(Graphics2D g2, ImagePt pts[] ) {
        return draw(g2, makeEntryAry(pts));
    }

    public Rectangle draw(Graphics2D g2, Ellipse2D ellipse) {
        AffineTransform savTran= g2.getTransform();
        AffineTransform trans  = g2.getTransform();
        BasicStroke stroke     = new BasicStroke();
        float w= (_lineWidth==-1) ? 2*stroke.getLineWidth() : _lineWidth;
        stroke                 = new BasicStroke( w / (float)trans.getScaleX() );
        g2.setPaint(_color);
        g2.setStroke( stroke);

        g2.draw(ellipse);
        g2.setTransform(savTran);
        return ellipse.getBounds();

    }

    public Rectangle draw(Graphics2D g2, Entry entry[] ) {
        AffineTransform savTran= g2.getTransform();
        AffineTransform trans  = g2.getTransform();
        GeneralPath gp         = new GeneralPath();
        BasicStroke stroke     = (_lineWidth<1) ? new BasicStroke() : new BasicStroke(_lineWidth);
        if (_dash != null) {
            stroke = new BasicStroke(2*stroke.getLineWidth() / (float)trans.getScaleX(),
                                    BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, _dash, 0.0f);

        } else {
            stroke = new BasicStroke( 2*stroke.getLineWidth() /
                                    (float)trans.getScaleX() );
        }
        ImagePt pt;
        g2.setPaint(_color);
        g2.setStroke( stroke);

        boolean penDown= false;
        for (Entry anEntry : entry) {
            pt = anEntry.getPt();
            if (pt != null) {
                penDown = penDown && anEntry.getDrawTo();
                if (penDown) {
                    gp.lineTo((float) pt.getX(), (float) pt.getY());
                } else {
                    gp.moveTo((float) pt.getX(), (float) pt.getY());
                    penDown = true;
                }
            } // end if
            else {
                penDown = false;
            } // end else
        } // end loop

        g2.draw(gp);
        g2.setTransform(savTran);
        return gp.getBounds();
    }


    public Rectangle computeRepair(AffineTransform trans, ImagePt pts[]) {
        return computeRepair(trans, makeEntryAry(pts));
    }
  
    public Rectangle computeRepair(AffineTransform trans, Entry entry[]) {
        GeneralPath gp     = new GeneralPath();
        boolean     penDown= false;
        Point2D     newpt  = new Point2D.Double(0,0);
        ImagePt     pt;
        for (Entry anEntry : entry) {
            pt = anEntry.getPt();
            if (pt != null) {
                newpt = trans.transform(
                        new Point2D.Double(pt.getX(), (float) pt.getY()),
                        newpt);
                penDown = penDown && anEntry.getDrawTo();
                if (penDown) {
                    gp.lineTo((float) newpt.getX(), (float) newpt.getY());
                } else {
                    gp.moveTo((float) newpt.getX(), (float) newpt.getY());
                    penDown = true;
                }
            } // end if
            else {
                penDown = false;
            } // end else
        }  // end loop
        return gp.getBounds();
    }


  private Entry[] makeEntryAry(ImagePt pts[]) {
        Entry entry[]= new Entry[pts.length];
        for(int i=0; (i<entry.length); i++) {
             entry[i]= new Entry(pts[i]);
        }
        return entry;
  }

//===================================================================
//------------------------- Public Inner classes --------------------
//===================================================================

   public static class Entry {
       private ImagePt _pt;
       private boolean _drawTo;
       public Entry(ImagePt pt) {
          this(pt, true);
       }
       public Entry(ImagePt pt, boolean drawTo) {
          _pt= pt;
          _drawTo= drawTo;
       }
       public ImagePt getPt()     { return _pt;}
       public boolean getDrawTo() { return _drawTo;}
   }

}
