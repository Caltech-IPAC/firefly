/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.commands;

import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.panels.SearchPanel;

/**
 * @author tatianag
 *  $Id: SearchCmd.java,v 1.5 2011/09/29 01:32:14 loi Exp $
 */
public class SearchCmd  extends GeneralCommand {

    public static String COMMAND_NAME = "Search";

    public SearchCmd() {
        super(COMMAND_NAME);
    }

    protected void doExecute() {
        Request req = Application.getInstance().getRequestHandler().getCurrentSearchRequest();
        if (req != null) {
            req.setDoSearch(false);
            req.setIsSearchResult(false);
            Application.getInstance().processRequest(req);
        } else {
            SearchPanel.getInstance().processDefaultCommand();
        }
    }
}
