/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.commands;


import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.ui.JsExampleDialog;

public class ExampleJsDialogCmd extends GeneralCommand {
    public static final String COMMAND_NAME= "ExampleJsDialogCmd";
    JsExampleDialog exampleDialog;



    public ExampleJsDialogCmd() {
        super(COMMAND_NAME);
    }

//    @Override
//    protected boolean init() {
//        exampleDialog= JsExampleDialog.Builder.makeDialog();
//        return true;
//    }

    protected void doExecute() {
        showExampleDialog();
    }


    public static native JsExampleDialog showExampleDialog() /*-{
        if ($wnd.firefly && $wnd.firefly.gwt) {
            $wnd.firefly.gwt.showExampleDialog();
        }
    }-*/;


    @Override
    public boolean hasIcon() { return false; }
}