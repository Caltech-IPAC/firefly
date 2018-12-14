/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.draw;


import edu.caltech.ipac.util.Assert;

import javax.swing.*;
import java.awt.*;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;

public class ColorDisplay extends JComponent implements Icon {

    private ColorModel _model       = null;
    private Color           _colorBand   = null;

    public ColorDisplay() { };

    public void setColor(ColorModel model) {
       _model= model;
       _colorBand= null;
       repaint();
    }

    public void setColor(Color color) {
        Assert.argTst((color==Color.RED ||
                       color==Color.GREEN ||
                       color==Color.BLUE),
                      "Color must be Color.RED, Color.BLUE, or Color.GREEN");
        _model= null;
        _colorBand= color;
        repaint();
    }

    public void paintComponent(Graphics g) {
       if (_model != null || _colorBand != null) {
           paintIcon(this,g,0,0);
       } // end if
       else {
          super.paintComponent(g);
       } // end else
    }



    public Dimension getMaximumSize() {
         Dimension retval= super.getMaximumSize();
         Dimension p= super.getPreferredSize();
         retval.height=  p.height;
         return retval;
    }


    public int getIconWidth() {
       return getSize().width;
    }
    public int getIconHeight() {
       return getSize().height;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
       if (_model != null || _colorBand != null) {
          int        idx;
          Graphics2D g2     = (Graphics2D)g;
          Insets     insets = getInsets();
           int        clength= _colorBand==null  && (_model instanceof IndexColorModel)?
                                  ((IndexColorModel)_model).getMapSize() - 1 : 255;
          Dimension  dim    = getSize();
          int        width  = getIconWidth()  - (insets.left + insets.right);
          int        height = getIconHeight() - (insets.top + insets.bottom);
          float      div    = (float)width / (float)clength;

          g2.setStroke( new BasicStroke(1) );
          for (int i=0; (i<width); i++) {
             idx= (int)(i/div);
             g2.setPaint( determinePaint(idx));
             g2.drawLine(x+i+insets.left, y+insets.top,
                         x+i+insets.left, y+insets.top+height);
          }
       } // end if
    }

    private Color determinePaint(int idx) {
        Color retval=null;
        if (_colorBand!=null) {
            if (_colorBand==Color.RED) {
                retval= new Color(idx,0,0);
            }
            else if (_colorBand==Color.GREEN) {
                retval= new Color(0,idx,0);
            }
            else if (_colorBand==Color.BLUE) {
                retval= new Color(0,0,idx);
            }
        }
        else {
            Assert.tst(_model!=null);
            retval= new Color(_model.getRed(idx), _model.getGreen(idx),
                               _model.getBlue(idx) );
        }
        return retval;
    }

}
