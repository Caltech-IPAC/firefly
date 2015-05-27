/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.ui;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.caltech.ipac.firefly.ui.BaseDialog;
import edu.caltech.ipac.firefly.ui.ButtonType;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.input.FileUploadField;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.visualize.RegionLoader;
import edu.caltech.ipac.util.dd.ValidationException;


/**
 * @author Trey Roby
 */
public class DS9RegionLoadDialog extends BaseDialog {

    private static final WebClassProperties _prop= new WebClassProperties(DS9RegionLoadDialog.class);

    private final VerticalPanel _topPanel= new VerticalPanel();
    private FileUploadField _uploadField;


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public DS9RegionLoadDialog() {
        super(RootPanel.get(), ButtonType.OK_CANCEL_HELP, _prop.getTitle(), "visualization.RegionLoad");

//        Button applyB= getButton(ButtonID.OK);
//        applyB.setText(_prop.getName("load"));

        createContents();
        setWidget(_topPanel);
        _topPanel.setPixelSize(400, 100);
        setHideAlgorythm(HideType.AFTER_COMPLETE);
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================


    @Override
    public void setVisible(boolean v) {
        super.setVisible(v, PopupPane.Align.TOP_LEFT, 200, 45);
    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


    private void createContents() {
        SimpleInputField field = SimpleInputField.createByProp(_prop.makeBase("upload"));
        _topPanel.add(field);
        _uploadField= (FileUploadField)field.getField();
        HTML help= new HTML("Supported regions: text, circle, box, polygon, line, annulus, text");
        _topPanel.add(help);


    }

    @Override
    public void inputCompleteAsync(final AsyncCallback<String> cb) {
        _uploadField.submit(new AsyncCallback<String>() {
            public void onFailure(Throwable caught) { }

            public void onSuccess(String fileKey) {
                RegionLoader.loadRegFile(fileKey, null, cb);
            }
        });
    }


    @Override
    protected boolean validateInput() throws ValidationException {
        return true;
    }

// =====================================================================
// -------------------- Inner Classes ----------------------------------
// =====================================================================


}
