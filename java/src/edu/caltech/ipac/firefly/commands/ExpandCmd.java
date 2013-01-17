package edu.caltech.ipac.firefly.commands;

public class ExpandCmd extends BaseGroupVisCmd {
    public static final String CommandName= "expand";

    public ExpandCmd() {
        super(CommandName);
    }


    protected void doExecute() {
        getMiniPlotWidget().toggleExpand();
    }
}