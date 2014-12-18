package edu.caltech.ipac.firefly.server.dyn.xstream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import edu.caltech.ipac.firefly.data.dyn.xstream.LayoutTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.LayoutTypeTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.SplitPanelTag;

public class LayoutConverter implements Converter {

    public boolean canConvert(Class clazz) {
        return clazz.equals(LayoutTag.class);
    }

    public void marshal(Object value, HierarchicalStreamWriter writer,
                        MarshallingContext context) {
        LayoutTag layoutTag = (LayoutTag) value;

        LayoutTypeTag lt = layoutTag.getLayoutType();
        if (lt == null) return;
        if (lt instanceof SplitPanelTag) {
            writer.startNode("SplitPanel");
            context.convertAnother((SplitPanelTag) lt);
            writer.endNode();
        }

    }

    public Object unmarshal(HierarchicalStreamReader reader,
                            UnmarshallingContext context) {

        LayoutTag layoutTag = new LayoutTag();

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            String layoutType = reader.getNodeName();
            if (layoutType.equalsIgnoreCase("SplitPanel")) {
                SplitPanelTag d = (SplitPanelTag) context.convertAnother(layoutTag, SplitPanelTag.class);
                layoutTag.setLayoutType(d);
            }

            reader.moveUp();
        }

        return layoutTag;

    }

}

