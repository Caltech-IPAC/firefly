package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.visualize.ui.FitsHeaderDialog;

/**
 * User: balandra
 * Date: Sep 16, 2009
 * Time: 10:08:02 AM
 */
public class FitsHeaderCmd extends BaseGroupVisCmd{
    public static final String CommandName= "FitsHeader";
    FitsHeaderDialog _dialog;

    public FitsHeaderCmd() {
        super(CommandName);
    }

    protected void doExecute() {
        _dialog = new FitsHeaderDialog(getMiniPlotWidget());
        _dialog.setVisible(true);

    }

    @Override
    public Image createCmdImage() {
        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        String iStr= getIconProperty();
        if (iStr!=null && iStr.equals(CommandName+".Icon"))  {
            return new Image(ic.getFitsHeader());
        }
        return null;
    }
}

