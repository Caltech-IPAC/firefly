/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table.io;
import edu.caltech.ipac.table.*;

import static edu.caltech.ipac.table.DataType.*;
import static edu.caltech.ipac.util.StringUtils.isEmpty;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import edu.caltech.ipac.table.TableUtil;

import edu.caltech.ipac.util.StringUtils;
import uk.ac.starlink.table.*;
import uk.ac.starlink.votable.*;
import nom.tam.fits.FitsFactory;

/**
 * This class handles an action to save a catalog in VOTable (metadata, binary, binary2, fits) format to local file.
 *
 * @author  Cindy Wang
 * @see     DataGroup
 * @see     DataType
 */
public class VoTableWriter {

    /**
     * save the catalogs to a file
     *
     * @param file the file to be saved
     * @param dataGroup data group
     * @param outputFormat votable output format
     * @throws IOException on error
     */
    public static void save(File file, DataGroup dataGroup, TableUtil.Format outputFormat)
            throws IOException {


        OutputStream ostream = new BufferedOutputStream( new FileOutputStream(file) );
        save(ostream, dataGroup, outputFormat);
    }


    /**
     * save the catalogs to a stream, stream is not closed
     *
     * @param stream the output stream to write to
     * @param dataGroup data group
     * @param outputFormat votable output format
     * @throws IOException on error
     */
    public static void save(OutputStream stream, DataGroup dataGroup, TableUtil.Format outputFormat)
            throws IOException {

        FitsFactory.useThreadLocalSettings(true);  // consistent with FitsTableReader
        FitsFactory.setLongStringsEnabled(false);

        VOTableWriterImpl voWriter = new VOTableWriterImpl(outputFormat, dataGroup);
        voWriter.write(stream);

        FitsFactory.useThreadLocalSettings(false);
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

    private static class DataGroupXML {
        private List<DataGroup.Attribute> tableMeta;
        private DataGroup dataGroup;
        private String tagDesc = "DESCRIPTION";

        DataGroupXML(DataGroup dg) {
            this.dataGroup = dg;
            tableMeta = dg.getTableMeta().getKeywords();
        }

        public String xmlINFOs() {
            List<DataGroup.Attribute> metaList = infosInTable(tableMeta);

            return metaList.isEmpty() ? ""
                    : metaList.stream()
                        .map( oneAtt -> "<INFO" +
                                VOSerializer.formatAttribute(TableMeta.NAME, oneAtt.getKey()) +
                                VOSerializer.formatAttribute("value", oneAtt.getValue()) + "/>" )
                        .collect(Collectors.joining("\n"));
        }

        private String xmlDESCRIPTION() {
            String descVal = dataGroup.getAttribute(TableMeta.DESC);

            return isEmpty(descVal) ? "" : "<"+tagDesc+">" + descVal + "</"+tagDesc+">";
        }

        private String xmlTABLE() {
            long nrow = dataGroup.size();
            String tname = dataGroup.getTableMeta().getAttribute(TableMeta.NAME);
            String res = "<TABLE";
            String ucd = dataGroup.getTableMeta().getAttribute(TableMeta.UCD);
            String utype = dataGroup.getTableMeta().getAttribute(TableMeta.UTYPE);

            if (!StringUtils.isEmpty(tname)) {
                res += VOSerializer.formatAttribute(TableMeta.NAME, tname.trim());
            }
            if (nrow > 0) {
                res += VOSerializer.formatAttribute("nrows", Long.toString(nrow));
            }
            if (ucd != null) {
                res += VOSerializer.formatAttribute(TableMeta.UCD, ucd);
            }
            if (utype != null) {
                res += VOSerializer.formatAttribute(TableMeta.UTYPE, utype);
            }
            res += ">";
            return res;
        }

        private String xmlLINK(LinkInfo link) {
            List<String> atts = new ArrayList<>();

            atts.add(elementAtt(TableMeta.ID, link.getID()));
            atts.add(elementAtt("content-role", link.getRole()));
            atts.add(elementAtt("content-type", link.getType()));
            atts.add(elementAtt("title" , link.getTitle()));
            atts.add(elementAtt("value", link.getValue()));
            atts.add(elementAtt("href", link.getHref()));
            atts.add(elementAtt("action", link.getAction()));

            String attsInElement =  atts.stream()
                                    .filter( v -> !isEmpty(v))
                                    .collect(Collectors.joining());

            return "<LINK"+attsInElement + "/>";
        }

        private String xmlVALUES(String nullStr, String minStr, String maxStr, String options) {
            String[] opts = isEmpty(options) ? new String[0] : options.split(",");

            if (isEmpty(nullStr) && isEmpty(minStr) && isEmpty(maxStr) && (opts.length == 0)) {
                return "";
            }

            //  null="null", null='""' or null="xxxx"
            String valuesAtts = elementAtt("null", nullStr);
            String valuesElement = "";
            if (!isEmpty(minStr)) {
                valuesElement += "<MIN" + elementAtt("value", minStr) + "/>\n";
            }
            if (!isEmpty(maxStr)) {
                valuesElement += "<MAX" + elementAtt("value", maxStr) + "/>\n";
            }
            if (opts.length > 0) {
                for (String op : opts) {
                    valuesElement += "<OPTION" + elementAtt("value", op) + "/>\n";
                }
            }

            return completeTag(valuesAtts, valuesElement, "VALUES");
        }

        private String xmlColumnAtt(DataType dt) {
            String prec = dt.getPrecision();
            String width = dt.getWidth() == 0 ? "" : ""+dt.getWidth();
            String atts = elementAtt(TableMeta.ID, dt.getID()) +
                          elementAtt(TableMeta.NAME, dt.getKeyName()) +
                          elementAtt(TableMeta.UCD, dt.getUCD()) +
                          elementAtt("datatype", mapToVoType(dt.getTypeDesc())) +
                          elementAtt("width", width) +
                          elementAtt("precision",
                                     (!isEmpty(prec) && prec.startsWith("G")) ? prec.substring(1) : prec) +
                          elementAtt("unit", dt.getUnits()) +
                          elementAtt(TableMeta.UTYPE, dt.getUType()) +
                          getArraySize(dt);


            return atts;

        }

        public String xmlLINKs(List<LinkInfo> links) {
            return links.stream()
                        .map(oneLink -> xmlLINK(oneLink))
                        .collect(Collectors.joining("\n"));
        }

        private String xmlFIELD(DataType dt) {
            String descriptionTag = tagElement(tagDesc, dt.getDesc());
            String linkTag = xmlLINKs(dt.getLinkInfos());
            String values = xmlVALUES(dt.getNullString(), dt.getMinValue(), dt.getMaxValue(),
                                      dt.getDataOptions());
            String childElements = (isEmpty(descriptionTag) ? "" : descriptionTag + "\n") +
                                   (isEmpty(values) ? "" : values + "\n") +
                                   (isEmpty(linkTag) ? "" : linkTag + "\n");

            String atts = xmlColumnAtt(dt);

            return completeTag(atts, childElements, "FIELD");
        }

        private String xmlPARAM(ParamInfo pInfo) {
            String paramAtts = xmlColumnAtt(pInfo) + elementAtt("value", pInfo.getStringValue());
            String descriptionTag = tagElement(tagDesc, pInfo.getDesc());
            String linkTag = xmlLINKs(pInfo.getLinkInfos());
            String childElements = (isEmpty(descriptionTag) ? "" : descriptionTag + "\n") +
                                   (isEmpty(linkTag) ? "" : linkTag + "\n");

            return completeTag(paramAtts, childElements, "PARAM" );
        }

        public String xmlPARAMs(List<ParamInfo> paramInfos) {
            return paramInfos.stream()
                             .map( (oneParam) -> xmlPARAM(oneParam))
                             .collect(Collectors.joining("\n"));
        }

        private String xmlRefAtt(GroupInfo.RefInfo ref) {
            return  elementAtt(TableMeta.REF, ref.getRef()) +
                    elementAtt(TableMeta.UCD, ref.getUcd()) +
                    elementAtt(TableMeta.UTYPE, ref.getUtype());
        }


        private String xmlPARAMrefs(List<GroupInfo.RefInfo> refs) {
            return refs.stream()
                    .map( (oneRef) -> "<PARAMref" + xmlRefAtt(oneRef) + "/>")
                    .collect(Collectors.joining("\n"));

        }

        private String xmlFIELDrefs(List<GroupInfo.RefInfo> refs) {
            return refs.stream()
                    .map( (oneRef) -> "<FIELDref" + xmlRefAtt(oneRef) + "/>")
                    .collect(Collectors.joining("\n"));
        }


        private String xmlGROUP(GroupInfo gInfo) {
            String params = xmlPARAMs(gInfo.getParamInfos());
            String groups = xmlGROUPs(gInfo.getGroupInfos());
            String paramRefs = xmlPARAMrefs(gInfo.getParamRefs());
            String fieldRefs = xmlFIELDrefs(gInfo.getColumnRefs());
            String descriptionTag = tagElement(tagDesc, gInfo.getDescription());
            String groupElement = "<GROUP" + elementAtt(TableMeta.ID, gInfo.getID()) +
                                             elementAtt(TableMeta.NAME, gInfo.getName()) +
                                             elementAtt(TableMeta.UTYPE, gInfo.getUtype()) +
                                             elementAtt(TableMeta.UCD, gInfo.getUCD()) + ">\n" +
                                  (isEmpty(descriptionTag) ? "" : descriptionTag+"\n") +
                                  (isEmpty(fieldRefs) ? "" : fieldRefs+"\n") +
                                  (isEmpty(params) ? "" : params+"\n") +
                                  (isEmpty(groups) ? "" : groups+"\n") +
                                  (isEmpty(paramRefs) ? "" : paramRefs + "\n") +
                                  "</GROUP>";

            return groupElement;
        }

        public String xmlGROUPs(List<GroupInfo> groups) {

            return groups.stream()
                         .map( (oneGroup -> xmlGROUP(oneGroup)))
                         .collect(Collectors.joining("\n"));
        }

        public static List<DataGroup.Attribute> getInfosFromMeta(List<DataGroup.Attribute> meta) {
            String[] tableMetaNotInfo = { TableMeta.ID, TableMeta.REF,
                                          TableMeta.UCD, TableMeta.UTYPE,
                                          TableMeta.NAME, TableMeta.DESC};

            List<String> attList = Arrays.asList(tableMetaNotInfo);
            List<DataGroup.Attribute> infoList = meta.stream()
                    .filter(oneAtt -> (!oneAtt.isComment()) && !attList.contains(oneAtt.getKey()))
                    .collect(Collectors.toList());
            return infoList;
        }


        private List<DataGroup.Attribute> infosInTable(List<DataGroup.Attribute> meta) {
            return getInfosFromMeta(meta);
        }

        private String tagElement(String tag, String val) {
            return !isEmpty(val) ? "<"+tag+">" + val + "</"+tag+">" : "";
        }

        private String elementAtt(String key, String val) {
             return (val != null && !isEmpty(val))  ? VOSerializer.formatAttribute(key, val) : "";
        }

        private String getArraySize(DataType type) {
            if (type.getDataType() == String.class && StringUtils.isEmpty(type.getArraySize())) {
                return VOSerializer.formatAttribute("arraysize", "*");
            }
            return elementAtt("arraysize", type.getArraySize());
        }

        private String completeTag(String elementAtts, String childElements, String tagName) {
            String startTag = "<"+tagName+elementAtts;
            return isEmpty(childElements) ? startTag+"/>" : startTag+">\n"+childElements+"</"+tagName+">";
        }
    }





    private static class VOTableWriterImpl extends VOTableWriter {
        private DataGroup dataGroup;

        private VOTableWriterImpl(TableUtil.Format outputFormat, DataGroup dataGroup) {
            super(getDataFormat(outputFormat), true, getVotVersion(outputFormat));
            this.dataGroup = dataGroup;
        }

        public void write(OutputStream stream)  throws IOException {

            // this should return only visible columns
            List<String> columns = Arrays.asList(dataGroup.getDataDefinitions()).stream()
                    .filter(dt -> IpacTableUtil.isVisible(dataGroup, dt))
                    .map((dt) -> dt.getKeyName())
                    .collect(Collectors.toList());

            StarTable st = new DataGroupStarTable(dataGroup, columns);



            writeStarTable(st, stream);
        }

        /**
         *  Override to inject additional info into the table.
         *  This also forces inline writing only.. and ignore optimization and File writing.
         */
        @Override
        public void writeStarTables( TableSequence tableSeq, OutputStream out, File file ) throws IOException {

            OutputStreamWriter osw;
            osw = new OutputStreamWriter( out, StandardCharsets.UTF_8);
            BufferedWriter writer = new BufferedWriter( osw );

            /* Output preamble. */
            writePreTableXML( writer );

            /* Loop over all tables for output. */
            int itable = 0;
            for ( StarTable startab; ( startab = tableSeq.nextTable() ) != null;
                  itable++ ) {

                VOSerializer serializer = VOSerializer.makeSerializer( getDataFormat(), getVotableVersion(), startab );

                /* Now add our additional info */
                DataGroupXML dgXML = new DataGroupXML(dataGroup);
                //note: in accordance with the IVOA standard for VOTable, the order of the tags below needs to be maintained
                outputElement(writer, dgXML.xmlTABLE());     // TABLE tag
                outputElement(writer, dgXML.xmlDESCRIPTION());     // DESCRIPTION tag
                serializer.writeFields(writer);     // FIELDS tags
                outputElement(writer, dgXML.xmlGROUPs(dataGroup.getGroupInfos()));          // GROUP
                outputElement(writer, dgXML.xmlPARAMs(dataGroup.getParamInfos())); // PARAM
                outputElement(writer, dgXML.xmlLINKs(dataGroup.getLinkInfos()));

                /* Now write the DATA element. */
                /* ALWAYS write inline. */
                serializer.writeInlineDataElement( writer );

                /* Now add our additional info */
                outputElement(writer, dgXML.xmlINFOs());

                /* Write postamble. */
                serializer.writePostDataXML( writer );
            }
            writePostTableXML( writer );

            /* Tidy up. */
            writer.flush();
        }

        private static DataFormat getDataFormat(TableUtil.Format outputFormat) {


            if (outputFormat.equals(TableUtil.Format.VO_TABLE_BINARY2)) {
                return DataFormat.BINARY2;
            } else if (outputFormat.equals(TableUtil.Format.VO_TABLE_BINARY)) {
                return DataFormat.BINARY;
            } else if (outputFormat.equals(TableUtil.Format.VO_TABLE_FITS)) {
                return DataFormat.FITS;
            } else {
                return DataFormat.TABLEDATA;
            }
        }

        private static VOTableVersion getVotVersion(TableUtil.Format outputFormat) {
            return outputFormat.equals(TableUtil.Format.VO_TABLE_BINARY2) ? VOTableVersion.V13 : VOTableVersion.V12;
        }

        private static void outputElement(BufferedWriter out, String s) throws IOException {
            if (!isEmpty(s)) {
                out.write(s + "\n");
            }
        }

    }
}

