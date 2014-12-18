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

