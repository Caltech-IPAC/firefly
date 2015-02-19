/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.input;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Anchor;
import edu.caltech.ipac.firefly.core.Preferences;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.FieldDef;
import edu.caltech.ipac.util.dd.PasswordFieldDef;
import edu.caltech.ipac.util.dd.ValidationException;

/**
 * @author loi
 *         $Id: HiddenField.java,v 1.1 2011/09/02 00:40:36 loi Exp $
 */
public class HiddenField extends InputField {


    private String value;
    private FieldDef fieldDef;
    private FieldLabel label;
    private Anchor holder;


    /**
     * This is the normal construct used for creating or subclassing TextBoxFieldWidget
     * @param def the fieldDef determines the type of field to create
     */
    public HiddenField(FieldDef def) {
        value = def.getDefaultValueAsString();
        fieldDef = def;
        label = new FieldLabel.Immutable() {
                    public String getHtml() {
                        return "";
                    }
                };
        holder = new Anchor("");
        holder.setVisible(false);
        initWidget(holder);
    }

//======================================================================
//----------------- Implementation of InputField -----------
//======================================================================


    public FieldDef getFieldDef() { return fieldDef; }

    public FieldLabel getFieldLabel() { return label; }


    public FocusWidget getFocusWidget() { return holder; }

    public String getValue() { return value; }

    public void setValue(String v) { value = v; }

    public boolean validate() { return true; }

    public boolean validateSoft() { return true; }

    public void reset() { setValue(fieldDef.getDefaultValueAsString()); }

    public void forceInvalid(String reason) {}

    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> h) {return null;}


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

}