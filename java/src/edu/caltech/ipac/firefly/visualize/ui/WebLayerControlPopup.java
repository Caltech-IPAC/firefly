package edu.caltech.ipac.firefly.visualize.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.resbundle.css.CssData;
import edu.caltech.ipac.firefly.resbundle.css.FireflyCss;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.draw.SubgroupVisController;
import edu.caltech.ipac.firefly.visualize.draw.WebLayerItem;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * User: roby
 * Date: Nov 16, 2009
 * Time: 9:20:24 AM
 */


/**
 * @author Trey Roby
 */
public class WebLayerControlPopup extends PopupPane {

    private static final int ON_COL= 0;
    private static final int COLOR_FEEDBACK= 1;
    private static final int COLOR_COL= 2;
    private static final int USER_DEFINED_COL= 3;
    private static final int DELETE_COL= 4;
    private static final int DETAILS_COL= 5;
    private static final int LAYERS= 0;
    private static final int NO_LAYERS= 1;
    private static final int FIRST_LINE_CELL_CNT= 3;


    private static final String ALL= "all";
    private static final String GROUP= "row";
    private static final String IMAGE = "image";
    private static final List<String> sgOps= Arrays.asList(ALL, GROUP, IMAGE);


    private static final FireflyCss _ffCss = CssData.Creator.getInstance().getFireflyCss();
    private static final WebClassProperties _prop=
            new WebClassProperties(WebLayerControlPopup.class);
    private final DeckPanel _panel= new DeckPanel();
    private Map<Widget,WebLayerItem> _layerMap= new HashMap<Widget,WebLayerItem>();
    private FlexTable _layerTable= new FlexTable();
    private VerticalPanel _layerMaster= new VerticalPanel();
    private final LayerListener _listener= new LayerListener(this);
    private Label _showMenu;
    private Widget lastBottomWidget= null;



