package edu.caltech.ipac.firefly.server.dyn.xstream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import edu.caltech.ipac.firefly.data.dyn.xstream.HtmlLoaderTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.LabelTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ParamTag;

public class HtmlLoaderConverter implements Converter {

    public boolean canConvert(Class clazz) {
        return clazz.equals(HtmlLoaderTag.class);
    }

    public void marshal(Object value, HierarchicalStreamWriter writer,
                        MarshallingContext context) {
    }

    public Object unmarshal(HierarchicalStreamReader reader,
                            UnmarshallingContext context) {

        HtmlLoaderTag loaderTag = new HtmlLoaderTag();

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            String childName = reader.getNodeName();
            if (childName.equalsIgnoreCase("QueryId")) {
                loaderTag.setQueryId(reader.getValue());
            } else if (childName.equalsIgnoreCase("Param")) {
                ParamTag pt = (ParamTag) context.convertAnother(
                        loaderTag, ParamTag.class);
                loaderTag.addParam(pt);
            } else if (childName.equalsIgnoreCase("Label")) {
                LabelTag lt = (LabelTag) context.convertAnother(
                        loaderTag, LabelTag.class);
                loaderTag.setLabel(lt);
            }

            reader.moveUp();
        }
        return loaderTag;
    }

}

