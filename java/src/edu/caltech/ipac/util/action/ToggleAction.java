package edu.caltech.ipac.util.action;

import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.html.HtmlDocumentEntry;

import javax.swing.AbstractButton;
import java.awt.event.ActionEvent;

/**
 * An Action for creating check box or toggle buttons.
 * @author Trey Roby
 */
public class ToggleAction extends GeneralAction {

    private final boolean _saveStateAsPreference;

    /**
     * Create a ToggleAction.
     */
    public ToggleAction() { this(null, true); }

    /**
     * Create a ToggleAction.
     * @param command the command for this action
     */
    public ToggleAction(String command) { this(command,true); }

    /**
     * Create a ToggleAction.
     * @param  saveStateAsPreference save the state as a preference
     */
    public ToggleAction(boolean saveStateAsPreference) { 
        this(null,saveStateAsPreference);
    }

    /**
     * Create a ToggleAction.
     * @param command the command for this action
     * @param  saveStateAsPreference save the state as a preference
     */
    public ToggleAction(String  command, boolean saveStateAsPreference) { 
       super(command);
       _saveStateAsPreference= saveStateAsPreference;
    }

    /**
     * Call when a toggle button or check box button is pressed.
     * If you subclass this class you make sure you call 
     * <b>super.actionPerformed()</b>.
     * @param ev the event describing what happened.
     */
    public void actionPerformed(ActionEvent ev) {
       AbstractButton b=  (AbstractButton)ev.getSource();
       setSelected(b.isSelected());
    }

    public void document(HtmlDocumentEntry entry) {
       super.document(entry);
    }

   /**
    * Set a toggle button or check box button to true.
    * @param set value for the toggle button or check box
    * @see ToggleAction
    */
   public void setSelected(boolean set) {
      super.setSelected(set);
       if (_saveStateAsPreference) {
                AppProperties.setPreference(
                     getActionCommand()+ "."+ActionConst.SELECTED,  set+ "" );
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
