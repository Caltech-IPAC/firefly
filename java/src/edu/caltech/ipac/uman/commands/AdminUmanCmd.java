package edu.caltech.ipac.uman.commands;

import edu.caltech.ipac.firefly.ui.Form;

/**
 * @author loi
 * $Id: UmanCmd.java,v 1.13 2012/11/19 22:05:43 loi Exp $
 */
abstract public class AdminUmanCmd extends UmanCmd {

    public AdminUmanCmd(String command, String accessRole) {
        super(command, accessRole);
        setAutoSubmit(true);
    }

    @Override
    protected Form createForm() {
        return null;
    }

    @Override
    protected void updateSearchPanel() {}
}
