package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Stack;

/**
 * @author Trey Roby
 */
public class VosiCapabilityRetrieve {
    private static final Logger.LoggerImpl _log = Logger.getLogger();

    public static Capabilities getCapabilities(URL url) {

        try {
            File f = File.createTempFile("capabilities", ".xml", ServerContext.getTempWorkDir());
            try (InputStream inStream = new BufferedInputStream(new FileInputStream(f))) {
                URLDownload.getDataToFile(url,f);
                SAXParserFactory factory= SAXParserFactory.newInstance();
                CapabilityHandle  h= new CapabilityHandle();
                SAXParser parser= factory.newSAXParser();
                parser.parse(inStream,h);
                TapCapability tc=  h.getTapCapability();
                return new Capabilities(tc);
            } catch (FailedRequestException | IOException | SAXException | ParserConfigurationException e) {
                _log.warn(e, "Could not parse capabilities");
            }
        } catch (IOException e) {
            _log.warn(e, "Could not parse capabilities");
        }
        return null;
    }




    static class CapabilityHandle extends DefaultHandler {


        private static final String[] adqlGeo =  {
                "ivo://ivoa.net/std/TAPRegExt#features-adqlgeo",
                "ivo://ivoa.net/std/TAPRegExt#features-adql-geo",
                "ivo://ivoa.net/std/TAPRegExt#features-adql-geo"
        };

        private static final String[] uploadHttp=  { "ivo://ivoa.net/std/TAPRegExt#upload-http" };
        private static final String[] standardIdTap=  { "ivo://ivoa.net/std/TAP" };

        private final Stack<String> currElement= new Stack<>();
        private final StringBuilder currValue= new StringBuilder();
        private boolean canUpload= false;
        private boolean canUsePoint= false;
        private boolean canUseCircle= false;
        private boolean canUseBox= false;
        private boolean canUsePolygon= false;
        private boolean canUseRegion= false;
        private boolean canUseContains= false;
        private boolean canUseIntersects= false;
        private boolean canUseArea= false;
        private boolean canUseCentroid= false;
        private boolean canUseCoord1= false;
        private boolean canUseCoord2= false;
        private boolean canUseCoordSys= false;
        private boolean canUseDistance= false;
        boolean foundTAP= false;
        boolean foundFeatures= false;
        boolean geoFeaturesProcess = false;

        CapabilityHandle() { }

        public TapCapability getTapCapability() {
            if (geoFeaturesProcess) {
                return new TapCapability( canUpload, true,
                        canUsePoint, canUseCircle, canUseBox,
                        canUsePolygon, canUseRegion, canUseContains,
                        canUseIntersects, canUseArea, canUseCentroid,
                        canUseCoord1, canUseCoord2, canUseCoordSys,
                        canUseDistance);
            }
            else {
                return makeDefaultTapCapability(canUpload);
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes att) {
            if (qName.equals("capability") && confirmAttribute(att, "standardID", standardIdTap)) {
               foundTAP= true;
            }
            if (foundTAP) {
                if (qName.equals("languageFeatures") && confirmAttribute(att,"type", adqlGeo)) {
                    foundFeatures= true;
                }
                if (qName.equals("uploadMethod") && confirmAttribute(att,"ivo-id", uploadHttp)) {
                    canUpload= true;
                }
            }
            currValue.setLength(0);
            currElement.push(qName);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            if (foundTAP && qName.equals("capability")) foundTAP= false;
            if (foundFeatures && qName.equals("languageFeatures")) foundFeatures= false;




            String value= currValue.toString();
            if (foundTAP && foundFeatures && qName.equals("form")) {
                switch (value) {
                    case "POINT" -> canUsePoint = true;
                    case "CIRCLE" -> canUseCircle = true;
                    case "POLYGON" -> canUsePolygon = true;
                    case "BOX" -> canUseBox = true;
                    case "CONTAINS" -> canUseContains = true;
                    case "REGION" -> canUseRegion = true;
                    case "INTERSECTS" -> canUseIntersects = true;
                    case "AREA" -> canUseArea = true;
                    case "CENTROID" -> canUseCentroid = true;
                    case "COORD1" -> canUseCoord1 = true;
                    case "COORD2" -> canUseCoord2 = true;
                    case "COORDSYS" -> canUseCoordSys = true;
                    case "DISTANCE" -> canUseDistance = true;
                }
                geoFeaturesProcess = true;
            }
            if (!currElement.empty()) currElement.pop();
        }

        public void characters(char[] ch, int start, int length) {
            currValue.append(ch, start, length);
        }
    }

    private static boolean confirmAttribute(Attributes att, String attName, String[] attValue) {
        int len= att.getLength();
        if (len==0) return false;
        for(int i=0; (i<len); i++) {
            if (attName.equals(att.getLocalName(i))) {
                String v= att.getValue(i);
                if (Arrays.asList(attValue).contains(v)) return true;
            }
        }
        return false;
    }


    public record Capabilities( TapCapability tapCapability) {}

    public record TapCapability( boolean canUpload, boolean foundGeoLanguageFeatures,
                                 boolean canUsePoint, boolean canUseCircle, boolean canUseBox,
                                 boolean canUsePolygon, boolean canUseRegion, boolean canUseContains,
                                 boolean canUseIntersects, boolean canUseArea, boolean canUseCentroid,
                                 boolean canUseCoord1, boolean canUseCoord2, boolean canUseCoordSys,
                                 boolean canUseDistance) {}

    private static TapCapability makeDefaultTapCapability(boolean canUpload) {
        return new TapCapability(canUpload, false,
                true,true,false,
                true,false,true,
                false,false,false,
                false,false,false,
                false);
    }

}
