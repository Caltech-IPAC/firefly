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

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313)
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
 * HOWEVER USED.
 *
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 *
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
 * OF THE SOFTWARE.
 */
