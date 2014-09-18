package edu.caltech.ipac.firefly.ui.searchui;
/**
 * User: roby
 * Date: 1/28/14
 * Time: 1:18 PM
 */


import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.DataSetInfo;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.SpacialType;
import edu.caltech.ipac.firefly.ui.FieldDefCreator;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.SimpleTargetPanel;
import edu.caltech.ipac.firefly.ui.input.AsyncInputFieldGroup;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.util.dd.EnumFieldDef;
import edu.caltech.ipac.util.dd.ValidationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static edu.caltech.ipac.firefly.data.SpacialType.AllSky;
import static edu.caltech.ipac.firefly.data.SpacialType.Box;
import static edu.caltech.ipac.firefly.data.SpacialType.Cone;
import static edu.caltech.ipac.firefly.data.SpacialType.Elliptical;
import static edu.caltech.ipac.firefly.data.SpacialType.IbeMultiPrevSearch;
import static edu.caltech.ipac.firefly.data.SpacialType.IbeMultiTableUpload;
import static edu.caltech.ipac.firefly.data.SpacialType.IbeSingleImage;
import static edu.caltech.ipac.firefly.data.SpacialType.MultiPoints;
import static edu.caltech.ipac.firefly.data.SpacialType.MultiPrevSearch;
import static edu.caltech.ipac.firefly.data.SpacialType.MultiTableUpload;
import static edu.caltech.ipac.firefly.data.SpacialType.Polygon;

/**
 * @author Trey Roby
 */
public class SpacialSelectUI extends Composite implements AsyncInputFieldGroup {

    private static final int NORMAL_HEIGHT_REQUIRED= 225;
    private static final int IBE_HEIGHT_REQUIRED= 250;
    private static final int ALL_SKY_HEIGHT_REQUIRED= 120;
    private static final int POLYGON_HEIGHT_REQUIRED= 205;
    private static final int PADDING_TOP= 12;

    private static final String CONE_KEY = "cone";
    private static final String ELLIPTICAL_KEY = "elliptical";
    private static final String BOX_KEY = "box";



    private static final int CONE_IDX= 0;
    private static final int ELLIPTICAL_IDX= 1;
    private static final int BOX_IDX= 2;


    private static final String UPLOAD_KEY = "upload";
    private static final String PREV_SEARCH_KEY = "prevSearch";
    private static final String COORDS_KEY = "coords";


    private static final int UPLOAD_IDX= 0;
    private static final int PREV_SEARCH_IDX= 1;
    private static final int COORDS_IDX= 2;


    private static final Map<SpacialType,SpatialOps> spacialOpsMap= new HashMap<SpacialType, SpatialOps>(15);

    private static final WebClassProperties _prop = new WebClassProperties(SpacialSelectUI.class);
    private DataSetInfo dsInfo= null;
    private DataSetInfo.DataTypes currDataType= DataSetInfo.DataTypes.CATALOGS;

    enum TabMode { SINGLE, IBE_SINGLE, MULTI, IBE_MULTI, POLYGON, ALL_SKY}
    FlowPanel mainPanel= new FlowPanel();
    private SimpleInputField  singleSearchMethod = null;
    private final SimplePanel singleSearchMethodWrapper= new SimplePanel();
    private final SimpleInputField multiSearchMethod =
            SimpleInputField.createByProp(
                    _prop.makeBase("multiMethod"), new SimpleInputField.Config("1px"), false);


    private Widget singleTarget;
    private Widget singleTargetIbe; // todo
    private Widget polyTarget;
    private Widget multiTarget;
    private Widget multiTargetIbe; //  todo
    private Widget allSky;
    private DeckLayoutPanel singleModeOpsPanel= new DeckLayoutPanel();
    private DeckLayoutPanel multiModeOpsPanel= new DeckLayoutPanel();

