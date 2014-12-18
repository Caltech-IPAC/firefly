package edu.caltech.ipac.fftools.core;
/**
 * User: roby
 * Date: 5/6/13
 * Time: 1:54 PM
 */


import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.fftools.FFToolEnv;
import edu.caltech.ipac.firefly.resbundle.images.IconCreator;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopoutWidget;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;

/**
 * @author Trey Roby
 */
public class StandaloneViewControls {

    private HorizontalPanel panel= new HorizontalPanel();
    private GwtUtil.ImageButton one;
    private GwtUtil.ImageButton grid;
    private GwtUtil.ImageButton tableImage;
    private ChangeListener listener= new ChangeListener();
    private Label title;


    StandaloneViewControls () {
       init();
    }

    public Widget getWidget() {
        return panel;
    }

    private void init() {
        IconCreator _ic = IconCreator.Creator.getInstance();
        Image oneTile = new Image(_ic.getOneTile());
        oneTile.setPixelSize(24, 24);
        Image gridIcon= new Image(_ic.getGrid());
        gridIcon.setPixelSize(24,24);
        Image tableImageIcon= new Image(_ic.getTableImage());
        gridIcon.setPixelSize(24,24);

        one= GwtUtil.makeImageButton(oneTile, "Single View: Show single image at full size");
        grid= GwtUtil.makeImageButton(gridIcon, "Tile View: Show all as tiles");
        tableImage= GwtUtil.makeImageButton(tableImageIcon, "Table View: Show the table and images side-by-side");

        GwtUtil.setStyle(one,        "padding", "0 3px 0 3px");
        GwtUtil.setStyle(grid,       "padding", "0 3px 0 3px");
        GwtUtil.setStyle(tableImage, "padding", "0 3px 0 3px");


        one.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                switchToOne();
            }
        });

        grid.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                switchToGrid();
            }
        });

        tableImage.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                switchToTableImage();
            }
        });

        AllPlots ap= AllPlots.getInstance();
        ap.addListener(Name.FITS_VIEWER_ADDED,        listener);
        ap.addListener(Name.FITS_VIEWER_REMOVED,      listener);
        ap.addListener(Name.ALL_FITS_VIEWERS_TEARDOWN,listener);
        FFToolEnv.getHub().getEventManager().addListener(EventHub.ON_TABLE_ADDED,listener);
        FFToolEnv.getHub().getEventManager().addListener(EventHub.ON_TABLE_REMOVED,listener);

        title= new Label("View Mode: ");
        GwtUtil.setStyles(title, "padding", "5px 0 0 15px",
                                 "fontSize",   "13px");

        panel.add(title);
        panel.add(one);
        panel.add(grid);
        panel.add(tableImage);

        GwtUtil.setStyles(panel, "padding", "3px 0 0 0",
                                 "fontSize",   "13px");

        tableImage.setVisible(false);
        grid.setVisible(false);
        one.setVisible(false);
        title.setVisible(false);

    }

    private void switchToOne() {
        AllPlots ap= AllPlots.getInstance();
        if (ap.getMiniPlotWidget()!=null) {
            if (ap.isExpanded()) {
               PopoutWidget w= ap.getExpandedController();
                w.forceSwitchToOne();
            }
            else {
                PopoutWidget.setViewType(PopoutWidget.ViewType.ONE);
                ap.forceExpand(ap.getMiniPlotWidget());
            }
        }
    }

    private void switchToGrid() {
        AllPlots ap= AllPlots.getInstance();
        if (ap.getMiniPlotWidget()!=null) {
            if (ap.isExpanded()) {
                PopoutWidget w= ap.getExpandedController();
                w.forceSwitchToGrid();
            }
            else {
                PopoutWidget.setViewType(PopoutWidget.ViewType.GRID);
                ap.forceExpand(ap.getMiniPlotWidget());

            }
        }
    }

    private void switchToTableImage() {
        Application.getInstance().getToolBar().getDropdown().close();
//        AllPlots.getInstance().forceCollapse();
    }

    private void computeVisibleButtons() {
        int plotSize= AllPlots.getInstance().getAll().size();
        int tabSize=  FFToolEnv.getHub().getTables().size();
        one.setVisible(plotSize>1);
        grid.setVisible(plotSize>1);
        tableImage.setVisible(tabSize>0);
        title.setVisible(plotSize>1 || tabSize>0);
    }

    private class ChangeListener implements WebEventListener {
        public void eventNotify(WebEvent ev) {
            Name name= ev.getName();
            if (name.equals(Name.FITS_VIEWER_ADDED)              ||
                name.equals(Name.FITS_VIEWER_REMOVED)            ||
                name.equals(EventHub.ON_TABLE_ADDED) ||
                name.equals(EventHub.ON_TABLE_REMOVED) ) {
                computeVisibleButtons();
            }
        }
    }

}

