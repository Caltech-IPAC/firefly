package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.client.ClientLog;
import edu.caltech.ipac.gui.MouseCentral;
import edu.caltech.ipac.gui.MouseUser;
import edu.caltech.ipac.gui.MouseUserEvent;
import edu.caltech.ipac.gui.MouseUserListener;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.visualize.PlotFrame;
import edu.caltech.ipac.visualize.PlotFrameManager;
import edu.caltech.ipac.visualize.VisFrame;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.PlotPaintEvent;
import edu.caltech.ipac.visualize.plot.PlotPaintListener;
import edu.caltech.ipac.visualize.plot.PlotView;
import edu.caltech.ipac.visualize.plot.PlotViewStatusEvent;
import edu.caltech.ipac.visualize.plot.PlotViewStatusListener;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
/**
 * User: roby
 * Date: Sep 29, 2007
 * Time: 3:21:16 PM
 */


/**
 * @author Trey Roby
 */
public class AreaSelect implements MouseUserListener,
                                   PlotViewStatusListener,
                                   PlotPaintListener {

    private final AreaSelectResults _areaSelect;
    private final PlotFrameManager _frameManager;
    private boolean    _dragging= false;
    private PlotView _pv= null;
    private PlotFrame _pf= null;
    private MouseUser _mouseUser;
    ImageWorkSpacePt _pts[];

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public AreaSelect(AreaSelectResults areaSelect,
                      PlotFrameManager frameManager,
                      String           mouseHelp,
                      String           mouseDesc ) {
        Assert.argTst(areaSelect!=null,
                      "You may not pass null for the areaSelect parameter");
        _frameManager= frameManager;
        _areaSelect= areaSelect;
        
        PlotFrame pf= _frameManager.getActivePlotFrame();
        PlotView pv;
        MouseCentral mouse;
        if (pf != null) {
            AreaSelectMouse dmouse= new AreaSelectMouse();
            _mouseUser= new MouseUser( dmouse,dmouse,mouseHelp, mouseDesc);
            for(VisFrame vf : _frameManager) {
                if (vf instanceof PlotFrame) {
                    pv= ((PlotFrame)vf).getPlotView();
                    mouse= pv.getMouseCentral();
                    mouse.addControl(_mouseUser);
                    mouse.grabControl(_mouseUser);
                    mouse.addMouseUserListener(this);
                }
            }
        }
        else {
            _areaSelect.cancel();
        }

    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    // -------------------------------------------------------------------
    // ===============  PlotViewStatusListener Methods ===================
    // -------------------------------------------------------------------

    public void plotAdded(PlotViewStatusEvent ev) {
    }

    public void plotRemoved(PlotViewStatusEvent ev) {
    }

    // -------------------------------------------------------------------
    // ===============  PlotPaintListener Methods ===================
    // -------------------------------------------------------------------
    public void paint(PlotPaintEvent ev) {
        Graphics2D g2= ev.getGraphics();
        g2.setColor(new Color(0F,0F,1.0F,.5F));
        ImageWorkSpacePt pt1= _pts[0];
        ImageWorkSpacePt pt2= _pts[2];
        int x= (int)Math.min(pt2.getX(),pt1.getX());
        int y= (int)Math.min(pt2.getY(),pt1.getY());
        int width= Math.abs((int)(pt2.getX()-pt1.getX()));
        int height= Math.abs((int)(pt2.getY()-pt1.getY()));
        Rectangle2D rec= new Rectangle2D.Double(x, y, width, height);
        g2.fill(rec);
//        System.out.printf("drawing x: %d, y: %d, width: %d, height: %d%n",
//                          (int)pt1.getX(), (int)pt1.getY(),
//                          width, height);
    }

    // -------------------------------------------------------------------
    // ====================  MouseUserListener Methods ===================
    // -------------------------------------------------------------------

    public void mouseUserChanged(MouseUserEvent ev) {
        _areaSelect.cancel();
    }
    public void mouseUserAdded(MouseUserEvent ev) { }
    public void mouseUserRemoved(MouseUserEvent ev) { }

//=======================================================================
//-------------- Method from LabelSource Interface ----------------------
//=======================================================================

    private void startDrag(ImageWorkSpacePt ipt) {
        _pf= _frameManager.getActivePlotFrame();
        _pv= _pf.getPlotView();
        _pts= new ImageWorkSpacePt[] {ipt, ipt, ipt, ipt};
        _dragging= true;

        Color c= Color.RED;
        LineShape line= new LineShape();
        line.setColor(c);
//        _pv.addPlotViewStatusListener( this);
        _pv.addPlotPaintListener(this);
        _pv.repaint();
        _pv.repair();
    }

    private void doDrag(ImageWorkSpacePt newIpt, int newX, int newY) {
        _pts= makeRec(_pts[0],newIpt,newX,newY);
        _pv.repair();
    }

    private void endDrag() {
        _dragging= false;
        releaseMouseUser();
        if (_pts!=null)  {
            _pv.removePlotViewStatusListener( this);
            _pv.removePlotPaintListener(this);
            _pv.repair();
            _areaSelect.areaSelected(_pf, _pv, _pts[0], _pts[2]);
        }
        _pts= null;
        _mouseUser= null;
        _pv= null;
        _pf= null;
    }





//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private ImageWorkSpacePt [] makeRec(ImageWorkSpacePt ipt0, ImageWorkSpacePt newIpt, int newX, int newY) {
        ImageWorkSpacePt ptAry[]= { ipt0, ipt0, ipt0, ipt0, ipt0 };
        try {
            Plot plot= _pv.getPrimaryPlot();
            ImageWorkSpacePt iwspt = new ImageWorkSpacePt(ipt0.getX(), ipt0.getY());
            Point2D pt0= plot.getScreenCoords(iwspt);

            ptAry[0]= ipt0;
            ptAry[1]= plot.getImageWorkSpaceCoords(new Point((int)pt0.getX(), newY));
            ptAry[2]= newIpt;
            ptAry[3]= plot.getImageWorkSpaceCoords( new Point(newX, (int)pt0.getY()));
        } catch (NoninvertibleTransformException nte) {
            ClientLog.warning( "FocalControl.FocalMouse.mousePressed: "+ nte);
        }

        return ptAry;
    }


    private void releaseMouseUser() {
        PlotView pv;
        MouseCentral mouse;
        for(VisFrame vf : _frameManager) {
            if (vf instanceof PlotFrame) {
                pv= ((PlotFrame)vf).getPlotView();
                mouse= pv.getMouseCentral();
                mouse.removeMouseUserListener(this);
                pv.getMouseCentral().removeControl(_mouseUser);
            }
        }

    }


    // -------------------------------------------------------------------
    // ==================  Private Inner Classes    ======================
    // -------------------------------------------------------------------

    /**
     * This class controls the mouse picking for the Focal Control.
     * Currently on Mouse button 1 is used.  It will move the focal plane.
     */
    private class AreaSelectMouse extends MouseAdapter
                                  implements MouseMotionListener {
        public void mousePressed(MouseEvent ev) {
            PlotFrame pf= _frameManager.getActivePlotFrame();
            Plot plot= pf.getPlotView().getPrimaryPlot();
            try {
                ImageWorkSpacePt ipt = plot.getImageWorkSpaceCoords(ev);
                if (ev.getModifiers() == InputEvent.BUTTON1_MASK) {
                    startDrag(ipt);
                }
            } catch (NoninvertibleTransformException nte) {
                System.out.println(
                                              "FocalControl.FocalMouse.mousePressed: "+ nte);
            }
        }
        public void mouseReleased(MouseEvent e) {
            if (_dragging) {
                endDrag();
            } // end if isSelected
        }

        public void mouseDragged(MouseEvent ev) {
            PlotFrame pf= _frameManager.getActivePlotFrame();
            if (_dragging) {
                Plot plot= pf.getPlotView().getPrimaryPlot();
                try {
                    ImageWorkSpacePt ipt= plot.getImageWorkSpaceCoords(ev);
                    if ((ev.getModifiers() & InputEvent.BUTTON1_MASK) > 0) {
                        doDrag(ipt,ev.getX(), ev.getY());
                    }
                } catch (NoninvertibleTransformException nte) {
                    System.out.println(
                                                  "FocalControl.FocalMouse.mousePressed: "+ nte);
                }
            } // end if layerEnabled
        }
        public void mouseMoved(MouseEvent e) {}
    }



// =====================================================================
// -------------------- public Inner Classes --------------------------------
// =====================================================================

    public interface AreaSelectResults {
        public void areaSelected(PlotFrame pf,
                                 PlotView pv,
                                 ImageWorkSpacePt corner1,
                                 ImageWorkSpacePt corner2);
        public void cancel();
    }

}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
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
