/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table.io;

import edu.caltech.ipac.firefly.core.FileAnalysisReport;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.network.HttpServices;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.table.*;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.StringUtils;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.*;
import uk.ac.starlink.votable.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
            VOElement docRoot = getVoTableRoot(location, null);
            List<TableElement> tableAry = getAllTableElements(docRoot);
            List<ResourceInfo> resources = getAllResources(docRoot);
            for (int i = 0; i < tableAry.size(); i++) {
                if (indices == null || indices.length == 0 || indicesList.contains(i)) {
                    TableElement tableEl = tableAry.get(i);
                    DataGroup dg = convertToDataGroup(tableEl, new VOStarTable(tableEl), headerOnly);
                    if (resources != null) dg.setResourceInfos(resources);
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

    private static VOElement getVoTableRoot(String location, StoragePolicy policy) throws IOException {

        String voTablePath, url = null;
        try {
            url = new URL(location).toString();
        } catch (MalformedURLException ex) { /* ok to ignore.  location may not be a URL */ }

        if (url == null) {
            voTablePath = location;
        } else {
            // location is a URL, download it first.
            File tmpFile = File.createTempFile("voreader-", ".xml", QueryUtil.getTempDir());
            try {
                HttpServices.getData( HttpServiceInput.createWithCredential(url), tmpFile);
                voTablePath = tmpFile.getPath();
            } catch (Exception e) {
                tmpFile.delete();
                LOG.error(e);
                throw new IOException("Unable to fetch URL: "+ location + "\n" + e.getMessage(), e);
            }
        }

        try {
            policy = policy == null ? PREFER_MEMORY : policy;
            VOElementFactory voFactory =  new VOElementFactory();
            voFactory.setStoragePolicy(policy);
            return voFactory.makeVOElement(voTablePath);
        }  catch (SAXException | IOException e) {
            throw new IOException("Unable to parse VOTABLE from "+ location + "\n" +
                    e.getMessage(), e);
        }
    }

    // get all <TABLE> from one votable file
    private static List<TableElement> getAllTableElements(VOElement docRoot) throws IOException {

        List<TableElement> tableAry = new ArrayList<>();
        if (docRoot == null) return tableAry;
        Arrays.stream(docRoot.getChildrenByName("RESOURCE"))
                .forEach( res -> {
                    Arrays.stream(res.getChildrenByName("TABLE"))
                            .forEach(table -> tableAry.add((TableElement) table));
                });

        if (tableAry.size() == 0) {
            String error = getQueryStatusError(docRoot);
            if (error != null) {
                throw new IOException(error);
            }
        }
        return tableAry;
    }

    // get all non-table <RESOURCEs> from one votable file
    private static List<ResourceInfo> getAllResources(VOElement docRoot) throws IOException {

        if (docRoot == null) return null;

        return Arrays.stream(docRoot.getChildrenByName("RESOURCE"))
                .filter(res -> res.getChildrenByName("TABLE").length == 0)
                .map( res -> {
                    ResourceInfo ri = new ResourceInfo(res.getID(), res.getName(),
                                            res.getAttribute("type"), res.getAttribute("utype"),
                                            res.getDescription());
                    List<ParamInfo> params = Arrays.stream(res.getChildrenByName("PARAM"))
                            .map(VoTableReader::paramElToParamInfo).collect(Collectors.toList());
                    if (params.size() > 0) ri.setParams(params);

                    List<GroupInfo> groups = Arrays.stream(res.getChildrenByName("GROUP"))
                            .map(VoTableReader::groupElToGroupInfo).collect(Collectors.toList());
                    if (groups.size() > 0) ri.setGroups(groups);

                    return ri;
                }).collect(Collectors.toList());
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
                    dg.getTableMeta().addKeyword(el.getName(), el.getAttribute("value"));
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
            }else if (v.equals("location")) {       // mentioned in appendix A.4(FIELDs as Data Pointers)
                dt.setTypeDesc(v);
            }
        });

        applyIfNotEmpty(el.getAttribute("arraysize"), dt::setArraySize);

        // handle VALUES and OPTIONS
        VOElement values = el.getChildByName("VALUES");
            if (values != null) {
                applyIfNotEmpty(el.getAttribute("null"), dt::setNullString);
                dt.setMaxValue(getChildValue(values, "MAX", "value"));
                dt.setMinValue(getChildValue(values, "MIN", "value"));
                String options = Arrays.stream(values.getChildrenByName("OPTION"))
                        .map(o -> o.getAttribute("value"))
                        .collect(Collectors.joining(","));
                if (!StringUtils.isEmpty(options)) dt.setDataOptions(options);
            }

        // add all links
        Arrays.stream(el.getChildrenByName("LINK"))
                .forEach(lel -> dt.getLinkInfos().add(linkElToLinkInfo(lel)));

        return dt;
    }

    public static String getChildValue(VOElement el, String childTagName, String attribName) {
        VOElement c = el.getChildByName(childTagName);
        return c == null ? null : c.getAttribute(attribName);
    }

    //====================================================================
//  file analysis support
//====================================================================

    public static FileAnalysisReport analyze(File infile, FileAnalysisReport.ReportType type) throws Exception {

        FileAnalysisReport report = new FileAnalysisReport(type, TableUtil.Format.VO_TABLE.name(), infile.length(), infile.getPath());
        List<FileAnalysisReport.Part> parts = tablesToParts(infile);
        parts.forEach(report::addPart);

        for(int i = 0; i < parts.size(); i++) {
            if (type == FileAnalysisReport.ReportType.Details) {
                // convert DataGroup headers into Report's details
                DataGroup p = parts.get(i).getDetails();
                IpacTableDef meta = new IpacTableDef();
                meta.setCols(Arrays.asList(p.getDataDefinitions()));
                parts.get(i).getDetails().getAttributeList().forEach(attr -> meta.setAttribute(attr.getKey(), attr.getValue()));
                DataGroup details = TableUtil.getDetails(i, meta);
                applyIfNotEmpty(p.getGroupInfos(), details::setGroupInfos);
                applyIfNotEmpty(p.getLinkInfos(), details::setLinkInfos);
                applyIfNotEmpty(p.getParamInfos(), details::setParamInfos);
                applyIfNotEmpty(p.getResourceInfos(), details::setResourceInfos);
                parts.get(i).setDetails(details);
            } else {
                parts.get(i).setDetails(null);      // remove table headers.. everything else is good
            }
        }
        return report;
    }

    /**
     * @param infile  input file to analyze
     * @return each Table as a part with details containing DataGroup without data
     */
    private static List<FileAnalysisReport.Part> tablesToParts(File infile) throws Exception {

        VOElement docRoot = getVoTableRoot(infile.getAbsolutePath(), null);
        List<TableElement> tables = getAllTableElements(docRoot);
        List<ResourceInfo> resources = getAllResources(docRoot);

        List<FileAnalysisReport.Part> parts = new ArrayList<>();
        tables.forEach(table -> {
            FileAnalysisReport.Part part = new FileAnalysisReport.Part(FileAnalysisReport.Type.Table);
            part.setIndex(parts.size());
            part.setFileLocationIndex(parts.size());
            DataGroup dg = getTableHeader(table);
            if (resources != null) dg.setResourceInfos(resources);
            String title = isEmpty(dg.getTitle()) ? "VOTable" : dg.getTitle().trim();
            part.setDetails(dg);
            part.setDesc(String.format("%s (%d cols x %s rows)", title, dg.getDataDefinitions().length, dg.size()));
            part.setTotalTableRows(dg.size());
            parts.add(part);
        });

        return parts;
    }

}

