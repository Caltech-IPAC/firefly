package edu.caltech.ipac.firefly.ui.creator.eventworker;

import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.ui.FormHub;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.table.TablePreviewEventHub;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.ui.creator.eventworker.BaseFormEventWorker;

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

    public void bind(TablePreviewEventHub hub) {
       // should do something here
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
