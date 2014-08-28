package edu.caltech.ipac.firefly.ui.searchui;
/**
 * User: roby
 * Date: 1/28/14
 * Time: 1:18 PM
 */


import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.DataSetInfo;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.task.IrsaAllDataSetsTask;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.util.dd.EnumFieldDef;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static edu.caltech.ipac.firefly.data.DataSetInfo.DataTypes.CATALOGS;
import static edu.caltech.ipac.firefly.data.DataSetInfo.DataTypes.IMAGES;
import static edu.caltech.ipac.firefly.data.DataSetInfo.DataTypes.SPECTRUM;

/**
 * @author Trey Roby
 */
public class AnyDataSetSearchUI implements SearchUI {


    private static int CATALOG_IDX= 0;
    private static int IMAGE_IDX= 1;
    private static int SPECTRUM_IDX= 2;

    DockLayoutPanel mainPanel= new DockLayoutPanel(Style.Unit.PX);

    private SpacialSelectUI spacialArea;
    private List<DataSetInfo> allDatasetList;
    private DeckLayoutPanel dataTypeViews= new DeckLayoutPanel();
    private FlowPanel typeSelectPanel= new FlowPanel();
    private FlowPanel dataTypeWrapper= new FlowPanel();
    private SimpleInputField mission;
    private SimpleInputField dataType= null;
    private Set<DataSetInfo.DataTypes> availableDataTypes= new HashSet<DataSetInfo.DataTypes>(5);
    private DataSetInfo.DataTypes lastUserSetDataType= null;
    private DockLayoutPanel topArea= new DockLayoutPanel(Style.Unit.PX);


    private FlowPanel catalogView= new FlowPanel();
    private FlowPanel imageView= new FlowPanel();
    private FlowPanel spectrumView= new FlowPanel();

    private DataTypeSelectUI viewAry[]= new DataTypeSelectUI[3];

    public String getKey() { return "DataSetSearch"; }

    public AnyDataSetSearchUI() {

    }

    private void initMissionSection() {
        List<String> missionList= new ArrayList<String>(allDatasetList.size());
        for(DataSetInfo dsInfo : allDatasetList) {
            missionList.add(dsInfo.getUserDesc());
        }
        mission= GwtUtil.createListBoxField("", "Choose mission", missionList, missionList.get(0));
        mission.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent<String> event) {
                datasetChange();
                changeView();
            }
        });
        mission.getField().getFocusWidget().addStyleName("standout-option");

        updateDataTypes();

