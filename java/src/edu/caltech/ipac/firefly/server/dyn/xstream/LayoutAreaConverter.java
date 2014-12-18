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

        String id = reader.getAttribute("id");
        if (id != null) {
            layoutAreaTag.setId(id);
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

