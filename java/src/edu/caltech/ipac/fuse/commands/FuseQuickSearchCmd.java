package edu.caltech.ipac.fuse.commands;

//import edu.caltech.ipac.fuse.ui.PopularQuickSearchUI;
import edu.caltech.ipac.firefly.commands.BaseBackgroundSearchCmd;
import edu.caltech.ipac.firefly.ui.searchui.LoadCatalogFromVOSearchUI;
import edu.caltech.ipac.firefly.ui.searchui.LoadCatalogSearchUI;
import edu.caltech.ipac.firefly.ui.searchui.PopularQuickSearchUI;
import edu.caltech.ipac.firefly.ui.searchui.SearchUI;

import java.util.Arrays;
import java.util.List;

/**
 * Date: Sep 12, 2013
 *
 * @author loi
 * @version $Id: CommonRequestCmd.java,v 1.44 2012/10/03 22:18:11 loi Exp $
 */
public class FuseQuickSearchCmd extends BaseBackgroundSearchCmd {

    public static final String COMMAND_NAME = "FuseQuickSearch";

    public FuseQuickSearchCmd() {
        super(COMMAND_NAME);
    }

    @Override
    protected List<SearchUI> getSearchUIList() {
        return Arrays.asList(
                new LoadCatalogFromVOSearchUI(),
                new LoadCatalogSearchUI(),
                new PopularQuickSearchUI());
    }


}

