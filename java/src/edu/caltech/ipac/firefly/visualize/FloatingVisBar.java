package edu.caltech.ipac.firefly.visualize;
/**
 * User: roby
 * Date: 1/9/12
 * Time: 2:56 PM
 */


import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.commands.LayerCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.DynRequestHandler;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.resbundle.images.IconCreator;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PackageTask;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.PopupType;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

import java.util.List;

/**
 * @author Trey Roby
 */
public class FloatingVisBar {

    private final PopupPane _popup;
    private final PlotWidgetGroup _group;
    private boolean _showing = false;
    private CheckBox _allCheckCB = new CheckBox(" All");
    private boolean _enableAllCheckEvent = true;
    private boolean _dropClosed = true;
    private HandlerRegistration hreg = null;

    private static int downloadCounter = 1;

    public FloatingVisBar(PlotWidgetGroup group, Widget alignWidget) {

        _group = group;
        IconCreator _ic = IconCreator.Creator.getInstance();
        VerticalPanel panel = new VerticalPanel();
        _popup = new PopupPane("", panel, PopupType.LOW_PROFILE, false, false, false, PopupPane.HeaderType.NONE);
        updateAlignWidget(alignWidget);
        _popup.setDoRegionChangeHide(false);

        Widget popoutButton = GwtUtil.makeImageButton(
                new Image(_ic.getExpandIcon()),
                "Expand this panel to take up a larger area",
                new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        expand();
                    }
                }
        );

//        Image im = new Image(IconCreator.Creator.getInstance().getToolsIcon());
//        Widget toolsButton = GwtUtil.makeImageButton(im, "Show tools for more image operations", new ClickHandler() {
//            public void onClick(ClickEvent event) {
//                ensureSelected();
//                AllPlots ap = AllPlots.getInstance();
//                ap.setSelectedWidget(ap.getMiniPlotWidget(), true);
//            }
//        });

        Widget pdfButton = GwtUtil.makeImageButton(
                new Image(_ic.getPdf()),
                "Download PDF",
                new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        downloadPDF();
                    }
                }
        );

        Widget layerButton = GwtUtil.makeImageButton(
                new Image(_ic.getPlotLayers()),
                "Plot Layers",
                new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        doPlotLayers();
                    }
                }
        );
        ValueChangeHandler<Boolean> vc = new ValueChangeHandler<Boolean>() {
            public void onValueChange(ValueChangeEvent<Boolean> ev) {
                if (_enableAllCheckEvent) _group.setAllChecked(ev.getValue(), false);
            }
        };

        SimplePanel sp = new SimplePanel();
        _allCheckCB.addValueChangeHandler(vc);
        GwtUtil.setStyle(sp, "padding", "5px 0 3px 0");
        sp.setWidget(_allCheckCB);

        panel.setSpacing(3);