//        GwtUtil.setStyles(mission, "display", "inline-block",
//                          "padding", "10px 0 0 20px");
//        GwtUtil.setStyles(dataTypeWrapper, "display", "inline-block",
//                                           "padding", "10px 0 0 20px");
//        mission.addStyleName("left-floating");
        typeSelectPanel.add(mission);
        typeSelectPanel.add(dataTypeWrapper);
        typeSelectPanel.add(new HTML("Currently only 2MASS, WISE and<br> Spitzer Enhanced Product<br> images available"));


        changeView();

    }

    private void updateCatalogView(DataSetInfo dsInfo){
        catalogView.clear();
        viewAry[CATALOG_IDX]= new CatalogSelectUI(dsInfo, new CatalogSelectUI.SearchMaxChange() {
            public void onSearchMaxChange(int maxArcSec) {
                spacialArea.updateSearchMax(maxArcSec);
            }
        });
        catalogView.add(viewAry[CATALOG_IDX].makeUI());
    }


    private void updateImageView(DataSetInfo dsInfo){
        viewAry[IMAGE_IDX]= new ImageSelectUI(dsInfo);
        imageView.clear();
        imageView.add(viewAry[IMAGE_IDX].makeUI());
    }

    private void updateSpectrumView(DataSetInfo dsInfo){
        viewAry[SPECTRUM_IDX]= new SpectrumSelectUI(dsInfo);
        spectrumView.clear();
        spectrumView.add(viewAry[SPECTRUM_IDX].makeUI());
    }

    private void changeView() {
        DataSetInfo dsInfo= getSelectedDataSet();
        DataSetInfo.DataTypes selectedDT= getSelectedDataType();
        switch (selectedDT) {
            case CATALOGS:
                updateCatalogView(dsInfo);
               dataTypeViews.showWidget(CATALOG_IDX);
                break;
            case IMAGES:
                updateImageView(dsInfo);
                dataTypeViews.showWidget(IMAGE_IDX);
                break;
            case SPECTRUM:
                updateSpectrumView(dsInfo);
                dataTypeViews.showWidget(SPECTRUM_IDX);
                break;
        }
        spacialArea.setDataSetInfo(dsInfo, selectedDT);
    }

    private void datasetChange() {
        updateDataTypes();
    }


    private void updateDataTypes() {
        dataTypeWrapper.clear();
        availableDataTypes.clear();

        DataSetInfo dsInfo= getSelectedDataSet();


        String descBefore= "";
        String descAfter= "";
        if (isOnlyOneDataTypeAvailable(dsInfo)) {
            descBefore= "Only ";
            descAfter= " Data Available";
        }

        EnumFieldDef fd= new EnumFieldDef("");
        fd.setMask("[RADIO]");
        fd.setName("");
        fd.setOrientation(EnumFieldDef.Orientation.Vertical);
        fd.setShortDesc("What type of data to search for");
        if (dsInfo.getHasCatalogs()) {
            fd.addItem(CATALOGS.toString(),descBefore+"Catalog"+descAfter);
            fd.setDefaultValue(CATALOGS.toString());
            availableDataTypes.add(CATALOGS);
        }

        if (dsInfo.getHasImages()) {
            fd.addItem(IMAGES.toString(),descBefore+"Images"+descAfter);
            availableDataTypes.add(IMAGES);
        }

        if (dsInfo.getHasSpectrum()) {
            fd.addItem(SPECTRUM.toString(), descBefore + "Spectrum" + descAfter);
            availableDataTypes.add(SPECTRUM);
        }
        fd.setDefaultValue(getDefDataTypeValue(dsInfo).toString());


        dataType= SimpleInputField.createByDef(fd);

        dataType.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent<String> event) {
                lastUserSetDataType= getSelectedDataType();
                changeView();
            }
        });

        dataTypeWrapper.add(dataType);
    }

    private DataSetInfo.DataTypes getSelectedDataType() {
        DataSetInfo.DataTypes v;
        try {
            v= Enum.valueOf(DataSetInfo.DataTypes.class, dataType.getValue());
        } catch (Exception e) {
            v= CATALOGS;
        }
        return v;
    }

    private int getSelectedDataTypeIdx() { return convertDataTypeToIdx(getSelectedDataType()); }
    private DataTypeSelectUI getActiveDataTypeSelectUI() { return viewAry[getSelectedDataTypeIdx()]; }

    private int convertDataTypeToIdx(DataSetInfo.DataTypes dt) {
        int retval= CATALOG_IDX;
        switch (dt) {
            case CATALOGS:
                retval= CATALOG_IDX;
                break;
            case IMAGES:
                retval= IMAGE_IDX;
                break;
            case SPECTRUM:
                retval= SPECTRUM_IDX;
                break;
            default:
                WebAssert.argTst(false, "unknown dataType"+dt.toString());
                break;
        }
        return retval;
    }

    private void setSelectedDataType(DataSetInfo.DataTypes dt) {
        if (!availableDataTypes.contains(dt)) {
            dt= availableDataTypes.iterator().next();
        }
        dataType.setValue(dt.toString());
    }

    private DataSetInfo.DataTypes getDefDataTypeValue(DataSetInfo dsInfo) {
        DataSetInfo.DataTypes defValue= null;

        if (dsInfo.getHasCatalogs()) {
            defValue= CATALOGS;
        }
        if (dsInfo.getHasImages()) {
            if (defValue==null || lastUserSetDataType== IMAGES) {
                defValue= IMAGES;
            }
        }
        if (dsInfo.getHasSpectrum()) {
            if (defValue==null || lastUserSetDataType== SPECTRUM) {
                defValue= SPECTRUM;
            }
        }
        return defValue;
    }

    private static boolean isOnlyOneDataTypeAvailable(DataSetInfo dsInfo) {
        int cnt= dsInfo.getHasCatalogs() ? 1 : 0;
        cnt+= dsInfo.getHasImages() ? 1 : 0;
        cnt+= dsInfo.getHasSpectrum() ? 1 : 0;
        return cnt==1;
    }

    private void setSelectedDataSet(DataSetInfo dsInfo) {
        mission.setValue(dsInfo.getUserDesc());
    }

    private DataSetInfo getSelectedDataSet() {
        return findDataSet(mission.getValue());
    }

    private DataSetInfo findDataSet(String dsStr) {
        DataSetInfo retval= null;
        for(DataSetInfo dsInfo : allDatasetList) {
            if (dsStr.equals(dsInfo.getUserDesc())) {
                retval= dsInfo;
            }
        }
        return retval;
    }

    public String getPanelTitle() {
        return "General Search";
    }

    public String getDesc() {
        return "Search any data set in IRSA";
    }

    public String getSearchTitle() {
        return  getActiveDataTypeSelectUI().getDataDesc() + spacialArea.getSpacialDesc();
    }

    public Widget makeUI() {
        spacialArea= new SpacialSelectUI(new SpacialSelectUI.TabChange() {
            public void onTabChange() {
                adjustSpacialHeight();
            }
        });
        mainPanel.setSize("100%", "100%");

        topArea= new DockLayoutPanel(Style.Unit.PX);
        Widget typeSelectPanelWrap= GwtUtil.wrap(typeSelectPanel,50,5,0,5);


        topArea.addWest(typeSelectPanelWrap, 200);
        topArea.add(new SimplePanel(spacialArea));


//        mainPanel.addNorth(new SimplePanel(spacialArea), SpacialSelectUI.MIN_HEIGHT_REQUIRED);
//        mainPanel.addNorth(typeSelectPanel, 45);
        mainPanel.addNorth(topArea, spacialArea.getHeightRequired());
//        Element topWrap = DOM.getParent(topArea.getElement());
//        topWrap.addClassName("change-height-transition");
        DOM.getParent(topArea.getElement()).addClassName("change-height-transition");


        Widget dataTypeViewsWrapper= GwtUtil.wrap(dataTypeViews,10,0,0,5);

        mainPanel.add(dataTypeViewsWrapper);
        DOM.getParent(dataTypeViewsWrapper.getElement()).addClassName("change-height-transition");

//        typeSelectPanel.setSize("100%", "45px");
//        dataTypeViews.setSize("100%", "300px");

        dataTypeViews.add(catalogView);
        dataTypeViews.add(imageView);
        dataTypeViews.add(spectrumView);
        dataTypeViews.showWidget(0);


        if (IrsaAllDataSetsTask.isIrsaAllDataSetsRetrieved()) {
            allDatasetList= IrsaAllDataSetsTask.getIrsaAllDataSetsImmediate();
            initMissionSection();
        }
        else {
            IrsaAllDataSetsTask.getIrsaAllDataSets(mainPanel,new AsyncCallback<List<DataSetInfo>>() {
                public void onFailure(Throwable caught) {
                    //todo
                }

                public void onSuccess(List<DataSetInfo> result) {
                    allDatasetList= result;
                    initMissionSection();
                }
            });
        }


        return mainPanel;
    }

    public boolean validate() {
        return spacialArea.validate() && getActiveDataTypeSelectUI().validate();
    }

    private void adjustSpacialHeight() {
        GwtUtil.DockLayout.setWidgetChildSize(topArea, spacialArea.getHeightRequired());
        mainPanel.forceLayout();
    }


    public void makeServerRequest(final AsyncCallback<ServerRequest> cb) {
        DataSetInfo.DataTypes dataTypes= getSelectedDataType();

        final ServerRequest r= new ServerRequest(makeRequestID());

        r.setParam(ServerParams.REQUESTED_DATA_SET, getSelectedDataSet().getUserDesc());
        r.setParam(ServerParams.DATA_TYPE, dataTypes.toString());
        r.setParams(getActiveDataTypeSelectUI().getFieldValues());

        spacialArea.getFieldValuesAsync(new AsyncCallback<List<Param>>() {
            public void onFailure(Throwable caught) {
                cb.onFailure(caught);
            }

            public void onSuccess(List<Param> l) {
                r.setParams(l);
                cb.onSuccess(r);
            }
        });
    }

    private String makeRequestID() {
        return getActiveDataTypeSelectUI().makeRequestID();
    }


    public boolean setServerRequest(ServerRequest r) {
        String dsStr= r.getParam(ServerParams.REQUESTED_DATA_SET);
        setSelectedDataSet(findDataSet(dsStr));

        DataSetInfo.DataTypes dt;
        try {
            dt= Enum.valueOf(DataSetInfo.DataTypes.class, dsStr);
        } catch (Exception e) {
            dt= CATALOGS;
        }
        setSelectedDataType(dt);

        List<Param> fieldValues= r.getParams();
        getActiveDataTypeSelectUI().setFieldValues(fieldValues);
        spacialArea.setFieldValues(fieldValues);

        return true;
    }


//====================================================================
//  implementing HasWidget
//====================================================================

    public void add(Widget w) { throw new UnsupportedOperationException("operation not allowed"); }
    public void clear() { throw new UnsupportedOperationException("operation not allowed"); }
    public boolean remove(Widget w) { throw new UnsupportedOperationException("operation not allowed"); }

    public Iterator<Widget> iterator() {
        return new ArrayList<Widget>().iterator();
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
