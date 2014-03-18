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
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.uman.commands.UmanCmd;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.ValidationException;

import static edu.caltech.ipac.uman.data.UmanConst.*;

/**
 */
public class AddMissionDialog extends BaseDialog {


    private Form form;
    private final UmanCmd parentCmd;

    //======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================
    public AddMissionDialog(Widget parent, UmanCmd parentCmd) {
        super(parent, ButtonType.OK_CANCEL, "Add a mission", "Add a mission to the system");
        this.parentCmd = parentCmd;

        Button b = this.getButton(ButtonID.OK);
        InputField missionId = FormBuilder.createField(MISSION_ID);
        InputField mission = FormBuilder.createField(MISSION_NAME);

        b.setText("Add");
        Widget fields = FormBuilder.createPanel(new FormBuilder.Config(125, 0), missionId, mission);

        VerticalPanel vp = new VerticalPanel();
        vp.add(fields);

        form = new Form();
        form.add(vp);

        setWidget(form);
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
        sreq.setParam(ACTION, ADD_MISSION_XREF);
        parentCmd.submitRequst(sreq);
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