    private WebLayerControlPopup() {
        super(_prop.getTitle(),null,false,false);
        setWidget(_panel);
        createContents();
        alignTo(RootPanel.get(), PopupPane.Align.TOP_RIGHT, 0, 70);

        AllPlots.getInstance().addListener(Name.FITS_VIEWER_CHANGE, new WebEventListener() {
            public void eventNotify(WebEvent ev) { if (isPopupShowing()) redrawAll(); }
        });

        AllPlots.getInstance().addListener(Name.LAYER_ITEM_UI_CHANGE, new WebEventListener() {
            public void eventNotify(WebEvent ev) { if (isPopupShowing()) redrawAll(); }
        });

        _panel.addDomHandler(new MouseOverHandler() {
            public void onMouseOver(MouseOverEvent event) {
                AllPlots.getInstance().suggestHideMouseReadout();
            }
        }, MouseOverEvent.getType());
    }




//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    private void redrawAll() {
        MiniPlotWidget mpw= AllPlots.getInstance().getMiniPlotWidget();
        if (mpw!=null) {
            WebPlotView pv= mpw.getPlotView();
//            alignTo(pv,  PopupPane.Align.TOP_LEFT_POPUP_RIGHT, -25,0);
//            alignToCenter();
                    _layerMap.clear();
            _layerTable.clear();
            _layerTable.removeAllRows();

            for(WebLayerItem item : pv.getUserDrawerLayerSet()) {
                if (item.isActive()) addLayer(item);
            }
            if (mpw.getTitle()!=null && mpw.getTitle().length()>0) setHeader(_prop.getTitle()+"- "+mpw.getTitle());
        }
    }


    @Override
    protected void onClose() {
//        AlertLayerPopup.setLayerDialogVisibleStatus(false);
    }

    @Override
    public void show() {
        super.show();    //To change body of overridden methods use File | Settings | File Templates.
        _showMenu.setVisible(!AllPlots.getInstance().isMenuBarVisible());
        redrawAll();
//        AlertLayerPopup.setLayerDialogVisibleStatus(true);
    }

    private void createContents() {

        AllPlots ap= AllPlots.getInstance();
        ap.addListener(Name.LAYER_ITEM_ADDED, _listener);
        ap.addListener(Name.LAYER_ITEM_REMOVED,_listener);
        ap.addListener(Name.LAYER_ITEM_ACTIVE,_listener);

        Label noLayerLabel= new Label(_prop.getName("noLayers"));
        DOM.setStyleAttribute(noLayerLabel.getElement(), "padding", "5px");
        _layerMaster.add(_layerTable);
        _panel.add(_layerMaster);
        _panel.add(noLayerLabel);
        _panel.showWidget(NO_LAYERS);
        GwtUtil.setStyle(_layerMaster, "padding", "5px 10px 0px 10px");


        _showMenu= GwtUtil.makeLinkButton("More Tools", "Show visualization tool bar", new ClickHandler() {
            public void onClick(ClickEvent event) {
                AllPlots ap= AllPlots.getInstance();
                ap.toggleShowMenuBarPopup(ap.getMiniPlotWidget());
            }
        });

        AllPlots.getInstance().addListener(Name.VIS_MENU_BAR_POP_SHOWING,
                                   new WebEventListener<Boolean>() {
                                       public void eventNotify(WebEvent<Boolean> ev) {
                                           boolean showing= ev.getData();
                                           _showMenu.setVisible(!showing);
                                       }
                                   });

        _layerMaster.add(_showMenu);
        _layerMaster.setCellHorizontalAlignment(_showMenu,VerticalPanel.ALIGN_RIGHT);
        GwtUtil.setStyle(_showMenu, "padding", "0px 0px 5px 0px");


        redrawAll();
    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void removeLayer(WebLayerItem item) {

        WebLayerItem testItem;
        int rowCnt= _layerTable.getRowCount();
        for(int i=0; (i<rowCnt); i++) {
            Widget w= _layerTable.getWidget(i,ON_COL);
            if (w!=null && w instanceof CheckBox) {
                testItem= _layerMap.get(w);
                if (item.getID().equals(testItem.getID())) {
                    _layerMap.remove(w);
                    _layerTable.removeRow(i); // remove the check box row
                    _layerTable.removeRow(i); // remove help row, index the same as before
                    if (item.makeExtraUI()!=null) {
                        _layerTable.removeRow(i); // remove extra UI row, index the same as before
                    }
                    if (item.isUsingSubgroups()) {
                        _layerTable.removeRow(i);
                    }
                    break;
                }
            }
        }

        if (_layerMap.size()==0) _panel.showWidget(NO_LAYERS);

        if (_layerTable.getRowCount()>0) {
            lastBottomWidget= _layerTable.getWidget(_layerTable.getRowCount()-1,0);
            if (lastBottomWidget!=null) {
                GwtUtil.setStyle(lastBottomWidget, "borderBottom",  "none");
            }
        }

    }


    private void addLayer(final WebLayerItem item) {

        if (lastBottomWidget!=null) {
            GwtUtil.setStyles(lastBottomWidget, "borderBottom",  "1px solid rgba(0,0,0,.3)",
                                                "marginBottom",  "7px");
        }
        int activeRow= _layerTable.getRowCount();

        _panel.showWidget(LAYERS);


        String name= _prop.getName("on")+ " " + item.getTitle();
        String tip= _prop.getTip("on") + " " + item.getTitle();
        final CheckBox cb= GwtUtil.makeCheckBox(name,tip,true);
        DOM.setStyleAttribute(cb.getElement(), "paddingRight", "15px");
        cb.setValue(item.isVisible());
        cb.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            public void onValueChange(ValueChangeEvent<Boolean> ev) {
                item.setVisible(cb.getValue());
            }
        });
        item.addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent<String> ev) {
                cb.setHTML(_prop.getName("on")+ " " + item.getTitle());
            }
        });
        item.getPlotView().addListener(Name.LAYER_ITEM_VISIBLE,new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                if (ev.getSource()==item) {
                    boolean v= (Boolean)ev.getData();
                    if (v!=cb.getValue()) cb.setValue(v);
                }
            }
        });

        Label colorFeedback= new Label();

        _layerTable.setWidget(activeRow,ON_COL,cb);
        if (item.getHasColorSetting()) {
            _layerTable.setWidget(activeRow,COLOR_FEEDBACK,colorFeedback);
            _layerTable.setWidget(activeRow,COLOR_COL,makeChangeColorLink(colorFeedback, item));
        }
        Widget userDefined= item.makeUserDefinedColUI();
        if (userDefined!=null) {
            _layerTable.setWidget(activeRow,USER_DEFINED_COL,userDefined);
        }
        if (item.getHasDelete()) {
//            int column= item.getHasColorSetting() ? DELETE_COL : COLOR_COL;
            _layerTable.setWidget(activeRow,DELETE_COL,makeDeleteLink(item));
        }
        if (item.getHasDetails()) {
            _layerTable.setWidget(activeRow,DETAILS_COL,makeDetailsLink(item));
        }
