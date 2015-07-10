/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ButtonBase;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.HelpManager;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.input.AsyncInputFieldGroup;
import edu.caltech.ipac.firefly.ui.input.HasSubmitField;
import edu.caltech.ipac.firefly.ui.input.HiddenField;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.input.InputFieldGroup;
import edu.caltech.ipac.firefly.util.AsyncCallbackGroup;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.FieldDef;
import edu.caltech.ipac.util.dd.StringFieldDef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 *
 * @author loi
 * @version $Id: Form.java,v 1.41 2012/10/20 01:05:49 tlau Exp $
 */
public class Form extends Composite implements HasWidgets {

    private Map<String, InputField> fields= null;          // lazy-loading.. do not access directly.. use getFieldsMap()
    private List<InputFieldGroup> groups= null;            // lazy-loading.. do not access directly.. use getFieldGroups()
    private Map<String, InputField> ungroupedFields= null; // lazy-loading.. do not access directly.. use getUngroupedFieldsMap()
    private ButtonBar buttonBar;
    private VerticalPanel form = new VerticalPanel();
    private FormHub hub= new FormHub(this);
    private FieldValChangeHandler fieldVCHandler = new FieldValChangeHandler();
    private ContainerValChangeHandler containerHandler = new ContainerValChangeHandler();
    private List<HandlerRegistration>  handlerRegList= new ArrayList<HandlerRegistration>(40);

    private ButtonBase submitButton;
    private boolean isListenersAdded = false;

    public Form () {
        this(true);
    }

    public Form(boolean addComponentBackground) {

        form.addStyleName("input-form-layout");
        //form.addStyleName("standard-border");
        if (addComponentBackground) form.addStyleName("component-background");
        buttonBar = new ButtonBar();
        buttonBar.setVisible(false);
        buttonBar.addStyleName("button-bar");
        //buttonBar.addStyleName("standard-border");

        VerticalPanel mainLayoutPanel = new VerticalPanel();
        mainLayoutPanel.setSpacing(3);
        mainLayoutPanel.addStyleName("input-form");

        mainLayoutPanel.add(form);
        mainLayoutPanel.add(buttonBar);

        //initWidget(GwtUtil.centerAlign(mainLayoutPanel));
        initWidget(mainLayoutPanel);
    }

    @Override
    protected void onLoad() {
        super.onLoad();
        hub.onShow();
    }

    public void setHub(FormHub hub) {
        this.hub = hub;
    }

    public FormHub getHub() { return hub; }

    public void setHelpId(String helpId) {
        if (helpId == null) {
            buttonBar.getHelpIcon().setVisible(false);
        } else {
            if (!buttonBar.getHelpIcon().isVisible()) {
                buttonBar.getHelpIcon().setVisible(true);
            }
            buttonBar.getHelpIcon().setHelpId(helpId);
        }
    }

    public int getFieldCount() {
        return getFieldsMap().size();
    }

    public ButtonBase getSubmitButton() {
        return submitButton;
    }

    public List<String> getFieldNames() {
        Map<String, InputField> fs = getFieldsMap();
        ArrayList<String> names = new ArrayList<String>(getFieldsMap().size());
        for(InputField f : fs.values()) {
            names.add(f.getName());
        }
        return names;
    }

    public List<String> getFieldIds() {
        return new ArrayList<String>(getFieldsMap().keySet());
    }

    public void reset() {
        for(InputField f : getFieldsMap().values()) {
            f.reset();
        }
    }

    /**
     * Validate all fields
     * @return true if all fields are valid, false otherwise
     */
    public boolean validate() {
        return validated().isValid();
    }

    public FormHub.Validated validated() {
        FormHub.Validated validated= new FormHub.Validated();

        for(InputField f : getUngroupedFieldsMap().values()) {
            if (!GwtUtil.isHidden(f.getElement()) && !f.validate()) {
                validated.invalidate(null);
            }
        }
        for (InputFieldGroup g : getFieldGroups()) {
            if (!g.validate()) {
                validated.invalidate(null);
            }
        }
        if (validated.isValid()) {
            hub.fireValidate(validated);
        }

        return validated;
    }

