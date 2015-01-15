/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Panel;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.resbundle.images.IconCreator;
import edu.caltech.ipac.firefly.ui.panels.user.PreferencePanel;

/**
 * @author tatianag
 * @version $Id: ShowPreferencesCmd.java,v 1.13 2011/09/16 01:16:34 loi Exp $
 */
public class ShowPreferencesCmd extends GeneralCommand {

    public static final String COMMAND_NAME = "ShowPreferences";

    PreferencePanel prefPanel;
    Panel holder;

    public ShowPreferencesCmd() {
        super(COMMAND_NAME);
    }

    @Override
    public boolean init() {
        prefPanel = new PreferencePanel();
        prefPanel.addStyleName("content-panel");
//        ScrollPanel scrollPanel = new ScrollPanel();
        prefPanel.setWidth("700px");
        prefPanel.setHeight("400px");
//        GwtUtil.setStyle(prefPanel, "margin", "20px");
        holder = new HorizontalPanel();
//        holder.setWidth("100%");
        holder.add(prefPanel);

//        scrollPanel.setWidget(prefPanel);
//        popup = new PopupPane("Preferences", prefPanel);
//        popup.setCloseable(true);
//        popup.addCloseHandler(new CloseHandler<PopupPane>() {
//            public void onClose(CloseEvent<PopupPane> closeEvent) {
//                // remove preference update listener on popup close
//                Preferences.getEventManager().removeListener(Name.PREFERENCE_UPDATE, eventListener);
//            }
//        });
//        popup.alignTo(Application.getInstance().getLayoutManager().getDisplay(), PopupPane.Align.TOP_LEFT, 70, 70);
        return true;
    }


    @Override
    public Image createCmdImage() {
        IconCreator ic= IconCreator.Creator.getInstance();
        String iStr= getIconProperty();
        if (iStr!=null && iStr.equals(COMMAND_NAME+".Icon"))  {
            return new Image(ic.getPreferences());
        }
        return null;
    }

    protected void doExecute() {        
        Application.getInstance().getLayoutManager().getRegion(LayoutManager.DROPDOWN_REGION).setDisplay(holder);
    }
}
