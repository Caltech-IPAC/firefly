/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table.io;

import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.IpacTableUtil;
import edu.caltech.ipac.table.GroupInfo;
import edu.caltech.ipac.table.LinkInfo;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.util.FitsHDUUtil;
import uk.ac.starlink.table.*;
import uk.ac.starlink.votable.VOStarTable;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.votable.LinkElement;
import uk.ac.starlink.votable.FieldElement;
import uk.ac.starlink.votable.ValuesElement;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.Arrays;
import org.xml.sax.SAXException;
import java.net.URL;
import java.net.MalformedURLException;
import java.lang.Exception;

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
        return voToDataGroups(location,false);
    }

    /**
     * returns an array of DataGroup from a votable file.
     * @param location  location of the votable data source using automatic format detection.  Can be file or url.
     * @param headerOnly  if true, returns only the headers, not the data.
     * @return an array of DataGroup object
     */
    public static DataGroup[] voToDataGroups(String location, boolean headerOnly) throws IOException {
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
    private static VOElement getVOElementFromVOTable(String location, StoragePolicy policy) {
        try {
            VOElementFactory voFactory =  new VOElementFactory();
            if (policy != null) {
                voFactory.setStoragePolicy(policy);
            }
           return voFactory.makeVOElement(location);
        }  catch (SAXException|IOException e) {
            e.printStackTrace();
        }
        return null;
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
    private static List<GroupInfo> makeGroupInfosFromTable(TableElement tableEl, DataGroup dg) {
        VOElement[] groupAry = getGroupsFromTable(tableEl);
        List<GroupInfo> groupObjAry = new ArrayList<>();

        for (VOElement group : groupAry) {
            String name = group.getName();
            String desc = group.getDescription();
            VOElement[] fieldRef = group.getChildrenByName("FIELDref");

            GroupInfo gObj = new GroupInfo(name, desc);
            gObj.setID(getElementAttribute(group, ID));

            for (VOElement fRef : fieldRef) {
                String refStr = getElementAttribute(fRef, REF);
                String ucdStr = getElementAttribute(fRef, UCD);
                String utypeStr = getElementAttribute(fRef, UTYPE);
                GroupInfo.FieldRef ref = new GroupInfo.FieldRef(refStr, ucdStr, utypeStr);

                gObj.getFieldRefs().add(ref);
            }


            if (dg != null) {
                List<GroupInfo> dgGroupInfos = dg.getGroupInfos();

                if (dgGroupInfos != null) {
                    dgGroupInfos.add(gObj);
                }
            }
            groupObjAry.add(gObj);

        }
        return groupObjAry;
    }

    // convert LinkElement to LinkInfo
    private static LinkInfo linkElementToLinkInfo(VOElement voEl) {
        LinkElement lnkEl = (LinkElement) voEl;
        String ContentRole = "content-role";
        String ContentType = "content-type";

        try {
            String title = lnkEl.getHandle();
            URL href = lnkEl.getHref();
            String linkRef = (href != null) ? href.toString() : null;
            LinkInfo linkObj = new LinkInfo(linkRef, title);

            linkObj.setRole(getElementAttribute(voEl, ContentRole));
            linkObj.setType(getElementAttribute(voEl, ContentType));
            linkObj.setID(getElementAttribute(voEl, ID));
            return linkObj;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }

    }

    // convert <LINK>s under <TABLE> to a list of LinkInfo and add it to the associated table (a DataGroup object)
    private static List<LinkInfo> makeLinkInfosFromTable(TableElement tableEl, DataGroup dg) {
        VOElement[] linkAry = getLinksFromTable(tableEl);
        List<LinkInfo> linkObjAry = new ArrayList<>();

        for (VOElement link : linkAry) {
            LinkInfo linkObj = linkElementToLinkInfo(link);

            if (linkObj != null) {
                if (dg != null) {
                    List<LinkInfo> dgLinkInfos = dg.getLinkInfos();

                    if (dgLinkInfos != null) {
                        dgLinkInfos.add(linkObj);
                    }
                }
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
    private static List<DataType> makeParamsFromTable(TableElement tableEl, StarTable table, DataGroup dg) {
        VOElement[] paramsEl = getParamsFromTable(tableEl);
        List<DataType> allParams = new ArrayList<>();

        for (VOElement param : paramsEl) {
            String name = getElementAttribute(param, "name");
            DescribedValue dv = table.getParameterByName(name);
            if (dv == null) continue;

            ValueInfo vInfo = dv.getInfo();
            Class clz = vInfo.isArray() ? String.class : vInfo.getContentClass();

            // create Datatype
            DataType dt = new DataType(name, clz, null, vInfo.getUnitString(), null, null);

            // set precision
            String precisionStr = getElementAttribute(param, "precision");
            String widthStr = getElementAttribute(param, "width");

            if (precisionStr != null) {
                precisionStr = makePrecisionStr(precisionStr);
                dt.setPrecision(precisionStr);
            }

            // set width
            if (widthStr != null) {
                dt.setWidth(Integer.parseInt(widthStr));
            }

            // set ucd, utype and value to DataType
            dt.setUCD(getElementAttribute(param, "ucd"));
            dt.setUType(getElementAttribute(param, "utype"));
            dt.setID(getElementAttribute(param, "ID"));
            dt.setValue(getElementAttribute(param, "value"));
            allParams.add(dt);

            if (dg != null) {
                List<DataType> staticCols = dg.getParams();
                if (staticCols != null) {
                    staticCols.add(dt);
                }
            }
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
            if (f.getName().equals(name)) {
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

    // add info of <VALUES> under a <FIELD> to the associated column (a DataType object)
    // the added info includes: minimum/maximum values, options, or null string
    private static void getValuesFromField(TableElement tableEl, DataType dt) {
        FieldElement fieldEle = getFieldElementByName(tableEl, dt.getKeyName());

        ValuesElement values = fieldEle.getActualValues();

        if (values != null) {
            dt.setNullString(values.getNull());
            dt.setMaxValue(values.getMaximum());
            dt.setMinValue(values.getMinimum());
            dt.setOptions(values.getOptions());
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
    private static List<TableElement> getTableElementsFromFile(String location, StoragePolicy policy) {
        VOElement top = getVOElementFromVOTable(location, policy);
        List<TableElement> tableAry = new ArrayList<>();

        if (top != null) {
            getTableElements(top, tableAry);
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
    public static DataGroup voHeaderToDataGroup(String voTableFile) {
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
                List<DataType> tblParams = makeParamsFromTable(tableEl, table, null);
                for (DataType param : tblParams) {
                    rowStats = new ArrayList<>();

                    rowStats.add(Integer.toString(rowIdx++));
                    rowStats.add(param.getKeyName());
                    rowStats.add(param.getValue());
                    rowStats.add("PARAM in TABLE");
                    headerRows.add(rowStats);
                }

                // add LINK link/href/comment to the header
                List<LinkInfo> links = makeLinkInfosFromTable(tableEl, null);
                for (LinkInfo link : links) {
                    rowStats = new ArrayList<>();

                    rowStats.add(Integer.toString(rowIdx++));
                    rowStats.add(link.getTitle());
                    rowStats.add(link.toString());
                    rowStats.add("LINK in TABLE");
                    headerRows.add(rowStats);
                }

                // get Group Group/refs/comment to the header
                List<GroupInfo> groups = makeGroupInfosFromTable(tableEl, null);
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
        } catch (IOException e) {
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

            // attribute precision
            if(cinfo.getAuxDatum(VOStarTable.PRECISION_INFO)!=null){
                try{
                    DescribedValue pDV  = cinfo.getAuxDatum(VOStarTable.PRECISION_INFO);

                    if (pDV != null) {
                        precisionStr = pDV.toString();
                        precision = Integer.parseInt(precisionStr);
                        precisionStr = makePrecisionStr(precisionStr);
                        dt.setPrecision(precisionStr);
                    }
                } catch (NumberFormatException e){
                    // problem with VOTable vinfo precision: should be numeric - keep default min precision
                }
            }

            // attribute width
            if (cinfo.getAuxDatum(VOStarTable.WIDTH_INFO) != null) {
                dt.setWidth(Integer.parseInt(cinfo.getAuxDatum(VOStarTable.WIDTH_INFO).toString()));
            }

            // attribute ref
            if (cinfo.getAuxDatum(VOStarTable.REF_INFO) != null) {
                dt.setRef(cinfo.getAuxDatum(VOStarTable.REF_INFO).toString());
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
                dt.setRef(cinfo.getAuxDatum(VOStarTable.UTYPE_INFO).toString());
            }

            // attribute ID
            if (cinfo.getAuxDatum(VOStarTable.ID_INFO) != null) {
                dt.setID(cinfo.getAuxDatum(VOStarTable.ID_INFO).toString());
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
            //dg.addAttribute(dv.getInfo().getName(), dv.getValueAsString(50).replace("\n", " "));  // take the original string?
            dg.addAttribute(dv.getInfo().getName(), dv.getValueAsString(Integer.MAX_VALUE).replace("\n", " "));
        }


        if (raCol != null && decCol != null) {
            dg.addAttribute("POS_EQ_RA_MAIN", raCol);
            dg.addAttribute("POS_EQ_DEC_MAIN", decCol);
        }

        if (tableEl != null) {
            // attribute ID, ref, ucd, utype from TABLE
            dg.addAttribute(TableMeta.ID,  getElementAttribute(tableEl, ID));
            dg.addAttribute(TableMeta.REF, getElementAttribute(tableEl, REF));
            dg.addAttribute(TableMeta.UCD, getElementAttribute(tableEl, UCD));
            dg.addAttribute(TableMeta.UTYPE, getElementAttribute(tableEl, UTYPE));

            // child element PARAM, GROUP, LINK for TABLE
            makeParamsFromTable(tableEl, table, dg);
            makeGroupInfosFromTable(tableEl, dg);
            makeLinkInfosFromTable(tableEl, dg);

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
                while (rs.next()) {
                    DataObject row = new DataObject(dg);
                    for(int i = 0; i < cols.size(); i++) {
                        DataType dtype = cols.get(i);
                        Object val = rs.getCell(i);
                        String sval = table.getColumnInfo(i).formatValue(val, Integer.MAX_VALUE);
                        if (dtype.getDataType().isAssignableFrom(String.class) && !(val instanceof String)) {
                            row.setDataElement(dtype, sval);   // array value
                        } else {
                            if (val instanceof Double && Double.isNaN((Double) val)) {
                                val = null;
                            }
                            row.setDataElement(dtype, val);
                        }
                        IpacTableUtil.guessFormatInfo(dtype, sval, precision);// precision min 8 can come from VOTable attribute 'precision' later on.
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
