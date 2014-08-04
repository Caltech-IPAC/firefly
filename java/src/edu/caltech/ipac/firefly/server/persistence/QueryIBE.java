package edu.caltech.ipac.firefly.server.persistence;

/**
 * @author tatianag
 *         $Id: $
 */

import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.astro.ibe.IBE;
import edu.caltech.ipac.astro.ibe.IbeDataSource;
import edu.caltech.ipac.astro.ibe.IbeQueryParam;
import edu.caltech.ipac.astro.ibe.datasource.PtfIbeDataSource;
import edu.caltech.ipac.astro.ibe.datasource.WiseIbeDataSource;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.WspaceMeta;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.WorkspaceManager;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.IpacTablePartProcessor;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.IpacTableParser;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SearchProcessorImpl(id = "ibe_processor", params=
        {@ParamDoc(name="mission", desc="mission"),
         @ParamDoc(name="UserTargetWorldPt", desc="the target point, a serialized WorldPt object"),
         @ParamDoc(name="radius", desc="radius in degrees"),
         @ParamDoc(name="mcenter", desc="Specifies whether to return only the most centered (in pixel space) image-set for the given input position.")
        })
public class QueryIBE extends IpacTablePartProcessor {

    @Override
    protected String getWspaceSaveDirectory() {
        return "/" + WorkspaceManager.SEARCH_DIR + "/" + WspaceMeta.IMAGESET;
    }


    @Override
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {

        String mission = request.getParam("mission");
        Map<String,String> paramMap = getParamMap(request.getParams());

        IBE ibe = getIBE(mission, paramMap);
        IbeDataSource ibeDataSource = ibe.getIbeDataSource();
        IbeQueryParam queryParam= ibeDataSource.makeQueryParam(paramMap);
        File ofile = createFile(request); //File.createTempFile(mission+"-", ".tbl", ServerContext.getPermWorkDir());
        ibe.query(ofile, queryParam);

        // no search results situation
        if (ofile == null || !ofile.exists() || ofile.length() == 0) {
            return ofile;
        }

        SortInfo sortInfo = getSortInfo(ibeDataSource);
        if (sortInfo != null) {
            DataGroupPart.TableDef meta = IpacTableParser.getMetaInfo(ofile);
            if (meta.getRowCount() < 100000) {
                doSort(ofile, ofile, sortInfo, request.getPageSize());
            }
        }
        return ofile;
    }

    IBE getIBE(String mission, Map<String,String> paramMap) throws IOException, DataAccessException {
        IbeDataSource  ibeDataSource;
        if (StringUtils.isEmpty(mission)) {
            throw new DataAccessException("Unspecified mission");
        }
        if (mission.equals(WiseIbeDataSource.WISE)) {
            ibeDataSource = new WiseIbeDataSource();
        } else if (mission.equals(PtfIbeDataSource.PTF)) {
            ibeDataSource = new PtfIbeDataSource();
        } else {
            throw new DataAccessException("Unsupported mission: "+mission);
        }
        ibeDataSource.initialize(paramMap);
        return new IBE(ibeDataSource);
    }

    SortInfo getSortInfo(IbeDataSource source) {
        if (source instanceof WiseIbeDataSource) {
            String productLevel = ((WiseIbeDataSource)source).getDataProduct().plevel();
            if (productLevel.startsWith("1")) {
                return new SortInfo("scan_id","frame_num","band");
            } else if (productLevel.startsWith("3")) {
                return new SortInfo("coadd_id","band");
            }
        } else if (source instanceof PtfIbeDataSource) {
            // TODO PTF
            return null;
        }
        return null;
    }

    Map<String,String> getParamMap(List<Param> params) {
        HashMap<String,String> paramMap = new HashMap<String,String>();
        for (Param p : params) {
            paramMap.put(p.getName(), p.getValue());
        }
        return paramMap;
    }

