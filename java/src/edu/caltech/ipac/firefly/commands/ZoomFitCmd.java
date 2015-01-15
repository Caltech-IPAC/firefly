/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.visualize.ZoomUtil;


public class ZoomFitCmd extends ZoomCmd {
    public static final String CommandName= "zoomFit";

    public ZoomFitCmd() { super(CommandName, ZoomUtil.FitFill.FIT); }

    @Override
    protected Image createCmdImage() {
        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        return new Image(ic.getZoomFit());
    }

}