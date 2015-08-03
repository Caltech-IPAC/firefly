/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.RootPanel;
import edu.caltech.ipac.firefly.visualize.JsColorStretchDialog;
import edu.caltech.ipac.firefly.visualize.ui.ColorStretchDialog;


public class ShowColorOpsCmd extends BaseGroupVisCmd {
    public static final String COMMAND_NAME= "showColorOps";
    ColorStretchDialog.AsyncCreator creator= null;
    JsColorStretchDialog newJsDialog;



    public ShowColorOpsCmd() {
        super(COMMAND_NAME);
    }

    @Override
    protected boolean init() {
        creator= new ColorStretchDialog.AsyncCreator(RootPanel.get());
        newJsDialog= JsColorStretchDialog.Builder.makeDialog();
        return true;
    }

    protected void doExecute() {
        if (isExperimental()) {
            newJsDialog.showDialog();
        }
        creator.show();
    }



    @Override
    public boolean hasIcon() { return false; }


    private static native boolean isExperimental() /*-{
        return ($wnd.firefly && $wnd.firefly.EXPERIMENTAL);
    }-*/;


}