package edu.caltech.ipac.firefly.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.StatusCodeException;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.RPCException;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.ui.input.TextBoxInputField;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.ValidationException;

import java.util.HashMap;
import java.util.Map;
/**
 * User: roby
 * Date: Aug 18, 2009
 * Time: 11:32:21 AM
 */


/**
 * A class with a bunch of static methods define the different types of
 * statandard popups we use.
 *
 * @author Trey Roby
 */
public class PopupUtil {

    private static final String INFO_MSG_STYLE= "info-msg";
    private static final String ERROR_MSG_STYLE= "error-msg";
    private static final String ERROR_DETAILS_STYLE= "error-details";

    private static final Map<String,BaseDialog> _errorShowMap= new HashMap<String, BaseDialog>(8);

//======================================================================
//----------------------- Public Static Methods ------------------------
//======================================================================

    public static BaseDialog showDialog(Widget parent,
                                Widget content,
                                String title,
                                String buttonLabel,
                                String helpId) {

        final BaseDialog dialog= new BaseDialog(parent, ButtonType.REMOVE, title, true, helpId){};
        buttonLabel = buttonLabel == null ? "Done" : buttonLabel;
        dialog.setButtonText(BaseDialog.ButtonID.REMOVE, buttonLabel);
        dialog.setWidget(content);
        dialog.setVisible(true);
        return dialog;

    }

    public static void showConfirmMsg(String title,
                                      String msg,
                                      ClickHandler yesHandler) {
        showConfirmMsg(null,title,msg,yesHandler,null);
    }

    public static void showConfirmMsg(Widget w,
                                      String title,
                                      String msg,
                                      ClickHandler yesHandler) {
        showConfirmMsg(w,title,msg,yesHandler,null);
    }

    public static void showConfirmMsg(final Widget w,
                                      final String title,
                                      final String msg,
                                      final ClickHandler yesHandler,
                                      final ClickHandler noHandler) {
        BaseDialog dialog= new BaseDialog(w, ButtonType.YES_NO,title,true,null) {
            protected void inputComplete() {
                if (yesHandler!=null) yesHandler.onClick(null);
            }
            protected void inputCanceled() {
                if (noHandler!=null) noHandler.onClick(null);
            }
        };

        HTML html= new HTML(msg);
        dialog.setWidget(html);

        dialog.setVisible(true);

    }

    public static BaseDialog showInputDialog(Widget parent,
                                      String msg,
                                      String intialValue,
                                      final ClickHandler okHandler,
                                      final ClickHandler cancelHandler) {
        int msgSize = msg == null ? 0 : msg.length();
        int w = Math.max(msgSize, 30);
        return showInputDialog(parent, msg, intialValue, w, okHandler, cancelHandler);
    }

    public static BaseDialog showInputDialog(Widget parent,
                                      String msg,
                                      String intialValue,
                                      int preferWidth,
                                      final ClickHandler okHandler,
                                      final ClickHandler cancelHandler) {

        final TextBox text = new TextBox();
        text.setVisibleLength(preferWidth);
        if (!StringUtils.isEmpty(intialValue)) {
            text.setText(intialValue);
        }

        final BaseDialog dialog= new BaseDialog(parent, ButtonType.OK_CANCEL,"Input Dialog",true,null) {
            protected void inputComplete() {
                if (okHandler!=null) {
                    okHandler.onClick(new ClickEvent() {
                        @Override
                        public Object getSource() {
                            return text.getText();
                        }
                    });
                }

            }
            protected void inputCanceled() {
                if (cancelHandler!=null) cancelHandler.onClick(null);
            }
        };

        text.addKeyPressHandler(new KeyPressHandler() {
                    public void onKeyPress(KeyPressEvent ev) {
                        final char keyCode= ev.getCharCode();
                        DeferredCommand.addCommand(new Command() {
                            public void execute() {
                                if (keyCode== KeyCodes.KEY_ENTER) {
                                    dialog.getButton(BaseDialog.ButtonID.OK).click();
                                }
                            }
                        });
                    }
                });

        VerticalPanel vp = new VerticalPanel();
        vp.setSpacing(5);
        vp.add(new HTML("<h3>" + msg + "</h3>"));
        vp.add(text);

        dialog.setWidget(vp);
        dialog.setVisible(true);
        text.setFocus(true);
        return dialog;

    }



