package edu.caltech.ipac.visualize.draw;


import edu.caltech.ipac.util.Assert;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.border.TitledBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.IndexColorModel;

public class ColorDisplay extends JComponent implements Icon {

    private IndexColorModel _model       = null;
    private Color           _colorBand   = null;
    private boolean         _heightLocked= false;

    public ColorDisplay() { };

    public ColorDisplay(String title) {
       if (title != null)  setBorder( new TitledBorder(title));
    }

    public void setColor(IndexColorModel model) {
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

    public void clearColor() {
        _model= null;
        _colorBand= null;
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


    public void setHeightLocked(boolean heightLocked) {
       _heightLocked= heightLocked;
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
           int        clength= _colorBand==null ?  _model.getMapSize() - 1 :
                                                   255;
          Dimension  dim    = getSize();
          int        width  = getIconWidth()  - (insets.left + insets.right);
          int        height = getIconHeight() - (insets.top + insets.bottom);
          float      div    = (float)width / (float)clength;

          g2.setStroke( new BasicStroke(1) );
          //System.out.println("width=" + width + "    div="+ div
          //                                        + "    clength="+ clength);
          for (int i=0; (i<width); i++) {
             idx= (int)(i/div);
             //System.out.println("width=" + width + "    idx="+ idx
             //                                    + "    i="+ i
             //                                    + "    div="+ div
             //                                    + "    clength="+ clength);
//             g2.setPaint( new Color(_model.getRed(idx),
//                                    _model.getGreen(idx),
//                                    _model.getBlue(idx) ) );
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
