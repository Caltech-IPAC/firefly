package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.RootPanel;
import edu.caltech.ipac.firefly.visualize.ui.ColorStretchDialog;


public class ShowColorOpsCmd extends BaseGroupVisCmd {
    public static final String COMMAND_NAME= "showColorOps";
    ColorStretchDialog.AsyncCreator creator= null;



    public ShowColorOpsCmd() {
        super(COMMAND_NAME);
    }

    @Override
    protected boolean init() {
        creator= new ColorStretchDialog.AsyncCreator(RootPanel.get());
        return true;
    }

    protected void doExecute() { creator.show(); }


//    @Override
//    protected Image createCmdImage() {
//        VisIconCreator ic= VisIconCreator.Creator.getInstance();
//        String iStr= this.getIconProperty();
//        if (iStr!=null && iStr.equals(COMMAND_NAME+".Icon"))  {
//            return new Image(ic.getStretch());
//        }
//        return null;
//    }

    @Override
    public boolean hasIcon() { return false; }
}