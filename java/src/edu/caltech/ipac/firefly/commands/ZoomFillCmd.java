package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.visualize.ZoomUtil;


public class ZoomFillCmd extends ZoomCmd {
    public static final String CommandName= "zoomFill";

    public ZoomFillCmd() { super(CommandName, ZoomUtil.FitFill.FILL); }

    @Override
    protected Image createCmdImage() {
        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        return new Image(ic.getZoomFill());
    }

}