/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;


public class StringShape {


    static public final int CENTER= 10000;
    static public final int NORTH = 10001;
    static public final int SOUTH = 10002;
    static public final int EAST  = 10003;
    static public final int WEST  = 10004;
    static public final int NE    = 10005;
    static public final int SE    = 10006;
    static public final int NW    = 10007;
    static public final int SW    = 10008;

    private static Color  DEF_STANDARD_COLOR= Color.blue;

    private Color _color=  DEF_STANDARD_COLOR;
    private int   _offsetDist;
    private int   _offsetDir;
    private Font  _font;
    private boolean _backDraw= true;
    private boolean useRegionCalc = false;

   public StringShape(int offsetDist,
                      int offsetDir) {
      this(offsetDist, offsetDir, new Font("SansSerif", Font.PLAIN, 11));
   }

   public StringShape(int  offsetDist,
                      int  offsetDir,
                      Font font) {


      _offsetDist= offsetDist;
      setOffsetDirection(offsetDir);
      _font     =  font;
   }

   public void  setDrawWithBackground(boolean backDraw) { _backDraw= backDraw; }

   public void setOffsetDirection(int offsetDir) {
      Assert.tst( offsetDir == CENTER ||
                  offsetDir == NORTH  ||
                  offsetDir == SOUTH  ||
                  offsetDir == EAST   ||
                  offsetDir == WEST   ||
                  offsetDir == NE     ||
                  offsetDir == SE     ||
                  offsetDir == NW     ||
                  offsetDir == SW , 
                  "bad offsetDir= " + offsetDir   );
      _offsetDir=  offsetDir;
   }

   public void setOffsetDistance(int offsetDist) { _offsetDist= offsetDist; }
   public void  setColor(Color c)                 { _color= c; }
   public Color getColor()                        { return _color; }
   public Font  getFont()                         { return _font; }
   public void  setFont(Font f)                   { _font = f; }

    public void draw(Graphics2D   g2,
                    ImageWorkSpacePt      ipt,
                    float        rotation,
                    String       str) {
        String strAry[] = new String[1];
        strAry[0] = str;
        draw(g2,ipt,rotation, strAry);
    }

    public void draw(Graphics2D         g2,
                    ImageWorkSpacePt    ipt,
                    float               rotation,
                    String              str[]) {
        if (str != null) {
            int             yoffset=0;
            AffineTransform savTran= g2.getTransform();
            AffineTransform trans  = g2.getTransform();
            TextLayout      textLayout;
            Rectangle2D     rec;
            Point2D         pt= trans.transform( new Point2D.Double(
                            ipt.getX(),ipt.getY()), null);
            //g2.setPaint(_color);
            trans.setToIdentity();
            g2.setTransform(trans);

            if (rotation > Math.PI / 2 && rotation <= Math.PI * 3 / 2)
                g2.rotate(rotation + Math.PI,pt.getX(),pt.getY());
            else
                g2.rotate(rotation,pt.getX(),pt.getY());

            Dimension dim= computeSize(trans, str);
            pt= addOffset(pt,dim);
            //------------ ??? need to do something with rotation
            for (int i=str.length-1; (i>=0); i-- ) {
                if (str[i].length() > 0) {
                    textLayout=
                    new TextLayout(str[i],_font,g2.getFontRenderContext());
                    rec= textLayout.getBounds();

                    if (useRegionCalc) {
                        pt= new Point2D.Double(pt.getX()-rec.getX(),pt.getY()+rec.getHeight());
                    }

                    if (_backDraw) {
                        g2.setPaint(_color.darker().darker());
                        g2.fillRect( (int)pt.getX()-_offsetDist, (int)(pt.getY()-rec.getHeight()-_offsetDist),
                                     (int)rec.getWidth()+3*_offsetDist, (int)rec.getHeight()+3*_offsetDist );
                        /*g2.setPaint(_color.darker());
                        g2.drawRect( (int)pt.getX()-_offsetDist, (int)(pt.getY()-rec.getHeight()-_offsetDist),
                                     (int)rec.getWidth()+3*_offsetDist, (int)rec.getHeight()+3*_offsetDist );
                        */
                    }
                    g2.setPaint(_color);
                    textLayout.draw(g2, (float)pt.getX(),
                         (float)pt.getY()-yoffset);
                    yoffset+= rec.getHeight()+3;
                }
            }
            g2.setTransform(savTran);
        }
    }

    public Rectangle computeRepair(AffineTransform trans,
                                   ImageWorkSpacePt         ipt,
                                   float           rotation,
                                   String          str[] ) {
        Rectangle retval= null;
        if (str != null) {
           int        pad= 4;
           Point2D    pt= trans.transform( new Point2D.Double(
                                                ipt.getX(),ipt.getY()), null);
           //------------ ??? need to do something with rotation

           Dimension dim= computeSize(trans, str);
           pt= addOffset(pt,dim);

           retval= new Rectangle( (int)pt.getX(),
                                  (int)pt.getY() - dim.height,
                                  dim.width, dim.height );

           retval.x-= pad;
           retval.y-= pad;
           retval.width += pad*4;
           retval.height+= pad*4;
        }
        return retval;
    }


    private Dimension computeSize(AffineTransform trans, 
                                  String          str[] ) {
           TextLayout        textLayout;
           Rectangle2D       rec;
           FontRenderContext fContext= new FontRenderContext(
                                                    trans,true,true);
           Dimension retDim= new Dimension(0,0);
           for (int i=0; (i<str.length); i++ ) {
              if (str[i].length() > 0) {
                   textLayout= new TextLayout(str[i],_font,fContext );
                   rec= textLayout.getBounds();
                   retDim.height+= (int)rec.getHeight()+1;
                   if (rec.getWidth() > retDim.width) 
                                 retDim.width= (int)rec.getWidth();
              }
           }
           return retDim;
    }

    public void setUseRegionCalc(boolean useRegionCalc) {
        this.useRegionCalc = useRegionCalc;
    }

    private Point2D addOffset(Point2D pt, Dimension dim) {
        if (useRegionCalc) return pt;
        Point2D retval= null;
        int yoffset= 0;
        int xoffset= 0;
        xoffset = 2*_offsetDist;
        yoffset = -2*_offsetDist;

        if (_offsetDir==EAST || _offsetDir==SE || _offsetDir==NE) {
                xoffset= -1 * (dim.width + 2*_offsetDist);
        }
        retval= new Point2D.Double( pt.getX() + xoffset, pt.getY() + yoffset);
        
        return retval;
    }
}
