package edu.caltech.ipac.visualize.draw;


import edu.caltech.ipac.gui.BaseUserDialog;
import edu.caltech.ipac.gui.ButtonScenario;
import edu.caltech.ipac.gui.CompletedListener;
import edu.caltech.ipac.gui.DialogEvent;
import edu.caltech.ipac.gui.DialogLocatorFactory;
import edu.caltech.ipac.gui.DoneButtonScenario;
import edu.caltech.ipac.gui.SwingSupport;
import edu.caltech.ipac.gui.UserDialog;
import edu.caltech.ipac.gui.table.GenericTableModel;
import edu.caltech.ipac.gui.table.SortableJTable;
import edu.caltech.ipac.target.PositionJ2000;
import edu.caltech.ipac.target.Target;
import edu.caltech.ipac.target.TargetFixedSingle;
import edu.caltech.ipac.targetgui.TargetUIControl;
import edu.caltech.ipac.util.OSInfo;
import edu.caltech.ipac.util.action.ClassProperties;
import edu.caltech.ipac.util.action.GeneralAction;
import edu.caltech.ipac.visualize.actions.SaveCatalogAction;
import edu.caltech.ipac.visualize.plot.WorldPt;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * Dialog for FixedObject Data
 * @author Trey Roby
 */
public class FixedGroupDialog implements CompletedListener, 
                                         UserDialog,
                                         PropertyChangeListener {

  private final static ClassProperties _prop  = new ClassProperties(
                                                  FixedGroupDialog.class);
  private final static String DIALOG_TITLE  = _prop.getTitle();
  private final static String CONTROLS_TITLE= _prop.getTitle("controls");
  //private final static String SAVE          = _prop.getName("save");
  private final static String DELETE        = _prop.getName("delete");
  private final static String ASC_GIF       = "resources/sort_asc.gif";
  private final String ENABLE               = getEnableAllTitle();
  private final String DISABLE              = getDisableAllTitle();
  private final String NAME_ENABLE          = getNameEnableAllTitle();
  private final String NAME_DISABLE         = getNameDisableAllTitle();
  

  protected BaseUserDialog     _dialog;
  protected GenericTableModel  _model;
  protected SortableJTable     _tab;
  protected FixedObjectGroup   _skyGroup;
  private boolean            _usePropertyChange= true;
  protected JScrollPane        _pane;
  private JButton            _enableNameButton;
  private JButton            _enablePtButton;
  private JButton            _colorButton;
  private List<Object>       _userList= new LinkedList<Object>();

  public FixedGroupDialog(JFrame f, FixedObjectGroup skyGroup) {
    this(f,skyGroup,"FixedGroupDialog",false);
  }

  public FixedGroupDialog(JFrame           f, 
                          FixedObjectGroup skyGroup, 
                          String           helpID) {
       this(f,skyGroup,helpID,false);
  }
  public FixedGroupDialog(JFrame           f,
                          FixedObjectGroup skyGroup,
                          String           helpID,
                          boolean          mayDelete){

        this(f, skyGroup, helpID, mayDelete, null);
  }
  public FixedGroupDialog(JFrame           f,
                          FixedObjectGroup skyGroup, 
                          String           helpID,
                          boolean          mayDelete,
                          SkyShape defShape){
     _skyGroup= skyGroup;
     _skyGroup.addPropertyChangeListener(this);

     SkyShapeFactory fact= SkyShapeFactory.getInstance();
     SkyShape shapes[]= {
                                    fact.getSkyShape("square"),
                                    fact.getSkyShape("x"),
                                    fact.getSkyShape("circle"),
                                    fact.getSkyShape("cross")
      };
      //TLau 08/26/09 move _model assignment to initColumns() so inherited class can override it.
     initColumns(skyGroup, shapes);
     //_sortedModel= new SortableTableModel(skyGroup);
     ButtonScenario buttons  = makeButtons( SwingConstants.HORIZONTAL, 
                                            helpID );
     buttons.addCompletedListener(this);
     _dialog  = makeDialog(f, DIALOG_TITLE, false, buttons);
     addStuff(mayDelete, shapes, defShape);
     _dialog.setDialogLocator( 
             DialogLocatorFactory.getSideLocator(SwingConstants.EAST), true);

     //_saveCatalogDialog = new SaveCatalogDialog(f, skyGroup);
  }

  public void addUser(Object user) { _userList.add(user); }

  public void removeUser(Object user) { 
      if (user != null && _userList.contains(user)) {
          _userList.remove(user);
          if (_userList.size() == 0) {
                setVisible(false);
                _dialog.dispose();
          }
      }
  }

  public void updateButtonColors() {
      Color c= _skyGroup.getAllColor( FixedObjectGroup.COLOR_TYPE_STANDARD);
      if (c != null) _colorButton.setForeground(c);
  }

  protected void initColumns(FixedObjectGroup skyGroup, SkyShape shapes[]) {
      //TLau 08/26/09 moved from FixedGroupDialog() constructor
      FixedObjectGroupTableColumns columns=
                              new FixedObjectGroupTableColumns(skyGroup,shapes);
      _model= new GenericTableModel("FixedGroupDialog",skyGroup,
                                   columns.getColumns());
  }

  protected void addStuff(boolean mayDelete, SkyShape shapes[], SkyShape defShape) {
     _tab= new SortableJTable(_model);

     _tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
     addSelectionListener();
     if (defShape == null) defShape= _skyGroup.getAllShapes();
     _skyGroup.setAllShapes(defShape); 
     _pane= _tab.createScrollPane();
     _pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
     _pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
     JPanel workPanel= new JPanel( new BorderLayout());
     _dialog.getContentPane().add( workPanel, BorderLayout.CENTER);
     workPanel.add( _pane,  BorderLayout.CENTER);

     _tab.setBackground(Color.lightGray);
     JTableHeader head= _tab.getTableHeader();
//     head.setFont( head.getFont().deriveFont((float)14) );

     JPanel topbox= addControlButtons(shapes, defShape, mayDelete);
     topbox.setBorder(BorderFactory.createTitledBorder(CONTROLS_TITLE) );
     workPanel.add( topbox, BorderLayout.NORTH);
     
       //------------

     Dimension dim= _pane.getPreferredSize();
     dim.width= 600;
     _pane.setPreferredSize(dim);
     updateButtonColors();
  }

  protected JPanel addControlButtons(SkyShape shapes[],
                                     SkyShape defShape,
                                     boolean  mayDelete) {
     JPanel topbox= SwingSupport.makeHorizontalJPanelBox();
     topbox.add(Box.createGlue());
     topbox.add(makeEnableAllButton());
     topbox.add(Box.createGlue());
     topbox.add(makeEnableAllNamesButton());
     topbox.add(Box.createGlue());
     topbox.add(makeAllShapeButton(shapes,defShape));
     topbox.add(Box.createGlue());
     topbox.add(makeColorButton());
     topbox.add(Box.createGlue());
     topbox.add(makeMakeTargetButton());
     topbox.add(Box.createGlue());
     topbox.add(makeSaveCatalogButton());
     topbox.add(Box.createGlue());
     if (mayDelete) {
        topbox.add(makeDeletePointButton());
        topbox.add(Box.createGlue());
     }
     return topbox;
  }


  public JButton makeMakeTargetButton() {
      JButton b= SwingSupport.makeButton(new MakeTargetAction());
      if (!OSInfo.isPlatform(OSInfo.MAC)) b.setMargin( new Insets(1,1,1,1) );
      b.setMaximumSize( b.getPreferredSize() );
      return b;
  }

  public JButton makeDeletePointButton() {
      JButton b= new JButton(DELETE);
      if (!OSInfo.isPlatform(OSInfo.MAC)) b.setMargin( new Insets(1,1,1,1) );
      b.setMaximumSize( b.getPreferredSize() );
      b.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                 List<FixedObject> delList= new ArrayList<FixedObject>(10);
                 for(FixedObject fixedObj: _skyGroup) {
                    if (fixedObj.isSelected()) delList.add(fixedObj);
                 }
                 if (delList.size()>0 && _tab.isEditing()) {
                     _tab.editingCanceled( new ChangeEvent(this) );
                 }
                 for(FixedObject fixedObj: delList) {
                    _skyGroup.remove(fixedObj);
                 }
            }
        } );
      return b;
  }

  public JButton makeEnableAllButton() {
      _enablePtButton= new JButton(DISABLE);
       if (!OSInfo.isPlatform(OSInfo.MAC)) {
           _enablePtButton.setMargin( new Insets(1,1,1,1) );
       }
      _enablePtButton.setMaximumSize( _enablePtButton.getPreferredSize() );
      _enablePtButton.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                 JButton button= (JButton)ev.getSource();
                 if (button.getText().equals(ENABLE)) {
                       _skyGroup.setAllEnabled(true);
                       button.setText(DISABLE);
                       endEnableAllPointsToEnabled(true);
                 }
                 else {
                       _skyGroup.setAllEnabled(false);
                       button.setText(ENABLE);
                       endEnableAllPointsToEnabled(false);
                 }
            }
        } );
      return _enablePtButton;
  }

  public JButton makeEnableAllNamesButton() {
      _enableNameButton= new JButton(NAME_ENABLE);
       if (!OSInfo.isPlatform(OSInfo.MAC)) {
           _enableNameButton.setMargin( new Insets(1,1,1,1) );
       }
      _enableNameButton.setMaximumSize( _enableNameButton.getPreferredSize() );
      _enableNameButton.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                 JButton button= (JButton)ev.getSource();
                 if (button.getText().equals(NAME_ENABLE)) {
                       beginEnableAllNamesToEnabled(true);
                       _skyGroup.setAllNamesEnabled(true);
                       button.setText(NAME_DISABLE);
                       endEnableAllNamesToEnabled(true);
                 }
                 else {
                       beginEnableAllNamesToEnabled(false);
                       _skyGroup.setAllNamesEnabled(false);
                       button.setText(NAME_ENABLE);
                       endEnableAllNamesToEnabled(false);
                 }
            }
        } );
      return _enableNameButton;
  }

  public JComboBox makeAllShapeButton(SkyShape shapes[], SkyShape defShape) {
      JComboBox b= new JComboBox(shapes);
      b.setSelectedItem(defShape);
      b.setMaximumSize( b.getPreferredSize() );
      b.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
               JComboBox combo= (JComboBox)ev.getSource();
               SkyShape shape= (SkyShape)combo.getSelectedItem();
               _skyGroup.setAllShapes(shape);
            }
        } );
      return b;
  }

  public JButton makeColorButton() {
      JButton b= new JButton(getColorButtonTitle());
      if (!OSInfo.isPlatform(OSInfo.MAC)) b.setMargin( new Insets(1,1,1,1) );
      b.setMaximumSize( b.getPreferredSize() );
      b.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                 JButton button= (JButton)ev.getSource();
                 Color c= _skyGroup.getAllColor(
                              FixedObjectGroup.COLOR_TYPE_STANDARD);
                 if (c == null) c= Color.red;
                 c= JColorChooser.showDialog( getJDialog(), 
                                              "Choose color", c);
                 if (c != null) {
                    button.setForeground(c);
                    _skyGroup.setAllColor(
                         FixedObjectGroup.COLOR_TYPE_STANDARD,c);
                 }
            }
        } );
      _colorButton= b;
      return b;
  }

  public JButton makeSaveCatalogButton() {
      SaveCatalogAction a= new SaveCatalogAction(getJDialog(),_skyGroup);
      JButton b= SwingSupport.makeButton(a);
      if (!OSInfo.isPlatform(OSInfo.MAC)) b.setMargin( new Insets(1,1,1,1) );
      b.setMaximumSize( b.getPreferredSize() );
      return b;
  }

  protected void addSelectionListener() {
       _tab.getSelectionModel().addListSelectionListener(
           new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent ev) {
                    if (_usePropertyChange) {
                       _usePropertyChange= false;
                       ListSelectionModel mm= createMappedModel(
                               (ListSelectionModel)ev.getSource() );
                       _skyGroup.copySelectionFromModel( mm );
                       _usePropertyChange= true;
                    }
                }
           } );
  }

  public void scrollTo(FixedObject o) {
     int len= _skyGroup.size();
     FixedObject testO;
     for(int i=0; (i<len); i++) {
         testO= _skyGroup.get(i);
         if (o==testO) {
              scrollTo(i);
              break;
         }
     }
  }

  public void scrollTo(int row) {
      row= _model.getSortedView().mapUnsortedToSorted( row );
      if (row>-1) {
         _tab.setEditingRow(row);
         JScrollBar vbar= _pane.getVerticalScrollBar();
         int svalue= (int)((row * vbar.getMaximum()) / _skyGroup.size());
         vbar.setValue( svalue);
      }
  }

  public void setEnableDisableNamesToDisable(boolean toDisable) {
      if (_enableNameButton != null) {
          if (toDisable) _enableNameButton.setText(NAME_DISABLE);
          else           _enableNameButton.setText(NAME_ENABLE);
      }
  }

  public void setEnableDisablePtToDisable(boolean toDisable) {
      if (toDisable) _enablePtButton.setText(DISABLE);
      else           _enablePtButton.setText(ENABLE);
  }

  private ListSelectionModel createMappedModel(ListSelectionModel inModel) {
       ListSelectionModel outModel= new DefaultListSelectionModel();
       outModel.clearSelection();
       if (!inModel.isSelectionEmpty()) {
          int len= _skyGroup.size();
          int j;
          for(int i=0; (i<len); i++) {
               j= _model.getSortedView().mapSortedToUnsorted( i );
               if (inModel.isSelectedIndex(i))
                        outModel.addSelectionInterval( j,j);
          }
       }
       return outModel;
  }

  private void copySelectionFromDataToModel() {
     //_usePropertyChange= false;
     int len= _skyGroup.size();
     FixedObject o;
     boolean first= true;
     int j;
     for(int i=0; (i<len); i++) {
         o= _skyGroup.get(i);
         if (o.isSelected()) {
            j= _model.getSortedView().mapUnsortedToSorted( i );
            if (first) {
               _tab.setRowSelectionInterval(j,j);
               first= false;
            }
            else {
               _tab.addRowSelectionInterval(j,j);
            }
         } // end if isSelected
     } // end loop
     //_usePropertyChange= true;
  }

  private void makeTarget() {
      if (_tab.getSelectedRowCount() >0) {
          int i= _model.getSortedView().mapSortedToUnsorted( _tab.getSelectedRow());
          FixedObject fo= _skyGroup.get(i);
          WorldPt wpt= fo.getEqJ2000Position();
          PositionJ2000 pos= new PositionJ2000(wpt.getLon(), wpt.getLat());
          Target t= new TargetFixedSingle(fo.getTargetName(), pos);
          TargetUIControl.getInstance().modify(t);
      }
  }

