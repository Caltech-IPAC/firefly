package edu.caltech.ipac.firefly.ui;


import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.input.InputFieldGroup;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

import java.util.List;

public class ActiveTabPane extends Composite implements HasInputFieldsAndGroups {

    private TabPane tp;

    public ActiveTabPane() {
        tp = new TabPane();
        initWidget(tp);
        tp.addSelectionHandler(new SelectionHandler<Integer>() {
            public void onSelection(SelectionEvent<Integer> integerSelectionEvent) {
                ValueChangeEvent.fire(ActiveTabPane.this, 0);
            }
        });
    }

    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Integer> handler) {
        return addHandler(handler, ValueChangeEvent.getType());
    }

    public void setSize(String width, String height) {
        if (width!=null && height!=null) {
            tp.setSize(width, height);
            tp.forceLayout();
        }
    }

    public void setTabPaneName(String name) { tp.setTabPaneName(name); }

    public void addTab(Widget tabContent, String name) { tp.addTab(tabContent, name); }

    public void selectTab(int idx) { tp.selectTab(idx); }

    public WebEventManager getEventManager() { return tp.getEventManager(); }

    public TabPane getTabPane() { return tp; }


    public List<InputField> getFields() {
        TabPane.Tab sTab= tp.getSelectedTab();
        return FormUtil.getAllChildFields(sTab);
    }


    public List<InputFieldGroup> getGroups() {
        TabPane.Tab sTab= tp.getSelectedTab();
        return FormUtil.getAllChildGroups(sTab);
    }




    public void add(Widget w) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Widget w) {
        throw new UnsupportedOperationException();
    }

    private boolean isGroupHidden(InputFieldGroup group) {
        boolean retval= false;
        if (group instanceof Widget) {
            retval= !GwtUtil.isVisible(((Widget)group).getElement());
        }
        return retval;

    }

}

