package edu.caltech.ipac.firefly.ui;
/**
 * User: roby
 * Date: 9/14/11
 * Time: 10:57 AM
 */


import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.core.layout.Region;
import edu.caltech.ipac.firefly.resbundle.css.CssData;
import edu.caltech.ipac.firefly.resbundle.css.FireflyCss;
import edu.caltech.ipac.firefly.resbundle.images.IconCreator;
import edu.caltech.ipac.firefly.ui.input.CheckBoxGroupInputField;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.util.Dimension;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.util.dd.EnumFieldDef;

import java.util.ArrayList;
import java.util.List;

/**
* @author Trey Roby
*/
public class PopoutControlsUI {

    private static final int RESIZE_DELAY= 500;
    private static final IconCreator _ic = IconCreator.Creator.getInstance();
    private static final FireflyCss _ffCss = CssData.Creator.getInstance().getFireflyCss();

    private static final String DEFAULT_TILE_TITLE= "Tiled View";
    private static String _tiledTitle= DEFAULT_TILE_TITLE;
    private VerticalPanel _oneImageNavigationPanel= null;
    private HorizontalPanel _controlPanel= null;
    private HorizontalPanel _topBar= null;
    private VerticalPanel _headerBarControls= new VerticalPanel();
    private Label _goRight = new Label();
    private Label _goLeft = new Label();
    private Label _goRightArrow = new Label(">>");
    private Label _goLeftArrow = new Label("<<");

    private Grid _currentDisplayDots = new Grid(1,1);
    private MyDeckLayoutPanel _expandDeck= new MyDeckLayoutPanel();
    private MyGridLayoutPanel _expandGrid= new MyGridLayoutPanel();
    private final HTML _expandTitleLbl= new HTML();
    private final List<PopoutWidget> _expandedList;
    private final List<PopoutWidget> _originalExpandedList;
    private final PopoutWidget _popoutWidget;
    private final PopoutWidget.Behavior _behavior;
    private String _expandedTitle= "";
    private boolean _resizeZoomEnabled= true;


    public PopoutControlsUI(PopoutWidget popoutWidget,
                            PopoutWidget.Behavior behavior,
                            List<PopoutWidget> expandedList,
                            List<PopoutWidget> originalExpandedList) {
        _popoutWidget= popoutWidget;
        _expandedList= expandedList;
        _behavior= behavior;
        _originalExpandedList= originalExpandedList;
        initExpandControls();
    }

    public static void setTitledTitle(String title) {_tiledTitle= title; }

    void setResizeZoomEnabled(boolean enable) {
        _resizeZoomEnabled= enable;
    }

    private void initExpandControls() {
        _oneImageNavigationPanel= new VerticalPanel();
        _controlPanel= new HorizontalPanel();

        _expandDeck.setAnimationDuration(0);
        Image oneTile = new Image(_ic.getOneTile());
        oneTile.setPixelSize(24, 24);
        Image gridIcon= new Image(_ic.getGrid());
        gridIcon.setPixelSize(24,24);
        Image list= new Image(_ic.getList());
        list.setPixelSize(24,24);

        createOneImageNavigationPanel();

        final GwtUtil.ImageButton one= GwtUtil.makeImageButton(oneTile, "Show single image at full size");
        final GwtUtil.ImageButton grid= GwtUtil.makeImageButton(gridIcon, "Show all as tiles");
        final GwtUtil.ImageButton choiceList= GwtUtil.makeImageButton(list, "Choose which plots to show");

        one.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                int currIdx= 0;
                PopoutWidget currPopout= _behavior.chooseCurrentInExpandMode();
                if (currPopout!=null) currIdx= _expandedList.indexOf(currPopout);
                if (currIdx==-1) currIdx= 0;
                _popoutWidget.onePopout(currIdx);
            }
        });

        grid.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                _popoutWidget.gridPopout();
                updateDirLinks();

            }
        });

        choiceList.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                showPlotChoiceDialog();
            }
        });


        GwtUtil.setStyle(one, "marginLeft", "11px");
        GwtUtil.setStyle(grid, "marginLeft", "11px");
        GwtUtil.setStyle(choiceList, "marginLeft", "11px");


        GwtUtil.setStyles(_expandTitleLbl, "fontSize", "10pt",
                          "padding", "0px 0 0 5px",
                          "lineHeight", "1.2em",
                          "whiteSpace", "normal");


        _headerBarControls.add(_controlPanel);


        if (_popoutWidget.getPopoutContainer().getHeaderBar()==null) {
            _topBar= new HorizontalPanel();
            _topBar.add(_headerBarControls);
            _popoutWidget.getExpandRoot().addNorth(_topBar, PopoutWidget.CONTROLS_HEIGHT);
            if (!AllPlots.getInstance().isMenuBarPopup()) {
                _popoutWidget.getExpandRoot().addSouth(AllPlots.getInstance().getMenuBarInlineStatusLine(), 20);
                //.add();
            }
        }



        GwtUtil.setStyle(_controlPanel, "paddingTop", "2px");



