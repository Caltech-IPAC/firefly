package edu.caltech.ipac.firefly.server.dyn.xstream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import edu.caltech.ipac.firefly.data.dyn.xstream.ConstraintsTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ParamTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.PreviewTag;
import edu.caltech.ipac.firefly.server.dyn.DynServerData;
import edu.caltech.ipac.firefly.server.dyn.DynServerUtils;

public class PreviewConverter implements Converter {

    public boolean canConvert(Class clazz) {
        return clazz.equals(PreviewTag.class);
    }

    public void marshal(Object value, HierarchicalStreamWriter writer,
                        MarshallingContext context) {
    }

    public Object unmarshal(HierarchicalStreamReader reader,
                            UnmarshallingContext context) {

        DynServerData dataStore = (DynServerData) DynServerData.getInstance();

        PreviewTag previewTag = new PreviewTag();
        String xidFlag = null;

        String attrVal = reader.getAttribute("xid");
        if (attrVal != null) {
            xidFlag = attrVal;
            previewTag.setXid(attrVal);
        }

        attrVal = reader.getAttribute("ref-xid");
        if (attrVal != null) {
            previewTag = (PreviewTag) DynServerUtils.copy((PreviewTag) dataStore.getProjectXid(attrVal));
            if (previewTag == null) {
                previewTag = new PreviewTag();
            }
        }

        attrVal = reader.getAttribute("id");
        if (attrVal != null) {
            previewTag.setId(attrVal);
        }

        attrVal = reader.getAttribute("align");
        if (attrVal != null) {
            previewTag.setAlign(attrVal);
        }

        attrVal = reader.getAttribute("frameType");
        if (attrVal != null) {
            previewTag.setFrameType(attrVal);
        }

        attrVal = reader.getAttribute("type");
        if (attrVal != null) {
            previewTag.setType(attrVal);
        }

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            String childName = reader.getNodeName();
            if (childName.equalsIgnoreCase("QueryId")) {
                previewTag.addQueryId(reader.getValue());

            } else if (childName.equalsIgnoreCase("EventWorkerId")) {
                previewTag.addEventWorkerId(reader.getValue());

            } else if (childName.equalsIgnoreCase("Title")) {
                previewTag.setTitle(reader.getValue());

            } else if (childName.equalsIgnoreCase("ShortDescription")) {
                previewTag.setShortDescription(reader.getValue());

            } else if (childName.equalsIgnoreCase("Width")) {
                previewTag.setWidth(reader.getValue());

            } else if (childName.equalsIgnoreCase("Height")) {
                previewTag.setHeight(reader.getValue());

            } else if (childName.equalsIgnoreCase("TopIndent")) {
                previewTag.setTopIndent(reader.getValue());

            } else if (childName.equalsIgnoreCase("BottomIndent")) {
                previewTag.setBottomIndent(reader.getValue());

            } else if (childName.equalsIgnoreCase("LeftIndent")) {
                previewTag.setLeftIndent(reader.getValue());

            } else if (childName.equalsIgnoreCase("RightIndent")) {
                previewTag.setRightIndent(reader.getValue());

            } else if (childName.equalsIgnoreCase("Param")) {
                ParamTag pt = (ParamTag) context.convertAnother(
                        previewTag, ParamTag.class);
                previewTag.addParam(pt);
            } else if (childName.equalsIgnoreCase("Constraints")) {
                ConstraintsTag ct = (ConstraintsTag) context.convertAnother(
                        previewTag, ConstraintsTag.class);
                previewTag.setConstraints(ct);

            }

            reader.moveUp();
        }

        if (xidFlag != null) {
            dataStore.addProjectXid(xidFlag, previewTag);
        }

        return previewTag;
    }

}

