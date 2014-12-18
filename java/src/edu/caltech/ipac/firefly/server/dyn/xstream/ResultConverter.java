package edu.caltech.ipac.firefly.server.dyn.xstream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import edu.caltech.ipac.firefly.data.dyn.xstream.EventWorkerTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.LayoutTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ResultTag;
import edu.caltech.ipac.firefly.server.dyn.DynServerData;
import edu.caltech.ipac.firefly.server.dyn.DynServerUtils;

public class ResultConverter implements Converter {

    public boolean canConvert(Class clazz) {
        return clazz.equals(ResultTag.class);
    }

    public void marshal(Object value, HierarchicalStreamWriter writer,
                        MarshallingContext context) {
    }

    public Object unmarshal(HierarchicalStreamReader reader,
                            UnmarshallingContext context) {

        DynServerData dataStore = (DynServerData) DynServerData.getInstance();

        ResultTag resultTag = new ResultTag();
        String xidFlag = null;

        String attrVal = reader.getAttribute("xid");
        if (attrVal != null) {
            xidFlag = attrVal;
            resultTag.setXid(attrVal);
        }

        attrVal = reader.getAttribute("ref-xid");
        if (attrVal != null) {
            resultTag = (ResultTag) DynServerUtils.copy((ResultTag) dataStore.getProjectXid(attrVal));
            if (resultTag == null) {
                resultTag = new ResultTag();
            }
        }

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            String childName = reader.getNodeName();
            if (childName.equalsIgnoreCase("EventWorker")) {
                EventWorkerTag ewt = (EventWorkerTag) context.convertAnother(
                        resultTag, EventWorkerTag.class);
                resultTag.addEventWorker(ewt);

            } else if (childName.equalsIgnoreCase("Layout")) {
                LayoutTag lt = (LayoutTag) context.convertAnother(
                        resultTag, LayoutTag.class);
                resultTag.setLayout(lt);
            }

            reader.moveUp();
        }

        if (xidFlag != null) {
            dataStore.addProjectXid(xidFlag, resultTag);
        }

        return resultTag;
    }
}

