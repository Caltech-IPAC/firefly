/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.commands;

import edu.caltech.ipac.firefly.visualize.AllPlots;

public class ExpandCmd extends BaseGroupVisCmd {
    public static final String CommandName= "expand";

    public ExpandCmd() { super(CommandName); }

    protected void doExecute() { AllPlots.getInstance().forceExpandCurrent(); }
}