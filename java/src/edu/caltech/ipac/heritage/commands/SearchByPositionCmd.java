package edu.caltech.ipac.heritage.commands;


import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import edu.caltech.ipac.firefly.core.BaseCallback;
import edu.caltech.ipac.firefly.core.HelpManager;
import edu.caltech.ipac.firefly.data.*;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.ui.*;
import edu.caltech.ipac.firefly.ui.catalog.CatalogSearchResponse;
import edu.caltech.ipac.firefly.ui.catalog.IrsaCatalogTask;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.ui.panels.*;
import edu.caltech.ipac.firefly.ui.table.DownloadSelectionIF;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.ui.table.builder.BaseTableConfig;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.heritage.data.entity.DataType;
import edu.caltech.ipac.heritage.searches.SearchByPosition;
import edu.caltech.ipac.heritage.ui.DownloadSelectionDialog;
import edu.caltech.ipac.heritage.ui.InstrumentPanel;
import edu.caltech.ipac.heritage.ui.MoreOptionsPanel;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.EnumFieldDef;
import edu.caltech.ipac.util.dd.StringFieldDef;
import edu.caltech.ipac.visualize.plot.WorldPt;


import java.util.*;


/**
 * Search by Position
 * @version $Id: SearchByPositionCmd.java,v 1.144 2013/01/05 22:14:07 tatianag Exp $
 */
public class SearchByPositionCmd extends HeritageRequestCmd {

    public static final String RADIUS_KEY = "SearchByPosition.field.radius";
    public static final String MATCH_BY_AOR_KEY = "SearchByPosition.field.matchByAOR";
    public static final String START_DATE_KEY = MoreOptionsPanel.START_DATE_KEY;
    public static final String END_DATE_KEY = MoreOptionsPanel.END_DATE_KEY;


    public static final String COMMAND_NAME= "SearchByPosition";
    public static final String UPLOADED_FILE_PATH = "UploadedFilePath";

    private SimpleTargetPanel targetPanel;
    private FormPanel uploadForm;
    private String targetListCachedID;
    private MaskPane uploadMask;

    private DataSet spitzerCatalogs;     // inventory search
    public static int irsEnhancedMaxUploadRadius; // in arcsecs
    public static int sourceListMaxUploadRadius; // in arcsecs
    public static int sourceListMaxConeRadius; // in arcsecs
    public static String sourceListCatalogName;

    private Request pendingInventoryRequest;


    public SearchByPositionCmd() {
        super(COMMAND_NAME);
    }

    @Override
    public boolean init() {
        TableServerRequest treq = new TableServerRequest("catScan");
        treq.setPageSize(1000);
        treq.setParam("projshort", "SPITZER");
        SearchServices.App.getInstance().getRawDataSet(treq, new BaseCallback<RawDataSet>(){
                    public void doSuccess(RawDataSet result) {
                        spitzerCatalogs = DataSetParser.parse(result);
                        for (int j = 0; j < spitzerCatalogs.getModel().getRows().size(); j++) {
                            TableData.Row ddr = spitzerCatalogs.getModel().getRow(j);
                            String desc = String.valueOf(ddr.getValue("description"));
                            if (desc.startsWith("Spitzer Enhanced Imaging") && desc.endsWith("Source List")) {
                                sourceListMaxUploadRadius = Integer.parseInt((String)ddr.getValue("uploadradius")); // in arcsecs
                                sourceListMaxConeRadius = Integer.parseInt((String)ddr.getValue("coneradius")); // in arcsecs
                                sourceListCatalogName = (String)ddr.getValue("catname");
                            } else if (desc.equals("IRS Enhanced Products")) {
                                irsEnhancedMaxUploadRadius = Integer.parseInt((String)ddr.getValue("uploadradius")); // in arcsecs
                            }
                        }
                        SearchByPositionCmd.this.setInit(true);
                    }
                });
        super.init();
        return false;
    }

    @Override
    public boolean isTagSupported(Request req) {
        return req.getParam(UPLOADED_FILE_PATH) == null;
    }

