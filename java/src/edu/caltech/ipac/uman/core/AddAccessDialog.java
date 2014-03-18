package edu.caltech.ipac.uman.core;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.RPCException;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.ui.BaseDialog;
import edu.caltech.ipac.firefly.ui.ButtonType;
import edu.caltech.ipac.firefly.ui.FieldDefCreator;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.FormBuilder;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.ui.input.SuggestBoxInputField;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.uman.commands.UmanCmd;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.FieldDef;
import edu.caltech.ipac.util.dd.ValidationException;

import java.util.ArrayList;
import java.util.List;

import static edu.caltech.ipac.uman.data.UmanConst.*;

/**
 */
public class AddAccessDialog extends BaseDialog {

    private UmanCmd parentCmd;
    private Form form;
    private MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
//    private List<String> users = new ArrayList<String>();


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================
    public AddAccessDialog(Widget parent, UmanCmd parentCmd) {
        super(parent, ButtonType.OK_CANCEL, "Grant access to the selected role", "Grant access to the selected role");

        this.parentCmd = parentCmd;
        FieldDef fd = FieldDefCreator.makeFieldDef(EMAIL);
        SimpleInputField userField = new SimpleInputField(new SuggestBoxInputField(fd, oracle), new SimpleInputField.Config("125px"), true);

        rebuildOracle();

        Button b = this.getButton(ButtonID.OK);
        InputField mission = FormBuilder.createField(MISSION_NAME);
        InputField group = FormBuilder.createField(GROUP_NAME);
        InputField missionId = FormBuilder.createField(MISSION_ID);
        InputField groupId = FormBuilder.createField(GROUP_ID);
        InputField privilege = FormBuilder.createField(PRIVILEGE);
        mission.getFocusWidget().setEnabled(false);
        missionId.getFocusWidget().setEnabled(false);
        group.getFocusWidget().setEnabled(false);
        groupId.getFocusWidget().setEnabled(false);
        privilege.getFocusWidget().setEnabled(false);

        b.setText("Add");
        Widget role = FormBuilder.createPanel(new FormBuilder.Config(125, 0),
                mission, missionId, group, groupId, privilege, userField);
        GwtUtil.setStyle(role, "backgroundColor", "linen");

        Widget user = FormBuilder.createPanel(new FormBuilder.Config(125, 0), userField);

        VerticalPanel vp = new VerticalPanel();
        vp.add(role);
        vp.add(GwtUtil.getFiller(0, 5));
        vp.add(user);

        form = new Form();
        form.add(vp);

//        form.setFocus(EMAIL);
        setWidget(form);
    }

    @Override
    public void show(int autoCloseSecs, PopupPane.Align alignAt) {
        super.show(autoCloseSecs, alignAt);
        rebuildOracle();
    }

    void rebuildOracle() {
        oracle.clear();
        ServerTask<RawDataSet> task = new ServerTask<RawDataSet>() {

            public void onSuccess(RawDataSet result) {
                ArrayList<String> users = new ArrayList<String>();
                if (result != null) {
                    DataSet ds = DataSetParser.parse(result);
                    for (int i = 0; i < ds.getTotalRows(); i++) {
                        String email = String.valueOf(ds.getModel().getRow(i).getValue(0));
                        if ( email != null) {
                            oracle.add(email);
                            users.add(email);
                        }
                    }
                }
            }
            public void doTask(AsyncCallback<RawDataSet> passAlong) {
                final TableServerRequest sreq = new TableServerRequest(UMAN_PROCESSOR);
                sreq.setParam(ACTION, USER_LIST);
                sreq.setPageSize(Integer.MAX_VALUE);
                SearchServices.App.getInstance().getRawDataSet(sreq, passAlong);
            }
        };
        task.start();
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


    @Override
    protected boolean validateInput() throws ValidationException {
        return form.validate();
    }

    protected void inputComplete() {
        Request req = new Request();
        form.populateRequest(req);

        final TableServerRequest sreq = new TableServerRequest(UMAN_PROCESSOR, req);
        sreq.setParam(ACTION, ADD_ACCESS);
        parentCmd.submitRequst(sreq);
    }

    public void updateForm(TableData.Row row) {
        form.setValue(MISSION_NAME, String.valueOf(row.getValue(DB_MISSION)));
        form.setValue(MISSION_ID, String.valueOf(row.getValue(DB_MISSION_ID)));
        form.setValue(GROUP_NAME, String.valueOf(row.getValue(DB_GROUP)));
        form.setValue(GROUP_ID, String.valueOf(row.getValue(DB_GROUP_ID)));
        form.setValue(PRIVILEGE, String.valueOf(row.getValue(DB_PRIVILEGE)));
        form.setFocus(EMAIL);
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
