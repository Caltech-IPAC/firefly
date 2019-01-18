/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table.io;
import edu.caltech.ipac.table.*;
import static edu.caltech.ipac.util.StringUtils.isEmpty;
import static edu.caltech.ipac.util.StringUtils.applyIfNotEmpty;

import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.text.SimpleDateFormat;
import java.util.Date;

import uk.ac.starlink.table.*;
import uk.ac.starlink.votable.*;
import nom.tam.fits.FitsFactory;

/**
 * This class handles an action to save a catalog in IPAC table format to local file.
 *
 * @author Xiuqin Wu
 * @see DataGroup
 * @see DataObject
 * @see DataType
 * @version $Id: IpacTableWriter.java,v 1.11 2012/08/10 20:58:28 tatianag Exp $
 */
public class VoTableWriter {

    public static void save(File file, DataGroup dataGroup, String outputFormat)
            throws IOException {
        save(file, dataGroup, outputFormat, false, true);
    }

    public static void save(File file, DataGroup dataGroup, String outputFormat, boolean isGenericOutput)
        throws IOException {
        save(file, dataGroup, outputFormat, false, isGenericOutput);
    }

    public static void save(OutputStream stream, DataGroup dataGroup, String outputFormat)
            throws IOException {
        save(stream, dataGroup, outputFormat, false, true);
    }

    public static void save(OutputStream stream, DataGroup dataGroup, String outputFormat, boolean isGenericOutput)
            throws IOException {
        save(stream, dataGroup, outputFormat, false, isGenericOutput);
    }

    /**
     * save the catalogs to a file
     *
     * @param file the file to be saved
     * @param dataGroup data group
     * @param outputFormat votable output format
     * @throws IOException on error
     */
    public static void save(File file, DataGroup dataGroup, String outputFormat, boolean forExport, boolean isGenericOutput)
            throws IOException {

        FitsFactory.useThreadLocalSettings(true);  // consistent with FitsTableReader
        FitsFactory.setLongStringsEnabled(false);

        StarTable st = formStarTableFrom(dataGroup, forExport, outputFormat);

        if (isGenericOutput) {
            writeGenericVotable(file, outputFormat, st);
        } else {
            writeVotable(new FileWriter(file), outputFormat, st);
        }
    }


    /**
     * save the catalogs to a stream, stream is not closed
     *
     * @param stream the output stream to write to
     *
     * @param dataGroup data group
     * @param outputFormat votable output format
     * @throws IOException on error
     */
    public static void save(OutputStream stream, DataGroup dataGroup, String outputFormat, boolean forExport, boolean isGenericOutput)
            throws IOException {

        FitsFactory.useThreadLocalSettings(true);  // consistent with FitsTableReader
        FitsFactory.setLongStringsEnabled(false);

        StarTable st = formStarTableFrom(dataGroup, forExport, outputFormat);

        if (isGenericOutput) {
            writeGenericVotable(stream, outputFormat, st);
        } else {
            writeVotable(new OutputStreamWriter(stream), outputFormat, st);
        }
    }

    private static List<DataType> getColumnsForVotable(DataGroup dataGroup, boolean forExport) {

        List<DataType> headers = Arrays.asList(dataGroup.getDataDefinitions());

        if (forExport) {
            // this should return only visible columns
            headers = headers.stream()
                    .filter(dt -> IpacTableUtil.isVisible(dataGroup, dt)
                            && !dt.getKeyName().equals(DataGroup.ROW_IDX)
                            && !dt.getKeyName().equals(DataGroup.ROW_NUM))
                    .collect(Collectors.toList());
        }

        return headers;
    }

    private static StarTable formStarTableFrom(DataGroup dataGroup, boolean forExport, String outputFormat) {

        List<DataType> headers = getColumnsForVotable(dataGroup, forExport);
        List<ColumnInfo> colList = headers.stream()
                .map((dt) -> DataGroupStarTable.convertToColumnInfo(dt, outputFormat))
                .collect(Collectors.toList());

        ColumnInfo[] colInfos = colList.toArray(new ColumnInfo[colList.size()]);
        return new DataGroupStarTable(dataGroup, colInfos, headers);
    }


