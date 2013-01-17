package edu.caltech.ipac.heritage.data.entity.download;

import edu.caltech.ipac.firefly.data.DownloadRequest;

import java.io.Serializable;

/**
 * @author tatianag
 *         $Id: InventoryDownloadRequest.java,v 1.2 2010/11/29 22:14:28 roby Exp $
 */
public class InventoryDownloadRequest extends DownloadRequest implements Serializable {

    private boolean _includeRelated;

    public InventoryDownloadRequest() {}

    public InventoryDownloadRequest(DownloadRequest req, boolean includeRelated) {
        _includeRelated= includeRelated;
        copyFrom(req);
        setRequestId("inventoryDownload");
        setDataSource("Spitzer");
    }

    public boolean includeRelated() { return _includeRelated; }
}
