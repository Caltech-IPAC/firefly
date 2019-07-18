/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table.io;

import edu.caltech.ipac.firefly.core.FileAnalysis;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.table.*;
import edu.caltech.ipac.util.FitsHDUUtil;
import edu.caltech.ipac.util.StringUtils;
import org.json.simple.JSONObject;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.*;
import uk.ac.starlink.votable.*;
import uk.ac.starlink.util.DOMUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static edu.caltech.ipac.util.StringUtils.applyIfNotEmpty;
import static edu.caltech.ipac.util.StringUtils.isEmpty;
import static uk.ac.starlink.table.StoragePolicy.PREFER_MEMORY;

/**
 * Date: Dec 5, 2011
 *
 * @author loi
 * @version $Id: VoTableUtil.java,v 1.4 2013/01/07 22:10:01 tatianag Exp $
 */
public class VoTableReader {

    private static final Pattern HMS_UCD_PATTERN =
            Pattern.compile( "POS_EQ_RA.*|pos\\.eq\\.ra.*",
                    Pattern.CASE_INSENSITIVE );
    private static final Pattern DMS_UCD_PATTERN =
            Pattern.compile( "POS_EQ_DEC.*|pos\\.eq\\.dec.*",
                    Pattern.CASE_INSENSITIVE );