    private static void writeGenericVotable(File file, String outputFormat, StarTable st) throws IOException
    {
        VOTableWriter voWriter = new VOTableWriter(getDataFormat(outputFormat), true, getVotVersion(outputFormat));
        voWriter.writeStarTable(st, file.getName(), new StarTableOutput());
    }

    private static void writeGenericVotable(OutputStream stream, String outputFormat, StarTable st) throws IOException
    {
        VOTableWriter voWriter = new VOTableWriter(getDataFormat(outputFormat), true, getVotVersion(outputFormat));
        voWriter.writeStarTable(st, stream);
    }

    private static void outputElement(BufferedWriter out, String s) throws IOException {
        if (!isEmpty(s)) {
            out.write(s + "\n");
        }
    }

    private static void writeVotable(Writer writer, String outputFormat, StarTable st) throws IOException
    {
        BufferedWriter out = new BufferedWriter(writer);
        DataGroup dataGroup = ((DataGroupStarTable)st).getDataGroup();
        DataGroupXML dgXML = new DataGroupXML( dataGroup, ((DataGroupStarTable)st).getColumns());
        VOTableVersion ver =  getVotVersion(outputFormat);

        out.write("<?xml version='1.0'?>\n");
        out.write("<VOTABLE version=\"" + ver.getVersionNumber() + "\">\n");
        out.write("<RESOURCE type=\"results\">\n");
        VOSerializer ser = VOSerializer.makeSerializer(getDataFormat(outputFormat), ver, st);

        out.write(dgXML.xmlTABLE() + "\n");                    // TABLE start tag
        outputElement(out, dgXML.xmlDESCRIPTION());     // DESCRIPTION tag
        outputElement(out, dgXML.xmlGROUPs());          // GROUP
        outputElement(out, dgXML.xmlPARAMs(dataGroup.getParamInfos())); // PARAM
        outputElement(out, dgXML.xmlFIELDs());          // FIELD
        outputElement(out, dgXML.xmlLINKs(dataGroup.getLinkInfos()));
        ser.writeInlineDataElement(out);
        outputElement(out, dgXML.xmlINFOs());
        out.write("</TABLE>\n");
        out.write("</RESOURCE>\n");
        out.write("</VOTABLE>\n");
        out.close();
    }


    private static DataFormat getDataFormat(String outputFormat) {
        String outF = outputFormat.toLowerCase();

        if (outF.contains("binary2")) {
            return DataFormat.BINARY2;
        } else if (outF.contains("binary")) {
            return DataFormat.BINARY;
        } else if (outF.contains("fits")) {
            return DataFormat.FITS;
        } else {
            return DataFormat.TABLEDATA;
        }
    }

    private static VOTableVersion getVotVersion(String outputFormat) {
        return outputFormat.toLowerCase().contains("binary2") ? VOTableVersion.V13 : VOTableVersion.V12;
    }


