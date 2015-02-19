/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.util.ArrayList;
import java.util.List;

// custom converter used (ResultConverter) - no annotations needed within class
@XStreamAlias("Result")
public class ResultTag extends XidBaseTag {

    // xml element 'EventWorker*'
    protected List<EventWorkerTag> eventWorkerTags;

    // xml element 'Layout'
    protected LayoutTag layoutTag;


    public List<EventWorkerTag> getEventWorkers() {
        if (eventWorkerTags == null) {
            eventWorkerTags = new ArrayList<EventWorkerTag>();
        }
        return eventWorkerTags;
    }

    public void setEventWorkers(List<EventWorkerTag> values) {
        eventWorkerTags = values;
    }

    public void addEventWorker(EventWorkerTag value) {
        if (eventWorkerTags == null) {
            eventWorkerTags = new ArrayList<EventWorkerTag>();
        }

        eventWorkerTags.add(value);
    }


    public LayoutTag getLayout() {
        return layoutTag;
    }

    public void setLayout(LayoutTag value) {
        layoutTag = value;
    }

}

