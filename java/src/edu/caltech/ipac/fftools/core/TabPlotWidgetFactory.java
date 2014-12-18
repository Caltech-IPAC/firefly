package edu.caltech.ipac.fftools.core;
/**
 * User: roby
 * Date: 3/22/13
 * Time: 4:13 PM
 */


import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.commands.ImageSelectDropDownCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.fftools.FFToolEnv;
import edu.caltech.ipac.firefly.ui.PopoutWidget;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotWidgetFactory;
import edu.caltech.ipac.firefly.visualize.PlotWidgetOps;
import edu.caltech.ipac.firefly.visualize.Vis;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.ZoomType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* @author Trey Roby
*/
public class TabPlotWidgetFactory implements PlotWidgetFactory {
    private static final String GROUP_NAME= "StandaloneGroup";

    private Map<TabPane.Tab, MiniPlotWidget> mpwMap= new HashMap<TabPane.Tab, MiniPlotWidget>(7);
    private StandaloneUI aloneUI;
//    private boolean sharingView= false;
    private  PlotErrorHandler errorHandler= new PlotErrorHandler();

    private final TabPane<Widget> plotTabPane= new TabPane<Widget>();

    TabPlotWidgetFactory() {
        plotTabPane.getEventManager().addListener(TabPane.TAB_REMOVED, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                final AllPlots ap= AllPlots.getInstance();
                TabPane.Tab tab= (TabPane.Tab)ev.getData();
                MiniPlotWidget mpw= getMPW(tab);
                if (mpw!=null) {
                    ap.delete(mpw);
                    DeferredCommand.addCommand(new Command() {
                        public void execute() {
                            if (ap.isExpanded())  ap.updateExpanded(PopoutWidget.getViewType());
                            if (!aloneUI.hasPlotResults()) {
                                if (aloneUI.hasTableResults()) {
                                    aloneUI.relayoutMainArea();
                                }
                                else {
                                    GeneralCommand cmd= Application.getInstance().getCommand(ImageSelectDropDownCmd.COMMAND_NAME);
                                    if (cmd!=null) cmd.execute();
                                }

                            }
                        }
                    });

                }
            }
        } );
    }

    public void setStandAloneUI(StandaloneUI aloneUI) {
        this.aloneUI = aloneUI;
    }

//    public void setSharingView(boolean sharingView) {
//        this.sharingView= sharingView;
//    }

    public void removeCurrentTab() {
        TabPane.Tab tab= plotTabPane.getSelectedTab();
        if (tab!=null) plotTabPane.removeTab(tab);
    }

    public MiniPlotWidget create() {
        MiniPlotWidget mpw = new MiniPlotWidget(GROUP_NAME, aloneUI.makePopoutContainerForApp());
        mpw.addStyleName("standard-border");
        mpw.setRemoveOldPlot(true);
        mpw.setMinSize(50, 50);
        mpw.setAutoTearDown(false);
        mpw.setSaveImageCornersAfterPlot(true);
        mpw.setImageSelection(true);
        mpw.setTitleAreaAlwaysHidden(true);
        mpw.setInlineToolbar(true);
        mpw.setUseToolsButton(false);
        mpw.setLockImage(false);
        mpw.setPlotWidgetFactory(this);
        mpw.setShowUnexpandedHighlight(false);

        mpw.setCatalogButtonEnable(false);
        mpw.setErrorDisplayHandler(errorHandler);



        final TabPane.Tab tabItem = plotTabPane.addTab(mpw, "Put Plot Title <i>Here</i>", "FITS Image", true);
        mpw.putTitleIntoTab(tabItem);
        plotTabPane.selectTab(tabItem);

        mpwMap.put(tabItem,mpw);
//        aloneUI.eventAddedImage();

        mpw.getOps(new MiniPlotWidget.OpsAsync() {
            public void ops(PlotWidgetOps wOps) {
                final WebPlotView pv = wOps.getPlotView();

                pv.addListener(Name.PLOT_REQUEST_COMPLETED, new WebEventListener<List<WebPlot>>() {
                    public void eventNotify(WebEvent<List<WebPlot>> ev) {
                        List<WebPlot> successList = ev.getData();
                        if (successList.size() == 0) {
                            plotTabPane.removeTab(tabItem);
                        }
                        if (aloneUI.hasOnlyPlotResults()) AllPlots.getInstance().forceExpand();
                    }
                });

            }
        });


        return mpw;
    }


    public TabPane.Tab addTab(Widget w, String title, String tip) {
        TabPane.Tab tabItem = plotTabPane.addTab(w,title,tip,false);
        return tabItem;
    }

    public void removeTab(TabPane.Tab<Widget> tab) { plotTabPane.removeTab(tab);  }


    public TabPane<Widget> getTabPane() { return plotTabPane; }

    public MiniPlotWidget getMPW(TabPane.Tab tab) {
        return mpwMap.get(tab);
    }

    public String getCreateDesc() {
        return "Create New Tab";
    }

    public void prepare(final MiniPlotWidget mpw, final Vis.InitComplete initComplete) {
        mpw.getOps(new MiniPlotWidget.OpsAsync() {
            public void ops(final PlotWidgetOps widgetOps) {
                FFToolEnv.getHub().getCatalogDisplay().addPlotView(mpw.getPlotView());
                FFToolEnv.getHub().getDataConnectionDisplay().addPlotView(mpw.getPlotView(), Arrays.asList("target"));
                initComplete.done();
            }
        });

    }

    public WebPlotRequest customizeRequest(MiniPlotWidget mpw,WebPlotRequest wpr) {
        wpr.setZoomType(ZoomType.FULL_SCREEN);
        final int w= mpw.getOffsetWidth()<400 ? 400 : mpw.getOffsetWidth();
        final int h= mpw.getOffsetHeight()<400 ? 400 : mpw.getOffsetHeight();
        wpr.setZoomToWidth(w);
        wpr.setZoomToHeight(h);
        return wpr;

    }

    public boolean isPlottingExpanded() {
        return  !aloneUI.hasTableResults();
    }

    public TabPane<Widget> getPlotTabPane() {
        return plotTabPane;
    }

    public void delete(MiniPlotWidget mpw) {
        for(Map.Entry<TabPane.Tab,MiniPlotWidget> entry : mpwMap.entrySet()) {
            if (mpw==entry.getValue()) {
                plotTabPane.removeTab(entry.getValue());
                break;
            }
        }
        FFToolEnv.getHub().getDataConnectionDisplay().removePlotView(mpw.getPlotView());
    }

    public class PlotErrorHandler implements MiniPlotWidget.PlotError {
        public void onError(WebPlot wp, String briefDesc, String desc, String details, Exception e) {
            PopupUtil.showError("Plot Error", desc, details,false);
        }
    }
}

