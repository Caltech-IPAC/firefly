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

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313)
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
 * HOWEVER USED.
 *
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 *
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
 * OF THE SOFTWARE.
 */
