package edu.caltech.ipac.visualize.draw;


import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.visualize.plot.ColorTable;
import edu.caltech.ipac.visualize.plot.ImagePlot;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.IndexColorModel;

public class HistogramDisplay extends JComponent implements Icon {


    private static final int UPPER = 45;
    private static final int LOWER = 46;

    private int             _histogram[] = null;
    private IndexColorModel _model       = null;
    private int             _lineDataSize[];
    private int             _orginalHistogramIdx[];
    private int             _upperBounds= -1;
    private int             _lowerBounds= -1;
    private int             _upperBounds2= -1;
    private int             _lowerBounds2= -1;
    private boolean         _do2nd= false;
    private boolean         _boundsEnabled= true;
    private byte            _histColorIdx[];
    private int             _bottomColorSize= 0;
    private int             _band= ImagePlot.NO_BAND;

    public HistogramDisplay() { }

    public HistogramDisplay(String title) {
       if (title != null)  setBorder( new TitledBorder(title));
    }


    public void setColorBand(int band) {
        Assert.argTst((band== ImagePlot.RED ||
                band==ImagePlot.GREEN ||
                band==ImagePlot.BLUE ||
                band==ImagePlot.NO_BAND ),
                      "Color must be ImagePlot.RED, ImagePlot.BLUE, ImagePlot.GREEN, or ImagePlot.NO_BAND");
        _band= band;
        if (_band!=ImagePlot.NO_BAND) {
            _model= ColorTable.getColorModel(0);
        }
    }

    public void setHistogramArray(int histogram[]) {
        _histogram= histogram;
        _histColorIdx= null;
        _model= null;
        repaint();
    }

    public void setHistogramArray(int histogram[], IndexColorModel model) {
        _histogram= histogram;
        _histColorIdx= null;
        _model= model;
        repaint();
    }


    public void setHistogramArray(int histogram[], byte histColorIdx[], IndexColorModel model) {
        _histogram= histogram;
        _histColorIdx= histColorIdx;
        _model= model;
        repaint();
    }




    public void setEnablebounds(boolean enable) {
        _boundsEnabled= enable;
        repaint();
    }

    public void setScaleOn2ndValue(boolean do2nd) {
        _do2nd= do2nd; 
    }

    public void setPrimaryUpperBounds(int upper) { 
        _upperBounds= upper; 
        repaint();
    }
    public void setPrimaryLowerBounds(int lower) { 
        _lowerBounds= lower; 
        repaint();
    }
    public void setSecondaryUpperBounds(int upper) { 
        _upperBounds2= upper; 
        repaint();
    }
    public void setSecondaryLowerBounds(int lower) { 
        _lowerBounds2= lower; 
        repaint();
    }

    public void setBottomSize(int size) {
        _bottomColorSize= size;
    }

