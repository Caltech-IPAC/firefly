package edu.caltech.ipac.firefly.visualize;
/**
 * User: roby
 * Date: 3/12/13
 * Time: 2:23 PM
 */


import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.TouchEndEvent;
import com.google.gwt.event.dom.client.TouchEndHandler;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.dom.client.TouchStartHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.commands.CenterPlotOnQueryCmd;
import edu.caltech.ipac.firefly.commands.FitsDownloadCmd;
import edu.caltech.ipac.firefly.commands.FitsHeaderCmd;
import edu.caltech.ipac.firefly.commands.ImageSelectCmd;
import edu.caltech.ipac.firefly.commands.IrsaCatalogCmd;
import edu.caltech.ipac.firefly.commands.LockImageCmd;
import edu.caltech.ipac.firefly.commands.RotateNorthCmd;
import edu.caltech.ipac.firefly.commands.SelectAreaCmd;
import edu.caltech.ipac.firefly.commands.ShowColorOpsCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.HelpManager;
import edu.caltech.ipac.firefly.core.MenuGenerator;
import edu.caltech.ipac.firefly.resbundle.css.CssData;
import edu.caltech.ipac.firefly.resbundle.css.FireflyCss;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.IconMenuItem;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.PopupType;
import edu.caltech.ipac.firefly.util.Dimension;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;

/**
 * @author Trey Roby
 */
public class VisMenuBar {

    private static final FireflyCss css = CssData.Creator.getInstance().getFireflyCss();
    public enum ToolbarRows {ONE, MULTI}
    private final AllPlots allPlots= AllPlots.getInstance();
    private PopupPane popup;
    private VerticalPanel mbarVP = new VerticalPanel();
    private VerticalPanel mbarPopBottom = new VerticalPanel();
    private HTML _toolbarTitle = new HTML();
    private boolean _usingBlankPlots = false;
    private final CheckBox _lockCB = GwtUtil.makeCheckBox("Lock related", "Lock images of all bands for zooming, scrolling, etc", false);
    private Label heightControl = new Label("");
    private int toolPopLeftOffset= 0;
    private boolean asPopup;
    private boolean mouseOverHidesReadout = true;
    private FlowPanel inlineLayout;


