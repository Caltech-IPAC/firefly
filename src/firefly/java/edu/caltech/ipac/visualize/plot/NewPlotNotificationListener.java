/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import java.util.EventListener;

/**
 * A listener that is called when ...
 * @author Trey Roby
 */
public interface NewPlotNotificationListener extends EventListener {
       /**
        * Called when ...
        * @param ev the event
        */
       public abstract void newPlot(NewPlotNotificationEvent ev);
       // ???  other status methods ????
}



