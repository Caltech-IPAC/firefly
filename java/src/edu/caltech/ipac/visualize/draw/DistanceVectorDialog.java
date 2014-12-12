package edu.caltech.ipac.visualize.draw;


import edu.caltech.ipac.gui.BaseUserDialog;
import edu.caltech.ipac.gui.ButtonScenario;
import edu.caltech.ipac.gui.CompletedListener;
import edu.caltech.ipac.gui.DialogEvent;
import edu.caltech.ipac.gui.DialogLocatorFactory;
import edu.caltech.ipac.gui.DoneButtonScenario;
import edu.caltech.ipac.gui.LabeledTextFields;
import edu.caltech.ipac.gui.SwingSupport;
import edu.caltech.ipac.gui.UserDialog;
import edu.caltech.ipac.gui.table.BoolRender;
import edu.caltech.ipac.gui.table.MultiLineTextRender;
import edu.caltech.ipac.gui.table.MultiLineValue;
import edu.caltech.ipac.gui.table.TableUtils;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.OSInfo;
import edu.caltech.ipac.util.action.ClassProperties;
import edu.caltech.ipac.util.action.RadioAction;
import edu.caltech.ipac.visualize.PlotFrameManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * Dialog for Distance vector data
 * @author Trey Roby
 */
public class DistanceVectorDialog implements CompletedListener, 
                                             UserDialog {

  private final static ClassProperties _prop  = new ClassProperties(
                                                  DistanceVectorDialog.class);
  private final static String DIALOG_TITLE  = _prop.getTitle();
  private final static String CONTROLS_TITLE= _prop.getTitle("controls");
  private final static String COLOR         = _prop.getName("color");
  private final static String DELETE        = _prop.getName("delete");
  private final static String POS1_TIP      = _prop.getTip("pos1");
  private final static String POS2_TIP      = _prop.getTip("pos2");

  private final static String UNIT_PROP     = 
                            _prop.makeBase("distance.units.RadioValue");

  private BaseUserDialog      _dialog;
  private JTable              _tab;
  private DistanceVectorGroup _vectGroup;
  private JScrollPane         _pane;
  private LabeledTextFields   _ltf    = LabeledTextFields.createByProp( 
                                                  _prop.makeBase("distance") );
  private JComboBox           _units   = _ltf.addRadioActionCombo("units");
  private PlotFrameManager    _frameManager;
  private Color               _defColor= new Color(153,255,153);
 

  public DistanceVectorDialog(JFrame              f, 
                              PlotFrameManager    frameManager,
                              DistanceVectorGroup vectGroup) {
     _vectGroup   = vectGroup;
     _frameManager= frameManager;
     ButtonScenario buttons  = makeButtons( SwingConstants.HORIZONTAL, 
                                            "DistanceVectorDialog" );
     buttons.addCompletedListener(this);
     _dialog  = makeDialog(f, DIALOG_TITLE, false, buttons);
     addStuff();
     _dialog.setDialogLocator( 
             DialogLocatorFactory.getSideLocator(SwingConstants.EAST), true);
  }

  private void addStuff() {
     _tab = new JTable(_vectGroup);
     JPanel topbox= SwingSupport.makeHorizontalJPanelBox();
     _pane =new JScrollPane(_tab);
     JPanel workPanel= new JPanel( new BorderLayout());
     _dialog.getContentPane().add( workPanel, BorderLayout.CENTER);
     workPanel.add( _pane,  BorderLayout.CENTER);
     workPanel.add( topbox, BorderLayout.NORTH);

     _tab.setBackground(Color.lightGray);
     JTableHeader head= _tab.getTableHeader();
     head.setFont( head.getFont().deriveFont((float)16) );

     topbox.setBorder(BorderFactory.createTitledBorder(CONTROLS_TITLE) );

     topbox.add(Box.createGlue());
     topbox.add(makeColorButton());
     topbox.add(Box.createGlue());
     topbox.add(_ltf);
     topbox.add(Box.createGlue());
     topbox.add(makeDeleteButton());
     topbox.add(Box.createGlue());

     addUnitListener();

     //  set a toggle button on the ENABLED column
     MultiLineTextRender ren;
     TableUtils.defineBoolRenderEdit( _tab, FixedObjectGroup.ENABLED_IDX, 
                                     _prop.makeBase("on") );
     ren= new MultiLineTextRender();
     TableUtils.setRendererOnColumn(ren,_tab, DistanceVectorGroup.POS1_IDX);
     TableUtils.setColumnTip(POS1_TIP, _tab,  DistanceVectorGroup.POS1_IDX);

     ren= new MultiLineTextRender(); // need a new renderer so the tooltip
                                     // for two columns could be different
                                     // AR_1077
     TableUtils.setRendererOnColumn(ren,_tab, DistanceVectorGroup.POS2_IDX);
     TableUtils.setColumnTip(POS2_TIP, _tab,  DistanceVectorGroup.POS2_IDX);

     computeRowsColumnsSizes(ren);



  } 

  public void computeRowsColumnsSizes(MultiLineTextRender ren) {

     MultiLineValue mentry[]=  new MultiLineValue[2];
     mentry[0]= new MultiLineValue("Lon: ", _vectGroup.formatLon(12.0,false) );
     mentry[1]= new MultiLineValue("Lat: ",_vectGroup.formatLat(34.0,false) );
     ren.setMultiValue(mentry);
     Dimension dim= ren.getPreferredSize();
     _tab.setRowHeight(dim.height);
     TableColumnModel cm= _tab.getColumnModel();
     cm.getColumn(DistanceVectorGroup.ENABLED_IDX).setPreferredWidth(30);
     TableColumn column= cm.getColumn(DistanceVectorGroup.ENABLED_IDX);
     BoolRender render= (BoolRender)column.getCellRenderer();
     TableUtils.setPrefAndMaxWidth(_tab, DistanceVectorGroup.ENABLED_IDX, 
                                     render.getPreferredSize().width+5 );
     cm.getColumn(DistanceVectorGroup.POS1_IDX).setPreferredWidth(dim.width+30);
     cm.getColumn(DistanceVectorGroup.POS2_IDX).setPreferredWidth(dim.width+30);
     cm.getColumn(DistanceVectorGroup.DIST_IDX).setPreferredWidth( 70);
  }

  public JButton makeDeleteButton() {
      JButton b= new JButton(DELETE);
      if (!OSInfo.isPlatform(OSInfo.MAC)) {
          b.setMargin( new Insets(1,1,1,1) );
      }
      b.setMaximumSize( b.getPreferredSize() );
      b.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                 JButton button= (JButton)ev.getSource();
                 if (_tab.getSelectedRowCount() > 0) {
                    int selectedRows[]= _tab.getSelectedRows();
                    _vectGroup.remove(selectedRows);
                    int newrow= selectedRows[0]-1;
                    if (_vectGroup.getRowCount() > 0) {
                       newrow=  (newrow > 0) ? newrow : 0;
                       select( newrow );
                       scrollTo( newrow );
                    }
                 }
            }
        } );
      return b;
  }

  public void scrollTo(int row) {
      _tab.setEditingRow(row);
      JScrollBar vbar= _pane.getVerticalScrollBar();
      int svalue;
      if (row == _vectGroup.getRowCount()-1) {
         svalue= vbar.getMaximum();
      }
      else {
         svalue= (int)((row * vbar.getMaximum()) / _vectGroup.getRowCount());
      }
      vbar.setValue( svalue);
      //System.out.println("scrollTo= "+row + " " + svalue);
  }

  public void select(int row) {
      _tab.setRowSelectionInterval(row,row);
  }

  public JButton makeColorButton() {
      JButton b= new JButton(COLOR);
      if (!OSInfo.isPlatform(OSInfo.MAC)) {
          b.setMargin( new Insets(1,1,1,1) );
      }
      b.setMaximumSize( b.getPreferredSize() );
      b.setForeground(_defColor);
      _vectGroup.setAllLineColor(_defColor);
      _vectGroup.setAllTextColor(_defColor);
      b.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                 JButton button= (JButton)ev.getSource();
                 _defColor= JColorChooser.showDialog( getJDialog(), 
                                              "Choose color", _defColor);
                 if (_defColor != null) {
                    button.setForeground(_defColor);
                    _vectGroup.setAllLineColor(_defColor);
                    _vectGroup.setAllTextColor(_defColor);
                    _frameManager.redrawAllPlots();
                 }
            }
        } );
      return b;
  }

  public void addUnitListener() {
      setUnits();
      _units.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                 setUnits();
            }
        } );
  }

//====================================================================
//---------------- Method from CompletedListener interface  ------------
//====================================================================

  public void inputCompleted(DialogEvent e) { }


//====================================================================
//---------------- Methods from UserDialog interface  ----------------
//====================================================================
  public void setVisible(boolean visible) {
         _dialog.setVisible(visible);
  }

  public JDialog getJDialog()             { return _dialog.getJDialog(); }
  public ButtonScenario  getButtonScenario() {
     return _dialog.getButtonScenario();
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

  //===================================================================
  //------------------ Private / Protected Methods ------------------==
  //===================================================================
 
  private void setUnits() {
        RadioAction ra= (RadioAction)_units.getModel();
        int value= ra.getSelectedInt();
        _vectGroup.setDistanceUnits(value);
        String command= ra.getRadioValue();
        AppProperties.setPreference(UNIT_PROP, command);
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
