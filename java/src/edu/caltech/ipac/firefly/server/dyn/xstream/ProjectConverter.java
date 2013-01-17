package edu.caltech.ipac.firefly.server.dyn.xstream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import edu.caltech.ipac.firefly.data.dyn.xstream.CatalogTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ParamTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ProjectTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.SearchGroupTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.SearchTypeTag;

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
