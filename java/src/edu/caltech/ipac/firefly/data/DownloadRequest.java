package edu.caltech.ipac.firefly.data;

import edu.caltech.ipac.firefly.data.table.SelectionInfo;

import java.io.Serializable;
import java.util.Collection;

public class DownloadRequest extends ServerRequest implements Serializable {



    public static final String TITLE_PREFIX= "TitlePrefix";
    public static final String FILE_PREFIX= "FilePrefix";
    public static final String BASE_FILE_NAME = "BaseFileName";
    public static final String TITLE = "Title";
    public static final String EMAIL = "Email";
    public static final String MAX_BUNDLE_SIZE = "MaxBundleSize";
    public static final String DATA_SOURCE = "DataSource";


    private TableServerRequest searchRequest;
    private SelectionInfo _selectInfo;


    public DownloadRequest() {}

    public DownloadRequest(TableServerRequest searchRequest, String titlePrefix, String filePrefix) {
        this.searchRequest = searchRequest;

        setTitlePrefix(titlePrefix);
        setFilePrefix(filePrefix);
    }

    public TableServerRequest getSearchRequest() {
        return this.searchRequest;
    }

    public void setTitlePrefix(String titlePrefix) {
        setParam(TITLE_PREFIX,titlePrefix);
    }

    public void setFilePrefix(String filePrefix) {
        setParam(FILE_PREFIX,filePrefix);
    }

    public void setSelectionInfo(SelectionInfo selectInfo) { this._selectInfo = selectInfo; }

    public void setBaseFileName(String baseFileName) { setParam(BASE_FILE_NAME,baseFileName); }

    public void setTitle(String title) { setParam(TITLE,title); }

    public void setDataSource(String source) { setParam(DATA_SOURCE,source); }

    public void setEmail(String email) { setParam(EMAIL,email); }

    public void setMaxBundleSize(long maxBundleSize) { setParam(MAX_BUNDLE_SIZE ,maxBundleSize+""); }

    public String getTitlePrefix() { return getParam(TITLE_PREFIX); }

    public String getFilePrefix() { return getParam(FILE_PREFIX); }

    public long getMaxBundleSize() { return getLongParam(MAX_BUNDLE_SIZE); }

    public Collection<Integer> getSelectedRows() { return _selectInfo.getSelected(); }

    public boolean isSelectAll () { return _selectInfo.isSelectAll(); }

    public String getBaseFileName() { return getParam(BASE_FILE_NAME); }

    public String getDataSource() { return getParam(DATA_SOURCE); }

    public String getTitle() { return getParam(TITLE); }

    public String getEmail() { return getParam(EMAIL); }


    @Override
    public void copyFrom(ServerRequest req) {
        super.copyFrom(req);
        if (req instanceof DownloadRequest) {
            DownloadRequest dreq = (DownloadRequest) req;
            searchRequest = (TableServerRequest)dreq.searchRequest.cloneRequest();
            _selectInfo = dreq._selectInfo;
        }
    }

    @Override
    public ServerRequest newInstance() {
        return new DownloadRequest();
    }

}