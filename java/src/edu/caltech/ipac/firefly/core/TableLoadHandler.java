/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core;

import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.creator.PrimaryTableUI;


/**
 * Date: Apr 27, 2010
 *
 * @author loi
 * @version $Id: TableLoadHandler.java,v 1.2 2010/09/17 00:27:47 loi Exp $
 */
public interface TableLoadHandler {
    Widget getMaskWidget();
    void onLoad();
    void onError(PrimaryTableUI table, Throwable t);
    void onLoaded(PrimaryTableUI table);
    void onComplete(int totalRows);
}
