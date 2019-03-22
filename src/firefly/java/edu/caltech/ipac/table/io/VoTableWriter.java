/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table.io;
import edu.caltech.ipac.table.*;
import static edu.caltech.ipac.util.StringUtils.isEmpty;

import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.text.SimpleDateFormat;
import java.util.Date;
import edu.caltech.ipac.table.TableUtil;

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

    public static void save(File file, DataGroup dataGroup, TableUtil.Format outputFormat)
            throws IOException {
        save(file, dataGroup, outputFormat, false);
    }

    public static void save(OutputStream stream, DataGroup dataGroup, TableUtil.Format outputFormat)
            throws IOException {
        save(stream, dataGroup, outputFormat, false);
    }

    /**
     * save the catalogs to a file
     *
     * @param file the file to be saved
     * @param dataGroup data group
     * @param outputFormat votable output format
     * @param isGenericOutput true for doing generic votable output by using Starlink provided functions to output
     *                        a VOTable document with the simplest structure capable of holding TABLE element
     *                        in a range of different format
     *                        false for doing more detail votable output which includes the tagged
     *                        elements under TABLE element such as DESCRIPTION, LINK, GROUP, PARAM, INFO, FIELD
     *                        and child elements under those elements.
     * @throws IOException on error
     */
    public static void save(File file, DataGroup dataGroup, TableUtil.Format outputFormat, boolean isGenericOutput)
            throws IOException {


        OutputStream ostream = new BufferedOutputStream( new FileOutputStream(file) );
        save(ostream, dataGroup, outputFormat, isGenericOutput);
    }


    /**
     * save the catalogs to a stream, stream is not closed
     *
     * @param stream the output stream to write to
     * @param dataGroup data group
     * @param outputFormat votable output format
     * @param isGenericOutput true for doing generic votable output by using Starlink provided functions to output VOTable
     *                        document with simplest structure.
     *                        false for doing more detail voatable output.
     *                        please see save(File file, ....)
     * @throws IOException on error
     */
    public static void save(OutputStream stream, DataGroup dataGroup, TableUtil.Format outputFormat, boolean isGenericOutput)
            throws IOException {

        FitsFactory.useThreadLocalSettings(true);  // consistent with FitsTableReader
        FitsFactory.setLongStringsEnabled(false);

        StarTable st = formStarTableFrom(dataGroup);

        if (isGenericOutput) {
            writeGenericVotable(stream, outputFormat, st);
        } else {
            writeVotable(new OutputStreamWriter(stream), outputFormat, st);
        }
        FitsFactory.useThreadLocalSettings(false);
    }


    private static List<DataType> getColumnsForVotable(DataGroup dataGroup) {

        List<DataType> headers = Arrays.asList(dataGroup.getDataDefinitions());

        // this should return only visible columns
        headers = headers.stream()
                    .filter(dt -> IpacTableUtil.isVisible(dataGroup, dt))
                    .collect(Collectors.toList());

        return headers;
    }

    private static StarTable formStarTableFrom(DataGroup dataGroup) {

        List<DataType> headers = getColumnsForVotable(dataGroup);
        List<ColumnInfo> colList = headers.stream()
                .map((dt) -> DataGroupStarTable.convertToColumnInfo(dt))
                .collect(Collectors.toList());

        ColumnInfo[] colInfos = colList.toArray(new ColumnInfo[colList.size()]);
        return new DataGroupStarTable(dataGroup, colInfos, headers);
    }

    private static void writeGenericVotable(OutputStream stream, TableUtil.Format outputFormat, StarTable st) throws IOException
    {
        VOTableWriter voWriter = new VOTableWriter(getDataFormat(outputFormat), true, getVotVersion(outputFormat));
        voWriter.writeStarTable(st, stream);
    }

    private static void outputElement(BufferedWriter out, String s) throws IOException {
        if (!isEmpty(s)) {
            out.write(s + "\n");
        }
    }

    private static void writeVotable(Writer writer, TableUtil.Format outputFormat, StarTable st) throws IOException
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
            dataTypeMap.put(Date.class, "char");
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
                          elementAtt("datatype", dataTypeMap.get(dt.getDataType())) +
                          getArraySize(dt.getDataType()) +
                          elementAtt("width", width) +
                          elementAtt("precision",
                                     (!isEmpty(prec) && prec.startsWith("G")) ? prec.substring(1) : prec) +
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