    public static BaseDialog showInputDialog(Widget parent,
                                             String title,
                                             final SimpleInputField field,
                                             final ClickHandler okHandler,
                                             final ClickHandler cancelHandler) {


        TextBox text= null;
        final BaseDialog dialog= new BaseDialog(parent, ButtonType.OK_CANCEL,title,true,null) {
            protected void inputComplete() {
                if (okHandler!=null) {
                    okHandler.onClick(new ClickEvent() {
                        @Override
                        public Object getSource() {
                            return field.getValue();
                        }
                    });
                }

            }
            protected void inputCanceled() {
                if (cancelHandler!=null) cancelHandler.onClick(null);
            }

            @Override
            protected boolean validateInput() throws ValidationException {
                if (!field.validate()) {
                    throw new ValidationException(field.getField().getFieldDef().getErrMsg());
                }
                return true;
            }
        };

        if (field.getField() instanceof TextBoxInputField) {
            text= ((TextBoxInputField)field.getField()).getTextBox();
            text.addKeyPressHandler(new KeyPressHandler() {
                public void onKeyPress(KeyPressEvent ev) {
                    final char keyCode= ev.getCharCode();
                    DeferredCommand.addCommand(new Command() {
                        public void execute() {
                            if (keyCode== KeyCodes.KEY_ENTER) {
                                dialog.getButton(BaseDialog.ButtonID.OK).click();
                            }
                        }
                    });
                }
            });

        }

        VerticalPanel vp = new VerticalPanel();
        vp.setSpacing(5);
        vp.add(field);

        dialog.setWidget(vp);
        dialog.setVisible(true);
        if (text!=null) text.setFocus(true);
        return dialog;

    }


    public static void showSevereError(final Throwable caught) {

        String eMsg= caught!=null ? caught.getMessage() : "unknown";
        GWT.log(eMsg, caught);

        String msgExtra= "<span class=\"faded-text\">" +
                "<br><br>If you still continue to receive this message, contact IRSA for <a href='http://irsa.ipac.caltech.edu/applications/Helpdesk' target='_blank'>help</a>.  " +
                "<span>";
        String title = "Error";
        String msg = "An unexpected error has occurred." +
                "<br>Caused by: " + eMsg;
        String details = null;

        if (caught instanceof IncompatibleRemoteServiceException) {
            title = "Application is out of date";
            msg = "This application is out of date.  In most cases, refreshing the page will resolve the problem.";
        } else if ( caught instanceof StatusCodeException) {
            StatusCodeException scx = (StatusCodeException) caught;
            title = "Server is not available";
            details = eMsg;
            if (scx.getStatusCode() == 503) {
                msg = "The site is down for scheduled maintenance.";
                msgExtra = "";
            } else if (scx.getStatusCode() == 0) {
                title = "Server/Network is not available";
                msg = "If you are not connected to the internet, check your internet connection and try again";
            } else {
                msg = "The server encountered an unexpected condition which prevented it from fulfilling the request.<br>" +
                      "Refreshing the page may resolve the problem.";
            }
        } else if (caught instanceof RPCException) {
            RPCException ex = (RPCException)caught;
            details = ex.toHtmlString();
            if (ex.getEndUserMsg()!=null) {
                msg= ex.getEndUserMsg();
            }
        }

        showMsgWithDetails(title, msg + msgExtra, PopupType.STANDARD, details, ERROR_MSG_STYLE);

    }

    public static void showWarning(String title,
                                   String msg,
                                   final ClickHandler okHandler) {
        final BaseDialog dialog= new BaseDialog(null, ButtonType.OK, PopupType.STANDARD, title, true, false, null) {
            protected void inputComplete() { okHandler.onClick(null); }
        };

        if (msg != null) {
            Widget mw= makeMsg(msg,ERROR_MSG_STYLE);
            dialog.setWidget(mw);
        }
        dialog.setVisible(true);
    }    


