package edu.caltech.ipac.firefly.ui;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.HelpManager;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.util.dd.ValidationException;

import java.util.ArrayList;
import java.util.List;


/**
 * User: roby
 * Date: Oct 29, 2008
 * Time: 9:47:20 AM
 */


/**
 * @author Trey Roby
 */
public abstract class BaseDialog {

    public enum ButtonID { NO_BUTTON, OK, APPLY, CANCEL, HELP, REMOVE, YES, NO, USER}
    public enum HideType {BEFORE_COMPLETE, AFTER_COMPLETE, DONT_HIDE}
    public static final int BUTTON_HEIGHT= 20;


    private static WebClassProperties _prop= new WebClassProperties(BaseDialog.class);

    private final static String CANCEL_TXT= _prop.getName("cancel");
    private final static String OK_TXT=     _prop.getName("ok");
    private final static String APPLY_TXT=  _prop.getName("apply");
    private final static String REMOVE_TXT= _prop.getName("remove");
    private final static String HELP_TXT=   _prop.getName("help");
    private final static String YES_TXT=    _prop.getName("yes");
    private final static String NO_TXT=     _prop.getName("no");


    private final PopupPane _popup;
    private HorizontalPanel _buttons= new HorizontalPanel();
    private ButtonType _type;
    private Widget _theWidget= null;
    private HideType _hideAlgorithm = HideType.BEFORE_COMPLETE;
    private final Widget _parent;
    private boolean _firstTime= true;
    private final List<ClickHandler> _clickList= new ArrayList<ClickHandler>(3);
    private final String _helpID;
    private boolean _sizeSet= false;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================


    public BaseDialog(Widget parent, ButtonType type, String title) {
        this(parent,type,PopupType.STANDARD,title,false,false,null);
    }

    public BaseDialog(Widget parent, ButtonType type, String title,String helpID) {
        this(parent,type,PopupType.STANDARD,title,false,false, helpID);
    }

    public BaseDialog(Widget parent, ButtonType type, String title, boolean modal, String helpID) {
       this(parent,type,PopupType.STANDARD,title,modal,false,helpID);
    }



