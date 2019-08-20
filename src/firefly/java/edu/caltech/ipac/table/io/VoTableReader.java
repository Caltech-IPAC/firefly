/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table.io;

import edu.caltech.ipac.firefly.core.FileAnalysis;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.*;
import edu.caltech.ipac.util.CollectionUtil;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.*;
import uk.ac.starlink.votable.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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

    private static final String ID = "ID";
    private static final String REF = "ref";
    private static final String UCD = "ucd";
    private static final String UTYPE = "utype";
    private static final String NAME = "name";


    private static final Pattern HMS_UCD_PATTERN =
            Pattern.compile( "POS_EQ_RA.*|pos\\.eq\\.ra.*",
                    Pattern.CASE_INSENSITIVE );
    private static final Pattern DMS_UCD_PATTERN =
            Pattern.compile( "POS_EQ_DEC.*|pos\\.eq\\.dec.*",
                    Pattern.CASE_INSENSITIVE );

    private static Logger.LoggerImpl LOG = Logger.getLogger();

    /**
     * returns an array of DataGroup from a vo table.
     * @param location  location of the vo table data source using automatic format detection.  Can be file or url.
     * @return an array of DataGroup objects
     */
    public static DataGroup[] voToDataGroups(String location, int ...indices) throws IOException {
        return voToDataGroups(location, false, indices);
    }

    /**
     * returns an array of DataGroup from a votable file.
     * @param location  location of the votable data source using automatic format detection.  Can be file or url.
     * @param headerOnly  if true, returns only the headers, not the data.
     * @param indices   only return table from this list of indices
     * @return an array of DataGroup object
     */
    public static DataGroup[] voToDataGroups(String location, boolean headerOnly, int ...indices) throws IOException {
        List<DataGroup> groups = new ArrayList<>();

        try {
            List<Integer> indicesList = indices == null ? null : CollectionUtil.asList(indices);
            List<TableElement> tableAry = getAllTableElements( location, null);
            for (int i = 0; i < tableAry.size(); i++) {
                if (indices == null || indices.length == 0 || indicesList.contains(i)) {
                    TableElement tableEl = tableAry.get(i);
                    DataGroup dg = convertToDataGroup(tableEl, new VOStarTable(tableEl), headerOnly);
                    groups.add(dg);
                }
            }
        } catch (IOException e) {
            LOG.error(e);
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
            LOG.error(e);
            throw new DataAccessException("unable to parse " + location + "\n"+
                    e.getMessage(), e);
        }
    }


