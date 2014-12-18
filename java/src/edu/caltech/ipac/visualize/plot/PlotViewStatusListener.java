package edu.caltech.ipac.visualize.plot;

import java.util.EventListener;

/**
 * A listener that is called when a PlotView status has a changed
 * @author Trey Roby
 */
public interface PlotViewStatusListener extends EventListener {

       /**
        * Called when a plot is added to this plot view
        * @param ev the event
        */
       public abstract void plotAdded(PlotViewStatusEvent ev);
       /**
        * Called when a plot is added to this plot view
        * @param ev the event
        */
       public abstract void plotRemoved(PlotViewStatusEvent ev);
}



