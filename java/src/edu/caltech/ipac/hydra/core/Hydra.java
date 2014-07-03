package edu.caltech.ipac.hydra.core;

import com.google.gwt.core.client.EntryPoint;
import edu.caltech.ipac.firefly.commands.DynHomeCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.creator.RangePanelCreator;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.hydra.ui.CollapsiblePanelVisibilityEventWorkerCreator;
import edu.caltech.ipac.hydra.ui.FieldChangeEventWorkerCreator;
import edu.caltech.ipac.firefly.ui.creator.eventworker.FieldDefVisibilityEventWorkerCreator;
import edu.caltech.ipac.hydra.ui.TabPaneSizeEventWorkerCreator;
import edu.caltech.ipac.hydra.ui.TabPaneSizeWithCPanelEventWorkerCreator;
import edu.caltech.ipac.hydra.ui.lsst.LsstPlotTypeUICreator;
import edu.caltech.ipac.hydra.ui.wise.WiseFormEventWorkerCreator;
import edu.caltech.ipac.hydra.ui.wise.WisePlotTypeUICreator;


/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Hydra implements EntryPoint {
    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
        Application.setCreator(new HydraCreator());

        Application app = Application.getInstance();

        Request home = new Request(DynHomeCmd.COMMAND_NAME, "Welcome Page", true, false);
        app.start(home, new AppReady());

        // used to add custom widgets to results widget factory
        WidgetFactory fact = Application.getInstance().getWidgetFactory();
        fact.addCreator("WisePlotTypeUI", new WisePlotTypeUICreator());
        fact.addCreator("LsstPlotTypeUI", new LsstPlotTypeUICreator());
//        fact.addCreator("WisePreviewControl", new WisePreviewControlCreator());
        fact.addCreator("WiseSourceIdChangeControl", new WiseFormEventWorkerCreator());
        fact.addCreator("CollapsiblePanelVisibilityControl", new CollapsiblePanelVisibilityEventWorkerCreator());
        fact.addCreator("TabPaneSizeControl", new TabPaneSizeEventWorkerCreator());
        fact.addCreator("TabPaneSizeWithCPanelControl", new TabPaneSizeWithCPanelEventWorkerCreator());
        fact.addCreator("FieldChangeControl", new FieldChangeEventWorkerCreator());
        fact.addCreator("RangePanel", new RangePanelCreator());
    }


    public class AppReady implements Application.ApplicationReady {
        public void ready() {
            Application.getInstance().hideDefaultLoadingDiv();
        }
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
