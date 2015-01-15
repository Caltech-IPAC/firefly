/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.creator;

import edu.caltech.ipac.firefly.ui.table.TablePanel;

import java.util.Map;

/**
 * Date: Dec 21, 2011
 *
 * @author loi
 * @version $Id: TableViewCreator.java,v 1.1 2012/01/05 02:09:25 loi Exp $
 */
public interface TableViewCreator extends UICreator {
    TablePanel.View create(Map<String, String> params);
}