    private static String getNowTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return df.format(new Date());
    }

    public static class DataGroupXML {
        private List<DataGroup.Attribute> tableMeta;
        private DataGroup dataGroup;
        private int totalRow;
        private List<DataType> columnsDef;
        private String tagDesc = "DESCRIPTION";
        private static final Map<Class, String> dataTypeMap = new HashMap<>();
        static {
            initDataTypeMap();
        }

        // not included: "bit", "unicodeCode", "floatComplex", "doubleComplex"
        private static void initDataTypeMap() {
            dataTypeMap.put(Boolean.class, "boolean");
            dataTypeMap.put(Byte.class, "unsignedByte");
            dataTypeMap.put(Short.class, "short");
            dataTypeMap.put(Integer.class, "int");
            dataTypeMap.put(Long.class, "long");
            dataTypeMap.put(String.class, "char");
            dataTypeMap.put(Character.class, "char");
            dataTypeMap.put(Float.class, "float");
            dataTypeMap.put(Double.class, "double");
        }


        public DataGroupXML(DataGroup dg, List<DataType> columns) {
            this.dataGroup = dg;
            this.columnsDef = columns;
            tableMeta = dg.getTableMeta().getKeywords();
            totalRow = dataGroup.size();
        }

        public String xmlTABLE() {
            List<String> tableAtt = new ArrayList<String>() {{
               add(TableMeta.ID);
               add(TableMeta.NAME);
               add(TableMeta.UCD);
               add(TableMeta.UTYPE);
               add(TableMeta.REF);
            }};
            String attStr = "";

            for (String att : tableAtt) {
                attStr += elementAtt(att, getMetaValueOf(att));
            }

            return "<TABLE" + attStr + VOSerializer.formatAttribute("nrows", ""+ totalRow) + ">";
        }

        public String xmlINFOs() {
            List<DataGroup.Attribute> metaList = infosInTable(tableMeta);

            if (metaList.size() == 0) {
                return "";
            }

            return metaList.size() == 0 ? "" : metaList.stream()
                                                .map( oneAtt -> "<INFO" +
                                                        VOSerializer.formatAttribute(TableMeta.NAME, oneAtt.getKey()) +
                                                        VOSerializer.formatAttribute("value", oneAtt.getValue()) + "/>" )
                                                .collect(Collectors.joining("\n"));
        }

        public String xmlDESCRIPTION() {
            String descVal = getMetaValueOf(TableMeta.DESC);

            return isEmpty(descVal) ? "" : "<"+tagDesc+">" + descVal + "</"+tagDesc+">";
        }

        private String xmlLINK(LinkInfo link) {
            List<String> atts = new ArrayList<>();

            atts.add(elementAtt(TableMeta.ID, link.getID()));
            atts.add(elementAtt("content-role", link.getRole()));
            atts.add(elementAtt("content-type", link.getType()));
            atts.add(elementAtt("title" , link.getTitle()));
            atts.add(elementAtt("href", link.getHref()));

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
                          elementAtt("datatype", dataTypeMap.get(dt.getDataType())) +
                          getArraySize(dt.getDataType()) +
                          elementAtt("width", width) +
                          elementAtt("precision",
                                     (!isEmpty(prec) && prec.startsWith("F")) ? prec.substring(1) : prec) +
                          elementAtt("unit", dt.getUnits()) +
                          elementAtt(TableMeta.UTYPE, dt.getUType()) +
                          elementAtt(TableMeta.REF, dt.getRef());


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

        public String xmlFIELDs() {
            return columnsDef.stream()
                             .map( oneCol -> xmlFIELD(oneCol))
                             .collect(Collectors.joining("\n"));

        }

        private String xmlPARAM(ParamInfo pInfo) {
            String paramAtts = xmlColumnAtt(pInfo) + elementAtt("value", pInfo.getValue());
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
            String paramRefs = xmlPARAMrefs(gInfo.getParamRefs());
            String fieldRefs = xmlFIELDrefs(gInfo.getColumnRefs());
            String descriptionTag = tagElement(tagDesc, gInfo.getDescription());
            String groupElement = "<GROUP" + elementAtt(TableMeta.ID, gInfo.getID()) +
                                             elementAtt(TableMeta.NAME, gInfo.getName()) + ">\n" +
                                  (isEmpty(descriptionTag) ? "" : descriptionTag+"\n") +
                                  (isEmpty(fieldRefs) ? "" : fieldRefs+"\n") +
                                  (isEmpty(params) ? "" : params+"\n") +
                                  (isEmpty(paramRefs) ? "" : paramRefs + "\n") +
                                  "</GROUP>";

            return groupElement;
        }

        public String xmlGROUPs() {
            List<GroupInfo> groups = dataGroup.getGroupInfos();

            return groups.stream()
                         .map( (oneGroup -> xmlGROUP(oneGroup)))
                         .collect(Collectors.joining("\n"));
        }

        private String getMetaValueOf(String keyName) {
            return DataGroupStarTable.getTableMetaValue(keyName, tableMeta);
        }

        private List<DataGroup.Attribute> infosInTable(List<DataGroup.Attribute> meta) {
            return DataGroupStarTable.getInfosFromMeta(meta);
        }

        private String tagElement(String tag, String val) {
            return !isEmpty(val) ? "<"+tag+">" + val + "</"+tag+">" : "";
        }

        private String elementAtt(String key, String val) {
             return (val != null && !isEmpty(val))  ? VOSerializer.formatAttribute(key, val) : "";
        }

        private String getArraySize(Class type) {
            return type.equals(String.class) ? elementAtt("arraysize", "*") : "";
        }

        private String completeTag(String elementAtts, String childElements, String tagName) {
            String startTag = "<"+tagName+elementAtts;
            return isEmpty(childElements) ? startTag+"/>" : startTag+">\n"+childElements+"</"+tagName+">";
        }
    }
}

