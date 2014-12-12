package edu.caltech.ipac.util.action;

import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.dd.FieldDefSource;

import java.awt.event.ActionEvent;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * An element of a RadioAction.  This class will define a radio button
 * inside a group of radios.
 * @see RadioAction
 * @author Trey Roby
 */
public class RadioActionElement extends GeneralAction {

    RadioAction _radioAction;
    String      _actionString;

    /**
     * Create a element and pass a RadioAction to assign it to.
     * @param command the command string
     * @param radioAction assign the element to this RadioAction
     */
    public RadioActionElement(String command,
                              RadioAction radioAction,
                              Properties  alternatePdb)    {
          super(command,alternatePdb);
          Assert.tst(command);
          _radioAction= radioAction;
          _radioAction.addRadioElement(this);
          StringTokenizer st= new StringTokenizer(command,".");
          for(; (st.hasMoreElements()); _actionString= st.nextToken() );
          if (getIntValue() == ActionConst.INT_NULL) {
               try {
                  Integer i= new Integer(_actionString);
                  setIntValue(i);
               } catch (NumberFormatException e) {}
          }
    }


    /**
     * Create a element and pass a RadioAction to assign it to.
     * @param fds the FieldDefSource
     * @param radioAction assign the element to this RadioAction
     */
    public RadioActionElement(FieldDefSource fds,
                              RadioAction radioAction)    {
        super(fds);
        Assert.tst(fds.getName());
        _radioAction= radioAction;
        _radioAction.addRadioElement(this);
        if (fds.getName().startsWith(radioAction.getActionCommand())) {
            StringTokenizer st= new StringTokenizer(fds.getName(),".");
            for(; (st.hasMoreElements()); _actionString= st.nextToken() );
        }
        else {
            _actionString= fds.getName();
        }
        if (getIntValue() == ActionConst.INT_NULL) {
            try {
                Integer i= new Integer(_actionString);
                setIntValue(i);
            } catch (NumberFormatException e) {}
        }
    }




    final public void actionPerformed(ActionEvent e) {
         _radioAction.changeRadio(e,_actionString);
    }

    public String getRadioActionString() {
         return _actionString;
    }

    public String toString() {
         return getName();
    }


    @Override
    public boolean equals(Object other) {
        boolean retval= false;
        if (other==this) {
            retval= true;
        }
        else if (other!=null && other instanceof RadioActionElement) {
            RadioActionElement ia= (RadioActionElement)other;
            if (ComparisonUtil.equals(_actionString, ia._actionString) &&
                getIntValue() == ia.getIntValue()) {
                retval= true;
            }
        }
        return retval;
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
