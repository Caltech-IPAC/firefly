package edu.caltech.ipac.firefly.ui.panels.user;


import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.LoginManager;
import edu.caltech.ipac.firefly.core.Preferences;
import edu.caltech.ipac.firefly.data.Status;
import edu.caltech.ipac.firefly.data.form.FieldSetDef;
import edu.caltech.ipac.firefly.ui.FieldDefCreator;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.FormBuilder;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.input.InputField;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author tatianag
 * @version $Id: PrefGroupPanel.java,v 1.12 2012/08/09 01:09:26 loi Exp $
 */
public class PrefGroupPanel extends Composite {


    VerticalPanel vp = new VerticalPanel();
    HTML status = new HTML("<br>");
    final Form inputForm = new Form();
    final String propBase;

    public PrefGroupPanel(String groupPropBase) {
        vp.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
        propBase = groupPropBase;
        vp.add(createForm(propBase));
        status.setStyleName("user-status-text");
        vp.add(status);
        initWidget(vp);
    }

    Widget createForm(String propBase) {
        FieldSetDef fsd  = FieldDefCreator.makeFieldSetDef(propBase);
        Widget fp = FormBuilder.createPanel(200, fsd.getFieldArray());

        inputForm.setHelpId("basics.preference");
        inputForm.add(fp);
        inputForm.addSubmitButton(GwtUtil.makeFormButton("Update",
                new ClickHandler(){
                    public void onClick(ClickEvent ev) {
                        if (inputForm.validate()) {
                            onUpdate();
                            Application.getInstance().getToolBar().close();
                        }
                    }
                }));

        inputForm.addButton(GwtUtil.makeFormButton("Reset",
                new ClickHandler(){
                    public void onClick(ClickEvent ev) {
                        inputForm.reset();
                        populateForm();
                    }
                }));


        inputForm.setSize("100%", "100%");
        return inputForm;
    }

    public void populateForm() {
        List<InputField> flds = Form.searchForFields(inputForm);
        Set prefNames = Preferences.getPrefNames();
        String shortName;
        for (InputField f : flds) {
            String name = getName(f);
            shortName = name.substring(propBase.length()+1); // 1 for dot
            if (prefNames.contains(shortName)) {
                 inputForm.setValue(name, Preferences.get(shortName));
            } else {
                inputForm.getField(name).reset();
            }
        }
//        setStatus("");
    }

    public Map<String, String> getUpdated() {
        List<InputField> flds = Form.searchForFields(inputForm);
        HashMap<String,String> updatedPrefs = new HashMap<String, String>(flds.size());
        Set prefNames = Preferences.getPrefNames();

        String shortName;
        String updatedValue;
        String currentValue;
        for (InputField f : flds) {
            String name = getName(f);
            shortName = name.substring(propBase.length()+1); // 1 for dot
            updatedValue = inputForm.getValue(name);
            currentValue = Preferences.get(shortName);
            if (!prefNames.contains(shortName) ||
                (currentValue != null && !currentValue.equals(updatedValue)) ||
                (currentValue == null && updatedValue != null))    {
                 updatedPrefs.put(shortName, updatedValue);
            }
        }
        return updatedPrefs;
    }

    private String getName(InputField f) {
        String name = f.getFieldDef().getName();
        return name;
    }

    public void onUpdate()  {
        /*
        Req  req = new Req("Preference Update");
        inputForm.populateRequest(req);
        System.out.println(req.toString());
        */
        final Map<String,String> updatedPrefs = getUpdated();
        LoginManager loginManager = Application.getInstance().getLoginManager();
        if (updatedPrefs.size() > 0) {
            Preferences.bulkSet(updatedPrefs, false);
            for (String key : updatedPrefs.keySet()) {
                Preferences.set(key, updatedPrefs.get(key));
            }
            setStatus("Preferences are successfully updated.");


            if (loginManager != null && loginManager.isLoggedIn()) {
                loginManager.setPreferences(updatedPrefs,
                        new AsyncCallback<Status>() {

                            public void onFailure(Throwable caught) {
                                setStatus("Preference update has failed.");
                                if (caught != null) {
                                    PopupUtil.showSevereError(caught);
                                }
                            }

                            public void onSuccess(Status result) {
                                setStatus("Preferences are successfully updated.");
                            }
                        });
            }
        }
    }

    public void setStatus(String msg) {
        status.setText(msg);
        status.setStyleName("user-status-text");
        new Timer(){
                    public void run() {
                        status.setText("");
                    }
                }.schedule(3000);
    }

    public void setStatus(String msg, String styleName) {
        status.setText(msg);
        status.setStyleName(styleName);
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
