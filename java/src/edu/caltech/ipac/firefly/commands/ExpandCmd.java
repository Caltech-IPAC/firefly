package edu.caltech.ipac.firefly.commands;

import edu.caltech.ipac.firefly.visualize.AllPlots;

public class ExpandCmd extends BaseGroupVisCmd {
    public static final String CommandName= "expand";

    public ExpandCmd() { super(CommandName); }

    protected void doExecute() { AllPlots.getInstance().forceExpand(); }
}