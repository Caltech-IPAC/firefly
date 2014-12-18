package edu.caltech.ipac.firefly.server.dyn.xstream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import edu.caltech.ipac.firefly.data.dyn.xstream.FormEventWorkerTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ParamTag;
import edu.caltech.ipac.firefly.server.dyn.DynServerData;
import edu.caltech.ipac.firefly.server.dyn.DynServerUtils;

public class FormEventWorkerConverter implements Converter {

    public boolean canConvert(Class clazz) {
        return clazz.equals(FormEventWorkerTag.class);
    }

    public void marshal(Object value, HierarchicalStreamWriter writer,
                        MarshallingContext context) {
    }

    public Object unmarshal(HierarchicalStreamReader reader,
                            UnmarshallingContext context) {

        DynServerData dataStore = (DynServerData) DynServerData.getInstance();

        FormEventWorkerTag fEventWorkerTag = new FormEventWorkerTag();
        String xidFlag = null;

        String attrVal = reader.getAttribute("xid");
        if (attrVal != null) {
            xidFlag = attrVal;
            fEventWorkerTag.setXid(attrVal);
        }

        attrVal = reader.getAttribute("ref-xid");
        if (attrVal != null) {
            fEventWorkerTag = (FormEventWorkerTag) DynServerUtils.copy((FormEventWorkerTag) dataStore.getProjectXid(attrVal));
            if (fEventWorkerTag == null) {
                fEventWorkerTag = new FormEventWorkerTag();
            }
        }

        attrVal = reader.getAttribute("id");
        if (attrVal != null) {
            fEventWorkerTag.setId(attrVal);
        }

        attrVal = reader.getAttribute("type");
        if (attrVal != null) {
            fEventWorkerTag.setType(attrVal);
        }

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            String childName = reader.getNodeName();
            if (childName.equalsIgnoreCase("ShortDescription")) {
                fEventWorkerTag.setShortDescription(reader.getValue());

            } else if (childName.equalsIgnoreCase("Param")) {
                ParamTag pt = (ParamTag) context.convertAnother(
                        fEventWorkerTag, ParamTag.class);
                fEventWorkerTag.addParam(pt);

            } else if (childName.equalsIgnoreCase("FieldDefIds")) {
                while (reader.hasMoreChildren()) {
                    reader.moveDown();

                    ParamTag pt = (ParamTag) context.convertAnother(
                            fEventWorkerTag, ParamTag.class);
                    fEventWorkerTag.addFieldDefId(pt);

                    reader.moveUp();
                }
            }

            reader.moveUp();
        }

        if (xidFlag != null) {
            dataStore.addProjectXid(xidFlag, fEventWorkerTag);
        }

        return fEventWorkerTag;
    }

}