//        _layerTable.setWidget(rowCnt,HELP_COL,makeHelpLink(item));
        activeRow++;




        Widget extra= item.makeExtraUI();
        if (extra!=null) {
            SimplePanel panel= new SimplePanel();
            panel.setWidget(extra);
            DOM.setStyleAttribute(panel.getElement(), "padding", "0 0 0 25px");
            _layerTable.setWidget(activeRow,0,panel);
            _layerTable.getFlexCellFormatter().setColSpan(activeRow,0,4);
            activeRow++;
        }

        if (item.isUsingSubgroups()) {
            SimplePanel panel= new SimplePanel();
            DOM.setStyleAttribute(panel.getElement(), "padding", "0 0 0 25px");
            _layerTable.setWidget(activeRow,0,panel);
            SimpleInputField field= GwtUtil.createRadioBox("Overlay", sgOps,
                                                           getCheckBoxValue(item.getSubgroupVisibility()) , true);
            panel.setWidget(field);
            _layerTable.getFlexCellFormatter().setColSpan(activeRow,0,4);
            activeRow++;
            field.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
                public void onValueChange(ValueChangeEvent<String> ev) {
                    item.setVisible( getLevel(ev.getValue()));
                }
            });
        }


        String helpStr= item.getHelp();
        if (helpStr==null) helpStr= "";
//        Label help= new Label();
        HTML help= new HTML();
        help.setHTML(helpStr);
        DOM.setStyleAttribute(help.getElement(), "padding", "2px 0 2px 25px");
        DOM.setStyleAttribute(help.getElement(), "fontSize", "90%");
        help.addStyleName(_ffCss.fadedText());
        _layerTable.setWidget(activeRow,0,help);
        _layerTable.getFlexCellFormatter().setColSpan(activeRow,0,3);

        lastBottomWidget= help;

        _layerMap.put(cb,item);

    }

    private SubgroupVisController.Level getLevel(String cbValue) {
        SubgroupVisController.Level retval= SubgroupVisController.Level.ALL;
        if (cbValue.equals(ALL)) retval= SubgroupVisController.Level.ALL;
        else if (cbValue.equals(GROUP)) retval= SubgroupVisController.Level.SUBGROUP;
        else if (cbValue.equals(IMAGE)) retval= SubgroupVisController.Level.PLOT_VIEW;
        return retval;
    }

    private String getCheckBoxValue(SubgroupVisController.Level level) {
        String retval= ALL;
        switch (level) {
            case ALL: retval= ALL; break;
            case SUBGROUP: retval= GROUP; break;
            case PLOT_VIEW: retval= IMAGE; break;
        }
        return retval;
    }




    private Widget makeChangeColorLink(Label colorFeedback, WebLayerItem item) {
        ClickHandler colorChange= new ColorChange(colorFeedback, item);
        colorFeedback.setText(" ");
        colorFeedback.addClickHandler(colorChange);
        Widget link= GwtUtil.makeLinkButton(_prop.makeBase("color"),colorChange);
        colorFeedback.setSize("10px", "10px");
        return link;
    }

    private Widget makeDeleteLink(final WebLayerItem item) {
        Widget link= GwtUtil.makeLinkButton("Delete", "Delete: "+item.getTitle(), new ClickHandler() {
            public void onClick(ClickEvent event) {
                item.suggestDelete();
            }
        });
        return link;
    }
    private Widget makeDetailsLink(final WebLayerItem item) {
        Widget link= GwtUtil.makeLinkButton("Details", "Detail for "+item.getTitle(), new ClickHandler() {
            public void onClick(ClickEvent event) {
                item.showDetails();
            }
        });
        return link;
    }


