package edu.caltech.ipac.firefly.visualize.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.commands.ImageSelectDropDownCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.HelpManager;
import edu.caltech.ipac.firefly.ui.BaseDialog;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.SimpleTargetPanel;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.visualize.PlotWidgetFactory;
import edu.caltech.ipac.util.dd.ValidationException;


/**
 * @author Trey Roby
 */
public class ImageSelectDropDown {
    private boolean showing= false;
    private final Widget mainPanel;
    private ImageSelectPanel imSelPanel;
    private BaseDialog.HideType hideType= BaseDialog.HideType.AFTER_COMPLETE;
    private SubmitKeyPressHandler keyPressHandler= new SubmitKeyPressHandler();


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public ImageSelectDropDown(PlotWidgetFactory plotFactory) {
        createContents(plotFactory);
        mainPanel= createContents(plotFactory);



    }


    private Widget createContents(PlotWidgetFactory plotFactory) {

        imSelPanel= new ImageSelectPanel(null,true,null,new DropDownComplete(),plotFactory);
        HorizontalPanel buttons= new HorizontalPanel();
        buttons.addStyleName("base-dialog-buttons");
        buttons.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
        GwtUtil.setStyle(buttons, "paddingRight", "80px");

        Button ok= new Button("Load");
        ok.addStyleName("highlight-text");
        ok.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent ev) { doOK(); }
        });

        buttons.add(ok);
        buttons.add(HelpManager.makeHelpIcon("basics.catalog"));

        VerticalPanel vp= new VerticalPanel();
        Widget content= GwtUtil.centerAlign(imSelPanel.getMainPanel());
        vp.add(content);
        vp.add(buttons);

        vp.setCellHorizontalAlignment(content, VerticalPanel.ALIGN_CENTER);
        vp.setSize("95%", "450px");
        vp.setSpacing(3);
        content.setSize("95%", "95%");
        content.addStyleName("component-background");

        addKeyPressToAll(imSelPanel.getMainPanel());

        return vp;
    }


    private void addKeyPressToAll(Widget inWidget) {
        if (inWidget instanceof HasWidgets) {
            HasWidgets container= (HasWidgets)inWidget;
            for (Widget w : container) {
                if (w instanceof InputField) {
                    InputField f= (InputField)w;
                    if (f.getFocusWidget()!=null) {
                        f.getFocusWidget().addKeyPressHandler(keyPressHandler);
                    }
                }
                else if (w instanceof SimpleTargetPanel) {
                    SimpleTargetPanel sp= (SimpleTargetPanel)w;
                    if (sp.getInputField()!=null && sp.getInputField().getFocusWidget()!=null) {
                        sp.getInputField().getFocusWidget().addKeyPressHandler(keyPressHandler);
                    }
                }
                else {
                    addKeyPressToAll(w);
                }
            }
        }
    }




    private void doOK() {
        try {
            if (validateInput()) {
                inputComplete();
            }
        } catch (ValidationException e) {
            PopupUtil.showError("Error", e.getMessage());
        }
    }


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public void hide() {
        showing= false;
        hideOnSearch();
        Application.getInstance().getToolBar().close();
    }

    protected void hideOnSearch() { }

    public void show() {
        showing= true;
        imSelPanel.showPanel();
        Application.getInstance().getToolBar().setTitle("Select Image");
        Application.getInstance().getToolBar().setContent(mainPanel,true,null, ImageSelectDropDownCmd.COMMAND_NAME);
    }



    private void inputComplete() {
        if (hideType== BaseDialog.HideType.BEFORE_COMPLETE) hide();
        imSelPanel.inputComplete();
        if (hideType== BaseDialog.HideType.AFTER_COMPLETE) hide();
    }


    protected boolean validateInput() throws ValidationException {
        return imSelPanel.validateInput();
    }


    private class DropDownComplete implements ImageSelectPanel.PanelComplete {
        public void performInputComplete() {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public BaseDialog.HideType getHideAlgorythm() {
            return hideType;
        }

        public void setHideAlgorythm(BaseDialog.HideType hideType) {
            ImageSelectDropDown.this.hideType= hideType;
        }

        public void hide() {
            ImageSelectDropDown.this.hide();
        }
    }


    public class SubmitKeyPressHandler implements KeyPressHandler {
        public void onKeyPress(KeyPressEvent ev) {
            final int keyCode = ev.getNativeEvent().getKeyCode();
            char charCode = ev.getCharCode();
            if ((keyCode == KeyCodes.KEY_ENTER || charCode == KeyCodes.KEY_ENTER) && ev.getRelativeElement() != null) {
                DeferredCommand.addCommand(new Command() {
                    public void execute() { doOK();  }
                });
            }
        }
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
