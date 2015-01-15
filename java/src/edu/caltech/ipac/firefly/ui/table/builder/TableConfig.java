/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.table.builder;

import edu.caltech.ipac.firefly.commands.DownloadCmd;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.table.DownloadSelectionIF;
import edu.caltech.ipac.firefly.ui.table.Loader;

/**
 * Date: Jun 9, 2009
 *
 * @author loi
 * @version $Id: TableConfig.java,v 1.8 2010/09/27 22:23:44 loi Exp $
 */
public interface TableConfig {

    String getTitle();
    String getShortDesc();
    TableServerRequest getSearchRequest();
    DownloadRequest getDownloadRequest();
    DownloadCmd getDownloadCmd();
    Loader<TableDataView> getLoader();
    DownloadSelectionIF getDownloadSelectionIF();
}
