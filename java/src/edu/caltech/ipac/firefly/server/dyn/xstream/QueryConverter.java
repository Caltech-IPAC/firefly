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
import edu.caltech.ipac.firefly.data.dyn.xstream.PreviewTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.QueryTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ConstraintsTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.DownloadTag;
import edu.caltech.ipac.firefly.server.dyn.DynServerData;
import edu.caltech.ipac.firefly.server.dyn.DynServerUtils;

public class QueryConverter implements Converter {

    public boolean canConvert(Class clazz) {
        return clazz.equals(QueryTag.class);
    }

    public void marshal(Object value, HierarchicalStreamWriter writer,
                        MarshallingContext context) {
    }

    public Object unmarshal(HierarchicalStreamReader reader,
                            UnmarshallingContext context) {

        DynServerData dataStore = (DynServerData) DynServerData.getInstance();

        QueryTag queryTag = new QueryTag();
        String xidFlag = null;

        String attrVal = reader.getAttribute("xid");
        if (attrVal != null) {
            xidFlag = attrVal;
            queryTag.setXid(attrVal);
        }

        attrVal = reader.getAttribute("ref-xid");
        if (attrVal != null) {
            queryTag = (QueryTag) DynServerUtils.copy((QueryTag) dataStore.getProjectXid(attrVal));
            if (queryTag == null) {
                queryTag = new QueryTag();
            }
        }

        attrVal = reader.getAttribute("id");
        if (attrVal != null) {
            queryTag.setId(attrVal);
        }

        attrVal = reader.getAttribute("searchProcessorId");
        if (attrVal != null) {
            queryTag.setSearchProcessorId(attrVal);
        }

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            String childName = reader.getNodeName();
            if (childName.equalsIgnoreCase("Constraints")) {

                ConstraintsTag ct = (ConstraintsTag) context.convertAnother(
                        queryTag, ConstraintsTag.class);
                queryTag.setConstraints(ct);

            } else if (childName.equalsIgnoreCase("Param")) {
                ParamTag pt = (ParamTag) context.convertAnother(
                        queryTag, ParamTag.class);
                queryTag.addParam(pt);

            } else if (childName.equalsIgnoreCase("Metadata")) {
                while (reader.hasMoreChildren()) {
                    reader.moveDown();

                    String childName2 = reader.getNodeName();
                    if (childName2.equalsIgnoreCase("Param")) {
                        ParamTag pt = (ParamTag) context.convertAnother(
                                queryTag, ParamTag.class);
                        queryTag.addMetadata(pt);
                    }

                    reader.moveUp();
                }

            } else if (childName.equalsIgnoreCase("Download")) {
                DownloadTag dt = (DownloadTag) context.convertAnother(
                        queryTag, DownloadTag.class);
                queryTag.setDownload(dt);

            }

            reader.moveUp();
        }

        if (xidFlag != null) {
            dataStore.addProjectXid(xidFlag, queryTag);
        }

        return queryTag;
    }
}

