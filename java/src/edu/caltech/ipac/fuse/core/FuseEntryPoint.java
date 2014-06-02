package edu.caltech.ipac.fuse.core;

import com.google.gwt.core.client.EntryPoint;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.layout.SSEClient;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.task.IrsaAllDataSetsTask;
import edu.caltech.ipac.fuse.commands.GeneralSearchCmd;


/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class FuseEntryPoint implements EntryPoint {
    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
        Application.setCreator(new FuseCreator());
        Application.getInstance().runOnStartup(new IrsaAllDataSetsTask());
        Request home = new Request(GeneralSearchCmd.COMMAND_NAME, "General Search", true, false);
        Application.getInstance().start(home, new AppReady());
    }

    public class AppReady implements Application.ApplicationReady {
        public void ready() {
            Application.getInstance().hideDefaultLoadingDiv();
        }
    }

    //=======================================================
    //--------------- Experimental -----------------
    //=======================================================




    //=======================================================
    //--------------- End Experimental -----------------
    //=======================================================
}
