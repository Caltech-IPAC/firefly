package edu.caltech.ipac.firefly.ui.creator;

import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.SearchDescResolver;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.ui.creator.eventworker.ActiveTargetCreator;
import edu.caltech.ipac.firefly.ui.creator.eventworker.AllPlotsQueryCreator;
import edu.caltech.ipac.firefly.ui.creator.eventworker.DatasetQueryCreator;
import edu.caltech.ipac.firefly.ui.creator.eventworker.DrawingLayerCreator;
import edu.caltech.ipac.firefly.ui.creator.eventworker.EventWorker;
import edu.caltech.ipac.firefly.ui.creator.eventworker.EventWorkerCreator;
import edu.caltech.ipac.firefly.ui.creator.eventworker.FormEventWorker;
import edu.caltech.ipac.firefly.ui.creator.eventworker.FormEventWorkerCreator;
import edu.caltech.ipac.firefly.ui.creator.eventworker.RowTargetCreator;
import edu.caltech.ipac.firefly.ui.creator.eventworker.TableViewListenerCreator;
import edu.caltech.ipac.firefly.ui.table.TablePreview;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.visualize.ui.PlotTypeUI;

import java.util.HashMap;
import java.util.Map;

/**
 * Date: Mar 25, 2010
 *
 * @author loi, Trey
 * @version $Id: WidgetFactory.java,v 1.31 2012/11/30 23:17:01 roby Exp $
 */
public class WidgetFactory {

    public static final String DATA_VIEW_3COLOR = "DATA_VIEW_3COLOR";
    public static final String DATA_VIEW = "DATA_VIEW";
    public static final String COVERAGE_VIEW = "COVERAGE_VIEW";
    public static final String DATA_SOURCE_COVERAGE_VIEW = "DataSourceCoverageView";
    public static final String TABLE = "TABLE";
    public static final String BASIC_TABLE = "BasicTable";
    public static final String BASIC_TABLE_PREVIEW = "BasicTablePreview";
    public static final String TABLE_FILTERING_PREVIEW = "TableFiltering";
    public static final String ROW_DETAILS_PREVIEW = "RowDetails";
    public static final String BASIC_IMAGEGRID_TABLE = "BasicPreviewImageGrid";
    public static final String IMAGEGRID_VIEW = "ImageGridView";
    public static final String XY_TABLE_VIEW = "XYTableView";

    public static final String TARGET_PANEL = "targetPanel";
    public static final String SIMPLE_TARGET_PANEL = "simpleTargetPanel";
    public static final String NAIF_TARGET_PANEL = "naifTargetPanel";
    public static final String CATALOG_POPUP_PANEL = "CatalogPopupPanel";

    public static final String EXAMPLE_FORM_WORKER = "ExampleFormWorker";
    public static final String PREVIEW_FORM_WORKER = "PreviewMgrFormWorker";
    public static final String SEARCH_DESC_RESOLVER_SUFFIX = "SearchDescResolver";


    private HashMap<String, UICreator> mappings = new HashMap<String, UICreator>();

    public WidgetFactory() {
        addCreator(DATA_VIEW_3COLOR, new ThreeColorDataViewCreator());
        addCreator(DATA_VIEW, new SimpleDataViewCreator());
        addCreator(COVERAGE_VIEW, new CoverageCreator());
        addCreator(TABLE, new SelectableTablePanelCreator());
        addCreator(BASIC_TABLE, new TablePanelCreator());
        addCreator(ROW_DETAILS_PREVIEW, new RowDetailsCreator());
        addCreator(BASIC_IMAGEGRID_TABLE, new PreviewImageGridCreator());
        addCreator(IMAGEGRID_VIEW, new ImageGridViewCreator());
        addCreator(XY_TABLE_VIEW, new XYPlotViewCreator());

        addCreator(TARGET_PANEL, new TargetPanelCreator());
        addCreator(SIMPLE_TARGET_PANEL, new SimpleTargetPanelCreator());
        addCreator(NAIF_TARGET_PANEL, new NaifTargetPanelCreator());
        addCreator(CATALOG_POPUP_PANEL, new CatalogPopupPanelCreator());
        addCreator(DatasetQueryCreator.DATASET_QUERY, new DatasetQueryCreator());
        addCreator(DrawingLayerCreator.DATASET_VIS_QUERY, new DrawingLayerCreator());
        addCreator(CommonParams.ACTIVE_TARGET, new ActiveTargetCreator());
        addCreator(RowTargetCreator.ROW_TARGET, new RowTargetCreator());
        addCreator(TableViewListenerCreator.TYPE, new TableViewListenerCreator());

        addCreator(EXAMPLE_FORM_WORKER, new ExampleFormEvWorkerCreator());
        addCreator(PREVIEW_FORM_WORKER, new PreviewMgrFormWorkerCreator());
        addCreator(BASIC_TABLE_PREVIEW, new BasicTablePreviewCreator());
        addCreator(TABLE_FILTERING_PREVIEW, new TableFilteringPreviewCreator());
        addCreator(DATA_SOURCE_COVERAGE_VIEW, new DataSourceCoverageCreator());
        addCreator(AllPlotsQueryCreator.ALL_PLOTS_QUERY, new AllPlotsQueryCreator());
    }