    protected Form createForm() {

        targetPanel = new SimpleTargetPanel();
        SimpleInputField radiusp = SimpleInputField.createByProp(RADIUS_KEY);
        final SimpleInputField matchByAOR = SimpleInputField.createByProp(MATCH_BY_AOR_KEY);
        matchByAOR.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent valueChangeEvent) {
                ((EnumFieldDef)matchByAOR.getFieldDef()).setDefaultValue(matchByAOR.getValue());
            }
        });
        final MoreOptionsPanel options = new MoreOptionsPanel(new DataType[]{DataType.IRS_ENHANCED, DataType.SM, DataType.LEGACY});


        Widget uploader = makeUploadWidget();

        TabPane tp = new TabPane();
        tp.addTab(targetPanel,"Position");
        tp.addTab(uploader,"From File");
        tp.selectTab(0);
        tp.setSize("540px", "135px");
        tp.addSelectionHandler(new SelectionHandler<Integer>() {
            public void onSelection(SelectionEvent<Integer> integerSelectionEvent) {
                options.setInventoryVisible(integerSelectionEvent.getSelectedItem() == 0);
            }
        });


//        if (BrowserUtil.isBrowser(Browser.FIREFOX)) {
//            tp.getDeckPanel().setSize("97%", "85%");
//        }

        FormBuilder.Config config = new FormBuilder.Config(FormBuilder.Config.Direction.HORIZONTAL,
                                            100, 5, HorizontalPanel.ALIGN_LEFT);
        DatePanel datePanel = new DatePanel((10 * 365 + 3) * 24 * 60 * 60, START_DATE_KEY, END_DATE_KEY, config);
        datePanel.setIntervalViolationError("Position searches can only cover 10-year period.");
        InstrumentPanel instPanel = new InstrumentPanel();
        VerticalPanel vpMO = new VerticalPanel();
        vpMO.add(matchByAOR);
        vpMO.add(instPanel);
        vpMO.add(datePanel);

        final CollapsiblePanel moreOptions = new CollapsiblePanel("More options", vpMO, false);

        instPanel.getEventManager().addListener(InstrumentPanel.ON_HIDE, new WebEventListener(){
                public void eventNotify(WebEvent ev) {
                    moreOptions.collapse();
                }
            });


        SimpleInputField rowFilter = SimpleInputField.createByDef(new StringFieldDef("ROWID", "row idx", "", "help", 50, 50, "", true, null));


        VerticalPanel vp = new VerticalPanel();
        vp.setSpacing(5);
        vp.add(tp);
        vp.add(radiusp);
        vp.add(options);
        //vp.add(GwtUtil.getFiller(50,5));
        vp.add(moreOptions);
        vp.add(rowFilter);

        Form form = new Form();
        form.setHelpId("searching.byPosition");
        form.add(vp);
        form.setFocus(TargetPanel.TARGET_NAME_KEY);
        return form;

    }

    @Override
    protected void createAndProcessRequest() {
        if (GwtUtil.isOnDisplay(uploadForm)) {
            if (validate().isValid()) {
                uploadForm.submit();
            } else {
                GwtUtil.showValidationError();
            }
        } else {
            targetListCachedID = null;
            targetPanel.getFieldValuesAsync(new AsyncCallback<List<Param>>() {
                public void onFailure(Throwable caught) {
                    PopupUtil.showSevereError(caught);
                }
                public void onSuccess(List<Param> list) {
                    SearchByPositionCmd.super.createAndProcessRequest();
                }
            });
        }
    }

