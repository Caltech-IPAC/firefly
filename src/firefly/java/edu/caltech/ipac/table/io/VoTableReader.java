/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table.io;

import edu.caltech.ipac.firefly.core.FileAnalysisReport;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.network.HttpServices;
import edu.caltech.ipac.firefly.server.persistence.MultiSpectrumProcessor;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.GroupInfo;
import edu.caltech.ipac.table.IpacTableDef;
import edu.caltech.ipac.table.LinkInfo;
import edu.caltech.ipac.table.ParamInfo;
import edu.caltech.ipac.table.ResourceInfo;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.table.TableUtil;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.ByteStore;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.storage.AdaptiveByteStore;
import uk.ac.starlink.table.storage.ByteStoreStoragePolicy;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;
import uk.ac.starlink.votable.VOStarTable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static edu.caltech.ipac.table.TableUtil.getAliasName;
import static edu.caltech.ipac.util.StringUtils.applyIfNotEmpty;
import static edu.caltech.ipac.util.StringUtils.isEmpty;
import static uk.ac.starlink.table.StoragePolicy.*;

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

    private static final String MULTI_SPEC_UTYPE_LOWER= "ipac:multispectrum";


    private static final Pattern HMS_UCD_PATTERN =
            Pattern.compile( "POS_EQ_RA.*|pos\\.eq\\.ra.*",
                    Pattern.CASE_INSENSITIVE );
    private static final Pattern DMS_UCD_PATTERN =
            Pattern.compile( "POS_EQ_DEC.*|pos\\.eq\\.dec.*",
                    Pattern.CASE_INSENSITIVE );

    private static final Logger.LoggerImpl LOG = Logger.getLogger();

    private static final StoragePolicy FIREFLY = new ByteStoreStoragePolicy() {
        protected ByteStore attemptMakeByteStore() throws IOException {
            return new AdaptiveByteStore(Math.min(AdaptiveByteStore.getDefaultLimit(), 64*1024*1024));      // default with a max of 64mb
        }
        public String toString() {
            return "StoragePolicy.FIREFLY";
        }
    };

    /**
     * returns an array of DataGroup from a vo table.
     * @param location  location of the vo table data source using automatic format detection.  Can be file or url.
     * @return an array of DataGroup objects
     */
    public static DataGroup[] voToDataGroups(String location, int ...indices) throws IOException {
        return voToDataGroups(location, false, indices);
    }

    /**
     * Similar to VoTableReader#voToDataGroups(java.lang.String, int...), but it also applies additional transformations
     * to the returned DataGroup(s) based on the hints provided by the given request.
     */
    public static DataGroup[] voToDataGroups(String location, TableServerRequest request, int ...indices) throws IOException {
        TableParseHandler.Memory handler = new TableParseHandler.Memory(false, SpectrumMetaInspector.hasSpectrumHint(request));
        VOElement docRoot = getVoTableRoot(location, null);
        parse(handler, docRoot, indices);
        return handler.getAllTable();
    }

    /**
     * returns an array of DataGroup from a votable file.
     * @param location  location of the votable data source using automatic format detection.  Can be file or url.
     * @param headerOnly  if true, returns only the headers, not the data.
     * @param indices   only return table from this list of indices
     * @return an array of DataGroup object
     */
    public static DataGroup[] voToDataGroups(String location,
                                             boolean headerOnly,
                                             int ...indices) throws IOException {
        VOElement docRoot = getVoTableRoot(location, null);
        return voToDataGroups(docRoot, headerOnly, indices);
    }

    /**
     * Similar to VoTableReader#voToDataGroups(java.lang.String, boolean, int...), except it accepts an InputStream as the source,
     * enabling streaming of data rather than reading from a file.
     */
    public static DataGroup[] voToDataGroups(InputStream source, boolean headerOnly, int ...indices) throws IOException {
        VOElement docRoot = getVoTableRoot(source, null);
        return voToDataGroups(docRoot, headerOnly, indices);
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
     * returns an array of DataGroup from a votable file.
     * @param docRoot  the root element of the votable.
     * @param headerOnly  if true, returns only the headers, not the data.
     * @param indices   only return table from this list of indices
     * @return an array of DataGroup object
     */
    private static DataGroup[] voToDataGroups(VOElement docRoot,
                                             boolean headerOnly,
                                             int ...indices) throws IOException {
        TableParseHandler.Memory handler = new TableParseHandler.Memory(headerOnly, false);
        parse(handler, docRoot, indices);
        return handler.getAllTable();
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
        if (error == null) {
            // workaround for misplaced INFO attributes with errors
            NodeList infos = top.getElementsByVOTagName("INFO");            // all descendant elements
            String [] namesWithMisspelling = {"QUERY_STATUS","QUERY STATUS"};
            for (int i = 0; i < infos.getLength(); i++) {
                Node node = infos.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element info = (Element) node;
                    String name = info.getAttribute("name");
                    if (Arrays.asList(namesWithMisspelling).contains(name)
                            && "ERROR".equalsIgnoreCase(info.getAttribute("value"))) {
                        error = info.getTextContent();
                    }
                }
            }
        }
        return error;
    }

    private static VOElement getVoTableRoot(String location, StoragePolicy policy) throws IOException {

        String voTablePath = location;
        try {
            String url = new URL(location).toString();
            // location is a URL, download it first.
            File tmpFile = File.createTempFile("voreader-", ".xml", QueryUtil.getTempDir(null));
            try {
                HttpServices.getData( HttpServiceInput.createWithCredential(url), tmpFile);
                voTablePath = tmpFile.getPath();
            } catch (Exception e) {
                tmpFile.delete();
                LOG.error(e);
                throw new IOException("Unable to fetch URL: "+ location + "\n" + e.getMessage(), e);
            }
        } catch (MalformedURLException ex) { /* ok to ignore.  location may not be a URL */ }


        try {
            // at this point, voTablePath is a file path.
            return getVoTableRoot(new FileInputStream(voTablePath), policy);
        }  catch (Exception e) {
            throw new IOException("Unable to parse VOTABLE from "+ location + "\n" +
                    e.getMessage(), e);
        }
    }

    private static VOElement getVoTableRoot(InputStream source, StoragePolicy policy) throws IOException {
        try {
            policy = policy == null ? FIREFLY : policy;
            VOElementFactory voFactory = new VOElementFactory();
            voFactory.setStoragePolicy(policy);
            return voFactory.makeVOElement(new BufferedInputStream(source), null);
        } catch (Exception e) {
            throw new IOException("Unable to parse VOTable from stream: \n" + e.getMessage(), e);
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

        if (tableAry.isEmpty()) {
            String error = getQueryStatusError(docRoot);
            if (error != null) {
                throw new IOException(error);
            }
        }
        return tableAry;
    }

    private static ResourceInfo makeResourceInfo(VOElement res) {
        if (res == null) return null;
        ResourceInfo ri = new ResourceInfo(res.getID(), res.getName(),
                res.getAttribute("type"), res.getAttribute("utype"),
                res.getDescription());
        List<ParamInfo> params = Arrays.stream(res.getChildrenByName("PARAM"))
                .map(VoTableReader::paramElToParamInfo).collect(Collectors.toList());
        if (!params.isEmpty()) ri.setParams(params);

        List<GroupInfo> groups = Arrays.stream(res.getChildrenByName("GROUP"))
                .map(VoTableReader::groupElToGroupInfo).collect(Collectors.toList());
        if (!groups.isEmpty()) ri.setGroups(groups);

        Map<String, String> infos = new HashMap<>();
        Arrays.stream(res.getChildrenByName("INFO"))
                .forEach(i -> infos.put(i.getName(), i.getAttribute("value")));
        if (!infos.isEmpty()) ri.setInfos(infos);
        return ri;
    }

    // return this table's resource as well as all non-table <RESOURCE> from the given votable
    private static List<ResourceInfo> getResourcesForTable(VOElement docRoot, TableElement table) {

        if (docRoot == null) return null;
        List<ResourceInfo> resources = new ArrayList<>();
        applyIfNotEmpty(makeResourceInfo(table.getParent()), resources::add);      // ensure the first resource is for the given table.

        Arrays.stream(docRoot.getChildrenByName("RESOURCE"))
                .filter(res -> res.getChildrenByName("TABLE").length == 0)      // only the ones without TABLE
                .forEach( res -> applyIfNotEmpty(makeResourceInfo(res), resources::add));
        return resources;
    }

    /**
     * Parses the provided VOTable and sends events with the parsed data to the specified handler.
     *
     * @param handler  the handler responsible for processing the parsed data.
     * @param location the location of the VOTable; file path or URL
     * @param indices  a list of table indices to parse. If no indices are specified, parse all tables.
     * @throws IOException if an I/O error occurs during parsing.
     */
    public static void parse(TableParseHandler handler,
                             String location,
                             int ...indices) throws IOException {
        VOElement docRoot = getVoTableRoot(location, FIREFLY);
        parse(handler, docRoot, indices);
    }

    private static void parse(TableParseHandler handler,
                              VOElement docRoot,
                              int ...indices) throws IOException {
        try {
            handler.start();
            List<Integer> indicesList = indices == null ? Collections.emptyList() : Arrays.asList(Arrays.stream(indices).boxed().toArray(Integer[]::new));
            List<TableElement> tableAry = getAllTableElements(docRoot);
            for (int i = 0; i < tableAry.size(); i++) {
                if (indicesList.isEmpty() || indicesList.contains(i)) {
                    TableElement tableEl = tableAry.get(i);
                    List<ResourceInfo> resources = getResourcesForTable(docRoot, tableEl);
                    handler.resources(resources);
                    handler.startTable(i);
                    parseTable(handler, tableEl, new VOStarTable(tableEl));
                    handler.endTable(i);
                }
            }
        } catch (Exception e) {
            LOG.error(e);
            throw new IOException(e.getMessage());
        } finally {
            handler.end();
        }
    }

    private static void parseTable(TableParseHandler handler, TableElement tableEl, StarTable table) throws DataAccessException {

        DataGroup header = getTableHeader(tableEl);
        List<DataType> cols = Arrays.asList(header.getDataDefinitions());
        header.setInitCapacity((int)table.getRowCount());

        // post-process to handle custom logic
        DataType raCol = cols.stream().filter(dt -> HMS_UCD_PATTERN.matcher(String.valueOf(dt.getUCD())).matches())
                .findFirst().orElse(null);
        DataType decCol = cols.stream().filter(dt -> DMS_UCD_PATTERN.matcher(String.valueOf(dt.getUCD())).matches())
                .findFirst().orElse(null);
        if (raCol != null && decCol != null) {
            header.addAttribute("POS_EQ_RA_MAIN", raCol.getKeyName());
            header.addAttribute("POS_EQ_DEC_MAIN", decCol.getKeyName());
        }
        try {
            handler.header(header);

            // table data
            if (!handler.headerOnly()) {
                RowSequence rs = table.getRowSequence();
                while (rs.next()) {
                    handler.data(handleVariance(rs.getRow()));
                }
            }
        } catch (IOException e) {
            LOG.error(e);
        }
    }


    private static Object[] handleVariance(Object[] row) {
        for (int i = 0; i < row.length; i++) {
            if      (row[i] instanceof Double v && v.isNaN())   row[i] = null;
            else if (row[i] instanceof Float v && v.isNaN())    row[i] = null;
            else if (row[i] instanceof String v)    row[i] = v.trim();
        }
        return row;
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
        applyIfNotEmpty(table.getDescription(), v -> dg.getTableMeta().addKeyword(TableMeta.DESC, v));

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
        return dg;
    }


    // precision: "2" => "F2", "F2"=> "F2", "E3" => "E3"
    private static String makePrecisionStr(String precisionStr) {
        return (precisionStr.matches("^[1-9][0-9]*$")) ? ("F"+precisionStr) : precisionStr;
    }

    /**
     * Although name is a required attribute for FIELD, some VOTable may not provide it.
     * When name is not given, use ID if exists. Otherwise, use COLUMN_${IDX} where IDX is the index of the fields.
     * In VOTable section 3.2, name does not need to be unique, although it is recommended that name of a FIELD be unique for a TABLE.
     * In Firefly, DataType.name has to be unique.  Therefore, if name is not unique, we will append _${IDX} to it.
     * @param el    element of the field
     * @param colIdx    column index
     * @param cols  the list of columns
     * @return the column name for the given field.
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
        applyIfNotEmpty(group.getAttribute(UCD), gObj::setUCD);
        applyIfNotEmpty(group.getAttribute(UTYPE), gObj::setUtype);

        // add FIELDref
        Arrays.stream(group.getChildrenByName("FIELDref"))
                .forEach(pEl -> gObj.getColumnRefs().add(refElToRefInfo(pEl)));

        // add PARAMrefs
        Arrays.stream(group.getChildrenByName("PARAMref"))
                .forEach(pEl -> gObj.getParamRefs().add(refElToRefInfo(pEl)));

        // add PARAMs
        Arrays.stream(group.getChildrenByName("GROUP"))
            .forEach(gEl -> gObj.getGroupInfos().add(groupElToGroupInfo(gEl)));

        // add GROUPs
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
        applyIfNotEmpty(dt.convertStringToData(el.getAttribute("value")), dt::setValue);
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
        applyIfNotEmpty(el.getAttribute("xtype"), dt::setXType);
        applyIfNotEmpty(el.getDescription(), dt::setDesc);
        applyIfNotEmpty(el.getAttribute("datatype"), v -> {
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

        List<FileAnalysisReport.Part> parts = new ArrayList<>();
        tables.forEach(table -> {
            FileAnalysisReport.Part part = new FileAnalysisReport.Part(FileAnalysisReport.Type.Table);
            String utype= table.getAttribute(UTYPE).toLowerCase();
            if (MULTI_SPEC_UTYPE_LOWER.toLowerCase().equals(utype)) {
               part.setSearchProcessorId(MultiSpectrumProcessor.PROC_ID);
            }
            part.setIndex(parts.size());
            part.setFileLocationIndex(parts.size());
            DataGroup dg = getTableHeader(table);

            // populate number of rows
            if (table.hasAttribute("nrows")) {
                dg.setSize((int) table.getNrows());
            } else {
                // if no nrows attribute, pull in the data and count the rows.
                try {
                    dg.setSize((int)  new VOStarTable(table).getRowCount());
                } catch (IOException ignored) { }     // just ignore it.
            }

            Arrays.stream(dg.getDataDefinitions()).forEach(dt -> dt.setKeyName(getAliasName(dt)));  // show case-sensitive column names if exists
            List<ResourceInfo> resources = getResourcesForTable(docRoot, table);
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