    public BaseDialog(Widget parent,
                      ButtonType type,
                      PopupType ptype,
                      String title,
                      boolean modal,
                      boolean autoHide,
                      String helpID) {
        _parent= parent;
        _type= type;
        _helpID= helpID;
        _popup= new DialogPopupPane(title,null,ptype,modal,autoHide);
        _popup.getPopupPanel().addStyleName("base-dialog");
        buildDialog();

        WebEventManager.getAppEvManager().addListener(Name.REGION_CHANGE, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                if (isVisible()) doCancel();
            }
        });
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public void setAnimationEnabled(boolean animation) {
        _popup.setAnimationEnabled(animation);
    }

    public void setButtonText(ButtonID button, String text) {
        Button b= getButton(button);
        if (b!=null) b.setText(text);
    }

    public Button getButton(ButtonID button) {
        Button b= null;
        for(Widget w : _buttons) {
            if (w instanceof DialogButton) {
                if (((DialogButton)w).getID()==button) {
                    b=(DialogButton)w;
                    break;
                }
            }
        }
        return b;
    }

    public void setWidget(Widget w)  {
        _theWidget= w;
        if (w instanceof RequiresResize) {
            DockLayoutPanel contents= new DockLayoutPanel(Style.Unit.PX);
            DockLayoutPanel layout= new DockLayoutPanel(Style.Unit.PX);
            _popup.setWidget(layout);
            VerticalPanel vp= new VerticalPanel();
            vp.add(_buttons);
            vp.setWidth("100%");
            layout.addSouth(vp,50);
            layout.add(contents);
            contents.addStyleName("base-dialog");
            contents.addStyleName("base-dialog-contents");
            contents.add(w);
        }
        else {
            SimplePanel contents= new SimplePanel();
            DockPanel layout= new DockPanel();
            _popup.setWidget(layout);
            layout.add(_buttons, DockPanel.SOUTH);
            layout.add(contents, DockPanel.CENTER);
            contents.addStyleName("base-dialog");
            contents.addStyleName("base-dialog-contents");
            contents.setWidget(w);
        }
    }

    public Widget getWidget()  { return _theWidget; }

    public void addButton(Button b) {
        Button helpButton= getButton(ButtonID.HELP);
        if (helpButton!=null) {
            int idx= _buttons.getWidgetIndex(helpButton);
            _buttons.insert(b,idx);
        }
        else {
            _buttons.add(b);
        }
    }

    public void addButtonAreaWidget(Widget w) {
        _buttons.add(w);
    }

    public void addButtonAreaWidgetBefore(Widget w) {
        _buttons.insert(w,0);
    }

    public boolean isVisible() { return _popup.isVisible(); }

    public void setVisible(boolean visible) {
        setVisible(visible,null,Integer.MIN_VALUE,Integer.MAX_VALUE);
    }

    public void setVisible(boolean visible, PopupPane.Align alignAt) {
       setVisible(visible,alignAt,Integer.MIN_VALUE,Integer.MAX_VALUE);
    }

    public void setVisible(boolean visible, PopupPane.Align alignAt, int xOffset, int yOffset) {

        if (_firstTime && visible)  deferredBuild();
        setContainerVisible(visible, alignAt, xOffset, yOffset);
        if ( visible) {
            final boolean first= _firstTime;
            DeferredCommand.addCommand(new Command() {
                public void execute() {
                    if (first) {
                        onFirstVisible();
                        if (_theWidget instanceof RequiresResize && !_sizeSet)  {
                            int maxW= Math.max(_theWidget.getOffsetHeight(),_buttons.getOffsetHeight());
                            setDefaultContentSize(maxW, _theWidget.getOffsetHeight()+_buttons.getOffsetHeight());
                        }
                    }
                    onVisible();
                }
            });
        _firstTime= false;
        }
    }


    public void show() { show(0, PopupPane.Align.CENTER); }

    public void show(int autoCloseSecs, PopupPane.Align alignAt) {
        setVisible(true, alignAt);
        if (autoCloseSecs>0) {
            Timer t= new Timer() {
                public void run() { BaseDialog.this.setVisible(false);  }
            };
            t.schedule(autoCloseSecs * 1000);
        }
    }


    public void setHideAlgorythm(HideType type) {
        _hideAlgorithm = type;

    }
    public HideType getHideAlgorythm() { return _hideAlgorithm; }

    public void setDefaultContentSize(int w, int h) {
        _popup.setDefaultSize(w,h);
        _sizeSet= true;
    }

    public void setContentMinWidth(int minWidth) { _popup.setContentMinWidth(minWidth); }
    public void setContentMinHeight(int minHeight) { _popup.setContentMinHeight(minHeight); }

    public Widget getDialogWidget() { return _popup.getPopupPanel(); }

    public final void doCancel() {
        setContainerVisible(false);
        inputCanceled();
    }

    public HandlerRegistration addClickHandler(final ClickHandler l) {
        _clickList.add(l);
        return new HandlerRegistration() {
            public void removeHandler() {
                if (_clickList.contains(l)) _clickList.remove(l);
            }
        };
    }

    /**
     * Enable/disable locating the dialog on browser or dialog resize
     * Must be called after visible or will have no effect
     * @param autoLocate enable/diable auto locating
     */
    public void setAutoLocate(boolean autoLocate) {
        _popup.setAutoLocate(autoLocate);
    }

    public void alignToCenter() {
        _popup.alignTo(null, PopupPane.Align.CENTER, 0, 0);
    }


    public void alignTo(Widget widget, PopupPane.Align alignAt) {
        _popup.alignTo(widget, alignAt, 3, 3);
    }

    public Widget getButtonsWidget() { return _buttons; }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


    private void callListeners(ClickEvent ev) {
        for(ClickHandler h : _clickList) h.onClick(ev);
    }

    private void buildDialog() {
        createButtons();
        buildContents();
    }

    private void buildContents() {
    }

    private void createButtons()  {

        _buttons.addStyleName("base-dialog-buttons");
        _buttons.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);


        boolean addHelp= false;
        if (_type.getIDs()!=null) {
            for(ButtonID id : _type.getIDs()) {
                if (id==ButtonID.HELP) addHelp= true;
                else  addButton(id);
            }
        }
        if (_helpID!=null || addHelp) {
            _buttons.add(HelpManager.makeHelpIcon(_helpID));
            
        }
    }

    private void addButton(ButtonID id) {
        Button b= new DialogButton(id);
        _buttons.add(b);
    }

//======================================================================
//------------------ Private Button Click Methods ----------------------
//======================================================================

    private void doOKClick(ClickEvent ev, HideType hideAlgorythm) {
        try {
            if (validateInput()) {
                if (hideAlgorythm==HideType.BEFORE_COMPLETE) {
                    setContainerVisible(false);
                }
                performInputComplete(ev, hideAlgorythm);
            }
        } catch (ValidationException e) {
            PopupUtil.showError("Error", e.getMessage());
        }
    }

    private void doCancelClick(ClickEvent ev) {
        doCancel();
        callListeners(ev);
    }

    private void doRemoveClick(ClickEvent ev) {
        setContainerVisible(false);
        performInputComplete(ev);
    }

    private void doHelpClick(ClickEvent ev) {
        Application.getInstance().getHelpManager().showHelpAt(_helpID);
        callListeners(ev);
    }


