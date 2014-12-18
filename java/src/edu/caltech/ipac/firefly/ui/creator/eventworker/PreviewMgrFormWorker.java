package edu.caltech.ipac.firefly.ui.creator.eventworker;

import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.ui.FormHub;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.previews.DataViewerPreview;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TablePreview;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;

import java.util.List;


public class PreviewMgrFormWorker extends BaseFormEventWorker {

    private EventHub tHub;

    public void bind(FormHub hub) {
        if (hub!=null) {
            addHub(hub);
            hub.getEventManager().addListener(FormHub.FIELD_VALUE_CHANGE, new WebEventListener() {
                public void eventNotify(WebEvent ev) {
                    Param param= (Param)ev.getData();
                    PopupUtil.showInfo("PreviewMgrFormEventWorker: "+ param.getName() + " changed to " + param.getValue());

                    String pNames = "";
                    List<TablePreview> pList = tHub.getPreviews();
                    for (TablePreview p : pList) {
                        if (p instanceof DataViewerPreview) {
                            DataViewerPreview pp = (DataViewerPreview) p;
                            pNames += pp.getName() + "; ";
                        }
                    }

                    //PopupUtil.showInfo("Data Viewer Previews: " + pNames);
                }
            });
        }
    }

    public void bind(EventHub hub) {
        tHub = hub;
    }
}

