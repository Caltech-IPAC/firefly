package edu.caltech.ipac.firefly.ui.input;

import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FocusWidget;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.util.dd.FieldDef;


/**
 * @author Trey Roby
 * $Id: InputField.java,v 1.10 2011/09/02 00:23:09 loi Exp $
 *
 * @see edu.caltech.ipac.util.dd.FieldDef
 * @see FieldLabel
 * @see com.google.gwt.user.client.ui.FocusWidget
 */
public abstract class InputField extends Composite
                                 implements HasValueChangeHandlers<String> {

    private InputFieldContainer _container= null;
    private boolean _fireOnKeystroke= false;
    private InputField _decorator= null;

    /**
     * Get the field def
     * @return the FieldDef
     */
    public abstract FieldDef getFieldDef();

    /**
     * Make a FieldLabel for this field
     * @return the FieldLabel object
     */
    public abstract FieldLabel getFieldLabel();

    /**
     * the internal view of the InputField.  This is the widget that the user actually interacts with. It might
     * not be the same as the InputField.  It could be contained inside the InputField
     * @return a FocusWidget
     */
    public abstract FocusWidget getFocusWidget();

    /**
     * put the default value back into the field
     */
    public abstract void reset();
    /**
     * validate the field
     * @return true, if valid; false, if invalid
     */
    public abstract boolean validate();

    /**
     * Do a soft validation.  If the field has invalid input but as the use continues to type it could good then
     * mark it as valid.
     * @return true, if might be valid; false, if invalid
     */
    public boolean validateSoft() { return validate(); };

    /**
     * Force the field into an invalid state.
     * @param errorText the string that the use should see
     */
    public abstract void forceInvalid(String errorText);

    /**
     * Get the value of this field as a string
     * @return a string representation of the value
     */
    public abstract String getValue();
    /**
     * Set the Value of this field as a string, the string will be converted to what ever is appropriate.
     * Setting a value that cannot be converted is undefined.
     * @param v the value as a string
     */
    public abstract void setValue(String v);

    /**
     * add a change listener
     * @param h the listener
     * @return the HandlerRegistration used to remove the handler
     */
    public abstract HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> h);


    public void setFireValueChangeOnKeystroke(boolean fireOnKeystroke) {
        _fireOnKeystroke= fireOnKeystroke;
    }

    public boolean getFireValueChangeOnKeystroke() { return _fireOnKeystroke; }



    /**
     * if the results of getValue() is a number
     * @return true if getValue() can be converted to a number
     */
    public final boolean isNumber() {
        boolean retval= false;
        String v= getValue();
        if (v!=null) {
            try {
                Double.parseDouble(v);
                retval= true;
            } catch (NumberFormatException e) {
                retval= false;
            }
        }
        return retval;
    }

    /**
     * This method will return the results of getValue() as a number. If the Number cannot be
     * converted then a NumberFormatException will be thrown
     * @return the results of getValue() as a number
     */
    public final Number getNumberValue() {
        return Double.valueOf(getValue());
    }

    public final String getId() { return getFieldDef().getId(); }
    public final String getName() { return getFieldDef().getName(); }
    public final Param getParam() { return new Param(getFieldDef().getName(),getValue()); }

    public final void setContainer(InputFieldContainer w) {_container= w;}
    public final InputFieldContainer getContainer() {return _container;}

    public final void setDecorator(InputField decorator) { _decorator= decorator; }
    public final InputField getDecorator() { return _decorator; }
}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313)
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
