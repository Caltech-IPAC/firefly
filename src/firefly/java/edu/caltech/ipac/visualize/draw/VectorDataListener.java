/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.draw;

import java.util.EventListener;

/**
 * A listener that is called when vector data is changed
 * @author Trey Roby
 */
public interface VectorDataListener extends EventListener {
       /**
        * Called when ...
        * @param ev the event
        */
       public abstract void dataChanged(VectorDataEvent ev);
}



