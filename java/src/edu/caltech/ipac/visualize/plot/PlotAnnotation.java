package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.astro.CoordException;
import edu.caltech.ipac.target.TargetUtil;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.visualize.VisConstants;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Point;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * This class is the "gui canvas" that a plot is painted on.  It is a 
 * subclass of JComponent so it can provide all the paint and graphics 
 * interface into Swing.  It also manages multiple plot classes.  Currently
 * only one plot class is primary.  When a paintComponent event comes through
 * it passes the event (and the Graphics2D) to the Plot class that is primary.
 * This is one of the most key classes in the all vis packages.
 *
 * @see edu.caltech.ipac.visualize.plot.Plot
 * @see edu.caltech.ipac.gui.MouseCentral
 *
 * @author Trey Roby
 * @version $Id: PlotAnnotation.java,v 1.3 2010/04/20 21:42:47 roby Exp $
 * *
 */
public class PlotAnnotation extends JComponent
                            implements PlotViewStatusListener,
                                       PlotPaintListener,
                                       PropertyChangeListener {

    public static enum Direction {HORIZONTAL, VERTICAL};
    private static final int BAR_THICKNESS= 30;
    private PlotView _pv;
    private Font _font= new JLabel().getFont().deriveFont(12.0F);
    private int _size;
    private JScrollPane _plotScroll;
    private Color       _fillColor;
    private Direction _direction;
    private CoordinateSys _csys;
    private boolean _showPosInDecimal;

  public PlotAnnotation(int direction,
                        PlotView pv,
                        JScrollPane plotScroll,
                        Color fillColor) {
      Assert.argTst(direction==SwingConstants.HORIZONTAL ||
                    direction==SwingConstants.VERTICAL,
                    "direction must be SwingConstants.HORIZONTAL"+
                    " or SwingConstants.VERTICAL");
      _pv= pv;
      _plotScroll= plotScroll;
      _fillColor= fillColor;
      if (direction==SwingConstants.HORIZONTAL) {
          _direction= Direction.HORIZONTAL;
      }
      else {
          _direction= Direction.VERTICAL;
      }
      setOpaque(true);
      setForeground(pv.getFillColor());
      _pv.addPlotViewStatusListener(this);
      _pv.addPlotPaintListener(this);
      AppProperties.addPropertyChangeListener(this);
      addSBListener();
  }

  public void paintComponent(Graphics g) {
      super.paintComponent(g);

       Graphics2D g2 = (Graphics2D)g;
      String csysDesc = AppProperties.getPreference(
                                    VisConstants.COORD_SYS_PROP,
                                    CoordinateSys.EQ_J2000_STR);

      _showPosInDecimal= AppProperties.getBooleanPreference(
                                    VisConstants.COORD_DEC_PROP, false);
      _csys= CoordinateSys.parse(csysDesc);
      WorldPt pt;
      g2.setPaint( _fillColor );
      Rectangle drawHere = g.getClipBounds();





      int edgeValue;
      int drawSize;
      int pSize= getPlotSize();
      int start;
      int end;
      Rectangle visRec=  _pv.getVisibleRect();
      String tstStr= getLabelStr(visRec.x + visRec.width/2,
                                 visRec.y+visRec.y/2);
      TextLayout textLayout= new TextLayout(tstStr,
                                            _font,g2.getFontRenderContext());
      int step= (int)textLayout.getBounds().getWidth();
      step+=20;

      if (_direction==Direction.HORIZONTAL) {
          edgeValue= _pv.getVisibleRect().y;
          drawSize= pSize < drawHere.width ? pSize : drawHere.width;
          start=  ((drawHere.x-200) / step)  * step;
          end= ( (drawHere.x+drawHere.width) / step)  * step;
          g.fillRect(drawHere.x, drawHere.y, drawSize, drawHere.height);
          start+=  50;
          end+= 50 + step;

      }
      else {
          edgeValue= visRec.x;
          drawSize= pSize < drawHere.height ? pSize : drawHere.height;
          start=  (visRec.y / step)  * step -step;
          start+=  50;
          if (start<50) start= 50;
          end= visRec.y + visRec.height+step;
          //end= ( (drawHere.y+drawHere.height) / 100)  * 100;
          g.fillRect(drawHere.x, drawHere.y, drawHere.width,drawSize);
//          System.out.println("clip: "+g2.getClip());
//          System.out.println("vis:  "+getVisibleRect());
//          System.out.println("dh :  "+drawHere);
//          System.out.println("pSize:"+pSize);
//          System.out.println("plot:  "+_pv.getVisibleRect());
//          System.out.printf("start= %d,   end= %d%n",start,end);
      }
      end= end > (pSize-step) ? pSize-step : end;

      Line2D line;
      g2.setPaint(Color.BLACK);
      AffineTransform normalTrans= (AffineTransform)g2.getTransform().clone();
      for(int i= start; (i<end); i+=step) {
          String out= getLabelStr(i,edgeValue);
          if (_direction==Direction.HORIZONTAL) {
              textLayout= new TextLayout(out,_font,g2.getFontRenderContext());
              int width= (int)textLayout.getBounds().getWidth();
              int backOffset=  width/2;
              textLayout.draw(g2, i-backOffset, 20);
              line= new Line2D.Float(i,20,i,30);
              System.out.printf("drawing at: %d%n",i);
          }
          else {
              textLayout= new TextLayout(out,_font,g2.getFontRenderContext());
              AffineTransform textAt = new AffineTransform();

              textAt.translate(0, (float)textLayout.getBounds().getHeight());
              Shape shape= textLayout.getOutline(null);

              boolean rotateString= true;

              if (rotateString) {
                  AffineTransform at= new AffineTransform();
                  at.setToIdentity();
                  at.translate(20,i);
                  at.rotate(Math.toRadians(270.0));
                  AffineTransform toCenterAt = new AffineTransform();
                  toCenterAt.concatenate(at);
                  Rectangle r = shape.getBounds();
                  toCenterAt.translate(-(r.width/2), -(r.height/2));
                  g2.transform(toCenterAt);
                  textLayout.draw(g2, 0,0);
              }
              else {
                  g2.setTransform(normalTrans);
                  textLayout.draw(g2, 2,i);
              }
              g2.setTransform(normalTrans);
              line= new Line2D.Float(20,i,30,i);
              //System.out.printf("drawing at: %d%n",i);
          }
          g2.draw(line);

      }

      if (_direction==Direction.HORIZONTAL) {
          if (getVisibleRect().width < _pv.getVisibleRect().width) {
              setSize(getPreferredSize());
              repaint();
          }
      }
      else {
          if (getVisibleRect().height < _pv.getVisibleRect().height) {
              setSize(getPreferredSize());
              repaint();
          }
      }

   }

    // ========================================================================
    // ----------------- Methods form PlotViewStatusListener interface --------
    // ========================================================================

    public void plotAdded(PlotViewStatusEvent ev) {
        _size= getPlotSize();
        setPreferredSize(_size);
    }

    public void plotRemoved(PlotViewStatusEvent ev) {
        _size= getPlotSize();
        setPreferredSize(_size);
    }


    private void addSBListener() {
        JScrollBar sb;
        if (_direction==Direction.HORIZONTAL) {
            sb= _plotScroll.getVerticalScrollBar();
        }
        else {
            sb= _plotScroll.getHorizontalScrollBar();
        }
        sb.getModel().addChangeListener( new ChangeListener() {
                   public void stateChanged(ChangeEvent ev) { repaint(); }
       } );
    }

    private void setPreferredSize(int size) {
        if (_direction==Direction.HORIZONTAL) {
            setPreferredSize(new Dimension(_size,BAR_THICKNESS));
        }
        else {
            setPreferredSize(new Dimension(BAR_THICKNESS,_size));
        }
    }

    private int getPlotSize() {
        int size;
        if (_direction==Direction.HORIZONTAL) {
            size= _pv.getPrimaryPlot().getScreenWidth();
        }
        else {
            size= _pv.getPrimaryPlot().getScreenHeight();
        }
        return size;
    }

    private String getLabelStr(int x, int y) {
        String out= "                 ";

        try {
            Plot p= _pv.getPrimaryPlot();
            WorldPt pt= p.getWorldCoords(new Point(x,y), _csys);
            if (_showPosInDecimal) {
                if (_direction==Direction.HORIZONTAL) {
                    out= String.format("%5.3f",pt.getLon());
                }
                else {
                    out= String.format("%5.3f",pt.getLat());
                }
            }
            else {
                if (_direction==Direction.HORIZONTAL) {
                    out= TargetUtil.convertLonToString(pt.getLon(),
                                    pt.getCoordSys().isEquatorial());
                }
                else {
                    out= TargetUtil.convertLatToString(pt.getLat(),
                                    pt.getCoordSys().isEquatorial());

                }
            }
        } catch (NoninvertibleTransformException ignore) {
        } catch (ProjectionException ignore) {
        } catch (CoordException ignore) {
        }
        return out;
    }

    // ========================================================================
    // ----------------- Methods form PlotPaintListener interface --------
    // ========================================================================

    public void paint(PlotPaintEvent ev) {
        int tstSize= getPlotSize();
        if (tstSize!=_size) {
            _size= tstSize;
            setPreferredSize(_size);
            repaint();
        }
    }

    // ========================================================================
    // ----------------- Methods form PropertyChangeListener interface --------
    // ========================================================================


    public void propertyChange(PropertyChangeEvent ev) {
        String prop= ev.getPropertyName();
        if (prop.equals(VisConstants.COORD_SYS_PROP) ||
            prop.equals(VisConstants.COORD_DEC_PROP)) {
            repaint();
        }
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
