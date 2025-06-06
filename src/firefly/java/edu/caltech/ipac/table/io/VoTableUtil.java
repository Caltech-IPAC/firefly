/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.table.io;

import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.GroupInfo;
import edu.caltech.ipac.table.LinkInfo;
import edu.caltech.ipac.table.ParamInfo;
import edu.caltech.ipac.table.ResourceInfo;
import edu.caltech.ipac.table.TableMeta;
import uk.ac.starlink.votable.VOTableWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.util.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.firefly.core.Util.Opt.ifNotEmpty;
import static edu.caltech.ipac.firefly.core.Util.Opt.ifNotNull;
import static edu.caltech.ipac.table.DataType.*;
import static edu.caltech.ipac.table.TableMeta.*;
import static edu.caltech.ipac.table.TableMeta.REF;
import static edu.caltech.ipac.util.StringUtils.isEmpty;

/**
 * Date: 6/2/25
 *
 * @author loi
 * @version : $
 */
public class VoTableUtil extends VOTableWriter {

    public static Element xmlResource(ResourceInfo ri) {
        Element el = new Element("RESOURCE")
                .attr(UTYPE, ri.getUtype())
                .attr(ID, ri.getID())
                .attr(NAME, ri.getName())
                .attr(TYPE, ri.getType())
                .add(xmlDesc(ri.getDesc()));

        ri.getInfos().forEach((k, v) -> el.add(xmlInfo(k, v)));
        ri.getGroups().forEach(g -> el.add(xmlGroup(g)));
        ri.getParams().forEach(p -> el.add(xmlParam(p)));
        return el;
    }

    public static Element xmlTable(DataGroup table)  {
        TableMeta meta = table.getTableMeta();
        Element el = new Element("TABLE")
                .attr(ID, meta.getAttribute(ID))
                .attr(NAME, meta.getAttribute(NAME))
                .attr(UTYPE, meta.getAttribute(UTYPE))
                .attr(UCD, meta.getAttribute(UCD))
                .add(xmlDesc(meta.getAttribute(DESC)));

        if (table.size() > 0) {
            el.attr("nrows", String.valueOf(table.size()));
        }
        return el;
    }

    public static Element xmlField(DataType dt) {
        return xmlColumn(dt, "FIELD");
    }

    public static Element xmlParam(ParamInfo pInfo) {
        Element el = xmlColumn(pInfo, "PARAM");
        el.attr("value", pInfo.getStringValue());
        return el;
    }

    static Element xmlColumn(DataType dt, String tagName) {
        String prec = ifNotNull(dt.getPrecision()).getOrElse("");
        prec = prec.startsWith("G") ? prec.substring(1) : prec; // remove 'G' prefix if exists
        String width = dt.getWidth() == 0 ? "" : "" + dt.getWidth();
        String arysize = ifNotEmpty(dt.getArraySize()).getOrElse(dt.getDataType() == String.class ? "*" : "");

        Element el = new Element(tagName)
                .attr(ID, dt.getID())
                .attr(NAME, dt.getKeyName())
                .attr(UCD, dt.getUCD())
                .attr("datatype", mapToVoType(dt.getTypeDesc()))
                .attr("width", width)
                .attr("precision", prec)
                .attr("unit", dt.getUnits())
                .attr(UTYPE, dt.getUType())
                .attr(REF, dt.getRef())
                .attr("arraysize", arysize)
                .add(xmlDesc(dt.getDesc()));
        dt.getLinkInfos().forEach(l -> el.add(xmlLink(l)));
        return el;
    }

    public static Element xmlGroup(GroupInfo gInfo) {
        Element el = new Element("GROUP")
                .attr(ID, gInfo.getID())
                .attr(NAME, gInfo.getName())
                .attr(UTYPE, gInfo.getUtype())
                .attr(UCD, gInfo.getUCD())
                .add(xmlDesc(gInfo.getDescription()));
        gInfo.getGroupInfos().forEach(g -> el.add(xmlGroup(g)));
        gInfo.getParamInfos().forEach(p -> el.add(xmlParam(p)));
        gInfo.getParamRefs().forEach(r -> el.add(xmlParamRef(r)));
        gInfo.getColumnRefs().forEach(r -> el.add(xmlFieldRef(r)));
        return el;
    }

