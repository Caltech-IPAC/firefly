package edu.caltech.ipac.visualize.draw;


import edu.caltech.ipac.gui.BaseUserDialog;
import edu.caltech.ipac.gui.ButtonScenario;
import edu.caltech.ipac.gui.CompletedListener;
import edu.caltech.ipac.gui.DialogEvent;
import edu.caltech.ipac.gui.DialogLocatorFactory;
import edu.caltech.ipac.gui.DoneButtonScenario;
import edu.caltech.ipac.gui.LabeledTextFields;
import edu.caltech.ipac.gui.SwingSupport;
import edu.caltech.ipac.gui.Title;
import edu.caltech.ipac.gui.UserDialog;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.action.ClassProperties;
import edu.caltech.ipac.util.action.Prop;
import edu.caltech.ipac.util.action.RadioAction;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * need to document
 *
 * @author Trey Roby
 * @version $Id: ScaleValueDialog.java,v 1.9 2007/10/23 01:03:36 roby Exp $
 *
 */
public class ScaleValueDialog implements CompletedListener,
                                         UserDialog,
                                         ChangeListener,
                                         PropertyChangeListener {

  private final static ClassProperties _prop  = new ClassProperties(
                                                  ScaleValueDialog.class);
  private final static String DIALOG_TITLE     = _prop.getTitle();
  //private final static String MIN_SCALE_TITLE  = _prop.getTitle("minimum");
  //private final static String MAX_SCALE_TITLE  = _prop.getTitle("maximum");
  private final static String SHOW          = _prop.getName("showLabelBase");
  private final static String HIDE          = _prop.getName("hideLabelBase");
  private final static String BETWEEN       = _prop.getName("betweenLabelBase");
  private final static String AND           = _prop.getName("and");

  private final static int MAX_ROWS= 5000;


  private final static String GREATER_THAN  = _prop.getName("greaterThan");
  private final static String LESS_THAN     = _prop.getName("lessThan");
  private final static String HIDE_ALL_LABEL= _prop.getName("hideAllLabel");
  private final static int    SHOW_BETWEEN  = 1;
  private final static int    HIDE_BETWEEN  = 2;

  private BaseUserDialog    _dialog;
  private FixedObjectGroup  _fixedGroup;
  private JComboBox         _dataField = new JComboBox();
  private JSlider           _minRange  = new JSlider(0,100);
  private JSlider           _maxRange  = new JSlider(0,100);
  private Title             _valueLabel= new Title(
     "dummy dummy dummy dummy  dummy dummy dummy dummy dummy dummy dummy ", 12);
  private Map<String,String> _dataMap   = new HashMap<String,String>(13);
  private NumberFormat      _nf        = NumberFormat.getInstance(); //I18N compliant
  private boolean           _makingMods= false;
  private LabeledTextFields _ltf       = LabeledTextFields.createByProp(
                                                     _prop.makeBase("fields") );
  private RadioAction       _dirAction = new RadioAction(
                                          _prop.makeBase("fields.direction") );
  private List<Data>        _dataList;
  private int               _typeOfAction;
  private boolean           _mayAdjust= true;
  private boolean           _firstDisplayed= true;
  private final JDialog _feedbackDialog;
  private JLabel _feedbackLabel= new JLabel(_prop.getName("feedback"));


  public ScaleValueDialog(JFrame f, FixedObjectGroup fixedGroup, String title){
     Assert.tst(fixedGroup.getExtraDataDefs() != null,
                "This dialog should never be made if there is no extra data");
     _fixedGroup= fixedGroup;
     _fixedGroup.addPropertyChangeListener(this);
     _nf.setMaximumFractionDigits(3);
     _nf.setMinimumFractionDigits(3);
     ButtonScenario buttons  = makeButtons( SwingConstants.HORIZONTAL,
                                            "ScaleValueDialog" );
     buttons.addCompletedListener(this);
     _dialog  = makeDialog(f, DIALOG_TITLE + " - " + title, false, buttons);
     _feedbackDialog= new JDialog(getJDialog());
     addStuff();
     _dialog.setDialogLocator(
             DialogLocatorFactory.getBottomLocator(SwingConstants.EAST), true);
  }




//======================================================================
//---------------- Method from ChangeListener interface  ---------------
//======================================================================

   public void stateChanged(ChangeEvent ev) {
       if (_dialog.getJDialog().isVisible() && _mayAdjust) {
           JSlider slider= (JSlider)ev.getSource();
            if (_fixedGroup.size() > MAX_ROWS &&
                slider.getValueIsAdjusting()) {
                    _fixedGroup.beginBulkUpdate();
            }
            else {
                int minValue= _minRange.getValue();
                int maxValue= _maxRange.getValue();
                if (_minRange == ev.getSource()) {
                    if (minValue > maxValue)
                        _minRange.setValue(maxValue);
                    else
                        sliderMoved(minValue, maxValue);
                }
                else if (_maxRange == ev.getSource()) {
                    if (maxValue < minValue)
                        _maxRange.setValue(minValue);
                    else
                        sliderMoved(minValue, maxValue);
                }
                else {
                    Assert.stop();
                }

                if (_fixedGroup.size() > MAX_ROWS &&
                    !slider.getValueIsAdjusting()) {
                    _fixedGroup.endBulkUpdate();
                }
            }


       } // end if isVisible
   }

//======================================================================
//---------------- Method from CompletedListener interface  ------------
//======================================================================

  public void inputCompleted(DialogEvent ev) { }

//======================================================================
//---------------- Method from PropertyChange interface  ---------------
//======================================================================

  public void propertyChange(PropertyChangeEvent ev) {
      String pname= ev.getPropertyName();
      if (!_makingMods &&
          (pname.equals(FixedObjectGroup.BULK_UPDATE) ||
           pname.equals(FixedObjectGroup.ADD)         ||
           pname.equals(FixedObjectGroup.REMOVE) ) ) {
          if (_dialog.isVisible()) updateSelected(true);
      }
  }


//======================================================================
//---------------- Methods from UserDialog interface  ------------------
//======================================================================
  public void    setVisible(boolean visible) {
      _dialog.setVisible(visible);
      _valueLabel.setText("");

      if (_firstDisplayed){
          updateSelected(true);
          _firstDisplayed= false;
      }
     if (visible) sliderMoved(_minRange.getValue(), _maxRange.getValue());
  }
  public JDialog getJDialog()                { return _dialog.getJDialog(); }

  public ButtonScenario  getButtonScenario() {
     return _dialog.getButtonScenario();
  }

//======================================================================
//--------------------- Private / Protected Methods --------------------
//======================================================================

  private void addStuff() {
     JPanel topbox= SwingSupport.makeVerticalJPanelBox();
     _dialog.getContentPane().add( topbox, BorderLayout.CENTER);

    //---

     DataType extra[]= _fixedGroup.getExtraDataDefs();
     Assert.tst(extra != null);
     String keyName;
     String defTitle;
     String name;
     for(int i=0; i<extra.length; i++) {
        if (extra[i].getDataType().getSuperclass()==Number.class &&
            extra[i].getImportance()!= DataType.Importance.IGNORE &&
            extra[i].getKeyName() !=null) {

           keyName= extra[i].getKeyName();
           defTitle= extra[i].getDefaultTitle();
            name= Prop.getColumnName(keyName,defTitle);
            _dataField.addItem(name);
            _dataMap.put(name, keyName);
        }
     }

     _minRange.addChangeListener(this);
     _maxRange.addChangeListener(this);
     _minRange.setPaintLabels(true);
     _maxRange.setPaintLabels(true);
     _minRange.setBorder(new TitledBorder("Minimum") );
     _maxRange.setBorder(new TitledBorder("Maximum") );
     addActionListener(_dataField);

     _ltf.addComponent(_dataField, "Data Field: ",
                           "Data Field blah blah blah");

     topbox.add(_ltf);
     topbox.add(_minRange);
     topbox.add(_maxRange);
     topbox.add(_valueLabel);
     Dimension d =  topbox.getPreferredSize();
     d.setSize(d.width, d.height + 60);
     topbox.setMinimumSize(d);
     topbox.setPreferredSize(d);
     JComboBox direction = _ltf.addCombo(_dirAction,"direction");
     _typeOfAction= _dirAction.getSelectedInt();
     addDirListener(direction);
      //=================
      _feedbackLabel.setOpaque(true);
      _feedbackLabel.setBackground(Color.blue);
      _feedbackLabel.setFont(_feedbackLabel.getFont().deriveFont(22.0F));
      _feedbackDialog.add(_feedbackLabel);
      _feedbackDialog.setModal(true);
      _feedbackDialog.setUndecorated(true);
  }

   private void addDirListener(JComboBox cbox) {
      cbox.addActionListener( new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                  _typeOfAction= _dirAction.getSelectedInt();
                  updateSelected(false);
              }
        });
   }


   private void addActionListener(JComboBox c) {
      c.addActionListener( new ActionListener() {
              public void actionPerformed(ActionEvent ev) {
                  updateSelected(true);
              }
            } );
   }

   public void updateSelected(boolean fieldChange) {
      String selected= (String)_dataField.getSelectedItem();
       if (selected!=null) {
           String prop= _dataMap.get(selected);
           setupSlider(prop, fieldChange);
       }
   }

   private void sliderMoved(int minValue, int maxValue) {
      int max     = _maxRange.getMaximum();
      Data    data;
      boolean outValue  = true;
      boolean innerValue= true;
      String  outstr= "";
      if (_typeOfAction == SHOW_BETWEEN) {
          outValue  = false;
          innerValue= true;
          outstr    = SHOW;
      }
      else if (_typeOfAction == HIDE_BETWEEN) {
          outValue  = true;
          innerValue= false;
          outstr    = HIDE;
      }
      else {
          Assert.stop();
      }

      int i;
      _makingMods= true;
      _fixedGroup.beginBulkUpdate();

      for(i= 0; (i<minValue); i++) {
         data= _dataList.get(i);
         data.setAllFixedObjsEnabled(outValue);
      }
      if (minValue==-1)  minValue= 0;
      if (maxValue==max) maxValue--;
      for(i= minValue; (i<=maxValue); i++) {
         data= _dataList.get(i);
         data.setAllFixedObjsEnabled(innerValue);
         if (minValue != maxValue)
                  data.setAllFixedObjsEnabled(innerValue);
         else
                  data.setAllFixedObjsEnabled(outValue);
      }
      for(i= maxValue+1; (i<max); i++) {
         data= _dataList.get(i);
         data.setAllFixedObjsEnabled(outValue);
      }

      _fixedGroup.endBulkUpdate();
      _makingMods= false;
      Data minData= _dataList.get(minValue);
      Data maxData= _dataList.get(maxValue);
      if (minValue > -1 && maxValue < max) {
          _valueLabel.setText(outstr +
                              _dataField.getSelectedItem() +
                              BETWEEN +
                              getNumberAsString(minData.getNumber()) +
                              AND +
                              getNumberAsString(maxData.getNumber()) );
      }
      else if (minValue == -1 && maxValue < max) {
          _valueLabel.setText(outstr +
                              _dataField.getSelectedItem() +
                              LESS_THAN +
                              getNumberAsString(maxData.getNumber()) );
      }
      else if (minValue > -1 && maxValue == max) {
          _valueLabel.setText(outstr +
                              _dataField.getSelectedItem() +
                              GREATER_THAN +
                              getNumberAsString(minData.getNumber()) );
      }
      else if (minValue == -1 && maxValue == max) {
          _valueLabel.setText(HIDE_ALL_LABEL);
      }
      else {
          Assert.stop();
      }
   }



   private void setupSlider(String prop, boolean resort) {
      _mayAdjust= false;
       if (resort) {
           resortData(prop);
       }
//       if (resort) {
//           _dataList= new ArrayList<Data>( size );
//           for(Iterator i= _fixedGroup.iterator(); i.hasNext();) {
//               fixobj= (FixedObject)i.next();
//               findOrAdd(fixobj,prop);
//           }
//
//       }
      if (_dataList.size() > 1) {
         int max= _dataList.size() -1;
//         Collections.sort(_dataList);
         _minRange.setMaximum(max);
         _minRange.setMinimum(-1);
         _minRange.setValue(-1);
         _maxRange.setMaximum(max+1);
         _maxRange.setMinimum(0);
         _maxRange.setValue(max+1);

         Hashtable<Integer,JLabel> table= new Hashtable<Integer,JLabel>(11);
         table.put(new Integer(0),     makeLabel(0) );
         table.put(new Integer(max/2), makeLabel(max/2) );
         table.put(new Integer(max),   makeLabel(max) );
         _minRange.setLabelTable(table);
         _maxRange.setLabelTable(table);
         _mayAdjust= true;
         sliderMoved(_minRange.getValue(), _maxRange.getValue());
         _valueLabel.setVisible(true);
         _minRange.setVisible(true);
         _maxRange.setVisible(true);

          //sliderMoved(_minRange.getValue(), _maxRange.getValue());
      }
      else {
         _valueLabel.setVisible(false);
         _minRange.setVisible(false);
         _maxRange.setVisible(false);
      }
   }

  private JLabel makeLabel(int idx) {
     Data   d   = (Data)_dataList.get(idx);
     String str = getNumberAsString(d.getNumber() );
     return new JLabel(str);
  }

    // - original method
