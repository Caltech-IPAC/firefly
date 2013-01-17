package edu.caltech.ipac.vamp.core;

import com.google.gwt.core.client.EntryPoint;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.vamp.commands.AvmSearchCmd;
import edu.caltech.ipac.vamp.ui.creator.ImageGridPanelCreator;
import edu.caltech.ipac.vamp.ui.creator.SimpleMetaResourceCreator;
import edu.caltech.ipac.vamp.ui.creator.SimpleSpectralCreator;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Vamp implements EntryPoint {
    public static final String IMAGE_TABLE = "IMAGE_TABLE";
    public static final String META_RESOURCE_VIEW = "META_RESOURCE_VIEW";
    public static final String SPECTRAL_VIEW = "SPECTRAL_VIEW";
    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
        Application.setCreator(new VampCreator());

        Application app = Application.getInstance();

        //Register platform-specific UI Creators. 
        app.getWidgetFactory().addCreator(IMAGE_TABLE, new ImageGridPanelCreator());
        app.getWidgetFactory().addCreator(META_RESOURCE_VIEW, new SimpleMetaResourceCreator());
        app.getWidgetFactory().addCreator(SPECTRAL_VIEW, new SimpleSpectralCreator());

        Request home = new Request(AvmSearchCmd.COMMAND_NAME, "Welcome Page", true, false);
        app.start(home, new AppReady());
    }


    public class AppReady implements Application.ApplicationReady {
        public void ready() {
            Application.getInstance().hideDefaultLoadingDiv();
        }
    }

}
