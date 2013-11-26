package edu.caltech.ipac.hydra.server.xml.finderchart;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomReader;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Date: 9/25/13
 *
 * Utility class to generate finderchart's xml output.
 * It will also read in xml and convert it into finderchart's tag objects.
 * Look at http://irsa.ipac.caltech.edu/applications/FinderChart/docs/finderProgramInterface.html
 * for xml's format.
 *
 * @author loi
 * @version $Id: $
 */
public class FcXmlToJava {

    private XStream init() {
        XStream xstream = new XStream();
        xstream.alias("finderchart", FinderChartTag.class);
        xstream.alias("input", InputTag.class);
        xstream.alias("result", ResultTag.class);
        xstream.alias("image", ImageTag.class);
        xstream.alias("results", ErrorTag.class);
        xstream.alias("artifact", ArtifactTag.class);

        xstream.addImplicitCollection(ResultTag.class, "images");
        xstream.addImplicitCollection(ResultTag.class, "artifacts");
        xstream.useAttributeFor(FinderChartTag.class, "status");
        xstream.useAttributeFor(ErrorTag.class, "status");
        xstream.useAttributeFor(ArtifactTag.class, "surveyname");

        return xstream;
    }

    public static FinderChartTag getFinderChart(String filename) {
        try {
            XStream xstream = new FcXmlToJava().init();
            FinderChartTag fct = (FinderChartTag) xstream.unmarshal(getReader(filename));
            return fct;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ErrorTag getErrorTag(String filename) {
        try {
            XStream xstream = new FcXmlToJava().init();
            ErrorTag o = (ErrorTag) xstream.unmarshal(getReader(filename));
            return o;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void toXml(Object o, String filename) {
        try {
            toXml(o, new FileWriter(filename));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void toXml(Object o, Writer writer) {
        try {
            writer.write("<?xml version=\"1.0\"?>\n");
            XStream xstream = new FcXmlToJava().init();
            xstream.toXML(o, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static DomReader getReader(String filename) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document doc = builder.parse( new File(filename)  );
        return new DomReader(doc){
                    @Override
                    public String getValue() {
                        String v = super.getValue();
                        return v == null ? null : v.trim();
                    }
                };
    }

    public static void main(String[] args) {

        try {
            FinderChartTag fct = FcXmlToJava.getFinderChart(args[0]);
            ErrorTag et = FcXmlToJava.getErrorTag(args[1]);
            FcXmlToJava.toXml(fct, args[0] + ".out");
            FcXmlToJava.toXml(et, args[1] + ".out");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
