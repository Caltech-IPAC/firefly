package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.visualize.ui.FitsDownloadOpsDialog;


public class FitsDownloadCmd extends BaseGroupVisCmd {
    public static final String CommandName= "FitsDownload";

    public FitsDownloadCmd() {
        super(CommandName);
    }


    protected void doExecute() {
        FitsDownloadOpsDialog.showOptions(getPlotView());
    }

    @Override
    public Image createCmdImage() {
        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        String iStr= getIconProperty();
        if (iStr!=null && iStr.equals("FitsDownload.Icon"))  {
            return new Image(ic.getSave());
        }
        return null;
    }

}