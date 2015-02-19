/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.visualize.WebPlot;


public class ZoomDownCmd extends ZoomCmd {
    public static final String CommandName= "zoomDown";

    public ZoomDownCmd() { super(CommandName,WebPlot.ZDir.DOWN); }

    @Override
    protected Image createCmdImage() {

        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        String iStr= this.getIconProperty();
        if (iStr!=null) {

            if (iStr.equals("zoomDown.Icon"))  {
                return new Image(ic.getZoomDown());
            }
        }
        return null;
    }

}