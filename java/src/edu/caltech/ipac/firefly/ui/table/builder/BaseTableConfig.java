package edu.caltech.ipac.firefly.ui.table.builder;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.commands.DownloadCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.background.BackgroundReport;
import edu.caltech.ipac.firefly.core.background.BackgroundState;
import edu.caltech.ipac.firefly.core.background.Backgroundable;
import edu.caltech.ipac.firefly.core.background.MonitorItem;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.table.BaseDownloadDialog;
import edu.caltech.ipac.firefly.ui.table.DownloadSelectionIF;
import edu.caltech.ipac.firefly.ui.table.Loader;
import edu.caltech.ipac.firefly.ui.table.PagingDataSetLoader;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.util.StringUtils;

/**
 * Date: Jan 7, 2010
 * @author balandra
 * @version $Id:
 */
public class BaseTableConfig<SReq extends TableServerRequest> implements TableConfig{

    private String shortDesc;
    private SReq searchReq;
    private String title;
    private String dlProcId;
    private String dlFilePrefix;
    private String dlTitlePrefix;
    private DownloadRequest downloadReq;
    private Loader<TableDataView> loader;
    private DownloadCmd downloadCmd;
    private DownloadSelectionIF downloadSelectionIF;

    public BaseTableConfig(SReq searchReq, String title, String shortDesc) {
        this(searchReq, title, shortDesc, null, null, null);
    }

    public BaseTableConfig(SReq searchReq, String title, String shortDesc, String dlProcId, String dlFilePrefix, String dlTitlePrefix) {
        this.shortDesc = shortDesc;
        this.searchReq = searchReq;
        this.title = title;
        this.dlProcId = dlProcId;
        this.dlFilePrefix = dlFilePrefix;
        this.dlTitlePrefix = dlTitlePrefix;
    }

    public String getTitle() {
        return title;
    }

    public String getShortDesc() {
        return shortDesc;
    }

    public SReq getSearchRequest() {
        return searchReq;
    }

    public String getDownloadFilePrefix() {
        return dlFilePrefix;
    }

    public String getDownloadTitlePrefix() {
        return dlTitlePrefix;
    }

    public DownloadRequest getDownloadRequest() {
        if (downloadReq == null) {
            downloadReq = makeDownloadRequest();
        }
        return downloadReq;
    }

    public DownloadCmd getDownloadCmd() {
        if (downloadCmd == null) {
            downloadCmd = makeDownloadCmd();
        }
        return downloadCmd;
    }

    public Loader<TableDataView> getLoader() {
        if (loader == null) {
            loader = makeLoader();
        }
        return loader;
    }

    public DownloadSelectionIF getDownloadSelectionIF() {
        return downloadSelectionIF;
    }

    public void setDownloadSelectionIF(DownloadSelectionIF downloadSelectionIF) {
        this.downloadSelectionIF = downloadSelectionIF;
    }

    /**
     * override this method to handle onload event.
     * @param data
     */
    protected void onLoad(TableDataView data) {}


//====================================================================
//
//====================================================================

    protected DownloadRequest makeDownloadRequest() {
        if (!StringUtils.isEmpty(dlProcId)) {
            DownloadRequest dlreq = new DownloadRequest(searchReq, getDownloadTitlePrefix(), getDownloadFilePrefix());
            dlreq.setRequestId(dlProcId);
            return dlreq;
        }
        return null;
    }

    protected Loader<TableDataView> makeLoader() {
        return getSearchRequest().isBackgroundable() ? new BackgroundablePagingLoader(this) : new PagingLoader(this);
    }

    protected DownloadCmd makeDownloadCmd() {
        DownloadRequest dlreq = getDownloadRequest();
        if (dlreq != null) {
            DownloadSelectionIF dlSelIF = downloadSelectionIF == null ? new DownloadDialog() : downloadSelectionIF;
            return new DownloadCmd(getLoader().getCurrentData(), dlSelIF);
        }
        return null;
    }

//====================================================================
//  Common Loader using SearchServices
//====================================================================

    static class DownloadDialog extends BaseDownloadDialog {

        public DownloadDialog() {
            super("Download Selection Dialog", null);
        }

        public void show() {
            PopupUtil.showInfo("Default Download Dialog goes here!!");
        }

        public void setVisible(boolean visible) {
        }
    }


    static class PagingLoader extends PagingDataSetLoader {
        private BaseTableConfig config;

        public PagingLoader(BaseTableConfig config) {
            super(config.getSearchRequest());
            this.config = config;
        }

        @Override
        public void onLoad(TableDataView result) {
            config.onLoad(result);
        }

        protected void doLoadData(TableServerRequest request, AsyncCallback<RawDataSet> passAlong) {
            SearchServices.App.getInstance().getRawDataSet(request, passAlong);

        }
    }

    static class BackgroundablePagingLoader extends PagingLoader implements Backgroundable {
        private BackgroundReport bgReport;
        private boolean isBackgrounded = false;
        private boolean dataReceived = false;

        public BackgroundablePagingLoader(BaseTableConfig config) {
            super(config);
        }

        public BackgroundReport getBgReport() {
            return bgReport;
        }

        public void backgrounded() {
            isBackgrounded = true;
        }

        public void cancelTask() {
            SearchServices.App.getInstance().cancel(bgReport.getID(), new AsyncCallback<Boolean>(){
                public void onFailure(Throwable caught) {}
                public void onSuccess(Boolean result) {}
            });
        }

        protected void doLoadData(final TableServerRequest request, final AsyncCallback<RawDataSet> passAlong) {
            if (dataReceived) {
                super.doLoadData(request, passAlong);
                return;
            }
            isBackgrounded = false;
            AsyncCallback<BackgroundReport> callback = new AsyncCallback<BackgroundReport>(){
                        public void onFailure(Throwable caught) {
                            passAlong.onFailure(caught);
                        }
                        public void onSuccess(BackgroundReport result) {
                            bgReport = result;
                            WebEventManager.getAppEvManager().addListener(Name.MONITOR_ITEM_UPDATE, new WebEventListener<MonitorItem>() {
                                public void eventNotify(WebEvent<MonitorItem> ev) {
                                    if (isBackgrounded) return;
                                    
                                    MonitorItem mi = ev.getData();
                                    BackgroundReport r= mi.getReport();
                                    if (r.getID().equals(bgReport.getID())) {
                                        if (r.getState().equals(BackgroundState.SUCCESS)) {
                                            dataReceived = true;
                                            WebEventManager.getAppEvManager().removeListener(this);

                                            SearchServices.App.getInstance().getRawDataSet(request, new AsyncCallback<RawDataSet>(){
                                                public void onFailure(Throwable caught) {}
                                                public void onSuccess(RawDataSet result) {
                                                    passAlong.onSuccess(result);
                                                }
                                            });
                                        }
                                    }
                                }
                            });
                        }
                    };
            SearchServices.App.getInstance().submitBackgroundSearch(request,
                            Application.getInstance().getRequestHandler().getCurrentSearchRequest(),  1, callback);
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