//    private Widget makeHelpLink(final WebLayerItem item) {
//        String name= _prop.getName("help");
//        String tip= _prop.getTip("help") + " " + item.getTitle();
//
//        Widget link= GwtUtil.makeLinkButton(name,tip,new ClickHandler() {
//            public void onClick(ClickEvent event) {
//                PopupUtil.showInfo(_layerTable,"Help",item.getHelp());
//            }
//        });
//
//        return link;
//    }
//

// =====================================================================
// -------------------- Inner Classes ----------------------------------
// =====================================================================


    private static class ColorChange implements ClickHandler {

        private WebLayerItem _item;
        private Widget _colorFeedback;


        public ColorChange(Widget colorFeedback, WebLayerItem item) {
            _colorFeedback= colorFeedback;
            _item= item;
            DOM.setStyleAttribute(_colorFeedback.getElement(),
                                  "backgroundColor", item.getAutoColorInterpreted());
        }


        public void onClick(ClickEvent ev) {
            String color= _item.getColor();
            ColorPickerDialog.chooseColor(_colorFeedback,_prop.getTitle("colorChooser") + _item.getTitle(),color,
                                          new ColorPickerDialog.ColorChoice() {
                                              public void choice(String color) {
                                                  if (color!=null && GwtUtil.isHexColor(color)) {
                                                      _item.setColor(color);
//                                                      DOM.setStyleAttribute(w.getElement(),"background", color);
                                                      DOM.setStyleAttribute(_colorFeedback.getElement(),
                                                              "backgroundColor", "#" +color);
                                                  }
                                              }
                                          });
        }
    }







    /**
     * making this class static and passing a parameter makes code splitting happen better
     */
    private class LayerListener implements WebEventListener {

        private WebLayerControlPopup _popup;

        LayerListener(WebLayerControlPopup popup)  { _popup=popup; }

        public void eventNotify(final WebEvent ev) {
            if (ev.getSource() instanceof WebPlotView  && isPopupShowing()) {
                WebPlotView pv= (WebPlotView)ev.getSource();
                WebLayerItem layer= (WebLayerItem )ev.getData();
                if (pv == AllPlots.getInstance().getPlotView()) {
                    if (ev.getName().equals(Name.LAYER_ITEM_ADDED)) {
                        _popup.addLayer(layer);
                    }
                    else if (ev.getName().equals(Name.LAYER_ITEM_REMOVED)) {
                        _popup.removeLayer(layer);
                    }
                    else if (ev.getName().equals(Name.LAYER_ITEM_ACTIVE)) {
                        if (layer.isActive()) _popup.addLayer(layer);
                        else                  _popup.removeLayer(layer);
                    }
                }
            }
        }
    }


    public static class AsyncCreator {
        private WebLayerControlPopup _dialog= null;


        public AsyncCreator() { }


        public void showOrHide() {
            GWT.runAsync( new GwtUtil.DefAsync() {
                public void onSuccess() {
                    if (_dialog==null) {
                        _dialog= new WebLayerControlPopup();
                    }
                    _dialog.showOrHide();
                }
            });
        }


    }




}

