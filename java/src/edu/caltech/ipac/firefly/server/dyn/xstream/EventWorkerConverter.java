package edu.caltech.ipac.firefly.server.dyn.xstream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import edu.caltech.ipac.firefly.data.dyn.xstream.EventWorkerTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ParamTag;
import edu.caltech.ipac.firefly.server.dyn.DynServerData;
import edu.caltech.ipac.firefly.server.dyn.DynServerUtils;

public class EventWorkerConverter implements Converter {

    public boolean canConvert(Class clazz) {
        return clazz.equals(EventWorkerTag.class);
    }

    public void marshal(Object value, HierarchicalStreamWriter writer,
                        MarshallingContext context) {
    }

    public Object unmarshal(HierarchicalStreamReader reader,
                            UnmarshallingContext context) {

        DynServerData dataStore = (DynServerData) DynServerData.getInstance();

        EventWorkerTag eventWorkerTag = new EventWorkerTag();
        String xidFlag = null;

        String attrVal = reader.getAttribute("xid");
        if (attrVal != null) {
            xidFlag = attrVal;
            eventWorkerTag.setXid(attrVal);
        }

        attrVal = reader.getAttribute("ref-xid");
        if (attrVal != null) {
            eventWorkerTag = (EventWorkerTag) DynServerUtils.copy((EventWorkerTag) dataStore.getProjectXid(attrVal));
            if (eventWorkerTag == null) {
                eventWorkerTag = new EventWorkerTag();
            }
        }

        attrVal = reader.getAttribute("id");
        if (attrVal != null) {
            eventWorkerTag.setId(attrVal);
        }

        attrVal = reader.getAttribute("delayTime");
        if (attrVal != null) {
            eventWorkerTag.setDelayTime(attrVal);
        }

        attrVal = reader.getAttribute("type");
        if (attrVal != null) {
            eventWorkerTag.setType(attrVal);
        }

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            String childName = reader.getNodeName();
            if (childName.equalsIgnoreCase("QueryId")) {
                eventWorkerTag.addQueryId(reader.getValue());

            } else if (childName.equalsIgnoreCase("ShortDescription")) {
                eventWorkerTag.setShortDescription(reader.getValue());

            } else if (childName.equalsIgnoreCase("Param")) {
                ParamTag pt = (ParamTag) context.convertAnother(
                        eventWorkerTag, ParamTag.class);
                eventWorkerTag.addParam(pt);

            }

            reader.moveUp();
        }

        if (xidFlag != null) {
            dataStore.addProjectXid(xidFlag, eventWorkerTag);
        }

        return eventWorkerTag;
    }

}

