/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.visualize.ui.DS9RegionLoadDialog;


public class LoadDS9RegionCmd extends BaseGroupVisCmd {
    public static final String COMMAND_NAME= "ds9Region";
    private DS9RegionLoadDialog dialog= null;



    public LoadDS9RegionCmd() {
        super(COMMAND_NAME);
    }

    @Override
    protected boolean init() {
        dialog= new DS9RegionLoadDialog();
        return true;
    }

    protected void doExecute() { dialog.setVisible(true); }


    @Override
    protected Image createCmdImage() {
        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        String iStr= this.getIconProperty();
        if (iStr!=null && iStr.equals(COMMAND_NAME+".Icon"))  {
            return new Image(ic.getDS9Symbol());
        }
        return null;
    }

}