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
