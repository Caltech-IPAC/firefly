/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.catalog;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.form.DegreeFieldDef;
import edu.caltech.ipac.firefly.data.form.PositionFieldDef;
import edu.caltech.ipac.firefly.data.table.BaseTableData;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.SimpleTargetPanel;
import edu.caltech.ipac.firefly.ui.input.AsyncInputFieldGroup;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.ui.table.AbstractLoader;
import edu.caltech.ipac.firefly.ui.table.SingleColDefinition;
import edu.caltech.ipac.firefly.ui.table.SingleColumnTablePanel;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.ActiveTarget;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.ValidationException;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: balandra Date: Jul 26, 2010
 */
public class CatalogPanel extends Composite implements AsyncInputFieldGroup {

    public static final String CONE = "cone";
    public static final String ELLIPTICAL = "elliptical";
    public static final String BOX = "box";
    public static final String POLYGON = "polygon";
    public static final String TABLE = "table";
    public static final String ALL_SKY = "all-sky";

    private static final String DEFAULT_SEARCH_METHOD = CONE;
    private static String _defaultMethodStr = DEFAULT_SEARCH_METHOD;

    private static final int TARGET_PANEL = 0;
    private static final int TARGET_DESC = 1;
    private static final int TARGET_HIDDEN = 2;
    private static final String LABEL_WIDTH = "80px";
    private CatalogData _catalogData = null;
    private static final WebClassProperties _prop = new WebClassProperties(CatalogPanel.class);
    //private String RANGES_STR = _prop.getName("ranges");
    private String SelectedColumns = "";
    private String SelectedConstraints = "";
    private String currCatalog = "";

    //public static final int MINMAX_IDX = 0;
    //public static final int ZSCALE_IDX = 1;
    private Proj _currentProj = null;
    private Catagory _currentCatagory = null;
    private Catalog _currentCatalog = null;
    private SimpleTargetPanel _targetPanel = new SimpleTargetPanel();
    private final DeckPanel _targetCards = new DeckPanel();
    private Widget _parent;
    private VerticalPanel vp = new VerticalPanel();
    private HorizontalPanel masterPanel = new HorizontalPanel();
    String _projectId;
    private KeyPressHandler keyPressHandler = null;
    //private int nonResizeableHeight= 0;

    private DeckPanel _searchMethodPanel = new DeckPanel();
    private final SimplePanel _targetPanelHolder = new SimplePanel();


    private final SimpleInputField _searchMethod =
            SimpleInputField.createByProp(_prop.makeBase("method"), new SimpleInputField.Config(LABEL_WIDTH), false);


    private final Map<CatalogRequest.Method, SearchMethods> _methods =
            new HashMap<CatalogRequest.Method, SearchMethods>(6);

    private final Label _catagoryLab = new Label(_prop.getName("catagory"));

    private final ListBox _projSelect = new ListBox();
    private final ListBox _catagorySelect = new ListBox();
    private final HTML _targetDesc = new HTML();
    private CatalogRequest.Method _currSearchMethod = CatalogRequest.Method.CONE;
    private final Label _catalogTitle = new Label();

    private SingleColumnTablePanel _catTable = new SingleColumnTablePanel(_prop.getTitle("catalog"),
            new FileteredDatasetLoader());
    private final boolean _inDialog;


    public CatalogPanel(Widget parent, String projectId) {
        this(parent, projectId, false);
    }

