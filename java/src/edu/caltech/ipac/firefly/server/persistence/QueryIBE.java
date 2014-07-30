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
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.WspaceMeta;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.WorkspaceManager;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.DynQueryProcessor;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
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
public class QueryIBE extends DynQueryProcessor {

    @Override
    protected String getWspaceSaveDirectory() {
        return "/" + WorkspaceManager.SEARCH_DIR + "/" + WspaceMeta.IMAGESET;
    }


    @Override
    protected File loadDynDataFile(TableServerRequest request) throws IOException, DataAccessException {
        String mission = request.getParam("mission");
        Map<String,String> paramMap = getParamMap(request.getParams());

        IBE ibe = getIBE(mission, paramMap);
        IbeDataSource ibeDataSource = ibe.getIbeDataSource();
        IbeQueryParam queryParam= ibeDataSource.makeQueryParam(paramMap);
        File ofile = createFile(request); //File.createTempFile(mission+"-", ".tbl", ServerContext.getPermWorkDir());
        ibe.query(ofile, queryParam);
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

    Map<String,String> getParamMap(List<Param> params) {
        HashMap<String,String> paramMap = new HashMap<String,String>();
        for (Param p : params) {
            paramMap.put(p.getName(), p.getValue());
        }
        return paramMap;
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
}
