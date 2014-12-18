package edu.caltech.ipac.firefly.ui.creator;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.table.DataSetTableModel;
import edu.caltech.ipac.firefly.ui.table.DownloadSelectionIF;
import edu.caltech.ipac.firefly.ui.table.Loader;

import java.util.List;

/**
 * Date: Apr 23, 2010
 *
 * @author loi
 * @version $Id: PrimaryTableUI.java,v 1.7 2010/09/27 22:23:45 loi Exp $
 */
public interface PrimaryTableUI extends ResultUIComponent {
    void load(AsyncCallback<Integer> callback);
    DataSetTableModel getDataModel();
    void addDownloadButton(DownloadSelectionIF downloadDialog, String downloadProcessorId,
                                  String filePrefix, String titlePrefix, List<Param> dlParamTags);
}