//==================================================================
//  private supporting methods
//====================================================================


    private Widget makeUploadWidget() {
        uploadForm = new FormPanel();
        uploadForm.setAction("servlet/Firefly_FileUpload");

        // Because we're going to add a FileUpload widget, we'll need to set the
        // form to use the POST method, and multipart MIME encoding.
        uploadForm.setEncoding(FormPanel.ENCODING_MULTIPART);
        uploadForm.setMethod(FormPanel.METHOD_POST);
        uploadForm.addSubmitHandler(new FormPanel.SubmitHandler(){
            public void onSubmit(FormPanel.SubmitEvent event) {
                    uploadMask = GwtUtil.mask("uploading file...", getForm());
                }
        });
        uploadForm.addSubmitCompleteHandler(new FormPanel.SubmitCompleteHandler(){
            public void onSubmitComplete(FormPanel.SubmitCompleteEvent event) {
                    if (uploadMask != null) {
                        uploadMask.hide();
                    }
                    ServletReply reply = ServletReply.parse(event.getResults());
                    //if (reply == null || reply.getValue() == null) {
                    if(reply.getStatus() != 200) {
                        PopupUtil.showError("Server Error", reply.getMessage());
                    } else {
                        targetListCachedID = reply.getValue();                        
                        SearchByPositionCmd.super.createAndProcessRequest();
                        if(reply.getMessage() != null && reply.getMessage().length() > 0){
                            PopupUtil.showInfo(reply.getMessage());
                        }
                    }
                }
            });

        VerticalPanel vPanel = new VerticalPanel();
        vPanel.setSpacing(20);
//        vPanel.add(new HTML("Select a file:"));
        FileUpload fileUpload = new FileUpload();
        fileUpload.setName("targetListToUpload");
        GwtUtil.setFileUploadSize(fileUpload, "50");
        //fileUpload.setWidth("350px");
        vPanel.add(fileUpload);

        Widget icon = HelpManager.makeHelpIcon("searching.byBatch");
        HTML text = GwtUtil.makeFaddedHelp("Help on file format&nbsp;&nbsp;");
        HorizontalPanel batchPanel = new HorizontalPanel();
        batchPanel.add(text);
        batchPanel.add(icon);
        vPanel.add(batchPanel);

        uploadForm.setWidget(vPanel);
        return uploadForm;
    }

    @Override
    protected FormHub.Validated validate() {
        targetPanel.setIgnoreValidation(GwtUtil.isOnDisplay(uploadForm));
        return super.validate();
    }

    @Override
    protected void onFormSubmit(Request req) {
        if (targetListCachedID != null) {
            req.setParam(UPLOADED_FILE_PATH, targetListCachedID);
        }
    }

    protected void processRequest(final Request req, final AsyncCallback<String> callback) {

        boolean isMultiTargets = req.getParam(UPLOADED_FILE_PATH) != null;

        createTablePreviewDisplay();

        pendingInventoryRequest = null;
        if (MoreOptionsPanel.isAorRequested(req)) addTable(new SearchByPosition(SearchByPosition.Type.AOR, req, isMultiTargets));
        if (MoreOptionsPanel.isPbcdRequested(req)) addTable(new SearchByPosition(SearchByPosition.Type.PBCD, req, isMultiTargets));
        if (MoreOptionsPanel.isBcdRequested(req)) addTable(new SearchByPosition(SearchByPosition.Type.BCD, req, isMultiTargets));
        if (MoreOptionsPanel.isIrsEnhancedRequested(req))  {
            addTable(new SearchByPosition(SearchByPosition.Type.IRS_ENHANCED, req, isMultiTargets));
        }
        if (MoreOptionsPanel.isSupermosaicRequested(req)) {
            addTable(new SearchByPosition(SearchByPosition.Type.SUPERMOSAIC, req, isMultiTargets));
        }
        if (MoreOptionsPanel.isSourceListRequested(req)) {
            req.setParam(CatalogRequest.CATALOG, sourceListCatalogName);
            addTable(new SearchByPosition(SearchByPosition.Type.SOURCE_LIST, req, isMultiTargets));
        }
        if (MoreOptionsPanel.isInventoryRequested(req)) {
            pendingInventoryRequest = req;
        }

        loadAll();

        setResults(getResultsPanel());
    }

    @Override
    public void onComplete(int totalRows) {
        if (pendingInventoryRequest != null) {
            processInventoryRequest(pendingInventoryRequest, totalRows);
        } else {
            super.onComplete(totalRows);
        }
        String ids = getForm().getValue("ROWID");
        if (!StringUtils.isEmpty(ids)) {
            this.getResultsPanel().getSelectedTable().getTable().setFilters(Arrays.asList("ROWID IN (" + getForm().getValue("ROWID") + ")"));
            this.getResultsPanel().getSelectedTable().doFilters();
        }
    }


    // Inventory related

    private void processInventoryRequest(final Request req, final int loadedRows) {
        req.setParam("InventorySearch.radius", req.getParam(RADIUS_KEY));
        TableServerRequest treq = new TableServerRequest("inventorySummary", req);
        treq.setPageSize(1000);
        SearchServices.App.getInstance().getRawDataSet(treq, new BaseCallback<RawDataSet>(){
            int rowsReturned = 0;

            public void doSuccess(RawDataSet result) {
                DataSet ds = DataSetParser.parse(result);
                TableDataView.Column c = ds.findColumn("count");
                if (c != null) {
                    c.setAlign(TableDataView.Align.RIGHT);
                }
                DoInventorySearch act = new DoInventorySearch(req);
                SearchSummaryPanel ssp = SearchSummaryPanel.createDataSetSummary(null, ds, ds.getMeta().getGroupByCols(), act);
                ssp.setHelpId("results.contribenhprod");
                SearchByPositionCmd.this.addTab(ssp, "Contributed Products", false);
                rowsReturned = result.getTotalRows();
                // search catalogs
                addCatalogSearch(req, ssp);
                ssp.layout();

                // HeritageRequestCmd is creating necessary previews
                //SearchByPositionCmd.this.addPreview(new CoveragePreview(new SimpleCoverageData(null, ZoomType.SMART)));
                //SearchByPositionCmd.this.addPreview(new DataViewerPreview(new HeritagePreviewData()));
            }

            @Override
            public void doFinally() {
                SearchByPositionCmd.super.onComplete(rowsReturned>0 ? rowsReturned : loadedRows);
            }
        });
    }

    private void addCatalogSearch(final Request req, final SearchSummaryPanel ssp) {

        // cat summary can return sets that are not legacy
        // the catalog is legacy if one of the following is true:
        // subtitle of the catalog starts with legacyTitlePrefix
        // or subtitle valid sets (ignore case)                 
        final String legacyTitlePrefix = "Spitzer Legacy Science Program (";

        TableServerRequest treq = new TableServerRequest("catSummary");
        treq.setPageSize(1000);
        treq.setParam("projshort", "spitzer");
        WorldPt pt = req.getWorldPtParam(ReqConst.USER_TARGET_WORLD_PT);
        String radius = req.getParam(RADIUS_KEY);
        treq.setParam("ra_dec_radius", pt.getLon()+","+pt.getLat()+","+radius);
        SearchServices.App.getInstance().getRawDataSet(treq, new BaseCallback<RawDataSet>(){
            public void doSuccess(RawDataSet result) {
                DataSet matches = DataSetParser.parse(result);
                TableData td = matches.getModel();
                ArrayList<String> columns = new ArrayList<String>();
                columns.addAll(Arrays.asList("datatype", "set", "description", "count"));

                for (int i=0; i < td.getRows().size(); i++) {
                    TableData.Row r = td.getRow(i);
                    String set = String.valueOf(r.getValue("set"));
                    final String catname = String.valueOf(r.getValue("catalog"));
                    String count = String.valueOf(r.getValue("count"));
                    String ddlink = (String) r.getValue("ddlink");

                    String desc = catname;
                    boolean isLegacy = false;
                    for (int j = 0; j < spitzerCatalogs.getModel().getRows().size(); j++) {
                        TableData.Row ddr = spitzerCatalogs.getModel().getRow(j);
                        String catalog = String.valueOf(ddr.getValue("catname"));
                        if (catname.equals(catalog)) {
                            String title = String.valueOf(ddr.getValue("subtitle"));
                            if (title.startsWith(legacyTitlePrefix) && title.indexOf(")")>legacyTitlePrefix.length()) {
                                set = title.substring(legacyTitlePrefix.length(), title.indexOf(")"));
                                isLegacy = true;
                            }

                            if (!isLegacy) {
                                // look for pattern ... (SETNAME) ... in subtitle
                                int idxStart = title.indexOf("(");
                                int idxEnd = title.indexOf(")");
                                if (idxStart>0 && idxEnd>0 && idxEnd>idxStart) {
                                    set = title.substring(idxStart+1, idxEnd);
                                    isLegacy = true;
                                // special cases with no braces in subtitle    
                                } else if (set.equalsIgnoreCase("Abell1763")) {
                                    // galaxy cluster Abell 1763 catalog
                                    set = "Abell1763";
                                    isLegacy = true;
                                } else if (set.equalsIgnoreCase("SpUDS")) {
                                    set = "SpUDS";
                                    isLegacy = true;
                                }
                            }

                            if (isLegacy) {
                                desc = String.valueOf(ddr.getValue("description"));
                            }
                            break;
                        }
                    }
                    if (!isLegacy) {continue;}

                    if (!StringUtils.isEmpty(ddlink)) {
                        desc = desc + "&nbsp;&nbsp;&nbsp;(" + ddlink + ")";
                    }

                    SearchSummaryItem ssi = new SearchSummaryItem();
                    ssi.setLoaded(true);
                    ssi.setColumns(columns);
                    ssi.setValue("datatype", "catalogs");
                    ssi.setValue("set", set);
                    ssi.setValue("description", desc);
                    ssi.setValue("count", count);
                    ssi.getCellData("count").setHalign(HasHorizontalAlignment.ALIGN_RIGHT);                    

                    final CatalogRequest creq = new CatalogRequest(CatalogRequest.RequestType.GATOR_QUERY);
                    creq.setUse(CatalogRequest.Use.DATA_PRIMARY);
                    creq.setRequestId(CatalogRequest.RequestType.GATOR_QUERY.getSearchProcessor());
                    creq.setParam(ReqConst.USER_TARGET_WORLD_PT, req.getParam(ReqConst.USER_TARGET_WORLD_PT));
                    creq.setParam(CatalogRequest.RADIUS, req.getParam(RADIUS_KEY));
                    creq.setParam(CatalogRequest.RAD_UNITS, "degree");
                    //creq.setPageSize(50);
                    creq.setQueryCatName(catname);
                    creq.setMethod(CatalogRequest.Method.CONE);


                    // loading catalog in background
                    final String fset = set;
                    //final String fdesc = desc;                                                
                    ssi.setActivation(new SearchSummaryItem.Activation() {
                        public void activate(SearchSummaryItem ssi) {
                            int cX= ssp.getAbsoluteLeft()+ ssp.getOffsetWidth()/2;
                            int cY= ssp.getAbsoluteTop()+ ssp.getOffsetHeight()/2;
                            IrsaCatalogTask.getCatalog(ssp,creq,new CatalogSearchResponse(){
                                public void showNoRowsReturned() {
                                    PopupUtil.showError("No Rows Returned", "No Rows Returned");
                                }
                                public void status(RequestStatus requestStatus) { }
                            }, cX, cY, fset+"_"+catname);

                            //BaseTableConfig tconfig = new BaseTableConfig(creq, fset+"_"+catname, fdesc);
                            //NewTableResults tr = new NewTableResults(tconfig, WidgetFactory.TABLE);
                            //WebEventManager.getAppEvManager().fireEvent(new WebEvent<NewTableResults>(this, Name.NEW_TABLE_RETRIEVED, tr));
                        }
                    });
                    ssp.addItem(ssi);
                }
                ssp.layout();
            }
        });

    }


    static class DoInventorySearch implements SearchSummaryItem.Activation {
        Request req;

        DoInventorySearch(Request req) {
            this.req = req;
        }

        public void activate(SearchSummaryItem ssi) {
            TableServerRequest treq = new TableServerRequest("heritageInventorySearch", req);
            treq.setPageSize(50);
            DataSetSummaryItem dsi = (DataSetSummaryItem) ssi;
            for (Object s : dsi.getRow().getValues().keySet()) {
                String key = String.valueOf(s);
                treq.setParam(key, dsi.getValue(key));
            }

            BaseTableConfig config;
            if (dsi.getValue("set").equals("IRS_Enhanced")) {
                req.setParam(SearchByPositionCmd.RADIUS_KEY, req.getParam(RADIUS_KEY));
                config = new SearchByPosition(SearchByPosition.Type.IRS_ENHANCED, req, false);
            } else if (dsi.getValue("set").equals("Spitzer")) {
                req.setParam(SearchByPositionCmd.RADIUS_KEY, req.getParam(RADIUS_KEY));
                config = new SearchByPosition(SearchByPosition.Type.PBCD, req, false);
            } else {
                config = new BaseTableConfig<TableServerRequest>(treq, dsi.getValue("sname"), dsi.getValue("description"), "inventoryDownload", getInventoryDownloadFilePrefix(dsi.getValue("set")), getInventoryDownloadTitlePrefix(dsi.getValue("set"))){
                    @Override
                    public DownloadSelectionIF getDownloadSelectionIF() {
                        DownloadSelectionIF dsif = super.getDownloadSelectionIF();
                        if (dsif == null) {
                            DownloadSelectionDialog dsd = new DownloadSelectionDialog(DownloadSelectionDialog.DialogType.LEGACY, getDownloadRequest(), getLoader().getCurrentData());
                            this.setDownloadSelectionIF(dsd);
                            return dsd;
                        }
                        return dsif;
                    }
                };
            }
            NewTableResults tr = new NewTableResults(config, WidgetFactory.TABLE);
            WebEventManager.getAppEvManager().fireEvent(new WebEvent<NewTableResults>(this, Name.NEW_TABLE_RETRIEVED, tr));
        }
    }


    private static String getInventoryDownloadFilePrefix(String setid) {
        return setid.toLowerCase() + "-";
    }

    private static String getInventoryDownloadTitlePrefix(String setid) {
        return setid + ": ";
    }

}