    @Override
    protected String getFilePrefix(TableServerRequest request) {
        return request.getParam("mission");
    }

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        super.prepareTableMeta(meta, columns, request);
        try {
            String mission = request.getParam("mission");
            Map<String,String> paramMap = getParamMap(request.getParams());

            IBE ibe = getIBE(mission, paramMap);
            IbeDataSource source = ibe.getIbeDataSource();
            CacheKey cacheKey = new StringKey("ibemeta", source.getIbeHost(), source.getMission(), source.getDataset(), source.getTableName());
            Cache cache = CacheManager.getCache(Cache.TYPE_PERM_SMALL);
            DataGroup coldefs = (DataGroup) cache.get(cacheKey);
            if (coldefs == null) {
                File ofile = File.createTempFile(mission+"-dd", ".tbl", ServerContext.getPermWorkDir());
                ibe.getMetaData(ofile);
                coldefs = IpacTableReader.readIpacTable(ofile, "coldefs");
                cache.put(cacheKey, coldefs);
                for (DataObject row : coldefs) {
                    String col = String.valueOf(row.getDataElement("name"));
                    if (exists(columns, col)) {
                        String desc = String.valueOf(row.getDataElement("description"));
                        meta.setAttribute(DataSetParser.makeAttribKey(DataSetParser.DESC_TAG, col), desc);
                    }
                }
            }
            meta.setAttribute("host", source.getIbeHost());
            meta.setAttribute("schemaGroup", source.getMission());
            meta.setAttribute("schema", source.getDataset());
            meta.setAttribute("table", source.getTableName());
            meta.setAttribute("ALL_CORNERS", "ra1;dec1;EQ_J2000,ra2;dec2;EQ_J2000,ra3;dec3;EQ_J2000,ra4;dec4;EQ_J2000");
            meta.setAttribute("CENTER_COLUMN", "crval1;crval2;EQ_J2000");
            meta.setAttribute("PREVIEW_COLUMN", "download");
            meta.setAttribute("RESOURCE_TYPE", "URL");

            String [] colsToHide = {"in_row_id", "in_ra", "in_dec",
                        "crval1", "crval2",
                        "ra1", "ra2", "ra3", "ra4",
                        "dec1", "dec2", "dec3", "dec4"
            };

            for (String c : colsToHide) {
                meta.setAttribute(DataSetParser.makeAttribKey(DataSetParser.VISI_TAG, c), DataSetParser.VISI_HIDE);
            }

            // mission specific attributes
            meta.setAttributes(getMissionSpecificTableMeta(source));

        }
        catch (Exception ignored) {}
    }

    private boolean exists(List<DataType> cols, String name) {
        for (DataType dt : cols) {
            if (dt.getKeyName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private Map<String,String> getMissionSpecificTableMeta(IbeDataSource source) {
        Map<String,String> attribs = new HashMap<String,String>();
        String [] colsToHide = null;
        String relatedCols = null;
        Map<String,String> sortByCols = new HashMap<String,String>(2);
        if (source instanceof WiseIbeDataSource) {
            String productLevel = ((WiseIbeDataSource)source).getDataProduct().plevel();
            attribs.put("ProductLevel", productLevel);
            if (productLevel.startsWith("1")) {
                // level 1
                sortByCols.put("scan_id", "scan_id,frame_num,band");
                sortByCols.put("frame_num", "frame_num,scan_id,band");
                colsToHide = new String[]{"magzp", "magzpunc", "modeint", "scangrp",
                        "utanneal", "exptime", "debgain", "febgain", "qual_scan",
                        "date_imgprep", "date_obs", "qa_status"};
                relatedCols = "scan_id,frame_num";
            } else {
                // level 3
                sortByCols.put("coadd_id", "coadd_id,band");
                colsToHide = new String[]{"magzp", "magzpunc", "qual_coadd",
                        "date_imgprep", "qa_status"};
                relatedCols = "coadd_id";
            }
        } else if (source instanceof PtfIbeDataSource) {
            // TODO: PTF
        }

        for (String sortCol : sortByCols.keySet()) {
            attribs.put(DataSetParser.makeAttribKey(DataSetParser.SORT_BY_TAG, sortCol), sortByCols.get(sortCol));
        }

        if (colsToHide != null) {
            for (String c : colsToHide) {
                attribs.put(DataSetParser.makeAttribKey(DataSetParser.VISI_TAG, c), DataSetParser.VISI_HIDE);
            }
        }

        if (relatedCols != null) {
            attribs.put("col.related", relatedCols);
        }
        return attribs;
    }
}
