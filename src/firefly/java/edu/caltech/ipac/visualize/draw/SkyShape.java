/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.draw;


import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.Serializable;


public class SkyShape implements Icon, Serializable {
    private final Shape  _standard;
    private final BasicStroke _templateStroke;

    private Color  DEF_STANDARD_COLOR= Color.red;
    private Color  DEF_BRIGHT_COLOR  = Color.blue;
    private Color  DEF_SELECTED_COLOR= Color.orange;




    static public final int CENTER= 0;
    static public final int NORTH = 1;
    static public final int SOUTH = 2;
    static public final int EAST  = 3;
    static public final int WEST  = 4;
    static public final int NE    = 5;
    static public final int SE    = 6;
    static public final int NW    = 7;
    static public final int SW    = 8;

    public SkyShape(Shape standard) {
       this(standard,new BasicStroke(1));
    }
    public SkyShape(Shape standard, BasicStroke templateStroke) {
        _standard= standard;
        _templateStroke= templateStroke;
    }

    public Rectangle draw(Graphics2D g2, double x, double y) {
        AffineTransform savTran= g2.getTransform();
        AffineTransform trans= g2.getTransform();
        Point2D pt= trans.transform( new Point2D.Double(x,y), null);
        trans.setToIdentity();
         //----
        Rectangle rec= _standard.getBounds();
        double moveX= rec.getWidth() /2;
        double moveY= rec.getHeight() /2;
        pt= new Point2D.Double( pt.getX() - moveX, pt.getY() - moveY);
         //----
        trans.translate(pt.getX(), pt.getY());
         //----
        float strokSize= new BasicStroke().getLineWidth() / (float)trans.getScaleX();
        BasicStroke stroke= new BasicStroke(
                           strokSize * _templateStroke.getLineWidth(),
                           _templateStroke.getEndCap(),
                           _templateStroke.getLineJoin());

        g2.setStroke( stroke);
         //----
        g2.setTransform(trans);
        g2.draw(_standard);
        g2.setTransform(savTran);
        return _standard.getBounds();
    }

    public Rectangle drawBright(Graphics2D g2, Color c, double x, double y) {
        g2.setPaint(c);
        return draw(g2,x,y);
    }

    public Rectangle drawBright(Graphics2D g2, double x, double y) {
        return drawBright(g2,DEF_BRIGHT_COLOR,x,y);
    }

    public Rectangle drawSelected(Graphics2D g2, Color c, double x, double y) {
        g2.setPaint(c);
        return draw(g2,x,y);
    }

    public Rectangle drawSelected(Graphics2D g2, double x, double y) {
        return drawSelected(g2,DEF_SELECTED_COLOR,x,y);
    }

    public Rectangle drawStandard(Graphics2D g2, Color c, double x, double y) {
        g2.setPaint(c);
        return draw(g2,x,y);
    }

    public Rectangle drawStandard(Graphics2D g2, double x, double y) {
        return drawStandard(g2,DEF_STANDARD_COLOR,x,y);
    }

/*
    public Rectangle drawString(Graphics2D g2,  
                                double     x, 
                                double     y,
                                String     str,     
                                int        pos) {
        if (str != null && str.length() > 0) {
           AffineTransform savTran= g2.getTransform();
           AffineTransform trans= g2.getTransform();
           Point2D pt= trans.transform( new Point2D.Double(x,y), null);
           trans.setToIdentity();
           g2.setTransform(trans); 
           Font f= new Font("Serif", Font.PLAIN, 11);
           TextLayout textLayout= 
                     new TextLayout(str,f,g2.getFontRenderContext() );
           textLayout.draw(g2, (float)pt.getX(), (float)pt.getY() );
           g2.setTransform(savTran);
        }
        return null;
    }
*/

/*
    public Rectangle repairString(AffineTransform trans, 
                                  double          x, 
                                  double          y,
                                  String          str,     
                                  int             pos) {
        Rectangle retval= null;
        if (str != null && str.length() > 0) {
           Point2D pt= trans.transform( new Point2D.Double(x,y), null);
           Font f= new Font("Serif", Font.PLAIN, 11);
           FontRenderContext fContext= new FontRenderContext(
                                      new AffineTransform(),true,true);
           TextLayout textLayout= new TextLayout(str,f,fContext );
           Rectangle2D rec= textLayout.getBounds();
           retval= new Rectangle( (int)pt.getX()-2,
                                  (int)(pt.getY() -rec.getHeight() - 2), 
                                  (int)rec.getWidth()+15, 
                                  (int)rec.getHeight()+15 );
        }
        return retval;
    }
*/

    public Rectangle computeRepair(AffineTransform trans,
                                   double          x,
                                   double          y) {
        Point2D newpt= trans.transform( new Point2D.Double(x,y), null);
        Rectangle rec= _standard.getBounds();
        rec.width++;
        rec.height++;
        rec.x= (int)newpt.getX() - rec.width / 2;
        rec.y= (int)newpt.getY() - rec.height / 2;
        rec.width++;
        rec.height++;
        return rec;
    }

//======================================================================
//--------------------- Methods from Icon Interface --------------------
//======================================================================

    public int getIconHeight() {
        return _standard.getBounds().height+1;
    }

    public int getIconWidth() {
        return _standard.getBounds().width+1;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2= (Graphics2D)g;
        AffineTransform savTran= g2.getTransform();
        g2.scale(1.3,1.3);
        g2.translate(x,y);
        g2.setPaint(Color.black);
        g2.fill(_standard);
        g2.setPaint(Color.red);
        g2.draw(_standard);
        g2.setTransform(savTran);
    }
}