    public void addCreator(String id, UICreator creator) {
        if (id != null) id = id.toLowerCase();
        if (creator != null) {
            mappings.put(id, creator);
        }
    }

    public boolean removeCreator(String id) {
        if (id != null) id = id.toLowerCase();
        return mappings.remove(id) != null;
    }

    public PrimaryTableUI createPrimaryUI(String id,
                                          TableServerRequest req,
                                          Map<String, String> params) throws IllegalArgumentException {
        if (id != null) id = id.toLowerCase();
        PrimaryTableUI retval = null;
        UICreator c = get(id);

        if (c instanceof PrimaryTableCreator) {
            retval = ((PrimaryTableCreator) c).create(req, params);
        }
        return retval;
    }


    public TablePreview createObserverUI(String id,
                                         Map<String, String> params) throws IllegalArgumentException {
        if (id != null) id = id.toLowerCase();
        TablePreview retval = null;
        UICreator c = get(id);

        if (c instanceof ObsResultCreator) {
            retval = ((ObsResultCreator) c).create(params);
        }
        return retval;
    }

    public EventWorker createEventWorker(String id, Map<String, String> params)
            throws IllegalArgumentException {
        if (id != null) id = id.toLowerCase();
        EventWorker retval = null;
        UICreator c = get(id);

        if (c instanceof EventWorkerCreator) {
            retval = ((EventWorkerCreator) c).create(params);
        }
        return retval;
    }


    public FormEventWorker createFormEventWorker(String id, Map<String, String> params)
            throws IllegalArgumentException {
        if (id != null) id = id.toLowerCase();
        FormEventWorker retval = null;
        UICreator c = get(id);

        if (c instanceof FormEventWorkerCreator) {
            retval = ((FormEventWorkerCreator) c).create(params);
        }
        return retval;
    }

    public Widget createFormWidget(String id, Map<String, String> params)
            throws IllegalArgumentException {
        if (id != null) id = id.toLowerCase();
        Widget retval = null;
        UICreator c = get(id);

        if (c instanceof FormWidgetCreator) {
            retval = ((FormWidgetCreator) c).create(params);
        }
        return retval;
    }


    public PlotTypeUI createPlotTypeUI(String id, Map<String, String> params) throws IllegalArgumentException {
        if (id != null) id = id.toLowerCase();
        PlotTypeUI retval = null;
        UICreator c = get(id);

        if (c instanceof PlotTypeUICreator) {
            retval = ((PlotTypeUICreator) c).create(params);
        }
        return retval;
    }

    public SearchDescResolver createSearchDescResolver(String appName) throws IllegalArgumentException {
        UICreator c = get(appName + "-" + SEARCH_DESC_RESOLVER_SUFFIX);
        if (c instanceof SearchDescResolverCreator) {
            return((SearchDescResolverCreator) c).create();
        }
        return new SearchDescResolver();
    }

    public TablePanel.View createTablePanelView(String type, Map<String, String> params) throws IllegalArgumentException {
        UICreator c = get(type);
        if (c instanceof TableViewCreator) {
            return((TableViewCreator) c).create(params);
        }
        return null;
    }


    private UICreator get(String id) {
        if (id != null) id = id.toLowerCase();
        return mappings.get(id);


    }

}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