    public CatalogPanel(Widget parent, String projectId, boolean inDialog) {
        _parent = parent;
        _inDialog = inDialog;
        _projectId = projectId;
        init();
        initWidget(masterPanel);
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

//    public void onResize() {
//        Widget parent= getParent();
//        int h= parent.getOffsetHeight() - (vp.getOffsetHeight() + BaseDialog.BUTTON_HEIGHT);
//        masterPanel.setCellHeight(_catTable, h+"px");
//        masterPanel.setCellWidth(_catTable, (parent.getOffsetWidth() - 15) + "px");
//    }

    public void addKeyPressOnCreation(KeyPressHandler keyPressHandler) {
        this.keyPressHandler = keyPressHandler;
    }

    public String getCatName() {
        return _currentCatalog.getQueryCatName();
    }

    public static void setDefaultSearchMethod(CatalogRequest.Method def) {
        _defaultMethodStr = convert(def);
    }

    private void init() {
        if (_projectId.equalsIgnoreCase("WISE")) {
            _searchMethod.setValue(POLYGON);
        } else {
            _searchMethod.setValue(_defaultMethodStr);
        }
        if (_catalogData == null) {
            retrieveMasterCatalogAndVisible();
        } else {
            createContents();
            updateToActive();
            setTargetCard(computeTargetCard());
        }
    }

    public void showPanel() {
        updateToActive();
        if (_catalogData != null) {
            setTargetCard(computeTargetCard());
        }
    }

    private void createContents() {
        _targetPanelHolder.setWidget(_targetPanel);
        _projSelect.clear();
        for (Proj proj : _catalogData.getProjects()) {
            _projSelect.addItem(proj.getShortProjName());
        }

        initChangeHandlers();
        createTargetPanel();
        createSearchMethodPanel();
        initCatalogDisplay();

        HorizontalPanel hp = new HorizontalPanel();
        hp.add(_searchMethod);
        hp.add(_searchMethodPanel);
        _searchMethodPanel.addStyleName("standard-border");
        hp.setCellVerticalAlignment(_searchMethod, HorizontalPanel.ALIGN_MIDDLE);
        DOM.setStyleAttribute(_searchMethod.getElement(), "paddingRight", "20px");

        FlexTable grid = new FlexTable();

        Label projLab = new Label(_prop.getName("proj"));
        projLab.setWidth(LABEL_WIDTH);
        _catagoryLab.setWidth(LABEL_WIDTH);

        Label pad = new Label(" ");
        pad.setHeight("25px");

        DOM.setStyleAttribute(grid.getElement(), "paddingTop", "5px");
        grid.setWidget(0, 0, projLab);
        grid.setWidget(0, 1, _projSelect);
        grid.setWidget(1, 0, _catagoryLab);
        grid.setWidget(1, 1, _catagorySelect);
        grid.setWidget(1, 2, pad);
        grid.setCellSpacing(5);


        Widget link = GwtUtil.makeLinkButton("Set Column Restrictions", "Set Column Restrictions", new ClickHandler() {
            public void onClick(ClickEvent event) {
                showCatalogDialog();
            }
        });

        vp.add(_targetCards);
        vp.add(hp);
        vp.add(grid);
        vp.add(link);
        vp.add(new HTML("<br><br>"));
//        vp.add(_catTable);
//        vp.setCellHeight(_catTable, "250px");
        _catTable.addStyleName("standard-border");


        VerticalPanel catalogSide = new VerticalPanel();
        catalogSide.add(_catalogTitle);
        catalogSide.add(_catTable);


        GwtUtil.setStyle(_catalogTitle, "padding", "5px 0 5px 1px");

        masterPanel.add(vp);
        masterPanel.add(catalogSide);
        GwtUtil.setStyle(catalogSide, "marginLeft", "30px");


        catalogSide.setWidth(_inDialog ? "88%" : "100%");
        masterPanel.setCellWidth(catalogSide, "100%");
//        masterPanel.setCellHeight(catalogSide, "300px");


        catalogSide.setCellWidth(_catTable, "90%");
        catalogSide.setCellHeight(_catTable, "300px");
        catalogSide.setCellHorizontalAlignment(_catTable, HasHorizontalAlignment.ALIGN_RIGHT);


//        masterPanel.setCellWidth(_catTable, "90%");
//        masterPanel.setCellHeight(_catTable, "300px");
//        masterPanel.setCellHorizontalAlignment(_catTable, HasHorizontalAlignment.ALIGN_RIGHT);

        GwtUtil.setStyle(link, "paddingLeft", "5px");
        masterPanel.setSpacing(3);

        addKeyPressToAll(masterPanel);

        updateProj();
        showSearchMethodPanel();
//        onResize();
    }

    private void checkCatalog() {
        if (!currCatalog.equals(_currentCatalog.getQueryCatName()) && StringUtils.isEmpty(currCatalog)) {
            currCatalog = _currentCatalog.getQueryCatName();
        } else if (!currCatalog.equals(_currentCatalog.getQueryCatName())) {
            currCatalog = _currentCatalog.getQueryCatName();
            clearValues();
        }
    }

    private void clearValues() {
        SelectedColumns = "";
        SelectedConstraints = "";

    }

    private void showCatalogDialog() {
        CatalogRequest req = new CatalogRequest(CatalogRequest.RequestType.GATOR_DD);
        req.setQueryCatName(_currentCatalog.getQueryCatName());

        // set catalogProject for project-level parameters for Gator
        req.setParam(CatalogRequest.CATALOG_PROJECT, _currentProj.getShortProjName());
        req.setParam("projectId", _projectId);

        checkCatalog();

        boolean defSelect = SelectedColumns.isEmpty();
//        if (SelectedColumns.isEmpty()) {
//            defSelect = true;
//        } else {
//            defSelect = false;
//        }

        CatalogQueryDialog.showCatalogDialog(this, new CatColumnInfo() {
            public void setSelectedColumns(String values) {
                SelectedColumns = values;
            }

            public void setSelectedConstraints(String values) {
                SelectedConstraints = values;
            }
        }, req.getParams(), SelectedColumns, SelectedConstraints, defSelect);
    }


    private void initChangeHandlers() {
        _projSelect.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                updateProj();
            }
        });

        _catagorySelect.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                updateCatalogPanel(_currentProj.get(_catagorySelect.getSelectedIndex()));
            }
        });

        _searchMethod.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent ev) {
                showSearchMethodPanel();
            }
        });

    }

    private void addKeyPressToAll(Widget inWidget) {
        if (keyPressHandler != null && inWidget instanceof HasWidgets) {
            HasWidgets container = (HasWidgets) inWidget;
            for (Widget w : container) {
                if (w instanceof InputField) {
                    InputField f = (InputField) w;
                    if (f.getFocusWidget() != null) {
                        f.getFocusWidget().addKeyPressHandler(keyPressHandler);
                    }
                } else if (w instanceof SimpleTargetPanel) {
                    SimpleTargetPanel sp = (SimpleTargetPanel) w;
                    if (sp.getInputField() != null && sp.getInputField().getFocusWidget() != null) {
                        sp.getInputField().getFocusWidget().addKeyPressHandler(keyPressHandler);
                    }
                } else {
                    addKeyPressToAll(w);
                }
            }
        }
    }

    private void showSearchMethodPanel() {
        String mStr = _searchMethod.getValue();
        _currSearchMethod = convert(mStr);
        _searchMethodPanel.showWidget(_currSearchMethod.getIdx());
        if (_currentCatalog != null) updateSearchMax(_currentCatalog.getMaxArcSec());
        setTargetCard(computeTargetCard());
    }

    private static CatalogRequest.Method convert(String mStr) {
        CatalogRequest.Method method;
        if (mStr.equals(CONE)) {
            method = CatalogRequest.Method.CONE;
        } else if (mStr.equals(ELLIPTICAL)) {
            method = CatalogRequest.Method.ELIPTICAL;
        } else if (mStr.equals(BOX)) {
            method = CatalogRequest.Method.BOX;
        } else if (mStr.equals(POLYGON)) {
            method = CatalogRequest.Method.POLYGON;
        } else if (mStr.equals(TABLE)) {
            method = CatalogRequest.Method.TABLE;
        } else if (mStr.equals(ALL_SKY)) {
            method = CatalogRequest.Method.ALL_SKY;
        } else {
            method = CatalogRequest.Method.CONE;
//            assert false; // if you are here you added a new search method
        }
        return method;
    }

    private static String convert(CatalogRequest.Method method) {
        String retval = CONE;
        switch (method) {
            case CONE:
                retval = CONE;
                break;
            case ELIPTICAL:
                retval = ELLIPTICAL;
                break;
            case BOX:
                retval = BOX;
                break;
            case POLYGON:
                retval = POLYGON;
                break;
            case TABLE:
                retval = TABLE;
                break;
            case ALL_SKY:
                retval = ALL_SKY;
                break;
            default:
                retval = CONE;
                break;
        }
        return retval;
    }

    private void reloadSearchMethodPanel(CatalogRequest req) {

        if (_currSearchMethod == null) return;

        SearchMethods method = _methods.get(_currSearchMethod);

        if (_currSearchMethod.equals(CatalogRequest.Method.CONE)) {
            _searchMethod.setValue(CONE);
            SimpleInputField field = SearchMethods.Cone.getField();
            double deg = DegreeFieldDef.getDegreeValue(req.getRadius(), DegreeFieldDef.Units.ARCSEC);
            field.setValue(String.valueOf(deg));
            //_targetPanel.setFieldValues(req.getParams());

            //updateTargetDesc();
        } else if (_currSearchMethod.equals(CatalogRequest.Method.ELIPTICAL)) {
            _searchMethod.setValue(ELLIPTICAL);
            SimpleInputField field1 = SearchMethods.Elliptical.getAxisField();
            double deg = DegreeFieldDef.getDegreeValue(req.getRadius(), DegreeFieldDef.Units.ARCSEC);
            field1.setValue(String.valueOf(deg));

            InputField field2 = SearchMethods.Elliptical.getPaField();
            field2.setValue(String.valueOf(req.getPA()));

            InputField field3 = SearchMethods.Elliptical.getRatioField();
            field3.setValue(String.valueOf(req.getRatio()));
        } else if (_currSearchMethod.equals(CatalogRequest.Method.BOX)) {
            _searchMethod.setValue(BOX);
        } else if (_currSearchMethod.equals(CatalogRequest.Method.POLYGON)) {
            _searchMethod.setValue(POLYGON);
            if (method != null) {
                SimpleInputField field = SearchMethods.Polygon.getField();
                field.setValue(req.getPolygon());
            }
        } else if (_currSearchMethod.equals(CatalogRequest.Method.TABLE)) {
            _searchMethod.setValue(TABLE);
        } else if (_currSearchMethod.equals(CatalogRequest.Method.ALL_SKY)) {
            _searchMethod.setValue(ALL_SKY);
        } else {
            assert false; // if you are here you added a new search method
        }

        _projSelect.setSelectedIndex(Integer.parseInt(req.getProjIndex()));
        updateProj();

        if (Integer.parseInt(req.getCatagoryIndex()) != -1) {
            _catagorySelect.setSelectedIndex(Integer.parseInt(req.getCatagoryIndex()));
        }

        _catTable.getTable().getDataTable().selectRow(Integer.parseInt(req.getCatTableRow()), true);

        _searchMethodPanel.showWidget(_currSearchMethod.getIdx());
        if (_currentCatalog != null) updateSearchMax(_currentCatalog.getMaxArcSec());
        setTargetCard(computeTargetCard());
    }

    public int computeTargetCard() {
        int card;
        SearchMethods method = _methods.get(_currSearchMethod);
        _searchMethodPanel.setHeight(method.getHeight() + "px");
        _searchMethodPanel.setWidth(method.getWidth() + "px");
        if (method.usesTarget()) {
            if (isValidPos()) {
                card = TARGET_DESC;
            } else {
                card = TARGET_PANEL;
            }
        } else {
            card = TARGET_HIDDEN;
        }
        return card;
    }

    private boolean isValidPos() {
        return _targetPanel.getPos() != null;
    }

    private void createSearchMethodPanel() {
        _searchMethodPanel.setSize("300px", "75px");


        _methods.put(CatalogRequest.Method.CONE, new SearchMethods.Cone());
        _methods.put(CatalogRequest.Method.ELIPTICAL, new SearchMethods.Elliptical());
        _methods.put(CatalogRequest.Method.BOX, new SearchMethods.Box());
        _methods.put(CatalogRequest.Method.POLYGON, new SearchMethods.Polygon());
        _methods.put(CatalogRequest.Method.TABLE, new SearchMethods.Table());
        _methods.put(CatalogRequest.Method.ALL_SKY, new SearchMethods.AllSky());

        addMethodPanel(CatalogRequest.Method.CONE);
        addMethodPanel(CatalogRequest.Method.ELIPTICAL);
        addMethodPanel(CatalogRequest.Method.BOX);
        addMethodPanel(CatalogRequest.Method.POLYGON);
        addMethodPanel(CatalogRequest.Method.TABLE);
        addMethodPanel(CatalogRequest.Method.ALL_SKY);
    }


    private void addMethodPanel(CatalogRequest.Method method) {
        SearchMethods sm = _methods.get(method);
        _searchMethodPanel.add(sm.makePanel());
    }


    private void updateSearchMax(int maxArcSec) {
        SearchMethods sm = _methods.get(_currSearchMethod);
        if (sm != null) {
            sm.updateMax(maxArcSec);
        }
    }


    private void updateProj() {
        final List<Proj> projs = _catalogData.getProjects();
        _currentProj = projs.get(_projSelect.getSelectedIndex());
        updateCatagoryPanel(_currentProj);
    }

    private void updateCatagoryPanel(Proj proj) {

        _catagorySelect.clear();
        if (proj.getCatagoryCount() > 1) {
            _catagorySelect.setVisible(true);
            _catagoryLab.setVisible(true);
            for (Catagory catagories : proj) {
                _catagorySelect.addItem(catagories.getCatagoryName());
            }
        } else {
            _catagorySelect.setVisible(false);
            _catagoryLab.setVisible(false);

        }
        _currentCatagory = proj.get(0);
        updateCatalogPanel(_currentCatagory);
    }

    private void updateCatalogPanel(Catagory catagory) {
        _currentCatagory = catagory;
        _catTable.reloadTable(0);
        _catalogTitle.setText(catagory.getCatagoryName());
    }

    Proj findProj(String projName) {
        Proj retval = null;
        Proj testVal = new Proj(projName);
        List<Proj> projs = _catalogData.getProjects();
        if (projs.contains(testVal)) {
            retval = projs.get(projs.indexOf(testVal));
        }
        return retval;


    }

    private void updateToActive() {
        ActiveTarget at = ActiveTarget.getInstance();
        _targetPanel.setTarget(at.getActive());

        if (_currSearchMethod == null) return;
        NumberFormat formatter = NumberFormat.getFormat("#.000000");
        String ra, dec;

        SearchMethods method = _methods.get(_currSearchMethod);
        if (_searchMethod.getValue().equals(POLYGON) && at.getImageCorners() != null) {
            WorldPt[] wpts = at.getImageCorners();
            StringBuffer wptsString = new StringBuffer();
            if (wpts.length > 0) {
                ra = formatter.format(wpts[0].getLon());
                dec = formatter.format(wpts[0].getLat());
                wptsString.append(ra + " " + dec);
                for (int i = 1; i < wpts.length; i++) {
                    ra = formatter.format(wpts[i].getLon());
                    dec = formatter.format(wpts[i].getLat());
                    wptsString.append(", " + ra + " " + dec);
                }

            }
            ((SearchMethods.Polygon) method).getField().setValue(new String(wptsString));
        }

    }

    private void setTargetCard(int card) {
        if (card == TARGET_PANEL && _targetPanelHolder.getWidget() == null) {
            _targetPanelHolder.setWidget(_targetPanel);
        }
        if (card == TARGET_DESC) updateTargetDesc();

        if (card == TARGET_HIDDEN && _targetCards.getVisibleWidget() == TARGET_DESC) {
            Widget desc = _targetCards.getWidget(TARGET_DESC);
            Widget hidden = _targetCards.getWidget(TARGET_HIDDEN);
            hidden.setSize(desc.getOffsetWidth() + "px",
                    desc.getOffsetHeight() + "px");
        }


        _targetCards.showWidget(card);
    }

    private void updateTargetDesc() {
        ActiveTarget.PosEntry t = _targetPanel.getTarget();
        String wrapBegin = "<div style=\"text-align: center;\">";
        String wrapEnd = "</div>";
        String s = PositionFieldDef.formatTargetForHelp(t.getName(), t.getResolver(), t.getPt());
        String html = wrapBegin + s + wrapEnd;

        _targetDesc.setHTML(html);
    }


    private void createTargetPanel() {
        HorizontalPanel hp = new HorizontalPanel();
        Widget modTarget = GwtUtil.makeLinkButton(_prop.getTitle("modTarget"),
                _prop.getTip("modTarget"),
                new ClickHandler() {
                    public void onClick(ClickEvent ev) {
                        setTargetCard(TARGET_PANEL);
                    }
                });
        hp.add(_targetDesc);
        hp.setSpacing(5);
        hp.add(modTarget);

        updateTargetDesc();
        _targetCards.add(_targetPanelHolder);
        _targetCards.add(hp);
        _targetCards.add(new Label());
        setTargetCard(TARGET_DESC);
        _targetCards.addStyleName("image-select-target-area");
    }


    private void retrieveMasterCatalogAndVisible() {
        MasterCatTableTask.getMasterCatalog(_parent, _projectId, new MasterCatResponse() {
            public void newMasterCatalog(DataSet ds) {
                _catalogData = new CatalogData(ds);
                init();
                //setVisible(true);
            }

            public void masterCatalogFailed() {
                PopupUtil.showError(_prop.getTitle("masterFailed"),
                        _prop.getError("masterFailed"));
                _catalogData = null;
            }
        });
    }


    public void inputComplete() {

    }

    public String getTitle() {
        return _currentCatalog.getProjStr() + "-" + _currentCatalog.getQueryCatName();
    }


    public boolean validatePanel() throws ValidationException {
        boolean valid = true;

        if (_currentCatalog == null) {
            throw new ValidationException(_prop.getError("noCatSelected"));
        }


        SearchMethods method = _methods.get(_currSearchMethod);

        if (method.usesTarget() &&
                _targetCards.getVisibleWidget() == TARGET_PANEL) {


            if (_targetPanel.isAsyncCallRequired()) {
                _targetPanel.getFieldValuesAsync(new AsyncCallback<List<Param>>() {
                    public void onFailure(Throwable caught) {
                        if (caught != null) PopupUtil.showSevereError(caught);
                    }

                    public void onSuccess(List<Param> params) {

                        boolean valid = false;
                        for (Param p : params) {
                            if (p.getName().equals(ServerParams.USER_TARGET_WORLD_PT) &&
                                    p.getValue() != null) {
                                valid = true;
                            }
                        }
                        if (valid) {
                            if (_parent != null) _parent.setVisible(false);
                            inputComplete();
                        } else {
                            _targetPanel.validate();
                        }
                    }
                });
                valid = false;
            } else if (!isValidPos()) {
                throw new ValidationException(_prop.getError("mustEnterTarget"));
            }

        }


//            if (_targetPanel.isResolveNeeded() ) {
//                _targetPanel.resolvePosition(new AsyncCallback<WorldPt>() {
//                    public void onFailure(Throwable caught) {
//                        PopupUtil.showSevereError(caught);
//                    }
//                    public void onSuccess(WorldPt result) {
//                        if (result != null) {
//                            _parent.setVisible(false);
//                            inputComplete();
//                        } else {
//                            _targetPanel.validate();
//                        }
//                    }
//                });
//                valid= false;
//            }
//            else if (!isValidPos()) {
//                throw new ValidationException(_prop.getError("mustEnterTarget"));
//            }

        if (valid && !method.getRequireUpload()) valid = method.validate();


        if (!valid) {
            throw new ValidationException("One or more fields are not valid");
        }

        return valid;

    }

    private void initCatalogDisplay() {

        _catTable.getEventManager().addListener(TablePanel.ON_INIT, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                postInit(_catTable);
            }
        });


        CurrCatalogListener listener = new CurrCatalogListener();
        WebEventManager wem = _catTable.getEventManager();
        wem.addListener(TablePanel.ON_ROWHIGHLIGHT_CHANGE, listener);
        wem.addListener(TablePanel.ON_PAGE_LOAD, listener);

        _catTable.init();
    }


    void postInit(final TablePanel table) {
        table.showToolBar(false);
        table.showColumnHeader(false);
        table.showPagingBar(false);
        table.showOptionsButton(false);
//        table.getTable().setHeight("200px");

        table.getTable().setTableDefinition(new SingleColDefinition(
                new CatalogItem("Results", table.getDataset())));
    }


    /**
     * creating a new Dataset from the cached _catalogData. make sure to not send the original DataSet out.. since it
     * may get modify by others.
     *
     * @return
     */
    private DataSet makeCatalogDataset() {
        DataSet dataset = _catalogData.getDataSet();
        if (_currentCatagory != null) {
            dataset = dataset.subset(new CollectionUtil.FilterImpl<BaseTableData.RowData>() {
                public boolean accept(BaseTableData.RowData testRow) {
                    boolean retval = false;
                    for (Catalog cat : _currentCatagory) {
                        if (testRow.equals(cat.getDataSetRow())) {
                            retval = true;
                            break;
                        }
                    }
                    return retval;
                }
            });
        } else {
            dataset = dataset.clone();
        }
        return dataset;
    }

    private String getSelectedCatRow() {
        int idx = _catTable.getTable().getHighlightedRowIdx();
        return String.valueOf(idx);
    }

