/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.visualize.WebPlot;


public class ZoomUpCmd extends ZoomCmd {
    public static final String CommandName= "zoomUp";

    public ZoomUpCmd() { super(CommandName, WebPlot.ZDir.UP); }

    @Override
    public Image createCmdImage() {
        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        String iStr= getIconProperty();
        if (iStr!=null && iStr.equals(CommandName+".Icon"))  {
            return new Image(ic.getZoomUp());
        }
        return null;
    }

}


