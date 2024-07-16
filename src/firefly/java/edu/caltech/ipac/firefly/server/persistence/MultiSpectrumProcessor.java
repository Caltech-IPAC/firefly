/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.persistence;

/**
 * @author loi
 *         $Id: $
 */

import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.EmbeddedDbProcessor;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.GroupInfo;
import edu.caltech.ipac.table.JsonTableUtil;
import edu.caltech.ipac.table.ParamInfo;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.table.TableUtil;

import java.io.File;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static edu.caltech.ipac.util.StringUtils.applyIfNotEmpty;
import static edu.caltech.ipac.util.StringUtils.isEmpty;

@SearchProcessorImpl(id = MultiSpectrumProcessor.PROC_ID, params=
        {@ParamDoc(name=MultiSpectrumProcessor.SOURCE, desc="URL or local path of the table file"),
         @ParamDoc(name=MultiSpectrumProcessor.MODE, desc="one of 'fetch', 'links', or 'extract'. Defaults to fetch."),
         @ParamDoc(name=MultiSpectrumProcessor.SEL_ROW_IDX, desc="Index of row to return links table.  Defaults to 0."),
         @ParamDoc(name=MultiSpectrumProcessor.SPECTR_IDX, desc="Index of spectrum to extract.  Defaults to 0."),
        })
public class MultiSpectrumProcessor extends EmbeddedDbProcessor {
    public static final String PROC_ID = "multi_spectrum";
    public static final String SOURCE = ServerParams.SOURCE;
    public static final String MODE = "mode";
    public static final String SEL_ROW_IDX = "sel_row_idx";
    public static final String SPECTR_IDX = "spectr_idx";

    private enum Mode { fetch,          // retrieve the MultiSpectrum table, return data product table
                        links,          // return links table to the spectrums
                        extract         // extract the spectrum from array data, then return a SpectralDM table.
    };


    public DataGroup fetchDataGroup(TableServerRequest treq) throws DataAccessException {
        try {
            String source = treq.getParam(SOURCE);
            File inFile = QueryUtil.resolveFileFromSource(source, treq);
            DataGroup table = TableUtil.readAnyFormat(inFile);

            return table;
        } catch (Exception e) {
            throw new DataAccessException("IBE query failed",  e);
        }
    }

    @Override
    protected DataGroupPart getResultSet(TableServerRequest treq, DbAdapter dbAdapter) throws DataAccessException {
        Mode mode = Mode.valueOf(treq.getParam(MODE, Mode.fetch.name()));

        MultiSpecInfo multiSpecInfo = findSpectrums(treq, dbAdapter);
        if (multiSpecInfo != null && multiSpecInfo.spectrums().size() > 0) {
            if (mode == Mode.fetch) {
                return createDataProductTable(treq, dbAdapter, multiSpecInfo);
            } else if (mode == Mode.links) {
                return createLinksTable(treq, dbAdapter, multiSpecInfo);
            } else if (mode == Mode.extract) {
                return createSpectrumTable(treq, dbAdapter, multiSpecInfo);
            }
        }
        return super.getResultSet(treq, dbAdapter);
    }


//====================================================================
//
//====================================================================


    record MultiSpecInfo(DataGroup table, List<DataType> metaCols, List<GroupInfo> spectrums){}


    private static MultiSpecInfo findSpectrums(TableServerRequest treq, DbAdapter dbAdapter) throws DataAccessException {
        DataGroup table = dbAdapter.getHeaders(dbAdapter.getDataTable());

        if (!table.getAttribute(TableMeta.UTYPE, "").equalsIgnoreCase("ipac:MultiSpectrum")) return null;

        List<GroupInfo> spectrums = table.getGroupInfos().stream()
                                    .filter(g -> String.valueOf(g.getUtype()).toLowerCase().matches("(?i)ipac:(Spectrum\\.)?ArrayData"))
                                    .collect(Collectors.toList());
        List<DataType> metaCols = Arrays.stream(table.getDataDefinitions())
                                    .filter(dt -> !dt.isArrayType()).collect(Collectors.toList());

        return new MultiSpecInfo(table, metaCols, spectrums);
    }

