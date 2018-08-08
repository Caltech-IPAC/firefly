/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.persistence;

/**
 * @author tatianag
 *         $Id: $
 */

import edu.caltech.ipac.table.io.IpacTableReader;
import edu.caltech.ipac.astro.ibe.IBE;
import edu.caltech.ipac.astro.ibe.IbeDataSource;
import edu.caltech.ipac.astro.ibe.IbeQueryParam;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.IpacTablePartProcessor;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.table.TableDef;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.IpacTableUtil;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@SearchProcessorImpl(id = "ibe_processor", params=
        {@ParamDoc(name="mission", desc="mission"),
         @ParamDoc(name="UserTargetWorldPt", desc="the target point, a serialized WorldPt object"),
         @ParamDoc(name="radius", desc="radius in degrees"),
         @ParamDoc(name="mcenter", desc="Specifies whether to return only the most centered (in pixel space) image-set for the given input position.")
        })
public class QueryIBE extends IpacTablePartProcessor {
    public static final String PROC_ID = QueryIBE.class.getAnnotation(SearchProcessorImpl.class).id();
    public static final String MISSION = "mission";
    public static final String POS_WORLDPT = "UserTargetWorldPt";
    public static final String RADIUS = "radius";
    public static final String MOST_CENTER = "mcenter";


    @Override
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {

        String mission = request.getParam(MISSION);
        Map<String,String> paramMap = IBEUtils.getParamMap(request.getParams());

        IBE ibe = IBEUtils.getIBE(mission, paramMap);
        IbeDataSource ibeDataSource = ibe.getIbeDataSource();
        IbeQueryParam queryParam= ibeDataSource.makeQueryParam(paramMap);
        File ofile = createFile(request); //File.createTempFile(mission+"-", ".tbl", ServerContext.getPermWorkDir());
        ibe.query(ofile, queryParam);

        // no search results situation
        if (ofile == null || !ofile.exists() || ofile.length() == 0) {
            return ofile;
        }

        SortInfo sortInfo = IBEUtils.getSortInfo(ibeDataSource);
        if (sortInfo != null) {
            TableDef meta = IpacTableUtil.getMetaInfo(ofile);
            if (meta.getRowCount() < 100000) {
                doSort(ofile, ofile, sortInfo, request);
            }
        }
        return ofile;
    }

    @Override
    protected String getFilePrefix(TableServerRequest request) {
        return request.getParam(MISSION);
    }

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        super.prepareTableMeta(meta, columns, request);
        try {
            String mission = request.getParam(MISSION);
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
            meta.setAttribute(MISSION, source.getMission());
            meta.setAttribute("dataset", source.getDataset());
            meta.setAttribute("table", source.getTableName());
            meta.setAttribute("subsize", request.getParam("subsize"));
            meta.setAttribute(MetaConst.DATASET_CONVERTER, source.getMission());
            meta.setAttribute("ALL_CORNERS", source.getCorners());
            meta.setAttribute("CENTER_COLUMN", source.getCenterCols());
            meta.setAttribute("PREVIEW_COLUMN", "download");
            meta.setAttribute("RESOURCE_TYPE", "URL");

            String [] colsToHide = source.getColsToHide();

            for (String c : colsToHide) {
                meta.setAttribute(TableMeta.makeAttribKey(TableMeta.VISI_TAG, c), DataType.Visibility.hide.name());
            }

            // mission specific attributes
            meta.setAttributes(IBEUtils.getMissionSpecificTableMeta(source));

        }
        catch (Exception ignored) {
            LOGGER.error(ignored,"Error ignored");
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
