package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;


public class CenterPlotOnQueryCmd extends BaseGroupVisCmd {
    public static final String CommandName= "centerPlotonQuery";

    public CenterPlotOnQueryCmd() {
        super(CommandName);
    }


    protected void doExecute() {
        for(MiniPlotWidget mpw : getGroupActiveList()) {
            mpw.getPlotView().smartCenter();
        }
    }


    @Override
    public Image createCmdImage() {
        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        String iStr= getIconProperty();
        if (iStr!=null && iStr.equals(CommandName+".Icon"))  {
            return new Image(ic.getCurrentTarget());
        }
        return null;
    }
}