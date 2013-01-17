package edu.caltech.ipac.firefly.server.dyn.xstream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import edu.caltech.ipac.firefly.data.dyn.xstream.FormTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.HtmlLoaderTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.LayoutAreaTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.LayoutContentTypeTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.PreviewTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.SplitPanelTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.TableTag;

import java.util.List;

public class LayoutAreaConverter implements Converter {

    public boolean canConvert(Class clazz) {
        return clazz.equals(LayoutAreaTag.class);
    }

    public void marshal(Object value, HierarchicalStreamWriter writer,
                        MarshallingContext context) {
        LayoutAreaTag layoutAreaTag = (LayoutAreaTag) value;

        List<LayoutContentTypeTag> lctList = layoutAreaTag.getLayoutContentTypes();
        for (LayoutContentTypeTag lct : lctList) {
            if (lct instanceof TableTag) {
                writer.startNode("Table");
                context.convertAnother((TableTag) lct);
                writer.endNode();

            } else if (lct instanceof PreviewTag) {
                writer.startNode("Preview");
                context.convertAnother((PreviewTag) lct);
                writer.endNode();
            }
        }

        List<FormTag> fList = layoutAreaTag.getForms();
        for (FormTag f : fList) {
            writer.startNode("Form");
            context.convertAnother((FormTag) f);
            writer.endNode();
        }

        List<SplitPanelTag> spList = layoutAreaTag.getSplitPanels();
        for (SplitPanelTag sp : spList) {
            writer.startNode("SplitPanel");
            context.convertAnother((SplitPanelTag) sp);
            writer.endNode();
        }

        List<HtmlLoaderTag> htmlLoaders = layoutAreaTag.getHtmlLoaders();
        for (HtmlLoaderTag hl : htmlLoaders) {
            writer.startNode("HtmlLoader");
            context.convertAnother(hl);
            writer.endNode();
        }
    }

    public Object unmarshal(HierarchicalStreamReader reader,
                            UnmarshallingContext context) {

        LayoutAreaTag layoutAreaTag = new LayoutAreaTag();

        layoutAreaTag.setLayout(reader.getAttribute("layout"));
        layoutAreaTag.setLayoutName(reader.getAttribute("layoutName"));
        layoutAreaTag.setTitle(reader.getAttribute("title"));
        layoutAreaTag.setIntialHeight(reader.getAttribute("initialHeight"));
        layoutAreaTag.setIntialWidth(reader.getAttribute("initialWidth"));

        String groupId = reader.getAttribute("groupId");
        if (groupId != null) {
            layoutAreaTag.setGroupId(groupId);
        }

        String helpId = reader.getAttribute("helpId");
        if (helpId != null) {
            layoutAreaTag.setHelpId(helpId);
        }

        String tagIt = reader.getAttribute("tagIt");
        if (tagIt != null) {
            layoutAreaTag.setTagIt(tagIt);
        }

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            String layoutAreaType = reader.getNodeName();
            if (layoutAreaType.equalsIgnoreCase("Table")) {
                TableTag t = (TableTag) context.convertAnother(layoutAreaTag, TableTag.class);
                layoutAreaTag.addLayoutContentType(t);

            } else if (layoutAreaType.equalsIgnoreCase("Preview")) {
                PreviewTag cp = (PreviewTag) context.convertAnother(layoutAreaTag, PreviewTag.class);
                layoutAreaTag.addLayoutContentType(cp);

            } else if (layoutAreaType.equalsIgnoreCase("Form")) {
                FormTag f = (FormTag) context.convertAnother(layoutAreaTag, FormTag.class);
                layoutAreaTag.addForm(f);

            } else if (layoutAreaType.equalsIgnoreCase("SplitPanel")) {
                SplitPanelTag sp = (SplitPanelTag) context.convertAnother(layoutAreaTag, SplitPanelTag.class);
                layoutAreaTag.addSplitPanel(sp);
            } else if (layoutAreaType.equalsIgnoreCase("HtmlLoader")) {
                HtmlLoaderTag tag = (HtmlLoaderTag) context.convertAnother(layoutAreaTag, HtmlLoaderTag.class);
                layoutAreaTag.addHtmlLoaders(tag);
            }

            reader.moveUp();
        }

        return layoutAreaTag;

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
