package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;

public class RestoreCmd extends BaseGroupVisCmd {
    public static final String CommandName= "restore";

    public RestoreCmd() {
        super(CommandName);
    }


    protected void doExecute() {
        AllPlots.getInstance().disableWCSMatch();
        for (MiniPlotWidget mpw : getGroupActiveList()) {
            mpw.getOps().restoreDefaults();
        }
    }

    @Override
    public Image createCmdImage() {
        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        String iStr= getIconProperty();
        if (iStr!=null && iStr.equals(CommandName+".Icon"))  {
            return new Image(ic.getRestore());
        }
        return null;
    }
}