//    private void findOrAdd(FixedObject fixobj, String prop) {
//        Iterator i    = _dataList.iterator();
//        Number   n    = (Number)fixobj.getExtraData(prop);
//        boolean  found= false;
//        Data     data;
//        if (n != null) {
//            while (i.hasNext() && !found) {
//                data= (Data)i.next();
//                if ( n.equals(data.getNumber()) ) {
//                    found= true;
//                    data.addFixedObj(fixobj);
//                } // end if
//            } // end loop
//            if (!found) {
//                _dataList.add(new Data( n, fixobj) );
//            }
//        }
//    }



    private void resortData(final String prop) {
        _feedbackDialog.pack();
        _feedbackDialog.setLocationRelativeTo(getJDialog());
        Thread thread= new Thread(new Runnable()  {
            public void run() { resortDataInThread(prop); }
        });
        thread.start();
        _feedbackDialog.setVisible(true);

    }


    private void resortDataInThread(final String prop) {
        FixedObject fixobj;
        int size= _fixedGroup.size();
        List<FixedObject> sortedFG= _fixedGroup.createSortedView(prop);
        _dataList= new ArrayList<Data>( size );
        Number n;
        Object extraData;

        for(int i=0; (i<size); i++) {
            fixobj= sortedFG.get(i);
            extraData= fixobj.getExtraData(prop);
            if (extraData!=null && extraData instanceof Number) {
                n= ((Number)extraData).doubleValue();
            }
            else {
                n= null;
            }
            if (n!=null) {
                double fixObjValue= n.doubleValue();
                Data data= new Data( fixObjValue, fixobj);
                _dataList.add(data);
                boolean findMore= true;
                while (findMore && (i+1<size)) {
                    findMore= false;
                    fixobj= sortedFG.get(i+1);
                    n= ((Number)fixobj.getExtraData(prop)).doubleValue();
                    if (n!=null) {
                        if(fixObjValue==n.doubleValue()) {
                            i++;
                            data.addFixedObj(fixobj);
                            findMore= true;
                        }
                    }
                    else {
                        i++;
                    }
                } // end loop
            } // end if
        } // end loop

        SwingUtilities.invokeLater(new Runnable() {
            public void run() { _feedbackDialog.setVisible(false); }
        });
    }
    // - list sort method