    private DataGroupPart createSpectrumTable(TableServerRequest treq, DbAdapter dbAdapter, MultiSpecInfo specs) throws DataAccessException {

        int selRow = treq.getIntParam(SEL_ROW_IDX, 0);
        int spectrIdx = treq.getIntParam(SPECTR_IDX, 0);
        GroupInfo selSpec = null;
        try {
            selSpec = (GroupInfo) specs.spectrums().get(spectrIdx).clone();
        } catch (CloneNotSupportedException e) {
            // should not happen.
        }

        if (selSpec == null) return null;

        List<GroupInfo.RefInfo> cols = getAllColRef(selSpec);

        String cnames = cols.stream().map(ri -> toCname(ri.getRef(), specs.table))
                        .map(this::quote)
                        .collect(Collectors.joining(","));
        String sql = String.format("SELECT %s FROM %s WHERE ROW_IDX = %d", cnames, dbAdapter.getDataTable(), selRow);

        DataGroup table = dbAdapter.execQuery(sql, dbAdapter.getDataTable());

        table = transformArrayToRows(table, getAllParamRef(selSpec));
        if (table == null) {
            return null;
        }

        // add SpectralDM meta
        table.addAttribute(TableMeta.UTYPE, "spec:Spectrum");
        selSpec.setUtype("spec:Spectrum.Data");
        table.setGroupInfos(Arrays.asList(selSpec));

        // therefore, convert all paramRefs to columnRefs
        table.getGroupInfos().forEach(gInfo -> {
            gInfo.getParamRefs().forEach(pRef -> {
                gInfo.getColumnRefs().add(pRef);
            });
            gInfo.setParamRefs(null);
        });



        return new DataGroupPart(table, 0, table.size());
    }

    private DataGroupPart createLinksTable(TableServerRequest treq, DbAdapter dbAdapter, MultiSpecInfo specs) {
        int selRow = treq.getIntParam(SEL_ROW_IDX, 0);

        DataGroup links = new DataGroup("links to spectra", new DataType[]{
                new DataType("ID", String.class),
                new DataType("access_url", String.class),
                new DataType("service_def", String.class),
                new DataType("error_message", String.class),
                new DataType("description", String.class),
                new DataType("semantics", String.class),
                new DataType("content_type", String.class),
                new DataType("content_length", String.class),
                new DataType("local_semantics", String.class),
        });

        for (int i = 0; i < specs.spectrums().size(); i++) {
            GroupInfo spec = specs.spectrums().get(i);
            String desc = isEmpty(spec.getName()) ? spec.getID() : spec.getName();
            links.add(new String[]{selRow+"", createSpectrumUrl(treq, selRow, i), "", "", desc, "#this", "application/x-votable+xml", "", "#grid-spectrum"});
        }

        return new DataGroupPart(links, 0, links.size());
    }

    private DataGroupPart createDataProductTable(TableServerRequest treq, DbAdapter dbAdapter, MultiSpecInfo specs) throws DataAccessException {
        if (isEmpty(treq.getInclColumns())) {
            treq = (TableServerRequest) treq.cloneRequest();
            treq.setInclColumns(  specs.metaCols.stream()
                    .map(DataType::getKeyName)
                    .map(this::quote)
                    .toArray(String[]::new)
            );
        }

        DataGroupPart dgp = dbAdapter.execRequestQuery(treq, dbAdapter.getDataTable());
        DataGroup table = dgp.getData();

        Arrays.asList("dataproduct_type", "access_format", "access_url")
                .forEach(cname -> {
                    DataType col = new DataType(cname, String.class);
                    col.setVisibility(DataType.Visibility.hidden);
                    table.addDataDefinition(col);
                });

        table.setGroupInfos(table.getGroupInfos().stream()
                .filter(gi -> !"ipac:Spectrum.ArrayData".equalsIgnoreCase(String.valueOf(gi.getUtype())))
                .collect(Collectors.toList()));     // remove all Spectrum.ArrayData groups;
        table.setResourceInfos(table.getResourceInfos().stream()
                .filter(ri -> !"ipac:MultiSpectrum".equalsIgnoreCase(String.valueOf(ri.getUtype())))
                .collect(Collectors.toList()));     // remove all ipac:MultiSpectrum resources;

        for (int i = 0; i < table.size(); i++) {
            table.setData("dataproduct_type", i, "spectrum");
            table.setData("access_format", i, "application/x-votable+xml;content=datalink");
            table.setData("access_url", i, createLinksUrl(treq, (int) table.getData(DataGroup.ROW_IDX, i)));
        }
        return dgp;
    }

    private String quote(String s) {
        return isEmpty(s) ? "" : "\"" + s.replace("\"", "\\\"") + "\"";
    }