//====================================================================
//---------------- Method from CompletedListener interface  ------------
//====================================================================

  public void inputCompleted(DialogEvent e) { }

//====================================================================
//---------------- Method from PropertyChange interface  ------------
//====================================================================

  public void propertyChange(PropertyChangeEvent ev) {
     if (ev.getPropertyName().equals(FixedObjectGroup.SELECTED_COUNT)) {
           if (_usePropertyChange) {
               _usePropertyChange= false;
               copySelectionFromDataToModel();
               _usePropertyChange= true;
           }
     }
     if (ev.getPropertyName().equals(FixedObjectGroup.ADD)) {
           _tab.editingStopped( new ChangeEvent(this) );
     }
  }

//====================================================================
//---------------- Methods from UserDialog interface  ----------------
//====================================================================
  public void setVisible(boolean visible) {
      _skyGroup.setTitle(getJDialog().getTitle());
      _dialog.setVisible(visible);
  }

  public JDialog getJDialog()             { return _dialog.getJDialog(); }
  public ButtonScenario  getButtonScenario() {
     return _dialog.getButtonScenario();
  }


  // ================================================================
  // -------------------- Protected Methods ---------------------------
  // ================================================================

  protected void endEnableAllPointsToEnabled(boolean toEnable) {}
  protected void beginEnableAllNamesToEnabled(boolean toEnable) {}
  protected void endEnableAllNamesToEnabled(boolean toEnable) {}

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

  protected String getColorButtonTitle()   { return _prop.getName("color"); }
  protected String getEnableAllTitle()     { return _prop.getName("enable"); }
  protected String getDisableAllTitle()    { return _prop.getName("disable"); }
  protected String getNameEnableAllTitle() { return
                                                _prop.getName("name.enable"); }
  protected String getNameDisableAllTitle(){ return
                                                _prop.getName("name.disable"); }

    // ================================================================
    // -------------------- Inner Classes  ---------------------------
    // ================================================================
    private class MakeTargetAction extends GeneralAction {
        public MakeTargetAction() {
            super(_prop.makeBase("MakeTargetAction"));
            setEnabled(false);
            _tab.getSelectionModel().addListSelectionListener(
                                              new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent ev) {
                    setEnabled(_tab.getSelectedRowCount()>0);
                }
            });
        }

        public void actionPerformed(ActionEvent ev) {
            makeTarget();
        }

    }
}                                         /*
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