                  // Tab pane & tabs
    private TabPane<Widget> spacialOpsTabs = new TabPane<Widget>();
    private TabPane.Tab<Widget> singleTab= null;
    private TabPane.Tab<Widget> ibeSingleTab= null;
    private TabPane.Tab<Widget> polyTab= null;
    private TabPane.Tab<Widget> multiTab= null;
    private TabPane.Tab<Widget> ibeMultiTab= null;
    private TabPane.Tab<Widget> allSkyTab= null;

    private int searchMaxArcSec= 10;
    private boolean tabUpdateOn= true;
    private TabChange tabChangeListener;
    private TabMode selectedMode= TabMode.SINGLE;
    private SimpleTargetPanel targetPanel = new SimpleTargetPanel();
    private SimplePanel targetPanelIbeWrapper = new SimplePanel();
    private SimplePanel targetPanelWrapper = new SimplePanel();
    private CheckBox oneToOneCB= GwtUtil.makeCheckBox("Use 1-to-1 Matching","Use 1-to-1 Matching", false);


    SpacialSelectUI(TabChange tabChangeListener) {
        initWidget(mainPanel);
        this.tabChangeListener= tabChangeListener;
        makeUI();

    }

    void setDataSetInfo(DataSetInfo dsInfo, DataSetInfo.DataTypes dataType) {
        if (dsInfo==null) return;
        if (this.dsInfo==null ||
                !this.dsInfo.getId().equals(dsInfo.getId()) ||
                currDataType!=dataType) {

            this.dsInfo= dsInfo;
            currDataType= dataType;
            Set<SpacialType> stGroup= dsInfo.getSpatialSearchType(dataType);
            boolean singleOp= stGroup.contains(Cone) || stGroup.contains(Box) || stGroup.contains(Elliptical);
            boolean ibeSingleOp= stGroup.contains(IbeSingleImage);
            boolean multiOp= stGroup.contains(MultiPoints) ||
                             stGroup.contains(MultiPrevSearch) ||
                             stGroup.contains(MultiTableUpload);
            boolean ibeMultiOp= stGroup.contains(IbeMultiTableUpload) || stGroup.contains(IbeMultiPrevSearch);
            boolean polyOp= stGroup.contains(Polygon);
            boolean allSkyOp= stGroup.contains(AllSky);
            reinitTabPane(singleOp, ibeSingleOp, multiOp, ibeMultiOp, polyOp, allSkyOp);
            if (singleOp) {
                reinitSingleSearchMethod(stGroup);
                updateSingleTargetDisplay();
            }

            if (multiOp) {
                reinitMultiOp();
            }

            if (ibeMultiOp) {
//                reinitIbeMultiOp(); //todo
            }

            updateSearchMax();
            if (tabUpdateOn) updateSelectedTab();
        }
    }

    public void makeUI() {
        spacialOpsTabs.addSelectionHandler(new SelectionHandler<Integer>() {
            public void onSelection(SelectionEvent<Integer> event) {
                if (tabUpdateOn) updateSelectedTab();
            }
        });

        spacialOpsTabs.addBeforeSelectionHandler(new BeforeSelectionHandler<Integer>() {
            public void onBeforeSelection(BeforeSelectionEvent<Integer> ev) {
                preSelectedTab(ev.getItem());
            }
        });

        makeSingle();
        makeIbeSingle();
        makeMulti();
        makeIbeMulti();
        makePolygon();
        makeAllSky();

        reinitTabPane(true, true, true, true, true, true);
        mainPanel.add(spacialOpsTabs);
        spacialOpsTabs.setSize("100%", "100%");
//        mainPanel.setSize("700px", "245px");
        mainPanel.setSize("600px", NORMAL_HEIGHT_REQUIRED+"px");
        GwtUtil.setStyle(mainPanel,"paddingTop", PADDING_TOP+"px");
    }

