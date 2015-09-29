/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.ui;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
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
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.ValidationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * @author Trey Roby
 */
public class MaskAddDialog extends BaseDialog {

    private static final int MAX_BITS= 5;
    private static final String DEFAULT_COLORS[]= { "#00FF00", "#FF0000", "#FFFF00", "#FF00FF", "#7F00AF"};
    private static final WebClassProperties _prop= new WebClassProperties(MaskAddDialog.class);

    private final FlowPanel topPanel = new FlowPanel();
    private SimpleInputField maskData;
    private SimpleInputField maskDataList;
    private SimpleInputField image;
    private SimpleInputField whichFile;
    private String colorOps[]= { "#00FF00", "#FF0000", "#FFFF00", "#FF00FF", "#FF00FF"};
    private static int colorChoice= 0;
    private SimpleInputField uploadFieldWrapper;
    private HTML bitFeedback= new HTML();
    private int bitNumberAry[]= null;
    private FlowPanel bitColorPanel= new FlowPanel();
    private String bitColorArray[]= new String[MAX_BITS];


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
        topPanel.setPixelSize(400, 260);
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

        for(int i=0; (i<DEFAULT_COLORS.length);i++) bitColorArray[i]= DEFAULT_COLORS[i];
        FlowPanel hp= new FlowPanel();
        FlowPanel hp2= new FlowPanel();
        whichFile = SimpleInputField.createByProp(_prop.makeBase("whichFile"));

        maskData = SimpleInputField.createByProp(_prop.makeBase("maskData"));
        maskDataList = SimpleInputField.createByProp(_prop.makeBase("maskDataList"));
        image = SimpleInputField.createByProp(_prop.makeBase("image"));

        uploadFieldWrapper= SimpleInputField.createByProp(_prop.makeBase("upload"));
        topPanel.add(uploadFieldWrapper);
        topPanel.add(hp);
        topPanel.add(new Label("Choose up to 5 bit numbers separated by commas"));
        topPanel.add(hp2);
//        topPanel.add(bitFeedback);


//        topPanel.add(maskData);

        hp2.add(maskDataList);
        hp2.add(bitColorPanel);

        hp.add(whichFile);
        hp.add(image);

        GwtUtil.setStyles(whichFile, "display", "inline-block",
                "verticalAlign", "top",
                "marginTop", "-11px");
        GwtUtil.setStyles(image,
                "display", "inline-block",
                "paddingLeft", "35px");

        GwtUtil.setStyles(maskDataList,
                "display", "inline-block",
                "verticalAlign", "top");

        GwtUtil.setStyles(bitColorPanel,
                "display", "inline-block",
                "paddingLeft", "35px",
                "width", "100px");


        GwtUtil.setStyles(uploadFieldWrapper,
                "padding", " 10px 0 20px 0");

        GwtUtil.setStyles(hp,
                "paddingBottom", "20px");


        TextBox tb= (TextBox)image.getField().getFocusWidget();
        GwtUtil.setStyle(tb, "width", "2.5em");


        TextBox listTB= (TextBox)maskDataList.getField().getFocusWidget();
        listTB.addKeyDownHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        updateBitNumber();
                    }
                });
            }
        });

//        maskDataList.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
//            @Override
//            public void onValueChange(ValueChangeEvent<String> event) {
//            }
//        });



        updateFileChooserVisible();

        whichFile.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(ValueChangeEvent<String> event) {
                updateFileChooserVisible();
            }
        });
    }

    private void updateBitNumber() {
        String data= maskDataList.getValue();
        bitNumberAry= null;
        if (!StringUtils.isEmpty(data)) {
            String dataAry[]= data.split(",");
            List<Integer> vIntList= new ArrayList<Integer>();
            for(String s : dataAry) {
                try {
                    int v= Integer.parseInt(s);
                    if (v>=0 && v<=32 && !vIntList.contains(v)) {
                        vIntList.add(v);
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }


            }
            bitNumberAry= new int[Math.min(vIntList.size(),MAX_BITS)];
            for(int i=0; i<bitNumberAry.length; i++) {
                bitNumberAry[i]= vIntList.get(i);
            }
            bitFeedback.setHTML(Arrays.toString(bitNumberAry));
        }
        bitColorPanel.clear();
        for(int i=0; (i<bitNumberAry.length); i++) {
            FlowPanel hp= new FlowPanel();
            Label l= new Label("Bit #"+bitNumberAry[i]);
            GwtUtil.setStyles(l,"display", "inline-block", "width", "45px");
            Widget colorTag= colorLabel(i, bitNumberAry[i]);
            GwtUtil.setStyles(colorTag,"display", "inline-block",
                                      "paddingLeft", "5px");
            GwtUtil.setStyles(hp,"paddingTop", "5px");

            hp.add(l);
            hp.add(colorTag);
            bitColorPanel.add(hp);
        }
        if (bitNumberAry!=null) {
            Label help= new Label("Click on color to change");
            GwtUtil.setStyles(help, "paddingTop", "10px");
            bitColorPanel.add(help);
        }


    }

    private Widget colorLabel(int idx, int bitNumber) {

        Label colorFeedback= new Label();

        ClickHandler colorChange= new ColorChange(colorFeedback, bitNumber+"", bitColorArray, idx);
        colorFeedback.setText(" ");
        colorFeedback.addClickHandler(colorChange);
        Widget link= GwtUtil.makeLinkButton(_prop.makeBase("color"),colorChange);
        colorFeedback.setSize("10px", "10px");
        return colorFeedback;
    }






    private void updateFileChooserVisible() {
         GwtUtil.setHidden(uploadFieldWrapper, whichFile.getValue().equals("current"));
    }


    private void doComplete(String fileKey) {
        if (bitNumberAry!=null && bitNumberAry.length>0) {
            WebPlotView pv= AllPlots.getInstance().getPlotView();
            int imageIdx= image.getField().getNumberValue().intValue();

            for(int i=0; (i<bitNumberAry.length); i++) {
                int maskValue= (int)Math.pow(2,bitNumberAry[i]);
                String desc= "bit #"+bitNumberAry[i];
                MaskAdjust.addMask(pv, maskValue, imageIdx, bitColorArray[i],desc,fileKey);
                colorChoice++;
            }
        }
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

        private Widget colorFeedback;
        private String title;
        private String bitColorArray[];
        private int arrayColorIdx;


        public ColorChange(Widget colorFeedback,
                           String title,
                           String bitColorArray[],
                           int arrayColorIdx
        ) {
            this.colorFeedback= colorFeedback;
            this.title= title;
            this.bitColorArray= bitColorArray;
            this.arrayColorIdx= arrayColorIdx;
            GwtUtil.setStyle(colorFeedback, "backgroundColor", bitColorArray[arrayColorIdx]);
        }


        public void onClick(ClickEvent ev) {
            ColorPickerDialog.chooseColor(colorFeedback,"Bit #" + title,
                                         bitColorArray[arrayColorIdx],
                    new ColorPickerDialog.ColorChoice() {
                        public void choice(String color) {
                            if (color!=null && GwtUtil.isHexColor(color)) {
                                ColorChange.this.bitColorArray[arrayColorIdx]= "#" +color;
                                GwtUtil.setStyle(colorFeedback, "backgroundColor", "#" +color);
                            }
                        }
                    });
        }
    }

}
