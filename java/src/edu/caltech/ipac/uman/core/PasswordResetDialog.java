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
public class PasswordResetDialog extends BaseDialog {

    private Form form;
    private MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
    private List<String> users = new ArrayList<String>();
    private final UmanCmd parentCmd;


    //======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================
    public PasswordResetDialog(Widget parent, UmanCmd parentCmd) {
        super(parent, ButtonType.OK_CANCEL, "Password Reset", "Reset a user's password");
        this.parentCmd = parentCmd;

        Button b = this.getButton(ButtonID.OK);
        b.setText("Reset");

        InputField email = FormBuilder.createField(EMAIL);
        email.getFocusWidget().setEnabled(false);

        InputField sendtoEmail = FormBuilder.createField(SENDTO_EMAIL);

        Widget emailF = FormBuilder.createPanel(new FormBuilder.Config(125, 0), email);
        Widget sendtoF = FormBuilder.createPanel(new FormBuilder.Config(125, 0), sendtoEmail);
        GwtUtil.setStyle(emailF, "backgroundColor", "linen");

        VerticalPanel vp = new VerticalPanel();
        vp.add(emailF);
        vp.add(GwtUtil.getFiller(0, 5));
        vp.add(sendtoF);

        form = new Form();
        form.add(vp);

        form.setFocus(SENDTO_EMAIL);
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
        sreq.setParam(ACTION, RESET_PASS);
        parentCmd.submitRequst(sreq);
    }


    public void updateForm(TableData.Row row) {
        form.setValue(EMAIL, String.valueOf(row.getValue(DB_EMAIL)));
        form.setValue(SENDTO_EMAIL, String.valueOf(row.getValue(DB_EMAIL)));
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
