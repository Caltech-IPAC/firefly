package edu.caltech.ipac.firefly.ui;


import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.input.InputFieldGroup;
import edu.caltech.ipac.firefly.ui.panels.CollapsiblePanel;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class ActiveCollapsiblePanel extends Composite implements InputFieldGroup, UsesFormHub {

    private CollapsiblePanel cp;
    private FormHub formHub;

    public ActiveCollapsiblePanel(String title, Widget content, boolean isOpen) {
        cp = new CollapsiblePanel(title, content, isOpen);
        initWidget(cp);
    }

    public void setVisible(boolean show) {
        cp.setVisible(show);
    }

    public WebEventManager getEventManager() {
        return cp.getEventManager();
    }

    public String getPanelName() {
        String panelName = cp.getPanelName();
        if (StringUtils.isEmpty(panelName)) {
            return "";
        } else {
            return panelName;
        }
    }
    public void setPanelName(String panelName) {
        cp.setPanelName(panelName);
    }

    public List<Param> getFieldValues() {
        List<Param> fieldValues = new ArrayList<Param>();

        if (cp.isVisible()) {
            List<InputField> allFields = Form.searchForFields(cp);
            for (InputField inF : allFields) {
                if (!GwtUtil.isHidden(inF.getElement())) {
                    String key = inF.getName();
                    String val = inF.getValue();

                    fieldValues.add(new Param(key, val));
                }
            }

            List<InputFieldGroup> groups = new ArrayList<InputFieldGroup>();
            FormUtil.getAllChildGroups(cp, groups);
            for (InputFieldGroup ifG : groups) {
                List<Param> pL = ifG.getFieldValues();
                for (Param _p : pL) {
                    fieldValues.add(_p);
                }
            }
        }

        return fieldValues;
    }

    public void setFieldValues(List<Param> list) {
    }

    public boolean validate() {
        boolean validated = true;
        if (cp.isVisible()) {
            List<InputField> allFields = Form.searchForFields(cp);
            for (InputField inF : allFields) {
                if (!GwtUtil.isHidden(inF.getElement())) {
                    validated = validated && inF.validate();
                }
            }

            List<InputFieldGroup> groups = new ArrayList<InputFieldGroup>();
            FormUtil.getAllChildGroups(cp, groups);
            for (InputFieldGroup ifG : groups) {
                validated = validated && ifG.validate();
            }
        }
        return validated;
    }


    public void add(Widget w) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public Iterator<Widget> iterator() {
        return cp.iterator();
    }

    public boolean remove(Widget w) {
        throw new UnsupportedOperationException();
    }

    public void bind(FormHub hub) {
        formHub = hub;
    }

    public FormHub getFormHub() {
        return formHub;
    }
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
