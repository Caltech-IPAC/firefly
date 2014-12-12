package edu.caltech.ipac.planner.io;

import edu.caltech.ipac.gui.BaseSaveAction;
import edu.caltech.ipac.gui.ExtensionFilter;
import edu.caltech.ipac.planner.Mode;
import edu.caltech.ipac.planner.PrimaryPlugin;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.action.Prop;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.awt.BorderLayout;


/**
 * Class used to save AOR List from SPOT to an ascii file.
 * Currently it works in two modes, Save Target list only or Save AOR list.
 * @author Carlos Campos, Xiuqin Wu
 */

public class SaveFileAction extends    BaseSaveAction
                            implements PropertyChangeListener {

    private static final String DIRECTORY_PROP = "sut.io.lastWorkingDirectory";
    private PrimaryPlugin    _plugin;
    private ModePanel        _mPanel;
    private boolean          _saveTargetsOnly= false;
    private FileFilter       _tgtFilter;
    private FileFilter       _aorFilter;
    private Mode             _mode;

    public SaveFileAction(JFrame f, PrimaryPlugin plugin, Mode mode) {
       super(f);
       _plugin = plugin;
       _mode   = mode;
    }

   protected JComponent makeAccessory() {
       _mPanel= new ModePanel();
       _mPanel.addPropertyChangeListener(this);
       JPanel accessory= new JPanel(new BorderLayout());
       accessory.add(_mPanel, BorderLayout.SOUTH);
       accessory.add(_plugin.getAorIO().makePreviewer(getFileChooser()),
                     BorderLayout.CENTER);
       return accessory;
   }


    public void propertyChange(PropertyChangeEvent ev){
        String command= getActionCommand();
	String propertyName = ev.getPropertyName();
        JFileChooser fileChooser= getFileChooser();
        if (propertyName.equals("saveAor")){

            setApproveButtonText(
                           Prop.getName(command + ".AorApproveButton") );
            _saveTargetsOnly= false;
            fileChooser.setFileFilter(_aorFilter);
        }
        else if (propertyName.equals("saveTarget")){
            setApproveButtonText(
                           Prop.getName(command + ".TargetApproveButton") );
            _saveTargetsOnly= true;
            fileChooser.setFileFilter(_tgtFilter);
        }
    }

    protected void postBuild() {
        String command= getActionCommand();
        setApproveButtonText( Prop.getName(command + ".AorApproveButton"));
    }

    //protected String getHelpButtonProperty() { return "SaveJFileChooser";}

    protected FileFilter[] getFilterArray() {
       FileFilter fAry[]= new FileFilter[2];
       String command= getActionCommand();
       String aorText= Prop.getName(command + ".aorFilter");
       String tgtText= Prop.getName(command + ".tgtFilter");
       _aorFilter= fAry[0]= new ExtensionFilter(FileUtil.AOR, aorText);
       _tgtFilter= fAry[1]= new ExtensionFilter(FileUtil.TGT, tgtText);
       return fAry;
    }

    protected String getDirProperty() { return DIRECTORY_PROP; } 

    protected File modifyFile(File f) {
        if (_saveTargetsOnly) {
           f = FileUtil.modifyFile(f,  FileUtil.TGT);
        }
        else {
           f = FileUtil.modifyFile(f, FileUtil.AOR);
        }
        return f;
    }

   /**
    * Save the file
    */
   protected void doSave(File f) throws Exception { 
        _plugin.getAorIO().doAorSave(_plugin, f, _saveTargetsOnly, 
                                     _mPanel.isSaveAllSelected(),
                                     _mPanel.isSaveInOriginalOrderSelected());
        if (!_saveTargetsOnly) { 
           _mode.setMode(Mode.PROPOSAL);
           _mode.setAorSource(f.getName());
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
