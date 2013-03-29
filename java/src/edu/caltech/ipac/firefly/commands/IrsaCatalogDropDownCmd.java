package edu.caltech.ipac.firefly.commands;

import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.core.layout.Region;
import edu.caltech.ipac.firefly.data.dyn.DynUtils;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.catalog.CatalogSearchDropDown;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

import java.util.HashMap;
import java.util.Map;


public class IrsaCatalogDropDownCmd extends GeneralCommand {
    public static final String COMMAND_NAME = "IrsaCatalogDropDown";
    private Map<String, CatalogSearchDropDown> _panelMap = new HashMap<String, CatalogSearchDropDown>();
    private boolean _show = false;


    public IrsaCatalogDropDownCmd() {
        super(COMMAND_NAME);
        WebEventManager evm = WebEventManager.getAppEvManager();
        setupListener();
    }

    protected void doExecute() {
        CatalogSearchDropDown currentPanel;
        String projectId = DynUtils.getProjectIdFromUrl();
        if (_panelMap.containsKey(projectId)) {
            currentPanel = _panelMap.get(projectId);

        } else {
            currentPanel = new CatalogSearchDropDown(projectId) {
                @Override
                protected void hideOnSearch() { catalogDropSearching(); }
            };
            _panelMap.put(projectId, currentPanel);
        }

        if (_show) {
            currentPanel.show();
        } else {
            PopupUtil.showInfo("Do a search first then you can add a catalog to your search results");
        }
    }


    private void setupListener() {
        WebEventManager evm = WebEventManager.getAppEvManager();
        evm.addListener(Name.REGION_ADDED, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                if (((Region) ev.getSource()).getId().equals(LayoutManager.RESULT_REGION)) {
                    _show = true;
                }
            }
        });
        evm.addListener(Name.REGION_REMOVED, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                if (((Region) ev.getSource()).getId().equals(LayoutManager.RESULT_REGION)) {
                    _show = false;
                }
            }
        });
    }

    protected void catalogDropSearching() {

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
