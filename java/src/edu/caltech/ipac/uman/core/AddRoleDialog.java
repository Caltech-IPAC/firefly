package edu.caltech.ipac.uman.core;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
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
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.FormBuilder;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.ValidationException;

import static edu.caltech.ipac.uman.data.UmanConst.*;

/**
 */
public class AddRoleDialog extends BaseDialog {


    private Form form;

    //======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================
    public AddRoleDialog(Widget parent) {
        super(parent, ButtonType.OK_CANCEL, "Add a role", "Adding a new role into the system");
        Button b = this.getButton(ButtonID.OK);
        b.setText("Add");
        Widget fields = FormBuilder.createPanel(new FormBuilder.Config(125, 0),
                FormBuilder.createField(MISSION_NAME),
                FormBuilder.createField(MISSION_ID),
                FormBuilder.createField(GROUP_NAME),
                FormBuilder.createField(GROUP_ID),
                FormBuilder.createField(PRIVILEGE)
        );


        VerticalPanel vp = new VerticalPanel();
        vp.add(fields);

        form = new Form();
        form.add(vp);
//            form.setHelpId(null);

        form.setFocus(MISSION_NAME);
        setWidget(form);
//        setDefaultContentSize(610,600);
//        setContentMinWidth(500);
//        setContentMinHeight(300);

    }

    public void onCompleted() {
        // do nothing
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
        sreq.setParam(ACTION, ADD_ROLE);
        ServerTask<RawDataSet> st = new ServerTask<RawDataSet>() {

            @Override
            protected void onFailure(Throwable caught) {
                String msg = caught instanceof RPCException && !StringUtils.isEmpty(((RPCException) caught).getEndUserMsg()) ?
                                ((RPCException) caught).getEndUserMsg() : caught.getMessage();
                PopupUtil.showError("Unable to add role.", msg);
            }

            @Override
            public void onSuccess(RawDataSet result) {
                DataSet data = DataSetParser.parse(result);
                String msg = data.getMeta().getAttribute("Message");
                if (msg != null) {
                    PopupUtil.showInfo(msg);
                    onCompleted();
                }
            }

            @Override
            public void doTask(AsyncCallback<RawDataSet> passAlong) {
                SearchServices.App.getInstance().getRawDataSet(sreq,passAlong);
            }
        };
        st.start();

    }


    public void updateForm(TableData.Row row) {
        form.setValue(MISSION_NAME, String.valueOf(row.getValue(DB_MISSION)));
        form.setValue(MISSION_ID, String.valueOf(row.getValue(DB_MISSION_ID)));
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