    private int getTabPaneSize() {
        int height;
        switch (selectedMode) {
            case POLYGON:
                height= POLYGON_HEIGHT_REQUIRED;
                break;
            case ALL_SKY:
                height= ALL_SKY_HEIGHT_REQUIRED;
                break;
            case IBE_SINGLE:
            case IBE_MULTI:
                height= IBE_HEIGHT_REQUIRED;
                break;
            case MULTI:
            case SINGLE:
            default:
                height= NORMAL_HEIGHT_REQUIRED;
                break;
        }
        return height;

    }

    public int getHeightRequired() {
        return getTabPaneSize()+PADDING_TOP;
    }

    public String getSpacialDesc() {
        StringBuilder sb= new StringBuilder(100);
        switch (computeSpacialType()) {
            case Cone:
                sb.append("Cone, ");
                sb.append(targetPanel.getTargetName());
                break;
            case Elliptical:
                sb.append("Elliptical, ");
                sb.append(targetPanel.getTargetName());
                break;
            case Box:
                sb.append("Box, ");
                sb.append(targetPanel.getTargetName());
                break;
            case IbeSingleImage:
                sb.append("Search, ");
                sb.append(targetPanel.getTargetName());
                break;
            case Polygon:
                sb.append("Polygon");
                break;
            case MultiTableUpload:
                sb.append("Upload");
                break;
            case MultiPrevSearch:
                sb.append("PrevSearch");
                break;
            case IbeMultiTableUpload:
                sb.append("Upload");
                break;
            case IbeMultiPrevSearch:
                sb.append("PrevSearch");
                break;
            case MultiPoints:
                sb.append("Multi object");
                break;
            case AllSky:
                sb.append("AllSky");
                break;
        }
        return sb.toString();

    }

    private void reinitTabPane(boolean singleOp,
                               boolean ibeSingleOp,
                               boolean multiOp,
                               boolean ibeMultiTabOp,
                               boolean polyOp,
                               boolean allSkyOp) {
        tabUpdateOn= false;
        TabPane.Tab<Widget> selectedTab= spacialOpsTabs.getSelectedTab();

        checkAndRemoveTab(selectedTab,singleTab,TabMode.SINGLE);
        checkAndRemoveTab(selectedTab,ibeSingleTab,TabMode.IBE_SINGLE);
        checkAndRemoveTab(selectedTab,polyTab,TabMode.POLYGON);
        checkAndRemoveTab(selectedTab,multiTab,TabMode.MULTI);
        checkAndRemoveTab(selectedTab,ibeMultiTab,TabMode.IBE_MULTI);
        checkAndRemoveTab(selectedTab,allSkyTab,TabMode.ALL_SKY);
        singleTab= null;
        ibeSingleTab= null;
        polyTab= null;
        multiTab= null;
        ibeMultiTab= null;
        allSkyTab= null;

        singleTab   = checkAndAddTab(singleOp,singleTarget,"Single");
        ibeSingleTab= checkAndAddTab(ibeSingleOp,singleTargetIbe,"Single");
        polyTab     = checkAndAddTab(polyOp,polyTarget,"Polygon");
        multiTab    = checkAndAddTab(multiOp,multiTarget,"Multi");
        ibeMultiTab = checkAndAddTab(ibeMultiTabOp,multiTargetIbe,"Multi");
        allSkyTab   = checkAndAddTab(allSkyOp,allSky,"All Sky");

        tabUpdateOn= true;
        selectedTab= findSelectedTabFromMode();
        preSelectedTab(selectedTab);
        spacialOpsTabs.selectTab(selectedTab);

    }

    private void checkAndRemoveTab(TabPane.Tab<Widget> selectedTab, TabPane.Tab<Widget> testTab, TabMode mode) {
        if (testTab!=null) {
            if (selectedTab==testTab) selectedMode= mode;
            spacialOpsTabs.removeTab(testTab);
        }
    }

    private TabPane.Tab<Widget> checkAndAddTab(boolean op, Widget w, String desc) {
        TabPane.Tab<Widget> retTab= null;
        if (op)  retTab= spacialOpsTabs.addTab(w,desc);
        return retTab;
    }


