package edu.caltech.ipac.planner.io;

import edu.caltech.ipac.gui.BaseOpenAction;
import edu.caltech.ipac.gui.CommandTable;
import edu.caltech.ipac.gui.DialogSupport;
import edu.caltech.ipac.gui.ExtensionFilter;
import edu.caltech.ipac.gui.OptionPaneWrap;
import edu.caltech.ipac.gui.RadioBox;
import edu.caltech.ipac.gui.ErrorReporter;
import edu.caltech.ipac.planner.AORList;
import edu.caltech.ipac.planner.AotFactory;
import edu.caltech.ipac.planner.ConstraintListInterface;
import edu.caltech.ipac.planner.Mode;
import edu.caltech.ipac.targetgui.TargetList;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.FileReadStatusException;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.action.ClassProperties;
import edu.caltech.ipac.util.action.Prop;
import edu.caltech.ipac.util.action.RadioAction;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.awt.BorderLayout;

/**
 * Reads AORs to file.  This class also handles determines how to handle
 * errors found in AOR files based on user settings.
 * Fuctions as a caller to Target read, Instrument read, Resources read, and
 * Flux Density read.
 * Handles Error exceptions comming from the Parser and the Validator.
 * Determines wether to overwrite existing AOR objects in spot with AOR objects
 * from the AOR file, or to allow duplicate object with different label, or to
 * Ignore file AOR if there is a duplicate AOR label in SPOT.
 * Handles the number of maximum errors allowed. Currently 50
 *
 * @author Carlos Campos, Xiuqin Wu, Michael Nguyen
 */

