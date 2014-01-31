package edu.caltech.ipac.fuse.ui;
/**
 * User: roby
 * Date: 1/28/14
 * Time: 1:18 PM
 */


import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.DataSetInfo;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.SpacialType;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.SimpleTargetPanel;
import edu.caltech.ipac.firefly.ui.input.AsyncInputFieldGroup;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.WebClassProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class SpacialSelectUI extends Composite implements AsyncInputFieldGroup {

    private static final int HEIGHT_REQUIRED= 225;
    private static final int PADDING_TOP= 12;
    public static final int MIN_HEIGHT_REQUIRED= HEIGHT_REQUIRED+PADDING_TOP;

    private static final String CONE= "cone";
    private static final String ELLIPTICAL= "elliptical";
    private static final String BOX= "box";



    private static final int CONE_IDX= 0;
    private static final int ELLIPTICAL_IDX= 1;
    private static final int BOX_IDX= 2;


    private static final String UPLOAD= "upload";
    private static final String PREV_SEARCH= "prevSearch";
    private static final String COORDS= "coords";


    private static final int UPLOAD_IDX= 0;
    private static final int PREV_SEARCH_IDX= 1;
    private static final int COORDS_IDX= 2;


    private SpacialBehaviorPanel singleSpacialPanels[];
    private SpacialBehaviorPanel multiSpacialPanels[];
    private SpacialBehaviorPanel polygonPanel;
    private static final Map<SpacialType,SpatialOps> spacialOpsMap= new HashMap<SpacialType, SpatialOps>(15);

    private static final WebClassProperties _prop = new WebClassProperties(SpacialSelectUI.class);
    private List<DataSetInfo> dataSetList= new ArrayList<DataSetInfo>(100);

    enum TabMode { SINGLE, MULTI, POLYGON, ALL_SKY}
    FlowPanel mainPanel= new FlowPanel();
    private final SimpleInputField singleSearchMethod =
            SimpleInputField.createByProp(
                    _prop.makeBase("singleTargetMethod"), new SimpleInputField.Config("1px"), false);
    private final SimpleInputField multiSearchMethod =
            SimpleInputField.createByProp(
                    _prop.makeBase("multiMethod"), new SimpleInputField.Config("1px"), false);


    private Widget singleTarget;
    private Widget polyTarget;
    private Widget multiTarget;
    private Widget allSky;
    private DeckLayoutPanel singleModeOpsPanel= new DeckLayoutPanel();
    private DeckLayoutPanel multiModeOpsPanel= new DeckLayoutPanel();
    private TabPane<Widget> spacialOpsTabs = new TabPane<Widget>();
    private TabPane.Tab<Widget> singleTab= null;
    private TabPane.Tab<Widget> polyTab= null;
    private TabPane.Tab<Widget> multiTab= null;
    private TabPane.Tab<Widget> allSkyTab= null;
    SpacialBehaviorPanel.Cone multiCone=  new SpacialBehaviorPanel.Cone();

    private TabMode selectedMode= TabMode.SINGLE;
    private SimpleTargetPanel targetPanel = new SimpleTargetPanel();


    SpacialSelectUI() {
        initWidget(mainPanel);
        makeUI();

    }


    private void populateDataSetList() {
        if (dataSetList.size()==0) {

            //todo async task - then call init
        }
        else {

        }

    }




    public void makeUI() {




        spacialOpsTabs.addSelectionHandler(new SelectionHandler<Integer>() {
            public void onSelection(SelectionEvent<Integer> event) {
                updateSelectedTab();
            }
        });


        makeSingle();
        makeMulti();
        makePolygon();
        makeAllSky();


//        makeSearchRegionOptions();


        reinitTabPane(true, true, true, true);
        mainPanel.add(spacialOpsTabs);
        spacialOpsTabs.setSize("100%", "100%");
//        mainPanel.setSize("700px", "245px");
        mainPanel.setSize("600px", HEIGHT_REQUIRED+"px");
        GwtUtil.setStyle(mainPanel,"paddingTop", PADDING_TOP+"px");
    }


    private void reinitTabPane(boolean singleOp, boolean multiOp, boolean polyOp, boolean allSkyOp) {
        TabPane.Tab<Widget> selectedTab= spacialOpsTabs.getSelectedTab();
        if (singleTab!=null) {
            if (selectedTab==singleTab) selectedMode= TabMode.SINGLE;
            spacialOpsTabs.removeTab(singleTab);
            singleTab= null;
        }
        if (polyTab!=null) {
            if (selectedTab==polyTab) selectedMode= TabMode.POLYGON;
            spacialOpsTabs.removeTab(polyTab);
            polyTab= null;
        }
        if (multiTab!=null) {
            if (selectedTab==multiTab) selectedMode= TabMode.MULTI;
            spacialOpsTabs.removeTab(multiTab);
            multiTab= null;
        }
        if (allSkyTab!=null) {
            if (selectedTab==allSkyTab) selectedMode= TabMode.ALL_SKY;
            spacialOpsTabs.removeTab(allSkyTab);
            allSkyTab= null;
        }


        if (singleOp) {
            singleTab= spacialOpsTabs.addTab(singleTarget, "Single");
            if (selectedMode== TabMode.SINGLE) selectedTab= singleTab;
        }
        if (polyOp) {
            polyTab= spacialOpsTabs.addTab(polyTarget, "Polygon");
            if (selectedMode== TabMode.POLYGON) selectedTab= polyTab;
        }
        if (multiOp) {
            multiTab= spacialOpsTabs.addTab(multiTarget, "Multi");
            if (selectedMode== TabMode.MULTI) selectedTab= multiTab;
        }
        if (allSkyOp) {
            allSkyTab= spacialOpsTabs.addTab(allSky, "All Sky");
            if (selectedMode== TabMode.ALL_SKY) selectedTab= allSkyTab;
        }
        spacialOpsTabs.selectTab(selectedTab);
    }

    private void updateSelectedTab() {
        TabPane.Tab<Widget> selectedTab= spacialOpsTabs.getSelectedTab();
        if      (selectedTab==singleTab) selectedMode= TabMode.SINGLE;
        else if (selectedTab==multiTab) selectedMode= TabMode.MULTI;
        else if (selectedTab==allSkyTab) selectedMode= TabMode.ALL_SKY;
    }


    private void makeMulti() {
//        multiTarget= new HTML("Place holder for multi target. Multi target will support:<br>"+
//                                      "File upload<br>" +
//                                      "Specifying a previous search<br>" +
//                                      "Entering multiple points");

        SpacialBehaviorPanel.TableUpload upload=  new SpacialBehaviorPanel.TableUpload();
        SpacialBehaviorPanel.PrevSearch prevSearch= new SpacialBehaviorPanel.PrevSearch();
        SpacialBehaviorPanel.UserEnteredCoords coords= new SpacialBehaviorPanel.UserEnteredCoords();

        multiSpacialPanels= new SpacialBehaviorPanel[3];
        multiSpacialPanels[UPLOAD_IDX]= upload;
        multiSpacialPanels[PREV_SEARCH_IDX]= prevSearch;
        multiSpacialPanels[COORDS_IDX]= coords;


        multiSearchMethod.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent ev) {
                updateMultiTargetDisplay();
            }
        });

        multiModeOpsPanel.add(upload.makePanel());
        multiModeOpsPanel.add(prevSearch.makePanel());
        multiModeOpsPanel.add(coords.makePanel());
        multiModeOpsPanel.showWidget(0);
        multiModeOpsPanel.setSize("220px", "130px");


        FlowPanel methodContainer= new FlowPanel();
        methodContainer.add(multiModeOpsPanel);
        methodContainer.add(multiSearchMethod);
        GwtUtil.setStyles(multiSearchMethod,
                          "display", "inline-block",
                          "verticalAlign", "top",
                          "padding", "8px 0 0 20px");
        GwtUtil.setStyles(multiModeOpsPanel, "display", "inline-block",
                          "padding", "0 0 0 30px");

        FlowPanel container= new FlowPanel();
        container.add(multiCone.makePanel());
        container.add(methodContainer);

        GwtUtil.setStyles(methodContainer,
                          "padding", "6px 0 0 0",
                          "borderTop", "1px solid rgba(0,0,0,.35)",
                          "marginTop", "10px",
                          "width", "84%"
        );

        multiTarget= container;


    }

    private void makePolygon() {
        FlowPanel panel= new FlowPanel();
        SpacialBehaviorPanel.Polygon p = new SpacialBehaviorPanel.Polygon();
        panel.add(p.makePanel());
        polygonPanel= new SpacialBehaviorPanel.Polygon();
        polyTarget= panel;
        spacialOpsMap.put(SpacialType.Polygon, new SpatialOps.Polygon(p.getPolygonField()));

    }




    private void makeAllSky() {
        allSky= new Label("Search all the sky");
    }

    private void makeSingle() {

        singleSpacialPanels= new SpacialBehaviorPanel[3];

        SpacialBehaviorPanel.Cone cone=  new SpacialBehaviorPanel.Cone();
        SpacialBehaviorPanel.Elliptical elliptical= new SpacialBehaviorPanel.Elliptical();
        SpacialBehaviorPanel.Box box= new SpacialBehaviorPanel.Box();

        singleSpacialPanels[CONE_IDX]= cone;
        singleSpacialPanels[ELLIPTICAL_IDX]= elliptical;
        singleSpacialPanels[BOX_IDX]= box;


        singleSearchMethod.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent ev) {
                updateSingleTargetDisplay();
            }
        });
        singleModeOpsPanel.add(cone.makePanel());
        singleModeOpsPanel.add(elliptical.makePanel());
        singleModeOpsPanel.add(box.makePanel());
        singleModeOpsPanel.showWidget(0);
        singleModeOpsPanel.setSize("220px", "130px");

        FlowPanel methodContainer= new FlowPanel();
        methodContainer.add(singleModeOpsPanel);
        methodContainer.add(singleSearchMethod);
        GwtUtil.setStyles(singleSearchMethod,
                          "display", "inline-block",
                          "verticalAlign", "top",
                          "padding", "8px 0 0 20px");
        GwtUtil.setStyles(singleModeOpsPanel, "display", "inline-block",
                          "padding", "0 0 0 30px");

        FlowPanel container= new FlowPanel();
        container.add(targetPanel);
        container.add(methodContainer);

        GwtUtil.setStyles(methodContainer,
                          "padding", "6px 0 0 0",
                          "borderTop", "1px solid rgba(0,0,0,.35)",
                          "marginTop", "10px",
                          "width", "84%"
        );

        singleTarget= container;

        spacialOpsMap.put(SpacialType.Cone, new SpatialOps.Cone(cone.getField().getField()));
        spacialOpsMap.put(SpacialType.Box, new SpatialOps.Box(box.getField().getField()));
        spacialOpsMap.put(SpacialType.Elliptical, new SpatialOps.Elliptical(elliptical.getAxisField(),
                                                                            elliptical.getPaField(),
                                                                            elliptical.getRatioField() ));
    }

    private void updateSingleTargetDisplay() {
        String mode= singleSearchMethod.getValue();
        singleModeOpsPanel.showWidget(convertSingleModeToIdx(mode));
    }

    private void updateMultiTargetDisplay() {
        String mode= multiSearchMethod.getValue();
        multiModeOpsPanel.showWidget(convertMultiModeToIdx(mode));
    }

    private int convertSingleModeToIdx(String singleMode) {
        int retval= 0;
        if      (singleMode.equals(CONE))        retval= CONE_IDX;
        else if (singleMode.equals(ELLIPTICAL))  retval= ELLIPTICAL_IDX;
        else if (singleMode.equals(BOX))         retval= BOX_IDX;
        else  WebAssert.argTst(false, "unknown mode for SpacialSelectUI single target:"+singleMode);
        return retval;
    }
    private int convertSingleModeToIdx(SpacialType singleMode) {
        int retval= 0;
        if      (singleMode== SpacialType.Cone)        retval= CONE_IDX;
        else if (singleMode== SpacialType.Elliptical)  retval= ELLIPTICAL_IDX;
        else if (singleMode== SpacialType.Box)         retval= BOX_IDX;
        else  retval= 0;
        return retval;
    }

    private int convertMultiModeToIdx(String multiMode) {
        int retval= 0;
        if      (multiMode.equals(UPLOAD))        retval= UPLOAD_IDX;
        else if (multiMode.equals(PREV_SEARCH))  retval= PREV_SEARCH_IDX;
        else if (multiMode.equals(COORDS))         retval= COORDS_IDX;
        else  WebAssert.argTst(false, "unknown mode for SpacialSelectUI single target:"+multiMode);
        return retval;
    }


    public void updateMax(int max) {
        SpacialType sType= computeSpacialType();
        if (sType== SpacialType.Cone || sType== SpacialType.Elliptical || sType== SpacialType.Box) {
            singleSpacialPanels[convertSingleModeToIdx(sType)].updateMax(max);
        }
    }


    public boolean validate() {
        return true;  //Todo
    }

    public SpacialType computeSpacialType() {
        return SpacialType.Cone;  // todo
    }



    public void getFieldValuesAsync(AsyncCallback<List<Param>> cb) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isAsyncCallRequired() { return true; }

    public List<Param> getFieldValues() { // todo
        ArrayList<Param> list= new ArrayList<Param>(10);
        SpacialType sType= computeSpacialType();
        switch (sType) {
            case Cone:
                break;
            case Elliptical:
                break;
            case Box:
                break;
            case Polygon:
                break;
            case MultiTableUpload:
                break;
            case AllSky:
                break;
            case MultiPrevSearch:
                break;
            case MultiPoints:
                break;
        }
        return list;
    }

    public void setFieldValues(List<Param> list) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

//====================================================================
//  implementing HasWidget
//====================================================================

    public void add(Widget w) { throw new UnsupportedOperationException("operation not allowed"); }
    public void clear() { throw new UnsupportedOperationException("operation not allowed"); }
    public boolean remove(Widget w) { throw new UnsupportedOperationException("operation not allowed"); }

    public Iterator<Widget> iterator() {
        return new ArrayList<Widget>().iterator(); // todo decide what goees here
        //return new ArrayList<Widget>(Arrays.asList(posField, resolveByField)).iterator();
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