    public static Element xmlLink(LinkInfo linkInfo)   {
        Element el = new Element("LINK")
                .attr(ID, linkInfo.getID())
                .attr("content-role", linkInfo.getRole())
                .attr("content-type", linkInfo.getType())
                .attr("title", linkInfo.getTitle())
                .attr("value", linkInfo.getValue())
                .attr("href", linkInfo.getHref())
                .attr("action", linkInfo.getAction());
        return el;
    }

    public static Element xmlParamRef(GroupInfo.RefInfo ref) {
        return xmlRefInfo(ref, "PARAMref");
    }

    public static Element xmlFieldRef(GroupInfo.RefInfo ref) {
        return xmlRefInfo(ref, "FIELDref");
    }

    public static Element xmlRefInfo(GroupInfo.RefInfo ref, String tagName) {
        return new Element(tagName)
                .attr(REF, ref.getRef())
                .attr(TableMeta.UCD, ref.getUcd())
                .attr(TableMeta.UTYPE, ref.getUtype());
    }

    public static Element xmlInfo(String name, String value) {
        return new Element("INFO")
                .attr(TableMeta.NAME, name)
                .attr("value", value);
    }

    public static Element xmlDesc(String desc) {
        if (isEmpty(desc)) return null;
        return new Element("DESCRIPTION").text(desc);
    }

    /**
     * @param typeDesc  String description of column's data type
     * @return VoTable type for the given Firefly's Type
     */
    private static String mapToVoType(String typeDesc) {
        if (typeDesc.equals(DATE))      return "char";
        if (typeDesc.equals(LOCATION))  return "char";
        if (typeDesc.equals(REAL))      return "double";

        return typeDesc;
    }

    //====================================================================
    //
    //====================================================================

    /**
     * This class provides helper methods to simplify the creation of XML elements
     * by using the {@link javax.xml.stream.XMLStreamWriter} API. All output
     * generated is compliant with XML standards, including correct escaping of special
     * characters and proper element nesting.
     */
    public static class Element {
        private final String name;
        private final Map<String, String> attrs = new LinkedHashMap<>();
        private final List<Object> children = new ArrayList<>(); // Element or String
        private static final XMLOutputFactory factory = XMLOutputFactory.newInstance();

        public Element(String name) {
            this.name = name;
        }

        public Element attr(String k, String v) {
            if (!isEmpty(k) && !isEmpty(v)) {
                attrs.put(k, v);
            }
            return this;
        }

        public Element add(Element child) {
            if (child != null) {
                children.add(child);
            }
            return this;
        }

        public Element text(String text) {
            if(!isEmpty(text)) {
                children.add(text);
            }
            return this;
        }

        public String toXml() {
            return toXml(false);
        }

        public String toXmlNoFooter() {
            return toXml(true);
        }

        // Serialize to XML string using StAX
        public String toXml(boolean noFooter) {
            try {
                StringWriter sw = new StringWriter();
                XMLStreamWriter writer = factory.createXMLStreamWriter(sw);

                writeElement(writer,noFooter, 0);
                writer.flush();
                writer.close();
                return sw.toString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // Recursive method to write self (and children) to the XMLStreamWriter
        private void writeElement(XMLStreamWriter writer, boolean noFooter, int depth) throws Exception {

            doIndent(writer, depth);
            writer.writeStartElement(name);

            for (Map.Entry<String, String> entry : attrs.entrySet()) {
                writer.writeAttribute(entry.getKey(), entry.getValue());
            }

            boolean hasChildEl = false;
            for (Object child : children) {
                if (child instanceof Element) {
                    hasChildEl = true;
                    ((Element) child).writeElement(writer, false, depth + 1);
                } else if (child instanceof String) {
                    writer.writeCharacters((String) child);
                }
            }

            if (noFooter) {
                writer.writeCharacters("");     // close previous element if needed
            } else {
                if (hasChildEl) doIndent(writer, depth);
                writer.writeEndElement();
            }
        }

    }

    private static void doIndent(XMLStreamWriter writer, int depth) throws Exception {
        writer.writeCharacters("\n");
        writer.writeCharacters("  ".repeat(depth));
    }
}