    public void populateRequest(Request req) {
        populateRequest(req, null);
    }

    public void populateRequest(Request req, AsyncCallback<String> callback) {
        AsyncCallbackGroup callbackGroup = null;
        boolean hasAsyncU= false;
        boolean hasAsyncG= false;
        if (callback!=null)callbackGroup= new AsyncCallbackGroup(callback);
        try {
            hasAsyncU= populateRequestFromUngroupedFields(req, callbackGroup);
            hasAsyncG= populateRequestFromFieldGroups(req, callbackGroup);
        } finally {
            if (!hasAsyncU && !hasAsyncG) {
                // no async needed.. call onSuccess immediately.
                if (callback != null) {
                    callback.onSuccess("ok");
                }
            }
        }
    }


    private boolean populateRequestFromUngroupedFields(final Request req, AsyncCallbackGroup cbGroup) {
        boolean hasAsync= false;
        for (final InputField f : getUngroupedFieldsMap().values()) {
            if (f!=null && isFieldVisible(f)) {
                if (f instanceof HasSubmitField && cbGroup!=null) {
                    hasAsync= true;
                    AsyncCallback<String> cb = new AsyncCallback<String>() {
                        public void onFailure(Throwable caught) {/* do nothing */}
                        public void onSuccess(String result) {
                            if (!StringUtils.isEmpty(result)) {
                                req.setParam(f.getName(), result);
                            }
                        }
                    };
                    ((HasSubmitField) f).submit(cbGroup.newCallback(cb));
                }
                else {
                    String v = f.getValue();
                    if (!StringUtils.isEmpty(v)) {
                        req.setParam(f.getName(), v);
                    }
                }
            }
        }
        return hasAsync;
    }

    private boolean populateRequestFromFieldGroups(final Request req, AsyncCallbackGroup cbGroup) {
        // groups set parameters themselves
        boolean hasAsync= false;
        for (InputFieldGroup g : getFieldGroups()) {
            if (cbGroup!=null && g instanceof AsyncInputFieldGroup &&
                ((AsyncInputFieldGroup)g).isAsyncCallRequired() ) {
                hasAsync= true;
                AsyncCallback<List<Param>> cb = new AsyncCallback<List<Param>>() {
                    public void onFailure(Throwable caught) {/* do nothing */}
                    public void onSuccess(List<Param> params) {
                        for(Param param : params) req.setParam(param);
                    }
                };
                ((AsyncInputFieldGroup)g).getFieldValuesAsync(cbGroup.newCallback(cb));
            }
            else {
                for (Param param : g.getFieldValues()) {
                    req.setParam(param);
                }
            }
        }
        return hasAsync;
    }

    public void populateFields(Request req) {
        for(String k : getUngroupedFieldsMap().keySet()) {
            InputField f = getField(k);
            if (f != null && isFieldVisible(f)) {
                if (req.containsParam(f.getName())) {
                    String v = req.getParam(f.getName());
                    f.setValue(v);
                } else {
                    if (!req.isDoSearch()) {
                        f.reset();
                    }
                }
            }
        }
        // groups handle field setting themselves
        for (InputFieldGroup g : getFieldGroups()) {
            ArrayList<Param> list = new ArrayList<Param>();
            for(Param p : req.getParams()) {
                list.add(new Param(p.getName(), p.getValue()));
            }
            g.setFieldValues(list);
        }

    }

    public void setFocus(final String id) {
        DeferredCommand.addCommand(new Command(){
            public void execute() {
                InputField f = getField(id);
                if (f != null) {
                    FocusWidget fw = f.getFocusWidget();
                    if (fw != null) {
                        fw.setFocus(true);
                    }
                }
            }
        });
    }

    public ButtonBar getButtonBar() {
        return buttonBar;
    }
    
