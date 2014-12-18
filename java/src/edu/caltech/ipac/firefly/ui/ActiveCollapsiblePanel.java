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

