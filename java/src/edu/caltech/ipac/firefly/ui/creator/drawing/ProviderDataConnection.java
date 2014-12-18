package edu.caltech.ipac.firefly.ui.creator.drawing;
/**
 * User: roby
 * Date: 3/23/12
 * Time: 1:34 PM
 */


import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.draw.AsyncDataLoader;
import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.LoadCallback;
import edu.caltech.ipac.firefly.visualize.draw.SimpleDataConnection;

import java.util.List;

/**
* @author Trey Roby
*/
abstract class ProviderDataConnection extends SimpleDataConnection {

    private DatasetDrawingLayerProvider datasetDrawingLayerProvider;

    public ProviderDataConnection(DatasetDrawingLayerProvider datasetDrawingLayerProvider,
                                  String title,
                                  String helpLine,
                                  String initDefColor) {
        super(title,helpLine,initDefColor);
        this.datasetDrawingLayerProvider = datasetDrawingLayerProvider;
    }
    public abstract void updateData(DataSet dataset);

    @Override
    public List<DrawObj> getData(boolean rebuild, WebPlot plot) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public AsyncDataLoader getAsyncDataLoader() {
        return new AsyncDataLoader() {
            public void requestLoad(final LoadCallback cb) {
                if (datasetDrawingLayerProvider.isEnabled()) {
                    cb.loaded();
                }
                else {
                    final WebEventManager em= datasetDrawingLayerProvider.getEventHub().getEventManager();
                    em.addListener(EventHub.ON_EVENT_WORKER_COMPLETE,
                                   new WebEventListener<DataSet>() {
                                       public void eventNotify(WebEvent<DataSet> ev) {
                                           if (ev.getSource().equals(datasetDrawingLayerProvider) && ev.getData()!=null) {
                                               cb.loaded();
                                               em.removeListener(EventHub.ON_EVENT_WORKER_COMPLETE,this);
                                           }
                                       }
                                   });
                    datasetDrawingLayerProvider.setEnabled(true);
                }
            }

            public void disableLoad() { datasetDrawingLayerProvider.setEnabled(false); }

            public boolean isDataAvailable() { return datasetDrawingLayerProvider.isEnabled() || getData(false,null)!=null; }

            public void markStale() { /* ignore */ }
        };
    }

    @Override
    public boolean isVeryLargeData() { return true; }

    @Override
    public boolean getOKForSubgroups() { return false; }
}