    public void addButton(ButtonBase button) {
        buttonBar.addLeft(button);
    }

    public void addSubmitButton(ButtonBase button) {
        addButton(button);
        submitButton = button;
    }

    public void addHiddenField(String name, String value) {
        getUngroupedFieldsMap();
        HiddenField hf = new HiddenField(new StringFieldDef(name));
        hf.setValue(value);
        ungroupedFields.put(name, hf);
    }

    public static List<InputField> searchForFields(Widget parent) {
        List<InputField> fields = new ArrayList<InputField>();
        FormUtil.getAllChildFields(parent, fields);
        return fields;
    }

    public static Form createFieldGroupForm() {
        return new Form(false);
    }
//====================================================================
//
//====================================================================

    public boolean containsField(String id) {
        return getField(id) != null;
    }

    /**
     * return a visible field given an id or name.
     * @param id
     * @return
     */
    public InputField getField(String id) {
        return getField(id, true);
    }

    /**
     * finds field based on ID and then name.  If onlyVisible is true,
     * it will ignore fields that are not visible.
     * @param id
     * @return
     */
    public InputField getField(String id, boolean onlyVisible) {
        InputField f = getFieldsMap().get(id);
        boolean tryByName = f == null || (onlyVisible && !isFieldVisible(f));

        if (tryByName) {
            for (InputField item : getFieldsMap().values()) {
                if (item.getName().equals(id)) {
                    if (!onlyVisible || isFieldVisible(item)) {
                        return item;
                    }
                }
            }

        }
        return f;
    }

    private boolean isFieldVisible(InputField f) {
        return (f instanceof HiddenField) ? f.isVisible() : GwtUtil.isOnDisplay(f);
    }

    public FieldDef getFieldDef(String id) {
        return containsField(id) ? getField(id).getFieldDef() : null;
    }

    public void setValue(String id, String value) {
        InputField f = getField(id);
        if (f!=null) f.setValue(value);
    }

    public void resetField(String id) {
        if (containsField(id)) {
            InputField f = getField(id);
            f.reset();
        }
    }

    public String getValue(String fieldName) {
        InputField f = getField(fieldName);
        assert(f != null);
        return f.getValue();
    }

    public int getIntValue(String fieldName) {
        return FormUtil.getIntValue(getField(fieldName));
    }

    public float getFloatValue(String fieldName) {
        return FormUtil.getFloatValue(getField(fieldName));
    }
//====================================================================
//====================================================================
//
//====================================================================

    /**
     * get returns a map of fields in this form keyed by it's ID.
     * @return
     */
    private Map<String, InputField> getFieldsMap() {
        if (fields == null) {
            fields = new LinkedHashMap<String, InputField>();
            List<InputField> allFields= FormUtil.getAllChildFields(form);
            for(InputField f : allFields)  fields.put(f.getId(), f);
            addListeners();
        }
        return fields;
    }

    private List<InputFieldGroup> getFieldGroups() {
        if (groups == null)  groups= FormUtil.getAllChildGroups(form);
        return groups;
    }

    private  Map<String, InputField> getUngroupedFieldsMap() {
        if (ungroupedFields == null) {
            ungroupedFields= FormUtil.getUngroupedFieldsMap(getFieldsMap(),getFieldGroups());
        }
        return ungroupedFields;
    }


    private void addListeners() {
        if (!isListenersAdded) {
            HandlerRegistration hReg;
            for (InputField f : getFieldsMap().values()) {
                if (f.getFocusWidget()!=null) {
                    hReg= f.getFocusWidget().addKeyPressHandler(new SubmitKeyPressHandler());
                    handlerRegList.add(hReg);
                }
                hReg= f.addValueChangeHandler(fieldVCHandler);
                if (hReg != null) handlerRegList.add(hReg);
            }
            insertContainerListeners(form);
            isListenersAdded = true;
        }
    }


