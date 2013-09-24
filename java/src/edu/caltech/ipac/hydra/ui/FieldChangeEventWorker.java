package edu.caltech.ipac.hydra.ui;

import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.ui.FormHub;
import edu.caltech.ipac.firefly.ui.creator.eventworker.BaseFormEventWorker;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.util.StringUtils;

import java.util.List;


public class FieldChangeEventWorker extends BaseFormEventWorker {

    // other actions can be added to this class
    private String fieldNamePrefix = null;
    private boolean sync = false;

    public FieldChangeEventWorker(String fieldName, boolean sync) {
        if (fieldName.endsWith("*")) {
            fieldNamePrefix = fieldName.substring(0, fieldName.length()-1);
        }
        this.sync = sync;
    }

    public void bind(FormHub hub) {
        if (hub!=null) {
            addHub(hub);
            hub.getEventManager().addListener(FormHub.FIELD_VALUE_CHANGE, new WebEventListener() {
                public void eventNotify(WebEvent ev) {
                    Param param= (Param)ev.getData();
                    String name = param.getName();

                    if (!StringUtils.isEmpty(fieldNamePrefix)) {
                        if (name.startsWith(fieldNamePrefix)) {
                            if (sync) {
                                List<String> allFields = getFieldIds();
                                String value = param.getValue();

                                for (String fName : allFields) {
                                    if (fName.startsWith(fieldNamePrefix)) {
                                        setValue(new Param(fName, value));
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    public void bind(EventHub hub) {
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