//        _controlPanel.setSpacing(7);
        _controlPanel.add(one);
        _controlPanel.add(grid);
        _controlPanel.add(choiceList);
        _controlPanel.add(_oneImageNavigationPanel);
    }



    private void createOneImageNavigationPanel() {
        GwtUtil.setHidden(_oneImageNavigationPanel, true);




        ClickHandler goLeftHandler= new ClickHandler() {
            public void onClick(ClickEvent event) {
                int len = _expandDeck.getWidgetCount();
                int curr = _expandDeck.getVisibleWidgetIndex();
                int next = curr > 0 ? curr - 1 : len - 1;
                PopoutWidget currW = _expandedList.get(curr);
                PopoutWidget nextW = _expandedList.get(next);
                oneImagePage(currW,nextW);
            }
        };

        ClickHandler goRightHandler= new ClickHandler() {
            public void onClick(ClickEvent event) {
                int len= _expandDeck.getWidgetCount();
                int curr= _expandDeck.getVisibleWidgetIndex();
                int next= curr<len-1 ? curr+1 : 0;
                PopoutWidget currW= _expandedList.get(curr);
                PopoutWidget nextW= _expandedList.get(next);
                oneImagePage(currW,nextW);
            }
        };

        GwtUtil.makeIntoLinkButton(_goLeft,_goLeftArrow);
        GwtUtil.makeIntoLinkButton(_goRight,_goRightArrow);

        _goLeft.addClickHandler(goLeftHandler);
        _goLeftArrow.addClickHandler(goLeftHandler);

        _goRight.addClickHandler( goRightHandler);
        _goRightArrow.addClickHandler( goRightHandler);

        HorizontalPanel hp= new HorizontalPanel();

        HorizontalPanel hpLeft= new HorizontalPanel();
        HorizontalPanel hpRight= new HorizontalPanel();
        hpLeft.add(_goLeftArrow);
        hpLeft.add(_goLeft);
        hpRight.add(_goRight);
        hpRight.add(_goRightArrow);
        hp.add(hpLeft);
        hp.add(hpRight);


        GwtUtil.setStyles(_goLeftArrow, "paddingLeft", "11px",
                                        "textDecoration", "none",
                                        "fontSize", "10pt");

        GwtUtil.setStyles(_goLeft, "textOverflow", "ellipsis",
                                   "overflow", "hidden",
                                   "fontSize", "10pt");

        GwtUtil.setStyles(_goRight, "paddingLeft", "11px",
                                    "textOverflow", "ellipsis",
                                    "overflow", "hidden",
                                    "fontSize", "10pt");

        GwtUtil.setStyles(_goRightArrow, "textDecoration", "none",
                                         "fontSize", "10pt",
                                         "marginLeft", "-2px");
        hpRight.addStyleName("step-right");
        hpLeft.addStyleName("step-left");


        GwtUtil.setStyle(_oneImageNavigationPanel, "paddingLeft", "10px");

        _goLeft.setWidth("100px");
        _goRight.setWidth("100px");

        _oneImageNavigationPanel.add(hp);
        _oneImageNavigationPanel.add(_currentDisplayDots);
        _currentDisplayDots.setCellPadding(2);
        _oneImageNavigationPanel.setSpacing(2);
        _oneImageNavigationPanel.setCellHorizontalAlignment(_currentDisplayDots, HasHorizontalAlignment.ALIGN_CENTER);
    }

    void updateDirLinks() {
        int cnt= _expandDeck.getWidgetCount();
        if (_expandDeck.getWidgetCount()==1) {
            GwtUtil.setHidden(_oneImageNavigationPanel, true);
            GwtUtil.setHidden(_goLeft, true);
            GwtUtil.setHidden(_goLeftArrow, true);
            updateExpandedTitle(_expandedList.get(0));
        }
        else if (_expandDeck.getWidgetCount()>1) {
            GwtUtil.setHidden(_oneImageNavigationPanel, false);
            GwtUtil.setHidden(_goLeft, _expandDeck.getWidgetCount()<3);
            GwtUtil.setHidden(_goLeftArrow, _expandDeck.getWidgetCount()<3);
            int curr= _expandDeck.getVisibleWidgetIndex();

            PopoutWidget right= (curr!=cnt-1) ?  _expandedList.get(curr+1) : _expandedList.get(0);
            PopoutWidget left= (curr!=0) ? _expandedList.get(curr-1) : _expandedList.get(cnt-1);

            if (_expandDeck.getWidgetCount()>2) _goLeft.setText(left.getExpandedTitle());
            _goRight.setText(right.getExpandedTitle());
//            pos.setText("showing " + (curr + 1) + " of " + cnt);

            if (_currentDisplayDots.getColumnCount()!=cnt) {
                _currentDisplayDots.resize(1,cnt);
            }
            for(int i= 0; (i<cnt); i++) {
                if (i==curr) {
                    _currentDisplayDots.setWidget(0,i, new Image(_ic.getGreenDot()));
                }
                else {
                    Image im= new Image(_ic.getBlueDot());
                    im.addClickHandler(new DotClick(i));
                    _currentDisplayDots.setWidget(0, i, im);
                }
            }

            updateExpandedTitle(_expandedList.get(curr));

        }
        else {
            GwtUtil.setHidden(_oneImageNavigationPanel, true);
            GwtUtil.setHidden(_goLeft, true);
            GwtUtil.setHidden(_goLeftArrow, true);
        }
    }

    private void oneImagePage(PopoutWidget oldPW, PopoutWidget newPW) {
        Dimension d= _popoutWidget.getPopoutContainer().getAvailableSize();
        _behavior.onPrePageInExpandedMode(oldPW, newPW, d);
        _expandDeck.showWidget(_expandedList.indexOf(newPW));
        updateDirLinks();
        _behavior.onPostPageInExpandedMode(oldPW, newPW, d);
    }

    public void updateExpandedTitle(PopoutWidget popout) {
        if (popout.isExpandedAsGrid() && _expandedList.size()>1) {
            _expandedTitle= _tiledTitle;
            _expandTitleLbl.setHTML(_expandedTitle);
            popout.getPopoutContainer().setTitle(_expandTitleLbl);
        }
        else {
            _expandedTitle= popout.getExpandedTitle();
            _expandTitleLbl.setHTML(_expandedTitle+popout.getSecondaryTitle());
            popout.getPopoutContainer().setTitle(_expandTitleLbl);
        }
    }

    private void showPlotChoiceDialog() {
        PopoutWidget curr= null;
        if (PopoutWidget.getViewType()== PopoutWidget.ViewType.ONE) {
            int idx= _expandDeck.getVisibleWidgetIndex();
            if (idx>-1) {
                curr= _expandedList.get(idx);
            }
        }
        final PopoutWidget currPopout= curr;

        EnumFieldDef fd= new EnumFieldDef();
        List<EnumFieldDef.Item> inputs= new ArrayList<EnumFieldDef.Item>(_expandedList.size());
        fd.setOrientation(EnumFieldDef.Orientation.Vertical);
        int i= 0;
        String value= "";
        for(PopoutWidget p : _originalExpandedList) {
            EnumFieldDef.Item item= new EnumFieldDef.Item(i+"",p.getExpandedTitle());
            inputs.add(item);
            if (_expandedList.contains(p))  value+= i+",";
            i++;
        }
        value= value.substring(0,value.length()-1);
        fd.addItems(inputs);
        final SimpleInputField checkBoxes= new SimpleInputField(new CheckBoxGroupInputField(fd),
                                                                new SimpleInputField.Config(), false);
        checkBoxes.setValue(value);
        PopupUtil.showInputDialog(_popoutWidget.getExpandRoot(),"choose which", checkBoxes, new ClickHandler() {
            public void onClick(ClickEvent event) {
                String selectedAry[]= checkBoxes.getValue().split(",");
                _expandedList.clear();
                for(String s : selectedAry) {
                    _expandedList.add(_originalExpandedList.get(Integer.parseInt(s)));
                }
                if (PopoutWidget.getViewType()== PopoutWidget.ViewType.ONE || _expandedList.size()==1) {
                    int currIdx= _expandedList.indexOf(currPopout);
                    _popoutWidget.onePopout(currIdx>-1 ? currIdx : 0);
                }
                else if (PopoutWidget.getViewType()==PopoutWidget.ViewType.GRID) {
                    _popoutWidget.gridPopout();
                }
            }
        }, null );
    }


    void addHeaderBar() {
        Panel headerBar= _popoutWidget.getPopoutContainer().getHeaderBar();
        if (headerBar!=null)  headerBar.add(_headerBarControls);
    }

    void removeHeaderBar() {
        Panel headerBar= _popoutWidget.getPopoutContainer().getHeaderBar();
        if (headerBar!=null)  headerBar.remove(_headerBarControls);
        //todo: move the vis tool bar  help
        LayoutManager lm= Application.getInstance().getLayoutManager();
        if (!AllPlots.getInstance().isMenuBarPopup()) {
            Region helpReg= lm.getRegion(LayoutManager.VIS_MENU_HELP_REGION);
            helpReg.setDisplay(AllPlots.getInstance().getMenuBarInlineStatusLine());
        }
    }


    void reinit(PopoutWidget.ViewType viewType, DockLayoutPanel expandRoot) {
        expandRoot.clear();
        _expandGrid.clear();
        _expandDeck.clear();
        if (_topBar!=null) expandRoot.addNorth(_topBar, PopoutWidget.CONTROLS_HEIGHT);
        if (!AllPlots.getInstance().isMenuBarPopup()) {
            expandRoot.addSouth(AllPlots.getInstance().getMenuBarInlineStatusLine(),25);
        }
        if (viewType== PopoutWidget.ViewType.GRID) {
            expandRoot.add(_expandGrid);
            _expandGrid.setPixelSize(expandRoot.getOffsetWidth(), expandRoot.getOffsetHeight());
        }
        else if (viewType== PopoutWidget.ViewType.ONE) {
            expandRoot.add(_expandDeck);
            GwtUtil.setHidden(_controlPanel, _originalExpandedList.size() <= 1);
        }

    }

    public DeckLayoutPanel getDeck() { return _expandDeck; }
