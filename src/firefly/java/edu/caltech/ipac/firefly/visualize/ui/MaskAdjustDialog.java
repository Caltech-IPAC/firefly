/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.ui;

import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.caltech.ipac.firefly.ui.BaseDialog;
import edu.caltech.ipac.firefly.ui.ButtonType;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.util.dd.ValidationException;


/**
 * @author Trey Roby
 */
public class MaskAdjustDialog extends BaseDialog {

    private static final WebClassProperties _prop= new WebClassProperties(MaskAdjustDialog.class);

    private final VerticalPanel topPanel = new VerticalPanel();
    private SimpleInputField maskData;
    private SimpleInputField image;


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public MaskAdjustDialog() {
        super(RootPanel.get(), ButtonType.OK_CANCEL_HELP, _prop.getTitle(), "visualization.RegionLoad");

//        Button applyB= getButton(ButtonID.OK);
//        applyB.setText(_prop.getName("load"));

        createContents();
        setWidget(topPanel);
        topPanel.setPixelSize(400, 100);
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
        maskData = SimpleInputField.createByProp(_prop.makeBase("maskData"));
        topPanel.add(maskData);
        image = SimpleInputField.createByProp(_prop.makeBase("image"));
        topPanel.add(image);
    }


    @Override
    protected void inputComplete() {
        WebPlotView pv= AllPlots.getInstance().getPlotView();
        int maskValue= maskData.getField().getNumberValue().intValue();
        int imageIdx= image.getField().getNumberValue().intValue();
        MaskAdjust.addOrUpdateMask(pv,maskValue,imageIdx);
    }

    @Override
    protected boolean validateInput() throws ValidationException {
        return true;
    }

// =====================================================================
// -------------------- Inner Classes ----------------------------------
// =====================================================================


}
