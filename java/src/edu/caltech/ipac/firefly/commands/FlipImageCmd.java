package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;

import java.util.List;


public class FlipImageCmd extends BaseGroupVisCmd {
    public static final String COMMAND_NAME= "FlipImage";



    public FlipImageCmd() {
        super(COMMAND_NAME);
    }

    @Override
    protected boolean init() {
        return true;
    }

    protected void doExecute() {
        List<MiniPlotWidget> mpwList= getActiveList();
        if (mpwList.size()>0) {
            for(MiniPlotWidget mpwItem : getActiveList()) {
                    mpwItem.getOps().flipImage();
            }
        }
    }


    @Override
    protected Image createCmdImage() {
        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        String iStr= this.getIconProperty();
        if (iStr!=null && iStr.equals(COMMAND_NAME+".Icon"))  {
            return new Image(ic.getFlip());
        }
        return null;
    }

}