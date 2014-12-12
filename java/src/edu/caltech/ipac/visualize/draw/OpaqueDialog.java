package edu.caltech.ipac.visualize.draw;


import edu.caltech.ipac.gui.BaseUserDialog;
import edu.caltech.ipac.gui.ButtonScenario;
import edu.caltech.ipac.gui.CompletedListener;
import edu.caltech.ipac.gui.DialogEvent;
import edu.caltech.ipac.gui.DialogLocatorFactory;
import edu.caltech.ipac.gui.DoneButtonScenario;
import edu.caltech.ipac.gui.SwingSupport;
import edu.caltech.ipac.gui.Title;
import edu.caltech.ipac.gui.UserDialog;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.action.ClassProperties;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.PlotView;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Iterator;


/**
 * Opaque Dialog
 *
 * @author Trey Roby
 * @version $Id: OpaqueDialog.java,v 1.3 2005/12/29 00:01:13 roby Exp $
 *
 */
public class OpaqueDialog implements CompletedListener, 
                                     UserDialog,
                                     ChangeListener {

  private final static ClassProperties _prop  = new ClassProperties(
                                                      OpaqueDialog.class);
  private final static String DIALOG_TITLE= _prop.getTitle();
  private final static String BASE_EXT    = _prop.getTitle("baseExt");
  private final static String SCALE_TITLE = _prop.getTitle("scale");
  private final static String LABEL_BASE  = _prop.getName("labelBase");

  private BaseUserDialog    _dialog;
  private JSlider           _opaqueRange  = new JSlider(0,100);
  private Title             _valueLabel= new Title(
              "dummy dummy  dummy dummy dummy ", 12);
  private PlotView          _pv  = null;
  private Plot              _plot= null;
 
  public OpaqueDialog(JFrame f, Plot plot, PlotView pv) {
     _plot  = plot;
     _pv  = pv;
     init(f);
  }

  public OpaqueDialog(JFrame f, PlotView pv) {
     _pv  = pv;
     init(f);
  }





//======================================================================
//---------------- Method from ChangeListener interface  ---------------
//======================================================================

   public void stateChanged(ChangeEvent ev) {
       if (_dialog.getJDialog().isVisible()) {
            int value= _opaqueRange.getValue();
            Plot p;
            if (_plot == null) {
                 Assert.tst(_pv);
                 Iterator i= _pv.iterator();
                 while( i.hasNext()) {
                     p= (Plot)i.next();
                     p.setPercentOpaque( (float)(value / 100.0F) );
                 }
            }
            else {
                 Assert.tst(_plot);
                 _plot.setPercentOpaque( (float)(value / 100.0F) );
            }
            _pv.repair();
            setValueLabel();
       } // end if isVisible
   }

//======================================================================
//---------------- Method from CompletedListener interface  ------------
//======================================================================

  public void inputCompleted(DialogEvent ev) { }



//======================================================================
//---------------- Methods from UserDialog interface  ------------------
//======================================================================
  public void    setVisible(boolean visible) { 
         if (visible) {
             initScale();
             setValueLabel();
         }
         _dialog.setVisible(visible);
  }
  public JDialog getJDialog()                { return _dialog.getJDialog(); }

  public ButtonScenario  getButtonScenario() {
     return _dialog.getButtonScenario();
  }

//======================================================================
//--------------------- Private / Protected Methods --------------------
//======================================================================

  private void init(JFrame f) {
     ButtonScenario buttons  = makeButtons( SwingConstants.HORIZONTAL, 
                                            "OpaqueDialog" );
     buttons.addCompletedListener(this);
     String title;
     title= (_plot==null) ? DIALOG_TITLE + " - " + BASE_EXT :
                            DIALOG_TITLE + " - " + _plot.getPlotDesc();
     _dialog  = makeDialog(f, title, false, buttons);
     addStuff();
     //_dialog.setDialogLocator( 
     //     DialogLocatorFactory.getBottomLocator(SwingConstants.WEST), true);
     _dialog.setDialogLocator( 
            DialogLocatorFactory.getPanelBottomLocator(), true);
  }

  private void addStuff() {
     JPanel workPanel= new JPanel( new BorderLayout());
     _dialog.getContentPane().add( workPanel, BorderLayout.CENTER);
     JPanel box= SwingSupport.makeVerticalJPanelBox();
     workPanel.add( box, BorderLayout.CENTER);

     _opaqueRange.addChangeListener(this);
     _opaqueRange.setBorder(new TitledBorder(SCALE_TITLE) );
     Dimension d= _opaqueRange.getPreferredSize();
     d.width= 200;
     _opaqueRange.setPreferredSize(d);
     _opaqueRange.setMaximumSize(d);
     box.add(_opaqueRange);
     box.add(_valueLabel);
  }


   public void setValueLabel() {
      if (_plot == null) {
         _valueLabel.setText(LABEL_BASE + 
                  (int)(_pv.getPrimaryPlot().getPercentOpaque() * 100) + "%" );
      }
      else {
         _valueLabel.setText(LABEL_BASE + 
                  (int)(_plot.getPercentOpaque() * 100) + "%" );
      }
   }

   public void initScale() {
      if (_plot == null) {
          _opaqueRange.setValue(
              (int)(_pv.getPrimaryPlot().getPercentOpaque() * 100));
      }
      else {
          _opaqueRange.setValue( (int)(_plot.getPercentOpaque() * 100));
      }
   }



  // ================================================================
  // -------------------- Factory Methods ---------------------------
  // ================================================================
  protected ButtonScenario makeButtons(int direction, String helpID) {
      return new DoneButtonScenario( SwingConstants.HORIZONTAL, helpID);
  }
  protected BaseUserDialog makeDialog(JFrame         f, 
                                      String         title,  
                                      boolean        modal, 
                                      ButtonScenario b) {
      return new BaseUserDialog(f, title, modal, b);
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
