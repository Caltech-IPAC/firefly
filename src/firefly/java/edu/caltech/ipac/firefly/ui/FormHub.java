/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui;

import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.ui.input.FieldLabel;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.input.InputFieldContainer;
import edu.caltech.ipac.firefly.ui.input.InputFieldGroup;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.ui.panels.CollapsiblePanel;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * User: roby
 * Date: Aug 13, 2010
 * Time: 12:45:19 PM
 */


/**
 * @author Trey Roby
 */
public class FormHub {

    public static final Name FIELD_VALUE_CHANGE =
            new Name("FieldValueChange",
                    "This event happens when a InputField changes, " +
                            "Data Should be a Param");


    public static final Name FORM_REINIT =
            new Name("FormReinit",
                    "This happens every time a widget is added to the form, it might happen mulitple times " +
                            "though in practicality it only gets call once or twice. " +
                            "Data is null");

    public static final Name FORM_VALIDATION =
            new Name("FormValidation",
                    "This event is call when Form.validate is called " +
                            "Data is a FormHub.Validated object.  " +
                            "If the data is not valid then call Validated.invalidate");

    public static final Name ON_SHOW =
            new Name("OnShow",
                    "This event is called when Form is shown.");

    public static final Name ON_TAB_SELECTED = TabPane.TAB_SELECTED;
    public static final Name ON_TAB_ADDED = TabPane.TAB_ADDED;

    public static final Name ON_PANEL_OPENED = CollapsiblePanel.PANEL_OPENED;
    public static final Name ON_PANEL_CLOSED = CollapsiblePanel.PANEL_CLOSED;

    private final Form form;
//    private Form dlForm = null;
    private Map<String, TabPane> tabs = new HashMap<String, TabPane>();
    private Map<String, ActiveTabPane> atabs = new HashMap<String, ActiveTabPane>();
    private Map<String, CollapsiblePanel> collapsePanels = new HashMap<String, CollapsiblePanel>();
    private Map<String, ActiveCollapsiblePanel> actCollapsePanels = new HashMap<String, ActiveCollapsiblePanel>();
    private EventBridge eventBridge = new EventBridge();


    public FormHub(Form form) {
        this.form = form;
    }

    public Form getForm() {
        return form;
    }

    private final WebEventManager _eventManager = new WebEventManager();

    public WebEventManager getEventManager() {
        return _eventManager;
    }

    public List<String> getFieldIds() {
        return form.getFieldIds();
    }

    public void bind(TabPane tPane, String id) {
        if (!tabs.containsKey(id)) {
            tabs.put(id, tPane);
            tPane.getEventManager().addListener(ON_TAB_SELECTED, eventBridge);
            tPane.getEventManager().addListener(ON_TAB_ADDED, eventBridge);
        }
    }

    public TabPane getTabPane(String id) {
        return tabs.get(id);
    }

    public void unbind(TabPane tPane) {
        tabs.remove(tPane);
    }

    public void bind(ActiveTabPane atPane, String id) {
        if (!atabs.containsKey(id)) {
            atabs.put(id, atPane);
            atPane.getEventManager().addListener(ON_TAB_SELECTED, eventBridge);
            atPane.getEventManager().addListener(ON_TAB_ADDED, eventBridge);
        }
    }

    public ActiveTabPane getActiveTabPane(String id) {
        return atabs.get(id);
    }

    public void unbind(ActiveTabPane atPane) {
        atabs.remove(atPane);
    }

    public void bind(CollapsiblePanel cPanel, String id) {
        if (!collapsePanels.containsKey(id)) {
            collapsePanels.put(id, cPanel);
            cPanel.getEventManager().addListener(ON_PANEL_OPENED, eventBridge);
            cPanel.getEventManager().addListener(ON_PANEL_CLOSED, eventBridge);
        }
    }

    public CollapsiblePanel getCollapsiblePanel(String id) {
        return collapsePanels.get(id);
    }

    public void bind(ActiveCollapsiblePanel acPanel, String id) {
        if (!actCollapsePanels.containsKey(id)) {
            actCollapsePanels.put(id, acPanel);
            acPanel.getEventManager().addListener(ON_PANEL_OPENED, eventBridge);
            acPanel.getEventManager().addListener(ON_PANEL_CLOSED, eventBridge);
        }
    }

    public ActiveCollapsiblePanel getActiveCollapsiblePanel(String id) {
        return actCollapsePanels.get(id);
    }