//====================================================================
//-------------------- Implementing InputFieldGroup -------------------

    //====================================================================

    public List<Param> getFieldValues() {
        SearchMethods method = _methods.get(_currSearchMethod);
        CatalogRequest request = new CatalogRequest(CatalogRequest.RequestType.GATOR_QUERY);
        //request.setParam("SearchId", CatalogRequest.RequestType.GATOR_QUERY.getSearchProcessor());
        request.setQueryCatName(_currentCatalog.getQueryCatName());
        request.setSelectedColumns(SelectedColumns);
        request.setConstraints(SelectedConstraints);
        request.setUse(CatalogRequest.Use.DATA_PRIMARY);
        request.setProjIndex(String.valueOf(_projSelect.getSelectedIndex()));
        request.setCatagoryIndex(String.valueOf(_catagorySelect.getSelectedIndex()));
        request.setCatTableRow(getSelectedCatRow());
//        request.setParam(TableServerRequest.SORT_INFO, new SortInfo(SortInfo.Direction.ASC,"dist").toString());
        if (method.usesTarget()) request.setWorldPtJ2000(_targetPanel.getJ2000Pos());
        method.setIntoRequest(request);

        // set catalogProject for project-level parameters for Gator
        request.setParam(CatalogRequest.CATALOG_PROJECT, _currentProj.getShortProjName());
        request.setParam("projectId", _projectId);

        return request.getParams();
    }

    public void setFieldValues(List<Param> list) {
        if (!list.isEmpty() && list.size() > 1) {
            CatalogRequest req = new CatalogRequest();
            req.setParams(list);
            _currSearchMethod = req.getMethod();
            SelectedColumns = req.getSelectedColumns();
            SelectedConstraints = req.getConstraints();
            //_reload = true;
            //init();
            reloadSearchMethodPanel(req);
        }
    }

    public boolean validate() {
        return true;
    }

    public void getFieldValuesAsync(final AsyncCallback<List<Param>> cb) {
        if (isAsyncCallRequired()) {
            final SearchMethods method = _methods.get(_currSearchMethod);
            method.upload(new AsyncCallback<String>() {
                public void onFailure(Throwable caught) {
                    cb.onFailure(caught);
                }

                public void onSuccess(String result) {
                    cb.onSuccess(getFieldValues());
                }
            });
        } else {
            cb.onSuccess(getFieldValues());
        }
    }

    public boolean isAsyncCallRequired() {
        SearchMethods method = _methods.get(_currSearchMethod);
        return method.getRequireUpload();
    }

    //====================================================================
