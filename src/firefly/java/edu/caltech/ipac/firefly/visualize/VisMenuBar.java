/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;
/**
 * User: roby
 * Date: 3/12/13
 * Time: 2:23 PM
 */


import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
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
import edu.caltech.ipac.firefly.commands.LockRelatedImagesCmd;
import edu.caltech.ipac.firefly.commands.RotateNorthCmd;
import edu.caltech.ipac.firefly.commands.SelectAreaCmd;
import edu.caltech.ipac.firefly.commands.ShowColorOpsCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.HelpManager;
import edu.caltech.ipac.firefly.core.MenuGeneratorV2;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.core.layout.Region;
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
    private PopupPane popup= null;
    private VerticalPanel mbarVP = new VerticalPanel();
    private VerticalPanel mbarPopBottom = null;
    private HTML _toolbarHelpLabel = new HTML("&nbsp;&nbsp;&nbsp;&nbsp;");
    private boolean _usingBlankPlots = false;
    private Label heightControl = new Label("");
    private int toolPopLeftOffset= 0;
    private final boolean asPopup;
    private boolean mouseOverHidesReadout = true;
    private DockLayoutPanel inlineLayout;
    private boolean neverShown= true;
    private LockRelatedImagesCmd lockRelatedCmd= null;


    VisMenuBar(boolean asPopup) {
        this.asPopup= asPopup;
        init();
    }



    private void init() {

        lockRelatedCmd=  (LockRelatedImagesCmd)allPlots.getCommandMap().get(LockRelatedImagesCmd.COMMAND_NAME);
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
            mbarPopBottom = new VerticalPanel();
            mbarPopBottom.addStyleName("mbar-pop-bottom");
            mbarPopBottom.setWidth("100%");
            mbarPopBottom= new VerticalPanel();
            mbarPopBottom.add(_toolbarHelpLabel);
            GwtUtil.setStyles(_toolbarHelpLabel, "fontSize", "9pt",
                              "padding", "0 5px 0 5px");
            HorizontalPanel mbarHP = new HorizontalPanel();
            mbarHP.add(heightControl);
            mbarHP.add(mbarVP);
            popup.setWidget(mbarHP);
            popup.setHeader("Visualization Tools");
        }
        else {
            inlineLayout= new DockLayoutPanel(Style.Unit.PX);
            Region r= Application.getInstance().getLayoutManager().getRegion(LayoutManager.VIS_TOOLBAR_REGION);
            if (r!=null) {
                r.setDisplay(inlineLayout);
                r.getDisplay().setSize("100%", "38px");
            }
            GwtUtil.setStyles(_toolbarHelpLabel,
                              "fontSize", "10px",
                              "whiteSpace", "normal",
                              "padding", "4px 2px 0 0");
            inlineLayout.setSize("100%", "100%");
            inlineLayout.addWest(mbarVP, 915);
            inlineLayout.add(_toolbarHelpLabel);
            neverShown= false;
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
                _toolbarHelpLabel.setWidth("500px");
            }
            else {
                mbarHor = menuGen.makeMenuToolBarFromProp("VisMenuBar.row1", true);
                mbarHor2 = menuGen.makeMenuToolBarFromProp("VisMenuBar.row2", true);
                mbarVP.add(mbarHor);
                mbarVP.add(mbarHor2);
                _toolbarHelpLabel.setWidth("300px");
            }
            GwtUtil.setStyles(mbarHor, "border", "none",
                                       "background", "transparent",
                                       "paddingTop", "3px" );
            if (mbarHor2!=null) {
                GwtUtil.setStyles(mbarHor2, "border", "none",
                                            "background", "transparent",
                                            "paddingTop", "3px");
            }
            mbarHor.add(makeHelp());
            mbarVP.add(mbarPopBottom);
        }
        else {
            MenuGeneratorV2 menuGen = MenuGeneratorV2.create(allPlots.getCommandMap(),new OverNotify(), true);
            FlowPanel mbarHor = menuGen.makeMenuToolBarFromProp("VisMenuBar.all", false);
            mbarVP.add(mbarHor);
            GwtUtil.setStyles(mbarHor, "padding", "3px 0 0 4px", "whitespace", "normal");
//            _toolbarHelpLabel.setSize("500px", "13px");
            mbarHor.add(makeHelp());
        }



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
                              "verticalAlign", "top",
                              "lineHeight", "32px",
                              "borderColor", "transparent");

        } else {
            GwtUtil.setStyles(help.getElement(),
                              "padding", "0 0 0px 10px",
                              "verticalAlign", "top",
                              "display", "inline-block",
                              "lineHeight", "32px",
                              "whitespace", "normal",
                              "borderColor", "transparent");

        }
        return help;
    }



    void updateVisibleWidgets() {
        PlotWidgetGroup group = allPlots.getActiveGroup();
        if (group!=null) {
            setCommandHidden(group.getAllActive().size() < 2,       LockRelatedImagesCmd.COMMAND_NAME);
            lockRelatedCmd.setLockRelated(group.getLockRelated());
        }
        else {
            setCommandHidden(true,       LockRelatedImagesCmd.COMMAND_NAME);
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
            _toolbarHelpLabel.setHTML("<b>" + mpw.getTitle() + "</b>");
        } else {
            _toolbarHelpLabel.setHTML("&nbsp;&nbsp;&nbsp;&nbsp;");
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
        lockRelatedCmd.setLockRelated(false);
    }



    PopupPane getPopup() { return popup; }
    DockLayoutPanel getInlineLayout() { return inlineLayout; }
//    Widget getInlineStatusLine() { return mbarPopBottom; }
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
            if (_toolbarHelpLabel.getOffsetWidth()>40) {
                _toolbarHelpLabel.setText(cmd.getShortDesc());
            }
            else {
                _toolbarHelpLabel.setHTML("&nbsp;&nbsp;&nbsp;&nbsp;");
            }
        }

        public void out(GeneralCommand cmd) {
            _toolbarHelpLabel.setHTML("&nbsp;&nbsp;&nbsp;&nbsp;");
        }
    }

}