    public void paintComponent(Graphics g) {
       int hist[]= _histogram;

       if (hist != null && hist.length > 1) {

          Insets insets= getInsets();
          Graphics2D g2 = (Graphics2D)g;
          int       y, idx, lastIdx, stepSize;
          Dimension dim= getSize();
          int       graphWidth= dim.width - (insets.left + insets.right);
          int       graphHeight= dim.height - (insets.top + insets.bottom);
          int       yTop= dim.height - insets.bottom;
          int       yBottom= insets.top;
          float     div= (float)graphWidth / (float)hist.length;
          int       max= 0; 
          int       max2= 0; 
          int       min= Integer.MAX_VALUE; 
          boolean   markOutOfBounds= false;

          _lineDataSize       = new int[graphWidth];
          _orginalHistogramIdx= new int[graphWidth];
          g2.setStroke( new BasicStroke(1) );
          g2.setPaint( Color.black);

          lastIdx= 0;
          for(int i=0; (i<hist.length); i++) {
             if (hist[i] > max) max= hist[i];
             if (hist[i] > max2 && hist[i] < max) max2= hist[i];
             if (hist[i] < min) min= hist[i];
          } 

          if (_do2nd) max= max2;

          float weight= (float)max/(float)(graphHeight-1);
          int   maxY= ((int)(max / weight)) - _bottomColorSize;

          Color color;

          for (int i=0; (i<graphWidth); i++) {
             idx= (int)(i/div); 
             stepSize= idx-lastIdx;
             lastIdx = idx;

                   // if there is not data check the bins before and after
                   // to find a better line to draw
             if (hist[idx]==0 && stepSize>=3) {
                if (hist[idx-1] > hist[idx+1]) idx= idx-1;
                else                           idx= idx+1;
             }

             y= (int)(hist[idx] / weight);
             if (hist[idx] > 0 && y < 2) y= 2;

             if (y > maxY) {
                y= maxY;
                markOutOfBounds= true;
             }
             else {
                markOutOfBounds= false;
             }

              if (_model!=null && _histColorIdx!=null) {
                  int cidx= _histColorIdx[idx] & 0xFF;
                  if (_band==ImagePlot.NO_BAND) {
                      color=  new Color(_model.getRed(cidx), _model.getGreen(cidx),
                                        _model.getBlue(cidx));
                  }
                  else {
                      int colorIdx= _model.getRed(cidx);
                      switch (_band) {
                          case ImagePlot.RED:
                              color=  new Color(colorIdx,0,0);
                              break;
                          case ImagePlot.GREEN:
                              color=  new Color(0,colorIdx,0);
                              break;
                          case ImagePlot.BLUE:
                              color=  new Color(0,0,colorIdx);
                              break;
                          default : color= null;
                              break;

                      }
                  }
              }
              else {
                  color= (_model==null) ? Color.black :
                         new Color(_model.getRed(idx), _model.getGreen(idx),
                                   _model.getBlue(idx));
              }

             g2.setPaint( color);
             g2.drawLine( i+insets.left, yTop, i+insets.left, yTop-(y+_bottomColorSize));
             g2.setPaint(Color.WHITE);
             g2.drawLine( i+insets.left, yTop-(y+1+_bottomColorSize), i+insets.left, yTop-(y+1+_bottomColorSize));
             _lineDataSize[i]       = hist[idx];
             _orginalHistogramIdx[i]= idx;
             if (markOutOfBounds) {
                 g2.setPaint( Color.red);
                 drawOutofBounds(g2, i+insets.left, yTop-(y+2+_bottomColorSize));
             } 
          }


           if (_boundsEnabled) {
               g2.setStroke( new BasicStroke(1));
               if (_upperBounds2 > -1) {
                   int x= (int)(_upperBounds2 * div);
                   g2.setPaint( Color.blue);
                   drawBounds(g2, x+insets.left, yTop, yBottom, UPPER);
               }
               if (_lowerBounds2 > -1) {
                   int x= (int)(_lowerBounds2 * div);
                   g2.setPaint( Color.blue);
                   drawBounds(g2, x+insets.left, yTop, yBottom, LOWER);
               }
               if (_upperBounds > -1) {
                   int x= (int)(_upperBounds * div);
                   g2.setPaint( Color.red);
                   drawBounds(g2, x+insets.left, yTop, yBottom, UPPER);
               }
               if (_lowerBounds > -1) {
                   int x= (int)(_lowerBounds * div);
                   g2.setPaint( Color.red);
                   drawBounds(g2, x+insets.left, yTop, yBottom, LOWER);
               }
           }
       } // end if
       else {
          super.paintComponent(g);
       } // end else
    }


    public void paintIcon(Component c, Graphics g, int x, int y) {
        paintComponent(g);
    }

    public int getIconWidth() { return getWidth(); }

    public int getIconHeight() { return getHeight(); }

    public int getHistogramDataFromScreenIdx(int x) {
        int retval= -1;
        if (_lineDataSize!=null) {
            Insets insets= getInsets();
            Dimension dim= getSize();
            if (x >= insets.left && x < dim.width-insets.right) {
                retval= _lineDataSize[x-insets.left];
            }
        }

        return retval;
    }

    public int getHistogramIdxFromScreenIdx(int x) { 
        int retval= -1;
        if (_orginalHistogramIdx!=null) {
            Insets insets= getInsets();
            Dimension dim= getSize();
            if (x >= insets.left && x < dim.width-insets.right) {
                retval= _orginalHistogramIdx[x-insets.left];
            }
        }
        return retval;
    }

    private void drawBounds(Graphics2D g2, 
                            int        x, 
                            int        yTop, 
                            int        yBottom,
                            int        which) {
       Assert.tst(which==LOWER || which==UPPER);
       if (!_boundsEnabled) return;
       int dir= (which==LOWER) ? 5 : -5;

       g2.drawLine( x, yTop,        x, yBottom);
       g2.drawLine( x+dir, yTop,    x, yTop);
       g2.drawLine( x+dir, yBottom, x, yBottom);
    }

    private void drawOutofBounds(Graphics2D g2, int x, int y) {
       g2.drawLine( x-1, y+1, x, y);
       g2.drawLine( x+1, y+1, x, y);
    }

}