    public void setCollapsiblePanelVisibility(String item, boolean show) {
        if (collapsePanels.containsKey(item)) {
            collapsePanels.get(item).setVisible(show);
            
        } else if (actCollapsePanels.containsKey(item)) {
            actCollapsePanels.get(item).setVisible(show);
        }
    }

    public List<String> getFieldNames() {
        return form.getFieldIds();
    }

    public List<String> getAllFieldNames() {
        List<String> ifNames = new ArrayList<String>();

        List<InputField> ifList = new ArrayList<InputField>();
        FormUtil.getAllChildFields(form, ifList);
        for (InputField inF : ifList) {
            ifNames.add(inF.getName());
        }

        List<InputFieldGroup> ifgList = new ArrayList<InputFieldGroup>();
        FormUtil.getAllChildGroups(form, ifgList);
        for (InputFieldGroup inG : ifgList) {
            List<Param> pList = inG.getFieldValues();
            for (Param p : pList) {
                ifNames.add(p.getName());
            }
        }

        return ifNames;
    }

    public boolean containsField(String fname) {
        return form.containsField(fname);
    }

    public void setValue(Param param) {
        form.setValue(param.getName(), param.getValue());
    }

    public String getValue(String name) {
        return form.getValue(name);
    }

    public boolean setVisible(String name, boolean visible) {
        InputField f = form.getField(name, false);
        if (f != null) {
            f.setVisible(visible);
            FieldLabel label = f.getFieldLabel();
            if (label instanceof FieldLabel.Mutable) {
                ((FieldLabel.Mutable) label).setVisible(visible);
                return true;
            } else {
                InputFieldContainer c = f.getContainer();
                if (c != null) {
                    c.setVisible(visible);
                    return true;
                }
            }
        }
        return false;
    }

    public void setHidden(String name, boolean isHidden) {
        InputField f = form.getField(name, false);
        if (f != null) {
            GwtUtil.setHidden(f, isHidden);
            FieldLabel label = f.getFieldLabel();
            if (label instanceof FieldLabel.Mutable) {
                GwtUtil.setHidden(((FieldLabel.Mutable) label).getWidget(), isHidden);
            } else {
                InputFieldContainer c = f.getContainer();
                if (c != null) {
                    GwtUtil.setHidden(c.getWidget(), isHidden);
                }

            }
        }
    }

    public void tabSelected() {
        fireReinitEvent();
    }

    void fireChangeEvent(Param param) {
        getEventManager().fireEvent(new WebEvent<Param>(this, FIELD_VALUE_CHANGE, param));
    }

    void fireReinitEvent() {
        getEventManager().fireEvent(new WebEvent<Object>(this, FORM_REINIT, null));
    }

    void fireValidate(Validated v) {
        getEventManager().fireEvent(new WebEvent<Validated>(this, FORM_VALIDATION, v));
    }

    void fireTabSelectedEvent(TabPane tp) {
        getEventManager().fireEvent(new WebEvent<TabPane>(this, ON_TAB_SELECTED, tp));
    }

    public void onShow() {
        getEventManager().fireEvent(new WebEvent<Object>(this, ON_SHOW, null));
    }


    class EventBridge implements WebEventListener {
        public void eventNotify(WebEvent ev) {
            if (ev.getName().equals(TabPane.TAB_SELECTED)) {
                getEventManager().fireEvent(new WebEvent(ev.getSource(), ON_TAB_SELECTED, ev.getData()));
            } else if (ev.getName().equals(TabPane.TAB_ADDED)) {
                getEventManager().fireEvent(new WebEvent(ev.getSource(), ON_TAB_ADDED, ev.getData()));
            } else if (ev.getName().equals(ON_PANEL_OPENED)) {
                getEventManager().fireEvent(new WebEvent(ev.getSource(), ON_PANEL_OPENED, ev.getData()));
            } else if (ev.getName().equals(ON_PANEL_CLOSED)) {
                getEventManager().fireEvent(new WebEvent(ev.getSource(), ON_PANEL_CLOSED, ev.getData()));
            }
        }
    }

    public static class Validated {
        private boolean valid;
        private String msg;

        public Validated() {
            this(true);
        }

        public Validated(boolean isValid) {
            this.valid = isValid;
            this.msg = null;
        }

        public boolean isValid() {
            return valid;
        }

        public void invalidate(String msg) {
            this.msg = msg;
            valid = false;
        }

        public String getMessage() {
            return msg;
        }
    }

}

