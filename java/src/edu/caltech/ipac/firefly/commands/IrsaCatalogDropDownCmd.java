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

