package edu.caltech.ipac.util.action;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import java.util.EventObject;

/**
 * A RadioEvent is used to notify interested parties when a group of related 
 * radio buttons has changed.
 * @author Trey Roby
 */
public class RadioEvent extends EventObject {

    private AbstractButton _radioButton;

    /**
     * Create the Event object.
     * @param source The source of the event- a radio button, or a RadioBox.
     * @param button that was pushed.
     */
    public RadioEvent (Object source, AbstractButton button) {
        super(source);
        _radioButton= button;
    }

    /**
     * Get the command string of the current radio button.
     * @return String the command as a string
     */
    public String getCommand() { return _radioButton.getActionCommand(); }
    /**
     * Get the integer constant of the current radio button, 
     * if it is available.  If it is not available return -999.
     * @return int the integer constant assoicated with this button
     */

    public int    getIntConst()  { 
         ButtonModel  model= _radioButton.getModel();
         int retval= -999;
         if ( model instanceof RadioIntButtonModel) {
              retval= ((RadioIntButtonModel)model).getValue(); 
         }
         return  retval;
    }

    /**
     * Get the button that is the current radio button.
     * @return AbstractButton the current radio button.  It may be a 
     * JRadioButton or a JRadioButtonMenuItem depending on the source.
     */
    public AbstractButton getButton()  { return _radioButton; }
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
