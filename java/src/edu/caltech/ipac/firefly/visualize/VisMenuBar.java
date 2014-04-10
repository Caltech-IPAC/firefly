package edu.caltech.ipac.firefly.visualize;
/**
 * User: roby
 * Date: 3/12/13
 * Time: 2:23 PM
 */


import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
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
import edu.caltech.ipac.firefly.core.MenuGeneratorV2;
import edu.caltech.ipac.firefly.resbundle.css.CssData;
import edu.caltech.ipac.firefly.resbundle.css.FireflyCss;
import edu.caltech.ipac.firefly.ui.GwtUtil;
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
    private Widget mbarPopBottom = new VerticalPanel();
    private HTML _toolbarTitle = new HTML("&nbsp;&nbsp;&nbsp;&nbsp;");
    private boolean _usingBlankPlots = false;
    private final CheckBox _lockCB = GwtUtil.makeCheckBox("Lock related", "Lock images of all bands for zooming, scrolling, etc", false);
    private Label heightControl = new Label("");
    private int toolPopLeftOffset= 0;
    private boolean asPopup;
    private boolean mouseOverHidesReadout = true;
    private FlowPanel inlineLayout;
    private boolean neverShown= true;


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
//        PopupPane.HeaderType hType= allPlots.isFullControl()? PopupPane.HeaderType.NONE :PopupPane.HeaderType.SIDE;
        if (asPopup) {
            popup = new PopupPane("Tools", null, PopupType.STANDARD, false, false, false, PopupPane.HeaderType.SIDE) {
                @Override
                protected void onClose() {
                    allPlots.updateUISelectedLook();
                    allPlots.fireEvent(
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
            VerticalPanel bottomVP= new VerticalPanel();
            bottomVP.add(_lockCB);
            bottomVP.add(_toolbarTitle);
            bottomVP.setCellHorizontalAlignment(_lockCB, HasHorizontalAlignment.ALIGN_RIGHT);
            GwtUtil.setStyles(_toolbarTitle, "fontSize", "9pt",
                              "padding", "0 5px 0 5px");
            mbarPopBottom= bottomVP;

        }
        else {
            FlowPanel fp= new FlowPanel();
//            HorizontalPanel inlineHP= new HorizontalPanel();
            fp.add(_toolbarTitle);
            fp.add(_lockCB);
//            inlineHP.setCellHorizontalAlignment(_lockCB, HasHorizontalAlignment.ALIGN_RIGHT);
            FlowPanel bottomWrapper= new FlowPanel();
            bottomWrapper.add(fp);
            GwtUtil.setStyle(_lockCB, "display", "inline-block");
            GwtUtil.setStyles(_toolbarTitle,
                              "display", "inline-block",
                              "fontSize", "10px",
                              "padding", "0px 5px 0 5px");
            neverShown= false;
            mbarPopBottom= bottomWrapper;
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

        setCommandHidden(true, ImageSelectCmd.CommandName);
        setCommandHidden(true, LockImageCmd.CommandName);
        setCommandHidden(true, IrsaCatalogCmd.CommandName);

    }

    private void hideMouseReadout() {
        if (mouseOverHidesReadout) allPlots.suggestHideMouseReadout();
    }

    void setLeftOffset(int offset) { toolPopLeftOffset= offset; }

    public void setMouseOverHidesReadout(boolean mouseOverHideReadout) {
        this.mouseOverHidesReadout = mouseOverHideReadout;
    }

    private void adjustSize() {
        if (popup!=null && popup.isVisible()) popup.internalResized();
    }

    void updateLayout() {



        ToolbarRows rows= (Window.getClientWidth()>1210+toolPopLeftOffset || !asPopup) ? ToolbarRows.ONE : ToolbarRows.MULTI;

        mbarVP.clear();
        if (asPopup) {
            FlowPanel mbarHor;
            FlowPanel mbarHor2= null;
            MenuGeneratorV2 menuGen = MenuGeneratorV2.create(allPlots.getCommandMap(),new OverNotify(), true);
            if (rows==ToolbarRows.ONE) {
                mbarHor = menuGen.makeMenuToolBarFromProp("VisMenuBar.all", true);
                mbarVP.add(mbarHor);
                _toolbarTitle.setWidth("500px");
            }
            else {
                mbarHor = menuGen.makeMenuToolBarFromProp("VisMenuBar.row1", true);
                mbarHor2 = menuGen.makeMenuToolBarFromProp("VisMenuBar.row2", true);
                mbarVP.add(mbarHor);
                mbarVP.add(mbarHor2);
                _toolbarTitle.setWidth("300px");
            }
            GwtUtil.setStyles(mbarHor, "border", "none",
                              "background", "transparent");
            if (mbarHor2!=null) {
                GwtUtil.setStyles(mbarHor2, "border", "none",
                                  "background", "transparent");
            }
            mbarHor.add(makeHelp());
        }
        else {
            MenuGeneratorV2 menuGen = MenuGeneratorV2.create(allPlots.getCommandMap(),new OverNotify(), true);
            FlowPanel mbarHor = menuGen.makeMenuToolBarFromProp("VisMenuBar.all", false);
            mbarVP.add(mbarHor);
            GwtUtil.setStyle(mbarHor, "padding", "3px 0 0 4px");
            _toolbarTitle.setSize("500px", "13px");
            mbarHor.add(makeHelp());
        }



        if (asPopup)  mbarVP.add(mbarPopBottom);
        adjustSize();
        updateVisibleWidgets();
    }

    public boolean isPopup() { return asPopup; }

    private Widget makeHelp() {

        Widget help = new SimplePanel(HelpManager.makeHelpImage());
        help.addDomHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                Application.getInstance().getHelpManager().showHelpAt("visualization.fitsViewer");
            }
        }, ClickEvent.getType());


        help.setTitle("Help on FITS visualization");
        if (asPopup) {
            GwtUtil.setStyles(help.getElement(), "paddingLeft", "7px",
                              "display", "inline-block",
                              "verticalAlign", "bottom",
                              "lineHeight", "27px",
                              "borderColor", "transparent");

        } else {
            GwtUtil.setStyles(help.getElement(),
                              "padding", "0 0 0px 10px",
                              "verticalAlign", "bottom",
                              "display", "inline-block",
                              "lineHeight", "27px",
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
            _toolbarTitle.setHTML("&nbsp;&nbsp;&nbsp;&nbsp;");
        }
    }

    void hide() {
        if (popup!=null) {
            popup.hide();
            allPlots.fireEvent(new WebEvent<Boolean>(this, Name.VIS_MENU_BAR_POP_SHOWING, false));
        }
    }

    void show() {
        if (popup!=null) {
            if (neverShown) updateLayout();
            neverShown= false;
            if (!GwtUtil.isOnDisplay(popup.getPopupPanel())) updateToolbarAlignment();
            popup.show();
            allPlots.fireEvent(new WebEvent<Boolean>(this,Name.VIS_MENU_BAR_POP_SHOWING, true));
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

    private class OverNotify implements MenuGeneratorV2.MouseOver {
        public void in(GeneralCommand cmd) {
            _toolbarTitle.setText(cmd.getShortDesc());
        }

        public void out(GeneralCommand cmd) {
            _toolbarTitle.setHTML("&nbsp;&nbsp;&nbsp;&nbsp;");
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