    public static void showMinimalError(Widget anchor, String msg) {
        final PopupPanel p = new PopupPanel(true);
        p.setAnimationEnabled(true);
        p.addStyleName("onTopDialog");
        p.setWidget(new HTML(msg));
        p.setPopupPosition(anchor.getAbsoluteLeft() + anchor.getOffsetWidth(), anchor.getAbsoluteTop() + anchor.getOffsetHeight());
        p.show();
        new Timer(){
                public void run() {
                    p.hide();
                }
            }.schedule(4000);

    }

    public static void showError(final String title, final String msg) {
        showError(title,msg,null);

    }

    public static void showError(String title, String msg, String details) {
        showError(title, msg, details, true);
    }

    public static void showError(final String title, final String msg, String details, boolean doRegionChangeHide) {
        showMsgWithDetails(title, msg, PopupType.STANDARD, details, ERROR_MSG_STYLE, doRegionChangeHide);

    }

    public static BaseDialog showInfo(String str) { return showInfo(null, "Information", str, 0); }

    public static BaseDialog showInfo(Widget p, String title, String str) {
        return showInfo(p, title, str, 0);
    }


    public static BaseDialog showInfo(Widget p, String title, Widget msg, int autoCloseSec) {
        return showInfo(p,title,(Object)msg,autoCloseSec);
    }

    public static BaseDialog showInfo(Widget p, String title, String msg, int autoCloseSec) {
        return showInfo(p,title,(Object)msg,autoCloseSec);
    }



    private static BaseDialog showInfo(Widget p, String title, Object msg, int autoCloseSec) {
        boolean autoClose= autoCloseSec>0;
        final BaseDialog dialog= new BaseDialog(p, autoClose ? ButtonType.NO_BUTTONS : ButtonType.OK,
                                                PopupType.STANDARD,
                                                title, !autoClose, autoClose,null) {
            protected void inputComplete() { }
        };
        Widget msgW= msg instanceof Widget ? (Widget)msg : makeMsg(msg.toString(), INFO_MSG_STYLE);
        dialog.setWidget(msgW);
        dialog.show(autoCloseSec, PopupPane.Align.CENTER);
        return dialog;
    }

    public static void showInfoPointer(final Widget w, final String title, final String msg, final int autoCloseSec) {
        boolean autoClose= autoCloseSec>0;
        final PopupPane popup= new PopupPane(title,makeMsg(msg, INFO_MSG_STYLE),
                                             PopupType.STANDARD, true,
                                             autoClose, autoClose, PopupPane.HeaderType.TOP);
        popup.alignTo(w, PopupPane.Align.BOTTOM_CENTER);
        popup.show(autoCloseSec);
    }

    public static void showInfoPointer(int x, int y, final String title, final String msg, final int autoCloseSec) {
        showInfoPointer(x,y,title, makeMsg(msg, INFO_MSG_STYLE), autoCloseSec);
    }

    public static void showInfoPointer(int x, int y, final String title, Widget msg, final int autoCloseSec) {
        boolean autoClose= autoCloseSec>0;
        final PopupPane popup= new PopupPane(title,msg,
                                             PopupType.STANDARD, true,
                                             false, autoClose, PopupPane.HeaderType.NONE);
        popup.alignTo(null, PopupPane.Align.DISABLE);
        popup.setRolldownAnimation(true);
        popup.setAnimateDown(true);
        popup.setAnimationEnabled(true);
        popup.setPopupPosition(x,y);
        popup.show(autoCloseSec);
    }

    public static void showMinimalMsg(final Widget parent,
                                      final String msg,
                                      final int autoCloseSec,
                                      final PopupPane.Align align,
                                      final int width) {
        boolean autoClose= autoCloseSec>0;
        Widget msgW= makeMsg(msg, INFO_MSG_STYLE);
        msgW.setWidth(width+ "px");
        final PopupPane popup= new PopupPane("",msgW,
                                             PopupType.STANDARD, false,
                                             false, autoClose, PopupPane.HeaderType.NONE);
        popup.setRolldownAnimation(true);
        popup.alignTo(parent, align, 0, -3);
        popup.show(autoCloseSec);
    }

