package edu.caltech.ipac.firefly.ui.creator.eventworker;

import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.ui.FormHub;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;

import java.util.List;
/**
 * User: roby
 * Date: Aug 13, 2010
 * Time: 3:00:22 PM
 */


/**
 * @author Trey Roby
 */
public class ExampleFormEvWorker extends BaseFormEventWorker {

    private boolean visible= false;
    private int cnt= 0;


    public void bind(FormHub hub) {
        if (hub!=null) {
            addHub(hub);
            hub.getEventManager().addListener(FormHub.FIELD_VALUE_CHANGE, new WebEventListener() {
                public void eventNotify(WebEvent ev) {
                    Param param= (Param)ev.getData();
                    PopupUtil.showInfo("ExampleFormEventWorker: "+ param.getName() + " changed to " + param.getValue());

                    List<String> fname= getFieldIds();

                    setVisible(fname.get(cnt % fname.size()),visible);



                    if (visible) cnt++;
                    visible= !visible;
                }
            });
        }
    }

    public void bind(EventHub hub) {
       // should do something here
    }
}