    private TabPane.Tab<Widget> findSelectedTabFromMode() {
         TabPane.Tab<Widget> retval;
        switch (selectedMode) {
            case SINGLE:
                retval= singleTab;
                break;
            case IBE_SINGLE:
                retval= ibeSingleTab;
                break;
            case MULTI:
                retval= multiTab;
                break;
            case IBE_MULTI:
                retval= ibeMultiTab;
                break;
            case POLYGON:
                retval= polyTab;
                break;
            case ALL_SKY:
                retval= allSkyTab;
                break;
            default:
                retval= null;
                break;
        }
        if (!spacialOpsTabs.isTabVisible(retval)) retval= spacialOpsTabs.getVisibleTab(0);
        return retval;

    }


    private void preSelectedTab(int nextIdx) {
        TabPane.Tab<Widget> preTab= spacialOpsTabs.getVisibleTab(nextIdx);
        preSelectedTab(preTab);
    }

    private void preSelectedTab(TabPane.Tab<Widget> preTab) {
        if (preTab!=null) {
            if      (preTab==singleTab) {
                targetPanelWrapper.setWidget(targetPanel);
            }
            else if (preTab==ibeSingleTab) {
                targetPanelIbeWrapper.setWidget(targetPanel);
            }
        }
    }



    private void updateSelectedTab() {
        TabPane.Tab<Widget> selectedTab= spacialOpsTabs.getSelectedTab();
        if      (selectedTab==singleTab) selectedMode= TabMode.SINGLE;
        else if (selectedTab==ibeSingleTab) selectedMode= TabMode.IBE_SINGLE;
        else if (selectedTab==multiTab) selectedMode= TabMode.MULTI;
        else if (selectedTab==ibeMultiTab) selectedMode= TabMode.IBE_MULTI;
        else if (selectedTab==polyTab) selectedMode= TabMode.POLYGON;
        else if (selectedTab==allSkyTab) selectedMode= TabMode.ALL_SKY;
        updateSearchMax();
        mainPanel.setSize("600px", getTabPaneSize()+"px");
        if (tabChangeListener!=null) tabChangeListener.onTabChange();
    }


    private void makeMulti() {

        SpacialBehaviorPanel.TableUpload upload=  new SpacialBehaviorPanel.TableUpload();
        SpacialBehaviorPanel.PrevSearch prevSearch= new SpacialBehaviorPanel.PrevSearch();
        SpacialBehaviorPanel.UserEnteredCoords coords= new SpacialBehaviorPanel.UserEnteredCoords();

        multiSearchMethod.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent ev) {
                updateMultiTargetDisplay();
            }
        });

        multiModeOpsPanel.add(upload.makePanel());
        multiModeOpsPanel.add(prevSearch.makePanel());
        multiModeOpsPanel.add(coords.makePanel());
        multiModeOpsPanel.showWidget(0);
        multiModeOpsPanel.setSize("220px", "70px");


        FlowPanel methodContainer= new FlowPanel();
        methodContainer.add(multiModeOpsPanel);
