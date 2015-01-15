/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.data.dyn.DynUtils;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.ui.catalog.CatalogSearchDialog;

import java.util.HashMap;
import java.util.Map;


public class IrsaCatalogCmd extends GeneralCommand {
    public static final String CommandName = "IrsaCatalog";
    private Map<String, CatalogSearchDialog> _dialogMap = new HashMap<String, CatalogSearchDialog>();


    public IrsaCatalogCmd() {
        super(CommandName);
    }

    protected void doExecute() {
        CatalogSearchDialog currentDialog;
        String projectId = DynUtils.getProjectIdFromUrl();
        if (_dialogMap.containsKey(projectId)) {
            currentDialog = _dialogMap.get(projectId);

        } else {
            LayoutManager lm= Application.getInstance().getLayoutManager();
            Widget p= (lm==null) ? RootPanel.get() : lm.getDisplay();
            currentDialog = new CatalogSearchDialog(p, projectId);
            _dialogMap.put(projectId, currentDialog);
        }

        currentDialog.setVisible(true);
    }



    @Override
    public Image createCmdImage() {
        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        return new Image(ic.getCatalog());
    }

//    @Override
//    protected Image createCmdImage() {
//        IconCreator ic= IconCreator.Creator.getInstance();
//        String iStr= this.getIconProperty();
//        if (iStr!=null && iStr.equals(COMMAND_NAME+".Icon"))  {
//            return ic.getCatalog().createImage();
//        }
//        return null;
//    }

}

