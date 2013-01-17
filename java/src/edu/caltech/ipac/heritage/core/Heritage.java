package edu.caltech.ipac.heritage.core;

import com.google.gwt.core.client.EntryPoint;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.MenuGenerator;
import edu.caltech.ipac.firefly.data.MenuItemAttrib;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.panels.SearchPanel;
import edu.caltech.ipac.heritage.commands.SearchByPositionCmd;


/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Heritage implements EntryPoint {
    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {

        Application.setCreator(new HeritageCreator());
        Request home = new Request(SearchByPositionCmd.COMMAND_NAME, "Position Search", true, false);
        Application.getInstance().start(home, new AppReady());

    }



    public class AppReady implements Application.ApplicationReady {
        public void ready() {
            Application.getInstance().hideDefaultLoadingDiv();
            MenuItemAttrib mia= MenuGenerator.getDefaultInstance().getMenuItemAttrib("allsearches");
            SearchPanel sp = SearchPanel.getInstance();
            sp.setFormAreaMinWidth("720px");
            sp.setApplicationContext(null, mia);
        }
    }

}