//  implementing HasWidget
//====================================================================

    public void add(Widget w) {
        throw new UnsupportedOperationException("This operation is not allowed");
    }

    public void clear() {
        throw new UnsupportedOperationException("This operation is not allowed");
    }

    public Iterator<Widget> iterator() {
        return vp.iterator();
    }

    public boolean remove(Widget w) {
        throw new UnsupportedOperationException("This operation is not allowed");
    }

//====================================================================
//-------------------- Table related Inner Classes -------------------
//====================================================================

    public class CurrCatalogListener implements WebEventListener {
        public void eventNotify(WebEvent ev) {
            int idx = _catTable.getTable().getHighlightedRowIdx();
            if (idx >= 0) {
                BaseTableData.RowData row = (BaseTableData.RowData) _catTable.getTable().getRowValues().get(idx);
                _currentCatalog = new Catalog(row);
                updateSearchMax(_currentCatalog.getMaxArcSec());
            }
        }
    }


    private class FileteredDatasetLoader extends AbstractLoader<TableDataView> {

        @Override
        public void load(int offset, int pageSize, AsyncCallback<TableDataView> callback) {
            DataSet results = makeCatalogDataset();
            this.setCurrentData(results);
            callback.onSuccess(makeCatalogDataset());
        }

        public String getSourceUrl() {
            return null;
        }

        public TableServerRequest getRequest() {
            return null;
        }
    }

}