    private String createLinksUrl(TableServerRequest treq, int rowIdx) {

        String baseUrl = ServerContext.getRequestOwner().getBaseUrl() + "CmdSrv/sync?cmd=tableSearch&FORMAT=votable&request=";
        TableServerRequest request = (TableServerRequest) treq.cloneRequest();
        request.keepBaseParamOnly();
        request.setParam(MODE, Mode.links.name());
        request.setParam(SEL_ROW_IDX, rowIdx+"");

        return baseUrl + QueryUtil.encode(JsonTableUtil.toJsonTableRequest(request).toJSONString());
    }

    private String createSpectrumUrl(TableServerRequest treq, int selRow, int spectrIdx) {
        String baseUrl = ServerContext.getRequestOwner().getBaseUrl() + "CmdSrv/sync?cmd=tableSearch&FORMAT=votable&request=";
        TableServerRequest request = (TableServerRequest) treq.cloneRequest();
        request.keepBaseParamOnly();
        request.setParam(MODE, Mode.extract.name());
        request.setParam(SEL_ROW_IDX, selRow+"");
        request.setParam(SPECTR_IDX, spectrIdx+"");

        return baseUrl + QueryUtil.encode(JsonTableUtil.toJsonTableRequest(request).toJSONString());
    }

    private List<GroupInfo.RefInfo> getAllColRef(GroupInfo root) {
        List<GroupInfo.RefInfo> rval = root.getColumnRefs();
        for (int i = 0; i < root.getGroupInfos().size(); i++) {
            rval.addAll(getAllColRef(root.getGroupInfos().get(i)));
        }
        return rval;
    }

    private List<GroupInfo.RefInfo> getAllParamRef(GroupInfo root) {
        List<GroupInfo.RefInfo> rval = root.getParamRefs();
        for (int i = 0; i < root.getGroupInfos().size(); i++) {
            rval.addAll(getAllParamRef(root.getGroupInfos().get(i)));
        }
        return rval;
    }

    private String toCname(String ref, DataGroup table) {
        return ref;  // assume ID and name are the same for now.  if there's time, resolve cname if ID is not the same as column name.
    }

    /**
     * Assuming it's a table of 1 row where each cell is an array of the same length.
     * Expand the arrays into rows, returning a table of n rows where n is the size of the array.
     * @param table
     * @param params
     * @return
     */
    private DataGroup transformArrayToRows(DataGroup table, List<GroupInfo.RefInfo> params) {
        List<DataType> cols = Arrays.stream(table.getDataDefinitions())
                            .map(c -> {
                                DataType nc = c.newCopyOf();
                                nc.setArraySize(null);
                                return nc;
                            }).collect(Collectors.toList());

        params.forEach(p -> cols.add(table.getParam(p.getRef())));  // add any parameter as column

        DataGroup ntable = new DataGroup("spectrum", cols);
        applyIfNotEmpty(table.getTitle(), ntable::setTitle);

        if (table.size() > 1) {
            Logger.getLogger().warn("MultiSpectrumProcessor:transformArrayToRows is expecting only 1 row but received: " + table.size());
        }
        Object[] cary = table.get(0).getData();
        if (!cary[0].getClass().isArray()) {
            Logger.getLogger().error("MultiSpectrumProcessor:transformArrayToRows: table cell is not an array");
            return null;
        }
        int nrows = Array.getLength(cary[cary.length-1]);       // assuming the added data has the right values.  need to find a better way to handle this.
        for (int i = 0; i < nrows; i++) {
            Object[] rowData = new Object[cols.size()];
            int csize = (cols.size() - params.size());
            for (int c = 0; c < csize; c++) {
                rowData[c] = getAryVal(cary, c, i);;
            }
            for (int c = 0; c < params.size(); c++) {
                ParamInfo pinfo = table.getParam(toCname(params.get(c).getRef(), table));
                rowData[csize+c] = pinfo == null ? null : getAryVal(pinfo.getValue(), i);
            }
            ntable.add(rowData);
        }
        return ntable;
    }
    private Object getAryVal(Object[] aryOfAry, int oIdx, int idx) {
        try {
            return getAryVal(aryOfAry[oIdx], idx);
        } catch (Exception e) {
            return null;
        }
    }

    private Object getAryVal(Object ary, int idx) {
        Object v = ary;
        if (ary != null && ary.getClass().isArray()) {
            try {
                v = Array.get(ary, idx);
            } catch (Exception e) {
                return null;
            } // assign null for now.  TBD: decide what we should do.
        }
        return v;
    }

}
