package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.visualize.WebPlot;


public class ZoomOriginalCmd extends ZoomCmd {
    public static final String CommandName= "zoomOriginal";

    public ZoomOriginalCmd() { super(CommandName,WebPlot.ZDir.ORIGINAL); }

    @Override
    protected Image createCmdImage() {

        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        String iStr= this.getIconProperty();
        if (iStr!=null) {

            if (iStr.equals("zoomOriginal.Icon"))  {
                return new Image(ic.getZoomOriginal());
            }
        }
        return null;
    }

}