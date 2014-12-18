package edu.caltech.ipac.fuse.commands;

import edu.caltech.ipac.firefly.commands.BaseBackgroundSearchCmd;
import edu.caltech.ipac.firefly.ui.searchui.DummyInventoryUI;
import edu.caltech.ipac.firefly.ui.searchui.SearchUI;

import java.util.Arrays;
import java.util.List;

/**
 * Date: Sep 12, 2013
 *
 * @author loi
 * @version $Id: CommonRequestCmd.java,v 1.44 2012/10/03 22:18:11 loi Exp $
 */
public class FuseInventoryCmd extends BaseBackgroundSearchCmd {

    public static final String COMMAND_NAME = "FuseInventorySearch";

    public FuseInventoryCmd() {
        super(COMMAND_NAME);
    }

    @Override
    protected List<SearchUI> getSearchUIList() {
        return Arrays.asList((SearchUI) new DummyInventoryUI());
    }



}