    public static void showMinimalMsg(final Widget parent,
                                      final Widget msg,
                                      final int autoCloseSec,
                                      final PopupPane.Align align,
                                      final int width) {
        boolean autoClose= autoCloseSec>0;
        Widget msgW= makeMsg(msg, INFO_MSG_STYLE,false);
        msgW.setWidth(width+ "px");
        final PopupPane popup= new PopupPane("",msgW,
                                             PopupType.STANDARD, false,
                                             false, autoClose, PopupPane.HeaderType.NONE);
        popup.setRolldownAnimation(true);
        popup.alignTo(parent, align,0,-3);
        popup.show(autoCloseSec);
    }



//======================================================================
//------------------ Private Static Methods ----------------------------
//======================================================================

    private static void showMsgWithDetails(String title,
                                           String msg,
                                           PopupType ptype,
                                           final String details,
                                           String msgStyle) {

        showMsgWithDetails(title, msg, ptype, details, msgStyle, true);
    }

    private static void showMsgWithDetails(String title,
                                           String msg,
                                           PopupType ptype,
                                           final String details,
                                           String msgStyle,
                                           final boolean doRegionChangeHide) {


        final String dialogKey= title+"-----"+msg;
        boolean showDialog= true;
        final BaseDialog dialog= new BaseDialog(null, ButtonType.OK,ptype, title,true,false, null) {
            protected void inputComplete() { }
        };
        dialog.setDoRegionChangeHide(doRegionChangeHide);

        Widget mw= (msg != null) ? makeMsg(msg,msgStyle) : makeDetailMsg(details,msgStyle);
        dialog.setWidget(mw);


        if (!StringUtils.isEmpty(details) && msg != null) {
            Button det = new Button("Details");
            dialog.addButton(det);
            det.addClickHandler(new ClickHandler(){
                public void onClick(ClickEvent ev) {
                    dialog.setVisible(false);
                    _errorShowMap.remove(dialogKey);
                    showMsgWithDetails("Error Details", null, PopupType.STANDARD, details,
                                       ERROR_DETAILS_STYLE, doRegionChangeHide);
                }
            });
        }


        if (!StringUtils.isEmpty(msg)) {
            if (_errorShowMap.containsKey(dialogKey)) {
                showDialog= false;
            }
            else {
                Button b= dialog.getButton(BaseDialog.ButtonID.OK);
                b.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        _errorShowMap.remove(dialogKey);
                    }
                });
            }
        }

        _errorShowMap.put(dialogKey,dialog);
        if (showDialog) dialog.setVisible(true);
    }

    private static Widget makeDetailMsg(String s, String msgStyle) {
//        HTML message = new HTML(s);
        HTML message = setScrollBar(s);
        message.setStyleName(msgStyle);
        SimplePanel panel = new SimplePanel();
        panel.add(message);
//        return new ScrollPanel(message);
        return panel;
    }


    private static Widget makeMsg(String s, String msgStyle) {
//        String hStr= "<span style=\"font-size: 120%;\">";
//        if(s.length() > 400){
//          s = "<div style='width: 350px; height: 250px; overflow: auto;'>" + s + "</div>";
//        }
//        HTML message = new HTML(hStr+ s + "</span>");

        HTML message = setScrollBar(s);
        message.setStyleName(msgStyle);
        SimplePanel panel = new SimplePanel();
        panel.add(message);
        //return new ScrollPanel(message);
        return panel;
    }

    private static Widget makeMsg(Widget w, String msgStyle) {
        return makeMsg(w,msgStyle,true);

    }
    private static Widget makeMsg(Widget w, String msgStyle, boolean makeScrollArea) {
        SimplePanel panel= new SimplePanel();
        DOM.setStyleAttribute(panel.getElement(),"fontSize", "120%");
        panel.setWidget(w);
        w.setStyleName(msgStyle);
        return makeScrollArea ? new ScrollPanel(panel): panel;

    }

    private static HTML setScrollBar(String string){
        // remove image tag, which could contain potentially long references
        String noImgStr = StringUtils.isEmpty(string) ? "No additional details" : string.replaceAll("\\<img[^>]*>","");
        if(noImgStr.length() > 400){
          string = "<div style='font-size: 120%; padding-left: 15px; width: 400px; height: 250px; overflow: auto;'>" + string + "</div>";
        }
        else {
          string= "<div style=\"font-size: 120%; padding-left: 15px;\">"+ string + "</div>";

        }
        return new HTML(string);

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