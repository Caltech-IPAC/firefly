package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.visualize.ui.RotateDialog;


public class RotateCmd extends BaseGroupVisCmd {
    public static final String COMMAND_NAME= "Rotate";
    private RotateDialog dialog= null;



    public RotateCmd() {
        super(COMMAND_NAME);
    }

    @Override
    protected boolean init() {
        dialog= new RotateDialog();
        return true;
    }

    protected void doExecute() { dialog.setVisible(true); }


    @Override
    protected Image createCmdImage() {
        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        String iStr= this.getIconProperty();
        if (iStr!=null && iStr.equals(COMMAND_NAME+".Icon"))  {
            return new Image(ic.getRotate());
        }
        return null;
    }

}