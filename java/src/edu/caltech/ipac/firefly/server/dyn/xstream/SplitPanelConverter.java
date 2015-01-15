/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.dyn.xstream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import edu.caltech.ipac.firefly.data.dyn.xstream.LayoutAreaTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.SplitPanelTag;
import edu.caltech.ipac.firefly.server.dyn.DynServerData;
import edu.caltech.ipac.firefly.server.dyn.DynServerUtils;

public class SplitPanelConverter implements Converter {

    public boolean canConvert(Class clazz) {
        return clazz.equals(SplitPanelTag.class);
    }

    public void marshal(Object value, HierarchicalStreamWriter writer,
                        MarshallingContext context) {
    }

    public Object unmarshal(HierarchicalStreamReader reader,
                            UnmarshallingContext context) {

        DynServerData dataStore = (DynServerData) DynServerData.getInstance();

        SplitPanelTag splitPanelTag = new SplitPanelTag();
        String xidFlag = null;

        String attrVal = reader.getAttribute("xid");
        if (attrVal != null) {
            xidFlag = attrVal;
            splitPanelTag.setXid(attrVal);
        }

        attrVal = reader.getAttribute("ref-xid");
        if (attrVal != null) {
            splitPanelTag = (SplitPanelTag) DynServerUtils.copy((SplitPanelTag) dataStore.getProjectXid(attrVal));
            if (splitPanelTag == null) {
                splitPanelTag = new SplitPanelTag();
            }
        }

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            String childName = reader.getNodeName();
            if (childName.equalsIgnoreCase("North")) {
                LayoutAreaTag lat = (LayoutAreaTag) context.convertAnother(
                        splitPanelTag, LayoutAreaTag.class);
                lat.setType(LayoutAreaTag.LayoutDirection.NORTH);
                splitPanelTag.addLayoutArea(lat);

            } else if (childName.equalsIgnoreCase("South")) {
                LayoutAreaTag lat = (LayoutAreaTag) context.convertAnother(
                        splitPanelTag, LayoutAreaTag.class);
                lat.setType(LayoutAreaTag.LayoutDirection.SOUTH);
                splitPanelTag.addLayoutArea(lat);

            } else if (childName.equalsIgnoreCase("East")) {
                LayoutAreaTag lat = (LayoutAreaTag) context.convertAnother(
                        splitPanelTag, LayoutAreaTag.class);
                lat.setType(LayoutAreaTag.LayoutDirection.EAST);
                splitPanelTag.addLayoutArea(lat);

            } else if (childName.equalsIgnoreCase("West")) {
                LayoutAreaTag lat = (LayoutAreaTag) context.convertAnother(
                        splitPanelTag, LayoutAreaTag.class);
                lat.setType(LayoutAreaTag.LayoutDirection.WEST);
                splitPanelTag.addLayoutArea(lat);

            } else if (childName.equalsIgnoreCase("Center")) {
                LayoutAreaTag lat = (LayoutAreaTag) context.convertAnother(
                        splitPanelTag, LayoutAreaTag.class);
                lat.setType(LayoutAreaTag.LayoutDirection.CENTER);
                splitPanelTag.addLayoutArea(lat);
            }

            reader.moveUp();
        }

        if (xidFlag != null) {
            dataStore.addProjectXid(xidFlag, splitPanelTag);
        }

        return splitPanelTag;
    }

}

