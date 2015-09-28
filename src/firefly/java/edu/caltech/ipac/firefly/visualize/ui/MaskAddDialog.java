/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.BaseDialog;
import edu.caltech.ipac.firefly.ui.ButtonType;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.MaskPane;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.input.FileUploadField;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.draw.WebLayerItem;
import edu.caltech.ipac.util.dd.ValidationException;



/**
 * @author Trey Roby
 */
public class MaskAddDialog extends BaseDialog {

    private static final WebClassProperties _prop= new WebClassProperties(MaskAddDialog.class);

    private final FlowPanel topPanel = new FlowPanel();
    private SimpleInputField maskData;
    private SimpleInputField image;
    private SimpleInputField whichFile;
    private String colorOps[]= { "#00FF00", "#FF0000", "#FFFF00", "#FF00FF", "#FF00FF"};
    private static int colorChoice= 0;
    private SimpleInputField uploadFieldWrapper;

    private MaskPane maskPane;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public MaskAddDialog() {
        super(RootPanel.get(), ButtonType.OK_CANCEL_HELP, _prop.getTitle(), "visualization.RegionLoad");

//        Button applyB= getButton(ButtonID.OK);
//        applyB.setText(_prop.getName("load"));

        createContents();
        setWidget(topPanel);
        topPanel.setPixelSize(400, 170);
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

        FlowPanel hp= new FlowPanel();
        whichFile = SimpleInputField.createByProp(_prop.makeBase("whichFile"));

        maskData = SimpleInputField.createByProp(_prop.makeBase("maskData"));
        image = SimpleInputField.createByProp(_prop.makeBase("image"));

        uploadFieldWrapper= SimpleInputField.createByProp(_prop.makeBase("upload"));
        topPanel.add(uploadFieldWrapper);
        topPanel.add(hp);


        topPanel.add(maskData);

        hp.add(whichFile);
        hp.add(image);

        GwtUtil.setStyles(whichFile, "display", "inline-block",
                "verticalAlign", "top",
                "marginTop", "-11px");
        GwtUtil.setStyles(image,
                "display", "inline-block",
                "paddingLeft", "35px");


        GwtUtil.setStyles(uploadFieldWrapper,
                "padding", " 10px 0 20px 0");

        GwtUtil.setStyles(hp,
                "paddingBottom", "20px");


        TextBox tb= (TextBox)image.getField().getFocusWidget();
        GwtUtil.setStyle(tb, "width", "2.5em");

        updateFileChooserVisible();

        whichFile.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(ValueChangeEvent<String> event) {
                updateFileChooserVisible();
            }
        });
    }

    private void colorLabel() {

//        Label colorFeedback= new Label();
//        _layerTable.setWidget(activeRow,ON_COL,cb);
//        if (item.getHasColorSetting()) {
//            _layerTable.setWidget(activeRow,COLOR_FEEDBACK,colorFeedback);
//            _layerTable.setWidget(activeRow,COLOR_COL,makeChangeColorLink(colorFeedback, item));
//        }
//
//        ClickHandler colorChange= new ColorChange(colorFeedback, item);
//        colorFeedback.setText(" ");
//        colorFeedback.addClickHandler(colorChange);
//        Widget link= GwtUtil.makeLinkButton(_prop.makeBase("color"),colorChange);
//        colorFeedback.setSize("10px", "10px");

    }






    private void updateFileChooserVisible() {
         GwtUtil.setHidden(uploadFieldWrapper, whichFile.getValue().equals("current"));
    }


    private void doComplete(String fileKey) {
        WebPlotView pv= AllPlots.getInstance().getPlotView();
        int bitNumber= maskData.getField().getNumberValue().intValue();
        int imageIdx= image.getField().getNumberValue().intValue();
        int maskValue= (int)Math.pow(2,bitNumber);
        String desc= "bit #"+bitNumber;

        MaskAdjust.addMask(pv, maskValue, imageIdx, colorOps[colorChoice % colorOps.length],desc);
        colorChoice++;
    }

    @Override
    public void inputCompleteAsync(final AsyncCallback<String> cb) {

        if (whichFile.getValue().equals("new")) {
            FileUploadField uploadField= (FileUploadField)uploadFieldWrapper.getField();
            uploadField.submit(new AsyncCallback<String>() {
                public void onFailure(Throwable caught) { }

                public void onSuccess(String fileKey) {
                    doComplete(fileKey);
                    cb.onSuccess("ok");
                }
            });
        }
        else {
            doComplete(null);
            cb.onSuccess("ok");
        }
    }




    @Override
    protected boolean validateInput() throws ValidationException {
        return true;
    }

// =====================================================================
// -------------------- Inner Classes ----------------------------------
// =====================================================================

    private static class ColorChange implements ClickHandler {

        private Widget _colorFeedback;


        public ColorChange(Widget colorFeedback, WebLayerItem item) {
            _colorFeedback= colorFeedback;
//            _item= item;
            DOM.setStyleAttribute(_colorFeedback.getElement(),
                    "backgroundColor", item.getAutoColorInterpreted());
        }


        public void onClick(ClickEvent ev) {
//            String color= _item.getColor();
//            ColorPickerDialog.chooseColor(_colorFeedback,_prop.getTitle("colorChooser") + _item.getTitle(),color,
//                    new ColorPickerDialog.ColorChoice() {
//                        public void choice(String color) {
//                            if (color!=null && GwtUtil.isHexColor(color)) {
//                                _item.setColor(color);
////                                                      DOM.setStyleAttribute(w.getElement(),"background", color);
//                                DOM.setStyleAttribute(_colorFeedback.getElement(),
//                                        "backgroundColor", "#" +color);
//                            }
//                        }
//                    });
        }
    }

}