//====================================================================
//
//====================================================================

    private static String getQueryStatusError(VOElement top) {
        String error = null;
        // check for errors: section 4.4 of http://www.ivoa.net/documents/DALI/20170517/REC-DALI-1.1.html
        VOElement[] resources = top.getChildrenByName( "RESOURCE" );
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

    // get all <TABLE> from one votable file
    private static List<TableElement> getAllTableElements(String location, StoragePolicy policy) throws IOException {

        VOElement root;
        try {
            policy = policy == null ? PREFER_MEMORY : policy;
            VOElementFactory voFactory =  new VOElementFactory();
            voFactory.setStoragePolicy(policy);
            root = voFactory.makeVOElement(location);
        }  catch (SAXException | IOException e) {
            throw new IOException("unable to parse "+ location + "\n" +
                    e.getMessage(), e);
        }

        List<TableElement> tableAry = new ArrayList<>();
        if (root != null) {
            Arrays.stream(root.getChildrenByName("RESOURCE"))
                    .forEach( res -> {
                        Arrays.stream(res.getChildrenByName("TABLE"))
                                .forEach(table -> tableAry.add((TableElement) table));
                    });

            if (tableAry.size() == 0) {
                String error = getQueryStatusError(root);
                if (error != null) {
                    throw new IOException(error);
                }
            }
        }

        return tableAry;
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
        int precision = 8;

        DataGroup dg = getTableHeader(tableEl);
        List<DataType> cols = Arrays.asList(dg.getDataDefinitions());

        // post-process to handle custom logic
        DataType raCol = cols.stream().filter(dt -> HMS_UCD_PATTERN.matcher(String.valueOf(dt.getUCD())).matches())
                .findFirst().orElse(null);
        DataType decCol = cols.stream().filter(dt -> DMS_UCD_PATTERN.matcher(String.valueOf(dt.getUCD())).matches())
                .findFirst().orElse(null);
        if (raCol != null && decCol != null) {
            dg.addAttribute("POS_EQ_RA_MAIN", raCol.getKeyName());
            dg.addAttribute("POS_EQ_DEC_MAIN", decCol.getKeyName());
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

                        if ((val instanceof Double && Double.isNaN((Double) val)) ||
                                (val instanceof Float && Float.isNaN((Float) val))    )    {
                            val = null;
                        }
                        row.setDataElement(dtype, val);
                        if (dtype.getPrecision() == null) {
                            String sval = table.getColumnInfo(i).formatValue(val, Integer.MAX_VALUE);
                            IpacTableUtil.guessFormatInfo(dtype, sval, precision);// precision min 8 can come from VOTable attribute 'precision' later on.
                        }
                    }
                    dg.add(row);
                }
            }
        } catch (IOException e) {
            LOG.error(e);
        }
        dg.trimToSize();
        return dg;
    }

    private static DataGroup getTableHeader(TableElement table) {

        // FIELD info  => columns
        VOElement[] fields = table.getChildrenByName("FIELD");
        List<DataType> cols = new ArrayList<>(fields.length);
        for (int i=0; i < fields.length; i++) {
            DataType dt = fieldElToDataType(null, fields[i], i, cols);
            cols.add(dt);
        }
        String name = table.getAttribute(NAME);
        DataGroup dg = new DataGroup(name, cols);

        // attribute ID, ref, ucd, utype from TABLE
        applyIfNotEmpty(name, v -> dg.getTableMeta().addKeyword(TableMeta.NAME, v));
        applyIfNotEmpty(table.getAttribute(ID), v -> dg.getTableMeta().addKeyword(TableMeta.ID, v));
        applyIfNotEmpty(table.getAttribute(REF), v -> dg.getTableMeta().addKeyword(TableMeta.REF, v));
        applyIfNotEmpty(table.getAttribute(UCD), v -> dg.getTableMeta().addKeyword(TableMeta.UCD, v));
        applyIfNotEmpty(table.getAttribute(UTYPE), v -> dg.getTableMeta().addKeyword(TableMeta.UTYPE, v));
        applyIfNotEmpty(table.getDescription(), v -> dg.getTableMeta().addKeyword(TableMeta.DESC, v.replace("\n", " ")));

        // PARAMs info
        Arrays.stream(table.getChildrenByName("PARAM"))
                .forEach(el -> dg.getParamInfos().add(paramElToParamInfo(el)));

        // GROUP info
        Arrays.stream(table.getChildrenByName("GROUP"))
                .forEach(el -> dg.getGroupInfos().add(groupElToGroupInfo(el)));

        // LINK info  => table level
        Arrays.stream(table.getChildrenByName("LINK"))
                .forEach(el -> dg.getLinkInfos().add(linkElToLinkInfo(el)));

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


    // precision: "2" => "F2", "F2"=> "F2", "E3" => "E3"
    private static String makePrecisionStr(String precisionStr) {
        return (precisionStr.matches("^[1-9][0-9]*$")) ? ("F"+precisionStr) : precisionStr;
    }

    /**
     * returns the column name for the given field.
     * Although name is a required attribute for FIELD, some VOTable may not provide it.
     * When name is not given, use ID if exists. Otherwise, use COLUMN_${IDX} where IDX is the index of the fields.
     *
     * In VOTable section 3.2, name does not need to be unique, although it is recommended that name of a FIELD be unique for a TABLE.
     * In Firefly, DataType.name has to be unique.  Therefore, if name is not unique, we will append _${IDX} to it.
     * @param el
     * @param colIdx
     * @return
     */
    private static String getCName(VOElement el, int colIdx, List<DataType> cols) {
        if (el == null) return null;
        String name = el.getAttribute("name");
        if (isEmpty(name)) name = el.getAttribute("ID");
        if (isEmpty(name)) name = "COLUMN_" + colIdx;

        name = name.replace("\"","");

        if (cols != null) {
            String finalName = name;
            if (CollectionUtil.findFirst(cols, dt -> Objects.equals(dt.getKeyName(), finalName)) != null) {
                // name is not unique
                name = name + "_" + colIdx;
            }
        }
        return name;
    }

//====================================================================
//  private conversion functions
//====================================================================

    private static GroupInfo groupElToGroupInfo(VOElement group) {

        String name = group.getName();
        String desc = group.getDescription();

        GroupInfo gObj = new GroupInfo(name, desc);
        applyIfNotEmpty(group.getAttribute(ID), gObj::setID);

        // add FIELDref
        Arrays.stream(group.getChildrenByName("FIELDref"))
                .forEach(pEl -> gObj.getColumnRefs().add(refElToRefInfo(pEl)));

        // add PARAMrefs
        Arrays.stream(group.getChildrenByName("PARAMref"))
                .forEach(pEl -> gObj.getParamRefs().add(refElToRefInfo(pEl)));

        // add PARAMs
        Arrays.stream(group.getChildrenByName("PARAM"))
            .forEach(pEl -> gObj.getParamInfos().add(paramElToParamInfo(pEl)));

        return gObj;
    }

    // convert LinkElement to LinkInfo
    private static LinkInfo linkElToLinkInfo(VOElement el) {
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

    private static ParamInfo paramElToParamInfo(VOElement el) {
        ParamInfo dt = (ParamInfo) fieldElToDataType(new ParamInfo(), el,0, null);
        applyIfNotEmpty(el.getAttribute("value"), dt::setValue);
        return dt;
    }

    private static GroupInfo.RefInfo refElToRefInfo(VOElement el) {
        String refStr = el.getAttribute(REF);
        String ucdStr = el.getAttribute(UCD);
        String utypeStr = el.getAttribute(UTYPE);
        return new GroupInfo.RefInfo(refStr, ucdStr, utypeStr);
    }

    /**  used by both ParamInfo and DataType  */
    private static DataType fieldElToDataType(DataType col, VOElement el, int idx, List<DataType> cols) {

        DataType dt = col == null ? new DataType(null, null) : col;
        String name = getCName(el, idx, cols);
        dt.setKeyName(name);

        applyIfNotEmpty(el.getAttribute(ID), dt::setID);
        applyIfNotEmpty(el.getAttribute("unit"), dt::setUnits);
        applyIfNotEmpty(el.getAttribute("precision"), v -> dt.setPrecision(makePrecisionStr(v)));
        applyIfNotEmpty(el.getAttribute("width"), v -> dt.setWidth(Integer.parseInt(v)));
        applyIfNotEmpty(el.getAttribute("ref"), dt::setRef);
        applyIfNotEmpty(el.getAttribute("ucd"), dt::setUCD);
        applyIfNotEmpty(el.getAttribute("utype"), dt::setUType);
        applyIfNotEmpty(el.getDescription(), dt::setDesc);
        applyIfNotEmpty(el.getAttribute("datatype"), v -> {
            dt.setTypeDesc(v);
            dt.setDataType(DataType.descToType(v));
        });
        applyIfNotEmpty(el.getAttribute("type"), v -> {
            if (v.equals("hidden")) {       // mentioned in appendix A.1(LINK substitutions)
                dt.setVisibility(DataType.Visibility.hidden);
            }
        });

        if (dt.getDataType() != String.class) {
            // we will ignore multi-dimensional char arrays for now
            applyIfNotEmpty(el.getAttribute("arraysize"), dt::setArraySize);
        }

        // add all links
        Arrays.stream(el.getChildrenByName("LINK"))
                .forEach(lel -> dt.getLinkInfos().add(linkElToLinkInfo(lel)));
        return dt;
    }




//====================================================================
//  file analysis support
//====================================================================

    public static FileAnalysis.Report analyze(File infile, FileAnalysis.ReportType type) throws Exception {

        FileAnalysis.Report report = new FileAnalysis.Report(type, TableUtil.Format.VO_TABLE.name(), infile.length(), infile.getPath());
        List<TableElement> tables = getAllTableElements(infile.getAbsolutePath(), null);
        List<FileAnalysis.Part> parts = tablesToParts(tables);
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
     * @param tables  a list of table element
     * @return each Table as a part with details containing DataGroup without data
     */
    private static List<FileAnalysis.Part> tablesToParts(List<TableElement> tables) {
        List<FileAnalysis.Part> parts = new ArrayList<>();
        tables.forEach(table -> {
            FileAnalysis.Part part = new FileAnalysis.Part(FileAnalysis.Type.Table);
            part.setIndex(parts.size());
            DataGroup dg = getTableHeader((TableElement)table);
            String title = isEmpty(dg.getTitle()) ? "VOTable" : dg.getTitle().trim();
            part.setDetails(dg);
            part.setDesc(String.format("%s (%d cols x %s rows)", title, dg.getDataDefinitions().length, dg.size()));
            parts.add(part);
        });

        return parts;
    }

}

