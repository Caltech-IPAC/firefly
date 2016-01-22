/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.dyn.xstream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import edu.caltech.ipac.firefly.data.dyn.xstream.*;

import java.util.List;

public class ProjectConverter implements Converter {

    public boolean canConvert(Class clazz) {
        return clazz.equals(ProjectTag.class);
    }

    public void marshal(Object value, HierarchicalStreamWriter writer,
                        MarshallingContext context) {
        ProjectTag projectTag = (ProjectTag) value;

        String name = projectTag.getName();
        if (name != null) {
            writer.startNode("Name");
            writer.setValue(name);
            writer.endNode();
        }

        String title = projectTag.getTitle();
        if (title != null) {
            writer.startNode("Title");
            writer.setValue(title);
            writer.endNode();
        }

        List<ParamTag> pList = projectTag.getProperties();
        writer.startNode("Properties");
        for (ParamTag p : pList) {
            writer.startNode("Param");
            context.convertAnother(p);
            writer.endNode();
        }
        writer.endNode();

        List<ParamTag> pList2 = projectTag.getOverrideProperties();
        writer.startNode("OverrideProperties");
        for (ParamTag p : pList2) {
            writer.startNode("Param");
            context.convertAnother(p);
            writer.endNode();
        }
        writer.endNode();

        List<ParamTag> params = projectTag.getParams();
        for (ParamTag p : params) {
            writer.startNode("Param");
            context.convertAnother(p);
            writer.endNode();
        }

        List<CatalogTag> cList = projectTag.getCatalogs();
        for (CatalogTag c : cList) {
            writer.startNode("Catalog");
            context.convertAnother(c);
            writer.endNode();
        }

        List<SearchTypeTag> stList = projectTag.getSearchTypes();
        for (SearchTypeTag st : stList) {
            writer.startNode("SearchGroup");
            context.convertAnother(st);
            writer.endNode();
        }
    }

    public Object unmarshal(HierarchicalStreamReader reader,
                            UnmarshallingContext context) {
        ProjectTag projectTag = new ProjectTag();

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            String projectElem = reader.getNodeName();
            if (projectElem.equalsIgnoreCase("Name")) {
                projectTag.setName(reader.getValue());

            } else if (projectElem.equalsIgnoreCase("Title")) {
                projectTag.setTitle(reader.getValue());

            } else if (projectElem.equalsIgnoreCase("Properties")) {
                while (reader.hasMoreChildren()) {
                    reader.moveDown();
                    ParamTag pt = (ParamTag) context.convertAnother(projectTag, ParamTag.class);
                    projectTag.addProperty(pt);

                    reader.moveUp();
                }

            } else if (projectElem.equalsIgnoreCase("OverrideProperties")) {
                while (reader.hasMoreChildren()) {
                    reader.moveDown();
                    ParamTag pt = (ParamTag) context.convertAnother(projectTag, ParamTag.class);
                    projectTag.addOverrideProperty(pt);

                    reader.moveUp();
                }

            } else if (projectElem.equalsIgnoreCase("Param")) {
                ParamTag p = (ParamTag) context.convertAnother(projectTag, ParamTag.class);
                projectTag.addParam(p);


            } else if (projectElem.equalsIgnoreCase("Catalog")) {
                CatalogTag c = (CatalogTag) context.convertAnother(projectTag, CatalogTag.class);
                projectTag.addCatalog(c);

            } else if (projectElem.equalsIgnoreCase("SearchGroup")) {
                SearchGroupTag st = (SearchGroupTag) context.convertAnother(projectTag, SearchGroupTag.class);
                projectTag.addSearchGroup(st);
            }

            reader.moveUp();
        }

        return projectTag;
    }

}