//  private void findOrAdd(FixedObject fixobj, String prop) {
//      Number   n    = ((Number)fixobj.getExtraData(prop)).doubleValue();
//      boolean  found= false;
//      Data     data;
//      double fixObjValue;
//      double testValue;
//      int dataListSize= _dataList.size();
//      if (n != null) {
//          fixObjValue= n.doubleValue();
//          int i=0;
//          for (; (i<dataListSize && !found); i++) {
//              data= _dataList.get(i);
//              testValue= data.getNumber();
//              if ( fixObjValue==data.getNumber()) {
//                  found= true;
//                  data.addFixedObj(fixobj);
//              } // end if
//              else if ( fixObjValue<testValue) {
//                  _dataList.add(i, new Data( fixObjValue, fixobj));
//                  found= true;
//              }
//          } // end loop
//          if (dataListSize==i) {
//              _dataList.add(new Data( fixObjValue, fixobj) );
//              found= true;
//          }
//          Assert.tst(found);
//      }
//  }


    // - tree method
//    private void findOrAdd(FixedObject fixobj, String prop) {
//        Number   n    = ((Number)fixobj.getExtraData(prop)).doubleValue();
//        boolean  found= false;
//        Data     data;
//        double fixObjValue;
//        double testValue;
//        int dataListSize= _dataList.size();
//        if (n != null) {
//            fixObjValue= n.doubleValue();
//            int i=0;
//            int idx= dataListSize/2;
//            int lastIdx= 0;
//            if (dataListSize== 0) {
//                _dataList.add(new Data( fixObjValue, fixobj));
//                found= true;
//            }
//            int delta= -1;
//            while(!found) {
//                found= true;
//                lastIdx= idx;
//                data= _dataList.get(idx);
//                testValue= data.getNumber();
//                delta= (Math.abs(idx-lastIdx)+1) / 2;
//                if (delta==0) delta= 1;
//                if ( fixObjValue<testValue)  delta*= -1;
//
//                if ( fixObjValue==testValue) {
//                    data.addFixedObj(fixobj);
//                } // end if
//                else if ((delta>0 && (fixObjValue-testValue) < 0 ) ||
//                         (delta<0 && (fixObjValue-testValue) > 0) ) {
//                    _dataList.add(i, new Data( fixObjValue, fixobj));
//                }
//                else if (idx==0) {
//                    _dataList.add(0, new Data( fixObjValue, fixobj));
//                }
//                else if (idx==dataListSize) {
//                    _dataList.add(new Data( fixObjValue, fixobj));
//                }
//                else {
//                    found= false;
//                    idx+= delta;
//                }
//            }
//            Assert.tst(found);
//        }
//    }

  private String getNumberAsString(Number n) {
     String str = "";
     if      (n instanceof Double) str=  _nf.format(n.doubleValue() );
     else if (n instanceof Float)  str=  _nf.format(n.floatValue() );
     else if (n instanceof Long)   str=  n.longValue() + "";
     else if (n instanceof Integer)str=  n.intValue()  + "";
     else if (n instanceof Byte)   str=  n.byteValue() + "";
     else if (n instanceof Short)  str=  n.shortValue()+ "";
     else                          Assert.stop();
     return str;
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
//------------------------- Private Inner classes -------------------
//===================================================================
  private static class Data implements Comparable<Data> {
      private double      _number;
      private List<FixedObject> _fixobjList= new LinkedList<FixedObject>();

      public Data( double number, FixedObject fixobj) {
         _number= number;
         _fixobjList.add(fixobj);
      }

      public double      getNumber()      { return _number; }

      public void addFixedObj(FixedObject fixobj) { 
          _fixobjList.add(fixobj);
      }

      public void setAllFixedObjsEnabled(boolean enable) {
          for(FixedObject fo: _fixobjList) {
              fo.setEnabled(enable);
         }

      }

      public int compareTo(Data other) {
          return ComparisonUtil.doCompare(_number, other._number);
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
