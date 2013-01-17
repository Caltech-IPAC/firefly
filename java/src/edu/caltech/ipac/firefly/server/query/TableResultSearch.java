package edu.caltech.ipac.firefly.server.query;
/**
 * User: roby
 * Date: 3/13/12
 * Time: 12:54 PM
 */


import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.dyn.DynUtils;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.util.multipart.MultiPartData;
import edu.caltech.ipac.firefly.server.visualize.VisContext;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
@SearchProcessorImpl(id = "resultSearch", params =
        {@ParamDoc(name = TableResultSearch.LOAD_TABLE, desc = "name of the ipac table in the upload directory"),
         @ParamDoc(name = CommonParams.CACHE_KEY, desc = "the cache key with more parameters")
        })
public class TableResultSearch extends DynQueryProcessor {

    public static final String LOAD_TABLE= "loadTable";

    public static final String FILE_COLUMN = "fileColumn";
    public static final String URL_COLUMN = "urlColumn";
    public static final String IS_UPLOAD = "isUpload";
    public static final String FILE = "file";
    public static final String URL = "url";


    @Override
    protected File loadDynDataFile(TableServerRequest req) throws IOException, DataAccessException {
        setXmlParams(req);
        boolean isUpload= req.getBooleanParam(IS_UPLOAD);
        addSearchParmsToData(req, isUpload);
        File f= null;
        if  (req.containsParam(LOAD_TABLE)) {
            f= VisContext.convertToFile(req.getParam(LOAD_TABLE));
        }
        return f;
    }

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        super.prepareTableMeta(meta, columns, request);
        Map<String,String> params= getCacheParams(request);
        if (params!=null) {
            if (params.containsKey(FILE_COLUMN)) {
                meta.setAttribute(CommonParams.PREVIEW_SOURCE_HEADER, "FILE");
                meta.setAttribute(CommonParams.PREVIEW_COLUMN_HEADER, params.get(FILE_COLUMN));
            }
            else if (params.containsKey(URL_COLUMN)) {
                meta.setAttribute(CommonParams.PREVIEW_SOURCE_HEADER, "URL");
                meta.setAttribute(CommonParams.PREVIEW_COLUMN_HEADER, params.get(URL_COLUMN));
            }
            else if (isUsingResolveProcessor(params.get(CommonParams.RESOLVE_PROCESSOR))) {
                meta.setAttribute(CommonParams.PREVIEW_SOURCE_HEADER, "PROCESSOR");
                meta.setAttribute(TableServerRequest.ID_KEY, MultiMissionFileRetrieve.ID);
                meta.setAttribute(CommonParams.CACHE_KEY, getKey(request).getUniqueString());
            }
        }

    }

    public static MultiPartData addSearchParmsToData(TableServerRequest req, boolean create) {
        MultiPartData data;
        if (create) {
            StringKey key= new StringKey("ShowResult", System.currentTimeMillis());
            data =  new MultiPartData(key);
            for (Param p : req.getParams()) {
                data.addParam(p.getName(),p.getValue());
            }
            addToCache(data);
        }
        else {
            data= getCacheData(req);
        }
        if (data!=null) {
            Map<String,String> params= data.getParams();
            if (isUsingResolveProcessor(params.get(CommonParams.RESOLVE_PROCESSOR))) {
                String processorKey= params.get(CommonParams.RESOLVE_PROCESSOR);
                String missionSearchParts= req.getParam(processorKey);
                String partsAry[]= missionSearchParts.split(":", 5);
                if (partsAry.length==4) {
                    params.put(DynUtils.HYDRA_PROJECT_ID, partsAry[0]);
                    params.put(DynUtils.SEARCH_NAME,      partsAry[1]);
                    params.put(DynUtils.QUERY_ID,         partsAry[2]);
                    params.put(CommonParams.SEARCH_PROCESSOR_ID, partsAry[3]);
                    addToCache(data);
                }
            }
            if (!req.containsParam(CommonParams.CACHE_KEY)) {
                req.setParam(CommonParams.CACHE_KEY, data.getCacheKey().toString());
            }
        }
        return data;
    }

    private static boolean isUsingResolveProcessor(String rv) {
        boolean using= true;
        if (rv==null || rv.equalsIgnoreCase(FILE) || rv.equalsIgnoreCase(URL)) {
            using= false;
        }
        return using;
    }


    public static MultiPartData getCacheData(ServerRequest r) {
        MultiPartData retval= null;
        if (r.containsParam(CommonParams.CACHE_KEY)) {
            CacheKey key= new StringKey(r.getParam(CommonParams.CACHE_KEY));
            if (getParamCache().isCached(key)) {
                try {
                    retval= (MultiPartData)getParamCache().get(key);
                } catch (ClassCastException e) {
                    // do nothing
                }
            }
        }
        return retval;
    }


    public static Map<String,String> getCacheParams(ServerRequest r) {
        Map<String,String> retval= null;
        MultiPartData data= getCacheData(r);
        if (data!=null) retval= data.getParams();
        return retval;
    }

    public static CacheKey getKey(ServerRequest r) {
        if (r.containsParam(CommonParams.CACHE_KEY)) {
            return new StringKey(r.getParam(CommonParams.CACHE_KEY));
        }
        else {
            return null;
        }
    }

    public static void addToCache(MultiPartData data) {
        getParamCache().put(data.getCacheKey(), data);
    }

    public static Cache getParamCache() {
        Cache cache= CacheManager.getCache(Cache.TYPE_PERM_SMALL);
        return cache;
    }

}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
