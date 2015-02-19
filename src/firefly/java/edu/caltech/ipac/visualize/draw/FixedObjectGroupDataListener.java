/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.draw;

import java.util.EventListener;

/**
 * A listener that is called when FixedObject data is changed
 * @author Trey Roby
 */
public interface FixedObjectGroupDataListener extends EventListener {
       /**
        * Called when ...
        */
       public abstract void dataChanged(FixedObjectGroupDataEvent ev);
}



