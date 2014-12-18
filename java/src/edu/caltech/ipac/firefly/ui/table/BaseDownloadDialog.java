package edu.caltech.ipac.firefly.ui.table;

import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.BaseDialog;
import edu.caltech.ipac.firefly.ui.ButtonType;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

/**
 * convenience base class to implements common methods for DownloadSelectionIF.
 *
 * Date: Mar 17, 2011
 *
 * @author loi
 * @version $Id: BaseDownloadDialog.java,v 1.2 2012/07/20 02:00:43 tlau Exp $
 */
public class BaseDownloadDialog extends BaseDialog implements DownloadSelectionIF {

    private DownloadRequest downloadRequest;
    private TableDataView view;
    private Validator validator;

    public BaseDownloadDialog(String title, String helpId) {
        super(null, ButtonType.OK_CANCEL,title,true, helpId);
    }

    public void setDownloadRequest(DownloadRequest downloadRequest) {
        this.downloadRequest = downloadRequest;
        WebEventManager.getAppEvManager().fireEvent(new WebEvent(downloadRequest, Name.DOWNLOAD_REQUEST_READY));
    }

    public DownloadRequest getDownloadRequest() {
        return downloadRequest;
    }

    public TableDataView getDataView() {
        return view;
    }

    public void setDataView(TableDataView view) {
        this.view = view;
    }

    public Validator getValidator() {
        return validator;
    }

    public void setValidator(Validator validator) {
        this.validator = validator;
    }
}