//        methodContainer.add(multiSearchMethod); // todo comment when we support more methods
        GwtUtil.setStyles(multiSearchMethod,
                          "display", "inline-block",
                          "verticalAlign", "top",
                          "padding", "8px 0 0 20px");
        GwtUtil.setStyles(multiModeOpsPanel, "display", "inline-block",
                          "padding", "0 0 0 30px");

        SpacialBehaviorPanel.Cone multiCone=  new SpacialBehaviorPanel.Cone();
        FlowPanel container= new FlowPanel();


        Widget multiConePanel= multiCone.makePanel();
        SimplePanel oneToOneWrapper= new SimplePanel(oneToOneCB);

        container.add(methodContainer);
        container.add(multiConePanel);
        container.add(oneToOneWrapper);


        GwtUtil.setStyles(oneToOneWrapper,
                          "padding", "25px 0 0 0"
                          );

        GwtUtil.setStyles(methodContainer,
//                          "padding", "6px 0 0 0",
//                          "borderTop", "1px solid rgba(0,0,0,.35)",
//                          "marginTop", "10px",
                          "width", "84%"
        );

        GwtUtil.setStyles(multiConePanel,
//                          "padding", "6px 0 0 0",
                          "borderTop", "1px solid rgba(0,0,0,.35)",
//                          "marginTop", "10px",
                          "width", "84%"
        );

        multiTarget= container;
        spacialOpsMap.put(SpacialType.MultiTableUpload,
                          new SpatialOps.TableUpload(multiCone.getField(),
                                                     upload.getUploadField(),multiCone,oneToOneCB));
        spacialOpsMap.put(SpacialType.MultiPrevSearch,
                          new SpatialOps.PrevSearch(multiCone.getField(), multiCone,oneToOneCB));
        spacialOpsMap.put(SpacialType.MultiPoints,
                          new SpatialOps.MultiPoint(multiCone.getField(), multiCone,oneToOneCB));

        reinitMultiOp();

    }


    private void makeIbeMulti() {

        SpacialBehaviorPanel.IbeTableUpload ibeTab=  new SpacialBehaviorPanel.IbeTableUpload();
        Widget ibeTabUploadPanel= ibeTab.makePanel();
        //todo

        spacialOpsMap.put(SpacialType.IbeMultiTableUpload, new SpatialOps.IbeTableUpload(ibeTab.getIntersect(),
                                                                                         ibeTab.getSize(),
                                                                                         ibeTab.getSubSize(),
                                                                                         ibeTab.getMCenter(),
                                                                                         ibeTab.getUploadField()) );
        multiTargetIbe= ibeTabUploadPanel;
    }



    private void reinitMultiOp() {
        if (currDataType!=null && dsInfo!=null) {
            Set<SpacialType> stGroup= dsInfo.getSpatialSearchType(currDataType);
            boolean hidden= !stGroup.contains(SpacialType.MultiSupportsOneToOneAttribute);
            GwtUtil.setHidden(oneToOneCB,hidden);
            if (hidden) oneToOneCB.setValue(false);
        }
    }

    private void makePolygon() {
        FlowPanel panel= new FlowPanel();
        SpacialBehaviorPanel.Polygon p = new SpacialBehaviorPanel.Polygon();
        panel.add(p.makePanel());
        polyTarget= panel;
        spacialOpsMap.put(SpacialType.Polygon, new SpatialOps.Polygon(p.getPolygonField()));

    }




    private void makeAllSky() {
        allSky= new Label("Search all the sky");
        spacialOpsMap.put(SpacialType.AllSky, new SpatialOps.AllSky());
    }

    private void makeSingle() {

        SpacialBehaviorPanel.Cone cone=  new SpacialBehaviorPanel.Cone();
        SpacialBehaviorPanel.Elliptical elliptical= new SpacialBehaviorPanel.Elliptical();
        SpacialBehaviorPanel.Box box= new SpacialBehaviorPanel.Box();

        reinitSingleSearchMethod(new HashSet<SpacialType>(Arrays.asList(Cone,Box,Elliptical)));
        singleModeOpsPanel.add(cone.makePanel());
        singleModeOpsPanel.add(elliptical.makePanel());
        singleModeOpsPanel.add(box.makePanel());
        singleModeOpsPanel.showWidget(0);
        singleModeOpsPanel.setSize("300px", "130px");

        FlowPanel methodContainer= new FlowPanel();
        methodContainer.add(singleSearchMethodWrapper);
        methodContainer.add(singleModeOpsPanel);
        GwtUtil.setStyles(singleSearchMethodWrapper,
                          "display", "inline-block",
                          "verticalAlign", "top",
                          "padding", "8px 45px 0 0px");
        GwtUtil.setStyles(singleModeOpsPanel, "display", "inline-block",
                          "padding", "0 0 0 30px");

        FlowPanel container= new FlowPanel();
        container.add(targetPanelWrapper);
        container.add(methodContainer);

        GwtUtil.setStyles(methodContainer,
                          "padding", "6px 0 0 0",
                          "borderTop", "1px solid rgba(0,0,0,.35)",
                          "marginTop", "10px",
                          "width", "90%"
        );

        targetPanelWrapper.setWidget(targetPanel);
        singleTarget= container;

        spacialOpsMap.put(SpacialType.Cone, new SpatialOps.Cone(cone.getField(), cone));
        spacialOpsMap.put(SpacialType.Box, new SpatialOps.Box(box.getField(), cone));
        spacialOpsMap.put(SpacialType.Elliptical, new SpatialOps.Elliptical(elliptical.getAxisField(),
                                                                            elliptical.getPaField(),
                                                                            elliptical.getRatioField(), cone ));
    }


    private void makeIbeSingle() {
        SpacialBehaviorPanel.IbeSingle ibeSingle=  new SpacialBehaviorPanel.IbeSingle();
        Widget ibeSinglePanel= ibeSingle.makePanel();
        FlowPanel container= new FlowPanel();
        container.add(targetPanelIbeWrapper);
        container.add(ibeSinglePanel);

        GwtUtil.setStyles(ibeSinglePanel,
                          "padding", "6px 0 0 0",
                          "borderTop", "1px solid rgba(0,0,0,.35)",
                          "marginTop", "10px",
                          "width", "95%"
        );
        //todo

        spacialOpsMap.put(SpacialType.IbeSingleImage, new SpatialOps.IbeSingle(ibeSingle.getIntersect(),
                                                                               ibeSingle.getSize(),
                                                                               ibeSingle.getSubSize(),
                                                                               ibeSingle.getMCenter()));
        singleTargetIbe= container;
    }



    private void reinitSingleSearchMethod(Set <SpacialType> spacialTypes) {
        singleSearchMethodWrapper.clear();
        String prevValue= singleSearchMethod!=null ? singleSearchMethod.getValue() : null;
        EnumFieldDef fd= (EnumFieldDef)FieldDefCreator.makeFieldDef(_prop.makeBase("singleTargetMethod"));
        if (!spacialTypes.contains(Cone)) {
            fd.removeItem(CONE_KEY);
            if (CONE_KEY.equals(prevValue)) prevValue= null;
        }
        if (!spacialTypes.contains(Box)) {
            fd.removeItem(BOX_KEY);
            if (BOX_KEY.equals(prevValue)) prevValue= null;
        }
        if (!spacialTypes.contains(Elliptical)) {
            fd.removeItem(ELLIPTICAL_KEY);
            if (ELLIPTICAL_KEY.equals(prevValue)) prevValue= null;
        }

        singleSearchMethod = SimpleInputField.createByDef(fd, new SimpleInputField.Config("1px"), false);
        singleSearchMethod.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent ev) {
                updateSingleTargetDisplay();
            }
        });
        singleSearchMethodWrapper.setWidget(singleSearchMethod);
        if (prevValue!=null) singleSearchMethod.setValue(prevValue);
    }

    private void updateSingleTargetDisplay() {
        String mode= singleSearchMethod.getValue();
        singleModeOpsPanel.showWidget(convertSingleModeToIdx(mode));
    }

    private void updateMultiTargetDisplay() {
        String mode= multiSearchMethod.getValue();
        multiModeOpsPanel.showWidget(convertMultiModeToIdx(mode));
    }


    private SpacialType convertSingleModeToSpacialType(String singleMode) {
        SpacialType retval= Cone;
        if      (singleMode.equals(CONE_KEY))        retval= Cone;
        else if (singleMode.equals(ELLIPTICAL_KEY))  retval= Elliptical;
        else if (singleMode.equals(BOX_KEY))         retval= Box;
        else  WebAssert.argTst(false, "unknown mode for SpacialSelectUI single target:"+singleMode);
        return retval;
    }

    private int convertSingleModeToIdx(String singleMode) {
        int retval= 0;
        if      (singleMode.equals(CONE_KEY))        retval= CONE_IDX;
        else if (singleMode.equals(ELLIPTICAL_KEY))  retval= ELLIPTICAL_IDX;
        else if (singleMode.equals(BOX_KEY))         retval= BOX_IDX;
        else  WebAssert.argTst(false, "unknown mode for SpacialSelectUI single target:"+singleMode);
        return retval;
    }
    private int convertSingleModeToIdx(SpacialType singleMode) {
        int retval;
        if      (singleMode== SpacialType.Cone)        retval= CONE_IDX;
        else if (singleMode== SpacialType.Elliptical)  retval= ELLIPTICAL_IDX;
        else if (singleMode== SpacialType.Box)         retval= BOX_IDX;
        else  retval= 0;
        return retval;
    }
    private String convertSingleSpacialTypeToMode(SpacialType singleSpacialType) {
        String retval= CONE_KEY;
        switch (singleSpacialType) {
            case Cone:       retval= CONE_KEY;
                break;
            case Elliptical: retval= ELLIPTICAL_KEY;
                break;
            case Box:        retval= BOX_KEY;
                break;
            case Polygon:
            case MultiTableUpload:
            case MultiPrevSearch:
            case MultiPoints:
            case AllSky:
            default:
                WebAssert.argTst(false, "spacialType should only be single target:"+singleSpacialType.toString());
                break;
        }
        return retval;
    }

    private int convertMultiModeToIdx(String multiMode) {
        int retval= 0;
        if      (multiMode.equals(UPLOAD_KEY))        retval= UPLOAD_IDX;
        else if (multiMode.equals(PREV_SEARCH_KEY))  retval= PREV_SEARCH_IDX;
        else if (multiMode.equals(COORDS_KEY))         retval= COORDS_IDX;
        else  WebAssert.argTst(false, "unknown mode for SpacialSelectUI single target:"+multiMode);
        return retval;
    }

    private SpacialType convertMultiModeToSpacialType(String multiMode) {
        SpacialType retval= MultiTableUpload;
        if      (multiMode.equals(UPLOAD_KEY))        retval= MultiTableUpload;
        else if (multiMode.equals(PREV_SEARCH_KEY))  retval= MultiPrevSearch;
        else if (multiMode.equals(COORDS_KEY))         retval= MultiPoints;
        else  WebAssert.argTst(false, "unknown mode for SpacialSelectUI multi mode:"+multiMode);
        return retval;
    }

    private String convertMultiSpacialTypeToMode(SpacialType multiSpacialType) {

        String retval= UPLOAD_KEY;
        switch (multiSpacialType) {
            case MultiTableUpload: retval= UPLOAD_KEY;
                break;
            case MultiPrevSearch:  retval= PREV_SEARCH_KEY;
                break;
            case MultiPoints:      retval= COORDS_KEY;
                break;
            case Cone:
            case Elliptical:
            case Box:
            case Polygon:
            case AllSky:
            default:
                WebAssert.argTst(false, "spacialType should only be single target:"+multiSpacialType.toString());
                break;
        }
        return retval;
    }

    public void updateSearchMax(int maxArcSec) {
        searchMaxArcSec= maxArcSec;
        updateSearchMax();
    }

    public void updateSearchMax() {
        spacialOpsMap.get(computeSpacialType()).updateMax(searchMaxArcSec);
    }

    public boolean validate() {
        try {
            boolean tValid= true;
            if (selectedMode==TabMode.SINGLE || selectedMode==TabMode.IBE_SINGLE) {
                tValid= targetPanel.validate();
            }

            return tValid && spacialOpsMap.get(computeSpacialType()).validate();
        } catch (ValidationException e) {
            PopupUtil.showError("Error", e.getMessage());
            return false;
        }
    }

    public SpacialType computeSpacialType() {
        SpacialType  spacialType= SpacialType.Cone;
        switch (selectedMode) {
            case SINGLE:
                spacialType= convertSingleModeToSpacialType(singleSearchMethod.getValue());
                break;
            case IBE_SINGLE:
                spacialType= IbeSingleImage;
                break;
            case MULTI:
                spacialType= convertMultiModeToSpacialType(multiSearchMethod.getValue());
                break;
            case POLYGON:
                spacialType= Polygon;
                break;
            case ALL_SKY:
                spacialType= AllSky;
                break;
        }
        return spacialType;
    }

    public void defineSpacialType(SpacialType sType) {
        switch (sType) {
            case Cone:
            case Elliptical:
            case Box:
                spacialOpsTabs.selectTab(singleTab);
                singleSearchMethod.setValue(convertSingleSpacialTypeToMode(sType));
                break;
            case IbeSingleImage:
                spacialOpsTabs.selectTab(ibeSingleTab);
                break;
            case IbeMultiPrevSearch:
            case IbeMultiTableUpload:
                spacialOpsTabs.selectTab(ibeSingleTab);
                break;
            case MultiTableUpload:
            case MultiPrevSearch:
            case MultiPoints:
                spacialOpsTabs.selectTab(multiTab);
                multiSearchMethod.setValue(convertMultiSpacialTypeToMode(sType));
                break;
            case Polygon:
                spacialOpsTabs.selectTab(polyTab);
                break;
            case AllSky:
                spacialOpsTabs.selectTab(allSkyTab);
                break;
        }
    }


    public void getFieldValuesAsync(final AsyncCallback<List<Param>> cb) {
        SpatialOps ops= spacialOpsMap.get(computeSpacialType());
        if (ops.getRequireUpload()) {
            ops.doUpload(new AsyncCallback<String>() {
                public void onFailure(Throwable caught) {
                    cb.onFailure(caught);
                }

                public void onSuccess(String s) {
                    cb.onSuccess(getFieldValues());
                }
            });

        }
        else {
            cb.onSuccess(getFieldValues());
        }
    }

    public boolean isAsyncCallRequired() {
        SpatialOps ops= spacialOpsMap.get(computeSpacialType());
        return ops.getRequireUpload();
    }

    public List<Param> getFieldValues() {
        ArrayList<Param> list= new ArrayList<Param>(10);
        SpacialType sType= computeSpacialType();
        SpatialOps ops= spacialOpsMap.get(sType);
        list.add(new Param(ServerParams.SPACIAL_TYPE, ops.getSpacialType().toString()));
        list.addAll(ops.getParams());

        if (ops.getRequireTarget()) {
            list.addAll(targetPanel.getFieldValues());
        }
        return list;
    }

    public void setFieldValues(List<Param> list) {
        SpacialType st= SpacialType.Cone;
        for(Param p : list) {
            if (p.getName().equalsIgnoreCase(ServerParams.SPACIAL_TYPE)) {
                try {
                    st= Enum.valueOf(SpacialType.class, p.getValue());
                } catch (Exception e) {
                    st= SpacialType.Cone;
                }
                defineSpacialType(st);
                break;
            }
        }
        SpatialOps ops= spacialOpsMap.get(st);
        ops.setParams(list);
    }

//====================================================================
//  implementing HasWidget
//====================================================================

    public void add(Widget w) { throw new UnsupportedOperationException("operation not allowed"); }
    public void clear() { throw new UnsupportedOperationException("operation not allowed"); }
    public boolean remove(Widget w) { throw new UnsupportedOperationException("operation not allowed"); }

    public Iterator<Widget> iterator() {
        return new ArrayList<Widget>().iterator(); // todo decide what goes here
        //return new ArrayList<Widget>(Arrays.asList(posField, resolveByField)).iterator();
    }


    public interface TabChange {
        void onTabChange();
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