//        panel.add(toolsButton);
        panel.add(popoutButton);

        boolean showAllCB = true;
        boolean showPdfDownload = false;
        boolean showDrawingLayers = false;
        //only "hide" _allCheckCB if getEnableChecking()==false
        if (_group != null && _group.getAllActive() != null
                && !_group.getAllActive().isEmpty()) {
            showAllCB = _group.getEnableChecking();
            showPdfDownload = _group.getEnablePdfDownload();
            showDrawingLayers = _group.getShowDrawingLayers();
        }
        if (showDrawingLayers) {
            panel.add(layerButton);
        }
        if (showPdfDownload) {
            panel.add(pdfButton);
        }
        if (showAllCB) panel.add(sp);

        WebEventManager.getAppEvManager().addListener(Name.DROPDOWN_CLOSE, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                DeferredCommand.add(new Command() {
                    public void execute() {
                        _dropClosed = true;
                        if (_showing) _popup.show();
                    }
                });
            }
        });

        WebEventManager.getAppEvManager().addListener(Name.DROPDOWN_OPEN, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                DeferredCommand.add(new Command() {
                    public void execute() {
                        _dropClosed = false;
                        if (_showing) _popup.hide();
                    }
                });
            }
        });


        AllPlots.getInstance().addListener(Name.CHECKED_PLOT_CHANGE,
                                                             new WebEventListener<Boolean>() {
                                                                 public void eventNotify(WebEvent<Boolean> ev) {
                                                                     MiniPlotWidget mpw = (MiniPlotWidget) ev.getSource();
                                                                     if (!mpw.isChecked()) {
                                                                         if (_allCheckCB.getValue()) {
                                                                             _enableAllCheckEvent = false;
                                                                             _allCheckCB.setValue(false);
                                                                             _enableAllCheckEvent = true;
                                                                         }

                                                                     }
                                                                 }
                                                             });

    }

    private void expand() {
        AllPlots ap = AllPlots.getInstance();
        final MiniPlotWidget mpw = ap.getMiniPlotWidget();
        ensureSelected();
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                mpw.toggleExpand();
                _popup.hide();
                mpw.getPlotView().addListener(Name.REPLOT,
                                              new WebEventListener<ReplotDetails>() {
                                                  public void eventNotify(WebEvent<ReplotDetails> ev) {
                                                      if (ev.getData().getReplotReason() == ReplotDetails.Reason.REPARENT) {
                                                          mpw.getPlotView().getEventManager().removeListener(Name.REPLOT, this);
                                                          _popup.show();
                                                      }
                                                  }
                                              });
            }
        });

    }

    private void downloadPDF() {
        PlotWidgetGroup group = _group == null ? AllPlots.getInstance().getActiveGroup() : _group;
        DownloadRequest dlreq;
        try {
            dlreq = group.getImageGrid().getDownloadRequest();
            //todo: research on how to present "current target" or "all targets" option.
            dlreq.setParam("scope","current"); // "current target" parameter for finder chart download.
            if (dlreq != null) {
                startPackaging(dlreq);
            }
        } catch (Exception e) {
            GWT.log(e.getMessage());
//            todo: exception handling
        }
    }

    private void doPlotLayers() {
        GeneralCommand cmd = AllPlots.getInstance().getCommand(LayerCmd.CommandName);
        if (cmd != null && cmd instanceof LayerCmd) {
            cmd.execute();
        }
    }

    private void startPackaging(DownloadRequest dataRequest) {
        /*todo: title= buildTitle();

        String emailStr= null;
        if (_useEmail.getValue() && !StringUtils.isEmpty(_emailField.getValue()))  {
            emailStr= _emailField.getValue();
            Preferences.set(BackgroundManager.EMAIL_PREF, emailStr);
        }*/

        LayoutManager lman = Application.getInstance().getLayoutManager();
        dataRequest.setBaseFileName(dataRequest.getFilePrefix());
        dataRequest.setTitle(dataRequest.getTitlePrefix() + "-pdf-" + downloadCounter++);
        /*todo: add dialog box dataRequest.setEmail(emailStr);*/
        dataRequest.setDataSource(DynRequestHandler.getCurrentProject());
        dataRequest.setParam("file_type", "pdf");

        /*todo: add dialog box
        // set options into request
        List<InputField> ifs = Form.searchForFields(_optionsPanel);
        for (InputField i : ifs) {
            if (GwtUtil.isOnDisplay(i) || i instanceof HiddenField) {
                dataRequest.setParam(i.getFieldDef().getName(), i.getValue());
            }
        }*/

        Widget maskW = lman.getRegion(LayoutManager.RESULT_REGION).getDisplay();
        Widget w = _popup.getPopupPanel().getParent();
        int cX = w.getAbsoluteLeft() + w.getOffsetWidth() / 2;
        int cY = w.getAbsoluteTop() + w.getOffsetHeight() / 2;
        PackageTask.preparePackage(maskW, cX, cY, dataRequest);
    }

    private void ensureSelected() {
        List<MiniPlotWidget> all = _group.getAllActive();
        AllPlots ap = AllPlots.getInstance();
        MiniPlotWidget selected = ap.getMiniPlotWidget();
        boolean needToSet = true;
        if (all.size() > 0) {
            if (selected != null) {
                for (MiniPlotWidget mpw : all) {
                    if (mpw == selected) {
                        needToSet = false;
                        break;
                    }
                }
            }
            if (needToSet) ap.setSelectedMPW(all.get(0));
        }
    }

    public void updateAlignWidget(Widget w) {
        if (hreg != null) {
            hreg.removeHandler();
            hreg = null;
        }
        _popup.alignTo(w, PopupPane.Align.TOP_RIGHT, -15, 0);
        hreg = w.addDomHandler(new MouseOverHandler() {
            public void onMouseOver(MouseOverEvent event) {
                if (_dropClosed) _popup.show();
            }
        }, MouseOverEvent.getType());
    }

    public void setAllCheckNoEvent(boolean checked) {
        _enableAllCheckEvent = false;
        _allCheckCB.setValue(checked);
        _enableAllCheckEvent = true;
    }

    public void show() {
        ensureSelected();
        _showing = true;
        _popup.show();
    }

    public void hide() {
        _showing = false;
        _popup.hide();
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
