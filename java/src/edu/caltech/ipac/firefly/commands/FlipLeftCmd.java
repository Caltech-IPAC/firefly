/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;


public class FlipLeftCmd extends FlipCmd {
    public static final String CommandName= "flipLeft";

    public FlipLeftCmd(MiniPlotWidget mpw) { super(CommandName,mpw, FlipDir.LEFT); }

    @Override
    public Image createCmdImage() {
        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        String iStr= getIconProperty();
        if (iStr!=null && iStr.equals(CommandName+".Icon"))  {
            return new Image(ic.getSideLeftArrow());
        }
        return null;
    }

}