package edu.caltech.ipac.planner.io;

import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JRadioButton;
import javax.swing.Box;
import edu.caltech.ipac.gui.RadioBox;
import edu.caltech.ipac.util.action.RadioAction;
import edu.caltech.ipac.util.action.RadioListener;
import edu.caltech.ipac.util.action.RadioEvent;
import edu.caltech.ipac.util.action.ClassProperties;
/**
* This is the class used to select the save all AOR or save selected AOR
* in Open File Dialog
* @see OpenFileAction
* @author Carlos Campos
*/

public class ModePanel extends    JPanel 
                       implements RadioListener {
    
    private static final ClassProperties _prop= 
                                         new ClassProperties(ModePanel.class);

    private boolean      _saveAllSelected     = true; 
    private boolean      _saveAORListSelected = true; 
    private RadioBox     _saveOrder           = null;
    private JRadioButton _saveAllAORs         = null;
    private JRadioButton _saveSelectedAORs    = null;
    
    
    ModePanel (){ 
        _saveOrder= new RadioBox(_prop.makeBase("saveOrder") );
        _saveOrder.addRadioAction(new RadioAction(_prop.makeBase("saveOrder")));

        RadioBox saveType = new RadioBox(_prop.makeBase("saveType")); 
        _saveAllAORs      = saveType.addRadio("saveAll");
        _saveSelectedAORs = saveType.addRadio("saveSelected");
        saveType.addRadioListener(this);
        
        RadioBox saveWhat = new RadioBox(_prop.makeBase("saveWhat"));
        saveWhat.addRadio("saveAor");
        saveWhat.addRadio("saveTarget");
        saveWhat.addRadioListener(this);
        
        setLayout( new BoxLayout(this,BoxLayout.X_AXIS) );
        
        add(Box.createGlue() );
        add(saveWhat);
        add(saveType);
        add(_saveOrder);
        add(Box.createGlue() );
    }
    
    public void radioChange(RadioEvent e){
        if ( e.getCommand().equals("saveSelected")){
            setSaveAll(false);
        }
        else if ( e.getCommand().equals("saveAll")){
            setSaveAll(true);
        }
        else if ( e.getCommand().equals("saveAor")){
            setSaveAORList(true);
            _saveAllAORs.setEnabled(true);
            _saveSelectedAORs.setEnabled(true);
            firePropertyChange("saveAor", null, "");
        }
        else if ( e.getCommand().equals("saveTarget")){
            setSaveAORList(false);
            _saveAllAORs.setEnabled(false);
            _saveSelectedAORs.setEnabled(false);
            firePropertyChange("saveTarget",null,"");
        }
    }
    
    private void setSaveAll(boolean a)    { _saveAllSelected     = a; }
    private void setSaveAORList(boolean a){ _saveAORListSelected = a; }

    public void setSaveInOriginalOrderSelected(boolean original){
        _saveOrder.setSelected( original ? "original" : "sorted" );
    }
    
    public boolean isSaveAllSelected()     { return _saveAllSelected; }
    public boolean isSaveAORListSelected() { return _saveAORListSelected; }

    public boolean isSaveInOriginalOrderSelected(){
        return _saveOrder.getSelectedCommand().equals("original");
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
