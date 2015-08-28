/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.visualize.ui.MaskAdjustDialog;


public class MaskOverlayCmd extends BaseGroupVisCmd {
    public static final String COMMAND_NAME= "maskOverlay";
    private MaskAdjustDialog dialog= null;



    public MaskOverlayCmd() {
        super(COMMAND_NAME);
    }

    @Override
    protected boolean init() {
        dialog= new MaskAdjustDialog();
        return true;
    }

    protected void doExecute() {
        dialog.setVisible(true);
    }


    @Override
    protected Image createCmdImage() {
        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        String iStr= this.getIconProperty();
        if (iStr!=null && iStr.equals(COMMAND_NAME+".Icon"))  {
            return new Image(ic.getImageMask());
        }
        return null;
    }

}