//    public MyGridLayoutPanel getGrid() { return _expandGrid; }


    public void resizeGrid(int rows, int cols) {
        _expandGrid.resize(rows, cols);
        _expandGrid.setCellPadding(20);
    }

    public void setGridWidget(int row, int col, Widget w) { _expandGrid.setWidget(row, col, w); }


    class MyDeckLayoutPanel extends DeckLayoutPanel {

        private OneResizeTimer _oneResizeTimer= new OneResizeTimer();

        @Override
        public void onResize() {
            Widget p= getParent();
            if (!GwtUtil.isOnDisplay(p)) return;
            super.onResize();    //To change body of overridden methods use File | Settings | File Templates.
            int w= p.getOffsetWidth();
            int h= (p.getOffsetHeight());

            int hAdjust= 0;
            if (!AllPlots.getInstance().isMenuBarPopup()) {
                hAdjust= 20;
            }
            for(PopoutWidget popout : _expandedList) {
                popout.getMovablePanel().setPixelSize(w, h-hAdjust);
                popout.getMovablePanel().forceLayout();
            }
            _oneResizeTimer.cancel();
            _oneResizeTimer.setupCall(w,h,_resizeZoomEnabled);
            _oneResizeTimer.schedule(RESIZE_DELAY);
        }
    }


    class MyGridLayoutPanel extends Grid implements RequiresResize {

        private GridResizeTimer _gridResizeTimer= new GridResizeTimer();
        private int _margin = 4;
        private int _panelMargin =14;
        public void onResize() {
            Widget p= getParent();
            if (!GwtUtil.isOnDisplay(p)) return;
            int rows= getRowCount();
            int cols= getColumnCount();
            int w= (p.getOffsetWidth() -_panelMargin)/cols -_margin;
            int h= (p.getOffsetHeight()-_panelMargin)/rows -_margin;
            this.setPixelSize(w, h);
            this.setPixelSize(w,h);
            for(PopoutWidget popout : _expandedList) {
                popout.getMovablePanel().setPixelSize(w, h);
                popout.getMovablePanel().forceLayout();
            }
            _gridResizeTimer.cancel();
            _gridResizeTimer.setupCall(w,h, _resizeZoomEnabled);
            _gridResizeTimer.schedule(RESIZE_DELAY);
        }
    }

    private class GridResizeTimer extends Timer {
        private int w= 0;
        private int h= 0;
        private boolean adjustZoom;

        public void setupCall(int w, int h, boolean adjustZoom) {
            this.w= w;
            this.h= h;
            this.adjustZoom = adjustZoom;
        }

        @Override
        public void run() {
            for(PopoutWidget popout : _expandedList) {
                _behavior.onResizeInExpandedMode(popout, new Dimension(w,h), PopoutWidget.ViewType.GRID, adjustZoom);
            }
        }
    }


    private class OneResizeTimer extends Timer {
        private int w= 0;
        private int h= 0;
        private boolean adjustZoom;

        public void setupCall(int w, int h, boolean adjustZoom) {
            this.w= w;
            this.h= h;
            this.adjustZoom = adjustZoom;
        }

        @Override
        public void run() {
            int curr= _expandDeck.getVisibleWidgetIndex();
            if (curr>-1) {
                PopoutWidget popout= _expandedList.get(curr);
                _behavior.onResizeInExpandedMode(popout, new Dimension(w,h), PopoutWidget.ViewType.ONE, adjustZoom);
            }
        }
    }




    private class DotClick implements ClickHandler {
        private final int _idx;

        public DotClick(int idx) { _idx= idx;}

        public void onClick(ClickEvent event) {
            int curr = _expandDeck.getVisibleWidgetIndex();
            PopoutWidget currW = _expandedList.get(curr);
            PopoutWidget nextW = _expandedList.get(_idx);
            oneImagePage(currW,nextW);
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
