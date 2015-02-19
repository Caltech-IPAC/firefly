/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;


public class DataFilterOutCmd extends DataFilterCmd {
    public static final String CommandName= "DataFilterOut";

    public DataFilterOutCmd(MiniPlotWidget mpw) { super(CommandName, mpw, true); }


    @Override
    protected Image createCmdImage() {
        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        String iStr= this.getIconProperty();
        if (iStr!=null) {
            if (iStr.equals("DataFilterOut.Icon"))  {
                return new Image(ic.getFilterOut());
            }
        }
        return null;
    }

}