    VisMenuBar(boolean asPopup) {
        this.asPopup= asPopup;
        _lockCB.setStyleName("groupLock");

        _lockCB.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                allPlots.getActiveGroup().setLockRelated(_lockCB.getValue());
            }
        });
        init();
    }



    private void init() {
        PopupPane.HeaderType hType= allPlots.isFullControl()? PopupPane.HeaderType.NONE :PopupPane.HeaderType.SIDE;
        if (asPopup) {
            popup = new PopupPane("Tools", null, PopupType.STANDARD, false, false, false, hType) {
                @Override
                protected void onClose() {
                    allPlots.updateUISelectedLook();
                    allPlots.getEventManager().fireEvent(
                            new WebEvent<Boolean>(this, Name.VIS_MENU_BAR_POP_SHOWING, false));
                }
            };
            popup.setAnimationEnabled(true);
            popup.setRolldownAnimation(true);
            popup.setAnimationDurration(300);
            popup.setAnimateDown(true);
        }
        else {
            inlineLayout= new FlowPanel();
        }

        mbarPopBottom.addStyleName("mbar-pop-bottom");
        if (asPopup) {
            mbarPopBottom.add(_lockCB);
            mbarPopBottom.add(_toolbarTitle);
            mbarPopBottom.setCellHorizontalAlignment(_lockCB, HasHorizontalAlignment.ALIGN_RIGHT);
            GwtUtil.setStyles(_toolbarTitle, "fontSize", "9pt",
                              "padding", "0 5px 0 5px");

        }
        else {
            HorizontalPanel inlineHP= new HorizontalPanel();
            inlineHP.add(_toolbarTitle);
            inlineHP.add(_lockCB);
            inlineHP.setCellHorizontalAlignment(_lockCB, HasHorizontalAlignment.ALIGN_RIGHT);
            mbarPopBottom.add(inlineHP);
            GwtUtil.setStyles(_toolbarTitle, "fontSize", "9pt",
                              "padding", "9px 5px 0 5px");
        }
        mbarPopBottom.setWidth("100%");



        HorizontalPanel mbarHP = new HorizontalPanel();
        mbarHP.add(heightControl);
        mbarHP.add(mbarVP);




        GwtUtil.setStyle(_lockCB, "paddingRight", "3px");

        _lockCB.setText("Lock images of all bands for zooming, scrolling, etc");
        if (asPopup) {
            popup.setWidget(mbarHP);
            popup.setHeader("Visualization Tools");
        }
        else {
            inlineLayout.add(mbarHP);
        }

        updateLayout();
        mbarVP.addDomHandler(new MouseOverHandler() {
            public void onMouseOver(MouseOverEvent event) {
                hideMouseReadout();
            }
        }, MouseOverEvent.getType());

    }

    private void hideMouseReadout() {
        if (mouseOverHidesReadout) allPlots.getMouseReadout().suggestHideMouseReadout();
    }

    void setLeftOffset(int offset) { toolPopLeftOffset= offset; }

    public void setMouseOverHidesReadout(boolean mouseOverHideReadout) {
        this.mouseOverHidesReadout = mouseOverHideReadout;
    }

    private void adjustSize() {
        if (popup!=null && popup.isVisible()) popup.internalResized();
    }

    void updateLayout() {

        MenuGenerator menuGen = MenuGenerator.create(allPlots.getCommandMap(),true);

        MenuBar mbarHor;
        MenuBar mbarHor2= null;

        ToolbarRows rows= (Window.getClientWidth()>1280+toolPopLeftOffset || !asPopup) ? ToolbarRows.ONE : ToolbarRows.MULTI;

        mbarVP.clear();
        if (rows==ToolbarRows.ONE) {
            mbarHor = menuGen.makeToolBarFromProp("VisMenuBar.all", new PopupMenubar(), false, true, true);
            mbarVP.add(mbarHor);
            _toolbarTitle.setWidth("500px");
            if (allPlots.isFullControl()) heightControl.setHeight("0px");
        }
        else {
            mbarHor = menuGen.makeToolBarFromProp("VisMenuBar.row1", new PopupMenubar(), false, true, true);
            mbarHor2 = menuGen.makeToolBarFromProp("VisMenuBar.row2", new PopupMenubar(), false, true, true);
            mbarVP.add(mbarHor);
            mbarVP.add(mbarHor2);
            _toolbarTitle.setWidth("300px");
            if (allPlots.isFullControl()) {
                heightControl.setHeight(allPlots.getMouseReadout().getContentHeight()+ "px");
            }
        }

        mbarHor.addItem(makeHelp());

        GwtUtil.setStyles(mbarHor, "border", "none",
                          "background", "transparent");
        if (mbarHor2!=null) {
            GwtUtil.setStyles(mbarHor2, "border", "none",
                              "background", "transparent");
        }

        if (asPopup)  mbarVP.add(mbarPopBottom);
        adjustSize();
        updateVisibleWidgets();
    }

    public boolean isPopup() { return asPopup; }

    private MenuItem makeHelp() {
        IconMenuItem help = new IconMenuItem(HelpManager.makeHelpImage(),
                         new Command() {
                             public void execute() {
                                 Application.getInstance().getHelpManager().showHelpAt("visualization.fitsViewer");
                             }
                         }, false);
        help.setTitle("Help on FITS visualization");
        if (asPopup) {
            GwtUtil.setStyles(help.getElement(), "paddingLeft", "7px",
                              "paddingBottom", "3px",
                              "borderColor", "transparent");
        } else {
            GwtUtil.setStyles(help.getElement(),
                              "padding", "0 0 2px 10px",
                              "borderColor", "transparent");

        }
        return help;
    }



    void updateVisibleWidgets() {
        PlotWidgetGroup group = allPlots.getActiveGroup();
        if (group!=null) {
            GwtUtil.setHidden(_lockCB, group.getAllActive().size() < 2);
            _lockCB.setValue(group.getLockRelated());
        }
        else {
            GwtUtil.setHidden(_lockCB,true);
        }
        if (popup!=null) {
            if (!GwtUtil.isOnDisplay(popup.getPopupPanel())) updateToolbarAlignment();
        }

        MiniPlotWidget mpw = allPlots.getMiniPlotWidget();
        if (mpw!=null && mpw.getCurrentPlot()!=null) {
            setCommandHidden(!mpw.isImageSelection(),       ImageSelectCmd.CommandName);
            setCommandHidden(!mpw.isLockImage(),            LockImageCmd.CommandName);
            setCommandHidden(!mpw.isCatalogButtonEnabled(), IrsaCatalogCmd.CommandName);

            if (!_usingBlankPlots) _usingBlankPlots= mpw.getCurrentPlot().isBlankImage();
            if (_usingBlankPlots) {
                setupBlankCommands();
            }

        }
        else {
            setCommandHidden(true, IrsaCatalogCmd.CommandName);
        }
    }

    private void setCommandHidden(boolean hidden, String... cmdName) {
        for(String name : cmdName) {
            GeneralCommand c=  allPlots.getCommandMap().get(name);
            if (c!=null) c.setHidden(hidden);
        }
    }

    private void setupBlankCommands() {
        MiniPlotWidget mpw = allPlots.getMiniPlotWidget();
        if (mpw.getCurrentPlot()!=null) {
            boolean hide= mpw.getCurrentPlot().isBlankImage();
            setCommandHidden(hide,
                             SelectAreaCmd.CommandName, FitsHeaderCmd.CommandName,
                             FitsDownloadCmd.CommandName, AllPlots.ColorTable.CommandName,
                             AllPlots.Stretch.CommandName, RotateNorthCmd.CommandName,
                             CenterPlotOnQueryCmd.CommandName, ShowColorOpsCmd.COMMAND_NAME   );

        }

    }

    void updatePlotTitleToMenuBar() {
        MiniPlotWidget mpw= allPlots.getMiniPlotWidget();
        if (mpw!=null && mpw.getTitle() != null && asPopup) {
            _toolbarTitle.setHTML("<b>" + mpw.getTitle() + "</b>");
        } else {
            _toolbarTitle.setHTML("");
        }
    }

    void hide() {
        if (popup!=null) {
            popup.hide();
            allPlots.getEventManager().fireEvent(new WebEvent<Boolean>(this, Name.VIS_MENU_BAR_POP_SHOWING, false));
        }
    }

    void show() {
        if (popup!=null) {
            if (!GwtUtil.isOnDisplay(popup.getPopupPanel())) updateToolbarAlignment();
            popup.show();
            allPlots.getEventManager().fireEvent(new WebEvent<Boolean>(this,Name.VIS_MENU_BAR_POP_SHOWING, true));
        }
    }


    public void toggleVisibleSpecial(MiniPlotWidget mpw) {
        if (mpw != null && popup!=null) {
            if (popup.isVisible() && !mpw.isExpanded()) popup.hide();
            else show();
        }
    }

    public void setPersistent(boolean p) { if (popup!=null) popup.setDoRegionChangeHide(!p); }

    public void teardown() {
        _lockCB.setValue(false);
    }



    PopupPane getPopup() { return popup; }
    FlowPanel getInlineLayout() { return inlineLayout; }
    Widget getInlineStatusLine() { return mbarPopBottom; }
    Widget getWidget() { return asPopup?popup.getPopupPanel():inlineLayout;}

    Dimension getToolbarSize() {
        Dimension dim= new Dimension(0,0);
        if (asPopup) {
            Widget pp= popup.getPopupPanel();
            if (pp!=null) {
                dim= new Dimension(pp.getOffsetWidth(), pp.getOffsetHeight());
            }
        }
        else {
            dim= new Dimension(inlineLayout.getOffsetWidth(), inlineLayout.getOffsetHeight());
        }
        return dim;

    }

    boolean isVisible() {
        if (asPopup) {
            return popup.isVisible();
        }
        else {
            return GwtUtil.isOnDisplay(inlineLayout);
        }
    }

    void updateToolbarAlignment() {
        if (popup!=null) {
            if (Window.getClientWidth() > 1220+toolPopLeftOffset && Application.getInstance().getCreator().isApplication()) {
                popup.alignTo(RootPanel.get(), PopupPane.Align.TOP_LEFT, 130 + toolPopLeftOffset, 0);
            } else {
                popup.alignTo(RootPanel.get(), PopupPane.Align.TOP_LEFT, toolPopLeftOffset, 0);
            }

        }
    }


    private class PopupMenubar extends MenuBar {


        private MenuItem _selected;

        public PopupMenubar() {
            super(false);

            addHandler(new MouseOutHandler() {
                public void onMouseOut(MouseOutEvent event) {
                    updatePlotTitleToMenuBar();
                    setHighlight(false);
                    _selected = null;
                }
            }, MouseOutEvent.getType());

            addDomHandler(new MouseDownHandler() {
                public void onMouseDown(MouseDownEvent event) {
                    setHighlight(false);
                    _selected = getSelectedItem();
                    setHighlight(true);
                }
            }, MouseDownEvent.getType());

            addDomHandler(new MouseUpHandler() {
                public void onMouseUp(MouseUpEvent event) {
                    setHighlight(false);
                    _selected = getSelectedItem();
                    setHighlight(false);
                }
            }, MouseUpEvent.getType());

            addDomHandler(new TouchStartHandler() {
                public void onTouchStart(TouchStartEvent event) {
                    setHighlight(false);
                    _selected = getSelectedItem();
                    setHighlight(true);
                }
            }, TouchStartEvent.getType());

            addDomHandler(new TouchEndHandler() {
                public void onTouchEnd(TouchEndEvent event) {
                    setHighlight(false);
                    _selected = getSelectedItem();
                    setHighlight(false);
                }
            }, TouchEndEvent.getType());

        }

        private void setHighlight(boolean highlight) {
            if (_selected != null) {
                DOM.setStyleAttribute(_selected.getElement(), "backgroundColor",
                                      highlight ? css.selectedColor() : "transparent");
            }
        }

        @Override
        public void onBrowserEvent(Event ev) {
            super.onBrowserEvent(ev);

            if (DOM.eventGetType(ev) == Event.ONMOUSEOVER) {
                hideMouseReadout();
                setHighlight(false);
                MiniPlotWidget mpw= allPlots.getMiniPlotWidget();
                String s = mpw != null ? mpw.getTitle() : "";
                _selected = getSelectedItem();
                if (_selected != null) s = _selected.getTitle();
                _toolbarTitle.setText(s);
            } else if (DOM.eventGetType(ev) == Event.ONCLICK) {
                setHighlight(false);
                _selected = getSelectedItem();
                setHighlight(true);
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