    /**
     * returns an array of DataGroup from a vo table.
     * @param location  location of the vo table data source using automatic format detection.  Can be file or url.
     * @return an array of DataGroup objects
     */
    public static DataGroup[] voToDataGroups(String location) throws IOException {
        try {
            return voToDataGroups(location, false);
        } catch (DataAccessException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * returns an array of DataGroup from a votable file.
     * @param location  location of the votable data source using automatic format detection.  Can be file or url.
     * @param headerOnly  if true, returns only the headers, not the data.
     * @return an array of DataGroup object
     */
    public static DataGroup[] voToDataGroups(String location, boolean headerOnly) throws IOException, DataAccessException {
        //VOTableBuilder votBuilder = new VOTableBuilder();
        List<DataGroup> groups = new ArrayList<>();

        try {
            //DataSource datsrc = DataSource.makeDataSource(voTableFile);
            //StoragePolicy policy = StoragePolicy.getDefaultPolicy();
            //TableSequence tseq = votBuilder.makeStarTables( datsrc, policy );
            //StarTableFactory stFactory = new StarTableFactory();
            //TableSequence tseq = stFactory.makeStarTables(location, null);

            List<TableElement> tableAry = getTableElementsFromFile( location, null);
            for ( TableElement tableEl : tableAry ) {
                DataGroup dg = convertToDataGroup(tableEl, new VOStarTable(tableEl), headerOnly);
                groups.add(dg);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        }

        return groups.toArray(new DataGroup[groups.size()]);
    }

    public static String getError(InputStream inputStream, String location) throws DataAccessException {
        try {
            VOElementFactory voFactory =  new VOElementFactory();
            voFactory.setStoragePolicy(PREFER_MEMORY);
            VOElement top = voFactory.makeVOElement(inputStream, null);
            return getQueryStatusError(top);
        }  catch (SAXException |IOException e) {
            e.printStackTrace();
            throw new DataAccessException("unable to parse " + location + "\n"+
                    e.getMessage(), e);
        }
    }

    public static final String ID = "ID";
    public static final String REF = "ref";
    public static final String UCD = "ucd";
    public static final String UTYPE = "utype";
    public static final String DESC = "desc";
    public static final String NAME = "name";

    private static String getElementAttribute(VOElement element, String attName) {
        return element.hasAttribute(attName) ? element.getAttribute(attName) : null;
    }

    // root VOElement for a votable file
    private static VOElement makeVOElement(File infile, StoragePolicy policy) throws DataAccessException {
        try {
            policy = policy == null ? PREFER_MEMORY : policy;
            VOElementFactory voFactory =  new VOElementFactory();
            voFactory.setStoragePolicy(policy);
            return voFactory.makeVOElement(infile);
        }  catch (SAXException |IOException e) {
            e.printStackTrace();
            throw new DataAccessException("unable to parse "+ infile.getPath() + "\n" +
                    e.getMessage(), e);
        }
    }

    private static VOElement getVOElementFromVOTable(String location, StoragePolicy policy) throws DataAccessException {
        return makeVOElement(new File(location), policy);
    }

    // get all <RESOURCE> under VOTable root or <RESOURCE>
    private static VOElement[] getResourceChildren(VOElement parent) {
        return parent.getChildrenByName( "RESOURCE" );
    }

    // get all <TABLE> children under <RESOURCE>
    private static VOElement[] getTableChildren(VOElement parent) {
        return parent.getChildrenByName( "TABLE" );
    }

    // get all <GROUP> under <TABLE>
    private static VOElement[] getGroupsFromTable(TableElement tableEl) {
        return tableEl.getChildrenByName("GROUP");
    }

    // get all <LINK> under <TABLE>
    private static LinkElement[] getLinksFromTable(TableElement tableEl) {
        return tableEl.getLinks();
    }

    // get all <PARAM> under <TABLE>
    private  static VOElement[] getParamsFromTable(TableElement tableEl) {
        return tableEl.getChildrenByName("PARAM");
    }

    // get all <INFO> under <TABLE>
    private static VOElement[] getInfoElementsFromTable(TableElement tableEl) {
        return tableEl.getChildrenByName("INFO");

    }

    // get all <FIELD> under <TABLE>
    private static FieldElement[] getFieldsFromTable(TableElement tableEl) {
        return tableEl.getFields();
    }

    // get all <LINK> from <FIELD>
    private static VOElement[] getLinksFromField(FieldElement fieldEl) {
        // VOElement -> LinkElement
        VOElement[] voLinkAry = fieldEl.getChildrenByName("LINK");
        VOElement[] links = new LinkElement[voLinkAry.length];
        System.arraycopy(voLinkAry, 0, links, 0, voLinkAry.length);

        return links;
    }

    // convert <GROUP>s under <TABLE> to a list of GroupInfo and add it to the associatede table (a DataGroup object)
    private static List<GroupInfo> makeGroupInfosFromTable(TableElement tableEl) {
        VOElement[] groupAry = getGroupsFromTable(tableEl);
        List<GroupInfo> groupObjAry = new ArrayList<>();

        for (VOElement group : groupAry) {
            String name = group.getName();
            String desc = group.getDescription();

            GroupInfo gObj = new GroupInfo(name, desc);
            gObj.setID(getElementAttribute(group, ID));

            // add FIELDref
            Arrays.stream(group.getChildrenByName("FIELDref"))
                    .forEach(pEl -> gObj.getColumnRefs().add(refInfoFromEl(pEl)));

            // add PARAMrefs
            Arrays.stream(group.getChildrenByName("PARAMref"))
                    .forEach(pEl -> gObj.getParamRefs().add(refInfoFromEl(pEl)));

            // add PARAMs
            Arrays.stream(group.getChildrenByName("PARAM"))
                .forEach(pEl -> gObj.getParamInfos().add(paramInfoFromEl(pEl)));

            groupObjAry.add(gObj);
        }
        return groupObjAry;
    }

    // convert LinkElement to LinkInfo
    private static LinkInfo linkElementToLinkInfo(VOElement el) {
        if (el == null) return null;
        LinkInfo li = new LinkInfo();
        applyIfNotEmpty(el.getAttribute(ID), li::setID);
        applyIfNotEmpty(el.getAttribute("content-role"), li::setRole);
        applyIfNotEmpty(el.getAttribute("content-type"), li::setType);
        applyIfNotEmpty(el.getAttribute("title"), li::setTitle);
        applyIfNotEmpty(el.getAttribute("href"), li::setHref);
        applyIfNotEmpty(el.getAttribute("value"), li::setValue);
        applyIfNotEmpty(el.getAttribute("action"), li::setAction);
        return li;
    }

    // convert <LINK>s under <TABLE> to a list of LinkInfo and add it to the associated table (a DataGroup object)
    private static List<LinkInfo> makeLinkInfosFromTable(TableElement tableEl) {
        VOElement[] linkAry = getLinksFromTable(tableEl);
        List<LinkInfo> linkObjAry = new ArrayList<>();

        for (VOElement link : linkAry) {
            LinkInfo linkObj = linkElementToLinkInfo(link);

            if (linkObj != null) {
                linkObjAry.add(linkObj);
            }
        }

        return linkObjAry;
    }


    // precision: "2" => "F2", "F2"=> "F2", "E3" => "E3"
    private static String makePrecisionStr(String precisionStr) {
        return (precisionStr.matches("^[1-9][0-9]*$")) ? ("F"+precisionStr) : precisionStr;
    }

    // convert <PARAM>s under <TABLE> to a list of <DataType> and add it to the associated table (a DataGroup object)
    private static List<ParamInfo> makeParamsFromTable(TableElement tableEl, StarTable table) {

        VOElement[] paramsEl = getParamsFromTable(tableEl);
        List<ParamInfo> allParams = new ArrayList<>();

        for (VOElement param : paramsEl) {
            String name = getElementAttribute(param, "name");
            DescribedValue dv = table.getParameterByName(name);
            if (dv == null) continue;

            ValueInfo vInfo = dv.getInfo();
            Class clz = vInfo.isArray() ? String.class : vInfo.getContentClass();

            // create Datatype
            ParamInfo params = new ParamInfo(name, clz);
            params.setUnits(vInfo.getUnitString());

            if (vInfo.isArray()) params.setTypeDesc(DataType.LONG_STRING);

            // set precision
            String precisionStr = getElementAttribute(param, "precision");
            String widthStr = getElementAttribute(param, "width");

            if (precisionStr != null) {
                precisionStr = makePrecisionStr(precisionStr);
                params.setPrecision(precisionStr);
            }

            // set width
            if (widthStr != null) {
                params.setWidth(Integer.parseInt(widthStr));
            }

            // set ucd, utype and value to DataType
            params.setUCD(getElementAttribute(param, "ucd"));
            params.setUType(getElementAttribute(param, "utype"));
            params.setID(getElementAttribute(param, "ID"));
            params.setValue(getElementAttribute(param, "value"));
            VOElement desElement = param.getChildByName("DESCRIPTION");
            if (desElement != null) {
                params.setDesc(DOMUtils.getTextContent(desElement));
            }

            allParams.add(params);
        }
        return allParams;
    }

    // get <INFO>s under <TABLE> as a list of DescribeValue
    private static List<DescribedValue> getInfosFromTable(TableElement tableEl) {

        VOElement[] infoEl = getInfoElementsFromTable(tableEl);
        List<DescribedValue> infosAry = new ArrayList<>();

        for (VOElement param : infoEl) {
            String name = getElementAttribute(param, "name");
            String val = getElementAttribute(param, "value");
            if (name != null && val != null) {
                infosAry.add(new DescribedValue(new DefaultValueInfo(name), val));
            }
        }
        return infosAry;
    }

    // get the FieldElement from a TableElement per field name
    private static FieldElement getFieldElementByName(TableElement tableEl, String name) {
        FieldElement[] fields = getFieldsFromTable(tableEl);

        for (FieldElement f : fields) {
            if ((f.getName() != null) && (f.getName().equals(name))) {
                return f;
            }
        }
        return null;
    }

    // convert <LINK>s under <FIELD> to a list of LinkInfo and add it to the associated column (a DataType Object)
    private static List<LinkInfo> makeLinkInfosFromField(TableElement tableEl, DataType dt) {
        List<LinkInfo> linkObjs = new ArrayList<>();
        FieldElement fieldEle = getFieldElementByName(tableEl, dt.getKeyName());

        VOElement[] links = (fieldEle != null) ? getLinksFromField(fieldEle) : null;
        if (links != null) {
            for (VOElement link : links) {
                LinkInfo linkObj = linkElementToLinkInfo(link);

                if (linkObj != null) {
                    List<LinkInfo> dtLinkInfos = dt.getLinkInfos();

                    if (dtLinkInfos != null) {
                        dtLinkInfos.add(linkObj);
                    }
                    linkObjs.add(linkObj);
                }
            }
        }
        return linkObjs;
    }

    private static String optionsToStr(String[] options) {
        if (options != null) {
            for (int i = 0; i < options.length; i++) {
                options[i] = options[i].trim();
            }
        }
        return StringUtils.toString(options, ",");
    }

    // add info of <VALUES> under a <FIELD> to the associated column (a DataType object)
    // the added info includes: minimum/maximum values, options, or null string
    private static void getValuesFromField(TableElement tableEl, DataType dt) {
        FieldElement fieldEle = getFieldElementByName(tableEl, dt.getKeyName());
        if (fieldEle == null) return;

        ValuesElement values = fieldEle.getActualValues();

        if (values != null) {
            dt.setNullString(values.getNull());
            dt.setMaxValue(values.getMaximum());
            dt.setMinValue(values.getMinimum());

            String[] options = values.getOptions();
            if (options != null) {
                dt.setDataOptions(optionsToStr(options));
            }
        }
    }

    // recursively collect all table element
    private static void getTableElements(VOElement root, List<TableElement>tblAry) {
        VOElement[] resources = getResourceChildren(root);

        for (VOElement resource : resources) {
            VOElement[] tables = getTableChildren(resource);

            for (VOElement tbl : tables) {
                TableElement tableEl = (TableElement) tbl;
                tblAry.add(tableEl);
            }

            VOElement[] subResources = getResourceChildren(resource);
            for (VOElement res : subResources) {
                getTableElements(res, tblAry);
            }
        }
    }

    // get all <TABLE> from one votable file
    private static List<TableElement> getTableElementsFromFile(String location, StoragePolicy policy) throws DataAccessException {
        VOElement top = getVOElementFromVOTable(location, policy);

        List<TableElement> tableAry = new ArrayList<>();

        if (top != null) {
            getTableElements(top, tableAry);

            if (tableAry.size() == 0) {
                String error = getQueryStatusError(top);
                if (error != null) {
                    throw new DataAccessException(error);
                }
            }
        }

        return tableAry;
    }

    private static String getQueryStatusError(VOElement top) {
        String error = null;
        // check for errors: section 4.4 of http://www.ivoa.net/documents/DALI/20170517/REC-DALI-1.1.html
        VOElement[] resources = getResourceChildren(top);
        for (VOElement r : resources) {
            if ("results".equals(r.getAttribute("type"))) {
                VOElement[] infos = r.getChildrenByName("INFO");
                for (VOElement info : infos) {
                    if ("QUERY_STATUS".equals(info.getName()) &&
                            "ERROR".equalsIgnoreCase(info.getAttribute("value"))) {
                        error = info.getTextContent();
                    }
                }
            }
        }
        // workaround for misplaced INFO attributes with errors
        VOElement[] infos = top.getChildrenByName( "INFO" );
        String [] namesWithMisspelling = {"QUERY_STATUS","QUERY STATUS"};
        for (VOElement info : infos) {
            String name = info.getName();
            if (Arrays.asList(namesWithMisspelling).contains(name)
                    && "ERROR".equalsIgnoreCase(info.getAttribute("value"))) {
                error = info.getTextContent();
            }
        }
        return error;
    }

    private enum MetaInfo {
        INDEX("Index", "Index", Integer.class, "table index"),
        TABLE("Table", "Table", String.class, "table name"),
        TYPE("Type", "Type", String.class, "table type");

        String keyName;
        String title;
        Class  metaClass;
        String description;

        MetaInfo(String key, String title, Class c, String des) {
            this.keyName = key;
            this.title = title;
            this.metaClass = c;
            this.description = des;
        }

        List<Object> getInfo() {
            return Arrays.asList(keyName, title, metaClass, description);
        }

        String getKey() {
            return keyName;
        }

        String getTitle() {
            return title;
        }

        Class getMetaClass() {
            return metaClass;
        }

        String getDescription() {
            return description;
        }
    }


    /**
     * convert votable content into DataGroup mainly by collecting TABLE elements and TABLE included metadata and data
     * For TABLE, collect attributes including ID, name, ucd, utype, ref, (as ID, title, ucd, utype, ref in DataGroup),
     *            and child elements including DESCRIPTION, FIELD, GROUP, LINK, (as desc, columns, groups, links in DataGroup),
     *                                         PARAM (as staticColumns in DataGroup and staticValue in DataType),
     *                                         INFO (as meta in DataGroup)
     *            and table data in DATA element if not headerOnly (data in DataGroup)
     * For FIELD, collect attributes including ID, name, datatype, unit, precision, width, ref, ucd, utype
     *                                         (as ID, keyName, type, units, precision, ref, ucd, utype in DataType)
     *            and child elements including DESCRIPTION, LINK and VALUES
     *                                         (as desc, links, minValue, maxValue, options, nullstring in DataType)
     *
     * @param tableEl    TableElement object
     * @param table      StarTable object
     * @param headerOnly get column and metadata only (no table data)
     * @return a DataGroup object representing a TABLE in votable file
     */
    private static DataGroup convertToDataGroup(TableElement tableEl, StarTable table,  boolean headerOnly) {
        String title = table.getName();
        List<DataType> cols = new ArrayList<>();
        String raCol=null, decCol=null;
        int precision = 8;
        String precisionStr = "";

        VOElement[] fields = tableEl.getChildrenByName("FIELD");


        // collect FIELD from TABLE
        // collect attributes of <FIELD> including name, datatype, unit, precision, width, ref, ID, ucd, utype
        // collect children elements including <DESCRIPTION>, <LINK>, <VALUES>
        for (int i = 0; i < table.getColumnCount(); i++) {
            ColumnInfo cinfo = table.getColumnInfo(i);

            DataType dt = convertToDataType(cinfo);

            // quick and dirty way to fix column name when it's not provided
            dt.setKeyName(getCName(fields[i], i));

            // attribute precision
            if(cinfo.getAuxDatum(VOStarTable.PRECISION_INFO)!=null){
                try{
                    DescribedValue pDV  = cinfo.getAuxDatum(VOStarTable.PRECISION_INFO);

                    if (pDV != null) {
                        precisionStr = pDV.getValue().toString();
                        precisionStr = makePrecisionStr(precisionStr);
                        dt.setPrecision(precisionStr);
                    }
                } catch (NumberFormatException e){
                    // problem with VOTable vinfo precision: should be numeric - keep default min precision
                }
            }

            // attribute width
            if (cinfo.getAuxDatum(VOStarTable.WIDTH_INFO) != null) {
                dt.setWidth(Integer.parseInt(cinfo.getAuxDatum(VOStarTable.WIDTH_INFO).getValue().toString()));
            }

            // attribute ref
            if (cinfo.getAuxDatum(VOStarTable.REF_INFO) != null) {
                dt.setRef(cinfo.getAuxDatum(VOStarTable.REF_INFO).getValue().toString());
            }

            // attribute ucd
            String ucd = cinfo.getUCD();
            if ( ucd != null) { // should we save all UCDs?
                if (HMS_UCD_PATTERN.matcher( ucd ).matches()) {
                    raCol = cinfo.getName();
                }
                if (DMS_UCD_PATTERN.matcher( ucd ).matches()) {
                    decCol = cinfo.getName();
                }
                dt.setUCD(ucd);
            }

            // attribute utype
            String utype = cinfo.getUtype();
            if (utype != null) {
                dt.setUType(utype);
            }

            // attribute ID
            if (cinfo.getAuxDatum(VOStarTable.ID_INFO) != null) {
                dt.setID(cinfo.getAuxDatum(VOStarTable.ID_INFO).getValue().toString());
            }

            // child element DESCRIPTION
            String desc = cinfo.getDescription();
            if (desc != null) {
                dt.setDesc(desc.replace("\n", " "));
            }

            // attribute type  .. this is not yet standard, mentioned in appendix
            // used for generating links and link substitutions
            DescribedValue type = cinfo.getAuxDatumByName("Type");
            if (type != null) {
                String val = String.valueOf(type.getValue());
                if (val.equals(DataType.LOCATION)) {
                    dt.setTypeDesc(val);
                } else if (val.equals("hidden")) {
                    dt.setVisibility(DataType.Visibility.hidden);
                }
            }

            // child elements <LINK> and <VALUES>
            if (tableEl != null) {
                makeLinkInfosFromField(tableEl, dt);
                getValuesFromField(tableEl, dt);
            }

            cols.add(dt);
        }

        // attribute name from TABLE
        DataGroup dg = new DataGroup(title, cols);

        // metadata for TABLE
        List<DescribedValue> dvAry = (tableEl == null) ? new ArrayList<>()
                                                       : getInfosFromTable(tableEl);
        for (Object p : dvAry) {
            DescribedValue dv = (DescribedValue)p;
            dg.getTableMeta().addKeyword(dv.getInfo().getName(), dv.getValueAsString(Integer.MAX_VALUE).replace("\n", " "));
        }


        if (raCol != null && decCol != null) {
            dg.addAttribute("POS_EQ_RA_MAIN", raCol);
            dg.addAttribute("POS_EQ_DEC_MAIN", decCol);
        }

        if (tableEl != null) {
            // attribute ID, ref, ucd, utype from TABLE
            applyIfNotEmpty(getElementAttribute(tableEl, ID), v -> dg.getTableMeta().addKeyword(TableMeta.ID, v));
            applyIfNotEmpty(getElementAttribute(tableEl, REF), v -> dg.getTableMeta().addKeyword(TableMeta.REF, v));
            applyIfNotEmpty(getElementAttribute(tableEl, UCD), v -> dg.getTableMeta().addKeyword(TableMeta.UCD, v));
            applyIfNotEmpty(getElementAttribute(tableEl, UTYPE), v -> dg.getTableMeta().addKeyword(TableMeta.UTYPE, v));
            applyIfNotEmpty(getElementAttribute(tableEl, NAME), v -> dg.getTableMeta().addKeyword(TableMeta.NAME, v));

            // child element PARAM, GROUP, LINK for TABLE
            dg.setParamInfos(makeParamsFromTable(tableEl, table));
            dg.setGroupInfos(makeGroupInfosFromTable(tableEl));
            dg.setLinkInfos(makeLinkInfosFromTable(tableEl));

            // child element DESCRIPTION
            String tDesc = tableEl.getDescription();
            if (tDesc != null) {
                dg.getTableMeta().addKeyword(TableMeta.DESC, tDesc.replace("\n", " "));
            }
        }

        // table data
        try {
            if (!headerOnly) {
                RowSequence rs = table.getRowSequence();
                int len;
                int find = 0;
                String findStr;

                while (rs.next()) {
                    DataObject row = new DataObject(dg);
                    for(int i = 0; i < cols.size(); i++) {
                        DataType dtype = cols.get(i);
                        Object val = rs.getCell(i);
                        String sval = table.getColumnInfo(i).formatValue(val, Integer.MAX_VALUE);

                        if (dtype.getKeyName().equals("spans")) {
                            len = sval.length();

                            if (len > 65536) {
                                find = 1;
                                findStr = val.toString();
                            }
                        }
                        if (dtype.getDataType().isAssignableFrom(String.class) && !(val instanceof String)) {
                            row.setDataElement(dtype, sval);   // array value
                        } else {
                            if ((val instanceof Double && Double.isNaN((Double) val)) ||
                                (val instanceof Float && Float.isNaN((Float) val))    )    {
                                val = null;
                            }
                            row.setDataElement(dtype, val);
                        }
                        if (dtype.getPrecision() == null) {
                            IpacTableUtil.guessFormatInfo(dtype, sval, precision);// precision min 8 can come from VOTable attribute 'precision' later on.
                        }
                    }
                    dg.add(row);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        dg.trimToSize();
        return dg;
    }

    public static DataType convertToDataType(ColumnInfo cinfo) {
        // attribute datatype, if the column is with data in array style, set the datatype to be "string" type
        Class clz = cinfo.isArray() ? String.class : cinfo.getContentClass();
        String nullString = null;

        String classType = DefaultValueInfo.formatClass(clz);
        Class java_class;

        if ((classType.contains("boolean")) || (classType.contains("Boolean"))) {
            java_class = Boolean.class;
        } else if ((classType.contains("byte")) || (classType.contains("Byte"))) {
            java_class = Integer.class;
        } else if ((classType.contains("short")) || (classType.contains("Short"))) {
            java_class = Integer.class;
        } else if ((classType.contains("int")) || (classType.contains("Integer"))) {
            java_class = Integer.class;
        } else if ((classType.contains("long")) || (classType.contains("Long"))) {
            java_class = Long.class;
        } else if ((classType.contains("float")) || (classType.contains("Float"))) {
            java_class = Float.class;
        } else if ((classType.contains("double")) || (classType.contains("Double"))) {
            java_class = Double.class;
        } else {        // char, string or else
            java_class = String.class;
        }

        // attribute name & unit
        DataType dt = new DataType(cinfo.getName(), java_class, null, cinfo.getUnitString(), nullString, null);
        if (cinfo.isArray()) dt.setTypeDesc(DataType.LONG_STRING);

        return dt;
    }

//====================================================================
//  DOM based setters
//====================================================================

    private static ParamInfo paramInfoFromEl(VOElement el) {
        ParamInfo dt = new ParamInfo(null, null);
        populateDataType(dt, el);
        applyIfNotEmpty(el.getAttribute("value"), dt::setValue);
        return dt;
    }

    /**
     * returns the column name for the given field.
     * Although name is a required attribute for FIELD, some VOTable may not provide it.
     * In this case, use ID or name is COLUMN_[IDX] where IDX is the index of the fields.
     * @param el
     * @param colIdx
     * @return
     */
    private static String getCName(VOElement el, int colIdx) {
        if (el == null) return null;
        String name = el.getAttribute("name");
        if (isEmpty(name)) name = el.getAttribute("ID");
        if (isEmpty(name)) name = "COLUMN_" + colIdx;
        return name;
    }


    /**  used by both ParamInfo and DataType  */
    private static void populateDataType(DataType dt, VOElement el) {

        applyIfNotEmpty(el.getAttribute(ID), dt::setID);
        applyIfNotEmpty(el.getAttribute("name"), dt::setKeyName);
        applyIfNotEmpty(el.getAttribute("unit"), dt::setUnits);
        applyIfNotEmpty(el.getAttribute("precision"), v -> dt.setPrecision(makePrecisionStr(v)));
        applyIfNotEmpty(el.getAttribute("width"), v -> dt.setWidth(Integer.parseInt(v)));
        applyIfNotEmpty(el.getAttribute("ref"), dt::setRef);
        applyIfNotEmpty(el.getAttribute("ucd"), dt::setUCD);
        applyIfNotEmpty(el.getAttribute("utype"), dt::setUType);
        applyIfNotEmpty(el.getDescription(), dt::setDesc);
//        callIfNotEmpty(el.getAttribute("arraysize"), dt::setArraySize);
        applyIfNotEmpty(el.getAttribute("datatype"), v -> {
            dt.setDataType(DataType.descToType(v));
            dt.setTypeDesc(v);
        });

        // add all links
        Arrays.stream(el.getChildrenByName("LINK"))
                .forEach(lel -> dt.getLinkInfos().add(linkElementToLinkInfo(lel)));
    }

    private static GroupInfo.RefInfo refInfoFromEl(VOElement el) {
        String refStr = el.getAttribute(REF);
        String ucdStr = el.getAttribute(UCD);
        String utypeStr = el.getAttribute(UTYPE);
        return new GroupInfo.RefInfo(refStr, ucdStr, utypeStr);
    }

//====================================================================
//
//====================================================================

    public static FileAnalysis.Report analyze(File infile, FileAnalysis.ReportType type) throws Exception {

        FileAnalysis.Report report = new FileAnalysis.Report(type, TableUtil.Format.VO_TABLE.name(), infile.length(), infile.getPath());
        VOElement root = makeVOElement(infile, null);
        List<FileAnalysis.Part> parts = describeDocument(root);
        parts.forEach(report::addPart);

        for(int i = 0; i < parts.size(); i++) {
            if (type == FileAnalysis.ReportType.Details) {
                // convert DataGroup headers into Report's details
                DataGroup p = parts.get(i).getDetails();
                IpacTableDef meta = new IpacTableDef();
                meta.setCols(Arrays.asList(p.getDataDefinitions()));
                parts.get(i).getDetails().getAttributeList().forEach(attr -> meta.setAttribute(attr.getKey(), attr.getValue()));
                DataGroup details = TableUtil.getDetails(i, meta);
                applyIfNotEmpty(p.getGroupInfos(), details::setGroupInfos);
                applyIfNotEmpty(p.getLinkInfos(), details::setLinkInfos);
                applyIfNotEmpty(p.getParamInfos(), details::setParamInfos);
                parts.get(i).setDetails(details);
            } else {
                parts.get(i).setDetails(null);      // remove table headers.. everything else is good
            }
        }
        return report;
    }

    /**
     * @param root  the root element of the VOTable
     * @return each Table as a part with details containing DataGroup without data
     */
    private static List<FileAnalysis.Part> describeDocument(VOElement root) {
        List<FileAnalysis.Part> parts = new ArrayList<>();
        Arrays.stream(root.getChildrenByName("RESOURCE"))
                .forEach( res -> {
                    Arrays.stream(res.getChildrenByName("TABLE"))
                            .forEach(table -> {
                                FileAnalysis.Part part = new FileAnalysis.Part(FileAnalysis.Type.Table);
                                part.setIndex(parts.size());
                                DataGroup dg = getTableHeader((TableElement)table);
                                String title = isEmpty(dg.getTitle()) ? "VOTable" : dg.getTitle().trim();
                                part.setDetails(dg);
                                part.setDesc(String.format("%s (%d cols x %s rows)", title, dg.getDataDefinitions().length, dg.size()));
                                parts.add(part);
                            });
                });


        return parts;
    }

    private static DataGroup getTableHeader(TableElement table) {
        String title = table.getAttribute("name");
        // FIELD info  => columns
        VOElement[] fields = table.getChildrenByName("FIELD");
        List<DataType> cols = new ArrayList<>(fields.length);
        for (int i=0; i < fields.length; i++) {
                    DataType dt = new DataType(getCName(fields[i], i), null);
                    populateDataType(dt, fields[i]);
                    cols.add(dt);
            }
        DataGroup dg = new DataGroup(title, cols);

        // PARAMs info
        Arrays.stream(table.getChildrenByName("PARAM"))
                .forEach(el -> dg.getParamInfos().add(paramInfoFromEl(el)));
        // GROUP info
        dg.setGroupInfos(makeGroupInfosFromTable(table));

        // LINK info  => table level
        Arrays.stream(table.getChildrenByName("LINK"))
                .forEach(el -> dg.getLinkInfos().add(linkElementToLinkInfo(el)));

        // INFO     => only takes name/value pairs for now
        Arrays.stream(table.getChildrenByName("INFO"))
                .forEach(el -> {
                    dg.getTableMeta().setAttribute(el.getName(), el.getAttribute("value"));
                });
        if (table.hasAttribute("nrows")) {
            dg.setSize((int) table.getNrows());
        } else {
            // if no nrows attribute, pull in the data and count the rows.
            try {
                dg.setSize((int)  new VOStarTable(table).getRowCount());
            } catch (IOException e) { }     // just ignore it.
        }
        return dg;
    }






}
