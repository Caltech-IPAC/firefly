package edu.caltech.ipac.firefly.server.dyn.xstream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import edu.caltech.ipac.firefly.data.dyn.xstream.CatalogTag;


public class CatalogConverter implements Converter {

    public boolean canConvert(Class clazz) {
        return clazz.equals(CatalogTag.class);
    }

    public void marshal(Object value, HierarchicalStreamWriter writer,
                        MarshallingContext context) {
        CatalogTag catalogTag = (CatalogTag) value;
    }

    public Object unmarshal(HierarchicalStreamReader reader,
                            UnmarshallingContext context) {
        CatalogTag catalogTag = new CatalogTag();

        String nameVal = reader.getAttribute("name");
        if (nameVal != null) {
            catalogTag.setName(nameVal);
        }

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            String catalogElem = reader.getNodeName();
            if (catalogElem.equalsIgnoreCase("Host")) {
                catalogTag.setHost(reader.getValue());

            } else if (catalogElem.equalsIgnoreCase("Credentials")) {
                catalogTag.setLogin(reader.getAttribute("login"));
                catalogTag.setPassword(reader.getAttribute("password"));

            } else if (catalogElem.equalsIgnoreCase("CatalogURL")) {
                catalogTag.setCatalogUrl(reader.getValue());

            } else if (catalogElem.equalsIgnoreCase("OriginalFilename")) {
                catalogTag.setOriginalFilename(reader.getValue());

            } else if (catalogElem.equalsIgnoreCase("MasterCatFilename")) {
                catalogTag.setMasterCatFilename(reader.getValue());

            } else if (catalogElem.equalsIgnoreCase("AddtlReqSearchParams")) {
                while (reader.hasMoreChildren()) {
                    reader.moveDown();

                    String searchParamElem = reader.getNodeName();
                    if (searchParamElem.equalsIgnoreCase("GatorParam")) {
                        catalogTag.addSearchParam(reader.getAttribute("requestName"), reader.getAttribute("masterCatName"));
                    }

                    reader.moveUp();
                }
            }

            reader.moveUp();
        }

        return catalogTag;
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