public class OpenFileAction extends    BaseOpenAction
                            implements PropertyChangeListener {

    private static final ClassProperties _openProp= 
                          new ClassProperties(OpenFileAction.class);
    public static final int AOR         = 999;
    public static final int TARGET_ONLY = 998;
    private static final String DIRECTORY_PROP = "sut.io.lastWorkingDirectory";
    private static final String READ_PROGRAM_ERROR =
            "You need to clean your current AORs before you read in a program";

    private AORList                 _aors;
    private AotFactory              _aotFactory;
    private TargetList              _targets;
    private Mode                    _mode;
    private ConstraintListInterface _cons;
    private boolean                 _readyToOpenFile = true;
    private AorIO                   _aorIO;
    private RadioBox                _mPanel;

    public OpenFileAction(JFrame                  f,
                          AorIO                   aorIO,
                          AORList                 aors,
                          AotFactory              aotFactory,
                          ConstraintListInterface cons,
                          TargetList              targets,
                          Mode                    mode) {
        super(f);
        _aors       = aors;
        _targets    = targets;
        _cons       = cons;
        _aotFactory = aotFactory;
        _mode       = mode;
        _aorIO      = aorIO;

    }

    protected FileFilter[] getFilterArray() {
       FileFilter fAry[]= new FileFilter[2];
       String command= getActionCommand();
       String aorText= Prop.getName(command + ".aorFilter");
       String tgtText= Prop.getName(command + ".tgtFilter");
       fAry[0]= new ExtensionFilter(FileUtil.AOR, aorText);
       fAry[1]= new ExtensionFilter(FileUtil.TGT, tgtText);
       return fAry;
    }

    protected String getDirProperty() { return DIRECTORY_PROP; } 

    protected boolean isSafeToOpen() {
      if (!_readyToOpenFile) {
          OptionPaneWrap.showError(getFrame(),
            "Please close the constraints editor tool\nbefore opening files.",
            "Error" );
      }
      return _readyToOpenFile;
    }

    protected void postBuild() {
        CommandTable com= CommandTable.getInstance();
        Action constraintEditorAction = com.findCommand("constraints");
        if (constraintEditorAction != null)
            constraintEditorAction.addPropertyChangeListener(this);
    }


   protected JComponent makeAccessory() {
       _mPanel= new RadioBox( _openProp.makeBase("options") );
       _mPanel.addRadioAction(new RadioAction(_openProp.makeBase("options") ));

       JPanel accessory= new JPanel(new BorderLayout());

       accessory.add(_mPanel, BorderLayout.SOUTH);
       accessory.add(_aorIO.makePreviewer(getFileChooser()),
                     BorderLayout.CENTER);
       return accessory;
   }

   /**
    * Read the file
    */
   public void doOpen(File f) throws IOException { 
      try {
            if (f.getName().endsWith("." + FileUtil.AOR)) {
                boolean saveBefore = _aors.isSaved();
                DialogSupport.setWaitCursor(true);
                _aors.beginBulkUpdate();
                _targets.beginBulkUpdate();
                read(f, AOR);
                _targets.endBulkUpdate();
                _aors.endBulkUpdate();
                DialogSupport.setWaitCursor(false);
                if (saveBefore) _aors.setSaved(true);
            }
            else if (f.getName().endsWith("." + FileUtil.TGT)) {
                DialogSupport.setWaitCursor(true);
                _targets.beginBulkUpdate();
                read(f, TARGET_ONLY);
                _targets.endBulkUpdate();
                DialogSupport.setWaitCursor(false);
            }
            else {
                throw new IOException("File extension not supported");
            }
     }catch (IOException ioe) {
        _targets.endBulkUpdate();
        _aors.endBulkUpdate();
        DialogSupport.setWaitCursor(false);
        throw ioe;
     }finally {
        DialogSupport.setWaitCursor(false);
     }
   }


    /**
     * Read all the AORS from file and add them to the AORList Object
     */
    public void read(File file, int type) throws IOException {

        //StringBuffer constraints_deleted_in_overwrite= new StringBuffer();

        String                  errorMessage = null;
        List                    newTargets   = null;
        boolean                 errorsFound  = false;
        AorIO.AorReadResults    results      = null;
        FileReadStatusException statusE      = null;



 
        int mode= AORListBuilder.OPEN_DISCARD;
        if (_mPanel != null) mode= _mPanel.getSelectedInt();

        AORListBuilder listBuilder = new AORListBuilder(_aors, _cons, 
                                           _aotFactory, _targets, mode);
        
                   // 1. Do the read from the file
        if (type == AOR) {
            results= _aorIO.doAorRead(file);
            statusE= results.getException();
        }
        else if (type == TARGET_ONLY) {
            AorIO.TargetReadResults tres = _aorIO.doTargetOnlyRead(file);
            newTargets= tres.getTargetList();
            statusE=    tres.getException();
        }
        else {
            Assert.tst(false, "type should only be AOR or TARGET_ONLY");
        }

                   // 2. Check for Errors
        if (statusE!= null) {
            errorsFound= 
                  (statusE.getSeverity() == FileReadStatusException.ERROR);
            errorMessage = statusE.getMessage();
        }


                   // 3. respond to Errors and update the data to all the list
        boolean updated = false;
        if (errorsFound) {
            boolean doUpdate=  ErrorReporter.showError(
                           getFrame(), "Error(s) Found", errorMessage, true);
            //ErrorReporter errorReporter =
            //        new ErrorReporter(_f, "Error(s) Found", errorMessage);
            //errorReporter.init();
            //errorReporter.show();
            errorMessage= null;

            if (doUpdate) {
                updated= updateList(type,file,listBuilder, newTargets,results);
            }
        }
        else {
            updated= updateList(type,file,listBuilder, newTargets, results);
        }

                   // 4. Show user any warnings
        if (updated && errorMessage != null) {
            ErrorReporter.showError( getFrame(), "Error(s) Found",errorMessage);
//            ErrorReporter errorReporter = new ErrorReporter(_f, "Warning",
//                                                        errorMessage, true);
//            errorReporter.init();
//            errorReporter.show();
        }

        //This is to inform the user about the Constraints
        // that got deleted while loading files
        if (mode != AORListBuilder.ALLOW_DUPLICATE) {
           String deletedConstraints= listBuilder.getDeletedConstraint();
           if (deletedConstraints.trim().length() > 0) {

               ErrorReporter.showError( getFrame(), "Warning",
                         "Warning - "+
                         "The following constraints were deleted/modified: ");
//               ErrorReporter errorReporter = new ErrorReporter(
//                   _f, "Warning",
//                   "Warning - The following constraints were deleted/modified: "
//                   + deletedConstraints, true);
//               errorReporter.init();
//               errorReporter.show();
           }
        }

    }



    private boolean updateList(int                  type,
                               File                 file,
                               AORListBuilder       listBuilder,
                               List                 newTargets,
                               AorIO.AorReadResults results) {
        boolean updated= false;
        if (type == TARGET_ONLY)
            listBuilder.updateTargetListOnly(newTargets);
        else if (type == AOR) {
            updated = updateAORListAndTargetList(
                                       file, listBuilder, results);
            listBuilder.updateConstraintList(
                                       results.getConstraintList());
        }
        else {
            Assert.tst(false, "type should only be AOR or TARGET_ONLY");
        }
        return updated;
    }

    /**
     * Here we tell users that they are reading in a
     * PROGRAM and they need to clean out the existing AORs in SPOT before we
     * can update the AORList. <br>
     * If they are reading ina proposal, the SPOT mode stays whatever it was
     * before the reading
     */
    private boolean updateAORListAndTargetList(File                 file,
                                               AORListBuilder       listBuilder,
                                               AorIO.AorReadResults results) {
        boolean updated = false;
        List    newreqs    = results.getRequestList();
        List    newcons    = results.getConstraintList();
        if (newreqs != null && !newreqs.isEmpty()) {
            updated = true;
            if (results.isProposal()) {
                listBuilder.updateAORListAndTargetList(newreqs, newcons);
                if (_mode.getMode() != Mode.PROGRAM)
                    _mode.setAorSource(file.getName());
            }
            else {  // program
                if (_aors.getNumAORs() == 0) {
                    try {
                        _mode.setMode(Mode.PROGRAM);
                    } catch (Exception e) { } // no need to do anything
                    listBuilder.updateAORListAndTargetList(newreqs, newcons);
                    _mode.setAorSource(file.getName());
                } // _aors empty
                else {  // not empty
                    updated = false;
                    OptionPaneWrap.showError(getFrame(), READ_PROGRAM_ERROR,
                                             "Error");
                }
            } // end of program
        }
        return updated;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("dialog up"))
            _readyToOpenFile= false;
        else if (evt.getPropertyName().equals("dialog down"))
            _readyToOpenFile= true;
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
