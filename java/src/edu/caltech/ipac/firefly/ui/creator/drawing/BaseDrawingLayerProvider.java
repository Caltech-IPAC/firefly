package edu.caltech.ipac.firefly.ui.creator.drawing;
/**
 * User: roby
 * Date: 2/21/12
 * Time: 9:40 AM
 */


import edu.caltech.ipac.firefly.ui.creator.eventworker.BaseEventWorker;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.visualize.draw.DataConnection;

/**
 * @author Trey Roby
 */
public abstract class BaseDrawingLayerProvider extends BaseEventWorker<DataConnection> implements DrawingLayerProvider {


    public BaseDrawingLayerProvider() {
        super("DrawingLayerProvider");
    }

    abstract protected void handleEvent(WebEvent ev);

}