//======================================================================
//------------------ Private Methods -----------------------------------
//======================================================================



    private void setContainerVisible(boolean v) {
        setContainerVisible(v, null, Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    private void setContainerVisible(boolean v, PopupPane.Align alignAt, int xOffset, int yOffset) {
            if (v) {
                alignAt = alignAt == null ? PopupPane.Align.CENTER : alignAt;
                if (xOffset == Integer.MIN_VALUE || yOffset == Integer.MIN_VALUE) {
                    _popup.alignTo(_parent, alignAt);
                } else {
                    _popup.alignTo(_parent, alignAt, xOffset, yOffset);
                }
                _popup.show();
            }
            else {
                _popup.hide();
            }
    }


    private void addButtonAttributes(DialogButton b) {
        ButtonID bID= b.getID();
        switch (bID) {
            case NO_BUTTON :
                b.setText("NO BUTTON");
                break;
            case OK :
                b.setText(OK_TXT);
//                b.addStyleName("ok");
                b.addStyleName("highlight-text");
                b.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent ev) {
//                        GwtUtil.showDebugMsg("ok button click");
                        doOKClick(ev, _hideAlgorithm);
                    }
                });
                break;
            case APPLY :
                b.setText(APPLY_TXT);
//                b.addStyleName("cancel");
                b.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent ev) {
                        doOKClick(ev,HideType.DONT_HIDE);
                    }
                });
                break;
            case CANCEL :
                b.setText(CANCEL_TXT);
//                b.addStyleName("cancel");
                b.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent ev) {
//                        GwtUtil.showDebugMsg("cancel button click");
                        doCancelClick(ev);
                    }
                });
                break;
            case HELP :
                b.setText(HELP_TXT);
//                b.addStyleName("help");
                b.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent ev) {
                        doHelpClick(ev);
                    }
                });
                break;
            case REMOVE :
                b.setText(REMOVE_TXT);
//                b.addStyleName("remove");
                b.addStyleName("highlight-text");
                b.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent ev) {
                        doRemoveClick(ev);
                    }
                });
                break;
            case YES :
                b.setText(YES_TXT);
//                b.addStyleName("ok");
                b.addStyleName("highlight-text");
                b.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent ev) {
                        doOKClick(ev, _hideAlgorithm);
                    }
                });
                break;
            case NO :
                b.setText(NO_TXT);
//                b.addStyleName("cancel");
                b.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent ev) {
                        doCancelClick(ev);
                    }
                });
                break;
            case USER :
                break;
            default :
                assert false;  // unrecognized button type
                break;
        }
    }



    public void performInputComplete(final ClickEvent ev) {
        performInputComplete(ev, _hideAlgorithm);

    }

    public void performInputComplete(final ClickEvent ev, final HideType hideAlgorythm) {
        inputCompleteAsync(new AsyncCallback<String>() {
            public void onFailure(Throwable caught) { }
            public void onSuccess(String result) {
                callListeners(ev);
                if (hideAlgorythm==HideType.AFTER_COMPLETE) setVisible(false);
            }
        });
    }


    public void inputCompleteAsync(AsyncCallback<String> cb) {
        inputComplete();
        cb.onSuccess("ok");
    }

    /**
     * This method should return true if input is valid and false if it is not.
     * If this method returns false the dialog will not be removed.  In the case of
     * returning false then the method is responsible for telling the user why the
     * input is invalid (such as by calling GwtUtil.showMsg).
     *
     * Alternatively, for convenience in the failure case, the dialog may throw a DialogInputException.
     * The message in the exception will be show to the user using GwtUtil.showMsg.
     *
     * @return true if success, false if failure
     * @throws ValidationException thrown in failure case when you want to show the
     * user a message that he must acknowledge.  The exception exist only for convinence.
     * It is optional it you wish to use it.
     *
     */
    protected boolean validateInput() throws ValidationException { return true; }

    protected void inputComplete() {}
    protected void inputCanceled() { }
    protected void onFirstVisible() {  }
    protected void onVisible() {  }
    protected void deferredBuild() {  }


    public class DialogButton extends Button {
        public final BaseDialog.ButtonID _id;
        public DialogButton(BaseDialog.ButtonID id) {
            super("");
            _id= id;
            addButtonAttributes(this);
//            if (BrowserUtil.isTouchInput()) {
//                GwtUtil.setStyle(this, "padding", "4px 7px 4px 7px");
//            }
        }

        public ButtonID getID() { return _id; }
    }

    private class DialogPopupPane extends PopupPane {

        DialogPopupPane(String header, Widget content, PopupType ptype, boolean modal, boolean autoHide) {
            super(header,content,ptype,modal,autoHide);
        }

        @Override
        protected void onCloseButtonClick(ClickEvent ev) { doCancelClick(ev); }
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
