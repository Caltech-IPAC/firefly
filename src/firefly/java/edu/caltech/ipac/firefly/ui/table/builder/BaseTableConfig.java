/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.table.builder;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.commands.DownloadCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.background.BackgroundState;
import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
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
        private BackgroundStatus bgStat;
        private boolean isBackgrounded = false;
        private boolean dataReceived = false;

        public BackgroundablePagingLoader(BaseTableConfig config) {
            super(config);
        }

        public BackgroundStatus getBgStatus() {
            return bgStat;
        }

        public void backgrounded() {
            isBackgrounded = true;
        }

        public boolean canBackground() {
            return !dataReceived;
        }

        public void cancelTask() {
            if (!dataReceived) {
                SearchServices.App.getInstance().cancel(bgStat.getID(), new AsyncCallback<Boolean>(){
                    public void onFailure(Throwable caught) {}
                    public void onSuccess(Boolean result) {}
                });
            }
        }

        protected void doLoadData(final TableServerRequest request, final AsyncCallback<RawDataSet> passAlong) {
            if (dataReceived) {
                super.doLoadData(request, passAlong);
                return;
            }
            isBackgrounded = false;
            AsyncCallback<BackgroundStatus> callback = new AsyncCallback<BackgroundStatus>(){
                        public void onFailure(Throwable caught) {
                            passAlong.onFailure(caught);
                        }
                        public void onSuccess(BackgroundStatus result) {
                            bgStat = result;
                            WebEventManager.getAppEvManager().addListener(Name.MONITOR_ITEM_UPDATE, new WebEventListener<MonitorItem>() {
                                public void eventNotify(WebEvent<MonitorItem> ev) {
                                    if (isBackgrounded) return;
                                    
                                    MonitorItem mi = ev.getData();
                                    BackgroundStatus r= mi.getStatus();
                                    if (r.getID().equals(bgStat.getID())) {
                                        if (r.getState().equals(BackgroundState.SUCCESS)) {
                                            dataReceived = true;
                                            WebEventManager.getAppEvManager().removeListener(this);
                                            SearchServices.App.getInstance().getRawDataSet(request, passAlong);
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
