/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.dyn.xstream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import edu.caltech.ipac.firefly.data.dyn.xstream.ParamTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.TableTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ViewTag;
import edu.caltech.ipac.firefly.server.dyn.DynServerData;
import edu.caltech.ipac.firefly.server.dyn.DynServerUtils;

public class ViewConverter implements Converter {

    public boolean canConvert(Class clazz) {
        return clazz.equals(ViewTag.class);
    }

    public void marshal(Object value, HierarchicalStreamWriter writer,
                        MarshallingContext context) {
    }

    public Object unmarshal(HierarchicalStreamReader reader,
                            UnmarshallingContext context) {

        ViewTag viewTag = new ViewTag();

        String attrVal = reader.getAttribute("id");
        if (attrVal != null) {
            viewTag.setId(attrVal);
        }

        attrVal = reader.getAttribute("type");
        if (attrVal != null) {
            viewTag.setType(attrVal);
        }

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            String childName = reader.getNodeName();
            if (childName.equalsIgnoreCase("QueryId")) {
                viewTag.setQueryId(reader.getValue());
            } else if (childName.equalsIgnoreCase("Name")) {
                viewTag.setName(reader.getValue());
            } else if (childName.equalsIgnoreCase("ShortDescription")) {
                viewTag.setShortDescription(reader.getValue());
            } else if (childName.equalsIgnoreCase("Index")) {
                viewTag.setIndex(reader.getValue());
            } else if (childName.equalsIgnoreCase("Param")) {
                ParamTag pt = (ParamTag) context.convertAnother(
                        viewTag, ParamTag.class);
                viewTag.addParam(pt);

            }

            reader.moveUp();
        }

        return viewTag;
    }

}

