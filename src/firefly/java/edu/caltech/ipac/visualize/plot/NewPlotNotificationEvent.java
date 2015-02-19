/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import java.util.EventObject;

/**
 * The event that is passed when ...
 * @author Trey Roby
 */
public class NewPlotNotificationEvent extends EventObject {

    /**
     * Create a new NewPlotNotificationEvent
     * @param p source of the event.
     */
    public NewPlotNotificationEvent(Plot p) {
        super(p);
    }

    public Plot getPlot() {return (Plot)getSource();}
}



