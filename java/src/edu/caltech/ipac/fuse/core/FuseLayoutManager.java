package edu.caltech.ipac.fuse.core;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.HtmlRegionLoader;
import edu.caltech.ipac.firefly.core.LayoutElement;
import edu.caltech.ipac.firefly.core.layout.BaseRegion;
import edu.caltech.ipac.firefly.core.layout.IrsaLayoutManager;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.core.layout.Region;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.gwtclone.SplitLayoutPanelFirefly;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.ui.table.TableResultsDisplay;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.ui.searchui.CoverageResultsDisplay;
import edu.caltech.ipac.firefly.ui.searchui.ImageDataResultsDisplay;
import edu.caltech.ipac.firefly.ui.searchui.XYPlotResultsDisplay;

/**
 * Date: Sep. 9, 2013
 *
 * @author loi
 * @version $Id: HeritageLayoutManager.java,v 1.43 2011/10/21 00:14:03 loi Exp $
 */
public class FuseLayoutManager extends IrsaLayoutManager {
    private LayoutElement tableArea;
    private LayoutElement xyplotArea;
    private LayoutElement coverageArea;
    private LayoutElement imDataArea;
    private VType currentViewType;
    private SplitLayoutPanelFirefly resultsPane;
//    private SplitLayoutPanelFirefly imagePane = new SplitLayoutPanelFirefly();
    private final TabPane<Widget> imageTabPane= new TabPane<Widget>();

    private TabPane.Tab imDataTab;
    private TabPane.Tab coverageDataTab;

    public enum VType {QUADVIEW, TRIVIEW, IMAGE_TABLE, XYPLOT_TABLE}

    public FuseLayoutManager() {
        super();
    }

    public void layout(String root) {
        super.layout(root);
        resultsPane = new SplitLayoutPanelFirefly();
        resultsPane.setHeight("100%");
        tableArea = new TableResultsDisplay();
        xyplotArea = new XYPlotResultsDisplay();
        coverageArea = new CoverageResultsDisplay();
        imDataArea = new ImageDataResultsDisplay();

//        imagePane.addSouth(coverageArea.getDisplay(),400);
//        imagePane.add(imDataArea.getDisplay());

        imDataTab = imageTabPane.addTab(imDataArea.getDisplay(), "image data", "Image Data",false);
        coverageDataTab = imageTabPane.addTab(coverageArea.getDisplay(), "Coverage", "Coverage",false);

        imageTabPane.addSelectionHandler(new SelectionHandler<Integer>() {
            public void onSelection(SelectionEvent<Integer> event) {
                TabPane.Tab tab= imageTabPane.getSelectedTab();
                if (tab==imDataTab) {
                    imDataArea.show();
                }
                else if (tab==coverageDataTab) {
                    coverageArea.show();
                }
            }
        });

        resultsPane.addWest(imageTabPane,700);
        resultsPane.addSouth(xyplotArea.getDisplay(), 400);
        resultsPane.add(tableArea.getDisplay());

        currentViewType = VType.TRIVIEW;
        tableArea.addChangeListener(new ContentChangedHandler());
        getResult().setDisplay(resultsPane);
    }

    @Override
    protected Widget makeSouth() {
        Widget s = super.makeSouth();
        HtmlRegionLoader footer= new HtmlRegionLoader();
        footer.load("irsa_footer_minimal.html",LayoutManager.FOOTER_REGION);
        return s;
    }

    @Override
    protected Region makeResult() {
        BaseRegion r = new BaseRegion(RESULT_REGION);
        return r;
    }

    public void redraw() {
        switchView(currentViewType);
    }

    public void switchView(VType type) {
        AllPlots.getInstance().getVisMenuBar();
        currentViewType = type;

        if ((type == VType.TRIVIEW || type == VType.XYPLOT_TABLE) && xyplotArea.hasContent()) {
            if (!xyplotArea.isShown()) {
                xyplotArea.show();
                GwtUtil.DockLayout.showWidget(resultsPane, xyplotArea.getDisplay());
            }
        } else {
            xyplotArea.hide();
            GwtUtil.DockLayout.hideWidget(resultsPane, xyplotArea.getDisplay());
        }

        if ((type == VType.TRIVIEW || type == VType.IMAGE_TABLE) && coverageArea.hasContent()) {
            if (!coverageArea.isShown()) {
                coverageArea.show();
//                GwtUtil.DockLayout.showWidget(imagePane, coverageArea.getDisplay());
            }
        } else {
            coverageArea.hide();
//            GwtUtil.DockLayout.hideWidget(imagePane, coverageArea.getDisplay());
        }


        if (tableArea.hasContent()) {
            if (!tableArea.isShown()) {
                tableArea.show();
                GwtUtil.DockLayout.showWidget(resultsPane, tableArea.getDisplay());
            }
        } else {
            tableArea.hide();
            GwtUtil.DockLayout.hideWidget(resultsPane, tableArea.getDisplay());
        }

    }

//    public void switchView(VType type) {
//        AllPlots.getInstance().getVisMenuBar();
//        currentViewType = type;
//
//        if ((type == VType.QUADVIEW || type == VType.XYPLOT_TABLE) && xyplotArea.hasContent()) {
//            if (!xyplotArea.isShown()) {
//                xyplotArea.show();
//                GwtUtil.DockLayout.showWidget(resultsPane, xyplotArea.getDisplay());
//            }
//        } else {
//            xyplotArea.hide();
//            GwtUtil.DockLayout.hideWidget(resultsPane, xyplotArea.getDisplay());
//        }
//
//        if ((type == VType.QUADVIEW || type == VType.IMAGE_TABLE) && coverageArea.hasContent()) {
//            if (!coverageArea.isShown()) {
//                coverageArea.show();
//                GwtUtil.DockLayout.showWidget(imagePane, coverageArea.getDisplay());
//            }
//        } else {
//            coverageArea.hide();
//            GwtUtil.DockLayout.hideWidget(imagePane, coverageArea.getDisplay());
//        }
//
//
//        if (tableArea.hasContent()) {
//            if (!tableArea.isShown()) {
//                tableArea.show();
//                GwtUtil.DockLayout.showWidget(resultsPane, tableArea.getDisplay());
//            }
//        } else {
//            tableArea.hide();
//            GwtUtil.DockLayout.hideWidget(resultsPane, tableArea.getDisplay());
//        }
//
//    }

    class ContentChangedHandler implements LayoutElement.ChangeListner {

        public void onContentChanged() {
            if (tableArea.hasContent()) {
                redraw();
                resultsPane.setVisible(true);
            } else {
                resultsPane.setVisible(false);
            }
        }

        public void onShow() {}

        public void onHide() {}
    }
}
