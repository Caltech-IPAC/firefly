/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.persistence;

/**
 * @author tatianag
 *         $Id: $
 */

import edu.caltech.ipac.astro.ibe.IBE;
import edu.caltech.ipac.astro.ibe.IbeDataSource;
import edu.caltech.ipac.astro.ibe.IbeQueryParam;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.EmbeddedDbProcessor;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.table.io.IpacTableReader;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SearchProcessorImpl(id = "ibe_processor", params=
        {@ParamDoc(name="mission", desc="mission"),
         @ParamDoc(name="UserTargetWorldPt", desc="the target point, a serialized WorldPt object"),
         @ParamDoc(name="radius", desc="radius in degrees"),
         @ParamDoc(name="mcenter", desc="Specifies whether to return only the most centered (in pixel space) image-set for the given input position.")
        })
public class QueryIBE extends EmbeddedDbProcessor {
    public static final String PROC_ID = QueryIBE.class.getAnnotation(SearchProcessorImpl.class).id();
    public static final String POS_WORLDPT = "UserTargetWorldPt";
    public static final String RADIUS = "radius";
    public static final String MOST_CENTER = "mcenter";



    protected String getFilePrefix(TableServerRequest request) {
        return request.getParam(MetaConst.MISSION);
    }

    @Override
    public DataGroup fetchDataGroup(TableServerRequest request) throws DataAccessException {
        try {
            String mission = request.getParam(MetaConst.MISSION);
            Map<String,String> paramMap = IBEUtils.getParamMap(request.getParams());

            IBE ibe = null;
                ibe = IBEUtils.getIBE(mission, paramMap);
            IbeDataSource ibeDataSource = ibe.getIbeDataSource();
            IbeQueryParam queryParam= ibeDataSource.makeQueryParam(paramMap);
            File ofile = createTempFile(request, ".tbl"); //File.createTempFile(mission+"-", ".tbl", ServerContext.getPermWorkDir());
            ibe.query(ofile, queryParam);

            // no search results situation
            if (ofile == null || !ofile.exists() || ofile.length() == 0) {
                throw new DataAccessException("Fail during QueryIBE: no data returned");
            }

            if (request.getSortInfo() == null) {
                SortInfo sortInfo = IBEUtils.getSortInfo(ibeDataSource);
                request.setSortInfo(sortInfo);
            }
            DataGroup dg = IpacTableReader.read(ofile);
            addAddtlMeta(dg.getTableMeta(), Arrays.asList(dg.getDataDefinitions()), request);
            return dg;
        } catch (IOException e) {
            throw new DataAccessException("Fail during QueryIBE:" + e.getMessage(), e);
        }
    }

    private void addAddtlMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        super.prepareTableMeta(meta, columns, request);
        try {
            String mission = request.getParam(MetaConst.MISSION);
            Map<String,String> paramMap = IBEUtils.getParamMap(request.getParams());

            IBE ibe = IBEUtils.getIBE(mission, paramMap);
            IbeDataSource source = ibe.getIbeDataSource();
            CacheKey cacheKey = new StringKey("ibemeta", source.getIbeHost(), source.getMission(), source.getDataset(), source.getTableName());
            Cache cache = CacheManager.getCache(Cache.TYPE_PERM_SMALL);
            DataGroup coldefs = (DataGroup) cache.get(cacheKey);
            if (coldefs == null) {
                File ofile = File.createTempFile(mission+"-dd", ".tbl", ServerContext.getPermWorkDir());
                ibe.getMetaData(ofile);
                coldefs = IpacTableReader.read(ofile);
                cache.put(cacheKey, coldefs);
            }

            for (DataObject row : coldefs) {
                String col = String.valueOf(row.getDataElement("name"));
                if (exists(columns, col)) {
                    String desc = String.valueOf(row.getDataElement("description"));
                    meta.setAttribute(TableMeta.makeAttribKey(TableMeta.DESC_TAG, col), desc);
                }
            }

            meta.setAttribute("host", source.getIbeHost());
            meta.setAttribute(MetaConst.MISSION, source.getMission());
            meta.setAttribute("dataset", source.getDataset());
            meta.setAttribute("table", source.getTableName());
            meta.setAttribute("subsize", request.getParam("subsize"));
            meta.setAttribute(MetaConst.IMAGE_SOURCE_ID, source.getMission());
            meta.setAttribute("ALL_CORNERS", source.getCorners());
            meta.setAttribute("CENTER_COLUMN", source.getCenterCols());
            meta.setAttribute("PREVIEW_COLUMN", "download");
            meta.setAttribute("RESOURCE_TYPE", "URL");

            String [] colsToHide = source.getColsToHide();

            for (String c : colsToHide) {
                meta.setAttribute(TableMeta.makeAttribKey(TableMeta.VISI_TAG, c), DataType.Visibility.hide.name());
            }

            // mission specific attributes
            IBEUtils.getMissionSpecificTableMeta(source).forEach( (k,v) -> meta.setAttribute(k,v));

        }
        catch (Exception ignored) {
            Logger.getLogger().error(ignored,"Error ignored");
        }
    }

    private boolean exists(List<DataType> cols, String name) {
        for (DataType dt : cols) {
            if (dt.getKeyName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

}