    public void insertContainerListeners(Widget widget) {
        if (widget instanceof HasInputFieldsAndGroups) {
            HandlerRegistration hReg=  ((HasInputFieldsAndGroups)widget).addValueChangeHandler(containerHandler);
            handlerRegList.add(hReg);
        }
        else if (widget instanceof HasWidgets) {
            for(Widget w : (HasWidgets)widget)  insertContainerListeners(w);
        }
    }




    private void clearFields() {
        if (fields!=null) {
            fields = null;
            ungroupedFields= null;
            groups= null;
        }
        if (isListenersAdded) {
            for(HandlerRegistration hr : handlerRegList) hr.removeHandler();
            handlerRegList.clear();
            isListenersAdded= false;
        }

    }

//====================================================================
//  implementing HasWidget
//====================================================================

    public void add(Widget w) {
        form.add(w);
        clearFields();
        hub.fireReinitEvent();
    }

    public void clear() {
        form.clear();
        clearFields();
    }

    public Iterator<Widget> iterator() {
        return Arrays.asList(form, buttonBar).iterator();
    }

    public boolean remove(Widget w) {
        clearFields();
        return form.remove(w);

    }

    public static class ButtonBar extends Composite {

        private HorizontalPanel right;
        private HorizontalPanel left;
        private HelpManager.HelpIcon hic = new HelpManager.HelpIcon();

        public ButtonBar() {
            HorizontalPanel hp = new HorizontalPanel();
            hp.setWidth("100%");
            hp.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
            left = new HorizontalPanel();
            left.setSpacing(3);
            left.setHorizontalAlignment(HorizontalPanel.ALIGN_LEFT);
            left.setWidth("100%");

            right = new HorizontalPanel();
            right.setSpacing(3);
            right.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);
            right.setWidth("100%");
            Label spacerRight = new Label();
            right.add(spacerRight);
            right.setCellWidth(spacerRight, "100%");

            hic.setPixelSize(16,16);
            hp.add(left);
            hp.add(right);
            hp.setCellWidth(right, "100%");
            hp.add(GwtUtil.getFiller(10,1));
            hp.add(hic);

            initWidget(hp);
        }

        public HelpManager.HelpIcon getHelpIcon() {
            return hic;
        }

        public void addRight(Widget w) {
            right.add(w);
            setVisible(true);
        }

        public void addLeft(Widget w) {
            left.add(w);
            setVisible(true);
        }

        public void addLeftSpacer() {
            Label spacerLeft = new Label();
            left.add(spacerLeft);
            right.setCellWidth(spacerLeft, "100%");
        }
    }

    public class SubmitCommand implements Command {
        public void execute() {
            //TODO: check that field is validated only once on key enter
            if (validate()) {
                //TODO: how to simulate click with PushButton?
                submitButton.setFocus(true);
                if (submitButton instanceof Button) {
                    ((Button)submitButton).click();
                }
                else {
                    submitButton.fireEvent(new ClickEvent(){ });
                }
            }
        }
    }

    public class SubmitKeyPressHandler implements KeyPressHandler {
        public void onKeyPress(KeyPressEvent ev) {
            if (submitButton == null) return;
            final int keyCode = ev.getNativeEvent().getKeyCode();
            char charCode = ev.getCharCode();
            if ((keyCode == KeyCodes.KEY_ENTER || charCode == KeyCodes.KEY_ENTER) && ev.getRelativeElement() != null) {
                DeferredCommand.addCommand(new SubmitCommand());
            }
        }
    }

    public class FieldValChangeHandler implements ValueChangeHandler<String> {
        public void onValueChange(ValueChangeEvent<String> ev) {
            InputField field= (InputField)ev.getSource();
            hub.fireChangeEvent(field.getParam());
        }
    }

    public class ContainerValChangeHandler implements ValueChangeHandler<Integer> {
        public void onValueChange(ValueChangeEvent<Integer> ev) {
            clearFields();
            getFieldsMap();
            hub.onShow();
        }
    }

}
