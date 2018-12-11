/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table.io;

import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.table.*;
import edu.caltech.ipac.util.FitsHDUUtil;
import edu.caltech.ipac.util.StringUtils;
import org.json.simple.JSONObject;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.*;
import uk.ac.starlink.votable.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static edu.caltech.ipac.util.StringUtils.applyIfNotEmpty;
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

    public static final String ID = "ID";
    public static final String REF = "ref";
    public static final String UCD = "ucd";
    public static final String UTYPE = "utype";
    public static final String DESC = "desc";

    private static String getElementAttribute(VOElement element, String attName) {
        return element.hasAttribute(attName) ? element.getAttribute(attName) : null;
    }


    // root VOElement for a votable file
    private static VOElement getVOElementFromVOTable(String location, StoragePolicy policy) throws DataAccessException {
        try {
            policy = policy == null ? PREFER_MEMORY : policy;
            VOElementFactory voFactory =  new VOElementFactory();
            voFactory.setStoragePolicy(policy);
            return voFactory.makeVOElement(location);
        }  catch (SAXException |IOException e) {
            e.printStackTrace();
            throw new DataAccessException("unable to parse "+location+"\n"+
                    e.getMessage(), e);
        }
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
    private static VOElement[] getInfosFromTable(TableElement tableEl) {
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
            allParams.add(params);
        }
        return allParams;
    }

    // get <INFO>s under <TABLE> as a list of DescribeValue
    private static List<DescribedValue> getInfosFromTable(TableElement tableEl, StarTable table) {

        VOElement[] infoEl = getInfosFromTable(tableEl);
        List<DescribedValue> infosAry = new ArrayList<>();

        for (VOElement param : infoEl) {
            String name = getElementAttribute(param, "name");
            if (name != null) {
                infosAry.add(table.getParameterByName(name));
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
                dt.setEnumVals(optionsToStr(options));
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
                // check for errors: section 4.4 of http://www.ivoa.net/documents/DALI/20170517/REC-DALI-1.1.html
                VOElement[] resources = getResourceChildren(top);
                for (VOElement r : resources) {
                    if ("results".equals(r.getAttribute("type"))) {
                        VOElement[] infos = r.getChildrenByName("INFO");
                        for (VOElement info : infos) {
                            if ("QUERY_STATUS".equals(info.getName()) && "ERROR".equals(info.getAttribute("value"))) {
                                throw new DataAccessException(info.getTextContent());
                            }
                        }
                    }
                }
            }
        }

        return tableAry;
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
     *
     * Compose a summary table containing analysis result of the VOTable file
     * the summary table includes:
     *  columns: defined in FitsHDUUtil.java
     *  rows:    contain info for each TABLE in the votable file.
     *  metadata: contain the header info for each TABLE in the votable file, the header info includes key/value from
     *            name attribute of the TABLE element and children elements DESCRIPTION, INFO, PARAM, LINK and GROUP
     *            of the TABLE element
     *
     * @param voTableFile path of votable file
     * @return summary table of votable file
     */
    public static DataGroup voHeaderToDataGroup(String voTableFile)  {
        List<DataType> cols = new ArrayList<>();

        for ( MetaInfo meta : MetaInfo.values()) {    // index, name, row, column
            DataType dt = new DataType(meta.getKey(), meta.getTitle(), meta.getMetaClass());
            dt.setDesc(meta.getDescription());
            cols.add(dt);
        }
        DataGroup dg = new DataGroup("votable", cols);
        String invalidMsg = "invalid votable file";

        try {
            /*
            StarTableFactory stFactory = new StarTableFactory();
            TableSequence tseq = stFactory.makeStarTables(voTableFile, null);
            */

            // parse the votable file using Starlink VOTable aware DOM parser
            // by setting StoragePolicy.DISCARD as the storage policy (throw away the rows), note: # of rows is lost
            //List<TableElement> tableAry = getTableElementsFromFile( voTableFile, StoragePolicy.DISCARD);

            List<TableElement> tableAry = getTableElementsFromFile( voTableFile, null);
            int index = 0;
            List<JSONObject> headerColumns = FitsHDUUtil.createHeaderTableColumns(true);

            for (TableElement tableEl : tableAry) {
                StarTable table = new VOStarTable(tableEl);
                String title = table.getName();
                Long rowNo = table.getRowCount();
                Integer columnNo = table.getColumnCount();
                String tableName = String.format("%d cols x %d rows", columnNo, rowNo) ;

                List<List<String>> headerRows = new ArrayList<>();
                //List<DescribedValue> tblParams = table.getParameters();
                int rowIdx = 0;

                List<String> rowStats = new ArrayList<>();     // first key/value/comment for header
                rowStats.add(Integer.toString(rowIdx++));
                rowStats.add("Name");
                rowStats.add(title);
                rowStats.add("Table name");
                headerRows.add(rowStats);

                String desc = tableEl.getDescription();

                // DESCRIPTION element under TABLE if there is
                if (desc != null) {
                    rowStats = new ArrayList<>();     // first key/value/comment for header
                    rowStats.add(Integer.toString(rowIdx++));
                    rowStats.add("Description");
                    rowStats.add(desc);
                    rowStats.add("Table description");
                    headerRows.add(rowStats);
                }

                // add INFO name/value/comment to the header
                List<DescribedValue> tblInfos = getInfosFromTable(tableEl, table);
                for (DescribedValue dv : tblInfos) {
                    ValueInfo vInfo = dv.getInfo();

                    rowStats = new ArrayList<>();
                    rowStats.add(Integer.toString(rowIdx++));
                    rowStats.add(vInfo.getName());
                    rowStats.add(dv.getValueAsString(Integer.MAX_VALUE));

                    desc = vInfo.getDescription();
                    if (desc == null) {
                        desc = "";
                    }
                    rowStats.add(desc);

                    headerRows.add(rowStats);
                }

                // add PARAM name/value/comment to the header
                List<ParamInfo> tblParams = makeParamsFromTable(tableEl, table);
                for (ParamInfo param : tblParams) {
                    rowStats = new ArrayList<>();

                    rowStats.add(Integer.toString(rowIdx++));
                    rowStats.add(param.getKeyName());
                    rowStats.add(param.getValue());
                    rowStats.add("PARAM in TABLE");
                    headerRows.add(rowStats);
                }

                // add LINK link/href/comment to the header
                List<LinkInfo> links = makeLinkInfosFromTable(tableEl);
                for (LinkInfo link : links) {
                    rowStats = new ArrayList<>();

                    rowStats.add(Integer.toString(rowIdx++));
                    rowStats.add("LINK");
                    rowStats.add(link.toString());
                    rowStats.add("LINK in TABLE");
                    headerRows.add(rowStats);
                }

                // get Group Group/refs/comment to the header
                List<GroupInfo> groups = makeGroupInfosFromTable(tableEl);
                for (GroupInfo group : groups) {
                    rowStats = new ArrayList<>();

                    rowStats.add(Integer.toString(rowIdx++));
                    rowStats.add("GROUP");
                    rowStats.add(group.toString());
                    rowStats.add("GROUP in TABLE");
                    headerRows.add(rowStats);
                }

                JSONObject voParamsHeader = FitsHDUUtil.createHeaderTable(headerColumns, headerRows,
                                               "Information of table with index " + index);


                DataObject row = new DataObject(dg);
                row.setDataElement(cols.get(0), index);
                row.setDataElement(cols.get(1),tableName );
                row.setDataElement(cols.get(2), "Table");
                dg.add(row);
                dg.addAttribute(Integer.toString(index), voParamsHeader.toJSONString());
                index++;
            }

            if (index == 0) {
                throw new IOException(invalidMsg);
            } else {
                String title = "VOTable" +
                        "-- The following left table shows the file summary and the right table shows the information of " +
                        "the table which is highlighted in the file summary.";
                dg.setTitle(title);
            }
        } catch (IOException | DataAccessException e) {
            dg.setTitle(invalidMsg);
            e.printStackTrace();
        }

        return dg;
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

        // collect FIELD from TABLE
        // collect attributes of <FIELD> including name, datatype, unit, precision, width, ref, ID, ucd, utype
        // collect children elements including <DESCRIPTION>, <LINK>, <VALUES>
        for (int i = 0; i < table.getColumnCount(); i++) {
            ColumnInfo cinfo = table.getColumnInfo(i);

            // attribute datatype, if the column is with data in array style, set the datatype to be "string" type
            Class clz = cinfo.isArray() ? String.class : cinfo.getContentClass();

            // attribute name & unit
            DataType dt = new DataType(cinfo.getName(), clz, null, cinfo.getUnitString(), null, null);

            if (cinfo.isArray()) dt.setTypeDesc(DataType.LONG_STRING);

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
            if (cinfo.getAuxDatum(VOStarTable.UTYPE_INFO) != null) {
                dt.setRef(cinfo.getAuxDatum(VOStarTable.UTYPE_INFO).getValue().toString());
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
        List<DescribedValue> dvAry = (tableEl == null) ? table.getParameters()
                                                       : getInfosFromTable(tableEl, table);
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

            // child element PARAM, GROUP, LINK for TABLE
            dg.setParamInfos(makeParamsFromTable(tableEl, table));
            dg.setGroupInfos(makeGroupInfosFromTable(tableEl));
            dg.setLinkInfos(makeLinkInfosFromTable(tableEl));

            // child element DESCRIPTION
            String tDesc = tableEl.getDescription();
            if (tDesc != null) {
                dg.addAttribute(TableMeta.DESC, tDesc.replace("\n", " "));
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
                            if (val instanceof Double && Double.isNaN((Double) val)) {
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


//====================================================================
//  DOM based setters
//====================================================================

    private static ParamInfo paramInfoFromEl(VOElement el) {
        ParamInfo dt = new ParamInfo(null, null);
        populateDataType(dt, el);
        applyIfNotEmpty(el.getAttribute("value"), dt::setValue);
        return dt;
    }

    private static DataType dataTypeFromEl(VOElement el) {
        DataType dt = new DataType(null, null);
        populateDataType(dt, el);
        return dt;
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

    public static void main(String args[]) {

        File inf = new File(args[0]);
        try {
            DataGroup[] groups = voToDataGroups(inf.getAbsolutePath(), false);
            if (groups != null) {
                for (DataGroup dg : groups) {
                    try {
                        IpacTableWriter.save(new File(inf.getParent(), inf.getName() + "-" + dg.getTitle()), dg);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException | DataAccessException e) {
            e.printStackTrace();
        }
    }
}
